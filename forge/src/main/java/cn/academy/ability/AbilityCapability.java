package cn.academy.ability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class AbilityCapability {

    public static final Capability<AbilityData> ABILITY_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private AbilityCapability() {}
}
