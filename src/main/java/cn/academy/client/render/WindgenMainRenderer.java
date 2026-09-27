package cn.academy.client.render;

import cn.academy.WindgenMainBlock;
import cn.academy.WindgenMainBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * L'eolienne : sa nacelle, et ses pales qui tournent.
 *
 * <p>Portage de {@code RenderWindGenMain}. Le corps et les pales sont deux fichiers OBJ,
 * dessines au meme endroit : le moyeu de l'helice vient se poser sur le nez du corps
 * (z 0,82 et mi-hauteur, comme dans l'original), et c'est autour de cet axe qu'elle
 * tourne.
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

    private static final ResourceLocation BODY_MODEL = ObjModels.model("windgen_main");
    private static final ResourceLocation BODY_TEXTURE = ObjModels.texture("windgen_main_model");
    private static final ResourceLocation FAN_MODEL = ObjModels.model("windgen_fan");
    private static final ResourceLocation FAN_TEXTURE = ObjModels.texture("windgen_fan_model");

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
        // L'origine des modeles du mod : le centre du bloc, pose sur son bas. C'est la
        // convention de leurs fichiers OBJ, et le tout tourne autour du centre du bloc.
        pose.translate(0.5d, 0.0d, 0.5d);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(WindgenMainBlock.FACING).toYRot()));

        VertexConsumer body = buffers.getBuffer(ObjModels.type(BODY_TEXTURE));
        ObjModels.draw(ObjModels.get(BODY_MODEL).all(), pose, body, light, overlay);

        if (rotor.isFanInstalled() && rotor.isNoObstacle()) {
            double seconds = rotor.getLevel() == null
                    ? 0.0d
                    : (rotor.getLevel().getGameTime() + partialTick) / 20.0d;
            float spin = rotor.advanceFan(seconds);

            pose.pushPose();
            pose.translate(0.0d, HUB_Y, HUB_Z);
            // L'axe de l'helice : elle tourne dans le plan du nez, comme l'original.
            pose.mulPose(Axis.ZP.rotationDegrees(-spin));
            VertexConsumer fan = buffers.getBuffer(ObjModels.type(FAN_TEXTURE));
            ObjModels.draw(ObjModels.get(FAN_MODEL).all(), pose, fan, light, overlay);
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
     * {@code toYRot}, qui est l'angle de cette direction.
     */
    private static float degreesFor(Direction facing) {
        return -facing.toYRot();
    }
}
