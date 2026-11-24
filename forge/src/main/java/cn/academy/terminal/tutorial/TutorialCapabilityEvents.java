package cn.academy.terminal.tutorial;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * La capacite des tutoriels, cote chargeur, et le tick qui declenche le balayage.
 *
 * <p>La donnee et le balayage sont communs — voir {@link TutorialTracker} — ; ce qui reste ici,
 * c'est la capacite Forge et le tick du joueur.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public final class TutorialCapabilityEvents {

    private TutorialCapabilityEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(TutorialData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(AcademyCraft.MOD_ID, "tutorial_data"),
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
            TutorialTracker.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        TutorialTracker.tick(player);
    }
}
