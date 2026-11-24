package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge du geste de main de {@code HandSwing}.
 *
 * <p>Le geste lui-meme est commun ; ce qui reste ici, c'est le tick du chargeur et l'evenement
 * de rendu de la main.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class HandSwingEvents {

    private HandSwingEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        HandSwing.tick();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        HandSwing.renderHand(event.getHand(), event.getPoseStack(), event.getPartialTick());
    }
}
