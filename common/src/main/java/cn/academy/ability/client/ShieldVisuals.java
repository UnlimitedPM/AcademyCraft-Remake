package cn.academy.ability.client;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Les nombres du bouclier de lumiere, portage de {@code RenderMdShield} et de son essaim.
 *
 * <p>Classe sans aucun type qui ait besoin du jeu, expres : ce sont les courbes de l'original —
 * apparition en quinze ticks, fondu en six, rotation qui accelere jusqu'a trente ticks — et
 * l'essaim de plasma qui le couvre. Les figer par un test est la seule facon de verifier qu'ils
 * ne bougent pas sans lancer un jeu.
 */
public final class ShieldVisuals {

    /** Taille du bouclier en blocs, comme {@code SIZE} de l'original. */
    public static final float SIZE = 1.8f;

    /** Distance devant le joueur ou il flotte, en blocs. */
    public static final double DISTANCE = 1.0;

    /** Hauteur au-dessus des pieds, en blocs : le {@code (0, 1.1, 0)} de l'original. */
    public static final double HEIGHT = 1.1;

    /** Ticks de l'apparition : le bouclier part petit et grossit jusqu'a sa taille. */
    private static final float GROW_TICKS = 15f;

    /** Ticks du fondu d'entree. */
    private static final float FADE_TICKS = 6f;

    /** Ticks au bout desquels la rotation atteint sa vitesse maximale. */
    private static final float SPIN_TICKS = 30f;

    /** La competence qui possede ce bouclier : celle de la meltdowner, et elle seule. */
    public static final String SKILL = "light_shield";

    // --- L'ESSAIM DE PLASMA ---

    /**
     * Une etincelle nait-elle a ce tick ?
     *
     * <p>L'original tirait un hasard a chaque tick et le comparait a 0,3 : trois ticks sur dix
     * portent donc une etincelle, et c'est ce qui fait grésiller le disque par bouffees plutot
     * que d'y coller une fumee continue.
     */
    public static final float SPARK_CHANCE = 0.3f;

    /** Le cote du cube de naissance, de part et d'autre : le {@code s = 0,5} de l'original. */
    public static final double SPARK_JITTER = 0.5;

    /** Et il nait un bloc devant les yeux : le {@code lookingPos(player, 1)} de l'original. */
    public static final double SPARK_FORWARD = 1.0;

    /**
     * Sa derive : deux centimetres au plus de cote, et de un centimetre en bas a cinq en haut.
     *
     * <p>C'est le seul essaim du meltdowner qui peut descendre — les autres montent ou derivent
     * dans n'importe quel sens — et c'est ce qui donne au bouclier son air de grésillement
     * autour du disque plutot que de fumee qui s'envole.
     */
    public static final double SPARK_DRIFT_XZ = 0.02;
    public static final double SPARK_RISE_MIN = -0.01;
    public static final double SPARK_RISE_MAX = 0.05;

    private ShieldVisuals() {}

    /**
     * Le tirage d'un tick : l'original prenait {@code nextFloat() < 0,3}.
     *
     * @param roll le hasard du tick, sur [0, 1)
     */
    public static boolean sparksThisTick(float roll) {
        return roll < SPARK_CHANCE;
    }

    /**
     * Ou nait l'etincelle du tick : devant les yeux, dans un cube de 0,5 bloc de cote.
     *
     * <p>Le point est <b>centre</b> sur le disque — les trois axes sont tires separement dans
     * le meme intervalle — et non pose sur un anneau : c'est ce qui fait que l'essaim couvre
     * aussi l'interieur du bouclier, et pas seulement son bord.
     */
    public static Vec3 sparkBorn(Vec3 eyes, Vec3 look, Random random) {
        Vec3 centre = eyes.add(look.scale(SPARK_FORWARD));
        return new Vec3(centre.x + spread(random), centre.y + spread(random),
                centre.z + spread(random));
    }

    /** Sa vitesse de depart : voir {@link #SPARK_DRIFT_XZ}. */
    public static Vec3 sparkDrift(Random random) {
        return new Vec3((random.nextDouble() * 2 - 1) * SPARK_DRIFT_XZ,
                SPARK_RISE_MIN + random.nextDouble() * (SPARK_RISE_MAX - SPARK_RISE_MIN),
                (random.nextDouble() * 2 - 1) * SPARK_DRIFT_XZ);
    }

    /** Un tirage dans [-0,5, 0,5], comme le {@code ranged(-s, s)} de l'original. */
    private static double spread(Random random) {
        return (random.nextDouble() * 2 - 1) * SPARK_JITTER;
    }

    /**
     * Ce maintien doit-il dessiner ce bouclier ?
     *
     * <p>Le port le dessinait pour n'importe quel maintien, sans jamais regarder lequel :
     * les quatorze competences tenues du mod affichaient donc le bouclier de la meltdowner
     * devant les yeux — un arc de charge, une detection de minerais ou un moteur a
     * reaction compris. Le nom de la competence est pourtant disponible juste a cote, dans
     * {@code ClientCharge}, et c'est deja lui qui choisit la boucle sonore du maintien.
     */
    public static boolean showsShield(String skill, boolean sustained) {
        return sustained && SKILL.equals(skill);
    }

    /** Taille du bouclier apres {@code ticks} ticks de maintien. */
    public static float scale(int ticks) {
        return SIZE * lerp(0.2f, 1f, ticks / GROW_TICKS);
    }

    /** Opacite du bouclier apres {@code ticks} ticks de maintien. */
    public static float alpha(int ticks) {
        return Math.min(ticks / FADE_TICKS, 1f);
    }

    /**
     * Vitesse de rotation du disque, en degres par milliseconde.
     *
     * L'original lisait le temps du jeu plutot que les ticks : sa rotation ne depend
     * donc pas de la cadence des ticks, seulement du temps qui passe.
     */
    public static float spinSpeed(int ticks) {
        return lerp(0.8f, 2f, ticks / SPIN_TICKS);
    }

    /** Interpolation bornee, la meme que celle des competences. */
    private static float lerp(float from, float to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return from + (to - from) * clamped;
    }
}
