package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.MineDetectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * La detection de minerais, portage de {@code MineDetect} : un eclat, l'oeil voit a travers
 * la pierre, et les minerais s'allument.
 *
 * <p>C'est la seule competence du port qui ne fait <b>rien</b> au monde : elle ne casse pas,
 * ne blesse pas, ne deplace pas. Elle paye un coup d'oeil, et c'est le client qui le rend —
 * l'original balayait deja les blocs chez le joueur, dans une entite de rendu a lui. Le port
 * fait la meme chose, mais range l'etat du cote client et le dessine pendant le rendu du
 * monde, comme le bouclier de lumiere.
 *
 * <h2>Ce que le regard coute</h2>
 *
 * Le paiement est celui de toutes les competences TENUES : l'ouverture paie les deux
 * ressources d'un coup ({@link #getCpCost}, {@link #getOverloadCost}), et rien du tout si la
 * reserve manque — pas de facture, pas d'aveuglement, pas de recharge. L'entretien, lui, se
 * paie tick par tick dans {@link #onHoldTick}, et la recharge ne tombe qu'a la fermeture :
 * second appui, reserve epuisee, ou plafond de la competence.
 *
 * <p>C'est la meme mecanique que les ailes d'orage, qui tiennent tant que la reserve suit :
 * la competence est une <b>bascule</b> ({@link #isToggle}), et le moteur ferme sur un second
 * appui avant tout refus — on ne peut donc jamais rester coince dans un eclat qu'on ne peut
 * plus eteindre.
 *
 * <p>Le prix suit l'experience a l'envers des habitudes : 1500 a 1000 CP et
 * 200 a 180 de surcout, donc moins cher quand on sait faire, et la portee grandit en meme
 * temps (15 a 30 blocs). Quarante-cinq secondes de recharge au depart, vingt au maximum.
 *
 * <h2>Ce que l'oeil voit</h2>
 *
 * Le joueur devient <b>aveugle</b> cent ticks — l'original payait la vision des minerais par
 * la perte de la vue ordinaire, et le port ne l'a pas adouci. Les minerais, eux, s'allument a
 * travers la pierre, et prennent une couleur par palier de pioche une fois la competence
 * <b>complete</b> : cinquante pour cent d'experience <b>et</b> une categorie de niveau 4. Le
 * palier lu est celui de la 1.12.2 — rien, la pierre, le fer, le diamant — transpose des
 * etiquettes de la 1.20.1, et c'est le seul endroit du port qui s'en serve.
 *
 * <p>Le son de l'eclat est bien la : le paquet l'annonce, et c'est le client qui le joue — voir
 * {@code MineDetectOverlay}. Le port n'a rien d'autre a porter : l'original n'avait ni onde ni
 * entite visible, seulement ses cubes textures, et c'est le rendu qui les dessine (voir
 * {@code MineDetectRenderer}). Sa parente, elle, est reposee : l'original la faisait descendre
 * de {@code mag_manip} avec toute son experience, ce que la categorie declare maintenant.
 *
 * <p>Un detail de l'original, conserve tel quel : l'experience est versee <b>avant</b> que la
 * recharge ne soit lue, et la recharge depend de l'experience. Le premier eclat pose donc
 * 896 ticks au lieu de 900, la courbe ayant deja glisse de huit millièmes. C'est le meme
 * genre d'auto-influence que la deviation de vecteur, qui s'amortit avec l'experience qu'elle
 * vient de donner.
 */
public class MineDetectSkill extends Skill {

    /** L'eclat, en ticks : cent, comme l'aveuglement et le temps de l'affichage. */
    public static final int TIME = 100;

    /**
     * L'entretien : ce que coute chaque tick d'eclat <b>au-dela</b> des cent premiers.
     *
     * <p>Demande du joueur le 26/10 : l'eclat ne doit plus etre seulement chronometre, il doit
     * pouvoir tenir tant que la reserve suit. Huit CP par tick font vingt secondes pour 160 :
     * c'est le prix d'un balayage qui continue. Il se paie <b>tick par tick</b>, dans
     * {@link #tick}, et non d'avance : le joueur doit pouvoir s'arreter au milieu.
     */
    public static final float HOLD_CP_PER_TICK = 8f;

    /** La portee : 15 blocs au depart, 30 au maximum d'experience. */
    public static final float RANGE_MIN_EXP = 15f;
    public static final float RANGE_MAX_EXP = 30f;

    /** Ce qu'il coute : 1500 a 1000 CP, et 200 a 180 de surcout, comme l'original. */
    public static final float CP_MIN_EXP = 1500f;
    public static final float CP_MAX_EXP = 1000f;
    public static final float OVERLOAD_MIN_EXP = 200f;
    public static final float OVERLOAD_MAX_EXP = 180f;

    /** La recharge : 45 secondes au depart, 20 au maximum. */
    public static final int COOLDOWN_MIN_EXP = 400;
    public static final int COOLDOWN_MAX_EXP = 900;

    /** 0,008 d'experience par eclat. */
    public static final float EXP_PER_CAST = 0.008f;

    /** L'eclat complet : cinquante pour cent d'experience et une categorie de niveau 4. */
    public static final float ADVANCED_EXP = 0.5f;
    public static final int ADVANCED_LEVEL = 4;

    public MineDetectSkill() {
        super("mine_detect", 3);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    /** La portee du balayage. */
    public float range(AbilityData data) {
        return lerp(RANGE_MIN_EXP, RANGE_MAX_EXP, data.getSkillExp(this));
    }

    /** Ce qu'un eclat coute a la reserve. */
    public float consumption(AbilityData data) {
        return lerp(CP_MIN_EXP, CP_MAX_EXP, data.getSkillExp(this));
    }

    /** Et a la surcharge. */
    public float overload(AbilityData data) {
        return lerp(OVERLOAD_MIN_EXP, OVERLOAD_MAX_EXP, data.getSkillExp(this));
    }

    /** La recharge posee par l'eclat, et seulement s'il a eu lieu. */
    public int cooldown(AbilityData data) {
        return (int) lerp(COOLDOWN_MAX_EXP, COOLDOWN_MIN_EXP, data.getSkillExp(this));
    }

    /**
     * L'eclat complet : la couleur par palier de pioche, et pas seulement la lueur.
     *
     * C'est la double condition de l'original — cinquante pour cent d'experience dans la
     * competence, <b>et</b> une categorie de niveau 4. Un debutant voit les minerais ; un
     * electromaster accompli sait lesquels valent une meilleure pioche.
     */
    public boolean advanced(AbilityData data, int categoryLevel) {
        return data.getSkillExp(this) > ADVANCED_EXP && categoryLevel >= ADVANCED_LEVEL;
    }

    // ------------------------------------------------------------------
    // L'eclat
    // ------------------------------------------------------------------

    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /** Le prix d'ouverture : ce que l'eclat coute a la reserve, paye a l'ouverture. */
    @Override
    public float getCpCost() {
        return CP_MIN_EXP;
    }

    @Override
    public float getCpCost(AbilityData data) {
        return consumption(data);
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    /**
     * La recharge n'est plus posee a l'ouverture, mais a la FERMETURE.
     *
     * <p>C'est toute la demande du joueur du 26/10 : « fait en sorte que le cooldown ne soit
     * qu'apres avoir desactive la competence ». C'est {@code AbilityEvents.endHeld} qui la pose,
     * au meme moment pour un second appui et pour une fin par epuisement — les deux chemins
     * passent par lui.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return cooldown(data);
    }

    // ------------------------------------------------------------------
    // Le maintien : une BASCULE, comme les ailes d'orage
    // ------------------------------------------------------------------

    /**
     * L'eclat est une competence TENUE : il vit tant que la reserve suit, et pas sur un minuteur.
     *
     * <p>Le parcours est celui du moteur, et c'est ce qui donne au joueur ce qu'il demandait :
     * un appui ouvre ({@code beginHeld}), le prix d'ouverture est paye une fois, chaque tick
     * d'entretien est paye par {@link #onHoldTick}, et la recharge tombe a la fin — par
     * {@code endHeld}, que la fin vienne d'un second appui ou de la reserve.
     */
    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Et il se <b>bascule</b> : un appui l'allume, un second l'eteint.
     *
     * <p>C'est la regle du moteur (voir {@code ActivateSkillPacket.handle}) : une bascule se
     * ferme sur un second appui, et fermer ne se refuse jamais — ni surcharge, ni recharge, ni
     * config ne s'y opposent. Le joueur n'avait donc plus a attendre pour l'eteindre.
     */
    @Override
    public boolean isToggle() {
        return true;
    }

    /**
     * L'allongement maximal de l'eclat, en ticks : RIEN a 0 % d'experience, soixante secondes a
     * 100 %, ou le plafond disparait et la reserve decide seule.
     *
     * <p>Demande du joueur : l'eclat doit s'allonger avec la competence, mais rester identique a
     * l'original tant qu'on ne l'a pas montee.
     */
    public static final int HOLD_TICKS_MIN_EXP = 0;
    public static final int HOLD_TICKS_MAX_EXP = 1200;

    /**
     * Les ticks d'entretien que la competence autorise. Zero a 0 %, sans fin a 100 %.
     *
     * <p>Zero est ce qui garde l'eclat de l'original : cent ticks, puis la recharge. A 100 %, le
     * plafond disparait et c'est la reserve qui decide — ce que le joueur a demande aux deux
     * etapes : « le temps s'allonge avec l'experience », puis « tant qu'il peut consommer des
     * CP l'effet dure ».
     */
    public int extensionTicks(AbilityData data) {
        float exp = data.getSkillExp(this);
        if (exp >= 1f) return Integer.MAX_VALUE;
        return Math.round(lerp(HOLD_TICKS_MIN_EXP, HOLD_TICKS_MAX_EXP, exp));
    }

    /**
     * La duree maximale du maintien : les cent ticks de l'original, plus ce que la competence
     * offre d'entretien.
     *
     * <p>Zero veut dire « aucune duree » dans le moteur : a 100 % d'experience, c'est donc la
     * reserve seule qui decide, et l'eclat tient tant qu'elle paie.
     */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        int extension = extensionTicks(data);
        return extension == Integer.MAX_VALUE ? 0 : TIME + extension;
    }

    /**
     * L'ouverture : l'aveuglement, l'experience, et le paquet qui allume le client.
     *
     * <p>Appele par {@code beginHeld} APRES paiement : une ouverture refusee n'allume donc rien
     * et ne verse rien, exactement comme l'eclat de l'original, dont tout le corps vivait dans
     * un {@code if} de paiement.
     */
    @Override
    public void onStart(Player player, AbilityData data) {
        // L'aveuglement, sans ses particules : l'original laissait celles de la cecite, et le
        // joueur les a refusees — elles tourbillonnent autour de lui et se lisent par-dessus la
        // pierre qu'il est justement en train de regarder. Pendant l'entretien, il est rafraichi
        // par {@link #onHoldTick} : c'est la duree de l'eclat qui commande, pas un minuteur.
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TIME, 0, false, false, true));
        data.addSkillExp(this, EXP_PER_CAST);

        if (player instanceof ServerPlayer server) {
            // `hold` : l'eclat peut durer au-dela des cent ticks, contre la reserve. A 0 %
            // d'experience l'entretien vaut zero, et le client se contente donc des cent ticks —
            // c'est l'eclat de l'original, au tick pres.
            AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> server),
                    new MineDetectPacket(range(data),
                            advanced(data, data.getCategoryLevel(getCategory())), TIME,
                            extensionTicks(data) > 0));
        }
    }

    /**
     * L'entretien, un tick a la fois : c'est ici que l'eclat paie sa duree.
     *
     * <p>Les cent premiers ticks sont deja payes par l'ouverture. Au-dela, chaque tick coute
     * {@link #HOLD_CP_PER_TICK}, et rendre faux quand la reserve ne suit plus fait fermer
     * l'eclat par le moteur — donc poser la recharge, comme un second appui.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        if (heldTicks <= TIME) return true;

        if (data.getControlPoint() < HOLD_CP_PER_TICK) return false;
        data.performForced(HOLD_CP_PER_TICK, 0f);
        // L'aveuglement suit l'eclat : rafraichi tant qu'il dure, il tombe avec lui.
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 8, 0, false, false, true));
        return true;
    }

    /**
     * La fermeture : plus d'aveuglement, et le client eteint son eclat.
     *
     * <p>La recharge, elle, n'est pas posee ici : c'est {@code endHeld} qui la pose juste apres,
     * avec le chiffre de la competence (voir {@link #getCooldownTicks}).
     */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        player.removeEffect(MobEffects.BLINDNESS);
        if (player instanceof ServerPlayer server) {
            AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> server),
                    new cn.academy.ability.network.MineDetectStopPacket());
        }
    }
}
