package cn.academy.terminal;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.terminal.network.SyncTerminalDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Attache la donnee du terminal aux joueurs et la tient a jour cote client.
 *
 * Meme forme que {@code AbilityEvents} : capacite, copie a la mort, envoi a la
 * connexion, plus un envoi explicite quand le contenu change — un objet qui
 * installe une application doit se voir tout de suite, sans attendre le prochain
 * rafraichissement periodique.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class TerminalEvents {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(TerminalData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "terminal_data"),
                    new TerminalDataProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(TerminalCapability.TERMINAL_DATA).ifPresent(old ->
                event.getEntity().getCapability(TerminalCapability.TERMINAL_DATA).ifPresent(fresh ->
                        fresh.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** Envoie l'etat courant au joueur. A appeler apres toute installation. */
    public static void sync(ServerPlayer player) {
        // Un faux joueur — celui des tests headless — n'a pas de vraie connexion :
        // lui envoyer un paquet laisserait une exception. C'est la seule exception
        // a l'envoi, et elle ne concerne aucun joueur en jeu.
        if (player instanceof net.minecraftforge.common.util.FakePlayer) return;
        player.getCapability(TerminalCapability.TERMINAL_DATA).ifPresent(data ->
                AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncTerminalDataPacket(data)));
    }
}
