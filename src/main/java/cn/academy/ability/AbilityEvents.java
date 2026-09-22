package cn.academy.ability;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.SyncAbilityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class AbilityEvents {

    /** Intervalle de resynchronisation des CP vers le client, en ticks. */
    private static int syncInterval() {
        return cn.academy.Config.controlPointSyncInterval;
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(AbilityData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "ability_data"), new AbilityDataProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(AbilityCapability.ABILITY_DATA).ifPresent(oldData ->
                event.getEntity().getCapability(AbilityCapability.ABILITY_DATA).ifPresent(newData -> newData.copyFrom(oldData)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data ->
                    AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityDataPacket(data)));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
            // Les sources d'interference sont evaluees ici, une fois par tick : une
            // source qui ne brouille plus disparait d'elle-meme.
            data.refreshInterference();
            // Le plafond vient de la config : on le reapplique a chaque tick pour
            // qu'un rechargement de config soit pris en compte sans reconnexion.
            data.clampToConfiguredMax();
            if (data.getControlPoint() < data.getMaxControlPoint()) {
                data.tickRegen((float) cn.academy.Config.controlPointRegenPerTick);
            }
            if (player.tickCount % syncInterval() == 0) {
                AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityDataPacket(data));
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
            for (Category category : CategoryManager.INSTANCE.getCategories()) {
                if (!data.hasLearned(category)) continue;
                for (Skill skill : category.getSkills()) {
                    if (skill.isPassive()) {
                        event.setAmount(skill.onDamaged(player, data, event));
                    }
                }
            }
        });
    }
}
