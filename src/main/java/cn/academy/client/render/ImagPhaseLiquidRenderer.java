package cn.academy.client.render;

import cn.academy.AcademyCraft;
import cn.academy.ImagPhaseLiquidBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
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
 * <p>Comme l'original, les nappes se dessinent sans test de profondeur : elles passent a
 * travers le bloc de fluide, qui est noir et opaque — sans cela, les deux nappes du haut
 * seraient enfermees dans le liquide et on ne verrait que celle du dessous. C'est aussi ce
 * qui fait qu'un lac se voit de loin, avant meme qu'on en approche.
 */
public class ImagPhaseLiquidRenderer implements BlockEntityRenderer<ImagPhaseLiquidBlockEntity> {

    /** Le melange alpha de l'eau : c'est l'alpha des sommets qui decide. */
    private static final RenderStateShard.TransparencyStateShard TRANSLUCENT =
            new RenderStateShard.TransparencyStateShard("academy_imag_phase_translucent",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                    },
                    () -> {
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableBlend();
                    });

    /**
     * La profondeur est toujours acceptee ({@code GL_ALWAYS}) : c'est ce que faisait
     * l'original, et sans cela les nappes restent enfermees dans le bloc de fluide, qui
     * est noir et opaque.
     */
    private static final RenderStateShard.DepthTestStateShard ALWAYS =
            new RenderStateShard.DepthTestStateShard("academy_imag_phase_always", 519);

    /** La couleur, mais pas la profondeur : une nappe ne cache pas celle qui la suit. */
    private static final RenderStateShard.WriteMaskStateShard COLOR_ONLY =
            new RenderStateShard.WriteMaskStateShard(true, false);

    private static final RenderStateShard.OverlayStateShard NO_OVERLAY =
            new RenderStateShard.OverlayStateShard(false);

    private static final RenderStateShard.LightmapStateShard NO_LIGHTMAP =
            new RenderStateShard.LightmapStateShard(false);

    /**
     * Une nappe : la texture de sa couche, et le rendu qui va avec.
     *
     * <p>A declarer <b>apres</b> les etats ci-dessus : une classe s'initialise de haut en
     * bas, et un etat encore nul fait tomber le jeu au demarrage (le constructeur de
     * {@code CompositeState} refuse le vide).
     */
    private static final RenderType[] LAYERS = {
            layer(texture("0")), layer(texture("1")), layer(texture("2")),
    };

    /**
     * Le rendu d'une nappe : lumineux, transparent, sans cull, et surtout sans test de
     * profondeur.
     *
     * <p>Les etats de Minecraft sont inaccessibles depuis un mod, donc refaits ici :
     * melange alpha comme l'eau, profondeur toujours acceptee (c'est la difference),
     * couleur ecrite mais pas la profondeur, et aucune lumiere — les nappes brillent.
     */
    private static RenderType layer(ResourceLocation texture) {
        return RenderType.create("academy_imag_phase_layer",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT)
                        .setDepthTestState(ALWAYS)
                        .setWriteMaskState(COLOR_ONLY)
                        .setOverlayState(NO_OVERLAY)
                        .setLightmapState(NO_LIGHTMAP)
                        .createCompositeState(false));
    }

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
            VertexConsumer out = buffers.getBuffer(LAYERS[i]);
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
