package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge de la rafale d'arcs de {@code RailgunHandEffect}.
 *
 * <p>L'effet est commun ; ce qui reste ici, c'est l'etape de rendu et le tick du chargeur.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class RailgunHandEffectEvents {

    private RailgunHandEffectEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        RailgunHandEffect.render(event.getCamera(), event.getPoseStack(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        RailgunHandEffect.tick();
    }
}
