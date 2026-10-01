package cn.academy.ability.meltdowner;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Les nombres de la bille de plasma, portage d'{@code EntityMdBall}.
 *
 * <p>La bille est le premier objet du meltdowner : c'est elle qu'on voit flotter devant le
 * porteur pendant qu'il prepare sa bombe a electrons, et c'est d'elle que part le petit rayon.
 * Trois autres competences s'en servent ensuite — la bombe a fragmentation, le missile a
 * electrons et le rayon chanceux — donc tout ce qu'elle porte est ici, hors du rendu, pour
 * pouvoir etre relu et mesure sans lancer un jeu.
 *
 * <h2>Elle flotte, elle ne vole pas</h2>
 *
 * <p>Contrairement a ce que son nom laisse croire, la bille ne traverse pas le monde : elle
 * se tient <b>a cote de son porteur</b>, a un ecart tire une fois pour toutes au lancement.
 * L'original la placait a chaque tick a {@code porteur + ecart}, et le port fait pareil : elle
 * suit donc le joueur qui marche, comme un objet qu'il porterait.
 *
 * <p>L'ecart se tire autour du <b>regard</b> : un angle de plus ou moins 45 % d'un demi-tour
 * autour du lacet, un rayon de 0,8 a 1,3 bloc, et une hauteur de -1,2 a +0,2 bloc — donc
 * plutot sous les yeux que devant, ce qui donne a la bille l'air de tourner autour du corps.
 *
 * <h2>Sa vie se lit en secondes</h2>
 *
 * <p>Les courbes d'opacite et de taille de l'original sont en <b>secondes</b> — son horloge,
 * {@code GameTimer.getTime()}, comptait en secondes — et non en ticks. Une bille de vingt
 * ticks vit donc une seconde : elle apparait en trois dixiemes de seconde, se tient a 0,6
 * d'opacite, puis <b>gonfle</b> a 1 sur les quatre derniers dixiemes et s'efface en 0,15.
 *
 * <p>Son balancement est de la meme eau : le deplacement du aux sinus est faible, mais il
 * tourne — c'est ce qui donne a la bille son air de vibrer plutot que d'etre posee.
 */
public final class MdBallVisuals {

    /** Les cinq images du coeur, comme les {@code mdball/0..4.png} de l'original. */
    public static final int CORE_TEXTURES = 5;

    /** La vie d'une bille ordinaire, et celle d'une bille amelioree. */
    public static final int LIFE_TICKS = 20;
    public static final int LIFE_IMPROVED_TICKS = 5;

    /**
     * L'experience au-dela de laquelle la bille tire tout de suite.
     *
     * <p>L'original abregeait sa bille a cinq ticks passe 80 % d'experience : la bombe devient
     * alors une arme rapide, presque instantanee, au lieu d'une charge d'une seconde.
     */
    public static final float IMPROVED_EXP = 0.8f;

    /** Ticks entre le tir et la disparition : la bille tire deux ticks avant de mourir. */
    public static final int SHOT_DELAY = 2;

    /** L'ecart de la bille autour de son porteur. */
    public static final double RANGE_FROM = 0.8;
    public static final double RANGE_TO = 1.3;
    public static final double SUB_Y_MIN = -1.2;
    public static final double SUB_Y_MAX = 0.2;

    /** L'ecart d'angle autour du regard : 45 % d'un demi-tour, de part et d'autre du lacet. */
    public static final double YAW_SPREAD = Math.PI * 0.45;

    /** Le balancement : trois centimetres en largeur, quatre en hauteur. */
    public static final double WOBBLE_XZ = 0.03;
    public static final double WOBBLE_Y = 0.04;

    /**
     * La periode du balancement, en secondes.
     *
     * <p>Attention : l'original s'en servait comme d'un <b>angle</b> et non d'une periode — son
     * {@code sin(t / 0,3)} fait donc un tour complet en 0,3 x 2 pi, soit 1,88 seconde. C'est
     * repris tel quel.
     */
    public static final double WOBBLE_PERIOD = 0.3;

    /** Les deux carres : la lueur fait 0,7 bloc de cote, le coeur 0,5. */
    public static final double GLOW_SIZE = 0.7;
    public static final double CORE_SIZE = 0.5;

    /**
     * La hauteur a laquelle la bille se DESSINE, par rapport a sa position.
     *
     * <p>Sa position logique est celle de son porteur plus l'ecart — donc au niveau des pieds.
     * L'original l'y dessinait pourtant <b>1,6 bloc plus haut</b>, et c'est ce que le joueur voit
     * dans le vrai mod : la bille flotte a hauteur d'yeux, et son rayon part de la. Le port
     * l'avait oubliee, et le joueur l'a vu tout de suite : « l'endroit ou la boule apparait et
     * l'endroit d'ou le laser part ne sont pas les memes ». La bille se dessinait donc dans les
     * jambes — jusqu'a 1,2 bloc sous les pieds — pendant que son rayon partait un bloc et demi
     * plus haut, a la hauteur des yeux, ou il prend sa source (voir {@code EntityMdBall.fire}).
     *
     * <p>1,6 et non 1,62 : c'est le chiffre de l'original, qui compensait la hauteur des yeux a
     * la main. Les deux centimetres qui restent ne se voient pas, et la bille n'a pas a
     * descendre quand son porteur s'accroupit.
     */
    public static final double RENDER_HEIGHT = 1.6;

    /** L'opacite au repos, et celle du tir. */
    public static final float BASE_ALPHA = 0.6f;

    /** Les trois temps de la vie : apparition, gonflement, effacement. */
    public static final double FADE_IN_TIME = 0.3;
    public static final double BURST_TIME = 0.4;
    public static final double FADE_OUT_TIME = 0.15;

    /** Le scintillement d'opacite : une marche au hasard, sur [0, 1], de +-4 par seconde. */
    public static final double WIGGLE_MAX_ACCEL = 4.0;

    /** Et ses deux tirages, tels quels : trois fois sur huit, et deux fois sur huit. */
    public static final int WIGGLE_CHANCE_IN_EIGHT = 3;
    public static final int TEXTURE_CHANCE_IN_EIGHT = 2;

    private MdBallVisuals() {
    }

    /** La duree de vie d'une bille, en secondes : cinquante millisecondes par tick. */
    public static double lifeSeconds(int lifeTicks) {
        return lifeTicks * 0.05;
    }

    /**
     * L'opacite de la bille a cet age, en secondes.
     *
     * <p>Trois morceaux, dans l'ordre de l'original : l'apparition sur les trois premiers
     * dixiemes de seconde, le palier a 0,6, puis le gonflement vers 1 sur les quatre derniers
     * dixiemes — c'est le moment du tir — et l'effacement final en 0,15 seconde.
     */
    public static float alpha(double ageSeconds, int lifeTicks) {
        double life = lifeSeconds(lifeTicks);

        if (ageSeconds > life - FADE_OUT_TIME) {
            return (float) Math.max(0.0,
                    lerp(1.0, 0.0, (ageSeconds - (life - FADE_OUT_TIME)) / FADE_OUT_TIME));
        }
        if (ageSeconds > life - BURST_TIME) {
            return (float) lerp(BASE_ALPHA, 1.0,
                    (ageSeconds - (life - BURST_TIME)) / (BURST_TIME - FADE_OUT_TIME));
        }
        if (ageSeconds < FADE_IN_TIME) {
            return (float) lerp(0.0, BASE_ALPHA, ageSeconds / FADE_IN_TIME);
        }
        return BASE_ALPHA;
    }

    /**
     * La taille de la bille.
     *
     * <p>C'est <b>un</b>, toujours. L'original avait une courbe de gonflement a la fin de la
     * vie, mais elle comparait un age en secondes a une duree en millisecondes : elle ne
     * s'allumait donc jamais, et ses billes gardaient leur taille. Le port la laisse de cote
     * pour la meme raison, et le dit ici pour que personne ne la cherche.
     */
    public static float size() {
        return 1f;
    }

    /** Le balancement en largeur : un sinus et un cosinus qui tournent ensemble. */
    public static double wobbleX(double ageSeconds) {
        return WOBBLE_XZ * Math.sin(ageSeconds / WOBBLE_PERIOD);
    }

    /** Le balancement en profondeur, en quadrature avec le precedent. */
    public static double wobbleZ(double ageSeconds) {
        return WOBBLE_XZ * Math.cos(ageSeconds / WOBBLE_PERIOD);
    }

    /**
     * Le balancement en hauteur.
     *
     * <p>Le meme angle, multiplie par 1,4 et decale d'un cinquieme de tour : c'est ce qui
     * empeche les trois axes de battre ensemble, et ce qui donne a la bille son mouvement
     * d'insecte plutot que celui d'un pendule.
     */
    public static double wobbleY(double ageSeconds) {
        return WOBBLE_Y * Math.cos(ageSeconds / WOBBLE_PERIOD * 1.4 + Math.PI / 3.5);
    }

    /**
     * L'ecart de la bille autour de son porteur, tire une fois pour toutes au lancement.
     *
     * <p>Le lacet du porteur oriente l'ecart — la bille se tient donc plutot devant lui — et
     * le hasard l'ouvre de plus ou moins 45 % d'un demi-tour. L'axe des X est un sinus et
     * celui des Z un cosinus, comme dans l'original : c'est ce qui met la bille sur un cercle
     * et non sur une diagonale.
     */
    public static Vec3 subOffset(float yawDegrees, RandomSource random) {
        double theta = -yawDegrees / 180.0 * Math.PI
                + ranged(random, -YAW_SPREAD, YAW_SPREAD);
        double range = ranged(random, RANGE_FROM, RANGE_TO);
        return new Vec3(Math.sin(theta) * range, ranged(random, SUB_Y_MIN, SUB_Y_MAX),
                Math.cos(theta) * range);
    }

    private static double ranged(RandomSource random, double from, double to) {
        return from + random.nextDouble() * (to - from);
    }

    /**
     * De combien deplacer le rendu pour que la bille soit <b>exactement</b> sur son porteur.
     *
     * <p>Le client ne connait pas la position que le serveur vient de donner a la bille : il
     * interpole celle qu'il a recue sur <b>trois</b> ticks. Dessinee a sa propre position, la
     * bille retarde donc sur son porteur et le rattrape par bonds des que celui-ci bouge — le
     * joueur l'a vu tout de suite : « quand on se deplace, l'animation du deplacement des
     * billes est un peu bizarre, c'est pas fluide, c'est pas instantane ». Le rendu repose donc
     * la bille la ou le serveur la tient : porteur, image comprise, plus son ecart — un ecart
     * fige depuis la naissance, donc rien ne bouge sous le dessin.
     *
     * @param spawnerPos le porteur, a l'image qu'on dessine
     * @param sub        l'ecart de la bille, celui qu'elle garde toute sa vie
     * @param drawnPos   la position que le moteur va utiliser pour la bille
     */
    public static Vec3 snapOffset(Vec3 spawnerPos, Vec3 sub, Vec3 drawnPos) {
        return spawnerPos.add(sub).subtract(drawnPos);
    }

    private static double lerp(double from, double to, double factor) {
        return from + (to - from) * factor;
    }
}
