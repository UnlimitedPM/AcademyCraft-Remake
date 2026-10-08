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
        // La marque de radiation : c'est elle qui fait fumer le plasma autour d'une cible, chez
        // tous ceux qui la voient. Voir RadiationMarkPacket.
        CHANNEL.registerMessage(nextId++, RadiationMarkPacket.class,
                RadiationMarkPacket::encode, RadiationMarkPacket::decode,
                RadiationMarkPacket::handle);
        // Et l'onde de choc de vecmanip : trois competences en posent une, et aucune ne se voit
        // sans ce message. Voir VecWavePacket et VecWaves.
        CHANNEL.registerMessage(nextId++, VecWavePacket.class,
                VecWavePacket::encode, VecWavePacket::decode,
                VecWavePacket::handle);
        // Et la trainee du depose au loin : le serveur seul sait que le geste a eu lieu, et c'est
        // la seule chose qu'il donne a voir — il ne deplace personne, il pose un bloc au loin.
        // Voir ShiftTeleportPacket et ShiftTrail.
        CHANNEL.registerMessage(nextId++, ShiftTeleportPacket.class,
                ShiftTeleportPacket::encode, ShiftTeleportPacket::decode,
                ShiftTeleportPacket::handle);
        // Et la gerbe de formule du coup critique : le serveur seul sait qui a ete frappe, et
        // c'est la seule chose que les deux passives du teleporteur montrent. Voir
        // TeleportCritPacket et FormulaParticles.
        CHANNEL.registerMessage(nextId++, TeleportCritPacket.class,
                TeleportCritPacket::encode, TeleportCritPacket::decode,
                TeleportCritPacket::handle);
        // Et le sang de la chair arrachee : le serveur seul sait qui a ete touche, donc c'est lui
        // qui seme la gerbe autour de la victime. Voir BloodSplashPacket.
        CHANNEL.registerMessage(nextId++, BloodSplashPacket.class,
                BloodSplashPacket::encode, BloodSplashPacket::decode,
                BloodSplashPacket::handle);
        // Et les taches de sang au sol : le serveur seul sait QUI a ete touche, donc lui seul sait
        // s'il faut en semer. Voir BloodSprayPacket et BloodSprays.
        CHANNEL.registerMessage(nextId++, BloodSprayPacket.class,
                BloodSprayPacket::encode, BloodSprayPacket::decode,
                BloodSprayPacket::handle);
        // Et les bouffees de fumee du choc au sol : le serveur dit ou, le client allume la bouffee.
        // Voir SmokePuffPacket et Smokes.
        CHANNEL.registerMessage(nextId++, SmokePuffPacket.class,
                SmokePuffPacket::encode, SmokePuffPacket::decode,
                SmokePuffPacket::handle);
        // Et le tir du canon a plasma : son corps de plasma vit chez son porteur seul, et c'est le
        // serveur qui sait ou la boule va. Voir PlasmaShotPacket et PlasmaBodies.
        CHANNEL.registerMessage(nextId++, PlasmaShotPacket.class,
                PlasmaShotPacket::encode, PlasmaShotPacket::decode,
                PlasmaShotPacket::handle);
        // L'anneau de plasma du missile electronique : un tick de plus, chez son lanceur seul.
        // Voir MdMissilePacket.
        CHANNEL.registerMessage(nextId++, MdMissilePacket.class,
                MdMissilePacket::encode, MdMissilePacket::decode, MdMissilePacket::handle);
        // Et la fin d'un maintien, refusee ou terminee : le client tenait le sien depuis l'appui
        // de la touche, il doit le fermer. Voir HoldOverPacket.
        CHANNEL.registerMessage(nextId++, HoldOverPacket.class,
                HoldOverPacket::encode, HoldOverPacket::decode, HoldOverPacket::handle);
        // Le depart du reacteur : son maintien a lui s'arrete au relachement, alors que celui
        // du serveur vole quinze ticks encore. C'est ce qui ouvre le bouclier. Voir
        // JetFlightPacket.
        CHANNEL.registerMessage(nextId++, JetFlightPacket.class,
                JetFlightPacket::encode, JetFlightPacket::decode, JetFlightPacket::handle);
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
    }
}
