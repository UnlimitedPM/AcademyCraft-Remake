package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Enregistrement des evenements sonores du mod sous Fabric.
 *
 * <p>Les identifiants sont exactement ceux de {@code ModSounds} cote Forge et ceux de
 * {@code assets/academy/sounds.json}. Un evenement declare dans le fichier mais absent du
 * registre se joue dans un silence total, sans erreur : c'est pourquoi les deux listes doivent
 * rester identiques.
 */
final class FabricSounds {
    private static final String[] EVENTS = {
            "ability.deny",
            "em.arc_weak",
            "em.arc_strong",
            "em.minedetect",
            "em.railgun",
            "em.move_loop",
            "em.charge_loop",
            "em.intensify_activate",
            "em.intensify_loop",
            "em.lf_loop",
            "em.mag_manip",
            "md.ballshoot",
            "md.ray_small",
            "md.shield_startup",
            "md.shield_loop",
            "md.meltdowner",
            "md.mine_loop",
            "md.mine_basic_startup",
            "md.mine_luck_startup",
            "md.mine_expert_startup",
            "md.simple_charge",
            "md.md_charge",
            "entity.flipcoin",
            "entity.silbarn_heavy",
            "entity.silbarn_light",
            "terminal.select",
            "terminal.confirm",
            "tp.tp",
            "tp.tp_pre",
            "tp.guts",
            "tp.tp_shift",
            "tp.tp_flashing",
            "vecmanip.blood_retro",
            "vecmanip.directed_shock",
            "vecmanip.groundshock",
            "vecmanip.directed_blast",
            "vecmanip.vec_accel",
            "vecmanip.plasma_cannon",
            "vecmanip.storm_wing",
            "vecmanip.vec_deviation",
            "vecmanip.vec_reflection",
            "vecmanip.plasma_cannon_t",
            "machine.imag_fusor_work",
            "machine.machine_work",
            "media.only_my_railgun",
            "media.level5_judgelight",
            "media.sisters_noise"
    };

    private FabricSounds() {
    }

    static void register() {
        for (String name : EVENTS) {
            ResourceLocation id = new ResourceLocation(AcademyCraftCommon.MOD_ID, name);
            Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        }
    }
}
