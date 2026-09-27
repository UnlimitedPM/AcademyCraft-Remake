package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Competence chargee, portage de FleshRipping : tenir la touche en visant quelqu'un,
 * et le frapper au relachement — a travers son armure.
 *
 * <p>C'est la teleportation retournee contre un corps : la cible est prise dans la
 * dechirure. Le coup ignore l'armure ({@code attackIgnoreArmor} de l'original), et une
 * fois sur vingt il laisse le lanceur la tete retournee — l'original appelait cela le
 * degout, cinq pour cent de chances et cinq secondes de nausee.
 *
 * <p>Tenir la touche sert a viser : la cible est relue a chaque tick, donc on peut
 * commencer a charger dans le vide et finir sur quelqu'un. Relacher sans cible ne coute
 * <b>rien</b> : ni les points, ni la recharge, ni l'experience, comme l'original qui
 * sortait par son {@code MSG_ABORT}.
 */
public class FleshRippingSkill extends Skill {

    /** Chance de degout, comme l'original. */
    private static final float DISGUST_CHANCE = 0.05f;

    public FleshRippingSkill() {
        super("flesh_ripping", 3);
    }

    /** Degats : de 5 a 12. */
    public float damage(AbilityData data) {
        return lerp(5f, 12f, data.getSkillExp(this));
    }

    /** Portee : de 6 a 14 blocs. */
    public double range(AbilityData data) {
        return lerp(6f, 14f, data.getSkillExp(this));
    }

    /** Cout en CP : 130 a 270, comme l'original. */
    public float cpCost(AbilityData data) {
        return lerp(130f, 270f, data.getSkillExp(this));
    }

    /** Surcout : de 60 a 50, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(60f, 50f, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return 130f;
    }

    @Override
    public float getCpCost(AbilityData data) {
        return cpCost(data);
    }

    /**
     * Recharge : de 90 a 40 ticks.
     *
     * Elle est posee par l'effet et non par le paquet : l'original ne l'appliquait que
     * s'il avait trouve une cible, et le paquet, lui, ne sait pas si le coup est parti.
     */
    public int cooldown(AbilityData data) {
        return (int) lerp(90f, 40f, data.getSkillExp(this));
    }

    /** Tout vient du coup porte : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
    }

    /**
     * L'entretien de la visée : la reserve doit pouvoir payer le coup.
     *
     * L'original verifiait a chaque tick que la cible restait a portee et que la reserve
     * suivait ; faute de quoi il terminait, donc sans rien facturer. C'est aussi ce qui
     * rend le paiement force du relachement sans danger.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        return data.getControlPoint() >= cpCost(data);
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        Entity found = TargetingUtil.findEntityInSight(player, range(data));
        if (!(found instanceof LivingEntity target)) return;

        // Paiement force, comme consumeWithForce : le tick de visee a deja verifie que la
        // reserve suivait, et un coup parti se paie.
        data.performForced(cpCost(data), getOverloadCost(data));
        // Les degats passent par les critiques de la teleportation : c'est par la que l'original
        // les faisait passer (TPSkillHelper.attackIgnoreArmor), et c'est ce qui donne son sens a
        // Space Fluctuation.
        TeleportCrits.strike(player, data, target, scaled(damage(data)));
        // Le son du coup, pose sur la cible dans l'original — mais il n'y a qu'un joueur
        // pour l'entendre, donc le port le lui donne directement.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_GUTS, 0.6f);

        if (player.getRandom().nextFloat() < DISGUST_CHANCE) {
            // 100 ticks, soit cinq secondes : joli geste, vilaine sensation.
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100));
        }

        data.setCooldown(this, cooldown(data));
        data.addSkillExp(this, 0.005f);
    }
}
