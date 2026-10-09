package cn.academy.ability.client;

import cn.academy.ability.client.arc.ArcPattern;
import cn.academy.ability.client.arc.ArcRenderer;
import cn.academy.ability.client.arc.SurroundArcs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les eclairs qui sortent du sol apres un claquement d'orage.
 *
 * <h2>Un ajout, pas un portage</h2>
 *
 * <p>L'original n'avait rien de tel : sa foudre etait celle de Minecraft — un seul eclair, au point
 * d'impact — alors que ses degats, eux, emportaient tout un <b>rayon</b> de quinze a trente blocs
 * (`ctx.attackRange`). Le joueur a dit exactement ce que cela donne : « on voit l'eclair vanilla de
 * minecraft mais les monstres alentour prennent quand meme des degats pour aucune raison
 * apparente ». Ces eclairs-ci sont donc la pour <b>montrer la portee</b> : une poignee de petites
 * decharges qui jaillissent du sol dans tout le rayon, « comme si la foudre s'etait propagee dans le
 * sol ».
 *
 * <h2>C'est un CARRE, pas un rond</h2>
 *
 * <p>La question du joueur — « je dis 360o mais je sais pas si c'est une forme de rond ou de
 * carree » — a une reponse, et c'est le code qui la donne : les degats de la competence prennent
 * tout ce qui vit dans une <b>boite</b> de cote deux fois le rayon ({@code new AABB(impact,
 * impact).inflate(range)}), donc un carre vu de dessus, et non un disque. Le semis couvre donc le
 * carre : ses quatre coins compris. S'en tenir au disque inscrit laissait mourir des monstres dans
 * les coins sans qu'aucun eclair n'y soit jamais sorti — exactement le defaut que ces eclairs-la
 * sont venus corriger.
 *
 * <h2>Une grille, et non un tirage libre</h2>
 *
 * <p>Seize positions tirees au hasard dans le carre laissent des trous et des paquets : le joueur
 * a demande « un peu plus d'eclairs repartis sur les 360o », et c'est cette repartition-la qui
 * manquait. Le carre est donc decoupe en {@link #GRID} x {@link #GRID} cellules, et chaque cellule
 * en recoit <b>un</b>, pose au hasard dedans : la grille est reguliere, le semis ne l'est pas. Aucune
 * direction n'est vide, et il n'y a jamais deux eclairs au meme endroit.
 *
 * <h2>La vague</h2>
 *
 * <p>Elles ne sortent pas toutes ensemble : chacune attend d'autant plus longtemps qu'elle est
 * <b>loin</b> du point d'impact — {@link #SPREAD_TICKS} ticks au bord du carre, rien au centre. La
 * distance qui compte est celle du <b>carre</b> (le plus grand des deux ecarts, celui des quatre
 * bords), et l'onde se lit donc comme un carre qui s'ouvre — pas comme un rond, qui n'aurait rien a
 * voir avec la zone frappee. L'oeil lit une propagation du centre vers l'exterieur, et non un semis
 * qui s'allume d'un coup : c'est ce que demandait le joueur, et c'est ce qui donne sa taille a
 * l'attaque.
 *
 * <h2>Ou elles sortent</h2>
 *
 * <p>Sur le <b>sol</b>, et pas dans l'air : pour chaque point tire, on descend une colonne depuis le
 * niveau de l'impact ({@link #SEARCH_DEPTH} blocs au plus) jusqu'a la premiere face tournee vers le
 * haut. Une colonne qui n'en trouve pas — un impact en plein ciel, une falaise sous laquelle on a
 * tire — ne donne rien du tout : mieux vaut quatre eclairs justes que trente-six qui flottent dans
 * le vide.
 *
 * <p>Le vrai <b>tirage</b> est dans {@link #rolls}, qui ne connait ni monde ni bloc : c'est la seule
 * partie de cet effet qu'un test puisse relire, et c'est aussi celle qui porte les deux regles — la
 * grille, et la vague qui part du centre.
 */
@OnlyIn(Dist.CLIENT)
public final class GroundArcs {

    /**
     * La grille du semis : {@link #GRID} x {@link #GRID} eclairs, un par cellule.
     *
     * <p>Six de cote, donc trente-six eclairs — un peu plus du double de la premiere version, qui en
     * posait seize au hasard. C'est ce qu'il fallait pour que les 360o soient tenus : a seize, un
     * tirage libre laissait des quartiers entiers sans rien.
     */
    public static final int GRID = 6;

    /** Le nombre d'eclairs d'un claquement : une cellule, un eclair. */
    public static final int COUNT = GRID * GRID;

    /** Leur penchant lateral, en blocs : ils ne sortent pas tous bien droits. */
    public static final double TILT_MAX = 0.3;

    /** Le retard du bord du carre, en ticks : c'est la duree de la vague. */
    public static final int SPREAD_TICKS = 6;

    /** La vie d'un eclair, en ticks : plus longue que celle d'un gresillement, pour qu'on la voie. */
    private static final int LIFE_TICKS = 6;

    /** La profondeur de la recherche du sol, en blocs sous le niveau de l'impact. */
    private static final int SEARCH_DEPTH = 6;

    /**
     * Le motif, et les longueurs : ceux du gresillement de la charge de ce meme orage.
     *
     * <p>C'est {@link SurroundArcs#BOLD}, l'electricite qui s'amassait dans la main pendant la
     * charge : la foudre qui sort du sol est la meme, et le joueur la reconnait. Ses bornes de
     * longueur sont celles de ce gabarit — de 1,05 a 1,35 bloc —, et c'est aussi loin que porte son
     * motif : demander plus long ne dessinerait rien de plus, voir {@code ArcMesh}.
     */
    private static final ArcPattern PATTERN = SurroundArcs.BOLD.pattern();
    private static final double LENGTH_MIN = SurroundArcs.BOLD.minLength();
    private static final double LENGTH_MAX = SurroundArcs.BOLD.maxLength();

    private static final Random RANDOM = new Random();

    /** Les eclairs qui attendent leur tour — voir {@link #tick}. */
    private static final List<Pending> PENDING = new ArrayList<>();

    private GroundArcs() {
    }

    /**
     * Un tirage, sans son sol : ou il tombe dans le carre — deux fractions du rayon, entre moins
     * un et un —, sa hauteur, son penchant, et le retard de sa sortie.
     */
    public record Shot(double offsetX, double offsetZ, double length, double tilt, int delay) {}

    /** Un eclair pret a sortir : ses deux bouts, et les ticks qu'il attend encore. */
    private static final class Pending {
        private final Vec3 from;
        private final Vec3 to;
        private int delay;

        Pending(Vec3 from, Vec3 to, int delay) {
            this.from = from;
            this.to = to;
            this.delay = delay;
        }
    }

    /**
     * La foudre vient de tomber : seme ses eclairs de sol dans tout le rayon.
     *
     * <p>Appele par le paquet de la competence, chez tous ceux qui voient le lanceur — c'est la
     * meme circulation que les arcs de degats, voir {@code ThunderClapGroundPacket}.
     */
    public static void burst(Vec3 impact, double radius) {
        Level level = Minecraft.getInstance().level;
        if (level == null || radius <= 0) return;

        for (Shot shot : rolls(RANDOM)) {
            Vec3 ground = surface(level, impact.x + shot.offsetX() * radius, impact.y,
                    impact.z + shot.offsetZ() * radius);
            if (ground == null) continue;

            // Il penche vers l'exterieur — du meme cote que celui ou il est par rapport a
            // l'impact —, comme s'il suivait la propagation.
            Vec3 outward = new Vec3(shot.offsetX(), 0, shot.offsetZ()).normalize();
            PENDING.add(new Pending(ground,
                    ground.add(outward.scale(shot.tilt())).add(0, shot.length(), 0),
                    shot.delay()));
        }
    }

    /**
     * Un tick : les eclairs dont le retard est ecoule sortent, les autres attendent.
     *
     * <p>Le compte a rebours est tenu ici plutot qu'a la pose : un eclair ne se pose pas d'avance
     * pour naitre plus tard, et c'est cette liste qui fait la vague. Voir la classe.
     */
    public static void tick() {
        for (int i = PENDING.size() - 1; i >= 0; i--) {
            Pending pending = PENDING.get(i);
            if (pending.delay > 0) {
                pending.delay -= 1;
                continue;
            }
            ArcRenderer.spawn(PATTERN.name(), pending.from, pending.to, LIFE_TICKS, false,
                    ArcRenderer.NO_OWNER);
            PENDING.remove(i);
        }
    }

    /** Tout oublier : un monde quitte n'emporte pas ses eclairs. */
    public static void clear() {
        PENDING.clear();
    }

    /**
     * Les traits d'un claquement, sans le sol : une cellule de la grille chacun.
     *
     * <p>Pur, et c'est tout l'interet : la grille — aucune direction vide — et la vague — un eclair
     * d'autant plus tardif qu'il est loin — se relisent en test, sans monde ni rendu. Le sol, lui,
     * se cherche apres, dans {@link #surface}.
     *
     * <p>Les tirages sont faits dans cet ordre, et ils n'ont pas le droit de bouger : c'est celui des
     * nombres du joueur quand il reglera l'effet a l'oeil.
     */
    public static List<Shot> rolls(Random random) {
        List<Shot> shots = new ArrayList<>(COUNT);
        for (int cellX = 0; cellX < GRID; cellX++) {
            for (int cellZ = 0; cellZ < GRID; cellZ++) {
                // Une position au hasard DANS la cellule, ramenee entre moins un et un — donc une
                // fraction du rayon. C'est ce qui fait que la grille est reguliere sans que le semis
                // le soit.
                double offsetX = edge(cellX, random);
                double offsetZ = edge(cellZ, random);
                double length = LENGTH_MIN + random.nextDouble() * (LENGTH_MAX - LENGTH_MIN);
                double tilt = (random.nextDouble() * 2 - 1) * TILT_MAX;
                int delay = (int) Math.round(square(offsetX, offsetZ) * SPREAD_TICKS);
                shots.add(new Shot(offsetX, offsetZ, length, tilt, delay));
            }
        }
        return shots;
    }

    /**
     * La distance d'un point au centre, <b>pour un carre</b> : le plus grand des deux ecarts.
     *
     * <p>C'est ce qui fait la vague carree — le bord du carre est partout a la meme distance, et
     * non le coin comme le voudrait un cercle.
     */
    public static double square(double offsetX, double offsetZ) {
        return Math.max(Math.abs(offsetX), Math.abs(offsetZ));
    }

    /** Une position tiree dans la cellule {@code cell}, ramenee entre moins un et un. */
    private static double edge(int cell, Random random) {
        return (cell + random.nextDouble()) / GRID * 2 - 1;
    }

    /**
     * La surface du sol sous un point, ou {@code null} s'il n'y en a pas.
     *
     * <p>Une colonne, du niveau de l'impact vers le bas, et la <b>premiere face tournee vers le
     * haut</b> : c'est ce qui pose l'eclair sur la terre, sur une dalle ou sur la neige, et non
     * dedans. Le point rendu est celui de la face frappee, donc un demi-bloc au-dessus d'une dalle
     * — c'est exactement ce qu'un rayon veut dire.
     */
    private static Vec3 surface(Level level, double x, double y, double z) {
        Vec3 from = new Vec3(x, y + 1, z);
        Vec3 to = new Vec3(x, y - SEARCH_DEPTH, z);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        if (hit.getDirection() != Direction.UP) return null;
        return hit.getLocation();
    }
}
