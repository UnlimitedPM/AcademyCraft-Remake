package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original ShiftTeleport: blinks toward where the player looks, stopping at any obstacle. */
public class ShiftTeleportSkill extends Skill {

    /** Le prix du depose : 260 a 320 CP, comme l'original, et il monte avec l'experience. */
    private static final float CP_COST_MIN_EXP = 260f;
    private static final float CP_COST_MAX_EXP = 320f;

    public ShiftTeleportSkill() {
        super("shift_tp", 4);
    }

    /**
     * Portee reprise de l'original : de 25 a 35 blocs selon l'experience.
     *
     * L'original blessait en plus les creatures traversees (15 a 35 degats) ; le port
     * ne fait que deplacer le joueur, donc cette part n'a pas ete portee. Le cout en
     * CP reste celui du port.
     */
    public double maxRange(AbilityData data) {
        return lerp(25f, 35f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 100 a 60 ticks, soit 5 a 3 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(100f, 60f, data.getSkillExp(this));
    }

    /**
     * L'original versait 0,002 par entite traversee, plus 0,002. Le port ne compte pas
     * les entites traversees : c'est donc le montant de base qui est verse.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.002f;
    }

    @Override
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return CP_COST_MIN_EXP;
    }

    /** Surcout repris de l'original : de 40 a 30 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(40f, 30f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Vec3 destination = destination(player, data);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0;
        // L'original le jouait au dernier moment, et seulement si sa ligne avait trouve
        // quelqu'un : le port, qui ne fait pas ce coup au passage, le joue toujours.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP_SHIFT, 0.5f);
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
    }

    /**
     * Ou le saut deposerait son joueur, sans le deplacer.
     *
     * <p>Calculee a part pour la meme raison que chez les trois autres competences a marque : le
     * fantome du client doit se poser <b>la ou le joueur arrivera</b>, et il ne peut le savoir
     * qu'en appelant la meme fonction. Une seule geometrie, donc, et la marque ne peut pas mentir
     * sur l'endroit ou l'on atterrit.
     *
     * <p>Trois cas, ceux de l'original : rien devant, et l'on va jusqu'au bout de la portee ;
     * le sol, et l'on se pose dessus ; un mur ou un plafond, et l'on s'arrete juste avant, a
     * hauteur d'yeux pour un plafond, a la hauteur actuelle pour un mur.
     */
    public Vec3 destination(Player player, AbilityData data) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(maxRange(data)));
        double eyeHeight = player.getEyeHeight();

        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() == HitResult.Type.MISS) {
            // Rien devant, sur toute la portee : on prend la distance entiere.
            return new Vec3(end.x, end.y - eyeHeight, end.z);
        }

        Vec3 hitLoc = hit.getLocation();
        Direction face = ((BlockHitResult) hit).getDirection();
        if (face == Direction.UP) {
            // On vise le sol : on se pose exactement sur le bloc vise.
            return hitLoc;
        }
        Vec3 backed = hitLoc.subtract(look.scale(0.5));
        if (face == Direction.DOWN) {
            // On vise un plafond : on recule d'un demi-bloc et l'on garde la hauteur d'yeux.
            return new Vec3(backed.x, backed.y - eyeHeight, backed.z);
        }
        // Un mur : on s'arrete juste avant, sans changer de hauteur.
        return new Vec3(backed.x, player.getY(), backed.z);
    }
}
