package cn.academy.ability.develop;

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
 * La capacite du developpeur portable, et son tick.
 *
 * <p>Le meme patron que les autres donnees du port : enregistrer la capacite, l'attacher
 * au joueur, la copier a la mort, et la faire avancer a chaque tick. Ce qui change ici,
 * c'est que l'objet lui-meme ne tick pas — un objet n'a pas de tick — donc c'est le joueur
 * qui avance l'apprentissage qu'il porte.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class PortableDevTracker {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PortableDevData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "portable_dev_data"),
                    new PortableDevDataProvider(player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(PortableDevCapability.PORTABLE_DEV).ifPresent(old ->
                event.getEntity().getCapability(PortableDevCapability.PORTABLE_DEV).ifPresent(fresh ->
                        fresh.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // L'objet peut changer a tout moment, donc on relit a chaque tick plutot que de
        // s'abonner a sa prise en main : un apprentissage s'interrompt des que le
        // portable quitte la main, comme dans l'original.
        player.getCapability(PortableDevCapability.PORTABLE_DEV).ifPresent(data -> tick(player, data));
    }

    /** Fait avancer un apprentissage d'un tick. Publique pour les tests. */
    public static void tick(ServerPlayer player, PortableDevData data) {
        data.tick(player);
    }
}
