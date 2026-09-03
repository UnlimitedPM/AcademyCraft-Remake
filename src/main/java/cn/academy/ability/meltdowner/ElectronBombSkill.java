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
    private static final float DAMAGE = 10f;

    public ElectronBombSkill() {
        super("electron_bomb");
    }

    @Override
    public float getCpCost() {
        return CP_COST;
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
            living.hurt(player.damageSources().indirectMagic(player, player), DAMAGE);
        }
    }
}
