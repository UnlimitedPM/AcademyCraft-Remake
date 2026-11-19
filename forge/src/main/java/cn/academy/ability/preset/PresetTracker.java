package cn.academy.ability.preset;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.preset.network.SyncPresetPacket;
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
 * La capacité des préréglages, et sa mise en réseau.
 *
 * <p>Même patron que les autres données de joueur du port : enregistrer, attacher au
 * joueur, copier à la mort, et pousser au client qui se connecte. Ce qui change ici, c'est
 * que le client peut aussi <b>modifier</b> la donnée : c'est lui qui connait les touches.
 * Les deux paquets qui le permettent vivent dans le paquet voisin.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class PresetTracker {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PresetCapability.Holder.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "preset_data"),
                    new PresetDataProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(PresetCapability.PRESET_DATA).ifPresent(old ->
                event.getEntity().getCapability(PresetCapability.PRESET_DATA).ifPresent(fresh ->
                        fresh.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** La donnée d'un joueur, ou {@code null} s'il n'en a pas. */
    public static PresetData of(Player player) {
        return player.getCapability(PresetCapability.PRESET_DATA)
                .map(PresetCapability.Holder::get)
                .orElse(null);
    }

    /** Pousse les quatre préréglages au client, qui en a besoin pour ses touches. */
    public static void sync(ServerPlayer player) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer) return;
        player.getCapability(PresetCapability.PRESET_DATA).ifPresent(holder ->
                AbilityNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player), new SyncPresetPacket(holder)));
    }

    /** Change la catégorie et efface les préréglages, comme dans l'original. */
    public static void clearOnCategoryChange(Player player) {
        PresetData data = of(player);
        if (data == null) return;
        data.clearAll();
        // Une barre vide ne porte plus rien : ce qui courait s'eteint, comme dans l'original, dont
        // le gestionnaire de contextes disposait tout sur un changement de categorie.
        if (player instanceof ServerPlayer server) {
            cn.academy.ability.AbilityEvents.endHoldsOutsideCurrentPreset(server);
            sync(server);
        }
    }
}
