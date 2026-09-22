package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base des rayons miniers du meltdowner, portage de {@code MineRaysBase}.
 *
 * <p>Une competence tenue : le rayon reste ouvert tant que la touche est enfoncee, il
 * creuse le bloc qu'il vise — d'autant plus vite que le rayon est evolué — et il paie son
 * entretien par tick. La tete de minage a besoin de deux choses qui n'existent pas
 * ailleurs dans le port : le bloc vise, et ce qu'il reste a en user. Les deux vivent avec
 * le maintien ({@code AbilityData.setHoldBlock/setHoldProgress}), sinon changer de bloc
 * remettrait le minage a zero.
 *
 * <p>La durete se retire par <b>tick</b>, comme dans l'original, et non par rapport a la
 * vitesse d'une pioche : un rayon n'est pas un outil, il use le bloc a sa facon. Ce que
 * le rayon ne perce pas, une pioche ne le percerait pas non plus — c'est ce que dit
 * {@link #tier()}, transpose des niveaux de pioche de la 1.12.2 aux etiquettes de la
 * 1.20.1 : le niveau 2 perce ce qu'une pioche en fer perce, le niveau 5 perce tout.
 */
public abstract class MineRaySkill extends Skill {

    /** Durete minimale retiree par tick, pour que le rayon perce toujours quelque chose. */
    private static final float MINIMUM_SPEED = 0.05f;

    protected MineRaySkill(String name, int level) {
        super(name, level);
    }

    /** Portee du rayon, en blocs. */
    public abstract double range();

    /** Niveau de pioche que le rayon vaut, au sens de la 1.12.2 : 2 en fer, 5 tout. */
    public abstract int tier();

    /** Durete retiree par tick, de l'experience minimale a la maximale. */
    public abstract float speed(AbilityData data);

    /** Cout en CP par tick, de l'experience minimale a la maximale. */
    public abstract float cpPerTick(AbilityData data);

    /** Experience gagnee par bloc perce, comme l'original. */
    public abstract float expPerBlock();

    /** Recharge posee a la fin du rayon, de l'experience minimale a la maximale. */
    public abstract int cooldown(AbilityData data);

    @Override
    public int getCooldownTicks(AbilityData data) {
        // La recharge se pose a la fin du maintien, donc par AbilityEvents.endHeld, qui
        // lit cette methode avec le compteur du maintien encore en place.
        return cooldown(data);
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        if (!data.consumeControlPoint(cpPerTick(data))) return false;

        BlockPos target = aimedBlock(player);
        if (target == null) {
            forgetTarget(data);
            return true;
        }

        BlockPos current = data.getHoldBlock(this);
        if (current == null || !current.equals(target)) {
            // Nouveau bloc : on repart de sa durete entiere. Une durete negative (lit,
            // eau) est inperçable, comme l'original qui la remplacait par l'infini.
            float hardness = player.level().getBlockState(target).getDestroySpeed(player.level(), target);
            if (hardness < 0f || !canHarvest(player.level().getBlockState(target))) {
                forgetTarget(data);
                return true;
            }
            data.setHoldBlock(this, target);
            data.setHoldProgress(this, hardness);
            return true;
        }

        float left = data.getHoldProgress(this) - Math.max(MINIMUM_SPEED, speed(data));
        if (left > 0f) {
            data.setHoldProgress(this, left);
            return true;
        }

        breakBlock(player, data, target);
        forgetTarget(data);
        return true;
    }

    /** Oublie le bloc vise, pour que le prochain reparte de sa durete entiere. */
    private void forgetTarget(AbilityData data) {
        data.setHoldBlock(this, null);
    }

    /** Le bloc que le rayon touche, ou {@code null} si le regard part dans le vide. */
    @Nullable
    private BlockPos aimedBlock(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range()));
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    /**
     * Ce que le rayon peut percer.
     *
     * Transposition des niveaux de pioche de la 1.12.2 : l'obsidienne demande le niveau
     * plein, le fer le niveau 2, et tout ce qui ne demande rien passe toujours.
     */
    public boolean canHarvest(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return tier() >= 5;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return tier() >= 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return tier() >= 1;
        return true;
    }

    /** Casse le bloc, avec les butins de ce rayon. */
    private void breakBlock(Player player, AbilityData data, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level)) return;

        BlockState state = level.getBlockState(pos);
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5f, 1f);

        List<ItemStack> drops = drops(level, pos, state, player);
        level.removeBlock(pos, false);
        for (ItemStack drop : drops) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        }

        data.addSkillExp(this, expPerBlock());
    }

    /**
     * Les butins du bloc casse.
     *
     * Par defaut ceux de n'importe quel casseur ; le rayon de chance y ajoute la fortune
     * de l'original, en passant une pioche enchanteresse au calcul des butins — c'est
     * ainsi qu'on demande a la 1.20.1 ce que la 1.12.2 demandait avec un niveau de
     * fortune.
     */
    protected List<ItemStack> drops(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        return net.minecraft.world.level.block.Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY);
    }

    /** Une pioche enchanteresse, pour les rayons qui promettent plus de butin. */
    protected static ItemStack fortunePickaxe(int fortune) {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(Enchantments.BLOCK_FORTUNE, fortune);
        return pickaxe;
    }
}
