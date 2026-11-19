package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original VecAccel: propels the player forward along their look vector. */
public class VecAccelSkill extends Skill {

    /** Le cout en CP, repris de l'original : de 120 a 80 selon l'experience. */
    private static final float CP_COST_MIN_EXP = 120f;
    private static final float CP_COST_MAX_EXP = 80f;

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
        return speedAt(data.getChargeTicks(this));
    }

    /**
     * La meme vitesse, lue sur un nombre de ticks de charge.
     *
     * <p>Le CLIENT ne connait que ce nombre-la : la charge d'une competence n'est pas
     * synchronisee, c'est son propre compteur qui la suit ({@code ClientCharge}). La parabole de
     * visee s'en sert donc, et le serveur garde {@link #speed(AbilityData)} — les deux passent par
     * ici, donc elles ne peuvent pas diverger.
     */
    public static double speedAt(int chargeTicks) {
        double prog = 0.4 + Math.min(1.0, chargeTicks / (double) MAX_CHARGE) * 0.6;
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

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return CP_COST_MIN_EXP;
    }

    /** Le cout en CP, qui suit l'experience. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Surcout repris de l'original : de 30 a 15 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(30f, 15f, data.getSkillExp(this));
    }

    /** Les dix degres de plus que l'original donnait a la visee. */
    public static final double LOOK_TILT = 10;

    /**
     * La direction de la poussee, dans le repere du monde : le regard, dix degres plus haut.
     *
     * <p>C'est le {@code EntityLook(yaw, pitch - 10).toVec3} de l'original, et ses deux
     * conventions sont gardees telles quelles : {@code pitchDegrees} est le tangage brut du jeu
     * ({@code getXRot()}, positif vers le bas), donc lui retirer dix degres fait <b>monter</b> la
     * poussee — c'est ce qui donne son arc au vol.
     *
     * <p>Pure : le serveur et le client s'en servent tous les deux, donc ils ne peuvent pas
     * diverger.
     */
    public static Vec3 boostDirection(double pitchDegrees, double yawDegrees) {
        double pitch = Math.toRadians(pitchDegrees - LOOK_TILT);
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)).normalize();
    }

    /** La vitesse posee : la direction du regard, a la vitesse de cette charge-la. */
    public static Vec3 boost(double pitchDegrees, double yawDegrees, int chargeTicks) {
        return boostDirection(pitchDegrees, yawDegrees).scale(speedAt(chargeTicks));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // La meme poussee que chez le client, posee ici aussi : c'est elle qui fait bouger le
        // joueur pour les AUTRES, et qui repond au rebond du sien si son propre crochet n'a pas
        // tourne. Voir onClientRelease pour le cote client, qui est celui de l'original.
        player.setDeltaMovement(boost(player.getXRot(), player.getYRot(), data.getChargeTicks(this)));
        player.fallDistance = 0;
        // Et la chute qui suit le lancement est gratuite : ce coup jette son porteur en l'air,
        // et il se tuait donc lui-meme en s'envoyant. Le joueur l'a demande — « j'aimerais que
        // apres avoir fait ces competences, tout comme avec la teleporteuse, on ne prenne pas de
        // degats de chute juste apres ». Voir AbilityData.protectFromFall.
        data.protectFromFall();
        player.hurtMarked = true;

        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.VECMANIP_VEC_ACCEL, 0.35f);
    }

    /**
     * Le relachement, chez le joueur : la poussee est posee ICI, comme dans l'original.
     *
     * <p>Le port la posait seulement chez le serveur, et elle y perdait la course. Un saut pile au
     * bon moment l'annulait : le paquet de vitesse arrive chez le client <b>avant</b> le tick de son
     * joueur, et le saut de ce tick-la ({@code jumpFromGround}, 0,42 en Y) ecrasait la poussee —
     * sans parler du regard, qui monte. Le joueur : « parfois, j'ai l'impression que si je saute pil
     * poil au bon moment ca annule la competence, pas litteralement mais dans le sens ou je n'ai
     * pas de boost ».
     *
     * <p>Ce crochet arrive APRES le tick du joueur (Forge tire la fin du tick client une fois le
     * monde avance), donc la poussee tient jusqu'au tick suivant — et le saut, lui, est deja passe.
     * C'est aussi ce que faisait l'original ({@code VecAccelContext.l_perform} posait la vitesse
     * chez le client, et le serveur se contentait de consommer).
     */
    @Override
    public void onClientRelease(Player player, AbilityData data, int heldTicks) {
        player.setDeltaMovement(boost(player.getXRot(), player.getYRot(), heldTicks));
    }
}
