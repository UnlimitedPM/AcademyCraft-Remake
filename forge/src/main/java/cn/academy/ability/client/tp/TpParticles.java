package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les etincelles de la teleportation, portage de {@code TPParticleFactory}.
 *
 * <p>Une seule particule pour toute la categorie : elle sert a la marque de teleportation, qui en
 * lache en continu, et a la trainee du saut, qui en seme le long du trajet. Sa texture est
 * {@code effects/tp_particle} — un carre pale — et sa taille va de dix a vingt centimetres, donc
 * elle se voit, la ou celle du plasma n'est qu'un point.
 *
 * <h2>Sa vie, en trois temps</h2>
 *
 * <p>Comme toutes les particules de l'original : elle <b>apparait</b> en cinq ticks, son opacite
 * montant de zero a la sienne, qui est tiree entre 153 et 204 sur 255 — donc franchement opaque.
 * Elle <b>se tient</b> ensuite vingt ticks, puis <b>s'efface</b> en vingt autres. Sa vie totale
 * est donc de quarante ticks, deux secondes : c'est la plus longue du port, et c'est voulu, un
 * fantome de teleportation doit avoir le temps de se dissiper.
 *
 * <p>Elle ne tombe pas et ne ralentit pas : l'original ne lui donnait ni gravite ni trainee, et
 * ses collisions de bloc ne se voient pas — elle s'eteint avant d'avoir bouge d'un demi-bloc.
 */
public final class TpParticles {

    /** Le temps d'apparition, puis celui de l'effacement, et le temps plein entre les deux. */
    public static final int FADE_IN_TICKS = 5;
    public static final int LIFE_TICKS = 20;
    public static final int FADE_TICKS = 20;

    /** Son opacite, sur 255 : de 153 a 204, comme l'original. */
    public static final int ALPHA_MIN = 153;
    public static final int ALPHA_MAX = 204;

    /** Sa taille, en blocs : de dix a vingt centimetres. */
    public static final double SIZE_MIN = 0.1;
    public static final double SIZE_MAX = 0.2;

    /** L'image de l'original, tel quel. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/tp_particle.png");

    /** Une etincelle vivante. */
    public static final class Spark {

        private final double[] pos;
        private final double[] vel;
        private final long birthMs;
        private final float startAlpha;
        private final float size;

        Spark(double[] pos, double[] vel, long birthMs, float startAlpha, float size) {
            this.pos = pos;
            this.vel = vel;
            this.birthMs = birthMs;
            this.startAlpha = startAlpha;
            this.size = size;
        }

        public double[] pos() {
            return pos;
        }

        public float size() {
            return size;
        }

        /** Son age, en ticks. */
        public double ageTicks(long nowMs) {
            return (nowMs - birthMs) / 50.0;
        }

        /** Son opacite : elle monte, se tient, puis s'efface — deux secondes en tout. */
        public float alpha(long nowMs) {
            double ticks = ageTicks(nowMs);
            if (ticks > LIFE_TICKS) {
                return (float) Math.max(0, startAlpha * (1 - (ticks - LIFE_TICKS) / FADE_TICKS));
            }
            if (ticks < FADE_IN_TICKS) {
                return (float) (startAlpha * ticks / FADE_IN_TICKS);
            }
            return startAlpha;
        }

        boolean dead(long nowMs) {
            return ageTicks(nowMs) >= LIFE_TICKS + FADE_TICKS;
        }

        /** Un tick : elle avance de sa vitesse, sans poids ni trainee. */
        void advance() {
            pos[0] += vel[0];
            pos[1] += vel[1];
            pos[2] += vel[2];
        }
    }

    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private TpParticles() {
    }

    /** Une etincelle, aux nombres de l'original. */
    public static void spawn(Vec3 pos, Vec3 vel) {
        spawn(pos, vel, Util.getMillis(), RANDOM);
    }

    /** La meme, avec un hasard et un instant donnes — pour le test. */
    public static void spawn(Vec3 pos, Vec3 vel, long nowMs, Random random) {
        int alpha = ALPHA_MIN + random.nextInt(ALPHA_MAX - ALPHA_MIN);
        double size = SIZE_MIN + random.nextDouble() * (SIZE_MAX - SIZE_MIN);

        SPARKS.add(new Spark(new double[] { pos.x, pos.y, pos.z },
                new double[] { vel.x, vel.y, vel.z }, nowMs, alpha / 255f, (float) size));
    }

    /** Un tick du client : les etincelles avancent, et les mortes s'en vont. */
    public static void tick() {
        tick(Util.getMillis());
    }

    public static void tick(long nowMs) {
        for (int i = SPARKS.size() - 1; i >= 0; i--) {
            Spark spark = SPARKS.get(i);
            if (spark.dead(nowMs)) {
                SPARKS.remove(i);
            } else {
                spark.advance();
            }
        }
    }

    /** Les etincelles vivantes, a dessiner. */
    public static List<Spark> live() {
        return SPARKS;
    }

    /** Tout oublier : la deconnexion d'un monde n'est pas une fin de competence. */
    public static void clear() {
        SPARKS.clear();
    }
}
