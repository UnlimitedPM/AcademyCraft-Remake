package cn.academy.client;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge du rendu des eclats de {@code SilbarnFrags}.
 *
 * <p>Le dessin est commun — voir {@link SilbarnFrags#render} — ; ce qui reste ici, c'est l'etape de
 * rendu du chargeur.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class SilbarnFragsEvents {

    private SilbarnFragsEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        SilbarnFrags.render(event.getCamera(), event.getPoseStack());
    }
}
