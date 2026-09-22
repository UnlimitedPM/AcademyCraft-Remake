package cn.academy.ability.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import cn.academy.AcademyCraft;
import cn.academy.ability.electromaster.MagManipVisuals;
import cn.academy.entity.EntityMagManipBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dessine le bloc de la manipulation magnetique, portage du rendu de {@code EntityBlock}.
 *
 * <p>L'original avait un rendu d'entite et un modele de cube : le port dessine le bloc
 * lui-meme, avec le moteur de blocs du jeu, depuis le rendu du monde — comme le bouclier et
 * les minerais, et pour la meme raison : ce qui se dessine ici n'a pas besoin d'etre une
 * entite cliente.
 *
 * <p>Le bloc <b>tourne</b> en l'air, et c'est l'original : deux degres par tick tires au
 * hasard, un pour le lacet et un pour le tangage. Le port les deduit de l'identifiant de
 * l'entite — voir {@link MagManipVisuals} — donc deux blocs attrapes ne tournent pas au meme
 * rythme, sans qu'aucun tirage n'ait lieu a chaque image.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class MagManipRenderer {

    private MagManipRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        boolean drawn = false;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityMagManipBlock block)) continue;

            pose.pushPose();
            pose.translate(block.getX() - camera.x - 0.5, block.getY() - camera.y,
                    block.getZ() - camera.z - 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(
                    (float) MagManipVisuals.spinYaw(block.tickCount, block.getId())));
            pose.mulPose(Axis.XP.rotationDegrees(
                    (float) MagManipVisuals.spinPitch(block.tickCount, block.getId())));
            client.getBlockRenderer().renderSingleBlock(block.getBlockState(), pose, buffers,
                    LevelRenderer.getLightColor(client.level, block.blockPosition()),
                    OverlayTexture.NO_OVERLAY);
            pose.popPose();
            drawn = true;
        }

        if (drawn) buffers.endBatch();
    }
}
