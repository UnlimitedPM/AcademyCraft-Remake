package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.md.MdRenderType;
import cn.academy.entity.SilbarnVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les eclats de la bille de silicium, portage des particules de {@code EntitySilbarn} : la
 * poussiere qu'elle laisse quand elle se casse.
 *
 * <h2>Ce qu'ils sont</h2>
 *
 * <p>L'original semait de dix-huit a vingt-sept fragments a l'impact — texture
 * {@code entities/silbarn_frag}, dix centimetres de cote, une gravite de trois centiemes et une
 * rotation de vingt-cinq degres par tick sur deux axes. Ils jaillissent dans toutes les
 * directions, avec un petit coup vers le haut qui les fait monter avant de retomber.
 *
 * <p>Ils apparaissent <b>au moment ou la bille se pose</b>, et a cet instant seulement : la
 * pose est une donnee synchronisee, donc chaque client voit la bascule, et c'est
 * {@code SilbarnRenderer} qui la guette — le seul endroit par ou passe une bille chez un client
 * sans que le serveur ait a s'en meler.
 *
 * <h2>Le carre qui tourne</h2>
 *
 * <p>Chaque eclat est un carre qui regarde la camera, comme les etincelles du plasma, mais son
 * orientation <b>tourne</b> : ses deux directions sont celles de l'ecran, tournees de son angle.
 * C'est l'idee de la {@code customRotation} de l'original, qui faisait tourner ses fragments sur
 * deux axes ; le port n'en garde qu'un, celui qui se voit.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class SilbarnFrags {

    /** L'image des eclats, celle de l'original. */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/entities/silbarn_frag.png");

    /** Un eclat vivant. */
    public static final class Frag {

        private final double[] pos;
        private final double[] vel;
        private float angle;
        private int age;

        Frag(double[] pos, double[] vel, float angle) {
            this.pos = pos;
            this.vel = vel;
            this.angle = angle;
        }

        public double[] pos() {
            return pos;
        }

        /** Son orientation courante, en degres. */
        public float angle() {
            return angle;
        }

        void advance() {
            age++;
            pos[0] += vel[0];
            pos[1] += vel[1];
            pos[2] += vel[2];
            // Il retombe : la gravite de l'original, trois centiemes par tick — six fois moins
            // que celle de la bille, donc un nuage qui flotte plus qu'il ne tombe.
            vel[1] -= SilbarnVisuals.FRAG_GRAVITY;
            angle += SilbarnVisuals.FRAG_SPIN_PER_TICK;
        }

        boolean dead() {
            return age >= SilbarnVisuals.FRAG_LIFE_TICKS;
        }
    }

    private static final List<Frag> FRAGS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private SilbarnFrags() {}

    /** Seme les eclats d'une bille qui vient de se casser. */
    public static void spawn(Vec3 at) {
        spawn(at, RANDOM);
    }

    /** La meme, avec un hasard donne — pour le test. */
    public static void spawn(Vec3 at, Random random) {
        int count = SilbarnVisuals.FRAG_MIN
                + random.nextInt(SilbarnVisuals.FRAG_MAX - SilbarnVisuals.FRAG_MIN);

        for (int i = 0; i < count; i++) {
            double speed = SilbarnVisuals.FRAG_SPEED_MIN
                    + random.nextDouble() * (SilbarnVisuals.FRAG_SPEED_MAX - SilbarnVisuals.FRAG_SPEED_MIN);
            double[] direction = SilbarnVisuals.fragDirection(random);

            FRAGS.add(new Frag(
                    new double[] { at.x, at.y, at.z },
                    new double[] { direction[0] * speed, direction[1] * speed,
                                   direction[2] * speed },
                    (float) (random.nextDouble() * 360.0)));
        }
    }

    /** Un tick du client : les eclats volent, et les vieux s'en vont. */
    public static void tick() {
        for (int i = FRAGS.size() - 1; i >= 0; i--) {
            Frag frag = FRAGS.get(i);
            if (frag.dead()) {
                FRAGS.remove(i);
            } else {
                frag.advance();
            }
        }
    }

    /** Les eclats vivants, a dessiner. */
    public static List<Frag> live() {
        return FRAGS;
    }

    /** Tout oublier : la deconnexion d'un monde n'est pas une fin de bille. */
    public static void clear() {
        FRAGS.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (FRAGS.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Vector3f upVector = event.getCamera().getUpVector();
        Vector3f leftVector = event.getCamera().getLeftVector();
        double[][] screen = { { leftVector.x, leftVector.y, leftVector.z },
                              { upVector.x, upVector.y, upVector.z } };

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(MdRenderType.of(TEXTURE));

        for (Frag frag : FRAGS) {
            draw(out, pose.last(), camera, screen, frag);
        }
        buffers.endBatch();
    }

    /** Un eclat : un carre tourne vers la camera, et tourne sur lui-meme. */
    private static void draw(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                             double[][] screen, Frag frag) {
        double half = SilbarnVisuals.FRAG_SIZE / 2.0;
        double[] pos = frag.pos();

        // Ses deux directions sont celles de l'ecran, tournees de son angle : le carre se
        // presente donc toujours de face, mais de biais.
        double radians = Math.toRadians(frag.angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double[] left = { screen[0][0] * cos + screen[1][0] * sin,
                          screen[0][1] * cos + screen[1][1] * sin,
                          screen[0][2] * cos + screen[1][2] * sin };
        double[] up = { screen[1][0] * cos - screen[0][0] * sin,
                        screen[1][1] * cos - screen[0][1] * sin,
                        screen[1][2] * cos - screen[0][2] * sin };

        corner(out, pose, camera, pos, left, up, -half, -half, 0f, 0f);
        corner(out, pose, camera, pos, left, up, half, -half, 0f, 1f);
        corner(out, pose, camera, pos, left, up, half, half, 1f, 1f);
        corner(out, pose, camera, pos, left, up, -half, half, 1f, 0f);
    }

    private static void corner(VertexConsumer out, PoseStack.Pose pose, Vec3 camera, double[] pos,
                               double[] left, double[] up, double along, double high, float u,
                               float v) {
        out.vertex(pose.pose(),
                        (float) (pos[0] + left[0] * along + up[0] * high - camera.x),
                        (float) (pos[1] + left[1] * along + up[1] * high - camera.y),
                        (float) (pos[2] + left[2] * along + up[2] * high - camera.z))
                .color(1f, 1f, 1f, 1f)
                .uv(u, v)
                .endVertex();
    }
}
