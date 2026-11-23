package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.arc.ArcRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Les deux crochets Forge du rendu des eclairs.
 *
 * <p>{@code ArcRenderer} est commun : il avance ses arcs et les dessine, mais ce sont les
 * evenements Forge qui savent QUAND. Cette classe ne fait que les brancher dessus.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class ArcRendererEvents {

    private ArcRendererEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ArcRenderer.tick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        ArcRenderer.render(event.getPoseStack(), event.getCamera().getPosition(),
                event.getCamera().getUpVector());
    }
}
