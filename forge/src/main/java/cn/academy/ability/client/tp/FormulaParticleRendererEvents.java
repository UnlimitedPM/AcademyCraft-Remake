package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge du rendu de {@code FormulaParticleRenderer}.
 *
 * <p>Le dessin lui-meme est commun ; ce qui reste ici, c'est l'evenement du chargeur et son
 * etape de rendu.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class FormulaParticleRendererEvents {

    private FormulaParticleRendererEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        FormulaParticleRenderer.render(event.getCamera(), event.getPoseStack(), event.getPartialTick());
    }
}