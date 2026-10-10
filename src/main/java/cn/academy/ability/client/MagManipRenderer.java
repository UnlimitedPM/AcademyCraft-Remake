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
 *
 * <p>Et il tourne sur son <b>centre</b>, ce qui tient a l'ORDRE des trois mouvements — celui de
 * l'original : on se pose sur la position de l'entite, qui est le centre du bloc, on tourne, puis
 * on recule d'un demi-bloc pour que le moteur, qui dessine ses modeles de 0 a 1, pose le bloc
 * centre sur ce point. Dans l'autre ordre le bloc tournait autour de son COIN : son centre
 * decrivait alors un petit cercle autour du curseur, et le joueur a vu le bloc pencher en bas a
 * gauche de sa visee au lieu de rester dessus.
 *
 * <p>Le bloc se dessine AVANT les arcs, et c'est la seule raison de cette etape-ci : l'etape des
 * particules, celle des arcs, vient apres celle des entites, donc le bloc est deja pose quand le
 * gresillement passe devant lui. Dans l'autre ordre — le bloc apres les arcs — le ruban d'arc
 * ecrivait sa profondeur, y compris dans les parties transparentes de sa texture, et le bloc
 * dessine derriere se faisait refuser : le joueur voyait le monde a travers lui, dans la forme
 * exacte du carre de l'arc. C'est le meme piege que le fond transparent du railgun.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class MagManipRenderer {

    private MagManipRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Les ENTITES, pas les particules : le bloc doit etre dessine avant les arcs, qui
        // passent leur temps devant lui. Voir la tete de la classe.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick();
        boolean drawn = false;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityMagManipBlock block)) continue;

            // La POSITION INTERPOLEE, comme pour n'importe quelle entite : c'est celle que le jeu
            // donne a `doRender` depuis toujours, et l'original s'en servait. Lire `getX()` nu
            // fait avancer le bloc par bonds de tick — deux blocs d'un coup quand il vole —, ce
            // que le joueur appelait des « ralentissements ».
            Vec3 at = block.getPosition(partialTick);
            pose.pushPose();
            pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            // L'age du RENDU, lui aussi interpole : l'original dessinait son lacet entre son
            // ancien et son nouveau (`lerpf(e.lastYaw, e.yaw, pt)`), et la rotation repart donc du
            // tick d'avant, avance du temps partiel.
            double age = block.tickCount - 1 + partialTick;
            pose.mulPose(Axis.YP.rotationDegrees(
                    (float) MagManipVisuals.spinYaw(age, block.getId())));
            pose.mulPose(Axis.XP.rotationDegrees(
                    (float) MagManipVisuals.spinPitch(age, block.getId())));
            // Le recul vient APRES la rotation : le bloc tourne ainsi sur son centre, au lieu de
            // tourner autour de son coin et de partir de biais.
            pose.translate(-0.5, -0.5, -0.5);
            client.getBlockRenderer().renderSingleBlock(block.getBlockState(), pose, buffers,
                    LevelRenderer.getLightColor(client.level, block.blockPosition()),
                    OverlayTexture.NO_OVERLAY);
            // Et son jumeau, un bloc plus haut ou plus bas, DANS le repere du bloc — donc apres la
            // rotation : le couple tourne d'un seul tenant, comme l'objet qu'il est. Voir
            // EntityMagManipBlock, qui les emporte et les repose ensemble.
            if (block.hasCompanion()) {
                pose.pushPose();
                pose.translate(0, block.companionDy(), 0);
                client.getBlockRenderer().renderSingleBlock(block.getCompanion(), pose, buffers,
                        LevelRenderer.getLightColor(client.level,
                                block.blockPosition().offset(0, block.companionDy(), 0)),
                        OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            pose.popPose();
            drawn = true;
        }

        if (drawn) buffers.endBatch();
    }
}
