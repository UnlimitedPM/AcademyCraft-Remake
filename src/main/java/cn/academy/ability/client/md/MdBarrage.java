package cn.academy.ability.client.md;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * La gerbe de la salve de rayons : les 25 a 30 traits qui partent d'une bille de silicium.
 *
 * <p>L'original en faisait une seule entite — {@code EntityMdRayBarrage} — qui portait ses
 * sous-rayons et se dessinait vingt-cinq fois dans la meme passe, en changeant seulement son
 * lacet et son tangage entre chaque. Chaque sous-rayon avait un decalage <b>tire au hasard</b>,
 * une fois pour toutes a la naissance : la gerbe ne bouge donc pas, elle s'agite sur place
 * pendant ses deux secondes et demie.
 *
 * <h2>Les nombres</h2>
 *
 * <p>L'original tirait une seule portee angulaire, entre 50 et 60 degres, et s'en servait comme
 * d'un couple : <b>plus ou moins elle</b> en lacet, <b>plus ou moins sa moitie</b> en tangage. La
 * gerbe est donc deux fois plus large que haute — la meme asymetrie que le cone de degats de la
 * competence, et pour la meme raison, c'est la forme que son auteur a choisie. Puis il tirait de
 * 25 a 30 sous-rayons, chacun de quinze blocs : la longueur par defaut de sa base de rayons, que
 * la salve ne prenait pas la peine de changer.
 *
 * <p>Classe sans aucun etat et sans horloge : elle ne fait que des tirages et une rotation, donc
 * un test peut la derouler entierement sans lancer un jeu.
 */
public final class MdBarrage {

    /** Sous-rayons par gerbe : de 25 a 30, le {@code rangei(25, 30)} de l'original. */
    public static final int SUBS_MIN = 25;
    public static final int SUBS_MAX = 30;

    /** La portee angulaire tiree : 50 a 60 degres, en lacet ET la moitie en tangage. */
    public static final double SPREAD_MIN = 50;
    public static final double SPREAD_MAX = 60;

    /** La longueur d'un trait : les quinze blocs par defaut de {@code EntityRayBase}. */
    public static final double RAY_LENGTH = 15.0;

    // --- D'OU PART LE TRAIT ---

    /**
     * Le trait part de la main, et non de l'oeil.
     *
     * <p>L'original le faisait partir des yeux — {@code player.posY + player.eyeHeight} — et le
     * joueur a vu tout de suite ce que cela donne : le rayon nait <b>dans la camera</b>, et il
     * remplit l'ecran au moment du tir, au point qu'on ne voit plus que lui. Le port le decale
     * donc vers la main droite, comme la plupart des tirs de ce genre : trois dixiemes de cote,
     * trois dixiemes plus bas, un peu en avant.
     *
     * <p>C'est un ecart assume, et demande. Il ne change <b>rien</b> a ce que le tir touche : le
     * point d'arrivee est toujours celui que le regard a trouve, seul le depart bouge.
     */
    public static final double HAND_SIDE = 0.3;
    public static final double HAND_DROP = 0.3;
    public static final double HAND_FORWARD = 0.4;

    private MdBarrage() {}

    /**
     * Le point d'ou le trait part : la main droite, telle qu'on la voit en premiere personne.
     *
     * <p>La droite se lit par le produit vectoriel du regard et de la verticale — pour un regard
     * vers le sud, elle tombe a l'ouest, ce qui est bien la droite du joueur. Un regard pile a la
     * verticale n'a pas de droite : le trait part alors de l'oeil, faute de mieux.
     */
    public static Vec3 handOrigin(Vec3 eye, Vec3 look) {
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        Vec3 base = right.lengthSqr() < 1.0E-6
                ? eye
                : eye.add(right.normalize().scale(HAND_SIDE));
        return base.add(0, -HAND_DROP, 0).add(look.scale(HAND_FORWARD));
    }

    /** Combien de traits porte cette gerbe. Le tirage de l'original : {@code rangei(25, 30)}. */
    public static int subCount(Random random) {
        return SUBS_MIN + random.nextInt(SUBS_MAX - SUBS_MIN);
    }

    /** La portee angulaire de cette gerbe, en degres : de 50 a 60. */
    public static double spread(Random random) {
        return SPREAD_MIN + random.nextDouble() * (SPREAD_MAX - SPREAD_MIN);
    }

    /** Le decalage en lacet d'un trait : la portee entiere, des deux cotes. */
    public static double yawOffset(double spread, Random random) {
        return (random.nextDouble() * 2 - 1) * spread;
    }

    /** Et son decalage en tangage : la moitie de la portee. La gerbe est plus large que haute. */
    public static double pitchOffset(double spread, Random random) {
        return (random.nextDouble() * 2 - 1) * spread / 2;
    }

    /**
     * La direction d'un trait : le regard du tireur, tourne de ses deux decalages.
     *
     * <p>C'est ce que faisait le rendu de l'original en changeant le lacet et le tangage de son
     * entite avant chaque passage. L'ordre compte peu — les deux rotations ne se melangent pas
     * pour de petits angles — mais il est le meme pour tous les traits, donc la gerbe garde sa
     * forme.
     */
    public static Vec3 direction(Vec3 look, double yawDegrees, double pitchDegrees) {
        return look.yRot((float) Math.toRadians(yawDegrees))
                .xRot((float) Math.toRadians(pitchDegrees));
    }
}
