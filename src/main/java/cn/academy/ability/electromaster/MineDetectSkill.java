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
 * Le paiement n'est pas celui des autres competences : l'original ne consommait la reserve
 * <b>qu'une fois son effet decide</b>, et rien du tout si elle manquait — pas de facture, pas
 * d'aveuglement, pas de recharge. Le port le dit avec {@link #paysOnEffect()} : le paquet
 * d'activation ne paie rien, et c'est l'effet qui decide. C'est la meme mecanique que l'onde
 * de choc au sol, qui ne coute rien quand le joueur a les pieds en l'air.
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

    /** Le paquet d'activation ne paie rien : l'effet decide, et paie lui-meme. */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    /** L'experience aussi est versee par l'effet, et seulement s'il a eu lieu. */
    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /** Aucun cout declare : c'est l'effet qui paie, et il paie les deux ressources. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    /** Aucune recharge declaree : l'eclat la pose lui-meme, s'il a eu lieu. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    /**
     * L'eclat, cote serveur.
     *
     * <p>Rien n'est verse ni pose quand la reserve manque : l'original appelait
     * {@code consume} d'abord, et tout le reste vivait dans le {@code if}. Le client, lui,
     * n'apprend l'eclat que s'il a eu lieu — c'est ce qui l'empeche d'allumer des minerais
     * pour une competence qui a ete refusee.
     */
    @Override
    public void onActivate(Player player, AbilityData data) {
        if (!data.perform(consumption(data), overload(data))) return;

        // L'aveuglement, sans ses particules : l'original laissait celles de la cecite, et le
        // joueur les a refusees — elles tourbillonnent autour de lui et se lisent par-dessus la
        // pierre qu'il est justement en train de regarder. Le temoin du HUD, lui, reste : c'est
        // la seule chose qui dit que l'effet est encore la.
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TIME, 0, false, false, true));
        data.addSkillExp(this, EXP_PER_CAST);
        data.setCooldown(this, cooldown(data));

        if (player instanceof ServerPlayer server) {
            AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> server),
                    new MineDetectPacket(range(data), advanced(data, data.getCategoryLevel(getCategory()))));
        }
    }
}
