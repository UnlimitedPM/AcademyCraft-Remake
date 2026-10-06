package cn.academy.ability.client.vm;

import cn.academy.util.CubicCurve;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Les ondes de choc de vecmanip, portage de {@code WaveEffect}.
 *
 * <p>Une onde est une <b>pile d'anneaux</b> qui s'ouvre le long d'un axe : deux ou trois disques
 * de lueur, perpendiculaires a la direction du coup et espaces d'un bloc et demi, qui s'ecartent
 * en grandissant puis s'effacent en un quart de seconde. C'est l'effet du <b>choc dirige</b>, de la
 * <b>deviation</b> — chaque objet qu'elle arrete —, et du <b>renvoi</b>, qui la pose sur tout ce
 * qu'il retourne.
 *
 * <h2>Ce qui se passe dans le temps</h2>
 *
 * <p>Deux courbes, lues sur des instants differents : l'<b>echelle</b> de l'onde, qui part de
 * quatre dixiemes et grandit jusqu'a une fois et demie en une trentaine de ticks, et l'<b>opacite</b>
 * de chaque anneau, qui monte, se tient, puis retombe. Chaque anneau a sa propre vie (huit a onze
 * ticks) et son propre retard, si bien que la pile s'allume par vagues. La pile entiere
 * <b>avance</b> aussi : un quarantieme de bloc par tick.
 *
 * <p>Et l'opacite globale de l'onde prend le pas sur celle des anneaux : au bout de quinze ticks,
 * plus rien n'est dessine, quelle que soit la courbe d'un anneau. C'est ce qui fait qu'une onde
 * dure toujours le meme temps, quels que soient ses tirages.
 *
 * <h2>Les nombres</h2>
 *
 * <p>Les deux courbes sont celles de l'original, point par point, et le port les relit avec la
 * meme courbe cubique ({@link CubicCurve}). Rien ici ne se devine.
 */
public final class VecWaves {

    /** La vie d'une onde : quinze ticks, et c'est elle qui decide de tout. */
    public static final int LIFE = 15;

    /** L'ecart entre deux anneaux de la pile, et le flottement qui l'empeche d'etre regulier. */
    public static final double RING_STEP = 1.5;
    public static final double RING_JITTER = 0.3;

    /** La vie d'un anneau : de huit a onze ticks, comme les tirages de l'original. */
    public static final int RING_LIFE_MIN = 8;
    public static final int RING_LIFE_MAX = 12;

    /** L'avancee de la pile : un quarantieme de bloc par tick. */
    public static final double DRIFT_PER_TICK = 1 / 40.0;

    /** L'echelle se lit sur des ticks divises par vingt, et ne depasse jamais 1,62. */
    public static final double SIZE_DIVISOR = 20.0;
    public static final double SIZE_CEILING = 1.62;

    /** Et l'original attenuait tout de trois dixiemes au moment de dessiner. */
    public static final double DRAW_ALPHA = 0.7;

    /** L'opacite : elle monte en un cinquieme, se tient, et retombe. */
    private static final CubicCurve ALPHA = new CubicCurve()
            .add(0, 0).add(0.2, 1).add(0.5, 1).add(0.8, 1).add(1, 0);

    /** L'echelle : elle part petite, saute a huit dixiemes, puis s'ouvre lentement. */
    private static final CubicCurve SIZE = new CubicCurve()
            .add(0, 0.4).add(0.2, 0.8).add(2.5, 1.5);

    /** Un anneau de la pile : sa vie, son avance sur l'axe, sa taille, et son retard. */
    public record Ring(int life, double offset, double size, int timeOffset) {
    }

    /** Une onde vivante : ou elle se tient, de quel cote elle s'ouvre, et son age. */
    public static final class Wave {

        private final Vec3 position;
        private final float yaw;
        private final float pitch;
        private final List<Ring> rings;
        private int age;

        private Wave(Vec3 position, float yaw, float pitch, List<Ring> rings) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.rings = rings;
        }

        public Vec3 position() {
            return position;
        }

        public float yaw() {
            return yaw;
        }

        public float pitch() {
            return pitch;
        }

        public List<Ring> rings() {
            return rings;
        }

        public int age() {
            return age;
        }
    }

    private static final List<Wave> WAVES = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private VecWaves() {
    }

    /**
     * Les anneaux d'une onde, tires au sort : leur vie, leur avance et leur taille.
     *
     * <p>Le <b>premier</b> anneau se tient a l'origine, le deuxieme un bloc et demi devant, et
     * chacun se decale un peu — c'est ce qui donne a la pile son epaisseur.
     */
    public static List<Ring> rings(RandomSource random, int count, double size) {
        List<Ring> out = new ArrayList<>(count);
        for (int idx = 0; idx < count; idx++) {
            out.add(new Ring(
                    RING_LIFE_MIN + random.nextInt(RING_LIFE_MAX - RING_LIFE_MIN),
                    idx * RING_STEP + ranged(random, -RING_JITTER, RING_JITTER),
                    size * ranged(random, 0.8, 1.2),
                    idx * 2 + (-1 + random.nextInt(2))));
        }
        return List.copyOf(out);
    }

    /**
     * L'age d'une onde a cette image : ses ticks, et la part du tick en cours.
     *
     * <p>Sans elle, tout ce qui vieillit ici — l'echelle, l'opacite, l'avancee — ne changeait
     * qu'une fois par tick : l'onde s'animait donc a <b>vingt images par seconde</b>, et cela se
     * voit sur une lueur qui s'efface. Le joueur l'a vu : « l'animation ne va qu'a 20 fps ». C'est
     * la meme lecon que la poussiere des ailes et la fumee du choc au sol — ce qui vit entre deux
     * ticks se relit entre deux ticks. Voir {@code WaveRenderer}.
     */
    public static double ageAt(Wave wave, float partialTick) {
        return wave.age() + partialTick;
    }

    /** L'opacite globale de l'onde, qui prend le pas sur celle de ses anneaux. */
    public static double maxAlpha(double ticks) {
        return clamp01(ALPHA.valueAt(ticks / (double) LIFE));
    }

    /** Et celle d'un anneau, qui a sa propre vie et son propre retard. */
    public static double ringAlpha(double ticks, Ring ring) {
        return clamp01(ALPHA.valueAt((ticks - ring.timeOffset()) / (double) ring.life()));
    }

    /** Ce qui se dessine vraiment : la plus petite des deux, attenuee de trois dixiemes. */
    public static double alpha(double ticks, Ring ring) {
        return Math.min(maxAlpha(ticks), ringAlpha(ticks, ring)) * DRAW_ALPHA;
    }

    /** L'echelle de l'onde a cet age-la : ses anneaux grandissent tous ensemble. */
    public static double sizeScale(double ticks) {
        return SIZE.valueAt(clamp(ticks / SIZE_DIVISOR, 0, SIZE_CEILING));
    }

    /** L'avancee de la pile, en blocs. */
    public static double drift(double ticks) {
        return ticks * DRIFT_PER_TICK;
    }

    /** Une onde de plus : c'est le client qui la seme, avec son propre hasard. */
    public static void play(Vec3 position, float yaw, float pitch, int count, double size) {
        WAVES.add(new Wave(position, yaw, pitch, rings(RANDOM, count, size)));
    }

    /** Un tick du client : les ondes vieillissent, et les finies s'en vont. */
    public static void tick() {
        for (int i = WAVES.size() - 1; i >= 0; i--) {
            Wave wave = WAVES.get(i);
            if (++wave.age >= LIFE) {
                WAVES.remove(i);
            }
        }
    }

    /** Les ondes vivantes, a dessiner. */
    public static List<Wave> live() {
        return WAVES;
    }

    /** Tout oublier : la deconnexion d'un monde n'est pas une fin de competence. */
    public static void clear() {
        WAVES.clear();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return clamp(value, 0, 1);
    }

    private static double ranged(RandomSource random, double from, double to) {
        return from + random.nextDouble() * (to - from);
    }
}
