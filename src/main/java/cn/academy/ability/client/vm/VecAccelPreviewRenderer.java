package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Le rendu de la parabole de visee de l'acceleration de vecteur.
 *
 * <p>Un ruban <b>vertical</b> — et non tourne vers l'oeil, comme les etincelles : l'original le
 * dessinait dans le plan du monde, du haut en bas de chaque pas, ce qui donne a la trajectoire son
 * aspect de lame fine vue de profil et de fil vu dans l'axe. Il se dessine donc sur les deux faces,
 * sans lumiere, et sans ecrire la profondeur.
 *
 * <p>Une seule chose empeche de le voir : le <b>repli a la troisieme personne</b>. L'original ne la
 * dessinait qu'a la premiere — c'est un instrument de visee, et de l'exterieur il ne voudrait rien
 * dire.
 *
 * <p>Et elle se relit a chaque image, avec les angles <b>interpoles</b> du porteur : la parabole
 * suit le regard en continu, au lieu de sauter d'un tick a l'autre.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class VecAccelPreviewRenderer {

    /** L'image de l'original, telle quelle : une ligne de lueur douce. */
    private static final ResourceLocation LINE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/glow_line.png");

    private VecAccelPreviewRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        VecAccelPreview.Live live = VecAccelPreview.live();
        if (live == null) return;
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        Player player = live.player();
        float partialTick = event.getPartialTick();
        float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        float yaw = Mth.lerp(partialTick, player.yRotO, player.getYRot());
        Vec3 feet = player.getPosition(partialTick);
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);

        List<Vec3> points = VecAccelPreview.points(
                VecAccelPreview.handFrom(feet, look),
                VecAccelPreview.initialSpeed(pitch, yaw, live.speed()));
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();

        RenderSystem.setShader(GameRenderer::getRendertypeBeaconBeamShader);
        RenderSystem.setShaderTexture(0, LINE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (int i = 1; i < points.size(); i++) {
            float alpha = (float) VecAccelPreview.alpha(i);
            if (alpha <= 0f) break;
            ribbon(buffer, base, camera, points.get(i - 1), points.get(i), alpha);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Un pas du ruban : une bande verticale, entre deux points de la trajectoire. */
    private static void ribbon(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Vec3 from,
                               Vec3 to, float alpha) {
        double h = VecAccelPreview.RIBBON_HALF;
        vertex(buffer, matrix, camera, from.x, from.y + h, from.z, 0f, 0f, alpha);
        vertex(buffer, matrix, camera, from.x, from.y - h, from.z, 0f, 1f, alpha);
        vertex(buffer, matrix, camera, to.x, to.y - h, to.z, 1f, 1f, alpha);
        vertex(buffer, matrix, camera, to.x, to.y + h, to.z, 1f, 0f, alpha);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 camera,
                               double x, double y, double z, float u, float v, float alpha) {
        buffer.vertex(matrix, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z))
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }
}
