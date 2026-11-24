package cn.academy.terminal.tutorial;

import cn.academy.content.AcademyContent;
import cn.academy.platform.PlatformHooks;
import cn.academy.platform.network.Network;
import cn.academy.terminal.tutorial.network.SyncTutorialDataPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ouvre les tutoriels, et tient le client au courant.
 *
 * <p>Portage du planificateur de {@code TutorialData} : toutes les dix ticks, le serveur regarde
 * chaque tutoriel non encore ouvert, et l'ouvre si le joueur a de quoi. L'original faisait
 * exactement cela — un balayage, pas des evenements — et c'est plus solide : ce qui compte est que
 * le joueur ait <b>eu</b> l'objet, peu importe comment il l'a eu.
 *
 * <p>Un point de fidelite qui se voit : un tutoriel <b>ne se referme pas</b>. Ranger son lingot dans
 * un coffre apres l'avoir fabrique le laisse ouvert, parce que c'est l'objet <i>obtenu</i> qui
 * compte, pas l'objet porte.
 *
 * <p>Et un cadeau de l'original, garde tel quel : le premier joueur a recevoir un terminal se voit
 * donner l'objet MisakaCloud, une fois, s'il est active en config.
 *
 * <p>La capacite qui porte la donnee et son balayage periodique restent chez le chargeur ; voir
 * {@code TutorialCapabilityEvents} cote Forge.
 */
public final class TutorialTracker {

    /**
     * La periode du balayage, en ticks. L'original en avait choisi trois.
     *
     * <p>Dix depuis le 26/10, et c'est une economie pure : le balayage relit l'inventaire du joueur
     * et ses criteres, sans rien qui presse — un tutoriel qui s'ouvre un demi-tick plus tard ne se
     * voit pas. A trois ticks, c'etait vingt inspections par seconde et par joueur ; a dix, six.
     */
    public static final int SCAN_PERIOD = 10;

    private TutorialTracker() {
    }

    /** Les tutoriels ouverts du joueur, ou {@code null}. */
    public static TutorialData of(Player player) {
        return PlatformHooks.tutorialData(player);
    }

    /** Un tick de joueur : le balayage, s'il est du a ce tick-la. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % SCAN_PERIOD != 0) return;
        TutorialData data = of(player);
        if (data != null) scan(player, data);
    }

    /**
     * Ouvre ce qui peut l'etre. Rien n'est envoye si rien n'a change.
     *
     * <p>Publique pour etre appelee directement : c'est le balayage lui-meme, et un test n'a pas a
     * fabriquer un evenement de tick pour l'exercer.
     *
     * <p>Les tutoriels sans condition — l'accueil, les bases, le terminal — ne sont pas ranges ici :
     * l'ecran les considere ouverts d'office, et l'original non plus ne les comptait pas comme
     * « actives ».
     */
    public static void scan(ServerPlayer player, TutorialData data) {
        boolean changed = data.ensureMisakaId();

        for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
            if (entry.alwaysOpen() || data.isUnlocked(entry.id())) continue;
            if (holds(player, entry)) {
                data.unlock(entry.id());
                changed = true;
            }
        }

        if (!data.isTerminalGiven() && cn.academy.Config.giveCloudTerminal) {
            // Le cadeau de l'original : l'objet MisakaCloud, une fois, au premier joueur. Il tombe
            // au sol plutot que dans l'inventaire, comme le sien, qui passait par une entite d'objet.
            ItemStack stack = new ItemStack(AcademyContent.item("tutorial"));
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 1.0,
                    player.getZ(), stack);
            player.level().addFreshEntity(drop);
            data.setTerminalGiven(true);
            changed = true;
        }

        if (changed) sync(player);
    }

    /** Vrai si le joueur a, ou a eu, de quoi ouvrir ce tutoriel. */
    private static boolean holds(Player player, TutorialLibrary.Entry entry) {
        for (String id : entry.requiredItems()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(key);
            if (item != null && player.getInventory().contains(new ItemStack(item))) return true;
        }
        return false;
    }

    /** Envoie l'etat courant au joueur. A appeler apres toute ouverture. */
    public static void sync(ServerPlayer player) {
        TutorialData data = of(player);
        if (data == null) return;
        Network.sendToPlayer(player, new SyncTutorialDataPacket(data));
    }
}
