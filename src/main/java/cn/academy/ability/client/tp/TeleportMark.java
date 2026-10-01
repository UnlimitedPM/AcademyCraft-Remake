package cn.academy.ability.client.tp;

import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
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
 * joueur : {@code MarkTeleportSkill.destination} pour la premiere, {@code FlashingSkill} pour le
 * scintillement. Il n'y a donc pas deux geometries a tenir d'accord, et le fantome ne peut pas
 * mentir sur l'endroit ou l'on arrive — c'est tout l'interet de la marque.
 *
 * <p>Le scintillement ne montre rien tant qu'aucune touche de direction n'est enfoncee : chez
 * l'original, c'etait la touche elle-meme qui allumait l'anneau, et son relachement qui faisait
 * partir le saut. Une fois partie, la marque s'efface et le fantome disparait avec elle.
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
     * qui a ete valide a l'epoque — et il tombe juste : la destination d'un atterrissage sur le
     * sol est elle-meme surelevee de 1,8 bloc, donc les etincelles se semment dans le vide entre
     * le sol et le fantome.
     */
    public static final double SPARK_LOW = -1.4;
    public static final double SPARK_HIGH = 0.0;

    /** Leur vitesse : trois centimetres par tick, un peu plus vers le haut que vers le bas. */
    public static final double SPARK_SPEED = 0.03;
    public static final double SPARK_RISE = 0.05;

    private static final RandomSource RANDOM = RandomSource.create();

    /** La ou la marque se tient, ou {@code null} s'il n'y en a pas. */
    @Nullable
    private static Vec3 position;

    /** L'orientation du tireur, relue au tick comme l'original recopiait sa rotation. */
    private static float yaw;

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
        Vec3 destination = destination(player, skill, chargeTicks, aimed);
        if (destination == null) {
            end();
            return;
        }

        // Une marque qui nait : son age repart de zero, donc son animation aussi.
        if (position == null) ageTicks = 0;
        position = destination;
        yaw = player.getYRot();
        sowSpark();
        ageTicks++;
    }

    /** La competence est finie : le fantome s'en va. */
    public static void end() {
        position = null;
        ageTicks = 0;
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

    /** L'age de la marque, en ticks. */
    public static int ageTicks() {
        return ageTicks;
    }

    /**
     * Ou la competence en cours emmenerait son joueur, ou {@code null} s'il n'y a pas de marque.
     *
     * <p>Les deux competences a marque n'en veulent pas au meme moment : la teleportation au
     * marqueur en montre une pendant toute sa charge — sa portee grandit avec elle — et le
     * scintillement seulement tant qu'une touche de direction est enfoncee. {@code aimed} est
     * cette touche, zero quand il n'y en a pas.
     */
    @Nullable
    static Vec3 destination(Player player, Skill skill, int chargeTicks, int aimed) {
        if (skill == TeleporterCategory.MARK_TELEPORT) {
            return TeleporterCategory.MARK_TELEPORT.destination(player, ClientAbilityData.get(),
                    chargeTicks);
        }
        if (skill == TeleporterCategory.FLASHING && aimed != 0) {
            return TeleporterCategory.FLASHING.destination(player, ClientAbilityData.get(), aimed);
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
