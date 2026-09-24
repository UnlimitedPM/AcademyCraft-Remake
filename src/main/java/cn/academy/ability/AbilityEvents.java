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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
        if (event.getObject() instanceof Player player) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "ability_data"), new AbilityDataProvider(player));
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
            // Les recharges avancent d'un tick, comme la boucle de CooldownData.
            data.tickCooldowns();
            // Et les charges en cours, comme le compteur du contexte d'activation.
            data.tickCharges();
            // Le surcout redescend apres son delai, comme dans CPData.tick.
            data.tickOverload();
            // Le scintillement laisse une chute presque annulee derriere lui : c'est le
            // GravityCancellor de l'original, deux secondes pendant lesquelles la gravite
            // (0,08 par tick) est compensee a 0,072.
            if (data.getGravitySuspension() > 0) {
                if (!player.getAbilities().flying && !player.onGround()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, 0.072, 0));
                }
                data.tickGravitySuspension();
            }
            // Les competences tenues vivent tant que la touche reste enfoncee.
            tickSustained(player, data);
            // Le plafond vient de la config : on le reapplique a chaque tick pour
            // qu'un rechargement de config soit pris en compte sans reconnexion.
            data.clampToConfiguredMax();
            if (data.getControlPoint() < data.getMaxControlPoint()) {
                data.tickRegen();
            }
            if (player.tickCount % syncInterval() == 0) {
                AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityDataPacket(data));
            }
        });
    }

    /**
     * A la mort, les recharges sont oubliees.
     *
     * Reprend {@code CooldownData.onPlayerDead} : le joueur repart sans attendre, ce
     * que faisait l'original.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(AbilityCapability.ABILITY_DATA)
                    .ifPresent(data -> {
                        data.clearCooldowns();
                        data.clearCharges();
                        // L'original eteignait l'aptitude a la mort, et la rallumait a la main :
                        // repartir avec son pouvoir allume n'est pas ce qu'il faisait.
                        data.setActivated(false);
                    });
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
            for (Category category : CategoryManager.INSTANCE.getCategories()) {
                if (!data.hasLearned(category)) continue;
                for (Skill skill : category.getSkills()) {
                    // Une passive est toujours la ; une competence tenue ne l'est que
                    // pendant son maintien, sinon le bouclier absorberait sans etre tenu.
                    if (skill.isPassive() || data.isCharging(skill)) {
                        event.setAmount(skill.onDamaged(player, data, event));
                    }
                }
            }
        });
    }

    /**
     * Fait vivre les competences qui gardent la touche enfoncee : un tick de plus, ou
     * leur fin.
     *
     * Le compteur avance d'abord, comme le {@code ticks += 1} de l'original, puis la
     * duree maximale est verifiee avant l'effet du tick.
     */
    private static void tickSustained(ServerPlayer player, AbilityData data) {
        for (Skill skill : data.getChargingSkills()) {
            int ticks = data.getChargeTicks(skill);
            if (skill.isHeld()) {
                int max = skill.getMaxHoldTicks(data);
                if (max > 0 && ticks > max) {
                    endHeld(player, data, skill);
                } else if (!skill.onHoldTick(player, data, ticks)) {
                    endHeld(player, data, skill);
                }
            } else if (skill.isChargeable() && !skill.onChargeTick(player, data, ticks)) {
                // Une charge qui n'a plus de quoi s'entretenir est abandonnee : rien
                // n'est lance, rien n'est facture, et le compteur est oublie — c'est le
                // terminate() de l'original, pas la fin d'un maintien.
                data.cancelCharge(skill);
            }
        }
    }

    /**
     * Termine un maintien : relachement, duree maximale ou ressources epuisees.
     *
     * Les trois chemins font la meme chose, donc passent par ici : terminer l'effet,
     * poser la recharge de la duree tenue, et prevenir le client. L'original le faisait
     * dans le {@code MSG_TERMINATED} de son contexte, quelle que soit la cause de la fin.
     */
    public static void endHeld(ServerPlayer player, AbilityData data, Skill skill) {
        int heldTicks = data.getChargeTicks(skill);
        // L'effet de fin passe AVANT que l'etat du maintien ne soit oublie : l'original
        // le faisait depuis son contexte, qui vivait encore pendant son propre
        // MSG_TERMINATED — la bombe a fragmentation y relit les billes qu'elle a posees.
        // La recharge se lit au meme moment, et pour la meme raison : le bouclier la
        // calcule avec ce qu'il a fallu tenir.
        skill.onHoldEnd(player, data, heldTicks);
        int cooldown = skill.getCooldownTicks(data);
        data.endCharge(skill);
        data.setCooldown(skill, cooldown);
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityDataPacket(data));
    }
}
