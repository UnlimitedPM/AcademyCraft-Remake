package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Le rendu des tornades de vecmanip, portage de {@code TornadoRenderer}.
 *
 * <p>Une tornade est une pile d'anneaux, et chaque anneau est un <b>ruban</b> : vingt quads poses
 * cote a cote autour d'un cercle, dont l'image tourne avec le temps. Le port les ecrit d'un seul
 * tampon, comme l'original faisait ses {@code GL_QUADS}.
 *
 * <h2>Le repere, et l'ordre des transformations</h2>
 *
 * <p>C'est le point delicat de ce portage, et il se recopie de l'original dans le meme ordre :
 * la position du monde, puis le lacet du porteur, puis son tangage (reduit a un cinquieme pour
 * les ailes, telles quelles pour rien d'autre), puis l'inclinaison de l'effet — soixante-dix
 * degres pour les ailes, qui les couche vers l'arriere —, puis le recul, puis, par fuseau, son
 * ecartement et ses deux rotations. Les memes nombres, dans le meme ordre, donnent la meme
 * forme ; les intervertir donnerait des ailes tordues.
 *
 * <p>Et le dessin lui-meme suit les memes regles que les autres effets du port : <b>aucune
 * lumiere</b> — une tornade de plasma emet la sienne —, aucune face cachee (un ruban n'a qu'une
 * face), et <b>pas d'ecriture de profondeur</b> : les anneaux se croisent en permanence, et
 * s'ils ecrivaient la profondeur ils se decouperaient les uns les autres au hasard.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class TornadoRenderer {

    /** L'image de l'original, telle quelle : un anneau qui se repete autour du cercle. */
    private static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/tornado_ring.png");

    private static final double TAU = Math.PI * 2;

    private TornadoRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        List<VecmanipTornados.Live> live = VecmanipTornados.live();
        if (live.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();
        float partialTick = event.getPartialTick();
        double seconds = Util.getMillis() / 1000.0;
        // La lumiere des anneaux est celle de l'oeil : c'est ainsi que l'ombre suit le regard. Voir
        // shade.
        Vector3f lamp = event.getCamera().getLookVector();

        RenderSystem.setShader(GameRenderer::getRendertypeBeaconBeamShader);
        RenderSystem.setShaderTexture(0, RING);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (VecmanipTornados.Live tornado : live) {
            float alpha = (float) (tornado.alpha() * TornadoVisuals.DRAW_ALPHA);

            // Les ailes SUIVENT leur porteur, et se relisent donc a chaque image avec ses valeurs
            // interpolees : au tick, elles resteraient une image en arriere et traineraient
            // derriere le joueur des qu'il tourne la tete. La colonne du canon, elle, est posee
            // une fois pour toutes.
            //
            // L'orientation est celle du CORPS — les ailes tiennent au dos — et la hauteur celle
            // des epaules : voir VecmanipTornados.
            var follower = tornado.following();
            Vec3 position = follower == null
                    ? tornado.position()
                    : follower.getPosition(partialTick)
                            .add(0, TornadoVisuals.SHOULDERS, 0);
            float yaw = follower == null ? tornado.yaw()
                    : Mth.lerp(partialTick, follower.yBodyRotO, follower.yBodyRot);
            float pitch = follower == null ? tornado.pitch()
                    : Mth.lerp(partialTick, follower.xRotO, follower.getXRot());

            Matrix4f root = new Matrix4f(base)
                    .translate((float) (position.x - camera.x),
                            (float) (position.y - camera.y),
                            (float) (position.z - camera.z))
                    .rotateY((float) Math.toRadians(-yaw))
                    .rotateX((float) Math.toRadians(pitch * 0.2))
                    .rotateX((float) Math.toRadians(tornado.layout().tiltX()))
                    .translate((float) tornado.layout().preX(), (float) tornado.layout().preY(),
                            (float) tornado.layout().preZ());

            for (TornadoVisuals.Part part : tornado.layout().parts()) {
                Matrix4f matrix = new Matrix4f(root)
                        .translate((float) part.tx(), (float) part.ty(), (float) part.tz())
                        .rotateY((float) Math.toRadians(part.rotateY()))
                        .rotateZ((float) Math.toRadians(part.rotateZ()));
                drawTornado(buffer, matrix, part.tornado(), seconds, alpha, lamp);
            }
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Une tornade entiere : ses anneaux, du pied au sommet. */
    private static void drawTornado(BufferBuilder buffer, Matrix4f matrix,
                                    TornadoVisuals.Tornado tornado, double seconds, float alpha,
                                    Vector3f lamp) {
        double t = TornadoVisuals.time(seconds, tornado.timeOffset());
        double uStep = 1.0 / TornadoVisuals.SEGMENTS;

        for (TornadoVisuals.Ring ring : tornado.rings()) {
            double ny = ring.y() / tornado.height();
            double[] wobble = TornadoVisuals.wobble(ny, t);
            double dx = wobble[0] * tornado.size() * tornado.scale();
            double dz = wobble[1] * tornado.size() * tornado.scale();
            double radius = TornadoVisuals.radius(ny, t) * tornado.size() * ring.sizeScale();
            double rotation = TornadoVisuals.spin(ny, t) + ring.phase();
            double y0 = ring.y() + ring.width() / 2;
            double y1 = ring.y() - ring.width() / 2;

            for (int i = 0; i < TornadoVisuals.SEGMENTS; i++) {
                double a0 = i * TAU / TornadoVisuals.SEGMENTS;
                double a1 = (i + 1) * TAU / TornadoVisuals.SEGMENTS;
                double x0 = Math.sin(a0) * radius, z0 = Math.cos(a0) * radius;
                double x1 = Math.sin(a1) * radius, z1 = Math.cos(a1) * radius;
                double u0 = (double) (uStep * i - rotation);
                double u1 = u0 + uStep;

                // L'original posait UNE normale par quad : glNormal3d(x0, y0, 0). Voir shade.
                float shade = shade(matrix, x0, y0, lamp);

                vertex(buffer, matrix, alpha, shade, x0 + dx, y0, z0 + dz, (float) u0, 0f);
                vertex(buffer, matrix, alpha, shade, x0 + dx, y1, z0 + dz, (float) u0, 1f);
                vertex(buffer, matrix, alpha, shade, x1 + dx, y1, z1 + dz, (float) u1, 1f);
                vertex(buffer, matrix, alpha, shade, x1 + dx, y0, z1 + dz, (float) u1, 0f);
            }
        }
    }

    /** Le plancher d'ombre : une face detournee n'est jamais noire, la lueur se voit encore. */
    private static final float SHADE_MIN = 0.55f;
    static final float SHADE_MAX = 1.0f;

    /**
     * L'ombre d'un anneau : la normale de l'original, telle quelle, eclairee par l'oeil.
     *
     * <p>Cette normale est bizarre, et c'est la sienne : le rayon du cercle a gauche, la hauteur de
     * l'anneau au milieu, et rien a droite — {@code (x0, y0, 0)}. Elle n'a donc pas grand-chose a
     * voir avec la vraie normale du ruban, mais elle suffit a son office : le long de l'axe elle
     * fait glisser la lumiere, et comme chaque aile est inclinee de son cote, les quatre en
     * prennent une differente. Une aile qui regarde vers le bas prend l'ombre, comme il faut — et
     * cette ombre suit le regard, parce que la lumiere est celle de l'oeil.
     */
    static float shade(Matrix4f matrix, double x0, double y0, Vector3f look) {
        Vector3f normal = matrix.transformDirection(new Vector3f((float) x0, (float) y0, 0f));
        float length = normal.length();
        if (length < 1e-6f) return SHADE_MAX;
        // La direction qui va de la surface vers la lumiere : l'oeil est derriere, donc l'oppose
        // du regard.
        float lambert = Math.max(0f, normal.div(length).dot(new Vector3f(look).negate()));
        return SHADE_MIN + (SHADE_MAX - SHADE_MIN) * lambert;
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float alpha, float shade,
                               double x, double y, double z, float u, float v) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(shade, shade, shade, alpha)
                .uv(u, v)
                .endVertex();
    }
}
