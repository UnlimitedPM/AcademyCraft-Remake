package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
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
 * <p>Ce que la charge coute :
 * <ul>
 *   <li>a l'appui, le surcout de l'original — 200 a 120 selon l'experience — epingle pendant
 *       toute la charge, exactement comme celui du claquement d'orage ;</li>
 *   <li>puis 20 a 15 CP par tick, et seulement les {@link #MAX_TIME} premiers : au-dela,
 *       l'original ne facturait plus rien.</li>
 * </ul>
 *
 * <p>Ce qu'elle rend : le renfort se lit sur le temps tenu. Sa probabilite vaut
 * {@code (ticks - 10) / 18}, donc nulle au minimum et depassee seulement a 28 ticks, et chaque
 * unite de cette probabilite vaut un effet de plus, tire parmi les cinq de l'original. Le tout
 * dure de 1,5 a 2,5 fois le temps tenu, et laisse de quoi manger.
 */
public class BodyIntensifySkill extends Skill {

    /** Le temps tenu en deca duquel rien ne se passe : l'original ne partait pas avant. */
    public static final int MIN_TIME = 10;

    /**
     * Au-dela, le renfort ne grandit plus, et l'original ne facturait plus.
     *
     * <p>Il laissait pourtant tenir jusqu'a cent ticks, en ne donnant rien du tout a celui qui
     * depassait — une punition, pas une regle. Le port s'arrete a ce plafond-ci : la charge part
     * toute seule a quarante ticks, et le renfort est alors au maximum, ce que l'original aurait
     * donne au meme moment.
     */
    public static final int MAX_TIME = 40;

    /** Le facteur de duree du renfort : de 1,5 a 2,5 fois le temps tenu. */
    private static final float TIME_FACTOR_MIN = 1.5f;
    private static final float TIME_FACTOR_MAX = 2.5f;

    /** Ce que le renfort laisse derriere lui : 1,25 fois le temps tenu, au niveau deux. */
    private static final float HUNGER_FACTOR = 1.25f;
    private static final int HUNGER_LEVEL = 2;

    /** L'entretien de la charge, par tick : de 20 a 15 CP, comme l'original. */
    private static final float CP_PER_TICK_MIN = 20f;
    private static final float CP_PER_TICK_MAX = 15f;

    /** Les cinq effets de l'original, dans son ordre, avec le niveau ou il les plafonnait. */
    private record Buff(MobEffect effect, int maxLevel) {}

    /**
     * La table des effets, construite A LA DEMANDE — et pas en constante.
     *
     * <p>{@code MobEffects} est un REGISTRE, et il n'existe pas dans les tests unitaires, qui
     * chargent cette classe pour lire ses courbes. Une table posee en champ statique faisait donc
     * echouer la classe entiere des qu'on la lisait, et avec elle les categories qui la
     * contiennent.
     */
    private static List<Buff> buffs() {
        return List.of(
                new Buff(MobEffects.MOVEMENT_SPEED, 3),
                new Buff(MobEffects.JUMP, 1),
                new Buff(MobEffects.REGENERATION, 1),
                new Buff(MobEffects.DAMAGE_BOOST, 1),
                new Buff(MobEffects.DAMAGE_RESISTANCE, 1));
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

    /** L'entretien d'un tick de charge : de 20 a 15 CP selon l'experience. */
    public float chargeCpCost(AbilityData data) {
        return cpPerTick(data.getSkillExp(this));
    }

    /** Surcout d'ouverture : de 200 a 120, celui de l'original. */
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

    /** La competence paie elle-meme, a l'appui et tick par tick : rien a la fin. */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    /**
     * L'entretien de la charge : 20 a 15 CP par tick, sur les quarante premiers.
     *
     * <p>Au-dela, l'original ne facturait plus rien. Une reserve qui ne suit plus ferme la
     * charge, comme chez lui — et rien ne part alors, ni renfort ni recharge.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        if (chargeTicks > MAX_TIME) return true;
        return data.consumeControlPoint(chargeCpCost(data));
    }

    // --- LE RENFORT ---

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        applyBuffs(player, data, chargeTicks);

        cn.academy.sound.AcademySounds.playFor(player,
                cn.academy.ModSounds.EM_INTENSIFY_ACTIVATE, 0.5f);

        sendEffect(player);
    }

    /**
     * Le renfort, ecrit comme l'original : jusqu'a deux effets tires dans les cinq, et de quoi
     * manger.
     *
     * <p>Sa boucle vaut `while (p > 0)`: a la probabilite, on ajoute un effet, puis on retire
     * une unite. Deux effets au plus — la probabilite plafonne a 1,67 — et meme parfois aucun,
     * puisque c'est un tirage.
     *
     * <p>ECART ASSUME : l'original incrementait son compteur <b>avant</b> de lire sa table, donc
     * ne servait jamais le premier effet tire et decalait tous les autres d'un cran. Le port lit
     * a partir de zero — meme tirage, sans ce decalage.
     */
    private void applyBuffs(Player player, AbilityData data, int chargeTicks) {
        int held = Math.min(chargeTicks, MAX_TIME);

        List<Buff> pool = new ArrayList<>(buffs());
        Collections.shuffle(pool, new java.util.Random(player.getRandom().nextLong()));

        double chance = probability(held);
        int level = level(held);
        int time = buffTime(held, player.getRandom().nextDouble(), timeFactor(data.getSkillExp(this)));

        int taken = 0;
        while (chance > 0.0) {
            if (player.getRandom().nextDouble() < chance) {
                Buff buff = pool.get(taken % pool.size());
                taken++;
                player.addEffect(new MobEffectInstance(buff.effect(), time,
                        Math.min(level, buff.maxLevel()), false, true));
            }
            chance -= 1.0;
        }

        player.addEffect(new MobEffectInstance(MobEffects.HUNGER,
                (int) (HUNGER_FACTOR * held), HUNGER_LEVEL, false, true));
    }

    // --- LES NOMBRES, EN CLAIR, POUR LE TEST ---

    /** La probabilite d'un effet apres ce temps tenu : {@code (ticks - 10) / 18}. */
    public static double probability(int heldTicks) {
        return (heldTicks - MIN_TIME) / 18.0;
    }

    /** Le niveau donne : la partie entiere de la probabilite. */
    public static int level(int heldTicks) {
        return (int) Math.floor(probability(heldTicks));
    }

    /** Le facteur de duree, de 1,5 a 2,5 selon l'experience. */
    public static float timeFactor(double exp) {
        return (float) (TIME_FACTOR_MIN + (TIME_FACTOR_MAX - TIME_FACTOR_MIN) * exp);
    }

    /** La duree d'un effet, en ticks : {@code roll} fois le temps tenu fois le facteur. */
    public static int buffTime(int heldTicks, double roll, float factor) {
        return (int) (roll * heldTicks * factor);
    }

    /** L'entretien d'un tick de charge, de 20 a 15 CP. */
    public static float cpPerTick(double exp) {
        return (float) (CP_PER_TICK_MIN + (CP_PER_TICK_MAX - CP_PER_TICK_MIN) * exp);
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
