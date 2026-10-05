package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La poussiere des ailes de tempete : les grains qui tournent autour du vol.
 *
 * <p>L'original en semait <b>douze par tick</b> de client, tant que ses ailes vivaient — deux cent
 * quarante par seconde, une poussiere dense — et c'est tout ce qui manquait a ses ailes : le joueur
 * a reconnu leur absence tout de suite.
 *
 * <h2>Ou ils naissent, et comment ils tournent</h2>
 *
 * <p>Sur une <b>coquille</b> de trois a huit blocs autour du porteur : la direction est tiree
 * uniformement sur la sphere (un azimut, une elevation, un rayon), donc le grain peut apparaitre
 * derriere, devant, au-dessus ou sous lui. Sa vitesse, elle, est <b>tangentielle</b> : sept
 * dixiemes de bloc par tick, perpendiculaires au rayon qui l'a fait naitre — d'ou le mouvement
 * circulaire, et d'ou l'essaim au lieu d'une pluie. Un souffle vertical minuscule (−0,01 a 0,05)
 * l'emporte un peu, et une pesanteur de deux centiemes le fait descendre a peine.
 *
 * <p>C'est ce qui distingue ces grains de la poussiere de bloc de vanilla : l'original la prenait
 * telle quelle — une poussiere de <b>terre</b>, mise a la moitie de sa taille — mais lui retirait
 * presque tout son poids. Des grains qui tombent ne tournent pas.
 *
 * <h2>Ce que le port remplace</h2>
 *
 * <p>La poussiere de bloc de vanilla n'est pas dessinable telle quelle, et le port n'a pas de
 * texture de bloc dans ses effets : le grain est donc un rond de lueur, teinte de la couleur de la
 * terre. La forme change, pas le mouvement — et c'est le mouvement que le joueur regarde.
 */
@OnlyIn(Dist.CLIENT)
public final class WingDust {

    /** Douze grains par tick, comme l'original. */
    public static final int PER_TICK = 12;

    /** La coquille ou ils naissent : de trois a huit blocs du porteur. */
    public static final double RADIUS_MIN = 3.0;
    public static final double RADIUS_MAX = 8.0;

    /** Leur vitesse tangentielle, et le souffle vertical qui l'accompagne. */
    public static final double TANGENT = 0.7;
    public static final double LIFT_MIN = -0.01;
    public static final double LIFT_MAX = 0.05;

    /** Leur poids : deux centiemes, contre un pour la poussiere de bloc. */
    public static final double GRAVITY = 0.02;

    /** Leur taille : un dixieme de bloc, la moitie d'une poussiere de bloc. */
    public static final double SIZE = 0.1;

    /** Leur vie : de vingt a quarante ticks, puis huit ticks d'effacement. */
    public static final int LIFE_MIN = 20;
    public static final int LIFE_MAX = 40;
    public static final int FADE_TICKS = 8;

    /** Et trois ticks pour apparaitre, sans quoi ils surgiraient d'un coup. */
    public static final int FADE_IN_TICKS = 3;

    /**
     * Leur opacite au plus fort, et la teinte de la terre.
     *
     * <p>L'original les prenait opaques — c'etait une poussiere de bloc, et non un rond de lueur —
     * donc le port demande un peu plus que la moitie pour que la poussiere se voie autant que la
     * sienne.
     */
    public static final float ALPHA = 0.5f;
    public static final float RED = 0.45f;
    public static final float GREEN = 0.34f;
    public static final float BLUE = 0.24f;

    /** L'image du grain : un rond de lueur, faute de poussiere de bloc. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/glow_circle.png");

    /** Un grain vivant : ou il est, ou il va, et son age. */
    public static final class Grain {

        private final double[] pos;
        private final double[] vel;
        private final int life;
        private int age;

        Grain(double[] pos, double[] vel, int life) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
        }

        public Vec3 pos() {
            return new Vec3(pos[0], pos[1], pos[2]);
        }

        /**
         * Son opacite : elle monte en trois ticks, se tient pendant sa vie, puis s'efface.
         *
         * <p>C'est la vie entiere du grain qui se lit ici, et non un compte a rebours separe : le
         * rendu n'a donc rien a tenir, et l'effacement commence apres la vie, comme chez l'original.
         */
        public float alpha() {
            if (age < FADE_IN_TICKS) {
                return ALPHA * age / (float) FADE_IN_TICKS;
            }
            if (age > life) {
                return ALPHA * Math.max(0f, 1f - (age - life) / (float) FADE_TICKS);
            }
            return ALPHA;
        }

        /** Vrai quand il a fini de s'effacer. */
        boolean dead() {
            return age >= life + FADE_TICKS;
        }

        /** Un tick : il avance de sa vitesse, et tombe d'un rien. */
        void advance() {
            pos[0] += vel[0];
            pos[1] += vel[1];
            pos[2] += vel[2];
            vel[1] -= GRAVITY;
            age++;
        }
    }

    private static final List<Grain> LIVE = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private WingDust() {
    }

    /** Les grains poses. */
    public static List<Grain> live() {
        return LIVE;
    }

    /** Tout effacer : le monde change, et rien de ce qui tournait n'y tourne plus. */
    public static void clear() {
        LIVE.clear();
    }

    /**
     * Le point de naissance d'un grain : la direction tiree sur la sphere, au rayon demande.
     *
     * <p>L'elevation se compte depuis le zenith — c'est le {@code r cos phi} de l'original — donc
     * l'azimut et l'elevation sont tires separement, et la sphere est couverte uniformement.
     */
    static Vec3 birth(double radius, double theta, double phi) {
        double flat = radius * Math.sin(phi);
        return new Vec3(flat * Math.cos(theta), radius * Math.cos(phi), flat * Math.sin(theta));
    }

    /** Sa vitesse : tangentielle — perpendiculaire au rayon — et un souffle vertical. */
    static Vec3 velocity(double theta, double lift) {
        return new Vec3(Math.sin(theta) * TANGENT, lift, -Math.cos(theta) * TANGENT);
    }

    /**
     * Semer les grains d'un tick autour du porteur.
     *
     * <p>Le porteur est pris <b>aux pieds</b>, comme l'original : c'est son {@code posX}, {@code
     * posY}, {@code posZ}.
     */
    public static void spawn(Player player) {
        Vec3 feet = player.position();
        for (int i = 0; i < PER_TICK; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = RANDOM.nextDouble() * Math.PI - Math.PI;
            double radius = RADIUS_MIN + RANDOM.nextDouble() * (RADIUS_MAX - RADIUS_MIN);
            double lift = LIFT_MIN + RANDOM.nextDouble() * (LIFT_MAX - LIFT_MIN);

            Vec3 start = birth(radius, theta, phi).add(feet);
            Vec3 speed = velocity(theta, lift);
            LIVE.add(new Grain(new double[] { start.x, start.y, start.z },
                    new double[] { speed.x, speed.y, speed.z },
                    LIFE_MIN + RANDOM.nextInt(LIFE_MAX - LIFE_MIN + 1)));
        }
    }

    /**
     * Un tick : les grains avancent, et ceux qui ont fini s'en vont.
     *
     * <p>L'appel vient de l'horloge du client, avec les autres effets de vecmanip : les grains ne
     * sont pas les ailes, et ils leur survivent — c'est ce que faisait l'original, dont le semeur
     * s'arretait avec son contexte et dont les grains, eux, continuaient.
     */
    public static void tick() {
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            Grain grain = LIVE.get(i);
            if (grain.dead()) {
                LIVE.remove(i);
            } else {
                grain.advance();
            }
        }
    }
}
