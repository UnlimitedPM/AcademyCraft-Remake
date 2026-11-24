package cn.academy.terminal;

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
 * La capacite du terminal, cote chargeur.
 *
 * <p>Meme forme que les autres donnees de joueur du port : enregistrer, attacher au joueur, copier
 * a la mort, et pousser au client qui se connecte. Le contenu et l'envoi sont communs : voir
 * {@link TerminalTracker}.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public final class TerminalCapabilityEvents {

    private TerminalCapabilityEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(TerminalData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(AcademyCraft.MOD_ID, "terminal_data"),
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
            TerminalTracker.sync(player);
        }
    }
}
