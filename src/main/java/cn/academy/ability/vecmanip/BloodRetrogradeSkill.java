package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Le retour de sang, portage de {@code BloodRetrograde} : le joueur touche ce qu'il a sous
 * la main, et le sang reflue.
 *
 * <p>La plus courte portee du port — <b>deux blocs</b> — pour le plus gros coup : 30 a 60
 * points de degats, la ou le railgun en fait 60 a 110 pour un rayon de vingt-cinq. C'est
 * une competence de contact, et elle ne part que si la main trouve quelque chose : sans
 * cible, <b>rien n'est facture</b>, pas meme une recharge. Le paquet ne peut pas le savoir
 * a l'avance, donc le prix se paie dans l'effet — voir {@link Skill#paysOnEffect()}.
 *
 * <p>La touche n'a pas de duree : l'original tirait tout seul au bout de trente ticks, mais
 * ce n'etait qu'un garde-fou de confort. Le port attend le relachement, comme le thunder clap
 * (voir l'ecart assume dans {@code ThunderClapSkill}) : tenir plus longtemps n'apporte rien,
 * puisque les degats ne dependent pas de la charge.
 *
 * <p>Non porte : l'effet de sang (les eclaboussures et les jets sur les murs, envoyes au
 * client par l'original), et le ralentissement du joueur pendant la charge — l'original
 * baissait pour cela {@code capabilities.walkSpeed}, un reglage que le deplacement n'utilise
 * pas (il lit l'attribut de vitesse) : le port ne reprend pas un geste qui ne changeait rien.
 */
public class BloodRetrogradeSkill extends Skill {

    /** La portee du contact : deux blocs, comme l'original. */
    public static final double REACH = 2.0;

    /** Les degats : 30 a 60, le plus gros coup du port. */
    public static final float DAMAGE_MIN = 30f;
    public static final float DAMAGE_MAX = 60f;

    /** Le cout en CP : 280 a 350 chez l'original, divises par 28. */
    public static final float CP_MIN = 10f;
    public static final float CP_MAX = 12.5f;

    /** Le surcout : 55 a 40, comme l'original. */
    public static final float OVERLOAD_MIN = 55f;
    public static final float OVERLOAD_MAX = 40f;

    /** 0,002 par contact porte. */
    public static final float EXP = 0.002f;

    public BloodRetrogradeSkill() {
        super("blood_retro", 4);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    public float damage(AbilityData data) {
        return lerp(DAMAGE_MIN, DAMAGE_MAX, data.getSkillExp(this));
    }

    public float consumption(AbilityData data) {
        return lerp(CP_MIN, CP_MAX, data.getSkillExp(this));
    }

    public float overload(AbilityData data) {
        return lerp(OVERLOAD_MIN, OVERLOAD_MAX, data.getSkillExp(this));
    }

    /** La recharge qu'un contact pose : de quatre secondes et demie a deux. */
    public int cooldown(AbilityData data) {
        return (int) lerp(90f, 40f, data.getSkillExp(this));
    }

    /** Tout est verse par le contact : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * Le prix se paie dans l'effet, et seulement s'il y a un contact.
     *
     * <p>L'original ne facturait rien quand la touche ne trouvait personne : c'etait son
     * client qui verifiait la cible avant d'envoyer quoi que ce soit. Le port fait ce
     * controle sur le serveur, donc au meme endroit que le paiement — voir
     * {@link Skill#paysOnEffect()}.
     */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    @Override
    public float getCpCost() {
        return 0f;
    }

    /** Le surcout declare, et paye par l'effet avec le reste. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    /** La recharge est posee par le contact : voir {@link #cooldown}. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    // ------------------------------------------------------------------
    // La charge, et le contact
    // ------------------------------------------------------------------

    @Override
    public boolean isChargeable() {
        return true;
    }

    /** Aucun minimum : la touche part des qu'elle trouve quelque chose a toucher. */
    @Override
    public int getMinChargeTicks(AbilityData data) {
        return 0;
    }

    /** Et aucun maximum : l'original s'arrêtait a trente ticks, sans que cela change rien. */
    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return 0;
    }

    /**
     * Ce que la main trouve, s'il y a quelque chose.
     *
     * <p>Le rayon est celui de l'original : deux blocs, et <b>des corps vivants seulement</b>
     * — viser une barque ne rend pas de sang. Fonction de lecture, donc : le serveur la refait
     * au relachement, la ou l'original faisait confiance a la cible annoncee par son client.
     */
    public LivingEntity touch(Player player) {
        Vec3 eye = player.getEyePosition(1f);
        Vec3 end = eye.add(player.getViewVector(1f).scale(REACH));
        Entity found = TargetingUtil.findEntityAlong(player, eye, end,
                e -> e instanceof LivingEntity);
        return found instanceof LivingEntity living ? living : null;
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        LivingEntity target = touch(player);
        // Rien sous la main : rien du tout, et rien de facture.
        if (target == null) return;
        if (!data.perform(consumption(data), overload(data))) return;

        target.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
        data.addSkillExp(this, EXP);
        data.setCooldown(this, cooldown(data));
    }
}
