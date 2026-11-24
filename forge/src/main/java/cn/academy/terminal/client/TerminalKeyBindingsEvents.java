package cn.academy.terminal.client;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge de la touche du terminal.
 *
 * <p>La touche et son geste sont communs — voir {@link TerminalKeyBindings} ; ce qui reste ici,
 * c'est le registre du chargeur et son tick de client.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class TerminalKeyBindingsEvents {

    private TerminalKeyBindingsEvents() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        TerminalKeyBindings.register(event::register);
    }

    /** Le tick passe par la boucle d'evenements du jeu, pas par celle du mod. */
    @Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
    public static final class Tick {

        private Tick() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            TerminalKeyBindings.tick();
        }
    }
}
