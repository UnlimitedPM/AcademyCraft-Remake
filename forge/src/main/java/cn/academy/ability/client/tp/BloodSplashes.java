package cn.academy.ability.client.tp;

import net.minecraft.Util;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * La gerbe de sang de la chair arrachee.
 *
 * <p>Portage de l'{@code EntityBloodSplash} de l'original : quand la chair ripping touche sa
 * bete, quatre a six eclaboussures naissent autour d'elle, chacune avec sa taille, et vivent le
 * temps de dix images — une par tick, la duree de l'animation d'origine.
 *
 * <p>C'est la fin de l'animation de la competence : la boite rouge marque la cible pendant la
 * visee, et le sang jaillit a l'instant du coup. Les deux se suivent sans se chevaucher, comme
 * dans l'original, ou le marqueur mourait dans le meme souffle que la gerbe partait.
 *
 * <p>Rien n'est dessine ici : cette classe tient les eclaboussures et leur age, et c'est
 * {@link BloodSplashRenderer} qui les peint. Comme les etincelles de la teleportation, elles
 * naissent chez le client qui les voit, et le serveur ne fait que dire a qui elles appartiennent —
 * voir {@code BloodSplashPacket}.
 */
@OnlyIn(Dist.CLIENT)
public final class BloodSplashes {

    /** Les dix images de {@code effects/blood_splash}. */
    public static final int FRAMES = 10;

    /** Le temps d'une image, en millisecondes : un tick, comme l'animation de l'original. */
    public static final double FRAME_MILLIS = 50.0;

    /** Le nombre d'eclaboussures d'une gerbe : de quatre a six, comme l'original. */
    public static final int MIN_COUNT = 4;
    public static final int MAX_COUNT = 6;

    /** Leur taille : de 0,8 a 1,3 bloc. */
    public static final double MIN_SIZE = 0.8;
    public static final double MAX_SIZE = 1.3;

    /** Leur distance a l'axe de la victime : la moitie de ce qu'on tire, au plus. */
    public static final double SPREAD = 0.5;

    /** Et le tirage ne descend pas sous cette fraction de la largeur de la bete. */
    public static final double SPREAD_FLOOR = 0.8;

    private static final RandomSource RANDOM = RandomSource.create();

    private static final List<Splash> LIVE = new ArrayList<>();

    private BloodSplashes() {
    }

    /** Une eclaboussure : ou elle est, sa taille, et quand elle est nee. */
    public record Splash(Vec3 position, double size, long born) {

        /** L'image a cet instant, de zero a {@link #FRAMES} moins un. */
        public int frame(long now) {
            return (int) Math.min(Math.max(now - born, 0L) / FRAME_MILLIS, FRAMES - 1);
        }

        /** Vrai quand ses dix images sont passees : elle n'a plus rien a montrer. */
        public boolean finished(long now) {
            return now - born >= FRAMES * FRAME_MILLIS;
        }
    }

    /**
     * Une gerbe, autour d'une creature qu'on vient de frapper.
     *
     * <p>Ses eclaboussures se semment sur le cote de la bete, a une hauteur tiree dans son corps,
     * et a une distance tiree dans son rayon : c'est la repartition de l'original, et c'est ce qui
     * fait qu'une gerbe ne ressemble jamais a la precedente.
     */
    public static void burst(Vec3 feet, double width, double height) {
        int count = MIN_COUNT + RANDOM.nextInt(MAX_COUNT - MIN_COUNT + 1);
        long now = Util.getMillis();
        for (int i = 0; i < count; i++) {
            double radius = SPREAD * lerp(SPREAD_FLOOR * width, width, RANDOM.nextDouble());
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double up = RANDOM.nextDouble();
            LIVE.add(new Splash(place(feet, height, theta, radius, up),
                    lerp(MIN_SIZE, MAX_SIZE, RANDOM.nextDouble()), now));
        }
    }

    /**
     * Ou tombe une eclaboussure : sur le cote de la bete, a une hauteur de son corps.
     *
     * <p>Fonction pure, donc verifiable : le rayon se lit en sinus et cosinus, et la hauteur en
     * fraction de celle de la creature — de ses pieds a son sommet.
     */
    public static Vec3 place(Vec3 feet, double height, double theta, double radius, double up) {
        return new Vec3(feet.x + radius * Math.sin(theta), feet.y + up * height,
                feet.z + radius * Math.cos(theta));
    }

    /** Les eclaboussures encore vivantes, les finies otees au passage. */
    public static List<Splash> live() {
        long now = Util.getMillis();
        LIVE.removeIf(splash -> splash.finished(now));
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
