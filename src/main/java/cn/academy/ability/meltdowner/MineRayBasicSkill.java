package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;

/**
 * Rayon minier de base, portage de {@code MineRayBasic} : le premier des trois, celui qui
 * perce la pierre a dix blocs.
 *
 * <p>Un rayon en fer, au sens de la 1.12.2 : il perce ce qu'une pioche en fer perce, donc
 * pas l'obsidienne ni le diamant. Les butins sont ceux du bloc, sans bonus.
 */
public class MineRayBasicSkill extends MineRaySkill {

    public MineRayBasicSkill() {
        super("mine_ray_basic", 3);
    }

    @Override
    public double range() {
        return 10.0;
    }

    @Override
    public int tier() {
        return 2;
    }

    @Override
    public float speed(AbilityData data) {
        return lerp(0.2f, 0.4f, data.getSkillExp(this));
    }

    @Override
    public float cpPerTick(AbilityData data) {
        return lerp(12f, 7f, data.getSkillExp(this));
    }

    @Override
    public float expPerBlock() {
        return 0.0005f;
    }

    @Override
    public int cooldown(AbilityData data) {
        return (int) lerp(40f, 20f, data.getSkillExp(this));
    }

    @Override
    public net.minecraftforge.registries.RegistryObject<net.minecraft.sounds.SoundEvent> startupSound() {
        return cn.academy.ModSounds.MD_MINE_BASIC_STARTUP;
    }

    /** Surcout : de 200 a 150, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(200f, 150f, data.getSkillExp(this));
    }
}
