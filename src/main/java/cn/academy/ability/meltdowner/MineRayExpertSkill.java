package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;

/**
 * Rayon minier de l'expert, portage de {@code MineRayExpert} : deux fois plus long, deux
 * fois plus rapide, et il perce l'obsidienne.
 *
 * <p>Le meme travail que le rayon de base, en mieux — c'est ce que l'original vendait :
 * vingt blocs au lieu de dix, une durete enlevee par tick deux fois et demie plus grande,
 * et de quoi trouer ce qui resiste. Les butins restent ceux du bloc.
 */
public class MineRayExpertSkill extends MineRaySkill {

    public MineRayExpertSkill() {
        super("mine_ray_expert", 4);
    }

    @Override
    public double range() {
        return 20.0;
    }

    @Override
    public int tier() {
        return 5;
    }

    @Override
    public float speed(AbilityData data) {
        return lerp(0.5f, 1f, data.getSkillExp(this));
    }

    @Override
    public float cpPerTick(AbilityData data) {
        return lerp(0.9f, 0.54f, data.getSkillExp(this));
    }

    @Override
    public float expPerBlock() {
        return 0.0003f;
    }

    @Override
    public int cooldown(AbilityData data) {
        return (int) lerp(60f, 30f, data.getSkillExp(this));
    }

    @Override
    public net.minecraftforge.registries.RegistryObject<net.minecraft.sounds.SoundEvent> startupSound() {
        return cn.academy.ModSounds.MD_MINE_EXPERT_STARTUP;
    }

    /** Surcout : de 300 a 200, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(300f, 200f, data.getSkillExp(this));
    }
}
