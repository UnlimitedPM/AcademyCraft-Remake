package cn.academy.ability.client.tp;

import cn.academy.ability.Skill;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 * <h2>Ce que le port fait autrement</h2>
 *
 * <p>L'original tirait ses neuf directions depuis la <b>tete de sa victime</b>, que seul son
 * contexte client connaissait. Le port les tire des <b>yeux du tireur</b>, deux blocs plus loin :
 * la victime est juste devant, la gerbe s'ouvre sur cinq blocs, et le sol frappe est le meme. La
 * difference se voit si l'on frappe une cible collee a un mur, et elle est assumee — le port
 * n'interroge pas le serveur pour savoir qui a ete touche, et n'ajoute donc pas de paquet.
 *
 * <p>Les taches vivent une minute, ou jusqu'a ce que leur bloc disparaisse — l'original disait
 * vingt-quatre mille ticks, soit vingt minutes, ce qui revient au meme a l'echelle d'une partie
 * mais laissait des taches dans les sauvegardes longues.
 */
public final class BloodSprays {

    /** Les neuf directions de la gerbe, en degres sous le regard : celle de l'original. */
    public static final int[] SPRAY_ANGLES = { 0, 30, 45, 60, 80, -30, -45, -60, -80 };

    /** Le flou du lacet : vingt degres de chaque cote. */
    public static final float YAW_JITTER = 20f;

    /** La portee de chaque direction : cinq blocs, et un demi-bloc en arriere du depart. */
    public static final double SPRAY_RANGE = 5.0;
    public static final double SPRAY_BACK = 0.5;

    /** Le nombre de taches par bloc rencontre : deux. */
    public static final int SPRAYS_PER_HIT = 2;

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
     * La gerbe du retour de sang : neuf directions depuis les yeux du tireur.
     *
     * <p>Chaque direction qui rencontre un bloc y pose deux taches, sur la face frappee. C'est un
     * detail de {@link #sprayFor}, et rien d'autre : la classe ne s'ouvre que par la competence.
     */
    private static void spray(Player player) {
        Level level = player.level();
        Vec3 eyes = player.getEyePosition(1f);
        float headYaw = player.getYHeadRot();

        for (int angle : SPRAY_ANGLES) {
            float yaw = headYaw + (RANDOM.nextFloat() * 2f - 1f) * YAW_JITTER;
            Vec3 look = Vec3.directionFromRotation(angle, yaw);
            Vec3 from = eyes.subtract(look.scale(SPRAY_BACK));
            Vec3 to = eyes.add(look.scale(SPRAY_RANGE));

            BlockHitResult hit = level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) continue;

            for (int i = 0; i < SPRAYS_PER_HIT; i++) {
                pose(level, hit.getBlockPos(), hit.getDirection());
            }
        }
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
     * La gerbe d'un coup porte, et rien du tout pour une autre competence.
     *
     * <p>C'est le chemin du client, au relachement de la touche : le meme instant que le coup du
     * serveur. Le port ne demande rien au serveur — il tire ses directions dans son propre monde,
     * qui est le meme.
     */
    public static void sprayFor(Skill skill) {
        if (skill != VecmanipCategory.BLOOD_RETROGRADE) return;
        Player player = Minecraft.getInstance().player;
        if (player != null) spray(player);
    }
}
