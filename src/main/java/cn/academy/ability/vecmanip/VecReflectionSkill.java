package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Passive skill, port of original VecReflection: redirects part of incoming damage back
 * at the attacker and absorbs part of it, at the cost of Control Points per hit.
 */
public class VecReflectionSkill extends Skill {

    /**
     * Part des degats renvoyee, reprise de l'original : de 0,6 a 1,2 selon
     * l'experience. Au maximum elle renvoie donc plus qu'elle n'encaisse.
     */
    public float reflectRatio(AbilityData data) {
        return lerp(0.6f, 1.2f, data.getSkillExp(this));
    }

    private static final float ABSORB_RATIO = 0.3f;
    private static final float CP_COST_PER_HIT = 5f;

    public VecReflectionSkill() {
        super("vec_reflection", 4);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return event.getAmount();
        if (!data.consumeControlPoint(CP_COST_PER_HIT)) return event.getAmount();

        float amount = event.getAmount();
        float reflected = amount * reflectRatio(data);
        float absorbed = amount * ABSORB_RATIO;
        attacker.hurt(player.damageSources().magic(), reflected);

        // L'original versait 0,0008 par point de difficulte de la cible renvoyee. Le
        // port n'a pas cette difficulte : c'est le montant renvoye qui sert de base,
        // ce qui donne la meme chose pour un adversaire ordinaire.
        data.addSkillExp(this, reflected * 0.0008f);
        return amount - absorbed;
    }
}
