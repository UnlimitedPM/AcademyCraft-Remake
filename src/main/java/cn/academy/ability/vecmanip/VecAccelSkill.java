package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original VecAccel: propels the player forward along their look vector. */
public class VecAccelSkill extends Skill {

    private static final float CP_COST = 15f;

    /** Vitesse maximale de l'original, atteinte a pleine charge. */
    public static final double MAX_VELOCITY = 2.5;

    /** Duree de charge au-dela de laquelle la poussee ne monte plus : {@code MAX_CHARGE}. */
    public static final int MAX_CHARGE = 20;

    public VecAccelSkill() {
        super("vec_accel", 2);
    }

    /**
     * L'acceleration de vecteur se chargeait dans l'original : un appui bref pousse
     * faiblement, une touche tenue pousse de tout son poids. Relacher reste toujours
     * possible, meme tout de suite.
     */
    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return MAX_CHARGE;
    }

    /**
     * Vitesse de lancement, portage de {@code speed} de l'original :
     * {@code sin(prog) * MAX_VELOCITY}, avec {@code prog} allant de 0,4 a 1 selon la
     * charge. Un appui bref pousse donc a 0,97 bloc par tick, une charge pleine a 2,10 —
     * soit moins que les 2,2 que le port appliquait sans charge.
     */
    public double speed(AbilityData data) {
        double prog = lerp(0.4f, 1f, (float) Math.min(1.0, data.getChargeTicks(this) / (double) MAX_CHARGE));
        return Math.sin(prog) * MAX_VELOCITY;
    }

    /** 0,002 par acceleration, comme dans l'original. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.002f;
    }

    /** Recharge reprise de l'original : de 80 a 50 ticks, soit 4 a 2,5 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(80f, 50f, data.getSkillExp(this));
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    /** Surcout repris de l'original : de 30 a 15 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(30f, 15f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // Look slightly upward like the original (pitch - 10) so the arc carries the player forward.
        double pitch = Math.toRadians(player.getXRot() - 10);
        double yaw = Math.toRadians(player.getYRot());
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);

        // Original replaces the velocity outright rather than stacking onto existing motion.
        player.setDeltaMovement(new Vec3(x, y, z).normalize().scale(speed(data)));
        player.fallDistance = 0;
        player.hurtMarked = true;

        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.VECMANIP_VEC_ACCEL, 0.35f);
    }
}
