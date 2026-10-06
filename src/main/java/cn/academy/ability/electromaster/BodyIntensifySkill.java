package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Competence chargee, portage de BodyIntensify.
 *
 * <p>ON TIENT LA TOUCHE, et c'est le relachement qui lance le renfort. L'original ouvrait un
 * contexte a l'appui, le laissait courir tant que la touche restait enfoncee, et n'appliquait
 * ses effets qu'a la fin. Le port appliquait tout d'un coup, des l'appui, et le joueur l'a
 * repris : « on est cense charger l'attaque et relacher la touche pour l'utiliser, pas
 * simplement appuyer pour activer immediatement ».
 *
 * <p>Ce que la charge coute : <b>rien</b>. Le surcout de l'original (200 a 120 selon l'experience)
 * tombe a l'appui et reste epingle pendant toute la charge, comme celui du claquement d'orage, mais
 * la reserve n'est pas touchee — la charge ne fait que tenir la touche, et elle est gratuite comme
 * celle des ailes de tempete.
 *
 * <p>ECART ASSUME : l'original facturait 20 a 15 CP par tick de charge, ce qui avait un sens chez lui
 * — tenir plus longtemps donnait un renfort plus fort. Le port ne le fait plus (le contenu depend des
 * <b>paliers d'experience</b>, voir {@link #boostsFor}), donc payer par tick ne payait plus rien : le
 * joueur l'a vu tout de suite, « quand on reste appuyer sur la competence pour la preparer elle
 * consomme tout nos cp pour rien ». La charge est donc gratuite, et seuls le surcout d'ouverture et
 * la recharge se paient.
 *
 * <p>Ce qu'elle rend : le renfort est le meme pour tous ceux qui la connaissent aussi bien, et il
 * grandit par <b>paliers d'experience</b> — voir {@link #boostsFor}, qui porte la table. Le temps
 * tenu ne decide plus de rien, pas meme de la duree : celle-ci ne depend que de l'experience, de
 * {@link #DURATION_MIN_TICKS} a {@link #DURATION_MAX_TICKS} — cinq secondes au depart, dix a pleine
 * experience, ce que le joueur a demande (« la duree est tres nulle dans le vrai mod, je voudrais que
 * ca dure pendant 5 secondes jusqu'a 10 secondes au niveau max »).
 *
 * <p>ECART ASSUME : l'original tirait au sort un a deux effets parmi cinq, et plafonnait leur niveau
 * sur le temps tenu — on ne savait donc jamais ce qu'on obtenait, et rien ne s'ameliorait vraiment en
 * s'entrainant. Le joueur trouvait la competence « pas assez forte » et a donne ses paliers, qui
 * remplacent ce tirage : un socle de force, la vitesse puis la regeneration qui s'ajoutent, la famine
 * qui s'en va, et le tout d'un cran a pleine experience.
 */
public class BodyIntensifySkill extends Skill {

    /** Le temps tenu en deca duquel rien ne se passe : l'original ne partait pas avant. */
    public static final int MIN_TIME = 10;

    /**
     * Le plafond du compteur de charge : quarante ticks.
     *
     * <p>Il ne decide plus de rien depuis que le renfort suit l'experience — ni son contenu, ni sa
     * duree : c'est la borne de la <b>barre de charge</b>, et le temps qu'on peut tenir au-dela ne
     * change rien. L'original, lui, laissait tenir jusqu'a cent ticks en ne donnant rien du tout a
     * celui qui depassait — une punition, pas une regle.
     */
    public static final int MAX_TIME = 40;

    /** La duree du renfort : cinq secondes au depart, dix a pleine experience. */
    public static final int DURATION_MIN_TICKS = 100;
    public static final int DURATION_MAX_TICKS = 200;

    // --- CE QUE LE RENFORT DONNE ---

    /** A partir de la, la vitesse s'ajoute. Sous ce seuil, la force et la famine, rien de plus. */
    public static final float SPEED_FROM = 0.25f;

    /** Et a partir de la, la regeneration. */
    public static final float REGENERATION_FROM = 0.5f;

    /** A partir de la, la famine s'en va : le renfort ne se paie plus. */
    public static final float NO_FAMINE_FROM = 0.75f;

    /** Et a pleine experience, tout monte d'un cran : vitesse et regeneration au niveau II. */
    public static final float MASTERY = 1f;

    /**
     * Une famille d'effets du renfort.
     *
     * <p>PURE, et c'est voulu : les effets de Minecraft sont des entrees de <b>registre</b>, et il
     * n'existe pas dans les tests unitaires — qui chargent cette classe pour lire ses courbes. Le nom
     * de la famille se traduit donc en effet au dernier moment, dans {@link #effectOf}.
     */
    public enum Kind { FORCE, SPEED, REGENERATION, FAMINE }

    /** Un effet du renfort, et son niveau : 1 vaut « I », 2 vaut « II ». */
    public record Boost(Kind kind, int level) {}

    /**
     * Ce que le renfort donne a cette experience — les paliers du joueur, tels quels.
     *
     * <p>Il trouvait la competence « pas assez forte », et la logique de l'original n'etait pas une
     * progression : un tirage d'un a deux effets parmi cinq, qu'on ne pouvait ni prevoir ni ameliorer
     * en s'entrainant. Les paliers le remplacent :
     *
     * <ul>
     * <li><b>0 a 24 %</b> : force I, et la famine qui la paie ;</li>
     * <li><b>25 a 49 %</b> : la vitesse I s'ajoute ;</li>
     * <li><b>50 a 74 %</b> : la regeneration I s'ajoute ;</li>
     * <li><b>75 a 99 %</b> : la famine s'en va ;</li>
     * <li><b>100 %</b> : force I, vitesse II et regeneration II.</li>
     * </ul>
     *
     * <p>La force, elle, ne bouge jamais : c'est le socle de la competence, et elle est au niveau I
     * partout. L'ordre de la liste est celui dans lequel les effets se posent — le HUD du joueur les
     * montre dans cet ordre-la.
     */
    public static List<Boost> boostsFor(float exp) {
        List<Boost> boosts = new ArrayList<>();
        // Le socle, toujours.
        boosts.add(new Boost(Kind.FORCE, 1));

        if (exp >= MASTERY) {
            // A pleine experience : les deux qui restent passent au niveau II, et la famine s'en va.
            boosts.add(new Boost(Kind.SPEED, 2));
            boosts.add(new Boost(Kind.REGENERATION, 2));
            return List.copyOf(boosts);
        }
        if (exp >= SPEED_FROM) boosts.add(new Boost(Kind.SPEED, 1));
        if (exp >= REGENERATION_FROM) boosts.add(new Boost(Kind.REGENERATION, 1));
        if (exp < NO_FAMINE_FROM) boosts.add(new Boost(Kind.FAMINE, 1));
        return List.copyOf(boosts);
    }

    /** L'effet de Minecraft d'une famille. Le seul endroit qui nomme le registre. */
    private static MobEffect effectOf(Kind kind) {
        return switch (kind) {
            case FORCE -> MobEffects.DAMAGE_BOOST;
            case SPEED -> MobEffects.MOVEMENT_SPEED;
            case REGENERATION -> MobEffects.REGENERATION;
            case FAMINE -> MobEffects.HUNGER;
        };
    }

    public BodyIntensifySkill() {
        super("body_intensify", 3);
    }

    // --- CE QUE LA CHARGE COUTE ---

    /**
     * Rien a l'ouverture : la charge se paie tick par tick.
     *
     * <p>Le port facturait d'un coup ce que l'original prelevait pendant ses quarante ticks de
     * charge. C'est la meme somme, mais elle n'avait plus aucun sens des lors que la charge
     * existe : c'est son entretien, et il se paie comme celui du claquement d'orage.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getCpCost(AbilityData data) {
        return 0f;
    }

    /**
     * Surcout d'ouverture : de 200 a 120, celui de l'original.
     *
     * <p>C'est le SEUL prix de la competence avec sa recharge : la charge, elle, ne touche pas a la
     * reserve — voir {@link #onChargeTick}.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(200f, 120f, data.getSkillExp(this));
    }

    /** 0,01 a l'application du renfort, comme dans l'original. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.01f;
    }

    /** Recharge reprise de l'original : de 900 a 600 ticks, soit 45 a 30 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(900f, 600f, data.getSkillExp(this));
    }

    // --- LA CHARGE ---

    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public int getMinChargeTicks(AbilityData data) {
        return MIN_TIME;
    }

    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return MAX_TIME;
    }

    /**
     * A l'appui : le surcout tombe, et il reste epingle pendant toute la charge.
     *
     * <p>C'est le {@code s_consume} de l'original, qui prelevait son surcout a la naissance du
     * contexte et le reposait a chaque tick tant que la charge tenait — la meme chose que
     * l'epinglage du claquement d'orage.
     */
    @Override
    public void onStart(Player player, AbilityData data) {
        data.perform(0f, getOverloadCost(data));
        data.setHeldOverload(this, data.getOverload());
    }

    /** La competence paie elle-meme a l'appui : la charge, elle, est gratuite. */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    /**
     * Un tick de charge : <b>rien</b>.
     *
     * <p>L'original facturait 20 a 15 CP par tick, ce qui avait un sens chez lui : tenir plus
     * longtemps donnait un renfort plus fort. Ici le contenu comme la duree se lisent sur
     * l'experience et sur rien d'autre, donc payer par tick ne paierait rien du tout — c'est ce que
     * le joueur a vu, « quand on reste appuyer sur la competence pour la preparer elle consomme tout
     * nos cp pour rien ». La charge ne fait plus que tenir la touche, et c'est le relachement qui
     * applique le renfort ; une reserve vide ne l'arrete donc plus au milieu de la preparation.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        return true;
    }

    // --- LE RENFORT ---

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        applyBuffs(player, data);

        cn.academy.sound.AcademySounds.playFor(player,
                cn.academy.ModSounds.EM_INTENSIFY_ACTIVATE, 0.5f);

        sendEffect(player);
    }

    /**
     * Le renfort, pose tel que {@link #boostsFor} le decrit.
     *
     * <p>Tous ses effets durent {@link #durationTicks} — la famine comprise. C'est elle qui paie le
     * renfort, et c'est pour cela que la maitrise la fait disparaitre : la garder plus longtemps que
     * le benefice n'aurait pas de sens, et l'ancienne regle (1,25 fois le temps tenu) ne dit plus
     * rien depuis que la duree ne depend plus de la charge.
     */
    private void applyBuffs(Player player, AbilityData data) {
        int ticks = durationTicks(data.getSkillExp(this));
        for (Boost boost : boostsFor(data.getSkillExp(this))) {
            player.addEffect(new MobEffectInstance(effectOf(boost.kind()), ticks,
                    boost.level() - 1, false, true));
        }
    }

    // --- LES NOMBRES, EN CLAIR, POUR LE TEST ---

    /**
     * La duree du renfort, en ticks : de {@link #DURATION_MIN_TICKS} a {@link #DURATION_MAX_TICKS}
     * selon l'experience, soit cinq a dix secondes.
     *
     * <p>Elle ne depend NI du temps tenu, NI d'un tirage : le joueur a demande une fourchette, et
     * elle est la meme pour tous ses effets — l'ancienne (une fraction aleatoire du temps tenu, fois
     * 1,5 a 2,5) donnait des renforts d'une seconde ou deux, « la duree est tres nulle dans le vrai
     * mod ».
     */
    public static int durationTicks(float exp) {
        return (int) lerp(DURATION_MIN_TICKS, DURATION_MAX_TICKS, exp);
    }

    /**
     * L'electricite du renfort, chez ceux qui voient le joueur.
     *
     * <p>L'original en faisait une entite cliente, nee chez chacun quand le serveur annoncait
     * que le renfort avait pris — c'est son {@code MSG_EFFECT_END} avec l'argument vrai. Le port
     * envoie la meme chose, et un seul message : les sept hauteurs, leurs trois ou quatre arcs
     * et leurs delais sont une affaire d'<b>image</b>, et c'est le client qui les rejoue (voir
     * {@code BodyIntensifyEffect}). Le serveur, lui, n'a rien a savoir de tout ca.
     *
     * <p>Sans ce message, le renfort ne se verrait que chez celui qui appuie sur la touche, et
     * les autres joueurs ne verraient rien du tout.
     */
    private static void sendEffect(Player player) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                new cn.academy.ability.network.BodyIntensifyPacket(player.getId()));
    }
}
