package cn.academy.ability.client;

import cn.academy.AcademyCraftCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Dessine la marque au sol du claquement d'orage : l'onde blanche qui dit ou la foudre tombera.
 *
 * <p>C'est la {@code EntityRippleMark} de l'original, posee par son contexte client des le debut de
 * la charge et relue a chaque tick. Le port n'a pas d'entite d'effet : l'etat vit dans
 * {@link ThunderClapEffect}, qui le repose a chaque tick, et le dessin est celui de
 * {@link RippleMark} — la marque du reacteur du meltdowner, dont l'original n'avait qu'une seule
 * version pour les deux competences.
 *
 * <p>Elle n'existe que chez le lanceur, comme chez l'original : elle est posee depuis l'etat de
 * charge du client, qui ne connait que ses propres charges.
 */
public class ThunderClapRenderer {
    public static void render(net.minecraft.client.Camera cam, com.mojang.blaze3d.vertex.PoseStack poseStack, float ticks) {

        Vec3 at = ThunderClapEffect.markAt();
        if (at == null) return;

        double age = ThunderClapEffect.markAgeSeconds(ticks);
        if (age < 0) return;

        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();

        RippleMark.draw(poseStack, buffers, cam.getPosition(), at, age,
                ThunderClapEffect.MARK_RED, ThunderClapEffect.MARK_GREEN,
                ThunderClapEffect.MARK_BLUE);
        buffers.endBatch();
    }
}
