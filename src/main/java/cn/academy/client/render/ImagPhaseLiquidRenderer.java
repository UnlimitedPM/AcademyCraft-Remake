package cn.academy.client.render;

import cn.academy.AcademyCraft;
import cn.academy.ImagPhaseLiquidBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Les nappes de l'imag phase liquide.
 *
 * <p>Portage de {@code RenderImagPhaseLiquid} : trois quads translucides poses a des
 * hauteurs differentes, qui defilent chacun a sa vitesse, avec la meme texture pour les trois
 * (une par couche dans l'original) et une opacite qui decroit avec la distance.
 *
 * <h2>Pourquoi pas un rendu de block entity</h2>
 *
 * <p>C'est ce que le port faisait, et le joueur n'en voyait que la nappe du bas : « on ne voit que
 * les particules du bas et pas les autres, elles sont cachees par le liquide ». La raison est dans
 * l'ordre des couches du monde : les rendus de block entity passent <b>avant</b> la couche
 * translucide ({@code LevelRenderer.renderLevel}, entre le terrain et
 * {@code RenderType.translucent()}), et un bloc de fluide se dessine justement dans cette
 * couche-la. La nappe du bas echappait au probleme parce qu'elle est <b>sous</b> le bloc de fluide,
 * donc dans l'air ; celles des hauteurs 0,35 et 0,7 sont prisonnieres du bloc et peintes par-dessus.
 *
 * <p>L'original dessinait dans son passage de rendu de block entities, qui venait apres tout le
 * reste, donc apres le fluide. On reprend sa place : {@code RenderLevelStageEvent} au stade
 * {@code AFTER_TRANSLUCENT_BLOCKS}, ou le liquide est deja peint.
 *
 * <h2>Pourquoi sans test de profondeur</h2>
 *
 * <p>Comme l'original : les nappes passent a travers le bloc de fluide, qui est noir et opaque, et
 * a travers le terrain — c'est ce qui fait qu'un lac se voit de loin, avant meme qu'on en approche.
 * Le tri du plus loin au plus pres remplace la profondeur : sans lui, deux nappes melangees
 * s'afficheraient dans l'ordre du hasard et le lac scintillerait.
 */
public final class ImagPhaseLiquidRenderer {

    /** Au-dela de cette distance l'opacite passe sous le seuil de l'original : rien a peindre. */
    private static final double MAX_DISTANCE = 45.0d;

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

    private ImagPhaseLiquidRenderer() {}

    /**
     * Dessine les nappes de tous les blocs de liquide assez proches, du plus loin au plus pres.
     *
     * <p>Appelee apres la couche translucide du monde, quand le fluide est deja peint — voir la
     * classe. Les blocs sont relus dans les chunks deja charges : un rendu de block entity ne
     * pourrait pas le faire a ce moment-la, il n'est deja plus appele.
     */
    public static void draw(PoseStack pose, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ClientChunkCache chunks = (ClientChunkCache) mc.level.getChunkSource();

        List<BlockPos> visible = blocksAround(chunks, camera);
        if (visible.isEmpty()) return;

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        // Le temps de l'original : des secondes, pas des ticks.
        float time = (mc.level.getGameTime() + mc.getFrameTime()) / 20.0f;

        for (BlockPos pos : visible) {
            FluidState fluid = mc.level.getFluidState(pos);
            ImagPhaseLayers.Layer[] layers = ImagPhaseLayers.layers(fluid.getOwnHeight());
            float alpha = ImagPhaseLayers.alpha(distanceTo(camera, pos));

            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            Matrix4f matrix = pose.last().pose();
            for (int i = 0; i < layers.length; i++) {
                drawLayer(buffers.getBuffer(LAYERS[i]), matrix, layers[i], time, alpha);
            }
            pose.popPose();
        }

        // Sans ce vidage, les nappes resteraient dans le tampon jusqu'a la prochaine image.
        for (RenderType type : LAYERS) {
            buffers.endBatch(type);
        }
    }

    /**
     * Les blocs de liquide visibles, tries du plus loin au plus pres.
     *
     * <p>La portee vient de {@link ImagPhaseLayers#alpha} : au-dela de {@value #MAX_DISTANCE} blocs
     * l'opacite passe sous le seuil de l'original, et il n'y a plus rien a peindre.
     */
    private static List<BlockPos> blocksAround(ClientChunkCache chunks, Vec3 camera) {
        List<BlockPos> found = new ArrayList<>();
        int radius = (int) Math.ceil(MAX_DISTANCE / 16.0d) + 1;
        int centerX = SectionPos.blockToSectionCoord(camera.x);
        int centerZ = SectionPos.blockToSectionCoord(camera.z);

        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                ChunkAccess chunk = chunks.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) continue;
                collect(levelChunk, camera, found);
            }
        }

        found.sort(Comparator.comparingDouble((BlockPos pos) -> distanceTo(camera, pos)).reversed());
        return found;
    }

    private static void collect(LevelChunk chunk, Vec3 camera, List<BlockPos> found) {
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            if (!(entry.getValue() instanceof ImagPhaseLiquidBlockEntity)) continue;
            if (distanceTo(camera, entry.getKey()) > MAX_DISTANCE) continue;
            found.add(entry.getKey().immutable());
        }
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

    private static double distanceTo(Vec3 camera, BlockPos pos) {
        double dx = camera.x - (pos.getX() + 0.5d);
        double dy = camera.y - (pos.getY() + 0.5d);
        double dz = camera.z - (pos.getZ() + 0.5d);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static ResourceLocation texture(String frame) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/imag_proj_liquid/" + frame + ".png");
    }
}
