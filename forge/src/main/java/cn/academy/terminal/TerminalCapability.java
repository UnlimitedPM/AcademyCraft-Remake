package cn.academy.terminal;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class TerminalCapability {

    public static final Capability<TerminalData> TERMINAL_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private TerminalCapability() {}
}
