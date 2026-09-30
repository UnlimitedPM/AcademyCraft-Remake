package cn.academy.ability.network;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.AbilityEvents;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** C2S: the player pressed or released the key bound to a given skill. */
public class ActivateSkillPacket {

    /**
     * Ce que le joueur vient de faire de la touche.
     *
     * L'original envoyait deux messages distincts, {@code MSG_KEYDOWN} et
     * {@code MSG_KEYUP} ; les competences qui se chargent ne font rien a l'appui, elles
     * accumulent, et partent au relachement.
     */
    public enum Phase {
        /** Touche enfoncee. */
        PRESS,
        /** Touche relachee : la competence part avec ce qu'elle a accumule. */
        RELEASE
    }

    private final int categoryId;
    private final int skillId;
    private final Phase phase;

    public ActivateSkillPacket(int categoryId, int skillId) {
        this(categoryId, skillId, Phase.PRESS);
    }

    public ActivateSkillPacket(int categoryId, int skillId, Phase phase) {
        this.categoryId = categoryId;
        this.skillId = skillId;
        this.phase = phase;
    }

    public static void encode(ActivateSkillPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.categoryId);
        buf.writeVarInt(msg.skillId);
        buf.writeByte(msg.phase.ordinal());
    }

    public static ActivateSkillPacket decode(FriendlyByteBuf buf) {
        int categoryId = buf.readVarInt();
        int skillId = buf.readVarInt();
        int phase = buf.readByte();
        Phase[] values = Phase.values();
        return new ActivateSkillPacket(categoryId, skillId,
                phase >= 0 && phase < values.length ? values[phase] : Phase.PRESS);
    }

    public static void handle(ActivateSkillPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Category category = CategoryManager.INSTANCE.getCategory(msg.categoryId);
            if (category == null) return;
            Skill skill = category.getSkill(msg.skillId);
            if (skill == null) return;
            player.getCapability(AbilityCapability.ABILITY_DATA).ifPresent(data -> handle(player, data, skill, msg.phase));
        });
        ctx.setPacketHandled(true);
    }

    private static void handle(ServerPlayer player, AbilityData data, Skill skill, Phase phase) {
        // Apprendre une competence passe par le developpeur : tant qu'elle ne
        // l'est pas, la touche ne fait rien. L'original ne posait meme pas de
        // touche dans ce cas ; le port en pose une par competence, donc il le
        // dit plutot que de rester muet.
        if (!data.isSkillLearned(skill)) {
            player.displayClientMessage(
                    Component.translatable("academy.ability.not_learned", skill.getDisplayName())
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (phase == Phase.PRESS) {
            // Une competence deja tenue ne se rouvre pas. L'original n'avait qu'un
            // contexte d'activation a la fois, et un second appui sur une competence
            // ouverte remettrait son compteur a zero — ce qui, pour un effet qui se
            // repere a son tick de depart comme le vol du reacteur, le ferait repartir
            // en arriere.
            if (data.isCharging(skill)) return;

            // Une competence tenue s'ouvre : son cout est paye maintenant, et elle vit
            // ensuite tant que la touche reste enfoncee.
            if (skill.isHeld()) {
                beginHeld(player, data, skill);
                return;
            }
            // Une competence qui se charge n'est pas lancee maintenant : on ouvre une
            // charge et on attend le relachement.
            if (skill.isChargeable()) {
                if (!canBegin(player, data, skill)) return;
                data.beginCharge(skill);
                skill.onStart(player, data);
                return;
            }
            activate(player, data, skill);
            return;
        }

        // Relachement : il n'y a quelque chose a faire que si une charge etait ouverte.
        if (!data.isCharging(skill)) return;

        // Une competence tenue se termine : c'est la fin normale du maintien, avec la
        // recharge de ce qui a ete tenu. Sauf si l'effet dit qu'il n'a pas fini — le vol
        // du jet engine commence au relachement et dure une seconde de plus : le maintien
        // reste alors ouvert, et c'est son propre tick qui appellera la fin.
        if (skill.isHeld()) {
            if (skill.onRelease(player, data, data.getChargeTicks(skill))) return;
            AbilityEvents.endHeld(player, data, skill);
            return;
        }

        data.endCharge(skill);

        // Relacher trop tot ne declenche rien du tout : l'original exigeait
        // TICKS_MIN avant d'envoyer quoi que ce soit a son serveur. Rien n'est
        // consomme dans ce cas, pas meme une recharge.
        int ticks = data.getChargeTicks(skill);
        if (ticks < skill.getMinChargeTicks(data)) return;

        activate(player, data, skill);
    }

    /**
     * Ouvre une competence tenue.
     *
     * Le cout d'ouverture est paye a l'appui, comme le {@code MSG_MADEALIVE} de
     * l'original : le bouclier charge sa reserve des qu'il apparait, puis s'entretient
     * par tick.
     */
    private static void beginHeld(ServerPlayer player, AbilityData data, Skill skill) {
        if (!canBegin(player, data, skill)) return;
        // L'original refusait de s'ouvrir quand il n'y avait rien a viser, et refusait
        // donc sans rien facturer : le port verifie avant de payer, pas apres.
        if (!skill.canStart(player, data)) return;
        if (!data.perform(skill.getCpCost(data), skill.getOverloadCost(data))) {
            player.displayClientMessage(
                    Component.literal("Not enough Control Points").withStyle(ChatFormatting.RED), true);
            return;
        }
        data.beginCharge(skill);
        skill.onStart(player, data);
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAbilityDataPacket(data));
    }

    /** Verifie qu'une charge peut s'ouvrir : ni surcharge, ni brouillage, ni recharge. */
    private static boolean canBegin(ServerPlayer player, AbilityData data, Skill skill) {
        if (!canUseAbility(player, data, false)) return false;
        int cooldown = data.getCooldown(skill);
        if (cooldown > 0) {
            announceCooldown(player, cooldown);
            return false;
        }
        return true;
    }

    /**
     * Le joueur peut-il seulement lancer quelque chose ?
     *
     * Portage de {@code CPData.canUseAbility} : une surcharge et un brouillage ferment
     * tout, y compris les competences qui se chargent — l'original ne creait meme pas
     * de contexte dans ce cas, donc la touche ne faisait rien du tout.
     */
    private static boolean canUseAbility(ServerPlayer player, AbilityData data, boolean released) {
        // L'aptitude doit etre ALLUMEE : chez l'original c'etait la touche (V) qui l'allumait,
        // et rien ne partait avant. C'est le premier refus, comme chez lui.
        if (!data.isActivated()) {
            player.displayClientMessage(
                    Component.literal("Ability is off - press the ability key")
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (data.isInterfered()) {
            player.displayClientMessage(
                    Component.literal("Abilities are jammed here").withStyle(ChatFormatting.RED), true);
            return false;
        }
        // Le verrou de l'original vaut pour toute la descente, et pas seulement pour le delai
        // pendant lequel le temoin affiche la surcharge : sinon le joueur relancerait une
        // competence au milieu de sa propre recuperation.
        //
        // Sauf pour une charge deja tenue : une competence qui paie son surcout A LA CHARGE
        // (le thunder clap, voir Skill#paysOnEffect) arrive ici avec sa propre surcharge dans
        // la reserve, et la refuser a ce moment-la avalerait la charge entiere sans rien
        // declencher — c'est ce que le joueur a vu, « le tonnerre ne fait pas assez de
        // degats », et rien du tout sous terre. L'original ne revalidait rien a la fin d'une
        // charge : il frappait, quel que soit l'etat de sa reserve.
        if (!released && data.isOverloadRecovering()) {
            player.displayClientMessage(
                    Component.literal("Overloaded - wait for your overload to drop")
                            .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    /** Le declenchement lui-meme, commun aux competences instantanees et chargees. */
    private static void activate(ServerPlayer player, AbilityData data, Skill skill) {
        // Une charge arrive ici APRES avoir pris son surcout : il n'est donc pas une raison de
        // la refuser, sans quoi tout ce qu'elle a amasse serait perdu. Voir canUseAbility.
        boolean released = skill.isChargeable()
                && data.getChargeTicks(skill) >= skill.getMinChargeTicks(data);
        if (!canUseAbility(player, data, released)) return;

        // Recharge : l'original tenait un compteur par competence dans
        // CooldownData et refusait le declenchement tant qu'il n'etait pas
        // revenu a zero. Le message dit combien il reste, sinon le joueur
        // n'aurait aucun moyen de savoir si la touche a echoue ou si elle est
        // simplement en attente.
        int cooldown = data.getCooldown(skill);
        if (cooldown > 0) {
            announceCooldown(player, cooldown);
            return;
        }

        // Les deux ressources ensemble ou aucune : portage de CPData.perform. Sans
        // cette atomicite, une competence refusee faute de CP laisserait quand meme
        // du surcout derriere elle. Les competences qui paient dans leur effet sautent
        // ce paiement : elles seules savent ce qu'elles doivent — voir Skill#paysOnEffect.
        if (!skill.paysOnEffect()
                && !data.perform(skill.getCpCost(data), skill.getOverloadCost(data))) {
            player.displayClientMessage(
                    Component.literal("Not enough Control Points").withStyle(ChatFormatting.RED), true);
            return;
        }

        // La competence lit elle-meme son temps de charge : le compteur reste
        // lisible apres le relachement, dans ses degats, sa recharge et son gain
        // d'experience.
        skill.onActivateCharged(player, data, data.getChargeTicks(skill));
        // La recharge part des que la competence est lancee, comme dans
        // l'original qui la posait a la fin de son effet.
        data.setCooldown(skill, skill.getCooldownTicks(data));
        // Utiliser une competence la fait progresser, et verse de
        // l'avancement au niveau de la categorie : c'est ce qui fait qu'on
        // monte en jouant, et non en attendant. L'original versait ces
        // points depuis chaque competence au moment ou son effet aboutissait ;
        // ici ils sont verses a l'activation, au montant de base de la
        // competence — l'ecart est note dans Skill#getExpGain.
        data.addSkillExp(skill, skill.getExpGain(data));
        AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncAbilityDataPacket(data));
    }

    private static void announceCooldown(ServerPlayer player, int cooldown) {
        player.displayClientMessage(
                Component.translatable("academy.ability.cooldown",
                                String.format(java.util.Locale.ROOT, "%.1f", cooldown / 20.0f))
                        .withStyle(ChatFormatting.RED), true);
    }
}
