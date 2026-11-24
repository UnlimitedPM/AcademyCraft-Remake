package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.MineDetectOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Le crochet Forge de la detection de minerais.
 *
 * <p>{@code MineDetectOverlay} est commun : il balaie la sphere et tient le son, mais c'est le
 * chargeur qui sait quand un tick de client tombe. Cette classe ne fait que le lui dire.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class MineDetectEvents {

    private MineDetectEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MineDetectOverlay.tick();
    }
}
