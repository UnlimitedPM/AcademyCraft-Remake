package cn.academy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class CatEngineRenderer implements BlockEntityRenderer<CatEngineBlockEntity> {
    // CORRECTION : Chemin exact de ta texture
    private static final ResourceLocation CAT_TEXTURE = new ResourceLocation("academy", "textures/block/cat_engine.png");

    public CatEngineRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CatEngineBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        float cameraYaw = Minecraft.getInstance().getEntityRenderDispatcher().camera.getYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));

        // ON SUPPRIME LE "entity.rotation += 8.0f" D'ICI !
        // On applique juste la rotation stockée dans l'entité
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.rotation));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(CAT_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        float s = 0.4f;
        drawVertex(vertexConsumer, matrix, -s, -s, 0, 0, 1, combinedLight);
        drawVertex(vertexConsumer, matrix,  s, -s, 0, 1, 1, combinedLight);
        drawVertex(vertexConsumer, matrix,  s,  s, 0, 1, 0, combinedLight);
        drawVertex(vertexConsumer, matrix, -s,  s, 0, 0, 0, combinedLight);

        poseStack.popPose();
    }

    private void drawVertex(VertexConsumer builder, Matrix4f matrix, float x, float y, float z, float u, float v, int light) {
        builder.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }
}