package cn.academy.ability;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.HoldOverPacket;
import cn.academy.ability.network.SyncAbilityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class AbilityEvents {

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
            // Un pouvoir protege de la chute jusqu'a ce que le joueur se soit pose — et non
            // jusqu'au premier bloc touche. Un atterrissage a cheval sur deux blocs, ou un
            // glissement de quelques ticks, se payait sinon : le joueur a vu « parfois je prends
            // quand meme des degats de chute [...] j'atteris entre plusieurs blocs et ca annule
            // les degats d'un bloc mais pas l'autre ». Le sol se lit sur fallDistance et NON sur
            // onGround : un deplacement d'un coup ne remet pas onGround a jour, donc juste apres
            // lui il porte encore l'etat d'avant. Vanilla remet fallDistance a zero en touchant un
            // bloc, et c'est ce signal qui ouvre la fenetre d'atterrissage — voir
            // AbilityData.tickFallProtection et LANDING_GRACE_TICKS.
            data.tickFallProtection(player.fallDistance > 0f);
            // Les competences tenues vivent tant que la touche reste enfoncee.
            tickSustained(player, data);
            // La reserve ne remonte que si elle est entamee : au ras bord il n'y a rien a
            // faire, et l'original le testait aussi avant de recalculer son gain.
            if (data.getControlPoint() < data.getMaxControlPoint()) {
                data.tickRegen();
            }
            // Le client est resynchronise a la cadence de l'original, et non a cadence
            // fixe : 4 ticks au pire apres un paiement, 10 en regime normal (voir
            // AbilityData.syncInterval). C'est ce qui fait qu'on voit ses points partir :
            // la reserve, elle, attend encore son delai de 15 ticks avant de remonter.
            if (data.tickSync()) {
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

    /**
     * Les marques de radiation avancent d'un tick, comme l'evenement de mise a jour d'un vivant
     * chez l'original.
     */
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        cn.academy.ability.meltdowner.RadiationMarks.tick(event.getEntity());
    }

    /**
     * La chute qui suit un pouvoir ne se paie pas.
     *
     * <p>C'est le refus lui-meme, et il dure tant que le joueur n'a pas touche un bloc (voir
     * {@code AbilityData.protectFromFall}) : une chute d'un seul tick passant le seuil serait
     * payee avant qu'on ait pu l'effacer, et une chute de trente blocs doit rester gratuite. Les
     * quatre teleportations qui deplacent le joueur la posent, et depuis peu l'acceleration de
     * vecteur et la fermeture des ailes de tempete avec elles.
     *
     * <p>Le refus passe par l'evenement des degats, avec la source {@code FALL} : c'est celui
     * que le jeu poste vraiment pour une chute — verifie au bytecode, {@code LivingEntity} le
     * poste une fois — et c'est deja par lui que la theorie du repli dimensionnel annulait
     * celle de tout le monde.
     */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
            if (data.isProtectedFromFall()) event.setCanceled(true);
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // La marque de radiation multiplie les degats recus, d'ou qu'ils viennent : c'est ce
        // que faisait l'original dans son propre evenement. Le port l'applique en premier, pour
        // que les crochets des competences partent des degats deja augmentes.
        cn.academy.ability.meltdowner.RadiationMarks.apply(event);
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
        // Et on le DIT au client, qui tient son propre temoin depuis l'appui : sans cela son
        // animation continuerait toute seule alors que le maintien est fini chez le serveur —
        // le joueur l'a vu, « a la fin de la competence, si on ne relache pas le clic, le
        // bouclier est toujours visible alors que la competence est finie ». C'est la meme
        // chose pour une fin de duree maximale et pour une reserve qui s'epuise : les trois
        // passent par ici. Voir HoldOverPacket.
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new HoldOverPacket(skill.getName()));
    }
}
