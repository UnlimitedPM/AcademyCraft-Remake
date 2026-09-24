package cn.academy.ability.network;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * L'allumage et l'extinction de l'aptitude.
 *
 * <p>Portage de {@code MSG_ACTIVATE_SVR} : le client demande, le serveur decide et renvoie
 * l'etat complet. Le client n'ecrit jamais dans son exemplaire, il n'a donc aucun moyen de
 * diverger — meme regle que pour les prereglages.
 *
 * <p>L'etat commande tout le reste : {@code canUseAbility} refuse tant qu'il est eteint, et le
 * HUD ne s'affiche pas.
 */
public class ToggleAbilityPacket {

    private final boolean activated;

    public ToggleAbilityPacket(boolean activated) {
        this.activated = activated;
    }

    public static void encode(ToggleAbilityPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.activated);
    }

    public static ToggleAbilityPacket decode(FriendlyByteBuf buf) {
        return new ToggleAbilityPacket(buf.readBoolean());
    }

    public static void handle(ToggleAbilityPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
                data.setActivated(msg.activated);
                AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncAbilityDataPacket(data));
            });
        });
        ctx.setPacketHandled(true);
    }
}
