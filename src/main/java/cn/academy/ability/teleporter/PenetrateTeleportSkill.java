package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Active skill, port of original PenetrateTeleport: blinks forward through walls, ignoring obstacles. */
public class PenetrateTeleportSkill extends Skill {

    private static final float CP_COST = 30f;

    public PenetrateTeleportSkill() {
        super("penetrate_teleport", 2);
    }

    /**
     * Portee reprise de l'original : de 10 a 35 blocs selon l'experience.
     *
     * Le port se contentait de 6 blocs. Le cout en CP reste le sien : celui de
     * l'original (14 a 9) se rapporte a une autre reserve.
     */
    public double range(AbilityData data) {
        return lerp(10f, 35f, data.getSkillExp(this));
    }

    /**
     * L'original versait 0,00014 par bloc parcouru. Le paquet d'activation ne connait
     * pas la distance, donc c'est la valeur d'un saut d'une dizaine de blocs qui est
     * versee — la distance moyenne de cette competence.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.00014f * 10f;
    }

    @Override
    public float getCpCost() {
        return CP_COST;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        Vec3 start = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 dest = start.add(look.scale(range(data)));
        player.teleportTo(dest.x, dest.y - 1.6, dest.z);
        player.fallDistance = 0;
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
    }
}
