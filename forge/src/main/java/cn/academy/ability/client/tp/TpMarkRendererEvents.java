package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge du rendu de {@code TpMarkRenderer}.
 *
 * <p>Le dessin lui-meme est commun ; ce qui reste ici, c'est l'evenement du chargeur et son
 * etape de rendu.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class TpMarkRendererEvents {

    private TpMarkRendererEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        TpMarkRenderer.render(event.getCamera(), event.getPoseStack(), event.getPartialTick());
    }
}