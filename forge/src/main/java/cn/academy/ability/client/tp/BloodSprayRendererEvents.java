package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge du rendu des eclaboussures de {@code BloodSprayRenderer}.
 *
 * <p>Le dessin et le vieillissement des taches sont communs ; ce qui reste ici, c'est l'etape de
 * rendu et le tick du chargeur.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class BloodSprayRendererEvents {

    private BloodSprayRendererEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        BloodSprayRenderer.render(event.getCamera(), event.getPoseStack());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        BloodSprayRenderer.tick();
    }
}
