package cn.academy.ability.network;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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
        CHANNEL.registerMessage(nextId++, ActivateSkillPacket.class,
                ActivateSkillPacket::encode, ActivateSkillPacket::decode, ActivateSkillPacket::handle);
        CHANNEL.registerMessage(nextId++, FlashingPacket.class,
                FlashingPacket::encode, FlashingPacket::decode, FlashingPacket::handle);
        // La distance du saut traversant : le client a la molette, le serveur a le monde, et
        // c'est lui qui saute — il lui faut donc ce nombre. Voir TeleportDistancePacket.
        CHANNEL.registerMessage(nextId++, TeleportDistancePacket.class,
                TeleportDistancePacket::encode, TeleportDistancePacket::decode,
                TeleportDistancePacket::handle);
        CHANNEL.registerMessage(nextId++, LocationTeleportPacket.class,
                LocationTeleportPacket::encode, LocationTeleportPacket::decode, LocationTeleportPacket::handle);
        CHANNEL.registerMessage(nextId++, ToggleAbilityPacket.class,
                ToggleAbilityPacket::encode, ToggleAbilityPacket::decode, ToggleAbilityPacket::handle);
        // Ce que le joueur accepte de casser : le serveur le tient, parce que c'est lui qui
        // fait disparaitre les blocs. Voir BreakPrefsPacket.
        CHANNEL.registerMessage(nextId++, BreakPrefsPacket.class,
                BreakPrefsPacket::encode, BreakPrefsPacket::decode, BreakPrefsPacket::handle);
        // La detection de minerais est la seule competence qui ait besoin de dire quelque
        // chose au client : c'est lui qui balaie le monde et l'allume.
        CHANNEL.registerMessage(nextId++, MineDetectPacket.class,
                MineDetectPacket::encode, MineDetectPacket::decode, MineDetectPacket::handle);
        CHANNEL.registerMessage(nextId++, cn.academy.ability.network.MineDetectStopPacket.class,
                cn.academy.ability.network.MineDetectStopPacket::encode,
                cn.academy.ability.network.MineDetectStopPacket::decode,
                cn.academy.ability.network.MineDetectStopPacket::handle);
        // Les eclairs, le renfort du corps et les eclairs du sol sont communs desormais : voir
        // AcademyPackets. Ce qui reste ici est Forge : les rayons du meltdowner, l'anneau du
        // missile, les maintiens.
        // Les rayons du meltdowner, comme les arcs et le reste des effets, sont communs : voir
        // AcademyPackets. Leur dessin reste cote client, derriere PlatformHooks.spawnRayBeam.
        // La marque de radiation et son fumee sont communes desormais : voir AcademyPackets.
        // Les sept effets d'onde, de trainee, de critique, de sang, de fumee, de taches et de tir
        // de plasma sont communs desormais : voir AcademyPackets.
        // L'anneau de plasma du missile et le depart du reacteur sont communs : voir
        // AcademyPackets (md_missile, jet_flight).
        // Et la fin d'un maintien, refusee ou terminee : le client tenait le sien depuis l'appui
        // de la touche, il doit le fermer. Voir HoldOverPacket.
        CHANNEL.registerMessage(nextId++, HoldOverPacket.class,
                HoldOverPacket::encode, HoldOverPacket::decode, HoldOverPacket::handle);
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
        // Et la rafale d'arcs du railgun, que le serveur annonce a ceux qui voient le lanceur —
        // l'effet de main de l'original, celui qui se voit sur les autres joueurs.
        CHANNEL.registerMessage(nextId++, cn.academy.ability.network.RailgunHandPacket.class,
                cn.academy.ability.network.RailgunHandPacket::encode,
                cn.academy.ability.network.RailgunHandPacket::decode,
                cn.academy.ability.network.RailgunHandPacket::handle);
        // Et les eclairs qui sortent du sol apres un claquement d'orage vivent desormais dans
        // AcademyPackets, avec le reste des effets communs.
        // Le nom et le mot de passe d'un noeud sans fil, la configuration du Matrix et l'onglet
        // sans fil vivent maintenant dans common/ : leur transport est installe par le chargeur
        // (voir AcademyPackets), pas ici.
        // Et la liste blanche du brouilleur : la seule chose de cette machine qu'un identifiant de
        // bouton ne peut pas porter, parce que c'est un nom de joueur et non un nombre.
        CHANNEL.registerMessage(nextId++, cn.academy.energy.network.InterfererWhitelistPacket.class,
                cn.academy.energy.network.InterfererWhitelistPacket::encode,
                cn.academy.energy.network.InterfererWhitelistPacket::decode,
                cn.academy.energy.network.InterfererWhitelistPacket::handle);
        // L'onglet sans fil d'une machine et son etat redescendant sont communs aussi.
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
        // Ajoute a la FIN : les identifiants sont attribues dans l'ordre.
        CHANNEL.registerMessage(nextId++, MineRaySightPacket.class,
                MineRaySightPacket::encode,
                MineRaySightPacket::decode,
                MineRaySightPacket::handle);
    }
}
