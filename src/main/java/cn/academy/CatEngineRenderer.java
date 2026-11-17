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

public class CatEngineRenderer implements BlockEntityRenderer<CatEngineBlockEntity> {
    // CORRECTION : Chemin exact de ta texture
    private static final ResourceLocation CAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/block/cat_engine.png");

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
        PoseStack.Pose frame = poseStack.last();

        // La normale du chat reste VERTICALE dans le monde, quelle que soit la direction du
        // regard et quel que soit l'angle du panneau. Le port la laissait en (0, 1, 0) dans le
        // repere de la pose, et le shader la faisait tourner avec le lacet de la camera : le chat
        // s'assombrissait donc rien qu'en regardant vers le sud ou l'est. Comme le panneau tourne
        // de `entity.rotation` autour de X, on lui donne la normale qui, une fois cette rotation
        // appliquee, redevient (0, 1, 0).
        double radians = Math.toRadians(entity.rotation);
        float nx = 0f;
        float ny = (float) Math.cos(radians);
        float nz = (float) -Math.sin(radians);

        float s = 0.4f;
        drawVertex(vertexConsumer, frame, -s, -s, 0, 0, 1, combinedLight, nx, ny, nz);
        drawVertex(vertexConsumer, frame,  s, -s, 0, 1, 1, combinedLight, nx, ny, nz);
        drawVertex(vertexConsumer, frame,  s,  s, 0, 1, 0, combinedLight, nx, ny, nz);
        drawVertex(vertexConsumer, frame, -s,  s, 0, 0, 0, combinedLight, nx, ny, nz);

        poseStack.popPose();
    }

    private void drawVertex(VertexConsumer builder, PoseStack.Pose frame, float x, float y, float z,
                            float u, float v, int light, float nx, float ny, float nz) {
        builder.vertex(frame.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(frame.normal(), nx, ny, nz)
                .endVertex();
    }
}