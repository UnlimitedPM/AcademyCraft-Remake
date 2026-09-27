package cn.academy.client.render;

import cn.academy.AcademyCraft;
import cn.academy.ImagPhaseLiquidBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Les nappes de l'imag phase liquide.
 *
 * <p>Portage de {@code RenderImagPhaseLiquid} : trois quads translucides poses a des
 * hauteurs differentes, qui defilent chacun a sa vitesse, avec la meme texture pour les trois
 * (une par couche dans l'original) et une opacite qui decroit avec la distance.
 *
 * <p>La difference assumee : l'original dessinait avec le test de profondeur desactive, donc
 * a travers le monde. Ici les nappes respectent la profondeur mais n'ecrivent pas dedans, ce
 * qui evite qu'un lac se voie a travers la colline d'a cote. Le reste — hauteurs, vitesses,
 * densites, opacite — est repris tel quel.
 */
public class ImagPhaseLiquidRenderer implements BlockEntityRenderer<ImagPhaseLiquidBlockEntity> {

    private static final ResourceLocation[] LAYERS = {
            texture("0"), texture("1"), texture("2"),
    };

    public ImagPhaseLiquidRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRender(ImagPhaseLiquidBlockEntity entity, Vec3 cameraPos) {
        return shouldDraw(cameraPos, entity.getBlockPos());
    }

    @Override
    public void render(ImagPhaseLiquidBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Level level = entity.getLevel();
        if (level == null) return;

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float alpha = alphaAt(camera, entity.getBlockPos());
        if (alpha < ImagPhaseLayers.MIN_ALPHA) return;

        // La hauteur du fluide : celle d'une source pleine, moins pour un fluide qui s'ecoule.
        double fluidHeight = level.getFluidState(entity.getBlockPos()).getOwnHeight();
        ImagPhaseLayers.Layer[] layers = ImagPhaseLayers.layers(fluidHeight);
        float time = (level.getGameTime() + partialTick) / 20.0f;

        pose.pushPose();
        Matrix4f matrix = pose.last().pose();
        for (int i = 0; i < layers.length; i++) {
            VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucentEmissive(LAYERS[i], false));
            drawLayer(out, matrix, layers[i], time, alpha);
        }
        pose.popPose();
    }

    /** Un quad horizontal, texture dans le coin oppose a celui ou on l'a lue. */
    private static void drawLayer(VertexConsumer out, Matrix4f matrix, ImagPhaseLayers.Layer layer,
                                  float time, float alpha) {
        double height = layer.height();
        double u = layer.offsetU(time);
        double v = layer.offsetV(time);
        double d = layer.density();

        vertex(out, matrix, 0.0d, height, 0.0d, u, v, alpha);
        vertex(out, matrix, 1.0d, height, 0.0d, u + d, v, alpha);
        vertex(out, matrix, 1.0d, height, 1.0d, u + d, v + d, alpha);
        vertex(out, matrix, 0.0d, height, 1.0d, u, v + d, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, double x, double y, double z,
                               double u, double v, float alpha) {
        out.vertex(matrix, (float) x, (float) y, (float) z)
                .color(1f, 1f, 1f, alpha)
                .uv((float) u, (float) v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0f, 1f, 0f)
                .endVertex();
    }

    /** L'opacite de ce bloc vu de la camera, ou zero s'il est hors de portee. */
    private static float alphaAt(Vec3 camera, BlockPos pos) {
        double dx = camera.x - (pos.getX() + 0.5d);
        double dy = camera.y - (pos.getY() + 0.5d);
        double dz = camera.z - (pos.getZ() + 0.5d);
        return ImagPhaseLayers.alpha(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private static boolean shouldDraw(Vec3 camera, BlockPos pos) {
        return alphaAt(camera, pos) >= ImagPhaseLayers.MIN_ALPHA;
    }

    private static ResourceLocation texture(String frame) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/imag_proj_liquid/" + frame + ".png");
    }
}
