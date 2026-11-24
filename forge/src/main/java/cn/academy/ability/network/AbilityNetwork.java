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

        // Le terminal voyage sur le meme canal : c'est aussi une donnee de joueur,
        // et un second canal pour un drapeau et une liste de noms ne gagnerait rien.
        CHANNEL.registerMessage(nextId++, cn.academy.terminal.network.SyncTerminalDataPacket.class,
                cn.academy.terminal.network.SyncTerminalDataPacket::encode,
                cn.academy.terminal.network.SyncTerminalDataPacket::decode,
                cn.academy.terminal.network.SyncTerminalDataPacket::handle);
        // Les tutoriels ouverts voyagent aussi par la : le client ne peut pas les deviner,
        // puisqu'un tutoriel reste ouvert meme apres avoir range son bloc.
        CHANNEL.registerMessage(nextId++, cn.academy.terminal.tutorial.network.SyncTutorialDataPacket.class,
                cn.academy.terminal.tutorial.network.SyncTutorialDataPacket::encode,
                cn.academy.terminal.tutorial.network.SyncTutorialDataPacket::decode,
                cn.academy.terminal.tutorial.network.SyncTutorialDataPacket::handle);
        // Les morceaux du lecteur media, pour la meme raison : c'est le serveur qui decide
        // ce que le joueur possede, et le client qui l'affiche.
        CHANNEL.registerMessage(nextId++, cn.academy.misc.media.network.SyncMediaPacket.class,
                cn.academy.misc.media.network.SyncMediaPacket::encode,
                cn.academy.misc.media.network.SyncMediaPacket::decode,
                cn.academy.misc.media.network.SyncMediaPacket::handle);

        // Les prereglages : leur etat descend au client, et les deux gestes qui les
        // modifient remontent — changer de prereglage, poser une competence sur une touche.
        CHANNEL.registerMessage(nextId++, cn.academy.ability.preset.network.SyncPresetPacket.class,
                cn.academy.ability.preset.network.SyncPresetPacket::encode,
                cn.academy.ability.preset.network.SyncPresetPacket::decode,
                cn.academy.ability.preset.network.SyncPresetPacket::handle);
        CHANNEL.registerMessage(nextId++, cn.academy.ability.preset.network.PresetActionPacket.class,
                cn.academy.ability.preset.network.PresetActionPacket::encode,
                cn.academy.ability.preset.network.PresetActionPacket::decode,
                cn.academy.ability.preset.network.PresetActionPacket::handle);

        // Et la liste blanche du brouilleur : la seule chose de cette machine qu'un identifiant de
        // bouton ne peut pas porter, parce que c'est un nom de joueur et non un nombre.
        CHANNEL.registerMessage(nextId++, cn.academy.energy.network.InterfererWhitelistPacket.class,
                cn.academy.energy.network.InterfererWhitelistPacket::encode,
                cn.academy.energy.network.InterfererWhitelistPacket::decode,
                cn.academy.energy.network.InterfererWhitelistPacket::handle);
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
