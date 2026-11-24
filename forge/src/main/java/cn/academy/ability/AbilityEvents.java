package cn.academy.ability;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.SyncAbilityDataPacket;
import cn.academy.platform.PlatformHooks;
import cn.academy.platform.network.Network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge de l'aptitude : la capacite du joueur, et les evenements qui menent au tick.
 *
 * <p>La logique, elle, est commune — voir {@link AbilityTicker} : recharges, charges, surcout,
 * interference, protection de chute, degats. Ce fichier ne fait que deux choses : cabler la
 * capacite Forge (enregistrement, attachement, copie a la mort, envoi a la connexion), et traduire
 * les evenements du chargeur en appels.
 *
 * <p>La traduction est la ou elle compte : {@code LivingAttackEvent} est le <b>premier</b> crochet
 * de {@code LivingEntity.hurt}, donc annuler la evite le recul et le rouge ; {@code LivingHurtEvent}
 * arrive apres, et c'est la que les degats se retravaillent.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public final class AbilityEvents {

    private AbilityEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(AbilityData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(
                    new ResourceLocation(AcademyCraft.MOD_ID, "ability_data"),
                    new AbilityDataProvider(player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(AbilityCapability.ABILITY_DATA).ifPresent(oldData ->
                event.getEntity().getCapability(AbilityCapability.ABILITY_DATA)
                        .ifPresent(newData -> newData.copyFrom(oldData)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityData data = PlatformHooks.abilityData(player);
            if (data != null) {
                Network.sendToPlayer(player, new SyncAbilityDataPacket(data));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        AbilityTicker.tick(player);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityTicker.onDeath(player);
        }
    }

    /**
     * Le reveil naturel — la nuit passee — rend la reserve et le surcout.
     *
     * <p>C'est la condition de l'original, traduite : il exigeait {@code !wakeImmediately() &&
     * !updateWorld() && shouldSetSpawn()}, c'est-a-dire un reveil par le matin. En 1.20.1
     * {@code shouldSetSpawn} n'existe plus, mais les deux drapeaux restants suffisent :
     * {@code ServerLevel.wakeUpAllPlayers} appelle {@code stopSleepInBed(false, false)} au matin,
     * alors que sortir du lit envoie {@code stopSleepInBed(false, true)} et qu'une deconnexion ou
     * une teleportation passent {@code true} en premier.
     */
    @SubscribeEvent
    public static void onWakeUp(net.minecraftforge.event.entity.player.PlayerWakeUpEvent event) {
        if (event.wakeImmediately() || event.updateLevel()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityTicker.onWakeUp(player);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        AbilityTicker.livingTick(event.getEntity());
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && AbilityTicker.protectsFromFall(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (AbilityTicker.refusesAttack(player, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSkillHitsPlayer(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (AbilityTicker.refusesSkillOnPlayer(target, event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        event.setAmount(AbilityTicker.onHurt(entity, event.getSource(), event.getAmount()));
    }
}
