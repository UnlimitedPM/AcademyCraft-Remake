package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Active skill, port of original ElectronBomb: throws a small area explosion at the aimed spot. */
public class ElectronBombSkill extends Skill {

    private static final float CP_COST = 30f;
    private static final double RANGE = 15;
    private static final double RADIUS = 3;

    public ElectronBombSkill() {
        super("electron_bomb", 1);
    }

    /** Degats repris de l'original : de 6 a 12 selon l'experience. */
    public float damage(AbilityData data) {
        return lerp(6f, 12f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 20 a 10 ticks, soit 1 a 0,5 seconde. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(20f, 10f, data.getSkillExp(this));
    }

    /** 0,005 au lancer, comme dans l'original. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.005f;
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    /**
     * Surcout repris de l'original : 200 points pour ouvrir la competence.
     *
     * L'original tenait une reserve ({@code overload_keep = 200}) pendant qu'il
     * lancait ses billes, puis 5 points par bille. Le port joue la competence d'un
     * seul coup, donc c'est le cout d'ouverture qui s'applique.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return 200f;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(RANGE));

        // Stop at the first block in the way instead of always exploding at max range.
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();

        AABB area = new AABB(target, target).inflate(RADIUS);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player);
        for (LivingEntity living : nearby) {
            living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
        }
    }
}
