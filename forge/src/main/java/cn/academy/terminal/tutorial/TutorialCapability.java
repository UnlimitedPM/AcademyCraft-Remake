package cn.academy.terminal.tutorial;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/** La capacite qui porte {@link TutorialData} sur un joueur. */
public final class TutorialCapability {

    public static final Capability<TutorialData> TUTORIAL_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private TutorialCapability() {}
}
