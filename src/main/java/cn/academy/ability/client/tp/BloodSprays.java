package cn.academy.ability.client.tp;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Les taches de sang qui restent sur le sol, portage de {@code BloodSprayEffect}.
 *
 * <p>Le sang du retour de sang ne s'arrete pas a la plaie : l'original tirait <b>neuf
 * directions</b> depuis la tete de sa victime — droit devant, puis par paires a 30, 45, 60 et 80
 * degres — les lancait a cinq blocs, et posait deux eclaboussures la ou chacune rencontrait un
 * bloc. Ce sont ces taches-la qui restent par terre une minute entiere.
 *
 * <h2>La tache elle-meme</h2>
 *
 * <p>Une tache est un carre pose <b>contre une face de bloc</b>, a un centieme au-dela : le centre
 * de la face, decale de 51 % de la boite du bloc dans la direction de la normale. Sa taille va de
 * 1,1 a 1,4 bloc — huit dixiemes de moins sur une paroi verticale —, son image est tiree parmi
 * trois, et son inclinaison parmi trois cent soixante degres. Un petit flottement gaussien de
 * quinze centimetres l'ecarte du centre exact, sinon toutes les taches d'une meme face
 * s'empileraient au meme endroit.
 *
 * <p>Le choix de l'image est celui de l'original, et il surprend : une face <b>horizontale</b> —
 * le sol, justement — prend les images nommees {@code wall}, et une paroi verticale les images
 * nommees {@code grnd}. Les deux familles sont apparues echangees dans les ressources de
 * l'original, et le port garde l'echange : ce sont ces images-la que le joueur a vues.
 *
 * <h2>D'ou elles viennent</h2>
 *
 * <p>Du <b>serveur</b>, et c'est tout le point : lui seul sait qui a ete touche, donc lui seul sait
 * s'il faut en semer. Le port les tirait chez le client, et le joueur a vu les deux defauts que
 * cela donne — des taches quand rien n'est touche, et des taches semees depuis ses propres yeux au
 * lieu de la tete de sa victime, donc trop loin, trop sur les murs. Le serveur les trace maintenant
 * depuis la bonne tete, avec les nombres de l'original, et les envoie — voir BloodSprayPacket.
 *
 * <p>Les taches vivent une minute, ou jusqu'a ce que leur bloc disparaisse — l'original disait
 * vingt-quatre mille ticks, soit vingt minutes, ce qui revient au meme a l'echelle d'une partie
 * mais laissait des taches dans les sauvegardes longues.
 */
public final class BloodSprays {

    /** Une tache vit une minute. */
    public static final int LIFE_TICKS = 1200;

    /** Trois images par famille, tirees parmi dix, comme l'original. */
    public static final int FRAMES = 3;
    public static final int TEXTURE_ROLL = 10;

    /** La taille : 1,1 a 1,4 bloc, et huit dixiemes de moins sur une paroi. */
    public static final double SIZE_MIN = 1.1;
    public static final double SIZE_MAX = 1.4;
    public static final double WALL_SHRINK = 0.8;

    /** La pose sur la face : 51 % de la boite, et quinze centimetres de flottement au plus. */
    public static final double FACE_OFFSET = 0.51;
    public static final double PLANE_JITTER = 0.15;

    /**
     * L'ombre d'une tache : l'ambiance de l'original, et son plein.
     *
     * <p>Le 0,4 n'est pas un reglage, c'est l'ambiance meme de la lumiere de GL — le peu qui reste
     * quand la face se detourne de la lampe. Voir {@link #shade}.
     */
    public static final float SHADE_MIN = 0.4f;
    public static final float SHADE_MAX = 1f;

    /**
     * Une tache posee : son bloc, sa face, son centre, sa taille, son inclinaison, son image, son age.
     *
     * @param pos le bloc qui la porte
     * @param centre ou elle se dessine, dans le monde
     * @param face la face du bloc qui la porte
     * @param size son cote, en blocs
     * @param rotation son inclinaison dans le plan de la face, en degres
     * @param frame son image, de zero a deux
     * @param age ses ticks
     */
    public record Spray(BlockPos pos, Vec3 centre, Direction face, double size, float rotation,
            int frame, int age) {

        /** Vrai si elle peut encore se voir : une minute, pas plus. */
        public boolean alive() {
            return age < LIFE_TICKS;
        }

        /** La tache d'un tick de plus. */
        public Spray aged() {
            return new Spray(pos, centre, face, size, rotation, frame, age + 1);
        }
    }

    private static final List<Spray> LIVE = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private BloodSprays() {
    }

    /** Les taches posees. */
    public static List<Spray> live() {
        return LIVE;
    }

    /** Tout effacer : le monde change, et rien de ce qui etait pose n'y est plus. */
    public static void clear() {
        LIVE.clear();
    }

    /**
     * Poser une tache sur une face de bloc.
     *
     * <p>Tout ce qui la definit se tire ici : sa taille selon la face, son inclinaison, son image,
     * et son centre — le milieu de la boite du bloc, pousse d'un demi-centieme au-dela de la face.
     */
    public static Spray pose(Level level, BlockPos pos, Direction face) {
        AABB shape = level.getBlockState(pos).getShape(level, pos).bounds();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 centre = Vec3.atLowerCornerOf(pos)
                .add(shape.getCenter())
                .add(normal.scale(FACE_OFFSET))
                .add(inPlane(face, RANDOM.nextGaussian() * PLANE_JITTER,
                        RANDOM.nextGaussian() * PLANE_JITTER));

        Spray spray = new Spray(pos, centre, face, size(RANDOM, face),
                RANDOM.nextFloat() * 360f, RANDOM.nextInt(TEXTURE_ROLL) % FRAMES, 0);
        LIVE.add(spray);
        return spray;
    }

    /** La taille d'une tache : 1,1 a 1,4 bloc, et huit dixiemes de moins sur une paroi. */
    public static double size(RandomSource random, Direction face) {
        double base = SIZE_MIN + random.nextDouble() * (SIZE_MAX - SIZE_MIN);
        return floor(face) ? base : base * WALL_SHRINK;
    }

    /** Vrai pour une face horizontale : le sol, ou un plafond. */
    public static boolean floor(Direction face) {
        return face.getAxis().isVertical();
    }

    /**
     * L'ombre d'une tache, selon le regard qui la voit.
     *
     * <p>Le materiau de l'original <b>ne coupait pas l'eclairage</b> : la ou les ailes appellent
     * {@code setIgnoreLight()}, lui ne l'appelle pas, et sa facture pose une normale — (0, 0, 1)
     * dans le repere de la tache, donc <b>la normale de la face</b> — puis laisse GL l'eclairer. Sa
     * lampe vient de l'oeil, comme celle des ailes, mais son plancher n'est pas le meme : ici il
     * n'est pas arrange, c'est l'ambiance meme de GL (0,4), le peu qui reste quand la face se
     * detourne. Aux ailes, la normale est fausse — {@code (rayon, hauteur, 0)} n'est pas la normale
     * du ruban — et son plancher de 0,55 est un reglage. Voir {@code TornadoRenderer#shade}.
     *
     * <p>Une tache qui regarde l'oeil est donc en pleine lumiere, et celle qu'on voit de biais — le
     * sol, la plupart du temps — s'assombrit jusqu'au plancher. Le port les dessinait toutes au
     * plein, et le joueur a trouve les siennes claires : « les taches de sang ne sont pas assez
     * sombres par rapport au vrai mod ».
     */
    public static float shade(Direction face, Vec3 look) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        double lambert = Math.max(0.0, normal.dot(look.scale(-1)));
        return (float) (SHADE_MIN + (SHADE_MAX - SHADE_MIN) * lambert);
    }

    /**
     * Une avancee dans le plan d'une face : {@code along} le long de sa premiere perpendiculaire,
     * {@code across} sur la seconde.
     */
    public static Vec3 inPlane(Direction face, double along, double across) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        // La premiere perpendiculaire : vers le haut pour une face horizontale, sinon vers l'est.
        Vec3 first = floor(face) ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        Vec3 u = first.subtract(normal.scale(first.dot(normal))).normalize();
        Vec3 v = normal.cross(u);
        return u.scale(along).add(v.scale(across));
    }

    /** Un tick : les taches trop vieilles s'en vont, et celles dont le bloc a disparu avec elles. */
    public static void tick(Level level) {
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            Spray spray = LIVE.get(i);
            if (!spray.alive() || level.isEmptyBlock(spray.pos())) {
                LIVE.remove(i);
            } else {
                LIVE.set(i, spray.aged());
            }
        }
    }

    /** L'inclinaison d'une tache, en radians, arrondie comme le rendu l'attend. */
    public static float radians(Spray spray) {
        return spray.rotation() * Mth.DEG_TO_RAD;
    }

    /** La famille d'images d'une tache : voir l'entete — les deux sont echangees, comme chez
     * l'original. */
    public static String family(Spray spray) {
        return floor(spray.face()) ? "wall" : "grnd";
    }

    /**
     * Pose les taches annoncees par le serveur : une par face, autant de fois qu'il l'a dit.
     *
     * <p>C'est le serveur qui a trace la gerbe — lui seul sait qui a ete touche — et le client ne
     * fait ici que ce qu'il ne peut pas faire sans lui : lire la forme des blocs frappes.
     */
    public static void poseAll(java.util.List<BlockPos> positions, java.util.List<Direction> faces) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        for (int i = 0; i < positions.size() && i < faces.size(); i++) {
            pose(level, positions.get(i), faces.get(i));
        }
    }
}
