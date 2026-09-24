package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original PenetrateTeleport: blinks forward through walls, ignoring obstacles. */
public class PenetrateTeleportSkill extends Skill {

    public PenetrateTeleportSkill() {
        super("penetrate_teleport", 2);
    }

    /**
     * Portee reprise de l'original : de 10 a 35 blocs selon l'experience.
     *
     * Le port se contentait de 6 blocs. Le saut se paie desormais au bloc parcouru,
     * comme chez l'original (14 a 9 par bloc).
     */
    public double range(AbilityData data) {
        return lerp(10f, 35f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 50 a 30 ticks, soit 2,5 a 1,5 seconde. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(50f, 30f, data.getSkillExp(this));
    }

    /**
     * L'original versait 0,00014 par bloc parcouru. Le paquet d'activation ne connait
     * pas la distance, donc c'est la valeur d'un saut d'une dizaine de blocs qui est
     * versee — la distance moyenne de cette competence.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.00014f * 10f;
    }

    /**
     * Rien a l'appui : c'est le saut qui se paie, au bloc parcouru.
     *
     * <p>Chez l'original le prix ne se connaissait qu'une fois la destination trouvee,
     * et il se payait <b>sans verification</b> ({@code consumeWithForce}) : un saut plus
     * long que la reserve la vidait, sans rien refuser. Le port le dit avec
     * {@link #paysOnEffect()}, comme la teleportation au marqueur.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /** C'est le saut qui paie : voir {@link #getCpCost()}. */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    /** Cout par bloc parcouru : 14 a 9, comme l'original. */
    public float cpPerBlock(AbilityData data) {
        return lerp(14f, 9f, data.getSkillExp(this));
    }

    /** Surcout repris de l'original : de 80 a 50 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(80f, 50f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Vec3 start = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        double distance = range(data);
        // Portage de consumeWithForce(distance x getConsumption(exp)) : la reserve se vide
        // au pire, elle ne refuse pas le saut.
        data.performForced(cpPerBlock(data) * (float) distance, getOverloadCost(data));
        Vec3 dest = start.add(look.scale(distance));
        player.teleportTo(dest.x, dest.y - 1.6, dest.z);
        player.fallDistance = 0;
        // Le son part au relachement dans l'original, juste avant le message d'execution :
        // c'est le meme instant.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP, 0.5f);
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
    }
}
