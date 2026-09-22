package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.entity.EntityMagManipBlock;
import cn.academy.util.Plotter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * La manipulation magnetique d'un bloc, portage de {@code MagManip} : on arrache un bloc
 * metallique, on le tient devant soi, et on le jette.
 *
 * <p>C'est la competence qui donne des mains a l'electromaster. Elle prend le bloc de la main
 * s'il est metallique, sinon le premier bloc metallique du regard a dix blocs — un rail, une
 * barre de fer, un bloc de fer, un wagonnet non : les entites ne sont pas concernees — et le
 * bloc devient une entite qui <b>suit</b> son porteur, deux blocs devant ses yeux.
 *
 * <p>Au relachement, deux destins :
 *
 * <ul>
 * <li>si le bloc est a moins de <b>cinq</b> blocs — vingt-cinq, au carre, comme l'original —
 *     et que la reserve suit, il <b>part</b> vers ce que le regard touche a vingt blocs, a la
 *     vitesse de l'experience (0,5 a 1 bloc par tick), frappe ce qu'il traverse de dix points,
 *     et se repose ou il tombe ; c'est la que la competence se paie, 140 a 270 CP (divises par
 *     28) et 35 a 20 de surcout, et qu'elle pose sa recharge de 60 a 40 ticks ;</li>
 * <li>sinon, il <b>tombe</b> simplement, et se repose ou il touche.</li>
 * </ul>
 *
 * <p>Rien n'est paye a l'appui : comme la detection de minerais, la competence paie dans son
 * effet ({@link #paysOnEffect}), parce que tout depend de ce qu'elle a trouve — et
 * {@link #canStart} refuse avant meme d'ouvrir quand il n'y a rien a prendre.
 *
 * <h2>Ce que la main tient</h2>
 *
 * Un bloc de la main est consomme — sauf en creatif — et devient le projectile ; un bloc
 * arrache au sol est retire du monde. Dans les deux cas le bloc n'est pas perdu quand la pose
 * echoue : voir {@link EntityMagManipBlock}.
 *
 * <p>Non portes : la sonde de particules de l'original, et le son de boucle.
 */
public class MagManipSkill extends Skill {

    /** Le regard cherche un bloc metallique a dix blocs. */
    public static final double GRAB_RANGE = 10.0;

    /** Le lancer vise a vingt blocs. */
    public static final double THROW_RANGE = 20.0;

    /** Le prix du lancer : 140 a 270 CP divises par 28, et 35 a 20 de surcout. */
    public static final float CP_MIN_EXP = 5f;
    public static final float CP_MAX_EXP = 9.64f;
    public static final float OVERLOAD_MIN_EXP = 35f;
    public static final float OVERLOAD_MAX_EXP = 20f;

    /** La recharge du lancer : 60 ticks au depart, 40 au maximum. */
    public static final int COOLDOWN_MIN_EXP = 40;
    public static final int COOLDOWN_MAX_EXP = 60;

    /** La vitesse du lancer : de 0,5 a 1 bloc par tick. */
    public static final double SPEED_MIN_EXP = 0.5;
    public static final double SPEED_MAX_EXP = 1.0;

    /** Et 0,005 d'experience par lancer. */
    public static final float EXP_PER_THROW = 0.005f;

    public MagManipSkill() {
        super("mag_manip", 2);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    /** Ce qu'un lancer coute a la reserve. */
    public float consumption(AbilityData data) {
        return lerp(CP_MIN_EXP, CP_MAX_EXP, data.getSkillExp(this));
    }

    /** Et a la surcharge. */
    public float overload(AbilityData data) {
        return lerp(OVERLOAD_MIN_EXP, OVERLOAD_MAX_EXP, data.getSkillExp(this));
    }

    /** La vitesse du lancer. */
    public double speed(AbilityData data) {
        return lerp((float) SPEED_MIN_EXP, (float) SPEED_MAX_EXP, data.getSkillExp(this));
    }

    /** La recharge posee par le lancer, et seulement s'il a eu lieu. */
    public int cooldown(AbilityData data) {
        return (int) lerp(COOLDOWN_MAX_EXP, COOLDOWN_MIN_EXP, data.getSkillExp(this));
    }

    /** Ce que la manipulation sait arracher : un bloc metallique, et rien d'autre. */
    public static boolean accepts(Block block) {
        return MetalTargets.isMetalBlock(block);
    }

    // ------------------------------------------------------------------
    // Le maintien
    // ------------------------------------------------------------------

    @Override
    public boolean isHeld() {
        return true;
    }

    /** Aucune duree : le bloc se tient tant que la touche reste enfoncee. */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return 0;
    }

    /** Rien n'est paye a l'appui : c'est le lancer qui paie, et seulement s'il part. */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /** Rien a arracher, rien a tenir : l'original terminait sans rien facturer. */
    @Override
    public boolean canStart(Player player, AbilityData data) {
        if (player.getMainHandItem().getItem() instanceof BlockItem item && accepts(item.getBlock())) {
            return true;
        }
        return findMetalBlock(player) != null;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        BlockState state;
        Vec3 position;
        BlockPos aimed = findMetalBlock(player);
        if (aimed != null) {
            state = player.level().getBlockState(aimed);
            player.level().removeBlock(aimed, false);
            position = Vec3.atCenterOf(aimed);
        } else {
            ItemStack stack = player.getMainHandItem();
            BlockItem item = (BlockItem) stack.getItem();
            state = item.getBlock().defaultBlockState();
            position = player.getEyePosition(1f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }

        EntityMagManipBlock block = new EntityMagManipBlock(player.level(), player, state, position);
        player.level().addFreshEntity(block);
        data.setHoldTarget(this, block.getId());
    }

    /** Le bloc suit les yeux, comme la cible d'un maintien qui se deplace. */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        EntityMagManipBlock block = heldBlock(player, data);
        if (block == null) return false;
        block.carryTo(MagManipVisuals.carryTarget(player.getEyePosition(1f),
                player.getViewVector(1f)));
        return true;
    }

    /**
     * Le relachement : le bloc part, ou tombe.
     *
     * <p>Rend {@code false} dans tous les cas — le maintien se termine, et le bloc vit sa vie.
     * L'original faisait exactement cela : son contexte mourait a son {@code s_perform}, quel
     * que soit le sort du bloc.
     */
    @Override
    public boolean onRelease(Player player, AbilityData data, int heldTicks) {
        EntityMagManipBlock block = heldBlock(player, data);
        if (block == null) return false;

        boolean thrown = player.distanceToSqr(block) < MagManipVisuals.THROW_RANGE_SQ
                && data.perform(consumption(data), overload(data));
        block.release();
        if (thrown) {
            Vec3 aim = TargetingUtil.findImpactPoint(player, THROW_RANGE);
            block.setDeltaMovement(MagManipVisuals.throwVelocity(block.position(), aim, speed(data)));
            // Le client doit accepter cette vitesse : sans cela il la corrigerait au tick
            // suivant, et le bloc semblerait ne pas partir.
            block.hurtMarked = true;
            data.setCooldown(this, cooldown(data));
            data.addSkillExp(this, EXP_PER_THROW);
        }
        return false;
    }

    /** Une fin sans lancer — relachement trop loin, reserve vide — repose le bloc. */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        EntityMagManipBlock block = heldBlock(player, data);
        if (block != null) block.release();
    }

    // ------------------------------------------------------------------
    // Ce qu'on arrache
    // ------------------------------------------------------------------

    /** Le bloc tenu qui suit le maintien, ou {@code null} s'il a disparu. */
    private EntityMagManipBlock heldBlock(Player player, AbilityData data) {
        if (player == null) return null;
        net.minecraft.world.entity.Entity entity =
                player.level().getEntity(data.getHoldTargetId(this));
        return entity instanceof EntityMagManipBlock block ? block : null;
    }

    /** Marche le long du regard, et rend le premier bloc metallique rencontre. */
    private static BlockPos findMetalBlock(Player player) {
        Vec3 look = player.getViewVector(1f);
        BlockPos from = BlockPos.containing(player.getEyePosition(1f));
        Plotter plotter = new Plotter(from.getX(), from.getY(), from.getZ(),
                look.x, look.y, look.z);
        for (int step = 0; step < (int) GRAB_RANGE; step++) {
            BlockPos pos = plotter.next();
            if (accepts(player.level().getBlockState(pos).getBlock())) return pos;
        }
        return null;
    }
}
