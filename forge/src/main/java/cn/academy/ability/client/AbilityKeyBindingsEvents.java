package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge des touches d'aptitude.
 *
 * <p>Les touches sont communes — voir {@link AbilityKeyBindings} — ; ce qui reste ici, c'est le
 * registre du chargeur, qui n'existe que le temps de son evenement.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class AbilityKeyBindingsEvents {

    private AbilityKeyBindingsEvents() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        AbilityKeyBindings.register(event::register);
    }
}
