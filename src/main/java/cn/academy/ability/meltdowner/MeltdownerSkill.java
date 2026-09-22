package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Active skill, port of original Meltdowner: sustained plasma beam, single-target hitscan damage. */
public class MeltdownerSkill extends Skill {

    private static final float CP_COST = 20f;
    private static final double RANGE = 20;

    public MeltdownerSkill() {
        super("meltdowner", 3);
    }

    /**
     * Degats repris de l'original : de 18 a 50 selon l'experience.
     *
     * L'original les multipliait encore par un facteur de temps de charge ; le port
     * n'a pas ce temps de charge, donc c'est la courbe de base qui s'applique.
     */
    public float damage(AbilityData data) {
        return lerp(18f, 50f, data.getSkillExp(this));
    }

    /**
     * L'original multipliait 0,002 par le temps de charge du tir. Le port ne remonte
     * pas cette duree au paquet d'activation : c'est donc le montant de base qui est
     * verse, sans le bonus de charge.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.002f;
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
    }
}
