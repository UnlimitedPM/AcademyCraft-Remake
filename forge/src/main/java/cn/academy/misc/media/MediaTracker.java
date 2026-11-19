package cn.academy.misc.media;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.misc.media.network.SyncMediaPacket;
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

import javax.annotation.Nullable;

/**
 * La capacite des morceaux, et sa mise en reseau.
 *
 * <p>Meme patron que les autres donnees de joueur du port : enregistrer, attacher au
 * joueur, copier a la mort, et pousser au client qui se connecte. Le client en a besoin
 * pour afficher ce qu'il possede, et c'est le serveur seul qui decide de l'ajouter.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class MediaTracker {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MediaCapability.Holder.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "media_data"),
                    new MediaDataProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(MediaCapability.MEDIA_DATA).ifPresent(old ->
                event.getEntity().getCapability(MediaCapability.MEDIA_DATA).ifPresent(fresh ->
                        fresh.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** La donnee d'un joueur, ou {@code null} s'il n'en a pas. */
    @Nullable
    public static MediaAcquireData of(Player player) {
        return player.getCapability(MediaCapability.MEDIA_DATA)
                .map(MediaCapability.Holder::get)
                .orElse(null);
    }

    /** Pousse la liste au client, qui affiche ce que le joueur possede. */
    public static void sync(ServerPlayer player) {
        // Un faux joueur n'a pas de connexion : lui envoyer un paquet leverait une
        // exception, comme pour les autres donnees du port.
        if (player instanceof net.minecraftforge.common.util.FakePlayer) return;
        player.getCapability(MediaCapability.MEDIA_DATA).ifPresent(holder ->
                AbilityNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player), new SyncMediaPacket(holder)));
    }
}
