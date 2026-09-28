package cn.academy.ability.client.arc;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.arc.ArcMesh.Segment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Dessine les eclairs des competences.
 *
 * <p>Portage du rendu d'{@code EntityArc} : l'original posait une entite au point de depart,
 * l'orientait vers la cible (lacet puis tangage), et dessinait son motif le long de l'axe.
 * Ici, le repere se construit directement : l'axe X du motif devient la direction de l'arc,
 * et les ecarts lateraux du motif sont poses sur les deux perpendiculaires. C'est le meme
 * dessin, sans angles d'Euler — et sans le cas ou viser droit vers le haut les rend
 * indetermines.
 *
 * <p>Chaque bout d'arc est un ruban de deux faces. Les deux faces, et pas une : le type de
 * rendu de Minecraft ecarte les faces arriere, et un ruban qu'on ne voit que d'un cote
 * disparait une fois sur deux quand on tourne autour. Le ruban est tourne vers la camera,
 * ce qui n'est pas exactement ce que faisait l'original — lui retournait chaque ruban au
 * hasard a chaque image, et cet effacement participait a son scintillement. On garde le
 * scintillement du motif, on perd son vacillement, et on y gagne un eclair qu'on voit
 * toujours.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class ArcRenderer {

    /** La texture d'un bout de trait : celle de l'original, inchangee. */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/arc/line_segment.png");

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // Quitter un monde emporte ses eclairs : ils appartiennent au monde, pas au jeu.
            ClientArcs.clear();
            return;
        }
        ClientArcs.tick(minecraft.level.getGameTime(), RANDOM);
    }

    /** Ouvre un eclair, sur le fil du client. Appele par le paquet de la competence. */
    public static void spawn(String pattern, Vec3 from, Vec3 to, int lifeTicks, boolean clipToDistance) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0 : minecraft.level.getGameTime();

        ClientArcs.spawn(ArcPattern.byName(pattern),
                new double[] { from.x, from.y, from.z },
                new double[] { to.x, to.y, to.z },
                lifeTicks, clipToDistance, gameTime, RANDOM);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ClientArcs.live().isEmpty()) return;

        // Les sommets se posent relativement a la camera : c'est ce que fait la pose du
        // rendu du monde, et un eclair pose en coordonnees du monde partirait a la derive
        // des qu'on s'eloigne de l'origine.
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        for (ClientArcs.LiveArc arc : ClientArcs.live()) {
            if (arc.visible()) {
                draw(out, pose.last().pose(), camera, arc);
            }
        }
        buffers.endBatch();
    }

    /** Un eclair entier : ses bouts, mis bout a bout le long de la direction visee. */
    private static void draw(VertexConsumer out, Matrix4f matrix, Vec3 camera, ClientArcs.LiveArc arc) {
        double[] from = arc.from();
        double[] to = arc.to();

        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1e-6) return;

        // L'axe de l'arc, et les deux perpendiculaires du motif : l'une porte les ecarts
        // lateraux du motif (son y), l'autre son z.
        double ux = dx / length, uy = dy / length, uz = dz / length;
        double[] side = perpendicular(ux, uy, uz);
        double[] up = cross(ux, uy, uz, side[0], side[1], side[2]);

        // La largeur d'un bout se prend dans le plan perpendiculaire a l'axe : celle de
        // l'original, qui croisait la direction du bout avec la normale de son motif.
        double[] lastWidth = side;
        for (Segment segment : arc.mesh().segments()) {
            double[] start = point(from, ux, uy, uz, side, up, segment.x0(), segment.y0(), segment.z0());
            double[] end = point(from, ux, uy, uz, side, up, segment.x1(), segment.y1(), segment.z1());

            double[] direction = normalize(end[0] - start[0], end[1] - start[1], end[2] - start[2]);
            double[] width = ribbonDirection(direction, start, camera);
            if (width == null) {
                // Le bout vise droit dans l'oeil : deux directions opposees tiennent le
                // meme ruban, donc on garde celle du bout precedent plutot que d'en tirer
                // une au hasard, qui ferait clignoter l'eclair.
                width = lastWidth;
            }
            lastWidth = width;

            double[] p1 = shift(start, width, -segment.width0());
            double[] p2 = shift(start, width, segment.width0());
            double[] p3 = shift(end, width, segment.width1());
            double[] p4 = shift(end, width, -segment.width1());

            float alpha = (float) segment.alpha();
            // Les deux faces : le rendu translucide de Minecraft ecarte les faces arriere.
            quad(out, matrix, camera, p1, p2, p3, p4, alpha);
            quad(out, matrix, camera, p4, p3, p2, p1, alpha);
        }
    }

    /**
     * Un point du motif dans le monde : {@code from} plus l'avance le long de l'axe, plus
     * les deux ecarts lateraux du motif.
     */
    private static double[] point(double[] from, double ux, double uy, double uz,
                                  double[] side, double[] up,
                                  double x, double y, double z) {
        return new double[] {
                from[0] + ux * x + side[0] * y + up[0] * z,
                from[1] + uy * x + side[1] * y + up[1] * z,
                from[2] + uz * x + side[2] * y + up[2] * z };
    }

    /** La direction de la largeur du ruban : perpendiculaire au trait, et tournee vers l'oeil. */
    private static double[] ribbonDirection(double[] direction, double[] start, Vec3 camera) {
        double[] toCamera = normalize(camera.x - start[0], camera.y - start[1], camera.z - start[2]);
        double[] width = cross(direction[0], direction[1], direction[2], toCamera[0], toCamera[1], toCamera[2]);
        double widthLength = Math.sqrt(width[0] * width[0] + width[1] * width[1] + width[2] * width[2]);
        if (widthLength < 1e-4) return null;
        return new double[] { width[0] / widthLength, width[1] / widthLength, width[2] / widthLength };
    }

    /** Une perpendiculaire quelconque a l'axe, choisie stable pour ne pas tourner d'une image a l'autre. */
    private static double[] perpendicular(double x, double y, double z) {
        // Le repere le plus eloigne de l'axe : prend la verticale, sauf si l'arc est
        // justement vertical ou le croisement serait nul.
        double[] reference = Math.abs(y) > 0.9 ? new double[] { 1, 0, 0 } : new double[] { 0, 1, 0 };
        double[] side = cross(x, y, z, reference[0], reference[1], reference[2]);
        return normalize(side[0], side[1], side[2]);
    }

    /** Un carre du ruban, dans l'ordre donne. */
    private static void quad(VertexConsumer out, Matrix4f matrix, Vec3 camera,
                             double[] a, double[] b, double[] c, double[] d, float alpha) {
        vertex(out, matrix, camera, a, 0f, 0f, alpha);
        vertex(out, matrix, camera, b, 0f, 1f, alpha);
        vertex(out, matrix, camera, c, 1f, 1f, alpha);
        vertex(out, matrix, camera, d, 1f, 0f, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 camera,
                               double[] point, float u, float v, float alpha) {
        out.vertex(matrix,
                        (float) (point[0] - camera.x),
                        (float) (point[1] - camera.y),
                        (float) (point[2] - camera.z))
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .overlayCoords(0)
                .uv2(0xf000f0)
                .endVertex();
    }

    private static double[] shift(double[] point, double[] direction, double amount) {
        return new double[] {
                point[0] + direction[0] * amount,
                point[1] + direction[1] * amount,
                point[2] + direction[2] * amount };
    }

    private static double[] normalize(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1e-9) return new double[] { 0, 1, 0 };
        return new double[] { x / length, y / length, z / length };
    }

    private static double[] cross(double ax, double ay, double az, double bx, double by, double bz) {
        return new double[] { ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx };
    }
}
