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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
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

    /**
     * Le temps entre deux relevés des surfaces, en ticks.
     *
     * <p>Une surface de liquide ne change pas d'une image a l'autre, et la relever coute cher :
     * chaque bloc interroge une vingtaine de blocs voisins, comme le rendu de l'eau de vanilla.
     */
    private static final int REFRESH_TICKS = 10;

    /** Les nappes a peindre, relues par {@link #refreshSheets}. */
    private static final List<Sheet> SHEETS = new ArrayList<>();
    private static long sheetsTick = Long.MIN_VALUE;

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
     * Le test de profondeur, entierement coupe.
     *
     * <p>PIEGE PAYE : `new RenderStateShard.DepthTestStateShard(nom, 519)` ne fait RIEN. Verifie
     * dans la classe elle-meme, dont le `setupRenderState` est vide quand la fonction vaut 519
     * (c'est ainsi que vanilla declare son `NO_DEPTH_TEST`, qui ne coupe donc rien tout seul et
     * s'appuie sur l'etat courant). Les nappes restaient testees en profondeur : le liquide, noir
     * et opaque, les cachait, alors que l'original faisait `glDisable(GL_DEPTH_TEST)` et les
     * laissait voir « au travers meme des murs », comme le joueur le decrit.
     */
    private static final class SansProfondeur extends RenderStateShard.DepthTestStateShard {

        SansProfondeur(String name) {
            super(name, 519);
        }

        @Override
        public void setupRenderState() {
            RenderSystem.disableDepthTest();
        }

        @Override
        public void clearRenderState() {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
        }
    }

    /** La profondeur, coupee pour de vrai. */
    private static final RenderStateShard.DepthTestStateShard ALWAYS =
            new SansProfondeur("academy_imag_phase_no_depth");

    /** La couleur, mais pas la profondeur : une nappe ne cache pas celle qui la suit. */
    private static final RenderStateShard.WriteMaskStateShard COLOR_ONLY =
            new RenderStateShard.WriteMaskStateShard(true, false);

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
     * Le rendu d'une nappe : la texture, sa couleur, rien d'autre.
     *
     * <p>C'est le shader le plus simple du jeu ({@code position_color_tex}) : la texture
     * multipliee par la couleur du sommet. Ni lumiere, ni brouillard, ni superposition —
     * exactement ce que faisait la fonction fixe de l'original, ou un {@code glColor4d} sur
     * une texture suffisait. Le shader d'entite translucide, lui, demandait une carte de
     * lumiere que ce rendu ne fournit pas : le jeu s'en plaignait au demarrage
     * (« could not find sampler named Sampler2 »).
     *
     * <p>Les etats de Minecraft sont inaccessibles depuis un mod, donc refaits ici :
     * melange alpha comme l'eau, aucune profondeur (c'est la difference), et couleur ecrite mais
     * pas la profondeur.
     */
    private static RenderType layer(ResourceLocation texture) {
        return RenderType.create("academy_imag_phase_layer",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getPositionColorTexShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT)
                        .setDepthTestState(ALWAYS)
                        .setWriteMaskState(COLOR_ONLY)
                        // Le sens de l'original : un lac se voit d'en haut comme d'en dessous.
                        .setCullState(new RenderStateShard.CullStateShard(false))
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

        refreshSheets(mc, chunks, camera);

        List<Sheet> visible = new ArrayList<>();
        for (Sheet sheet : SHEETS) {
            if (distanceTo(camera, sheet.pos()) <= MAX_DISTANCE) visible.add(sheet);
        }
        if (visible.isEmpty()) return;
        visible.sort(Comparator.comparingDouble((Sheet sheet) -> distanceTo(camera, sheet.pos())).reversed());

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        // Le temps de l'original : des secondes, pas des ticks.
        float time = (mc.level.getGameTime() + mc.getFrameTime()) / 20.0f;

        for (Sheet sheet : visible) {
            float alpha = ImagPhaseLayers.alpha(distanceTo(camera, sheet.pos()));
            ImagPhaseLayers.Layer[] layers = ImagPhaseLayers.layers(mean(sheet.corners()));
            BlockPos pos = sheet.pos();

            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            Matrix4f matrix = pose.last().pose();
            for (int i = 0; i < layers.length; i++) {
                drawLayer(buffers.getBuffer(LAYERS[i]), matrix, layers[i], sheet.corners(), time, alpha);
            }
            pose.popPose();
        }

        // Sans ce vidage, les nappes resteraient dans le tampon jusqu'a la prochaine image.
        for (RenderType type : LAYERS) {
            buffers.endBatch(type);
        }
    }

    /** Une surface prete : son bloc, et les quatre hauteurs de coin de sa surface. */
    private record Sheet(BlockPos pos, float[] corners) {}

    /**
     * Relit les blocs de liquide et leurs quatre hauteurs de coin, toutes les dix ticks.
     *
     * <p>Appelee avant le dessin, depuis {@link #draw} : la liste des surfaces ne depend ni de
     * l'image ni de l'angle, seulement du monde.
     */
    private static void refreshSheets(Minecraft mc, ClientChunkCache chunks, Vec3 camera) {
        long now = mc.level.getGameTime();
        // Le temps d'un monde neuf repart de zero : un `now - sheetsTick` negatif garderait la
        // liste de la session precedente pour toujours, et plus rien ne se dessinerait.
        boolean perime = sheetsTick == Long.MIN_VALUE || now < sheetsTick || now - sheetsTick >= REFRESH_TICKS;
        if (!perime) return;
        sheetsTick = now;
        SHEETS.clear();

        int radius = (int) Math.ceil(MAX_DISTANCE / 16.0d) + 1;
        int centerX = SectionPos.blockToSectionCoord(camera.x);
        int centerZ = SectionPos.blockToSectionCoord(camera.z);
        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                ChunkAccess chunk = chunks.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) continue;
                collect(mc.level, levelChunk, camera, SHEETS);
            }
        }
    }

    private static void collect(Level level, LevelChunk chunk, Vec3 camera, List<Sheet> found) {
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            if (!(entry.getValue() instanceof ImagPhaseLiquidBlockEntity)) continue;
            if (distanceTo(camera, pos) > MAX_DISTANCE) continue;
            found.add(new Sheet(pos.immutable(), cornerHeights(level, pos)));
        }
    }

    /**
     * Les quatre hauteurs de coin du bloc, dans l'ordre des sommets du quad.
     *
     * <p>Copie du calcul de vanilla : sur un liquide qui coule, la surface n'est pas plane, et
     * chaque coin vaut la moyenne ponderee des blocs qui le touchent. C'est ce qui fait que les
     * nappes suivent la pente au lieu de flotter a plat au-dessus d'elle.
     */
    private static float[] cornerHeights(Level level, BlockPos pos) {
        Fluid fluid = level.getFluidState(pos).getType();
        float self = heightOf(level, fluid, pos);
        float north = heightOf(level, fluid, pos.north());
        float south = heightOf(level, fluid, pos.south());
        float east = heightOf(level, fluid, pos.east());
        float west = heightOf(level, fluid, pos.west());
        return new float[] {
                corner(level, fluid, self, north, west, pos.north().west()),
                corner(level, fluid, self, north, east, pos.north().east()),
                corner(level, fluid, self, south, east, pos.south().east()),
                corner(level, fluid, self, south, west, pos.south().west()),
        };
    }

    /** Portage de {@code LiquidBlockRenderer.getHeight} : la hauteur d'un bloc de fluide. */
    private static float heightOf(Level level, Fluid fluid, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (!fluid.isSame(fluidState.getType())) {
            // Un bloc plein vaut -1 : il ne participe pas a la surface, alors qu'un bloc vide
            // vaut 0 et la tire vers le bas. C'est la distinction que fait vanilla.
            return state.getCollisionShape(level, pos).isEmpty() ? 0f : -1f;
        }
        // Un bloc de fluide qui a le meme fluide au-dessus est cache : sa surface est le plafond.
        return fluid.isSame(level.getBlockState(pos.above()).getFluidState().getType())
                ? 1f : fluidState.getOwnHeight();
    }

    /** Portage de {@code LiquidBlockRenderer.calculateAverageHeight} : la hauteur d'un coin. */
    private static float corner(Level level, Fluid fluid, float self, float sideA, float sideB,
                                BlockPos diagonal) {
        if (sideA >= 1f || sideB >= 1f) return 1f;
        float[] acc = new float[2];
        if (sideA > 0f || sideB > 0f) {
            float d = heightOf(level, fluid, diagonal);
            if (d >= 1f) return 1f;
            addWeightedHeight(acc, d);
        }
        addWeightedHeight(acc, self);
        addWeightedHeight(acc, sideA);
        addWeightedHeight(acc, sideB);
        return acc[1] <= 0f ? 0f : acc[0] / acc[1];
    }

    /** Portage de {@code LiquidBlockRenderer.addWeightedHeight} : une surface pleine pese plus. */
    private static void addWeightedHeight(float[] acc, float height) {
        if (height >= 0.8f) {
            acc[0] += height * 10f;
            acc[1] += 10f;
        } else if (height >= 0f) {
            acc[0] += height;
            acc[1] += 1f;
        }
    }

    private static float mean(float[] values) {
        float sum = 0f;
        for (float value : values) sum += value;
        return sum / values.length;
    }

    /** Un quad horizontal, texture dans le coin oppose a celui ou on l'a lue. */
    private static void drawLayer(VertexConsumer out, Matrix4f matrix, ImagPhaseLayers.Layer layer,
                                  float[] corners, float time, float alpha) {
        double u = layer.offsetU(time);
        double v = layer.offsetV(time);
        double d = layer.density();

        vertex(out, matrix, 0.0d, layer.heightAt(corners[0]), 0.0d, u, v, alpha);
        vertex(out, matrix, 1.0d, layer.heightAt(corners[1]), 0.0d, u + d, v, alpha);
        vertex(out, matrix, 1.0d, layer.heightAt(corners[2]), 1.0d, u + d, v + d, alpha);
        vertex(out, matrix, 0.0d, layer.heightAt(corners[3]), 1.0d, u, v + d, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, double x, double y, double z,
                               double u, double v, float alpha) {
        out.vertex(matrix, (float) x, (float) y, (float) z)
                .color(1f, 1f, 1f, alpha)
                .uv((float) u, (float) v)
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
