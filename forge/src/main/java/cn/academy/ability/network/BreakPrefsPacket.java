package cn.academy.ability.network;

import java.util.function.Supplier;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.BlockBreaking;
import cn.academy.ability.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Le choix du joueur sur la casse des blocs.
 *
 * <p>Le réglage est une décision de <b>jeu</b> — il fait disparaître des blocs — donc c'est le
 * serveur qui le tient : le client demande, le serveur écrit dans l'{@link
 * cn.academy.ability.AbilityData} du joueur et renvoie l'état complet. Le client n'écrit jamais
 * dans son exemplaire, il ne peut donc pas afficher une case cochée que le serveur ignore —
 * même règle que pour l'allumage de l'aptitude et les préréglages.
 *
 * <p>{@code skill} vide vaut « le drapeau général » ; sinon c'est la clé d'une compétence de
 * {@link BlockBreaking#prefSkills()} — « meltdowner.mine_ray_basic », par exemple.
 */
public class BreakPrefsPacket {

    private final String skill;
    private final boolean value;

    public BreakPrefsPacket(String skill, boolean value) {
        this.skill = skill == null ? "" : skill;
        this.value = value;
    }

    public static void encode(BreakPrefsPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skill);
        buf.writeBoolean(msg.value);
    }

    public static BreakPrefsPacket decode(FriendlyByteBuf buf) {
        return new BreakPrefsPacket(buf.readUtf(), buf.readBoolean());
    }

    public static void handle(BreakPrefsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
                if (msg.skill.isEmpty()) {
                    data.setPlayerBreaksBlocks(msg.value);
                } else {
                    Skill target = BlockBreaking.byKey(msg.skill);
                    // Une clé inconnue vient d'un client plus vieux ou plus jeune que le serveur :
                    // on la laisse tomber plutôt que de deviner.
                    if (target == null) return;
                    data.setBreaksBlocks(target, msg.value);
                }
                AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncAbilityDataPacket(data));
            });
        });
        ctx.setPacketHandled(true);
    }
}
