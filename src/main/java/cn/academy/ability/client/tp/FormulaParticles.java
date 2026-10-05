package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Les particules de formule, portage de {@code CriticalHitEffect} et de {@code FormulaParticleFactory}.
 *
 * <p>C'est l'effet signature du mod : un coup critique du teleporteur fait eclore des <b>fragments de
 * formule</b> autour de sa victime — des glyphes grecs et mathematiques, tires parmi dix images. Le
 * joueur a demande ce qu'il restait de la categorie : les deux passives ne s'activent jamais, et cet
 * effet est tout ce qu'elles montrent.
 *
 * <h2>Ou ils apparaissent</h2>
 *
 * <p>De cinq a sept, semes <b>autour du corps</b> de la bete frappee : un angle tire sur le tour, une
 * distance tiree entre la moitie et les sept dixiemes de sa largeur, et une hauteur tiree entre ses
 * pieds et son sommet. Chacun part ensuite dans une direction quelconque, sur un dixieme de bloc au
 * total — ils ne voyagent pas, ils derivent.
 *
 * <h2>Leur vie, en trois temps</h2>
 *
 * <p>Comme toutes les particules de l'original : ils <b>apparaissent</b> en deux ticks, se
 * <b>tiennent</b> dix a quatorze ticks, puis s'effacent en vingt autres. Leur opacite est tiree entre
 * 152 et 255 sur 255, donc du plus pale au plus franc, et leur taille entre dix et dix-sept
 * centimetres : la meme echelle que les etincelles de la teleportation.
 *
 * <p>L'age se lit en millisecondes, comme celui du sang : rien n'a besoin d'etre ticke, une particule
 * sait se placer et s'effacer toute seule a partir de sa naissance.
 */
public final class FormulaParticles {

    /** Les dizaines d'images de la formule : dix, comme chez l'original. */
    public static final int TEXTURES = 10;

    /** L'apparition, puis l'effacement, et la tenue entre les deux. */
    public static final int FADE_IN_TICKS = 2;
    public static final int HOLD_MIN_TICKS = 10;
    public static final int HOLD_MAX_TICKS = 15;
    public static final int FADE_TICKS = 20;

    /** Sa taille, en blocs : de dix a dix-sept centimetres. */
    public static final double SIZE_MIN = 0.10;
    public static final double SIZE_MAX = 0.17;

    /** Son opacite, sur 255 : de 152 a 255, comme l'original. */
    public static final int ALPHA_MIN = 152;
    public static final int ALPHA_MAX = 255;

    /** Combien en naissent d'un coup : cinq a sept, la borne haute exclue comme chez lui. */
    public static final int COUNT_MIN = 5;
    public static final int COUNT_MAX = 8;

    /** La vitesse d'une derive, en blocs par tick. */
    public static final double DRIFT = 0.03;

    /** La part de la largeur de la bete ou ils se posent, du plus pres au plus loin. */
    public static final double RADIUS_MIN = 0.5;
    public static final double RADIUS_MAX = 0.7;

    /** La teinte de l'original : un gris tres clair, l'opacite venant de chaque particule. */
    public static final float RED = 220f / 255f;
    public static final float GREEN = 220f / 255f;
    public static final float BLUE = 220f / 255f;

    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<Glyph> LIVE = new ArrayList<>();

    private FormulaParticles() {
    }

    /** L'image d'un rang, dans le dossier de l'original. */
    public static ResourceLocation texture(int index) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/formula/" + index + ".png");
    }

    /**
     * Un glyphe vivant : d'ou il vient, ou il derive, et quand il est ne.
     *
     * <p>Tout se lit depuis la naissance, sans qu'aucun tick n'ait a le faire avancer : sa place
     * derive de sa vitesse, son opacite monte, se tient puis retombe. C'est ce qui rend la classe
     * verifiable sans monde.
     */
    public record Glyph(Vec3 origin, Vec3 velocity, int frame, double size, float startAlpha,
                        int holdTicks, long born) {

        /** Sa place, en blocs : il derive de sa vitesse, un bloc par tick. */
        public Vec3 position(long now) {
            return origin.add(velocity.scale(ageTicks(now)));
        }

        /** Son age, en ticks. */
        public double ageTicks(long now) {
            return Math.max(now - born, 0L) / 50.0;
        }

        /** Son opacite : elle monte, se tient, puis s'efface. */
        public float alpha(long now) {
            double ticks = ageTicks(now);
            if (ticks > holdTicks) {
                return (float) Math.max(0, startAlpha * (1 - (ticks - holdTicks) / FADE_TICKS));
            }
            if (ticks < FADE_IN_TICKS) {
                return (float) (startAlpha * ticks / FADE_IN_TICKS);
            }
            return startAlpha;
        }

        /** Vrai quand il a fini de s'effacer : il n'a plus rien a montrer. */
        public boolean finished(long now) {
            return ageTicks(now) >= holdTicks + FADE_TICKS;
        }
    }

    /**
     * Ou nait un glyphe : autour du corps de la bete frappee.
     *
     * <p>Fonction pure, donc verifiable : le tour se lit en sinus et cosinus, la distance en part de
     * la largeur de la creature — deja calculee par l'appelant —, et la hauteur en fraction de la
     * sienne : de ses pieds a son sommet.
     */
    public static Vec3 place(Vec3 feet, double height, double angle, double radius, double up) {
        return new Vec3(feet.x + radius * Math.sin(angle), feet.y + up * height,
                feet.z + radius * Math.cos(angle));
    }

    /**
     * La gerbe d'un coup critique.
     *
     * <p>Le serveur seul sait qui a ete touche : c'est lui qui trace le rayon, et c'est donc de lui
     * que part la gerbe. Voir {@code TeleportCritPacket}.
     */
    public static void burst(Vec3 feet, double width, double height) {
        burst(feet, width, height, Util.getMillis(), RANDOM);
    }

    /** La meme, avec un hasard et un instant donnes — pour le test. */
    public static void burst(Vec3 feet, double width, double height, long now, RandomSource random) {
        int count = COUNT_MIN + random.nextInt(COUNT_MAX - COUNT_MIN);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = width * lerp(RADIUS_MIN, RADIUS_MAX, random.nextDouble());
            double up = random.nextDouble();

            LIVE.add(new Glyph(place(feet, height, angle, radius, up),
                    randomDirection(random).scale(DRIFT),
                    random.nextInt(TEXTURES),
                    lerp(SIZE_MIN, SIZE_MAX, random.nextDouble()),
                    (ALPHA_MIN + random.nextInt(ALPHA_MAX - ALPHA_MIN + 1)) / 255f,
                    HOLD_MIN_TICKS + random.nextInt(HOLD_MAX_TICKS - HOLD_MIN_TICKS),
                    now));
        }
    }

    /** Une direction quelconque, de longueur un : la derive d'un glyphe. */
    public static Vec3 randomDirection(RandomSource random) {
        Vec3 direction = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1,
                random.nextDouble() * 2 - 1);
        // Un tirage peut tomber tout pres du centre, et une direction sans longueur ne ferait pas
        // deriver le glyphe : on retire dans ce cas-la, qui est rarissime, plutot que de laisser le
        // tirage decider si la particule bouge ou non.
        while (direction.lengthSqr() < 1.0E-6) {
            direction = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1,
                    random.nextDouble() * 2 - 1);
        }
        return direction.normalize();
    }

    /** Les glyphes encore vivants, les finis otes au passage. */
    public static List<Glyph> live() {
        long now = Util.getMillis();
        LIVE.removeIf(glyph -> glyph.finished(now));
        return LIVE;
    }

    /** Et de quoi les oublier d'un coup : le rendu s'en sert quand le monde change. */
    public static void clear() {
        LIVE.clear();
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
