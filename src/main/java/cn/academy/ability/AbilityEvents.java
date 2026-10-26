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
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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
            // Un pouvoir DEJA en cours s'arrete quand le joueur entre dans un brouillage.
            //
            // L'original ne bloquait que les nouveaux : son `canUseAbility` est lu a l'appui, donc
            // une charge ouverte ou un maintien en cours continuait tranquillement dans le champ.
            // Le joueur a demande le contraire — « si j'etais en train de preparer mon pouvoir /
            // en train de l'utiliser, ca ne m'empeche pas de continuer a l'utiliser ». C'est donc
            // un ecart assume a l'original, et il est fait avant tout tick de maintien, pour que
            // rien ne parte pendant ce tick-la.
            if (data.isInterfered()) {
                endInterfered(player, data);
            }
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
            // Le surcout au maximum retire TOUT, et sur-le-champ.
            //
            // L'original postait un `OverloadEvent` au franchissement du plafond
            // (`CPData.addOverload`), et son gestionnaire de contextes y disposait tous les
            // contextes du joueur (`ServerManager.__onOverload` -> `disposePlayer`). Le port se
            // contentait de fermer le verrou des NOUVELLES competences, donc celle qui courait
            // continuait : le joueur l'a vu — « si j'atteins l'overload en ayant la competence de
            // lancer, elle continue toujours de fonctionner jusqu'a ce que je n'aie plus de CP ».
            //
            // Le controle est fait APRES le tick des maintiens, donc le franchissement de CE tick
            // les emporte dans la foulee, sans attendre une image de plus.
            tickSustained(player, data);
            if (data.isOverloadRecovering()) {
                endAllHolds(player, data);
            }
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
                        // Les maintiens se TERMINENT, un par un, avant d'etre oublies : c'est ce
                        // qui rend ce qu'ils ont emprunte (les ailes rendent le vol), ce qui pose
                        // leur recharge — effacee juste apres — et surtout ce qui le DIT au client.
                        // Le joueur : « si on meurt avec le deviation activer, quand on reapparait
                        // on a encore l'animation d'actif alors que le pouvoir ne l'est plus ».
                        // `clearCharges` seul vidait la table du serveur en laissant le temoin du
                        // client allume — la fin d'un maintien, elle, envoie `HoldOverPacket`.
                        endAllHolds(player, data);
                        data.clearCooldowns();
                        data.clearCharges();
                        // L'original eteignait l'aptitude a la mort, et la rallumait a la main :
                        // repartir avec son pouvoir allume n'est pas ce qu'il faisait.
                        data.setActivated(false);
                    });
        }
    }

    /**
     * Dormir dans un lit rend la reserve et le surcout.
     *
     * <p>Portage de {@code CPData.Events.playerWakeup} : l'original rendait tout au reveil, et
     * c'est la seule chose qui reste de son {@code recoverAll} — le port ne le fait plus a la
     * mort, ou le joueur veut garder ses points entames.
     *
     * <p>La condition est celle de l'original, traduite : il exigeait
     * {@code !wakeImmediately() && !updateWorld() && shouldSetSpawn()}, c'est-a-dire un reveil
     * <b>naturel</b> (la nuit passee), et non un joueur qui sort du lit a la main. En 1.20.1
     * {@code shouldSetSpawn} n'existe plus, mais les deux drapeaux restants suffisent :
     * {@code ServerLevel.wakeUpAllPlayers} appelle {@code stopSleepInBed(false, false)} au
     * matin, alors que sortir du lit envoie {@code stopSleepInBed(false, true)} et qu'une
     * deconnexion ou une teleportation passent {@code true} en premier. Le couple
     * {@code (false, false)} est donc exactement le reveil du matin.
     *
     * <p>L'original avait d'ailleurs corrige ce point dans sa 1.0pr2 : « Fixed can recover CP
     * by lying on the bed and leave bed » — se coucher puis se relever ne rendait rien.
     */
    @SubscribeEvent
    public static void onWakeUp(net.minecraftforge.event.entity.player.PlayerWakeUpEvent event) {
        if (event.wakeImmediately() || event.updateLevel()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
            data.recoverAll();
            // Le reveil est un evenement rare : on pousse l'etat tout de suite plutot que
            // d'attendre la cadence de synchronisation, sinon la barre du client resterait
            // vide quelques ticks apres que le joueur a ouvert les yeux.
            AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncAbilityDataPacket(data));
        });
    }

    /**
     * Termine tous les maintiens en cours, chacun par la fin ordinaire.
     *
     * <p>Trois chemins y menent, et c'est le meme geste : la mort, le surcout au maximum, et un
     * prereglage qui ne porte plus la competence. Passer par {@link #endHeld} est ce qui rend ce
     * que le maintien avait emprunte, ce qui pose sa recharge, et ce qui previent le client par
     * {@code HoldOverPacket} — sans quoi son temoin reste allume sur un pouvoir qui n'est plus la.
     */
    private static void endAllHolds(ServerPlayer player, AbilityData data) {
        // Une copie : `endHeld` retire la competence de la table qu'on parcourt.
        for (Skill skill : new java.util.ArrayList<>(data.getChargingSkills())) {
            endHeld(player, data, skill);
        }
    }

    /**
     * Termine tout ce qui etait en cours chez un joueur brouille.
     *
     * <p>Les maintiens passent par la fin ordinaire ({@link #endHeld}) : elle rend ce qu'ils avaient
     * emprunte — le vol des ailes, par exemple —, pose leur recharge et previent le client. Les
     * charges en cours s'annulent, comme le {@code terminate()} de l'original quand il abandonnait
     * un contexte.
     *
     * <p>Idempotent : sans rien d'ouvert, la boucle ne fait rien, et c'est ce qui permet de
     * l'appeler a chaque tick de brouillage sans compter les transitions.
     */
    private static void endInterfered(ServerPlayer player, AbilityData data) {
        for (Skill skill : new java.util.ArrayList<>(data.getChargingSkills())) {
            if (skill.isHeld()) {
                endHeld(player, data, skill);
            } else {
                data.cancelCharge(skill);
                // Le client tient sa propre charge des l'appui, donc sans ce message il
                // continuerait son animation sur un pouvoir que le serveur vient d'abandonner.
                AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new HoldOverPacket(skill.getName()));
            }
        }
    }

    /**
     * Termine tout ce qui est ouvert quand l'aptitude s'eteint.
     *
     * <p>Le joueur, 22/10 : « si j'active un pouvoir et que j'appuie sur V, je ne peux plus faire
     * de competence et pourtant la competence en cours continue quand meme ». C'etait exact : la
     * touche V ferme bien la porte des <b>nouvelles</b> competences ({@code canUseAbility}), mais
     * rien ne terminait celles qui couraient — un maintien a bascule (reflexion, deviation,
     * ailes) survivait donc a l'extinction de l'aptitude, et son HUD restait allume.
     *
     * <p>Le geste est celui du brouillage : chaque maintien par la fin ordinaire (il rend ce qu'il
     * a emprunte et previent le client), chaque charge en cours annulee. Un seul chemin pour les
     * deux, pour que l'extinction et le brouillage ne puissent pas diverger.
     */
    public static void endAllOnDeactivate(ServerPlayer player, AbilityData data) {
        endInterfered(player, data);
    }

    /**
     * Termine les maintiens dont la competence n'est plus dans le prereglage <b>en service</b>.
     *
     * <p>Appele par {@code PresetActionPacket} apres chaque changement de prereglage ou chaque
     * affectation de touche : une competence qu'on retire de sa barre s'eteint, comme si on avait
     * relache sa touche. Le joueur : « si je lance le vecteur reflexion par exemple, si je le
     * retire de ma barre des competences, il continue toujours de fonctionner, alors que ca devrais
     * faire en sorte de le desactiver par defaut si il n'est pas present dans ma barre ».
     */
    public static void endHoldsOutsideCurrentPreset(ServerPlayer player) {
        var presets = cn.academy.ability.preset.PresetTracker.of(player);
        if (presets == null) return;
        var current = presets.getCurrent();
        player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
            for (Skill skill : new java.util.ArrayList<>(data.getChargingSkills())) {
                if (!current.contains(skill.getName())) {
                    endHeld(player, data, skill);
                }
            }
        });
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

    /**
     * Le coup, AVANT qu'il ne soit porte : ce qu'une veille peut refuser en entier.
     *
     * <p>L'original avait deux crochets, et sa propre note dit pourquoi : annuler l'evenement de
     * degats provoque quand meme le recul, donc il testait d'abord sur {@code LivingAttackEvent} —
     * le tout premier crochet de {@code LivingEntity.hurt} — et annulait le coup la quand il etait
     * absorbe en entier. Sans ce crochet, le porteur voit le rouge et le recul d'un coup qu'il n'a
     * pas pris : « quand on a le vecteur reflexion d'actif, on est juste cense ne pouvoir prendre
     * litteralement aucun degats, la maintenant on prend des degats visuellement ».
     *
     * <p>Seule competence concernee : {@code vec_reflection}.
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> {
            for (Category category : CategoryManager.INSTANCE.getCategories()) {
                if (!data.hasLearned(category)) continue;
                for (Skill skill : category.getSkills()) {
                    // Meme regle que pour les degats : une passive est toujours la, une tenue
                    // seulement pendant son maintien.
                    if (skill.isPassive() || data.isCharging(skill)) {
                        if (skill.onAttacked(player, data, event)) {
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
        });
    }

    /**
     * « Enable PvP » : sans cette case, une competence ne touche pas un autre joueur.
     *
     * <p>C'est le {@code canAttackPlayer} de l'original, et il se lit au meme endroit que chez
     * lui — au PREMIER crochet de {@code LivingEntity.hurt} — donc un coup refuse ne laisse ni
     * degats, ni recul, ni rouge. Le port a deux types de source pour ses competences
     * ({@link Skill#skillDamage}, de l'{@code indirect_magic} avec le joueur pour auteur, et le
     * type propre aux teleportations de {@code TeleportCrits#source}), c'est donc eux qui les
     * reconnaissent : le corps a corps du jeu, lui, reste intact.
     */
    @SubscribeEvent
    public static void onSkillHitsPlayer(LivingAttackEvent event) {
        if (cn.academy.Config.attackPlayer) return;
        if (!(event.getEntity() instanceof Player)) return;

        var source = event.getSource();
        if (!(source.getEntity() instanceof Player)) return;
        // Le port a deux sources de competence : l'{@code indirect_magic} ordinaire, et le type
        // propre aux teleportations — ne pour son message de mort et pour que les veilles de
        // vecmanip le reconnaissent. Voir {@code TeleportCrits.source}.
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                && !source.is(cn.academy.ability.teleporter.TeleportCrits.type())) return;

        event.setCanceled(true);
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
