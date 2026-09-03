package cn.academy.ability.network;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** C2S: player pressed the key bound to a given skill. */
public class ActivateSkillPacket {

    private final int categoryId;
    private final int skillId;

    public ActivateSkillPacket(int categoryId, int skillId) {
        this.categoryId = categoryId;
        this.skillId = skillId;
    }

    public static void encode(ActivateSkillPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.categoryId);
        buf.writeVarInt(msg.skillId);
    }

    public static ActivateSkillPacket decode(FriendlyByteBuf buf) {
        return new ActivateSkillPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ActivateSkillPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Category category = CategoryManager.INSTANCE.getCategory(msg.categoryId);
            if (category == null) return;
            Skill skill = category.getSkill(msg.skillId);
            if (skill == null) return;
            player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
                if (!data.hasLearned(category)) return;
                if (data.consumeControlPoint(skill.getCpCost())) {
                    skill.onActivate(player, data);
                    AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncAbilityDataPacket(data));
                } else {
                    player.displayClientMessage(
                            Component.literal("Not enough Control Points").withStyle(ChatFormatting.RED), true);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
