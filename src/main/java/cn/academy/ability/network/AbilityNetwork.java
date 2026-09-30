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
        CHANNEL.registerMessage(nextId++, LocationTeleportPacket.class,
                LocationTeleportPacket::encode, LocationTeleportPacket::decode, LocationTeleportPacket::handle);
        CHANNEL.registerMessage(nextId++, ToggleAbilityPacket.class,
                ToggleAbilityPacket::encode, ToggleAbilityPacket::decode, ToggleAbilityPacket::handle);
        CHANNEL.registerMessage(nextId++, SyncAbilityDataPacket.class,
                SyncAbilityDataPacket::encode, SyncAbilityDataPacket::decode, SyncAbilityDataPacket::handle);
        // La detection de minerais est la seule competence qui ait besoin de dire quelque
        // chose au client : c'est lui qui balaie le monde et l'allume.
        CHANNEL.registerMessage(nextId++, MineDetectPacket.class,
                MineDetectPacket::encode, MineDetectPacket::decode, MineDetectPacket::handle);
        // Les eclairs des competences : le serveur sait ou ils sont partis et jusqu'ou, et
        // il le dit a ceux qui voient le tireur. Sans ce paquet, un arc ne se dessinerait
        // que chez celui qui appuie sur la touche.
        CHANNEL.registerMessage(nextId++, ArcEffectPacket.class,
                ArcEffectPacket::encode, ArcEffectPacket::decode, ArcEffectPacket::handle);
        // Le renfort du corps : un seul message, et pas un arc — ses sept hauteurs et leurs
        // delais sont une affaire d'image, que le client rejoue seul. Voir BodyIntensifyEffect.
        CHANNEL.registerMessage(nextId++, BodyIntensifyPacket.class,
                BodyIntensifyPacket::encode, BodyIntensifyPacket::decode,
                BodyIntensifyPacket::handle);
        // Les rayons du meltdowner : le serveur sait d'ou ils partent et jusqu'ou, et il le dit
        // a ceux qui voient le tireur. Le genre du rayon voyage par son nom, comme le motif des
        // eclairs, et c'est lui qui porte le son a jouer. Voir MdRayPacket.
        CHANNEL.registerMessage(nextId++, MdRayPacket.class,
                MdRayPacket::encode, MdRayPacket::decode, MdRayPacket::handle);
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
    }
}
