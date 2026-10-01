package cn.academy.entity;

import java.util.Random;

/**
 * Les nombres de la bille de silicium : sa rotation en vol, ses eclats, et sa taille.
 *
 * <p>Classe sans aucun type qui ait besoin du jeu, expres : ce sont les nombres de
 * {@code EntitySilbarn} et de son {@code RenderSibarn}, et les figer par un test est la seule
 * facon de verifier qu'ils ne bougent pas sans lancer un jeu. L'entite, elle, ne peut pas les
 * porter : un test unitaire qui la lirait chargerait ses donnees synchronisees, donc le registre
 * des entites, et un registre n'existe pas hors du jeu.
 */
public final class SilbarnVisuals {

    /**
     * Cote de sa boite de collision, en blocs.
     *
     * <p><b>ECART ASSUME, demande du joueur.</b> L'original faisait quarante centimetres
     * ({@code setSize(.4f, .4f)}), et c'est peu pour une barre qu'on jette a la main : viser une
     * bille en vol pour amorcer la salve de rayons demandait de la toucher au centimetre pres, et
     * le joueur a demande plus large. Le double, donc — et c'est d'autant plus utile que c'est
     * cette boite que le rayon de visee de la salve doit rencontrer.
     */
    public static final float HIT_SIZE = 0.8f;

    /**
     * Vitesse de rotation en vol : trente degres par seconde.
     *
     * <p>L'original tournait autour d'un axe tire au hasard, de {@code 0.03} degre par
     * milliseconde. C'est une rotation lente, presque majestueuse, et c'est exactement ce qu'on
     * veut d'une barre qui flotte : elle se lit comme un objet qui tourne, pas comme une helice.
     */
    public static final float SPIN_PER_TICK = 1.5f;

    /** Echelle du modele : {@code glScaled(0.05)} de l'original. */
    public static final float MODEL_SCALE = 0.05f;

    // --- LES ECLATS ---

    /** Nombre d'eclats a l'impact : dix-huit a vingt-sept, comme l'original. */
    public static final int FRAG_MIN = 18;
    public static final int FRAG_MAX = 27;

    /** Leur vitesse de depart, en blocs par tick : de 0,08 a 0,18. */
    public static final double FRAG_SPEED_MIN = 0.08;
    public static final double FRAG_SPEED_MAX = 0.18;

    /** Et un petit coup vers le haut, qui les fait jaillir plutot que retomber aussitot. */
    public static final double FRAG_RISE = 0.2;

    /** Leur taille, en blocs : dix centimetres, comme l'original. */
    public static final float FRAG_SIZE = 0.1f;

    /** Leur gravite : 0,03 par tick, six fois moins que celle de la bille. */
    public static final double FRAG_GRAVITY = 0.03;

    /** Et leur rotation, en degres par tick : vingt-cinq, comme l'original. */
    public static final float FRAG_SPIN_PER_TICK = 25f;

    /**
     * Combien de temps un eclat reste visible.
     *
     * <p>C'est le seul nombre que l'original ne chiffre pas : sa particule vivait selon le
     * reglage par defaut de sa fabrique, qui ne se lit pas dans la competence. Vingt ticks — une
     * seconde — donne le temps de les voir jaillir et retomber, et de comprendre que quelque
     * chose s'est casse.
     */
    public static final int FRAG_LIFE_TICKS = 20;

    private SilbarnVisuals() {}

    /**
     * L'angle de rotation de la bille a cet age, en degres.
     *
     * <p>L'original calculait {@code 0.03 * (temps en millisecondes)}, donc une rotation continue
     * qui ne repart jamais de zero : le port fait la meme chose a partir de l'age de l'entite, et
     * le tick partiel compte — sans lui, la bille tournerait par a-coups de vingt images par
     * seconde.
     */
    public static float spinDegrees(double ageTicks) {
        return (float) (ageTicks * SPIN_PER_TICK);
    }

    /**
     * La direction d'un eclat : au hasard sur la sphere, puis redressee.
     *
     * <p>L'original tirait ses trois composantes a la suite, en forcant la derniere a completer
     * la norme, et le resultat penchait vers le haut — ce qui est tant mieux, un eclat qui
     * jaillit vers le sol ne se voyant pas. Ici la direction est tiree vraiment au hasard, et
     * c'est {@link #FRAG_RISE} qui fait ce travail : le seul ecart, et il ne se voit qu'en mieux.
     */
    public static double[] fragDirection(Random random) {
        double azimuth = random.nextDouble() * Math.PI * 2;
        double height = random.nextDouble() * 2 - 1;
        double flat = Math.sqrt(1 - height * height);
        return new double[] { Math.cos(azimuth) * flat,
                              height + FRAG_RISE,
                              Math.sin(azimuth) * flat };
    }
}
