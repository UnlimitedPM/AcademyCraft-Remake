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
                // Apprendre une competence passe par le developpeur : tant qu'elle ne
                // l'est pas, la touche ne fait rien. L'original ne posait meme pas de
                // touche dans ce cas ; le port en pose une par competence, donc il le
                // dit plutot que de rester muet.
                if (!data.isSkillLearned(skill)) {
                    player.displayClientMessage(
                            Component.translatable("academy.ability.not_learned", skill.getDisplayName())
                                    .withStyle(ChatFormatting.RED), true);
                    return;
                }
                if (data.isInterfered()) {
                    player.displayClientMessage(
                            Component.literal("Abilities are jammed here").withStyle(ChatFormatting.RED), true);
                    return;
                }

                // Recharge : l'original tenait un compteur par competence dans
                // CooldownData et refusait le declenchement tant qu'il n'etait pas
                // revenu a zero. Le message dit combien il reste, sinon le joueur
                // n'aurait aucun moyen de savoir si la touche a echoue ou si elle est
                // simplement en attente.
                int cooldown = data.getCooldown(skill);
                if (cooldown > 0) {
                    player.displayClientMessage(
                            Component.translatable("academy.ability.cooldown",
                                            String.format(java.util.Locale.ROOT, "%.1f", cooldown / 20.0f))
                                    .withStyle(ChatFormatting.RED), true);
                    return;
                }

                if (data.consumeControlPoint(skill.getCpCost())) {
                    skill.onActivate(player, data);
                    // La recharge part des que la competence est lancee, comme dans
                    // l'original qui la posait a la fin de son effet.
                    data.setCooldown(skill, skill.getCooldownTicks(data));
                    // Utiliser une competence la fait progresser, et verse de
                    // l'avancement au niveau de la categorie : c'est ce qui fait qu'on
                    // monte en jouant, et non en attendant. L'original versait ces
                    // points depuis chaque competence au moment ou son effet aboutissait ;
                    // ici ils sont verses a l'activation, au montant de base de la
                    // competence — l'ecart est note dans Skill#getExpGain.
                    data.addSkillExp(skill, skill.getExpGain(data));
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
