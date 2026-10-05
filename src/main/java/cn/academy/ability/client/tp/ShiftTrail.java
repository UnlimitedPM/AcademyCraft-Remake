package cn.academy.ability.client.tp;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * La trainee du depose au loin, portage de la boucle de particules de {@code ShiftTeleport}.
 *
 * <p>Le geste ne deplace personne : il <b>pose</b> un bloc au loin, et frappe ce qu'il croise en
 * chemin. Son seul effet, chez l'original, etait donc cette trainee : des etincelles de
 * teleportation semees le long du trajet, du corps de celui qui lance jusqu'a la case visee. Le
 * port l'avait oubliee — voir {@code ShiftTeleportPacket}.
 *
 * <h2>Ce qu'elle suit</h2>
 *
 * <p>Le <b>trajet</b>, tel quel : une ligne droite du point de depart au <b>centre</b> de la case
 * visee, et rien d'autre. L'original partait des pieds, et meme un demi-bloc <b>sous</b> eux —
 * {@code player.posY - 0.5}, un reste du meme genre que celui des etincelles de la marque, ou il
 * prenait une position de pieds pour une hauteur d'yeux. Le port garde le nombre tel quel : c'est
 * celui qui a ete valide, et il ne se voit pas — un demi-bloc sous les pieds d'un joueur dont la
 * trainee monte vers une case plus haute.
 *
 * <h2>Le pas</h2>
 *
 * <p>Le premier grain se pose a <b>un bloc</b>, puis chaque suivant s'espace d'un tirage entre
 * <b>six dixiemes et un bloc</b>. C'est ce qui donne a la trainee son grain irregulier, et c'est
 * aussi ce qui la fait s'arreter d'elle-meme : le compte suit la distance, sans qu'aucune longueur
 * de vie n'ait a etre posee. Une portee de trente blocs seme donc entre trente et cinquante grains.
 *
 * <p>Et elle ne se dessine pas quand le geste vise ses pieds : en deca du premier pas, il n'y a
 * <b>aucun</b> grain. C'est le comportement de l'original, dont la boucle commencait a un bloc.
 *
 * <h2>Ou elle se dessine</h2>
 *
 * <p>Le serveur ne dit que <b>quand</b> et <b>ou</b> — les deux bouts —, et c'est le client qui
 * seme la trainee, comme il le fait pour les rayons du meltdowner. Chaque grain devient une
 * {@link TpParticles etincelle} ordinaire, avec sa duree de vie et son effacement.
 *
 * <p>Tout ce qui precede est pur : les nombres sont ceux de l'original, et le hasard est
 * <b>passe</b> a {@link #sparks}, ce qui rend la trainee verifiable en JUnit.
 */
public final class ShiftTrail {

    /** Le premier grain se pose a un bloc du depart, comme chez l'original. */
    public static final double FIRST_STEP = 1.0;

    /** Et les suivants s'espacent d'un tirage entre ces deux-ci. */
    public static final double STEP_MIN = 0.6;
    public static final double STEP_MAX = 1.0;

    /**
     * Le depart se prend un demi-bloc <b>sous</b> les pieds.
     *
     * <p>C'est le {@code posY - 0.5} de l'original : voir l'entete. Le port garde le nombre tel
     * quel, pour la meme raison que {@code TeleportMark.SPARK_LOW} garde le sien — c'est lui qui a
     * ete valide a l'epoque.
     */
    public static final double FEET_DROP = 0.5;

    /** La derive horizontale d'un grain, dans les deux sens. */
    public static final double DRIFT_XZ = 0.05;

    /** Et sa derive verticale, plus haute que basse : l'original, tel quel. */
    public static final double DRIFT_Y_MIN = -0.02;
    public static final double DRIFT_Y_MAX = 0.05;

    /** Un grain de la trainee : la ou il nait, et ou il derive. */
    public record Spark(Vec3 pos, Vec3 velocity) {
    }

    private ShiftTrail() {
    }

    /** Le point de depart d'une trainee : les pieds, un demi-bloc plus bas. */
    public static Vec3 start(Vec3 feet) {
        return feet.add(0, -FEET_DROP, 0);
    }

    /** Le point d'arrivee : le <b>centre</b> de la case visee, comme le faisait l'original. */
    public static Vec3 end(BlockPos cell) {
        return Vec3.atCenterOf(cell);
    }

    /**
     * Les grains d'une trainee, du depart jusqu'a la case visee.
     *
     * <p>Le grain est cale sur la <b>distance parcourue</b> et non sur un nombre d'etapes : c'est
     * ainsi que la trainee s'arrete au bout du trajet sans jamais le depasser. Le dernier grain
     * peut donc tomber a quelques centimetres de la cible, et le suivant ne serait plus dans le
     * segment.
     */
    public static List<Spark> sparks(Vec3 feet, BlockPos cell, RandomSource random) {
        Vec3 from = start(feet);
        Vec3 delta = end(cell).subtract(from);
        double distance = delta.length();

        List<Spark> out = new ArrayList<>();
        if (distance < FIRST_STEP) return out;

        Vec3 direction = delta.normalize();
        Vec3 at = from;
        double step = FIRST_STEP;
        double reached = step;
        while (reached <= distance) {
            at = at.add(direction.scale(step));
            out.add(new Spark(at, drift(random)));
            step = STEP_MIN + random.nextDouble() * (STEP_MAX - STEP_MIN);
            reached += step;
        }
        return out;
    }

    /**
     * La derive d'un grain : les trois tirages de l'original.
     *
     * <p>Plus haute que basse ({@code -0.02} a {@code 0.05}) : les etincelles montent un peu.
     */
    public static Vec3 drift(RandomSource random) {
        return new Vec3(signed(random, DRIFT_XZ),
                DRIFT_Y_MIN + random.nextDouble() * (DRIFT_Y_MAX - DRIFT_Y_MIN),
                signed(random, DRIFT_XZ));
    }

    /** Un tirage dans {@code [-limit, limit]}. */
    private static double signed(RandomSource random, double limit) {
        return -limit + random.nextDouble() * (2 * limit);
    }

    /**
     * Le geste du client : chaque grain devient une etincelle vivante.
     *
     * <p>Elle se dessine avec les autres, dans {@link TpMarkRenderer} : la trainee et les
     * etincelles de la marque partagent la meme liste et le meme rendu.
     */
    public static void play(Vec3 feet, BlockPos cell) {
        for (Spark spark : sparks(feet, cell, RandomSource.create())) {
            TpParticles.spawn(spark.pos(), spark.velocity());
        }
    }
}
