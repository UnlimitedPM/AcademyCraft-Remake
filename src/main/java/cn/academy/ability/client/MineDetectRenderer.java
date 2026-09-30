package cn.academy.ability.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import cn.academy.AcademyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
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
 * six fois. {@code mineview.png} est cette image, et le programme de la balise est ce materiau :
 * il multiplie la texture par la couleur du sommet sans jamais eclairer.
 *
 * <p>POURQUOI PAS UN TYPE DE RENDU. Un type de rendu range porte son propre etat de profondeur,
 * et c'est <b>lui</b> qui le rebatit au moment du vidage — un {@code disableDepthTest()} pose
 * avant est donc sans effet, et c'est exactement ce qui rendait les minerais invisibles : ils
 * etaient bel et bien dessines, mais testes contre la pierre qui les entoure. Le port ecrit donc
 * ses sommets a la main, avec l'etat qu'il veut, comme le faisait l'original avec ses appels
 * OpenGL directs.
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

    /**
     * Le brouillard, repousse a l'infini le temps du dessin.
     *
     * <p>Il ne se remet pas : chaque passe du rendu du monde repose le sien avant de dessiner,
     * et c'est la nôtre qui doit etre la derniere a parler. Voir l'appel, et l'original, qui
     * faisait la meme chose avec {@code glDisable(GL_FOG)}.
     */
    private static final float FOG_AWAY = 1.0E9f;

    private MineDetectRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !MineDetectOverlay.active(player)) return;
        if (MineDetectOverlay.ores().isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Vec3 from = player.position();
        float range = (float) MineDetectOverlay.range();
        Matrix4f matrix = event.getPoseStack().last().pose();

        // Le rendu du monde est deja a l'origine du monde : c'est a nous de retirer la camera,
        // comme le fait le bouclier. Et le test de profondeur tombe : c'est le coeur de la
        // competence, voir le commentaire de la classe.
        //
        // LE BROUILLARD TOMBE AUSSI, et c'est l'original qui le dit : son rendu commencait par
        // glDisable(GL_FOG) et le remettait a la fin. Sans ca, la cecite — qui n'est qu'un
        // brouillard noir tres serre — repeignait les minerais eloignes en NOIR, et la portee
        // de l'eclat se lisait a quelques blocs alors qu'elle vaut 0,65 fois la portee de la
        // competence. Le repere du brouillard est simplement repousse a l'infini.
        RenderSystem.setShaderFogStart(FOG_AWAY);
        RenderSystem.setShaderFogEnd(FOG_AWAY);
        RenderSystem.setShader(GameRenderer::getRendertypeBeaconBeamShader);
        RenderSystem.setShaderTexture(0, MINEVIEW);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (MineDetectOverlay.Ore ore : MineDetectOverlay.ores()) {
            Vec3 centre = Vec3.atCenterOf(ore.pos());
            float alpha = MineDetectVisuals.alpha(from.distanceTo(centre), range);
            if (alpha <= 0f) continue;
            cube(buffer, matrix, ore.pos(), camera, MineDetectVisuals.colorFor(ore.tier()), alpha);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Les six faces d'un cube, du coin bas au coin haut. */
    private static void cube(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, Vec3 camera,
                             int[] rgb, float alpha) {
        float x0 = (float) (pos.getX() + LOW - camera.x);
        float y0 = (float) (pos.getY() + LOW - camera.y);
        float z0 = (float) (pos.getZ() + LOW - camera.z);
        float x1 = (float) (pos.getX() + HIGH - camera.x);
        float y1 = (float) (pos.getY() + HIGH - camera.y);
        float z1 = (float) (pos.getZ() + HIGH - camera.z);

        // Dessous et dessus.
        face(buffer, matrix, rgb, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(buffer, matrix, rgb, alpha, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        // Et les quatre cotes.
        face(buffer, matrix, rgb, alpha, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        face(buffer, matrix, rgb, alpha, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        face(buffer, matrix, rgb, alpha, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        face(buffer, matrix, rgb, alpha, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    /** Un quadrilatere, dans l'ordre de ses quatre coins, avec l'image entiere dessus. */
    private static void face(BufferBuilder buffer, Matrix4f matrix, int[] rgb, float alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(buffer, matrix, rgb, alpha, ax, ay, az, 0f, 0f);
        vertex(buffer, matrix, rgb, alpha, bx, by, bz, 1f, 0f);
        vertex(buffer, matrix, rgb, alpha, cx, cy, cz, 1f, 1f);
        vertex(buffer, matrix, rgb, alpha, dx, dy, dz, 0f, 1f);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, int[] rgb, float alpha,
                               float x, float y, float z, float u, float v) {
        buffer.vertex(matrix, x, y, z)
                .color(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, alpha)
                .uv(u, v)
                .endVertex();
    }
}
