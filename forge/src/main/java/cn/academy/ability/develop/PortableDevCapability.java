package cn.academy.ability.develop;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/** La capacite qui porte {@link PortableDevData} sur un joueur. */
public final class PortableDevCapability {

    public static final Capability<PortableDevData> PORTABLE_DEV =
            CapabilityManager.get(new CapabilityToken<>() {});

    private PortableDevCapability() {}
}
