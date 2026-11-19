package cn.academy.ability.client.vm;

import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Les ondulations d'ecran du champ de vecmanip, portage de {@code WaveEffectUI}.
 *
 * <p>Les deux veilles de la categorie ne se contentent pas d'agir sur le monde : elles font
 * <b>trembler l'ecran</b> de celui qui les tient. Des ronds de lueur y apparaissent au hasard,
 * grossissent, et s'effacent — l'image d'un espace qui ondule sous le regard. La <b>deviation</b>
 * les fait discrets et serres, le <b>renvoi</b> plus visibles et plus larges, et c'est tout ce qui
 * les separe.
 *
 * <h2>Une image, pas un tick</h2>
 *
 * <p>C'est le seul effet du port qui se compte en <b>fractions de seconde</b> plutot qu'en ticks :
 * l'original le dessinait a chaque image de rendu et lisait le temps ecoule depuis la precedente.
 * Sa densite est d'ailleurs <b>par seconde</b> — une seconde et demie de veille fait naitre environ
 * deux ondulations —, donc un effet qui passerait au tick n'aurait pas le meme aspect sur un
 * ecran a soixante images et sur un autre a cent vingt.
 *
 * <p>La part pure est ici, et se verifie en JUnit : la duree de vie d'une ondulation, sa
 * croissance, sa courbe d'opacite, et le fait qu'une image n'en fasse naitre <b>qu'une</b> au
 * plus.
 */
public final class WaveRipples {

    /** Les trois nombres d'une veille : son opacite, sa taille moyenne, et sa densite. */
    public record Settings(double alpha, double size, double intensity) {
    }

    /** La deviation : discrète et serree. */
    private static final Settings DEVIATION = new Settings(0.2, 100, 1.4);

    /** Le renvoi : deux fois plus visible, un peu plus large, et un peu plus dense. */
    private static final Settings REFLECTION = new Settings(0.4, 110, 1.6);

    /** La vie d'une ondulation : d'une seconde et demie a deux secondes et demie. */
    public static final double LIFE_MIN = 1.5;
    public static final double LIFE_MAX = 2.5;

    /** Sa taille part de la taille moyenne, au cinquieme pres. */
    public static final double SIZE_SPREAD = 0.2;

    /** Et elle grandit de vingt pixels par seconde. */
    public static final double GROWTH = 20;

    /** Sa courbe d'opacite : elle monte en un cinquieme de sa vie, se tient jusqu'a la moitie,
     * puis retombe. */
    public static final double RISE = 0.2;
    public static final double HOLD = 0.5;

    /**
     * Le plus long delai compte comme un quart de seconde.
     *
     * <p>L'original ne bornait rien : apres un gel du jeu — un monde qui se charge, une fenetre qui
     * reprend la main — son delta valait des secondes, et l'ecran se couvrait d'un coup de vingt
     * ondulations. Le port borne ce delai-la, et rien d'autre.
     */
    public static final double MAX_DELTA = 0.25;

    /** Une ondulation : son centre a l'ecran, sa vie, sa taille, et son age. */
    public record Ripple(double x, double y, double life, double size, double alive) {

        /** Ou elle en est de sa vie, de zero a un. */
        public double progress() {
            return life <= 0 ? 1 : alive / life;
        }

        /** Son opacite, avant celle de la veille : elle monte, se tient, puis retombe. */
        public double alpha() {
            double progress = progress();
            if (progress < RISE) return progress / RISE;
            if (progress < HOLD) return 1;
            return Math.max(0, 1 - (progress - HOLD) / (1 - HOLD));
        }

        /** Et sa taille dessinee : elle grandit de vingt pixels par seconde. */
        public double drawSize() {
            return size + alive * GROWTH;
        }

        public boolean dead() {
            return alive >= life;
        }

        private Ripple aged(double delta) {
            return new Ripple(x, y, life, size, alive + delta);
        }
    }

    private static final List<Ripple> RIPPLES = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static long lastMillis = -1;

    private WaveRipples() {
    }

    /** Les reglages de la veille tenue, ou rien si la competence tenue n'en est pas une. */
    public static Settings forSkill(String skillName) {
        if (skillName == null) return null;
        if (skillName.equals(VecmanipCategory.VEC_DEVIATION.getName())) return DEVIATION;
        if (skillName.equals(VecmanipCategory.VEC_REFLECTION.getName())) return REFLECTION;
        return null;
    }

    /** Une image de rendu : le temps passe, et il nait parfois une ondulation. */
    public static void frame(long nowMillis, double width, double height, Settings settings) {
        if (settings == null) {
            clear();
            return;
        }
        double delta = lastMillis < 0 ? 0 : Math.min(MAX_DELTA, (nowMillis - lastMillis) / 1000.0);
        lastMillis = nowMillis;
        advance(RIPPLES, delta, settings, width, height, RANDOM);
    }

    /**
     * Le pas d'une image : les ondulations vieillissent, les mortes s'en vont, et une nouvelle
     * peut naitre.
     *
     * <p>Le tirage de naissance est celui de l'original : une chance de
     * {@code delta x densite}. Comme le delta d'une image ne vaut jamais une seconde, une image ne
     * fait jamais naitre plus d'une ondulation.
     */
    public static void advance(List<Ripple> ripples, double delta, Settings settings,
                               double width, double height, RandomSource random) {
        for (int i = ripples.size() - 1; i >= 0; i--) {
            Ripple aged = ripples.get(i).aged(delta);
            if (aged.dead()) {
                ripples.remove(i);
            } else {
                ripples.set(i, aged);
            }
        }

        if (delta > 0 && random.nextDouble() < delta * settings.intensity()) {
            ripples.add(new Ripple(
                    random.nextDouble() * width,
                    random.nextDouble() * height,
                    LIFE_MIN + random.nextDouble() * (LIFE_MAX - LIFE_MIN),
                    settings.size() * (1 - SIZE_SPREAD + random.nextDouble() * 2 * SIZE_SPREAD),
                    0));
        }
    }

    /** Les ondulations vivantes, a dessiner. */
    public static List<Ripple> live() {
        return RIPPLES;
    }

    /** Tout oublier : ni un monde quitte, ni une veille qui se termine n'en laisse derriere. */
    public static void clear() {
        RIPPLES.clear();
        lastMillis = -1;
    }

    /** L'horloge de l'ecran, en millisecondes. */
    public static long now() {
        return Util.getMillis();
    }

    /**
     * La place REELLE d'une ondulation a l'ecran, en large.
     *
     * <p>L'original avait oublie un <b>facteur deux</b> dans son nuanceur : il mappait ses carres
     * avec {@code pos / screenSize - 0.5} au lieu de {@code * 2 - 1}. Une ondulation nee a
     * {@code x} s'affichait donc au quart de l'ecran plus {@code x / 2}, et grande de la moitie de
     * sa taille : ses ronds tenaient dans la <b>moitie centrale</b> de l'ecran.
     *
     * <p>Le joueur l'a confirme sans le savoir : le port dessinait ses ronds a leur taille
     * annoncee, et il les a trouves « trop grands ». Le facteur deux est donc de la partie, comme
     * dans l'original.
     */
    public static double drawnX(Ripple ripple, double width) {
        return width / 4 + ripple.x() / 2;
    }

    /** Et la meme chose en hauteur. */
    public static double drawnY(Ripple ripple, double height) {
        return height / 4 + ripple.y() / 2;
    }

    /** Sa taille dessinee : la moitie de la taille annoncee, facteur deux compris. */
    public static double drawnSize(Ripple ripple) {
        return ripple.drawSize() / 2;
    }

    /**
     * Le coin haut-gauche du carre d'un rond, en large : le centre, recule d'une demi-taille.
     *
     * <p><b>Rien n'est arrondi</b>, et c'est tout le propos. Un {@code blit} de HUD, lui, arrondit
     * la position <b>et</b> la taille, et les deux arrondis ne tombent pas ensemble : le milieu du
     * carre se posait donc un pixel a cote du centre des que la taille changeait, et l'ondulation
     * vibrait en grandissant. Le joueur l'a vu : « le centre du rond n'est jamais au meme endroit,
     * ce qui fait que le cercle vibre ». Le carre se pose donc a sa place exacte, et seule sa
     * taille bouge — le milieu du carre reste le centre de {@link #drawnX} a tout age.
     */
    public static double cornerX(Ripple ripple, double width) {
        return drawnX(ripple, width) - drawnSize(ripple) / 2;
    }

    /** Et la meme chose en hauteur. */
    public static double cornerY(Ripple ripple, double height) {
        return drawnY(ripple, height) - drawnSize(ripple) / 2;
    }
}
