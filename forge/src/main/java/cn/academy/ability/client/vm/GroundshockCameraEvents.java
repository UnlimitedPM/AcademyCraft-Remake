package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge du piquage de regard de {@code GroundshockCamera}.
 *
 * <p>Le piquage lui-meme est commun ; ce qui reste ici, c'est le tick du chargeur.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class GroundshockCameraEvents {

    private GroundshockCameraEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        GroundshockCamera.tick();
    }
}
