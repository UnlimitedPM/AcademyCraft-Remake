package cn.academy.ability.network;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Le canal Forge du mod, pour ce qui n'est pas encore commun.
 *
 * <p>La plupart des messages d'aptitude vivent maintenant dans {@code AcademyPackets} et passent
 * par le transport neutre. Restent ici ceux qui n'ont pas encore de version commune : la rafale
 * d'arcs de la main du railgun, les prereglages, le terminal, les tutoriels, les morceaux du
 * lecteur media, la liste blanche du brouilleur et l'emetteur de frequence.
 */
public class AbilityNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "ability_main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    public static void register() {
        // La rafale d'arcs du railgun, que le serveur annonce a ceux qui voient le lanceur —
        // l'effet de main de l'original, celui qui se voit sur les autres joueurs.
        CHANNEL.registerMessage(nextId++, RailgunHandPacket.class,
                RailgunHandPacket::encode,
                RailgunHandPacket::decode,
                RailgunHandPacket::handle);

        // Les prereglages, le terminal, les tutoriels et les medias : leurs paquets sont maintenant
        // communs et voyagent par AcademyPackets. La liste blanche du brouilleur aussi.

        // L'Emetteur de frequence : ses demandes montent, ses reponses redescendent. Ajoutes a la
        // fin, parce que l'identifiant d'un paquet est sa place.
        CHANNEL.registerMessage(nextId++, cn.academy.energy.network.FreqTransmitterRequestPacket.class,
                cn.academy.energy.network.FreqTransmitterRequestPacket::encode,
                cn.academy.energy.network.FreqTransmitterRequestPacket::decode,
                cn.academy.energy.network.FreqTransmitterRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, cn.academy.energy.network.FreqTransmitterReplyPacket.class,
                cn.academy.energy.network.FreqTransmitterReplyPacket::encode,
                cn.academy.energy.network.FreqTransmitterReplyPacket::decode,
                cn.academy.energy.network.FreqTransmitterReplyPacket::handle);
    }
}
