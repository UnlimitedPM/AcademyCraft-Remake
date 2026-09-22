package cn.academy.ability.teleporter;

import cn.academy.ModItems;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Competence chargee, portage de ThreateningTeleport : le joueur lance l'objet qu'il
 * tient, et l'objet frappe.
 *
 * <p>C'est la porte d'entree de la categorie teleporteur dans l'original — c'est elle
 * qui apprend aux autres a viser. Le geste est simple : tenir un objet, garder la touche
 * enfoncee, et l'objet part au relachement. S'il touche une creature, l'objet a trois
 * chances sur dix de tomber la ou il a frappe ; sinon il tombe toujours.
 *
 * <p>Le coup <b>ignore l'armure</b>, comme {@code attackIgnoreArmor} de l'original, et
 * une aiguille fait moitie plus mal que n'importe quoi d'autre — c'est le seul objet que
 * l'original distinguait.
 */
public class ThreateningTeleportSkill extends Skill {

    public ThreateningTeleportSkill() {
        super("threatening_teleport", 1);
    }

    /** Degats de base : de 3 a 6 selon l'experience. */
    public float damage(AbilityData data) {
        return lerp(3f, 6f, data.getSkillExp(this));
    }

    /** Degats de l'objet tenu : une aiguille fait moitie plus mal, comme l'original. */
    public float damage(AbilityData data, ItemStack stack) {
        float base = damage(data);
        return stack.is(ModItems.NEEDLE.get()) ? base * 1.5f : base;
    }

    /** Portee du lancer : de 8 a 15 blocs. */
    public double range(AbilityData data) {
        return lerp(8f, 15f, data.getSkillExp(this));
    }

    /** Surcout : de 18 a 10, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(18f, 10f, data.getSkillExp(this));
    }

    /**
     * Cout en CP : 35 a 100 sur la reserve de l'original, donc 1,25 a 3,5 sur 100.
     *
     * C'est la competence qui coute le plus cher de la categorie, et de loin : lancer
     * ce qu'on tient est un geste deguise en attaque.
     */
    @Override
    public float getCpCost() {
        return 1.25f;
    }

    /** Le cout suit l'experience : de 1,25 a 3,5 selon le niveau d'usage. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(1.25f, 3.5f, data.getSkillExp(this));
    }

    /** Recharge : de 30 a 15 ticks. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(30f, 15f, data.getSkillExp(this));
    }

    /**
     * Experience : 0,0006 pour un lancer dans le vide, 0,003 quand l'objet frappe.
     *
     * L'original multipliait 0,003 par 0,2 quand rien n'etait touche. Le port verse la
     * petite part au paquet, qui ne peut pas savoir, et l'effet ajoute le reste.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.0006f;
    }

    /**
     * Le lancer se charge : l'original executait au relachement, et rien avant.
     *
     * Pas de duree maximale non plus : garder la touche enfoncee ne change rien, cela
     * attend simplement le geste.
     */
    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
    }

    /**
     * Rien dans la main, rien a lancer.
     *
     * L'original terminait des que la main se vidait, meme en plein maintien : le port
     * abandonne donc la charge, sans rien facturer.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        return !player.getMainHandItem().isEmpty();
    }

    @Override
    public boolean canStart(Player player, AbilityData data) {
        return !player.getMainHandItem().isEmpty();
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        Entity target = TargetingUtil.findEntityInSight(player, range(data));
        Vec3 impact = target != null
                ? target.position().add(0, target.getEyeHeight(), 0)
                : TargetingUtil.findImpactPoint(player, range(data));

        boolean hit = false;
        if (target instanceof LivingEntity living) {
            hit = true;
            // indirectMagic : c'est le type de degats qui traverse l'armure, ce que
            // l'original demandait explicitement.
            living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data, stack)));
            // Le son ne part que si l'objet a frappe : l'original le jouait sous un
            // `if(attacked)`, un lancer dans le vide restant muet.
            cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP, 0.5f);
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        // L'objet tombe la ou il a frappe — toujours s'il n'a rien touche, trois fois
        // sur dix sinon : une arme de jet qu'on ramasse a chaque fois ne serait pas une
        // arme de jet.
        double dropChance = hit ? 0.3 : 1.0;
        if (player.getRandom().nextDouble() < dropChance) {
            ItemEntity drop = new ItemEntity(player.level(), impact.x, impact.y, impact.z,
                    stack.copyWithCount(1));
            player.level().addFreshEntity(drop);
        }

        if (hit) {
            data.addSkillExp(this, 0.0024f);
        }
    }
}
