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

    private MdBarrage() {}

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
