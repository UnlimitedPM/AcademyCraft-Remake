package cn.academy.misc.media;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * La capacite des morceaux, cote chargeur.
 *
 * <p>Meme patron que les autres donnees de joueur du port : enregistrer, attacher au joueur,
 * copier a la mort, et pousser au client qui se connecte. Le contenu et l'envoi sont communs :
 * voir {@link MediaTracker}.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public final class MediaCapabilityEvents {

    private MediaCapabilityEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MediaCapability.Holder.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(AcademyCraft.MOD_ID, "media_data"),
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
            MediaTracker.sync(player);
        }
    }
}
