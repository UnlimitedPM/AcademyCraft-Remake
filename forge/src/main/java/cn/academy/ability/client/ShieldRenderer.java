package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Dessine le bouclier de lumiere devant son porteur, portage de {@code RenderMdShield}.
 *
 * <p>L'original avait une entite cliente et un rendu a elle : un disque texture qui
 * flotte un bloc devant les yeux, regarde ou le joueur regarde, et tourne sur lui-meme
 * de plus en plus vite. Le port n'a pas d'entite pour cela — l'etat d'un maintien vit
 * chez le serveur — donc c'est dessine directement pendant le rendu du monde, aux memes
 * coordonnees.
 *
 * <p>Seul le porteur voit son bouclier : un maintien n'est pas synchronise, comme une
 * charge, donc les autres joueurs ne peuvent pas le savoir. C'est la limite de ce
 * portage, et elle est notee dans le cerveau du projet.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class ShieldRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/effects/mdshield.png");

    /** Rotation courante du disque, en degres : elle s'accumule d'une image a l'autre. */
    private static float spin;

    /** Instant de l'image precedente, en nanosecondes. */
    private static long lastFrame;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        // C'est SA charge qui est lue, et non « la charge en cours » : le joueur peut tenir le
        // bouclier et charger autre chose en meme temps. Voir ClientCharge.
        if (!ShieldVisuals.showsShield(ShieldVisuals.SKILL,
                ClientCharge.isSustained(ShieldVisuals.SKILL))) {
            // Rien a dessiner : on remet le disque a zero pour que le prochain bouclier
            // reparte de la meme position, comme une entite neuve.
            spin = 0;
            lastFrame = 0;
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        int ticks = ClientCharge.getTicks(ShieldVisuals.SKILL);
        long now = System.nanoTime();
        float deltaMs = lastFrame == 0 ? 0f : (now - lastFrame) / 1_000_000f;
        lastFrame = now;
        spin = (spin + ShieldVisuals.spinSpeed(ticks) * deltaMs) % 360f;

        float partialTick = event.getPartialTick();
        // La direction de visee est deja interpolee : le bouclier suit donc la souris
        // sans a-coup, comme l'entite de l'original qui copiait le lacet de la tete.
        Vec3 look = player.getViewVector(partialTick);
        // Et la POSITION l'est aussi. L'original etait une entite, donc le moteur la dessinait
        // entre son ancienne et sa nouvelle place : la lire sur `position()` — la place du
        // dernier tick — fait avancer le disque par bonds d'un vingtieme de seconde pendant que
        // le reste du monde glisse, et c'est ce que le joueur a vu : « l'animation de quand on
        // se deplace est bizarre, comme les billes de tout a l'heure ». La bille avait le meme
        // defaut, dans l'autre sens : elle etait dessinee interpolee alors que le serveur la
        // collait a son porteur, et il a fallu la recoller. Ici, c'est l'inverse : tout ce qui
        // bouge dans une image doit se lire a la meme image.
        Vec3 pos = player.getPosition(partialTick)
                .add(look.scale(ShieldVisuals.DISTANCE))
                .add(0, ShieldVisuals.HEIGHT, 0);
        Vec3 camera = event.getCamera().getPosition();

        double yaw = Math.toDegrees(Math.atan2(-look.x, look.z));
        double pitch = Math.toDegrees(Math.asin(-look.y));

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        // Le disque regarde ou le joueur regarde, puis tourne sur lui-meme : c'est
        // l'ordre de l'original (lacet, tangage, rotation).
        pose.mulPose(Axis.YP.rotationDegrees((float) -yaw));
        pose.mulPose(Axis.XP.rotationDegrees((float) pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        pose.scale(ShieldVisuals.scale(ticks), ShieldVisuals.scale(ticks), 1f);

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        // Le type des effets du plasma, et pas celui des entites translucides : le second ECrit
        // la profondeur, et le disque est a un bloc devant les yeux — donc devant tout ce qui se
        // dessine apres lui, les nuages compris. Le joueur en a fait l'essai : « quand on regarde
        // le ciel il devient sombre ». La regle est celle des images du plasma, apprise sur la
        // bille : un effet translucide ne dispute pas la profondeur (voir MdRenderType).
        VertexConsumer consumer = buffers.getBuffer(
                cn.academy.ability.client.md.MdRenderType.of(TEXTURE));
        Matrix4f matrix = pose.last().pose();
        float alpha = ShieldVisuals.alpha(ticks);
        // Les deux faces : le rendu translucide de Minecraft ecarte les faces arriere,
        // la ou l'original se contentait de desactiver ce test.
        quad(consumer, matrix, alpha, false);
        quad(consumer, matrix, alpha, true);
        buffers.endBatch();
        pose.popPose();
    }

    /** Un carre d'un bloc de cote, centre sur l'origine. */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, float alpha, boolean back) {
        float near = -0.5f;
        float far = 0.5f;
        if (back) {
            vertex(consumer, matrix, near, far, 0f, 0f, alpha);
            vertex(consumer, matrix, far, far, 1f, 0f, alpha);
            vertex(consumer, matrix, far, near, 1f, 1f, alpha);
            vertex(consumer, matrix, near, near, 0f, 1f, alpha);
        } else {
            vertex(consumer, matrix, near, near, 0f, 1f, alpha);
            vertex(consumer, matrix, far, near, 1f, 1f, alpha);
            vertex(consumer, matrix, far, far, 1f, 0f, alpha);
            vertex(consumer, matrix, near, far, 0f, 0f, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            float x, float y, float u, float v, float alpha) {
        consumer.vertex(matrix, x, y, 0f)
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }
}
