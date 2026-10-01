package cn.academy.ability.client.md;

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

/**
 * Dessine les effets du plasma dans le monde : les rayons et leurs etincelles.
 *
 * <p>L'original posait une entite par rayon — {@code EntityMdRaySmall} et ses soeurs — et une
 * par etincelle, et chacune se dessinait toute seule. Le port n'a pas d'entites d'effet : ses
 * rayons vivent dans {@link MdRays} et ses etincelles dans {@link MdSparks}, et c'est ici
 * qu'elles prennent forme.
 *
 * <h2>Ce qu'un rayon est fait</h2>
 *
 * <p>Trois choses, superposees comme chez l'original :
 *
 * <ul>
 *   <li>la <b>gaine</b>, un tube large et tres transparent, qui donne au rayon sa couleur et
 *       son epaisseur ;</li>
 *   <li>le <b>coeur</b>, un tube plus etroit et clair, qui fait le trait ;</li>
 *   <li>la <b>lueur</b>, un ruban blanc qui tourne avec la camera — c'est elle qui rend le
 *       rayon lumineux, les deux tubes n'etant larges que de quelques centimetres.</li>
 * </ul>
 *
 * <p>Le ruban est fait de <b>trois</b> images mises bout a bout, comme le
 * {@code RendererRayGlow} de l'original : une qui fait apparaitre la lueur, une qui la
 * prolonge, une qui l'efface. Sans elles, la lueur commencerait et finirait par un coup de
 * ciseaux, ce qui se voit tout de suite sur un rayon de quinze blocs.
 *
 * <p>Le tout garde le test de profondeur du monde : un rayon ne se voit pas a travers une
 * montagne, et c'est voulu — le joueur l'avait signale sur son propre tir.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class MdEffects {

    /** L'image des etincelles : celle de l'original, tel quel. */
    private static final ResourceLocation SPARK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/md_particle.png");

    /** Dix cotes par tube : assez pour que la section ronde se lise, et rien de plus. */
    private static final int SIDES = 10;

    /** Quatre marches pour un bout arrondi, comme la « tete » de l'original. */
    private static final int CAP_STEPS = 4;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        long now = Util.getMillis();
        // Le tremblement des lueurs se fait par IMAGE, comme dans l'original : il ne se lit
        // pas au tick.
        MdRays.advanceFrame(now);
        if (MdRays.live().isEmpty() && MdSparks.live().isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Vector3f upVector = event.getCamera().getUpVector();
        Vector3f leftVector = event.getCamera().getLeftVector();
        double[] above = { upVector.x, upVector.y, upVector.z };
        double[][] screen = { { leftVector.x, leftVector.y, leftVector.z },
                              { upVector.x, upVector.y, upVector.z } };

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();

        for (MdRays.LiveRay ray : MdRays.live()) {
            drawRay(buffers, pose.last(), camera, above, ray, now);
        }
        if (!MdSparks.live().isEmpty()) {
            VertexConsumer out = buffers.getBuffer(MdRenderType.of(SPARK_TEXTURE));
            for (MdSparks.Spark spark : MdSparks.live()) {
                drawSpark(out, pose.last(), camera, screen, spark, now);
            }
        }
        buffers.endBatch();
    }

    /**
     * Un rayon : ses deux tubes, puis sa lueur par-dessus.
     *
     * <p>La longueur dessinee est celle de {@link MdRays.LiveRay#drawnLength} — le rayon pousse
     * depuis son depart pendant les deux premiers dixiemes de seconde au lieu d'apparaitre
     * entier — et sa largeur comme son opacite tombent a la fin de sa vie.
     */
    private static void drawRay(MultiBufferSource buffers, PoseStack.Pose pose, Vec3 camera,
                                double[] above, MdRays.LiveRay ray, long now) {
        MdRayKind kind = ray.kind();
        double[] from = ray.from();
        double[] axis = normalize(ray.to()[0] - from[0], ray.to()[1] - from[1],
                ray.to()[2] - from[2]);
        if (axis == null) return;

        double drawn = ray.drawnLength(now);
        double[] to = { from[0] + axis[0] * drawn, from[1] + axis[1] * drawn,
                        from[2] + axis[2] * drawn };

        double width = ray.widthFactor(now);
        float alpha = ray.alpha(now);
        if (alpha <= 0f || width <= 0.0) return;

        double[] u = perpendicular(axis, above);
        double[] v = cross(axis, u);
        if (v == null) return;

        VertexConsumer tubes = buffers.getBuffer(MdRenderType.flat());
        double outer = kind.outerRadius() * width;
        double inner = kind.innerRadius() * width;
        cylinder(tubes, pose, camera, from, to, u, v, outer, kind.outer(), alpha);
        cylinder(tubes, pose, camera, from, to, u, v, inner, kind.inner(), alpha);
        // Le bout de depart est arrondi : la tete de l'original suivait une racine, ce qui
        // fait une ogive plutot qu'un tuyau coupe.
        head(tubes, pose, camera, from, u, v, axis, inner, kind.inner(), alpha);

        glowBoards(buffers, pose, camera, ray, from, to, axis, width, alpha, now);
    }

    /** La lueur d'un rayon : ses trois morceaux, poses bout a bout. */
    private static void glowBoards(MultiBufferSource buffers, PoseStack.Pose pose, Vec3 camera,
                                   MdRays.LiveRay ray, double[] from, double[] to, double[] axis,
                                   double width, float alpha, long now) {
        MdRayKind kind = ray.kind();

        // Deux choses differentes, et c'est la que le port s'etait trompe : la LONGUEUR des
        // morceaux d'entree et de sortie, et la LARGEUR du ruban. L'original leur donnait le meme
        // nombre, parce qu'il le passait a un `drawBoard` qui le divisait par deux — le ruban fait
        // donc la largeur annoncee, et non son double. Le port la passait telle quelle et la lueur
        // de tous les rayons etait deux fois trop large : invisible sur les petits rayons, et tres
        // visible sur le faisceau du meltdowner, dont la lueur d'un bloc et demi en faisait trois.
        double span = kind.glowWidth() * width;
        double half = span / 2;

        // L'original multipliait l'opacite de la lueur par celle du rayon, puis encore par la
        // sienne — d'ou le carre de l'opacite, et son tremblement entre 0,9 et 1.
        float glow = (float) (kind.glowAlpha() * alpha * ray.glowAlpha(now));

        // Le bout de la lueur n'est pas toujours la pointe du rayon : son dernier morceau peut la
        // depasser — c'est l'`endFix` de l'original, que le pre-rayon de la salve reprend pour
        // couvrir la bille de silicium entiere. Voir `MdRayKind.glowEndFix`.
        double[] tip = add(to, axis, kind.glowEndFix());

        double[] in = add(from, axis, span);
        double[] out = add(tip, axis, -span);

        board(buffers, pose, camera, kind.glowIn(), from, in, axis, half, glow);
        if (length(from, out) > 0) {
            board(buffers, pose, camera, kind.glowTile(), in, out, axis, half, glow);
        }
        board(buffers, pose, camera, kind.glowOut(), out, tip, axis, half, glow);
    }

    /** Un morceau de lueur : un ruban qui tourne avec la camera. */
    private static void board(MultiBufferSource buffers, PoseStack.Pose pose, Vec3 camera,
                              ResourceLocation texture, double[] from, double[] to, double[] axis,
                              double half, float alpha) {
        if (alpha <= 0f) return;

        // La largeur du ruban est perpendiculaire a l'axe ET au regard : il se presente donc
        // toujours de face, et c'est ce qui fait sa lumiere.
        double[] view = normalize(camera.x - from[0], camera.y - from[1], camera.z - from[2]);
        double[] side = view == null ? null : cross(axis, view);
        if (side == null) return;

        VertexConsumer out = buffers.getBuffer(MdRenderType.of(texture));
        vertex(out, pose, camera, from, side, half, 0f, 0f, alpha);
        vertex(out, pose, camera, from, side, -half, 0f, 1f, alpha);
        vertex(out, pose, camera, to, side, -half, 1f, 1f, alpha);
        vertex(out, pose, camera, to, side, half, 1f, 0f, alpha);
    }

    /** Un coin de ruban, texte. */
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                               double[] at, double[] side, double offset, float u, float v,
                               float alpha) {
        out.vertex(pose.pose(),
                        (float) (at[0] + side[0] * offset - camera.x),
                        (float) (at[1] + side[1] * offset - camera.y),
                        (float) (at[2] + side[2] * offset - camera.z))
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }

    /** Un cylindre : {@code SIDES} quadrilateres entre les deux cercles. */
    private static void cylinder(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                 double[] from, double[] to, double[] u, double[] v, double radius,
                                 MdRayKind.Tint tint, float alpha) {
        for (int i = 0; i < SIDES; i++) {
            double a0 = Math.PI * 2 * i / SIDES;
            double a1 = Math.PI * 2 * (i + 1) / SIDES;
            double[] d0 = ring(u, v, a0);
            double[] d1 = ring(u, v, a1);

            tubeVertex(out, pose, camera, from, d0, radius, tint, alpha);
            tubeVertex(out, pose, camera, from, d1, radius, tint, alpha);
            tubeVertex(out, pose, camera, to, d1, radius, tint, alpha);
            tubeVertex(out, pose, camera, to, d0, radius, tint, alpha);
        }
    }

    /**
     * Le bout arrondi d'un tube : des anneaux qui retrecissent en racine de la distance.
     *
     * <p>C'est la courbe de la « tete » de l'original, {@code y = racine(x)} : le rayon est
     * donc plein au ras du tube et s'effile jusqu'au point, sur une longueur d'un rayon.
     */
    private static void head(VertexConsumer out, PoseStack.Pose pose, Vec3 camera, double[] at,
                             double[] u, double[] v, double[] axis, double radius,
                             MdRayKind.Tint tint, float alpha) {
        for (int step = 0; step < CAP_STEPS; step++) {
            double t0 = step / (double) CAP_STEPS;
            double t1 = (step + 1) / (double) CAP_STEPS;
            double r0 = radius * Math.sqrt(1.0 - t0);
            double r1 = radius * Math.sqrt(1.0 - t1);
            double[] c0 = add(at, axis, -radius * t0);
            double[] c1 = add(at, axis, -radius * t1);

            for (int i = 0; i < SIDES; i++) {
                double[] d0 = ring(u, v, Math.PI * 2 * i / SIDES);
                double[] d1 = ring(u, v, Math.PI * 2 * (i + 1) / SIDES);

                tubeVertex(out, pose, camera, c0, d0, r0, tint, alpha);
                tubeVertex(out, pose, camera, c0, d1, r0, tint, alpha);
                tubeVertex(out, pose, camera, c1, d1, r1, tint, alpha);
                tubeVertex(out, pose, camera, c1, d0, r1, tint, alpha);
            }
        }
    }

    /** Une direction de l'anneau : la combinaison des deux perpendiculaires a l'axe. */
    private static double[] ring(double[] u, double[] v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[] { u[0] * cos + v[0] * sin, u[1] * cos + v[1] * sin,
                              u[2] * cos + v[2] * sin };
    }

    /** Un coin de tube : un bout, une direction de section, et son rayon. */
    private static void tubeVertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                   double[] end, double[] direction, double radius,
                                   MdRayKind.Tint tint, float alpha) {
        out.vertex(pose.pose(),
                        (float) (end[0] + direction[0] * radius - camera.x),
                        (float) (end[1] + direction[1] * radius - camera.y),
                        (float) (end[2] + direction[2] * radius - camera.z))
                .color(tint.red(), tint.green(), tint.blue(), tint.alpha() * alpha)
                .endVertex();
    }

    /**
     * Une etincelle : un carre qui regarde la camera.
     *
     * <p>Ses deux directions sont celles de l'ecran — la gauche et le haut de la camera — donc
     * elle se presente toujours de face, quel que soit l'angle. C'est ce que faisait le
     * {@code Sprite} de l'original, qui se contentait de tourner son carre vers le joueur.
     */
    private static void drawSpark(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                  double[][] screen, MdSparks.Spark spark, long now) {
        float alpha = spark.alpha(now);
        if (alpha <= 0f) return;

        double[] pos = spark.pos();
        double half = spark.size() / 2.0;
        double[] left = screen[0];
        double[] up = screen[1];

        sparkVertex(out, pose, camera, pos, left, up, half, -half, 0f, 0f, alpha);
        sparkVertex(out, pose, camera, pos, left, up, half, half, 0f, 1f, alpha);
        sparkVertex(out, pose, camera, pos, left, up, -half, half, 1f, 1f, alpha);
        sparkVertex(out, pose, camera, pos, left, up, -half, -half, 1f, 0f, alpha);
    }

    private static void sparkVertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                    double[] pos, double[] left, double[] up, double along,
                                    double high, float u, float v, float alpha) {
        out.vertex(pose.pose(),
                        (float) (pos[0] + left[0] * along + up[0] * high - camera.x),
                        (float) (pos[1] + left[1] * along + up[1] * high - camera.y),
                        (float) (pos[2] + left[2] * along + up[2] * high - camera.z))
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }

    private static double[] add(double[] point, double[] direction, double amount) {
        return new double[] { point[0] + direction[0] * amount, point[1] + direction[1] * amount,
                              point[2] + direction[2] * amount };
    }

    private static double length(double[] from, double[] to) {
        return Math.sqrt(sqr(to[0] - from[0]) + sqr(to[1] - from[1]) + sqr(to[2] - from[2]));
    }

    private static double sqr(double value) {
        return value * value;
    }

    /** Un vecteur normalise, ou {@code null} s'il n'a pas de longueur. */
    private static double[] normalize(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-4) return null;
        return new double[] { x / length, y / length, z / length };
    }

    /** Une perpendiculaire a l'axe, tiree du haut de la camera ; l'autre vient du produit vectoriel. */
    private static double[] perpendicular(double[] axis, double[] above) {
        double[] side = cross(axis, above);
        if (side == null) side = cross(axis, new double[] { 0, 1, 0 });
        if (side == null) side = cross(axis, new double[] { 1, 0, 0 });
        return side == null ? new double[] { 1, 0, 0 } : side;
    }

    /** Le produit vectoriel de deux vecteurs, normalise ; {@code null} s'ils sont paralleles. */
    private static double[] cross(double[] a, double[] b) {
        return normalize(a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]);
    }
}
