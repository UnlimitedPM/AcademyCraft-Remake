package cn.academy.ability.client.md;

import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les rayons du meltdowner, chez le client.
 *
 * <p>L'original en faisait des <b>entites</b> : le serveur les posait, un paquet les annoncait,
 * et chacune vivait sa vie avant de disparaitre. Le port n'a pas d'entites d'effet — il tient
 * ses eclairs dans des listes, comme {@code ClientArcs} le fait pour l'electromaster — donc les
 * rayons vivent ici, et le rendu les lit.
 *
 * <p>Tout ce qui fait la <b>forme</b> d'un rayon est ici, et rien d'autre : sa longueur qui
 * pousse dans les deux premiers dixiemes de seconde, sa largeur et son opacite qui s'effondrent
 * a la fin. L'original comptait ces trois choses en millisecondes — son horloge
 * {@code GameTimer} etait une horloge a la seconde, multipliee par mille a l'entree de chaque
 * calcul — et c'est pourquoi les courbes ci-dessous parlent en millisecondes elles aussi.
 *
 * <p>Le temps est <b>donne</b> a chaque appel plutot que lu ici : c'est ce qui permet de
 * derouler la vie entiere d'un rayon dans un test, sans lancer un jeu.
 */
public final class MdRays {

    /** Le tremblement de la lueur : un dixieme au plus, et 0,4 de vitesse par seconde. */
    public static final double GLOW_WIGGLE_RADIUS = 0.1;
    public static final double MAX_GLOW_WIGGLE_SPEED = 0.4;

    /** Les etincelles d'un rayon : quelque part entre son depart et dix blocs, au hasard. */
    public static final double SPARK_DISTANCE = 10;
    public static final double SPARK_SPEED = 0.015;

    /**
     * Un rayon vivant.
     *
     * <p>Ses deux bouts sont figes — un rayon du meltdowner ne poursuit rien — et sa longueur
     * totale en decoule. Ce qui bouge, c'est ce qu'on en dessine : {@link #drawnLength} le fait
     * pousser depuis son depart, {@link #alpha} l'efface a la fin.
     */
    public static final class LiveRay {

        private final MdRayKind kind;
        private final double[] from;
        private final double[] to;
        private final long birthMs;
        private final double length;

        /** Le tremblement de la lueur, avance par le rendu, a chaque image. */
        private double glowWiggle;

        LiveRay(MdRayKind kind, double[] from, double[] to, long birthMs) {
            this.kind = kind;
            this.from = from;
            this.to = to;
            this.birthMs = birthMs;
            this.length = Math.sqrt(sqr(to[0] - from[0]) + sqr(to[1] - from[1]) + sqr(to[2] - from[2]));
        }

        public MdRayKind kind() {
            return kind;
        }

        public double[] from() {
            return from;
        }

        public double[] to() {
            return to;
        }

        public long birthMs() {
            return birthMs;
        }

        /** La longueur du segment vise, en blocs. */
        public double length() {
            return length;
        }

        /** L'age du rayon, en millisecondes. */
        public long ageMs(long nowMs) {
            return nowMs - birthMs;
        }

        /**
         * La longueur dessinee : le rayon <b>pousse</b> depuis son depart sur les deux premiers
         * dixiemes de seconde, comme la {@code getLength()} de l'original.
         */
        public double drawnLength(long nowMs) {
            long age = ageMs(nowMs);
            return (age < kind.blendInMs() ? (double) age / kind.blendInMs() : 1.0) * length;
        }

        /**
         * La part de largeur restante : elle s'effondre sur les derniers dixiemes de seconde.
         *
         * <p>C'est la {@code getWidth()} du petit rayon, qui efface sa largeur plus tot que la
         * base ne le faisait — cinq dixiemes de seconde au lieu de trois.
         */
        public double widthFactor(long nowMs) {
            long age = ageMs(nowMs);
            long shrink = kind.lifeMs() - kind.shrinkMs();
            return age > shrink ? Math.max(0.0, 1.0 - (double) (age - shrink) / kind.shrinkMs()) : 1.0;
        }

        /** L'opacite du rayon : pleine, puis effacee sur les derniers quatre dixiemes. */
        public float alpha(long nowMs) {
            long age = ageMs(nowMs);
            long fade = kind.lifeMs() - kind.blendOutMs();
            return age > fade
                    ? (float) Math.max(0.0, 1.0 - (double) (age - fade) / kind.blendOutMs())
                    : 1f;
        }

        /**
         * L'opacite de la lueur : celle de l'original, qui multipliait <b>encore</b> par celle du
         * rayon — et la faisait trembler entre 0,9 et 1.
         */
        public double glowAlpha(long nowMs) {
            return (1 - GLOW_WIGGLE_RADIUS + glowWiggle) * alpha(nowMs);
        }

        /** Le tremblement courant de la lueur, sur [0, 0,1]. */
        public double glowWiggle() {
            return glowWiggle;
        }

        /** Un pas de tremblement, en millisecondes : une marche au hasard, comme l'original. */
        void advanceWiggle(long deltaMs, Random random) {
            glowWiggle += deltaMs * (random.nextDouble() * 2 - 1) * MAX_GLOW_WIGGLE_SPEED / 1000.0;
            glowWiggle = Math.min(GLOW_WIGGLE_RADIUS, Math.max(0, glowWiggle));
        }

        /** Le rayon a-t-il vecu sa vie ? */
        boolean dead(long nowMs) {
            return ageMs(nowMs) >= kind.lifeMs();
        }

        /** Une etincelle, posee au hasard entre le depart et dix blocs plus loin. */
        void sowSpark() {
            double along = RANDOM.nextDouble() * SPARK_DISTANCE / length;
            Vec3 pos = new Vec3(from[0] + (to[0] - from[0]) * along,
                    from[1] + (to[1] - from[1]) * along,
                    from[2] + (to[2] - from[2]) * along);
            MdSparks.spawn(pos, new Vec3(spread(), spread(), spread()));
        }

        private static double spread() {
            return (RANDOM.nextDouble() * 2 - 1) * SPARK_SPEED;
        }
    }

    private static final List<LiveRay> RAYS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    /** L'instant de l'image precedente, pour le tremblement. */
    private static long lastFrameMs;

    private MdRays() {
    }

    /** Ouvre un rayon entre deux points du monde. */
    public static void spawn(MdRayKind kind, Vec3 from, Vec3 to) {
        spawn(kind, from, to, Util.getMillis());
    }

    /** Le meme, a un instant donne — pour le test, qui deroule sa vie sans horloge. */
    public static void spawn(MdRayKind kind, Vec3 from, Vec3 to, long nowMs) {
        if (kind.isBarrage()) {
            spawnBarrage(from, to, nowMs);
            return;
        }
        RAYS.add(new LiveRay(kind, new double[] { from.x, from.y, from.z },
                new double[] { to.x, to.y, to.z }, nowMs));
    }

    /**
     * La salve : une gerbe de traits, tires au hasard autour de la direction visee.
     *
     * <p>C'est une seule chose pour le serveur — un paquet, un point de depart, une direction —
     * et vingt-cinq a trente rayons pour le client, comme l'original dont l'entite portait ses
     * sous-rayons et se dessinait une fois par sous-rayon. Les decalages sont tires ici et jamais
     * ailleurs : c'est ce qui permet au serveur de n'envoyer qu'un mot.
     */
    private static void spawnBarrage(Vec3 from, Vec3 to, long nowMs) {
        Vec3 axis = to.subtract(from);
        if (axis.lengthSqr() < 1.0E-6) return;
        Vec3 look = axis.normalize();

        double spread = MdBarrage.spread(RANDOM);
        int count = MdBarrage.subCount(RANDOM);
        for (int i = 0; i < count; i++) {
            Vec3 direction = MdBarrage.direction(look,
                    MdBarrage.yawOffset(spread, RANDOM), MdBarrage.pitchOffset(spread, RANDOM));
            Vec3 end = from.add(direction.scale(MdBarrage.RAY_LENGTH));
            RAYS.add(new LiveRay(MdRayKind.BARRAGE,
                    new double[] { from.x, from.y, from.z },
                    new double[] { end.x, end.y, end.z }, nowMs));
        }
    }

    /** Un tick du client : les etincelles des rayons, puis les rayons morts s'en vont. */
    public static void tick() {
        tick(Util.getMillis());
    }

    public static void tick(long nowMs) {
        for (int i = RAYS.size() - 1; i >= 0; i--) {
            LiveRay ray = RAYS.get(i);
            if (ray.dead(nowMs)) {
                RAYS.remove(i);
                continue;
            }
            // Le rayon crache ses etincelles : c'est ce que faisait l'{@code onUpdate} de
            // {@code EntityMdRaySmall}, une par tick, posee au hasard le long de lui-meme.
            if (ray.kind().sparkRate() > 0 && RANDOM.nextDouble() < ray.kind().sparkRate()) {
                ray.sowSpark();
            }
        }
    }

    /**
     * Un pas d'image : le tremblement des lueurs.
     *
     * <p>Il se fait ici et non au tick parce que l'original l'avançait dans son
     * {@code onRenderTick} : une lueur tremble donc a la vitesse de l'ecran, ce qui ne se voit
     * pas au tick. Voir {@code EntityRayBase.onRenderTick}.
     */
    public static void advanceFrame(long nowMs) {
        long delta = lastFrameMs == 0 ? 0 : nowMs - lastFrameMs;
        lastFrameMs = nowMs;
        for (LiveRay ray : RAYS) {
            ray.advanceWiggle(delta, RANDOM);
        }
    }

    /** Les rayons vivants, a dessiner. */
    public static List<LiveRay> live() {
        return RAYS;
    }

    /** Tout oublier : la deconnexion d'un monde n'est pas une fin de competence. */
    public static void clear() {
        RAYS.clear();
        lastFrameMs = 0;
    }

    private static double sqr(double value) {
        return value * value;
    }
}
