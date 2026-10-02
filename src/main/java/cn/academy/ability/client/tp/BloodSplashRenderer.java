package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;

/**
 * Dessine le sang de la chair arrachee.
 *
 * <p>Portage du {@code SplashRenderer} de l'original : chaque eclaboussure est un carre qui
 * regarde la camera, de la taille qu'elle s'est donnee a sa naissance, et qui defile ses dix
 * images une par tick avant de disparaitre.
 *
 * <p>Son rouage est celui du fantome de la teleportation — un carre tourne vers l'oeil, sans
 * ecriture de profondeur — mais il ne laisse pas voir au travers des murs : l'original non plus,
 * dont le sang etait dessine dans la passe des transparents, test de profondeur allume. Du sang
 * qui traverse la pierre se lit comme un defaut, pas comme un effet.
 *
 * <p>C'est ce que l'original demandait a son {@code glDepthMask(false)} : le sang se mele au
 * decor sans le masquer, et deux eclaboussures qui se croisent se voient toutes les deux.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class BloodSplashRenderer {

    /** Les dix images de la gerbe, dans l'ordre ou l'original les enchainait. */
    private static final ResourceLocation[] TEXTURES = frames();

    /** La teinte de l'original : un rouge sombre, a peine transparent. */
    public static final int COLOR = 0xC8D51D1D;

    private BloodSplashRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        List<BloodSplashes.Splash> live = BloodSplashes.live();
        if (live.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Vector3f left = event.getCamera().getLeftVector();
        Vector3f up = event.getCamera().getUpVector();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();

        int argb = COLOR;
        float red = ((argb >> 16) & 0xFF) / 255f;
        float green = ((argb >> 8) & 0xFF) / 255f;
        float blue = (argb & 0xFF) / 255f;
        float alpha = ((argb >>> 24) & 0xFF) / 255f;

        long now = Util.getMillis();
        for (BloodSplashes.Splash splash : live) {
            VertexConsumer out = buffers.getBuffer(
                    TpRenderType.particle(TEXTURES[splash.frame(now)]));
            drawSplash(out, pose.last(), camera, splash, left, up, red, green, blue, alpha);
        }

        buffers.endBatch();
    }

    /**
     * Une eclaboussure : un carre qui regarde la camera.
     *
     * <p>Ses deux directions sont celles de l'ecran — la gauche et le haut de la camera — donc
     * elle se presente toujours de face, quel que soit l'angle. C'est ce que faisait le
     * {@code RenderIcon} de l'original, et c'est aussi ce que font ses etincelles : le carre est
     * pose dans le plan de l'ecran, a la position de la tache.
     */
    private static void drawSplash(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                   BloodSplashes.Splash splash, Vector3f left, Vector3f up,
                                   float red, float green, float blue, float alpha) {
        Vec3 at = splash.position();
        double half = splash.size() / 2.0;

        splashVertex(out, pose, camera, at, left, up, half, -half, 0f, 0f, red, green, blue, alpha);
        splashVertex(out, pose, camera, at, left, up, half, half, 0f, 1f, red, green, blue, alpha);
        splashVertex(out, pose, camera, at, left, up, -half, half, 1f, 1f, red, green, blue, alpha);
        splashVertex(out, pose, camera, at, left, up, -half, -half, 1f, 0f, red, green, blue, alpha);
    }

    private static void splashVertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera, Vec3 at,
                                     Vector3f left, Vector3f up, double along, double high,
                                     float u, float v, float red, float green, float blue,
                                     float alpha) {
        out.vertex(pose.pose(),
                        (float) (at.x + left.x * along + up.x * high - camera.x),
                        (float) (at.y + left.y * along + up.y * high - camera.y),
                        (float) (at.z + left.z * along + up.z * high - camera.z))
                .color(red, green, blue, alpha)
                .uv(u, v)
                .endVertex();
    }

    /** Les dix images, dans l'ordre ou l'original les enchainait. */
    private static ResourceLocation[] frames() {
        ResourceLocation[] frames = new ResourceLocation[BloodSplashes.FRAMES];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                    "textures/effects/blood_splash/" + i + ".png");
        }
        return frames;
    }
}
