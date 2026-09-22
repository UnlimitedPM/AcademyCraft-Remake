package cn.academy.terminal.tutorial;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.terminal.tutorial.network.SyncTutorialDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Ouvre les tutoriels, et tient le client au courant.
 *
 * <p>Portage du planificateur de {@code TutorialData} : toutes les trois ticks, le serveur
 * regarde chaque tutoriel non encore ouvert, et l'ouvre si le joueur a de quoi. L'original
 * faisait exactement cela — un balayage, pas des evenements — et c'est plus solide : ce qui
 * compte est que le joueur ait <b>eu</b> l'objet, peu importe comment il l'a eu.
 *
 * <p>Un point de fidelite qui se voit : un tutoriel <b>ne se referme pas</b>. Ranger son
 * lingot dans un coffre apres l'avoir fabrique le laisse ouvert, parce que c'est l'objet
 * <i>obtenu</i> qui compte, pas l'objet porte.
 *
 * <p>Et un cadeau de l'original, garde tel quel : le premier joueur a recevoir un terminal
 * se voit donner l'objet MisakaCloud, une fois, s'il est active en config.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class TutorialTracker {

    /** La periode du balayage. L'original en avait choisi trois ticks. */
    private static final int SCAN_PERIOD = 3;

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(TutorialData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "tutorial_data"),
                    new TutorialDataProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(TutorialCapability.TUTORIAL_DATA).ifPresent(old ->
                event.getEntity().getCapability(TutorialCapability.TUTORIAL_DATA).ifPresent(fresh ->
                        fresh.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % SCAN_PERIOD != 0) return;

        player.getCapability(TutorialCapability.TUTORIAL_DATA).ifPresent(data -> scan(player, data));
    }

    /**
     * Ouvre ce qui peut l'etre. Rien n'est envoye si rien n'a change.
     *
     * <p>Publique pour etre appelee directement : c'est le balayage lui-meme, et un test
     * n'a pas a fabriquer un evenement de tick pour l'exercer.
     *
     * <p>Les tutoriels sans condition — l'accueil, les bases, le terminal — ne sont pas
     * ranges ici : l'ecran les considere ouverts d'office, et l'original non plus ne les
     * comptait pas comme « actives ».
     */
    public static void scan(ServerPlayer player, TutorialData data) {
        boolean changed = false;

        for (TutorialLibrary.Entry entry : TutorialLibrary.entries()) {
            if (entry.alwaysOpen() || data.isUnlocked(entry.id())) continue;
            if (holds(player, entry)) {
                data.unlock(entry.id());
                changed = true;
            }
        }

        if (!data.isTerminalGiven() && cn.academy.Config.giveCloudTerminal) {
            // Le cadeau de l'original : l'objet MisakaCloud, une fois, au premier joueur.
            // Il tombe au sol plutot que dans l'inventaire, comme le sien, qui passait par
            // une entite d'objet.
            ItemStack stack = new ItemStack(cn.academy.ModItems.TUTORIAL.get());
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
            if (key == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)) {
                continue;
            }
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);
            if (item != null && player.getInventory().contains(new ItemStack(item))) return true;
        }
        return false;
    }

    /** Envoie l'etat courant au joueur. A appeler apres toute ouverture. */
    public static void sync(ServerPlayer player) {
        // Un faux joueur — celui des tests headless — n'a pas de vraie connexion : lui
        // envoyer un paquet laisserait une exception.
        if (player instanceof net.minecraftforge.common.util.FakePlayer) return;
        player.getCapability(TutorialCapability.TUTORIAL_DATA).ifPresent(data ->
                AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncTutorialDataPacket(data)));
    }
}
