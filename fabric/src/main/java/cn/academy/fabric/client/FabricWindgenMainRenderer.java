package cn.academy.fabric.client;

import cn.academy.WindgenMainBlockEntity;
import cn.academy.client.render.ObjModels;
import cn.academy.client.render.WindgenMainRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * L'eolienne au complet, cote Fabric : le corps ET les pales.
 *
 * <p>Chez Forge les deux se partagent le travail : le <b>corps</b> est un modele de bloc que son
 * chargeur de modele pose, et les <b>pales</b> sont dessinees par {@link WindgenMainRenderer}, qui
 * les fait tourner. Sous Fabric il n'y a pas de chargeur de modele pour le corps, donc les deux
 * passent par le meme rendu de block entity — un type de block entity n'en accepte qu'un.
 *
 * <p>L'ordre compte : le corps d'abord, les pales par-dessus. Et le repere est le meme pour les
 * deux — l'origine au centre du bas du bloc, tournee comme la face — parce que le rendu des pales a
 * ete ecrit pour tomber sur le nez du corps.
 */
public final class FabricWindgenMainRenderer implements BlockEntityRenderer<WindgenMainBlockEntity> {

    private final FabricObjRenderer body;
    private final WindgenMainRenderer blades;

    public static BlockEntityRendererProvider<WindgenMainBlockEntity> provider() {
        return context -> new FabricWindgenMainRenderer(
                new FabricObjRenderer(new FabricObjRenderer.Placement(
                        ObjModels.model("windgen_main"),
                        state -> ObjModels.texture("windgen_main_model"),
                        0.5d, 0.0d, 0.5d, 1.0f,
                        HorizontalDirectionalBlock.FACING, true)),
                new WindgenMainRenderer(context));
    }

    private FabricWindgenMainRenderer(FabricObjRenderer body, WindgenMainRenderer blades) {
        this.body = body;
        this.blades = blades;
    }

    @Override
    public void render(WindgenMainBlockEntity rotor, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        body.render(rotor, partialTick, pose, buffers, light, overlay);
        blades.render(rotor, partialTick, pose, buffers, light, overlay);
    }
}
