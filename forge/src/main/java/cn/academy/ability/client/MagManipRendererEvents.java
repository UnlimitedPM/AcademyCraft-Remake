package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge du rendu de {@code MagManipRenderer}.
 *
 * <p>Le dessin lui-meme est commun ; ce qui reste ici, c'est l'evenement du chargeur et son
 * etape de rendu.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class MagManipRendererEvents {

    private MagManipRendererEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        MagManipRenderer.render(event.getCamera(), event.getPoseStack(), event.getPartialTick());
    }
}