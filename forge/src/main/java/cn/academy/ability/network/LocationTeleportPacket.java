package cn.academy.ability.network;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.teleporter.LocationMark;
import cn.academy.ability.teleporter.LocationTeleportSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * C2S: une action sur la liste des endroits marques.
 *
 * <p>La teleportation a la marque est la seule competence du port qui ne se declenche pas par
 * sa touche : elle ouvre un ecran, et tout ce qui agit vient de la. Trois gestes y sont
 * possibles — marquer l'endroit ou l'on se trouve, oublier une marque, partir vers une
 * marque — et ils arrivent tous par ici.
 *
 * <p>Le paquet ne porte qu'un geste, un rang et un nom. Le nom vient de la saisie du joueur,
 * donc il est nettoye sur le serveur ({@code LocationMark.cleanName}) : un client bricole
 * pourrait sinon remplir l'ecran de mille caracteres.
 *
 * <p>Chaque geste renvoie la liste a jour, parce que le client ne saurait pas la deviner, et
 * que c'est elle qui alimente l'ecran.
 */
public class LocationTeleportPacket {

    /** Ce que le joueur a demande depuis l'ecran. */
    public enum Action {
        /** Marquer l'endroit ou il se trouve, sous ce nom. */
        ADD,
        /** Oublier la marque de ce rang. */
        REMOVE,
        /** Partir vers la marque de ce rang. */
        PERFORM
    }

    private final int categoryId;
    private final int skillId;
    private final Action action;
    private final int markId;
    private final String name;

    public LocationTeleportPacket(int categoryId, int skillId, Action action, int markId, String name) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.action = action;
        this.markId = markId;
        this.name = name == null ? "" : name;
    }

    public static void encode(LocationTeleportPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.categoryId);
        buf.writeVarInt(msg.skillId);
        buf.writeByte(msg.action.ordinal());
        buf.writeVarInt(msg.markId);
        buf.writeUtf(msg.name, 64);
    }

    public static LocationTeleportPacket decode(FriendlyByteBuf buf) {
        int categoryId = buf.readVarInt();
        int skillId = buf.readVarInt();
        int action = buf.readByte();
        Action[] values = Action.values();
        return new LocationTeleportPacket(categoryId, skillId,
                action >= 0 && action < values.length ? values[action] : Action.ADD,
                buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(LocationTeleportPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Category category = CategoryManager.INSTANCE.getCategory(msg.categoryId);
            if (category == null) return;
            Skill skill = category.getSkill(msg.skillId);
            if (!(skill instanceof LocationTeleportSkill teleport)) return;

            player.getCapability(AbilityCapability.ABILITY_DATA)
                    .ifPresent(data -> handle(player, data, teleport, msg));
        });
        ctx.setPacketHandled(true);
    }

    private static void handle(ServerPlayer player, AbilityData data,
                               LocationTeleportSkill skill, LocationTeleportPacket msg) {
        // La competence doit etre apprise : c'est l'ecran qui l'a ouverte, mais un client
        // bricole peut envoyer n'importe quoi.
        if (!data.isSkillLearned(skill)) return;

        switch (msg.action) {
            case ADD -> data.addMark(msg.name, LocationMark.of(player.level()),
                    player.getX(), player.getY(), player.getZ());
            case REMOVE -> data.removeMark(msg.markId);
            case PERFORM -> skill.perform(player, data, msg.markId);
        }

        // La liste a change, ou la reserve a ete depensee : le client doit le voir.
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncAbilityDataPacket(data));
    }
}
