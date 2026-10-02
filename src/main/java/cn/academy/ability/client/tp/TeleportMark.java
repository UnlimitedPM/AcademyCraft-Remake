package cn.academy.ability.client.tp;

import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.teleporter.PenetrateTeleportSkill;
import cn.academy.ability.teleporter.TeleporterCategory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * La marque de teleportation : le fantome qui montre ou l'on va atterrir.
 *
 * <p>C'est le portage d'{@code EntityTPMarking} et de ses trois contextes clients — la
 * teleportation au marqueur, celle qui traverse les murs, et le scintillement. L'original en
 * faisait une <b>entite cliente</b> : le contexte la posait chez son tireur, la deplacait a chaque
 * tick sur la destination du moment, et la tuait a la fin du maintien. Le port n'a pas d'entites
 * d'effet, donc elle vit ici — une position, une orientation, un age — et
 * {@code TpMarkRenderer} la dessine.
 *
 * <h2>Une seule destination, mais trois facons de la trouver</h2>
 *
 * <p>Chaque competence dit la sienne, et c'est la <b>meme</b> fonction que celle qui deplacera le
 * joueur : {@code MarkTeleportSkill.destination}, {@code FlashingSkill.destination} et
 * {@code PenetrateTeleportSkill.destination}. Il n'y a donc pas deux geometries a tenir d'accord,
 * et le fantome ne peut pas mentir sur l'endroit ou l'on arrive — c'est tout l'interet de la
 * marque.
 *
 * <p>Le scintillement ne montre rien tant qu'aucune touche de direction n'est enfoncee : chez
 * l'original, c'etait la touche elle-meme qui allumait l'anneau, et son relachement qui faisait
 * partir le saut. Une fois partie, la marque s'efface et le fantome disparait avec elle.
 *
 * <p>Le saut traversant, lui, se pose a <b>hauteur d'yeux</b> de sa destination, et il sait si
 * l'on peut y atterrir : dans un mur, le fantome devient rouge et le saut sera refuse. C'est
 * l'original, qui lisait son {@code dest.available} a chaque tick.
 *
 * <h2>Son age</h2>
 *
 * <p>La marque vit tant que la competence vit, et son age ne sert qu'a une chose : le defilement
 * de ses sept images. Il repart de zero a chaque nouvelle marque, donc deux teleportations de
 * suite ne reprennent pas l'animation ou la precedente l'avait laissee.
 */
@OnlyIn(Dist.CLIENT)
public final class TeleportMark {

    /** La chance qu'a la marque de lacher une etincelle, par tick — l'original : 0,4. */
    public static final double SPARK_CHANCE = 0.4;

    /** La boite ou elles naissent : un bloc de large, de part et d'autre de la marque. */
    public static final double SPARK_RADIUS = 1.0;

    /**
     * Et leur hauteur, sous les pieds de la marque.
     *
     * <p>De moins un bloc quatre a zero, ce qui n'est pas une erreur de signe : l'original posait
     * ses particules a {@code ranged(0,2, 1,6) - 1,6}, un reste de calcul ou la position de la
     * marque etait prise pour une hauteur d'yeux. Le port garde le nombre tel quel — c'est celui
     * qui a ete valide a l'epoque — et il tombe juste : la marque se tient a hauteur de cou, donc
     * ses etincelles se semment autour de son torse.
     */
    public static final double SPARK_LOW = -1.4;
    public static final double SPARK_HIGH = 0.0;

    /** Leur vitesse : trois centimetres par tick, un peu plus vers le haut que vers le bas. */
    public static final double SPARK_SPEED = 0.03;
    public static final double SPARK_RISE = 0.05;

    /** La teinte d'une marque sans rien a signaler : le blanc de l'original. */
    public static final int COLOR_NORMAL = 0xFFFFFFFF;

    /** Et celle d'une marque qui signale une cible ou un obstacle : son rouge. */
    public static final int COLOR_THREATENING = 0xFFFF3333;

    private static final RandomSource RANDOM = RandomSource.create();

    /** La ou la marque se tient, ou {@code null} s'il n'y en a pas. */
    @Nullable
    private static Vec3 position;

    /** L'orientation du tireur, relue au tick comme l'original recopiait sa rotation. */
    private static float yaw;

    /** La couleur de la marque : le fantome en est teinte, la boite en est dessinee. */
    private static int color = COLOR_NORMAL;

    /** Et sa forme : le fantome du joueur, ou une boite aux dimensions demandees. */
    private static Shape shape = Shape.GHOST;

    /** Son age, en ticks : il ne sert qu'au defilement de ses images. */
    private static int ageTicks;

    private TeleportMark() {
    }

    /**
     * Un tick de marque, si c'est bien une competence de teleportation qui tient la touche.
     *
     * <p>Rien ne se passe pour les autres : la marque s'efface, ce qui evite qu'un fantome reste
     * plante dans le decor quand le joueur change de competence sans relacher.
     */
    public static void tick(Player player, Skill skill, int chargeTicks, int aimed) {
        Seat seat = seat(player, skill, chargeTicks, aimed);
        if (seat == null) {
            end();
            return;
        }

        // Une marque qui nait : son age repart de zero, donc son animation aussi.
        if (position == null) ageTicks = 0;
        position = seat.position();
        color = seat.color();
        shape = seat.shape();
        yaw = player.getYRot();
        sowSpark();
        ageTicks++;
    }

    /** La competence est finie : le fantome s'en va. */
    public static void end() {
        position = null;
        ageTicks = 0;
        color = COLOR_NORMAL;
        shape = Shape.GHOST;
    }

    /** Ou la marque se tient, ou {@code null}. Lu par le rendu, qui n'a rien d'autre a savoir. */
    @Nullable
    public static Vec3 position() {
        return position;
    }

    /** L'orientation du tireur, en degres. */
    public static float yaw() {
        return yaw;
    }

    /** La couleur de la marque, ARGB. */
    public static int color() {
        return color;
    }

    /** Sa forme : le fantome du joueur, ou une boite a ses dimensions. */
    public static Shape shape() {
        return shape;
    }

    /** L'age de la marque, en ticks. */
    public static int ageTicks() {
        return ageTicks;
    }

    /**
     * La forme de la marque.
     *
     * <p>Deux formes dans l'original, et deux seulement. Le <b>fantome</b> du joueur, pour les
     * quatre competences qui teleportent le corps : lui seul a une silhouette, et le modele la lui
     * donne — d'ou une taille nulle ici. Et la <b>boite</b> de son {@code EntityMarker}, pour les
     * deux competences qui visent autre chose : la ou l'objet tombera, et la creature a qui il
     * arrachera les chairs. Ses dimensions sont alors celles que la competence demande — un bloc
     * pour du vide, la boite de la creature pour elle.
     */
    public record Shape(double width, double height) {

        /** Le fantome du joueur : c'est le modele qui lui donne sa taille, pas la marque. */
        public static final Shape GHOST = new Shape(0.0, 0.0);

        /** Une boite cubique, comme le {@code marker.width = marker.height} de l'original. */
        public static Shape box(double size) {
            return new Shape(size, size);
        }

        /** Vrai quand c'est une boite : le rendu sait alors quoi dessiner. */
        public boolean isBox() {
            return width > 0.0 && height > 0.0;
        }
    }

    /** Ou la marque se pose, de quelle couleur, et sous quelle forme. */
    public record Seat(Vec3 position, int color, Shape shape) {
    }

    /**
     * Ou la competence en cours emmenerait son joueur, ou {@code null} s'il n'y a pas de marque.
     *
     * <p>Les trois competences a marque n'en veulent pas au meme moment : la teleportation au
     * marqueur en montre une pendant toute sa charge — sa portee grandit avec elle — le
     * scintillement seulement tant qu'une touche de direction est enfoncee ({@code aimed}, zero
     * quand il n'y en a pas), et le saut traversant tant que sa touche se tient, a la distance que
     * la molette lui a donnee.
     */
    @Nullable
    static Seat seat(Player player, Skill skill, int chargeTicks, int aimed) {
        if (skill == TeleporterCategory.MARK_TELEPORT) {
            return new Seat(TeleporterCategory.MARK_TELEPORT.destination(player,
                    ClientAbilityData.get(), chargeTicks), COLOR_NORMAL, Shape.GHOST);
        }
        if (skill == TeleporterCategory.FLASHING && aimed != 0) {
            return new Seat(TeleporterCategory.FLASHING.destination(player, ClientAbilityData.get(),
                    aimed), COLOR_NORMAL, Shape.GHOST);
        }
        // Le saut court vise pendant tout son maintien, comme les deux precedents : le fantome se
        // pose la ou le saut deposerait son joueur, et c'est la meme fonction qui l'y deposera.
        if (skill == TeleporterCategory.SHIFT_TELEPORT) {
            return new Seat(TeleporterCategory.SHIFT_TELEPORT.destination(player,
                    ClientAbilityData.get()), COLOR_NORMAL, Shape.GHOST);
        }
        // Le lancer d'objet, lui, ne montre pas un fantome mais une BOITE, comme le marqueur de
        // l'original : un cube d'un demi-bloc, gris tant qu'il n'y a rien devant, rouge des
        // qu'une creature se trouve sous le geste. C'est la couleur qui porte l'information, et
        // non plus un drapeau : l'original avait ses deux teintes a lui.
        if (skill == TeleporterCategory.THREATENING_TELEPORT) {
            boolean threatens = TeleporterCategory.THREATENING_TELEPORT.threatens(player,
                    ClientAbilityData.get());
            return new Seat(TeleporterCategory.THREATENING_TELEPORT.dropPosition(player,
                    ClientAbilityData.get()),
                    threatens ? 0xBAB2232A : 0xBABABABA, Shape.box(0.5));
        }
        if (skill == TeleporterCategory.PENETRATE_TELEPORT && TeleportAim.active()) {
            PenetrateTeleportSkill.Destination destination =
                    TeleporterCategory.PENETRATE_TELEPORT.destination(player,
                            ClientAbilityData.get(), TeleportAim.distance());
            // Le fantome se tient a hauteur d'YEUX de sa destination, comme l'original : la
            // destination est un point aux pieds, et le modele d'un joueur a son origine au cou.
            return new Seat(destination.position().add(0, player.getEyeHeight(), 0),
                    destination.available() ? COLOR_NORMAL : COLOR_THREATENING, Shape.GHOST);
        }
        return null;
    }

    /** Les etincelles que la marque semme autour d'elle, une fois par tick sur deux et demie. */
    private static void sowSpark() {
        if (position == null || RANDOM.nextDouble() >= SPARK_CHANCE) return;

        Vec3 at = new Vec3(
                position.x + spread(SPARK_RADIUS),
                position.y + SPARK_LOW + RANDOM.nextDouble() * (SPARK_HIGH - SPARK_LOW),
                position.z + spread(SPARK_RADIUS));
        Vec3 velocity = new Vec3(spread(SPARK_SPEED),
                RANDOM.nextDouble() * SPARK_RISE, spread(SPARK_SPEED));
        TpParticles.spawn(at, velocity);
    }

    /** Un decalage tire dans les deux sens. */
    private static double spread(double bound) {
        return (RANDOM.nextDouble() * 2 - 1) * bound;
    }
}
