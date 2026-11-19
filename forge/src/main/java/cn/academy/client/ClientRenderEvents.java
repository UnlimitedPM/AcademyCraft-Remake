package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.DeveloperBlock;
import cn.academy.ModFluids;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
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
     *
     * <p>Le stade {@code AFTER_WEATHER} a ete essaye, et il ne va pas : la pose n'y est plus celle
     * du monde, les nappes se dessinaient « en fonction de la position de la camera » au lieu de
     * rester dans le liquide. Forge appelle le stade generique a la fin de chaque
     * {@code renderChunkLayer}, donc ici la couche translucide est deja peinte et la pose est
     * encore celle qui translate de {@code -camera}.
     */
    @SubscribeEvent
    public static void lesNappesDuLiquideApresLEau(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        cn.academy.client.render.ImagPhaseLiquidRenderer.draw(event.getPoseStack(),
                event.getCamera().getPosition(), event.getFrustum());
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
     * <p>Les distances sont celles de l'eau de vanilla, relevees dans {@code FogRenderer}, mais
     * beaucoup plus courtes : la 1.20.1 laisse voir a 96 blocs sous l'eau, la 1.12.2 a une
     * quinzaine. Le joueur compare au vrai mod — « quand on est dans le liquide, on a pas assez le
     * masque bleu a l'ecran » — et cette distance EST ce masque : tout ce qui est au-dela prend la
     * couleur du brouillard. Forge n'applique un changement que si l'evenement est annule.
     *
     * <p>C'est ce chiffre qu'il faut bouger si le voile est trop dense ou trop leger.
     *
     * <p>ET LA MEME CONDITION QUE LA COULEUR, sinon mur blanc : Forge ne change la couleur du
     * brouillard que si la camera est SOUS la surface du fluide
     * ({@code ForgeHooksClient.getFogColor}), et le joueur a vu ce que donne l'ecart — « quand on
     * est pile poil entre le liquide et l'air exterieur, on a une couleur blanche a l'ecran comme
     * si il y avait du brouillard ». Les distances courtes s'appliquaient des que l'oeil touchait
     * le fluide, donc une tete a la surface recevait le brouillard du liquide avec la couleur du
     * CIEL. On teste donc la surface nous-memes, et pas `isEyeInFluidType`.
     */
    @SubscribeEvent
    public static void leBrouillardDansLeLiquide(ViewportEvent.RenderFog event) {
        Entity viewer = event.getCamera().getEntity();
        if (viewer == null || viewer.level() == null) return;

        BlockPos pos = event.getCamera().getBlockPosition();
        FluidState state = viewer.level().getFluidState(pos);
        if (state.getFluidType() != ModFluids.PHASE_LIQUID_TYPE.get()) return;
        if (event.getCamera().getPosition().y >= pos.getY() + state.getHeight(viewer.level(), pos)) return;

        event.setNearPlaneDistance(-4.0f);
        event.setFarPlaneDistance(12.0f * Math.max(0.25f, visionSousLEau(viewer)));
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }

    /** La vision sous l'eau du joueur, 1.0 par defaut (moins avec Aqua Affinity, comme vanilla). */
    private static float visionSousLEau(Entity viewer) {
        return viewer instanceof LocalPlayer player ? player.getWaterVision() : 1.0f;
    }
}