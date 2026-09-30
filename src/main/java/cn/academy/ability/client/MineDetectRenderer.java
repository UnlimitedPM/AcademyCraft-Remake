package cn.academy.ability.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import cn.academy.AcademyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

/**
 * Allume les minerais vus par la detection, portage du rendu de {@code HandlerRender}.
 *
 * <p>L'original avait une entite cliente, un maillage de boite texturee et un jeu de
 * transformations OpenGL. Le port n'a ni entite ni maillage : il dessine six faces par minerai,
 * et coupe le test de profondeur — c'est ce qui fait voir <b>a travers la pierre</b>, et c'est
 * le seul interet de la competence.
 *
 * <p>C'est la texture qui fait tout le dessin, et elle vient de l'original : son
 * {@code createBoxWithUV} collait l'image entiere sur chaque face de sa boite, avec un materiau
 * qui <b>ignorait la lumiere</b> — d'ou ce cadre clair et ce corps brumeux, la meme image vue
 * six fois. {@code mineview.png} est cette image, et le type de rendu de la balise est ce
 * materiau : il multiplie la texture par la couleur du sommet sans jamais eclairer.
 *
 * <p>Un premier essai dessinait des cubes de debogage pleins, sans texture : le minerai se
 * voyait, mais comme un bloc de couleur, sans le cadre qui le fait lire comme une chose
 * <b>revelee</b> plutot que posee la.
 *
 * <p>La couleur vient du palier de pioche, la transparence de la distance : voir
 * {@link MineDetectVisuals}. Une centaine de cubes au maximum se dessinent par image, ce qui
 * est le prix de l'x-ray et le seul endroit du port qui se le permette.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class MineDetectRenderer {

    /** L'image de l'original : le cadre clair et le corps brumeux d'un minerai allume. */
    private static final ResourceLocation MINEVIEW = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/mineview.png");

    /** La boite d'un minerai : de cinq centimetres a quatre-vingt-quinze. */
    private static final float LOW = 0.05f;
    private static final float HIGH = 0.95f;

    private MineDetectRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !MineDetectOverlay.active(player)) return;

        Vec3 camera = event.getCamera().getPosition();
        Vec3 from = player.position();
        float range = (float) MineDetectOverlay.range();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(
                cn.academy.ability.client.arc.ArcRenderer.arc(MINEVIEW));
        Matrix4f matrix = pose.last().pose();

        // Le rendu du monde est deja place a l'origine du monde : c'est a nous de retirer la
        // camera, comme le fait le bouclier.
        RenderSystem.disableDepthTest();
        for (MineDetectOverlay.Ore ore : MineDetectOverlay.ores()) {
            Vec3 centre = Vec3.atCenterOf(ore.pos());
            float alpha = MineDetectVisuals.alpha(from.distanceTo(centre), range);
            int[] rgb = MineDetectVisuals.colorFor(ore.tier());
            cube(consumer, matrix, ore.pos(), camera, rgb, alpha);
        }
        buffers.endBatch();
        RenderSystem.enableDepthTest();
    }

    /** Les six faces d'un cube, du coin bas au coin haut. */
    private static void cube(VertexConsumer consumer, Matrix4f matrix, BlockPos pos, Vec3 camera,
                             int[] rgb, float alpha) {
        float x0 = (float) (pos.getX() + LOW - camera.x);
        float y0 = (float) (pos.getY() + LOW - camera.y);
        float z0 = (float) (pos.getZ() + LOW - camera.z);
        float x1 = (float) (pos.getX() + HIGH - camera.x);
        float y1 = (float) (pos.getY() + HIGH - camera.y);
        float z1 = (float) (pos.getZ() + HIGH - camera.z);

        // Dessous et dessus.
        face(consumer, matrix, rgb, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(consumer, matrix, rgb, alpha, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        // Et les quatre cotes.
        face(consumer, matrix, rgb, alpha, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        face(consumer, matrix, rgb, alpha, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        face(consumer, matrix, rgb, alpha, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        face(consumer, matrix, rgb, alpha, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    /** Un quadrilatere, dans l'ordre de ses quatre coins, avec l'image entiere dessus. */
    private static void face(VertexConsumer consumer, Matrix4f matrix, int[] rgb, float alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(consumer, matrix, rgb, alpha, ax, ay, az, 0f, 0f);
        vertex(consumer, matrix, rgb, alpha, bx, by, bz, 1f, 0f);
        vertex(consumer, matrix, rgb, alpha, cx, cy, cz, 1f, 1f);
        vertex(consumer, matrix, rgb, alpha, dx, dy, dz, 0f, 1f);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, int[] rgb, float alpha,
                               float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, alpha)
                .uv(u, v)
                .endVertex();
    }
}
