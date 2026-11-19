package cn.academy.energy.network;

import java.util.function.Supplier;

import cn.academy.MatrixBlock;
import cn.academy.energy.EnergyGenerator;
import cn.academy.energy.EnergyReceiver;
import cn.academy.energy.MatrixBlockEntity;
import cn.academy.energy.NodeBlockEntity;
import cn.academy.energy.NodeFinder;
import cn.academy.terminal.TerminalCapability;
import cn.academy.terminal.app.AppFreqTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Ce que l'Emetteur de frequence demande au serveur : qui est ce bloc, le mot de passe est-il bon,
 * et raccorde-les.
 *
 * <p>C'est le {@code Syncs} de {@code FreqTransmitterUI}, ses cinq canaux en trois actions. Le
 * serveur refuse tout joueur qui n'a pas installe l'application et tout bloc hors de portee de
 * main : le mot de passe est re-verifie a chaque raccordement, jamais cru sur parole.
 */
public class FreqTransmitterRequestPacket {

    /** Ce que le joueur demande. */
    public enum Action { QUERY, AUTH, LINK }

    /** Le plus loin, en blocs, d'ou le joueur peut viser un bloc. */
    private static final double REACH = 12.0d;

    private final Action action;
    private final BlockPos first;
    private final BlockPos second;
    private final String password;

    public FreqTransmitterRequestPacket(Action action, BlockPos first, BlockPos second,
                                        String password) {
        this.action = action;
        this.first = first;
        this.second = second;
        this.password = password == null ? "" : password;
    }

    public static FreqTransmitterRequestPacket query(BlockPos clicked) {
        return new FreqTransmitterRequestPacket(Action.QUERY, clicked, BlockPos.ZERO, "");
    }

    public static FreqTransmitterRequestPacket auth(BlockPos source, String password) {
        return new FreqTransmitterRequestPacket(Action.AUTH, source, BlockPos.ZERO, password);
    }

    public static FreqTransmitterRequestPacket link(BlockPos source, BlockPos clicked,
                                                    String password) {
        return new FreqTransmitterRequestPacket(Action.LINK, source, clicked, password);
    }

    public static void encode(FreqTransmitterRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeBlockPos(msg.first);
        buf.writeBlockPos(msg.second);
        buf.writeUtf(msg.password, 16);
    }

    public static FreqTransmitterRequestPacket decode(FriendlyByteBuf buf) {
        return new FreqTransmitterRequestPacket(buf.readEnum(Action.class), buf.readBlockPos(),
                buf.readBlockPos(), buf.readUtf(16));
    }

    public static void handle(FreqTransmitterRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !(sender.level() instanceof ServerLevel level)) return;

            boolean installed = sender.getCapability(TerminalCapability.TERMINAL_DATA)
                    .map(data -> data.isInstalled(AppFreqTransmitter.INSTANCE)).orElse(false);
            if (!installed) return;

            switch (msg.action) {
                case QUERY -> query(sender, level, msg);
                case AUTH -> auth(sender, level, msg);
                case LINK -> link(sender, level, msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** Ce qu'est un bloc pour l'Emetteur : 1 un Matrix, 2 un noeud, 0 rien de cela. */
    private record Target(int kind, BlockPos anchor) {}

    private static Target target(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MatrixBlock) {
            return new Target(1, MatrixBlock.anchorOf(pos, state));
        }
        if (level.getBlockEntity(pos) instanceof NodeBlockEntity) return new Target(2, pos);
        return new Target(0, pos);
    }

    private static boolean near(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH * REACH;
    }

    private static void query(ServerPlayer player, ServerLevel level, FreqTransmitterRequestPacket msg) {
        if (!near(player, msg.first)) {
            FreqTransmitterReplyPacket.send(player, Action.QUERY, 0, msg.first, "", false, "e4");
            return;
        }
        Target target = target(level, msg.first);
        BlockEntity be = level.getBlockEntity(target.anchor());
        if (target.kind() == 1) {
            if (!(be instanceof MatrixBlockEntity matrix) || !matrix.isInitialized()) {
                FreqTransmitterReplyPacket.send(player, Action.QUERY, 1, target.anchor(), "",
                        false, "e0");
                return;
            }
            FreqTransmitterReplyPacket.send(player, Action.QUERY, 1, target.anchor(),
                    matrix.getSsid(), !matrix.getPassword().isEmpty(), "ok");
        } else if (target.kind() == 2 && be instanceof NodeBlockEntity node) {
            FreqTransmitterReplyPacket.send(player, Action.QUERY, 2, target.anchor(),
                    node.getNodeName(), !node.getPassword().isEmpty(), "ok");
        } else {
            FreqTransmitterReplyPacket.send(player, Action.QUERY, 0, msg.first, "", false, "e4");
        }
    }

    private static void auth(ServerPlayer player, ServerLevel level, FreqTransmitterRequestPacket msg) {
        Target target = target(level, msg.first);
        String expected = null;
        if (near(player, msg.first)) {
            BlockEntity be = level.getBlockEntity(target.anchor());
            if (target.kind() == 1 && be instanceof MatrixBlockEntity matrix) {
                expected = matrix.getPassword();
            } else if (target.kind() == 2 && be instanceof NodeBlockEntity node) {
                expected = node.getPassword();
            }
        }
        boolean good = expected != null && expected.equals(msg.password);
        FreqTransmitterReplyPacket.send(player, Action.AUTH, target.kind(), target.anchor(), "",
                false, good ? "ok" : "e1");
    }

    private static void link(ServerPlayer player, ServerLevel level, FreqTransmitterRequestPacket msg) {
        Target source = target(level, msg.first);
        String result = "e4";

        if (near(player, msg.first) && near(player, msg.second)) {
            if (source.kind() == 1) {
                Target node = target(level, msg.second);
                if (node.kind() == 2) {
                    NodeFinder.LinkOutcome outcome = NodeFinder.linkMatrix(level, node.anchor(),
                            source.anchor(), msg.password);
                    result = outcome == NodeFinder.LinkOutcome.LINKED ? "ok" : "e2";
                }
            } else if (source.kind() == 2) {
                BlockEntity machine = level.getBlockEntity(msg.second);
                if (machine instanceof EnergyReceiver || machine instanceof EnergyGenerator) {
                    NodeFinder.LinkOutcome outcome = NodeFinder.link(level, msg.second,
                            source.anchor(), msg.password);
                    result = outcome == NodeFinder.LinkOutcome.LINKED ? "ok" : "e3";
                }
            }
        }
        FreqTransmitterReplyPacket.send(player, Action.LINK, source.kind(), source.anchor(), "",
                false, result);
    }
}
