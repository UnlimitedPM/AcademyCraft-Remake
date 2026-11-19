package cn.academy.ability.network;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.teleporter.PenetrateTeleportSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S : la distance que le joueur vient de donner a son saut traversant.
 *
 * <p>Le saut traversant est la seule teleportation du port qui se regle, et le reglage est chez le
 * client : c'est lui qui a la molette. La <b>destination</b>, elle, reste au serveur — c'est lui
 * qui connait le monde, et c'est lui qui fait le saut. Il faut donc que le nombre fasse le
 * voyage, et c'est ce paquet qui le porte, un cran a la fois.
 *
 * <p>L'original envoyait sa distance une seule fois, au relachement, avec son message d'execution.
 * Le port la fait voyager a chaque cran pour une raison simple : le paquet de relachement du port
 * ne porte rien, et en inventer un second qui ne serve qu'a ca reviendrait au meme. Un cran de
 * molette est rare — quelques-uns par seconde au plus — donc ce n'est rien a porter.
 *
 * <p>Il n'est ecoute que pendant un maintien ouvert, comme celui du scintillement : sans
 * competence ouverte, il n'y a personne pour le lire, et un client bricole ne peut donc pas se
 * donner une distance sans avoir jamais tenu la touche.
 */
public class TeleportDistancePacket {

    private final int categoryId;
    private final int skillId;
    private final float distance;

    public TeleportDistancePacket(int categoryId, int skillId, float distance) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.distance = distance;
    }

    public static void encode(TeleportDistancePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.categoryId);
        buf.writeVarInt(msg.skillId);
        buf.writeFloat(msg.distance);
    }

    public static TeleportDistancePacket decode(FriendlyByteBuf buf) {
        return new TeleportDistancePacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    /** La categorie visee. Lisible par le test, qui relit l'aller-retour. */
    public int categoryId() {
        return categoryId;
    }

    /** La competence visee. */
    public int skillId() {
        return skillId;
    }

    /** La distance visee, en blocs. */
    public float distance() {
        return distance;
    }

    public static void handle(TeleportDistancePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Category category = CategoryManager.INSTANCE.getCategory(msg.categoryId);
            if (category == null) return;
            Skill skill = category.getSkill(msg.skillId);
            if (!(skill instanceof PenetrateTeleportSkill)) return;

            player.getCapability(AbilityCapability.ABILITY_DATA)
                    .ifPresent(data -> handle(data, skill, msg.distance));
        });
        ctx.setPacketHandled(true);
    }

    private static void handle(AbilityData data, Skill skill, float distance) {
        // Sans maintien ouvert, il n'y a personne pour lire ce nombre — et c'est aussi ce qui
        // empeche un client bricole de regler un saut qu'il n'a jamais ouvert.
        if (!data.isCharging(skill)) return;

        // Le reglage reste dans les bornes de la competence, quoi qu'ait envoye le client : un
        // saut plus long que la portee maximale n'existe pas.
        float max = (float) ((PenetrateTeleportSkill) skill).maxDistance(data);
        data.setHoldDistance(skill,
                (float) Math.min(max, Math.max(PenetrateTeleportSkill.MIN_DISTANCE, distance)));
    }
}
