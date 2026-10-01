package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Saut traversant, portage de {@code PenetrateTeleport} : on traverse les murs, sans s'y arreter.
 *
 * <h2>Il se vise avant de partir</h2>
 *
 * <p>Le port sautait d'un coup a sa portee maximale, des l'appui. L'original, lui, se tenait : la
 * touche enfoncee, le fantome se posait de l'autre cote du mur au fur et a mesure qu'on tournait
 * la tete, et <b>la molette</b> rapprochait ou eloignait ce fantome d'un bloc par cran. On
 * relachait pour partir. C'est la seule teleportation du mod qui se regle, et le joueur l'a
 * reclamee : « le premier pouvoir de teleportation me TP directement quand j'appuie, donc je n'ai
 * pas le temps de rester appuye pour voir ou le fantome me mettra ».
 *
 * <p>La distance vit chez le client — c'est lui qui a la molette — et le serveur garde la meme,
 * cran par cran, par {@code TeleportDistancePacket}. Au relachement, c'est lui qui calcule la
 * destination et fait le saut : le client n'a jamais le dernier mot sur ou l'on atterrit.
 *
 * <h2>Ou l'on ressort</h2>
 *
 * <p>Le trajet part des <b>pieds</b> et avance par pas de huit dixiemes de bloc le long du regard.
 * Il compte trois temps, comme l'original : on est dans le vide, on entre dans la matiere, on en
 * ressort — et c'est la sortie qui est la destination. Au-dela, on avance encore quelques pas pour
 * ne pas se coller au mur, et l'on s'arrete au premier bloc qui se presente.
 *
 * <p>Si le trajet finit <b>dans</b> la matiere, il n'y a pas de destination : l'original refusait
 * alors le saut, et son fantome passait au rouge pour le dire. Le port fait pareil — le saut ne
 * coute rien et ne deplace personne, il n'y a simplement rien a faire.
 *
 * <p>La distance est en plus bornee par la reserve : le saut se paie au bloc parcouru, donc viser
 * plus loin que ce qu'on peut payer ne changerait rien. C'est le {@code cplim} de l'original.
 */
public class PenetrateTeleportSkill extends Skill {

    /** La distance minimale ou la molette peut descendre, comme l'original. */
    public static final double MIN_DISTANCE = 0.5;

    /** Le pas du trajet, en blocs : les huit dixiemes de l'original. */
    public static final double STEP = 0.8;

    /**
     * Les pas libres au-dela du mur, avant de s'arreter.
     *
     * <p>L'original en comptait quatre : la destination se pose donc jusqu'a trois blocs et quart
     * apres la sortie, ce qui laisse de la place pour ne pas ressortir le nez dans la pierre.
     */
    public static final int FREE_STEPS = 4;

    public PenetrateTeleportSkill() {
        super("penetrate_teleport", 2);
    }

    /**
     * Portee maximale du saut : de 10 a 35 blocs selon l'experience.
     *
     * Le port se contentait de 6 blocs. Le saut se paie au bloc parcouru, comme chez
     * l'original (14 a 9 par bloc).
     */
    public double maxDistance(AbilityData data) {
        return lerp(10f, 35f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 50 a 30 ticks, soit 2,5 a 1,5 seconde. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(50f, 30f, data.getSkillExp(this));
    }

    /**
     * Rien a l'appui : c'est le saut qui se paie, au bloc parcouru.
     *
     * <p>Chez l'original le prix ne se connaissait qu'une fois la destination trouvee,
     * et il se payait <b>sans verification</b> ({@code consumeWithForce}) : un saut plus
     * long que la reserve la vidait, sans rien refuser. Le port le dit avec
     * {@link #paysOnEffect()}, comme la teleportation au marqueur.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /** C'est le saut qui paie : voir {@link #getCpCost()}. */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    /**
     * L'experience vient de la distance, que seul le saut connait.
     *
     * <p>Le port versait la valeur d'un saut moyen, faute de savoir : le paquet d'activation ne
     * portait pas la distance. Il la connait maintenant, donc c'est l'effet qui verse les 0,00014
     * par bloc de l'original.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /** Le saut se tient : on vise, et on part au relachement. */
    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * A l'ouverture, la distance part de la portee maximale.
     *
     * <p>C'est l'original, et c'est aussi ce qui evite un saut nul : sans ce pose, la distance
     * stockee vaudrait zero et le saut se ferait sur place.
     */
    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHoldDistance(this, (float) maxDistance(data));
    }

    /** Cout par bloc parcouru : 14 a 9, comme l'original. */
    public float cpPerBlock(AbilityData data) {
        return lerp(14f, 9f, data.getSkillExp(this));
    }

    /** Surcout repris de l'original : de 80 a 50 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(80f, 50f, data.getSkillExp(this));
    }

    /**
     * Au relachement, on part — si l'on peut.
     *
     * <p>Rendre {@code false} termine le maintien comme un relachement ordinaire : la recharge se
     * pose, le client est prevenu, et une reserve videe laisse la surcharge la ou elle est.
     */
    @Override
    public boolean onRelease(Player player, AbilityData data, int heldTicks) {
        Destination destination = destination(player, data, data.getHoldDistance(this));
        // Un trajet qui finit dans la pierre ne deplace personne : l'original terminait la, sans
        // rien payer ni rien dire. Le fantome etait deja rouge.
        if (!destination.available()) return false;

        double distance = destination.position().distanceTo(player.position());
        // Portage de consumeWithForce(distance x getConsumption(exp)) : la reserve se vide au
        // pire, elle ne refuse pas le saut.
        data.performForced(cpPerBlock(data) * (float) distance, getOverloadCost(data));

        if (player.isPassenger()) player.stopRiding();
        player.teleportTo(destination.position().x, destination.position().y,
                destination.position().z);
        player.fallDistance = 0;
        // Le son part au relachement dans l'original, juste avant le saut : c'est le meme instant.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP, 0.5f);

        // 0,00014 par bloc parcouru, comme l'original : c'est la distance qui compte.
        data.addSkillExp(this, 0.00014f * (float) distance);
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
        return false;
    }

    /**
     * Ou le saut emmenerait son joueur, et si l'on peut y atterrir.
     *
     * <p>Portage de {@code getDest} : la distance demandee est bornee par la reserve, puis le
     * trajet avance le long du regard jusqu'a ressortir de la matiere.
     */
    public Destination destination(Player player, AbilityData data, double distance) {
        float perBlock = cpPerBlock(data);
        double reach = perBlock <= 0f ? distance
                : Math.min(distance, data.getControlPoint() / perBlock);
        return walk(player.position(), player.getViewVector(1f), reach,
                pos -> occupied(player.level(), pos));
    }

    /**
     * Le trajet, en trois temps : le vide, la matiere, et la sortie.
     *
     * <p>Fonction pure — elle demande a un predicat si un bloc est pris, et rien d'autre — donc
     * verifiable en test la ou on ne peut pas poser de mur.
     *
     * <p>Les trois temps sont ceux de l'original : le premier dit qu'on est dehors, le deuxieme
     * qu'on entre dans la matiere, et le troisieme qu'on en ressort. La destination est cet
     * endroit-la, avance de quelques pas pour ne pas se coller au mur. Si le trajet s'arrete dans
     * le deuxieme temps, c'est qu'on n'est jamais ressorti : il n'y a pas de destination.
     */
    public static Destination walk(Vec3 from, Vec3 look, double distance,
                                   Predicate<BlockPos> occupied) {
        double x = from.x;
        double y = from.y;
        double z = from.z;
        int stage = 0;
        int free = 0;

        for (double step = 0; step <= distance; step += STEP) {
            boolean free2 = !occupied.test(BlockPos.containing(x, y, z))
                    && !occupied.test(BlockPos.containing(x, y + 1, z));
            if (stage == 0) {
                if (!free2) stage = 1;
            } else if (stage == 1) {
                if (free2) stage = 2;
            } else {
                free++;
                if (!free2 || free > FREE_STEPS) break;
            }
            x += STEP * look.x;
            y += STEP * look.y;
            z += STEP * look.z;
        }

        return new Destination(new Vec3(x, y, z), stage != 1);
    }

    /** Un bloc qui arrete le corps : sa forme de collision n'est pas vide. */
    private static boolean occupied(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** Ou l'on atterrit, et si l'endroit est libre. */
    public record Destination(Vec3 position, boolean available) {
    }
}
