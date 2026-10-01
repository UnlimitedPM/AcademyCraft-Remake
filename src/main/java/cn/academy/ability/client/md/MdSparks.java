package cn.academy.ability.client.md;

import cn.academy.AcademyCraft;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les etincelles du plasma, portage de {@code MdParticleFactory} et de son {@code Particle}.
 *
 * <p>C'est la <b>seule</b> particule du meltdowner : elle sert a tout — le rayon de la bille,
 * l'essaim du bouclier, la marque de radiation, la charge du rayon, les rayons miniers et le
 * reacteur. L'original en faisait une entite, comme ses eclairs ; le port tient ses etincelles
 * dans une liste, et le rendu les lit.
 *
 * <h2>Sa vie, en trois temps</h2>
 *
 * <p>Elle <b>apparait</b> : pendant cinq ticks, son opacite monte de zero a la sienne, qui est
 * tiree entre 76 et 151 sur 255 — donc une etincelle assez pale. Elle <b>se tient</b> ensuite
 * pendant toute sa vie, qui est tiree entre 25 et 54 ticks. Puis elle <b>s'efface</b> en vingt
 * ticks, et meurt a la fin de cet effacement.
 *
 * <p>C'est bien « apres » et non « pendant », et ce n'est pas un detail : la vie de l'original
 * ne comptait que le temps plein — {@code fadeAfter(life, fadeTime)} — donc une etincelle de
 * 25 ticks vit en realite 45 ticks, deux secondes et quart. Elle traine donc bien plus
 * longtemps qu'elle n'en a l'air, et c'est ce qui donne au plasma sa fumee verte.
 *
 * <h2>Elle ne tombe pas</h2>
 *
 * <p>Sa vitesse lui est donnee au depart — un centieme de bloc par tick sur chaque axe, chez
 * le rayon — et elle la garde : la bille de l'original n'avait <b>ni gravite ni trainee</b>. Une
 * etincelle derive donc droit devant elle, et s'eteint avant d'avoir bouge d'un bloc.
 */
public final class MdSparks {

    /** La vie d'une etincelle avant son effacement : de 25 a 54 ticks. */
    public static final int LIFE_MIN_TICKS = 25;
    public static final int LIFE_MAX_TICKS = 55;

    /** Le temps d'apparition, puis celui de l'effacement, en ticks. */
    public static final int FADE_IN_TICKS = 5;
    public static final int FADE_TICKS = 20;

    /** Son opacite, sur 255 : de 76 a 151. */
    public static final int ALPHA_MIN = 76;
    public static final int ALPHA_MAX = 152;

    /** Sa taille, en blocs : 5 a 7 centimetres. */
    public static final double SIZE_MIN = 0.05;
    public static final double SIZE_MAX = 0.07;

    /** L'image de la bille de plasma, celle de toutes les etincelles du plasma. */
    public static final ResourceLocation PLAIN = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/md_particle.png");

    /**
     * Et l'etoile de la chance, que l'original ne donnait qu'a un seul rayon.
     *
     * <p>{@code EntityMineRayLuck} remplacait la texture de ses propres etincelles par
     * {@code md_particle_luck}, une etoile a quatre branches. C'est la seule entite de tout le
     * plasma a le faire : les deux autres rayons miniers crachaient la bille ordinaire.
     */
    public static final ResourceLocation LUCK = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/md_particle_luck.png");

    /** Une etincelle vivante. */
    public static final class Spark {

        private final double[] pos;
        private final double[] vel;
        private final long birthMs;
        private final int life;
        private final float startAlpha;
        private final float size;
        private final double gravity;
        private final ResourceLocation texture;

        Spark(double[] pos, double[] vel, long birthMs, int life, float startAlpha, float size,
              double gravity, ResourceLocation texture) {
            this.pos = pos;
            this.vel = vel;
            this.birthMs = birthMs;
            this.life = life;
            this.startAlpha = startAlpha;
            this.size = size;
            this.gravity = gravity;
            this.texture = texture;
        }

        public double[] pos() {
            return pos;
        }

        public float size() {
            return size;
        }

        /** L'image avec laquelle elle se dessine : la bille de plasma, ou l'etoile de la chance. */
        public ResourceLocation texture() {
            return texture;
        }

        /** Son age, en ticks. */
        public double ageTicks(long nowMs) {
            return (nowMs - birthMs) / 50.0;
        }

        /** Son opacite : elle monte, se tient, puis s'efface. */
        public float alpha(long nowMs) {
            double ticks = ageTicks(nowMs);
            if (ticks > life) {
                return (float) Math.max(0, startAlpha * (1 - (ticks - life) / FADE_TICKS));
            }
            if (ticks < FADE_IN_TICKS) {
                return (float) (startAlpha * ticks / FADE_IN_TICKS);
            }
            return startAlpha;
        }

        boolean dead(long nowMs) {
            return ageTicks(nowMs) >= life + FADE_TICKS;
        }

        /** Un tick : elle avance de sa vitesse, sans trainee — mais pas toujours sans poids. */
        void advance() {
            // Le poids, quand il y en a un : la {@code Rigidbody} de l'original, dont la gravite
            // par defaut vaut zero et que seules les etincelles de BLOC reglaient a 0,01. Elles
            // tombent donc lentement, pendant que celles du rayon continuent droit devant elles.
            if (gravity > 0) vel[1] -= gravity;
            pos[0] += vel[0];
            pos[1] += vel[1];
            pos[2] += vel[2];
        }
    }

    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private MdSparks() {
    }

    /** Une etincelle, aux nombres de l'original. */
    public static void spawn(Vec3 pos, Vec3 vel) {
        spawn(pos, vel, 0.0, MdSparks.PLAIN, Util.getMillis(), RANDOM);
    }

    /**
     * La meme, avec un poids et une image.
     *
     * <p>Le poids est celui de la {@code Rigidbody} de l'original, en blocs par tick au carre :
     * zero partout chez lui, 0,01 pour les trois etincelles du bloc mine — celles-la tombent.
     */
    public static void spawn(Vec3 pos, Vec3 vel, double gravity, ResourceLocation texture) {
        spawn(pos, vel, gravity, texture, Util.getMillis(), RANDOM);
    }

    /** La meme, avec un hasard et un instant donnes — pour le test. */
    public static void spawn(Vec3 pos, Vec3 vel, long nowMs, Random random) {
        spawn(pos, vel, 0.0, MdSparks.PLAIN, nowMs, random);
    }

    /** Toutes les memes, jusqu'au bout : c'est ici que vivent les nombres de l'original. */
    public static void spawn(Vec3 pos, Vec3 vel, double gravity, ResourceLocation texture,
                             long nowMs, Random random) {
        int life = LIFE_MIN_TICKS + random.nextInt(LIFE_MAX_TICKS - LIFE_MIN_TICKS);
        int alpha = ALPHA_MIN + random.nextInt(ALPHA_MAX - ALPHA_MIN);
        double size = SIZE_MIN + random.nextDouble() * (SIZE_MAX - SIZE_MIN);

        SPARKS.add(new Spark(new double[] { pos.x, pos.y, pos.z },
                new double[] { vel.x, vel.y, vel.z }, nowMs, life, alpha / 255f, (float) size,
                gravity, texture));
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
