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
 * Dessine les particules de formule du coup critique.
 *
 * <p>Portage de {@code CriticalHitEffect} et du rendu de particules de l'original : chaque fragment est
 * un carre qui regarde la camera, de la taille qu'il s'est donnee a sa naissance, portant une des dix
 * images de formule, et dont l'opacite monte, se tient puis s'efface.
 *
 * <p>Son rouage est celui du sang de la chair arrachee : un carre tourne vers l'oeil, sans ecriture de
 * profondeur mais avec son test, donc sans passer au travers de la pierre. L'original ne l'eclairait pas
 * non plus — c'est ce que voulait son {@code hasLight = false} — et le type de rendu des particules de
 * la teleportation s'en charge deja.
 *
 * <p>Les dix images sont celles du dossier {@code effects/formula} de l'original, arrivees avec le reste
 * de ses ressources : elles attendaient leur effet.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class FormulaParticleRenderer {

    /** Les dix images de formule, dans l'ordre ou l'original les avait numerotees. */
    private static final ResourceLocation[] TEXTURES = textures();

    private FormulaParticleRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        List<FormulaParticles.Glyph> live = FormulaParticles.live();
        if (live.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Vector3f left = event.getCamera().getLeftVector();
        Vector3f up = event.getCamera().getUpVector();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();

        long now = Util.getMillis();
        for (FormulaParticles.Glyph glyph : live) {
            float alpha = glyph.alpha(now);
            if (alpha <= 0f) continue;

            VertexConsumer out = buffers.getBuffer(
                    TpRenderType.particle(TEXTURES[glyph.frame()]));
            drawGlyph(out, pose.last(), camera, glyph.position(now), glyph.size(), left, up, alpha);
        }

        buffers.endBatch();
    }

    /**
     * Un fragment : un carre qui regarde la camera.
     *
     * <p>Ses deux directions sont celles de l'ecran — la gauche et le haut de la camera — donc il se
     * presente toujours de face, quel que soit l'angle. C'est ce que faisait le {@code RenderIcon} de
     * l'original, et c'est aussi ce que font ses etincelles et son sang.
     */
    private static void drawGlyph(VertexConsumer out, PoseStack.Pose pose, Vec3 camera, Vec3 at,
                                  double size, Vector3f left, Vector3f up, float alpha) {
        double half = size / 2.0;

        glyphVertex(out, pose, camera, at, left, up, half, -half, 0f, 0f, alpha);
        glyphVertex(out, pose, camera, at, left, up, half, half, 0f, 1f, alpha);
        glyphVertex(out, pose, camera, at, left, up, -half, half, 1f, 1f, alpha);
        glyphVertex(out, pose, camera, at, left, up, -half, -half, 1f, 0f, alpha);
    }

    private static void glyphVertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera, Vec3 at,
                                    Vector3f left, Vector3f up, double along, double high,
                                    float u, float v, float alpha) {
        out.vertex(pose.pose(),
                        (float) (at.x + left.x * along + up.x * high - camera.x),
                        (float) (at.y + left.y * along + up.y * high - camera.y),
                        (float) (at.z + left.z * along + up.z * high - camera.z))
                .color(FormulaParticles.RED, FormulaParticles.GREEN, FormulaParticles.BLUE, alpha)
                .uv(u, v)
                .endVertex();
    }

    /** Les dix images, dans l'ordre de l'original. */
    private static ResourceLocation[] textures() {
        ResourceLocation[] textures = new ResourceLocation[FormulaParticles.TEXTURES];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = FormulaParticles.texture(i);
        }
        return textures;
    }
}
