package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.DeveloperBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ce qui se dessine autour des blocs, et qui n'a pas de raison d'y etre.
 *
 * <p>Vanilla entoure d'un lisere noir le bloc qu'on regarde. Les machines developeur n'en veulent
 * pas : leur modele est une machine entiere posee sur une seule brique, le lisere ne montre donc que
 * la brique et non la machine, et il coupait le modele en deux. Le joueur l'a demande ainsi :
 * « j'aimerais que lorsqu'on regarde une de ces 2 machines, il n'y ait pas l'affichage du carre de
 * minecraft ».
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderEvents {

    private ClientRenderEvents() {}

    /**
     * Pas de lisere sur les machines developeur -- ni sur leurs sept parties invisibles, qui sont du
     * meme bloc et qu'on peut viser sans rien voir.
     */
    @SubscribeEvent
    public static void pasDeLisereSurLesMachines(RenderHighlightEvent.Block event) {
        BlockPos pos = event.getTarget().getBlockPos();
        Entity viewer = event.getCamera().getEntity();
        if (viewer.level().getBlockState(pos).getBlock() instanceof DeveloperBlock) {
            event.setCanceled(true);
        }
    }
}