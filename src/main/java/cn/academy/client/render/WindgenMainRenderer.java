package cn.academy.client.render;

import cn.academy.WindgenMainBlock;
import cn.academy.WindgenMainBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * L'eolienne : ses pales, qui tournent.
 *
 * <p>Portage de {@code RenderWindGenMain}, mais le partage est different : le CORPS de la
 * nacelle reste un modele de bloc ({@code models/block/windgen_main.json}), comme la base
 * et le pilier, et c'est Forge qui le dessine — c'est l'aspect que le joueur connait. Un
 * modele de bloc ne peut pas tourner, donc seules les PALES passent par ici : dessinees sur
 * le moyeu, elles tournent autour de l'axe du nez (z 0,82 et mi-hauteur, comme dans
 * l'original).
 *
 * <p>Ce rendu reprend donc exactement le repere du modele de bloc (origine au centre du
 * bas du bloc, rotation de la face) : les pales tombent sur le nez du corps dessine par
 * Forge, et le blockstate porte les memes rotations.
 *
 * <p>Deux choses viennent du block entity, et non d'ici : si une helice est installee, et
 * si la colonne est assez haute. La premiere se lit dans son inventaire, la seconde est
 * calculee par le serveur — voir {@link WindgenMainBlockEntity}.
 *
 * <p>Comme l'original, les pales ne se dessinent que si la zone qu'elles balaient est
 * degagee : une eolienne dont les pales traversent un mur ne les dessine pas. Et elles ne
 * tournent que sur une colonne complete.
 */
public class WindgenMainRenderer implements BlockEntityRenderer<WindgenMainBlockEntity> {

    private static final ResourceLocation FAN_MODEL = ObjModels.model("windgen_fan");
    private static final ResourceLocation FAN_TEXTURE = ObjModels.texture("windgen_fan_model");

    /** Le seul groupe du fichier des pales (releve dans {@code windgen_fan.obj}). */
    private static final String FAN_GROUP = "initialShadingGroup";

    /** Le nez du corps, dans son espace : c'est la que le moyeu de l'helice se pose. */
    private static final double HUB_Y = 0.5d;
    private static final double HUB_Z = 0.82d;

    public WindgenMainRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(WindgenMainBlockEntity rotor, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = rotor.getBlockState();
        if (!(state.getBlock() instanceof WindgenMainBlock)) return;

        pose.pushPose();
        // L'origine du modele de bloc (et des fichiers OBJ) : le centre du bas du bloc,
        // tourne comme la face du bloc. C'est le repere de windgen_main.json, et donc
        // aussi celui des pales.
        pose.translate(0.5d, 0.0d, 0.5d);
        pose.mulPose(Axis.YP.rotationDegrees(degreesFor(state.getValue(WindgenMainBlock.FACING))));

        if (rotor.isFanInstalled() && rotor.isNoObstacle()) {
            double seconds = rotor.getLevel() == null
                    ? 0.0d
                    : (rotor.getLevel().getGameTime() + partialTick) / 20.0d;
            float spin = rotor.advanceFan(seconds);

            pose.pushPose();
            pose.translate(0.0d, HUB_Y, HUB_Z);
            // L'axe de l'helice : elle tourne dans le plan du nez, comme l'original.
            pose.mulPose(Axis.ZP.rotationDegrees(-spin));
            ObjModels.draw(ObjModels.get(FAN_MODEL), FAN_GROUP, FAN_TEXTURE, pose, buffers, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * De combien le modele tourne pour cette orientation.
     *
     * <p>Le nez du modele regarde vers +z, et il doit regarder du cote de la partie avant du
     * rotor — celle que l'eolienne surveille pour ses pales. C'est donc la rotation qui
     * amene +z sur la direction du bloc, comme pour n'importe quelle entite : l'oppose de
     * {@code toYRot}, qui est l'angle de cette direction. Le blockstate de
     * {@code windgen_main} porte les memes orientations — mais une rotation de blockstate
     * tourne dans l'autre sens que {@code Axis.YP} (regle du four vanilla, dont le modele
     * regarde le nord a {@code y: 0}) : nord 180, sud 0, ouest 90, est 270.
     */
    private static float degreesFor(Direction facing) {
        return -facing.toYRot();
    }
}
