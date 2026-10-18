package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.DeveloperBlock;
import cn.academy.ModFluids;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
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
     * Les nappes de l'imag phase liquide, une fois la couche translucide du monde peinte.
     *
     * <p>C'est la place que l'original leur donnait, et la seule ou elles se voient : un bloc de
     * fluide se dessine dans la couche translucide, donc apres les rendus de block entity — un
     * rendu de block entity se ferait peindre par-dessus. Le joueur l'a vu : « on ne voit que les
     * particules du bas et pas les autres, elles sont cachees par le liquide ».
     */
    @SubscribeEvent
    public static void lesNappesDuLiquideApresLEau(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        cn.academy.client.render.ImagPhaseLiquidRenderer.draw(event.getPoseStack(),
                event.getCamera().getPosition());
    }

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

    /**
     * Le brouillard de l'eau, quand la tete est dans l'imag phase liquide.
     *
     * <p>L'original n'avait rien a faire pour l'avoir : son bloc etait en {@code Material.WATER},
     * et Minecraft appliquait le brouillard et le voile de l'eau. Le port n'en avait aucun — le
     * joueur voyait a travers : « a l'interieur du liquide, on voit tout en transparent alors que
     * dans le vrai mod on a le filtre a l'ecran comme quand on est dans de l'eau ».
     *
     * <p>Les distances sont celles de l'eau de vanilla, relevees dans {@code FogRenderer} :
     * de -8 a 96 blocs, et d'autant plus courtes que la vision sous l'eau est mauvaise. Forge
     * n'applique un changement que si l'evenement est annule.
     */
    @SubscribeEvent
    public static void leBrouillardDansLeLiquide(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!player.isEyeInFluidType(ModFluids.PHASE_LIQUID_TYPE.get())) return;
        event.setNearPlaneDistance(-8.0f);
        event.setFarPlaneDistance(96.0f * Math.max(0.25f, player.getWaterVision()));
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }
}