package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Random;

/** Active skill, port of original ArcGen: short-range electric arc, chance to set target on fire. */
public class ArcGenSkill extends Skill {

    private static final float CP_COST = 20f;
    private static final int IGNITE_TICKS = 80;

    private final Random random = new Random();

    public ArcGenSkill() {
        super("arc_gen", 1);
    }

    //
    // Courbes reprises de l'original : les degats vont de 5 a 9, la portee de 6 a 15
    // blocs, et la chance d'embraser de 0 a 60 %, le tout selon l'experience de la
    // competence. Le cout en CP, lui, reste celui du port : celui de l'original (30 a
    // 70) suppose une reserve de plusieurs milliers de points, la ou le port plafonne
    // a 100. Voir `ability.controlPointMax`.
    //

    public float damage(AbilityData data) {
        return lerp(5f, 9f, data.getSkillExp(this));
    }

    public double range(AbilityData data) {
        return lerp(6f, 15f, data.getSkillExp(this));
    }

    public float igniteChance(AbilityData data) {
        return lerp(0f, 0.6f, data.getSkillExp(this));
    }

    /**
     * Experience d'un arc qui touche, reprise de l'original : de 0,0048 a 0,0072 selon
     * l'experience deja acquise. L'original distinguait le coup porte du coup dans le
     * vide ; le port ne le peut plus, l'activation etant deja conditionnee a une cible
     * en vue, donc c'est la branche « touche » qui est reprise.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return lerp(0.0048f, 0.0072f, data.getSkillExp(this));
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Entity target = TargetingUtil.findEntityInSight(player, range(data));
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
        if (random.nextFloat() < igniteChance(data)) {
            living.setSecondsOnFire(IGNITE_TICKS / 20);
        }
    }
}
