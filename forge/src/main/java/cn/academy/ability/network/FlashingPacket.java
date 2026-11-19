package cn.academy.ability.network;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.teleporter.FlashingSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * C2S: une touche de deplacement a ete relachee pendant un maintien qui les ecoute.
 *
 * <p>Le scintillement est la seule competence du port dont une partie du geste n'est pas sa
 * propre touche : elle s'ouvre sur une touche, puis chaque saut se demande avec W, A, S ou D.
 * Le client ne peut donc pas passer par {@code ActivateSkillPacket}, qui ne connait que
 * l'appui et le relachement de la touche d'une competence.
 *
 * <p>Le paquet ne porte qu'une direction. Tout le reste — le paiement, la destination, le
 * deplacement — se decide sur le serveur, qui seul connait le monde : le client ne fait que
 * dire quelle touche a ete relachee.
 */
public class FlashingPacket {

    private final int categoryId;
    private final int skillId;
    private final int direction;

    public FlashingPacket(int categoryId, int skillId, int direction) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.direction = direction;
    }

    public static void encode(FlashingPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.categoryId);
        buf.writeVarInt(msg.skillId);
        buf.writeVarInt(msg.direction);
    }

    public static FlashingPacket decode(FriendlyByteBuf buf) {
        return new FlashingPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FlashingPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Category category = CategoryManager.INSTANCE.getCategory(msg.categoryId);
            if (category == null) return;
            Skill skill = category.getSkill(msg.skillId);
            if (skill == null) return;
            // Une direction qui n'existe pas ne vient pas de ce mod : on la jette plutot que
            // de la laisser atteindre la competence.
            if (!FlashingSkill.isDirection(msg.direction)) return;

            player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> handle(player, data, skill, msg.direction));
        });
        ctx.setPacketHandled(true);
    }

    private static void handle(ServerPlayer player, AbilityData data, Skill skill, int direction) {
        // Le message de l'original n'existait que pour un contexte vivant : sans maintien
        // ouvert, il n'y a personne pour l'ecouter. C'est la meme chose ici, et c'est ce qui
        // empeche un client bricole de sauter sans avoir jamais ouvert la competence.
        if (!data.isCharging(skill)) return;

        skill.onHoldAction(player, data, direction);

        // Le saut a depense des points : le client doit le voir tout de suite, sinon il
        // croit pouvoir enchainer alors que sa reserve est vide.
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityDataPacket(data));
    }
}
