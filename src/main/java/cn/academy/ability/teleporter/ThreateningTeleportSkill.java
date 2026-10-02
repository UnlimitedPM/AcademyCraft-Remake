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
import javax.annotation.Nullable;

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
     * Cout en CP : 35 a 100, comme l'original, et il monte avec l'experience.
     *
     * C'est la competence qui coute le plus cher de la categorie, et de loin : lancer
     * ce qu'on tient est un geste deguise en attaque.
     */
    @Override
    public float getCpCost() {
        return 35f;
    }

    /** Le cout suit l'experience : de 1,25 a 3,5 selon le niveau d'usage. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(35f, 100f, data.getSkillExp(this));
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

    /**
     * Ou l'objet lance tomberait : sur les yeux d'une creature, ou la ou le regard butte.
     *
     * <p>Calculee a part pour la meme raison que chez les trois autres competences a marque : le
     * fantome du client doit se poser sur ce que le geste atteindra, et il ne peut le savoir qu'en
     * appelant la meme fonction. Derriere, l'objet suit exactement le meme chemin.
     */
    public Vec3 dropPosition(Player player, AbilityData data) {
        Entity target = aimed(player, data);
        return target != null
                ? target.position().add(0, target.getEyeHeight(), 0)
                : TargetingUtil.findImpactPoint(player, range(data));
    }

    /**
     * Ce que le geste trouve devant lui, ou {@code null} s'il n'y a rien.
     *
     * <p>Publique pour la meme raison que {@link #dropPosition} : la marque du client a besoin de
     * la creature elle-meme, et pas seulement de savoir qu'il y en a une — c'est sa <b>taille</b> qui
     * donne la sienne a la boite, et ses <b>pieds</b> qui la portent.
     */
    @Nullable
    public Entity aimed(Player player, AbilityData data) {
        return TargetingUtil.findEntityInSight(player, range(data));
    }

    /**
     * Y a-t-il quelqu'un a frapper, la ou l'objet tomberait ?
     *
     * <p>C'est ce que l'original lisait pour choisir la couleur de son marqueur : sa teinte
     * ordinaire pour le vide, et sa teinte « threating » des qu'une creature se trouvait sous le
     * geste. Le port en fait le drapeau du fantome, qui rougit pour la meme raison.
     */
    public boolean threatens(Player player, AbilityData data) {
        return aimed(player, data) instanceof LivingEntity;
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        Entity target = aimed(player, data);
        Vec3 impact = target != null
                ? target.position().add(0, target.getEyeHeight(), 0)
                : TargetingUtil.findImpactPoint(player, range(data));

        boolean hit = false;
        if (target instanceof LivingEntity living) {
            hit = true;
            // Le coup passe par les critiques de la teleportation : Dimension Folding Theorem
            // et Space Fluctuation augmentent ses degats, comme dans l'original ou il partait de
            // TPSkillHelper.attackIgnoreArmor.
            TeleportCrits.strike(player, data, living, scaled(damage(data, stack)));
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
