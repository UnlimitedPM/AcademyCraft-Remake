package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
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
 * <p>Et le dessin lui-meme suit les memes regles que les autres effets du port : <b>aucun
 * eclairage de la scene</b> — une tornade de plasma emet la sienne —, aucune face cachee (un ruban
 * n'a qu'une face), et <b>pas d'ecriture de profondeur</b> : les anneaux se croisent en permanence,
 * et s'ils ecrivaient la profondeur ils se decouperaient les uns les autres au hasard.
 *
 * <p>Reste l'ombre, prise a la main : chaque quad porte la lumiere du <b>soleil</b> — sa direction
 * dans le monde, et sa hauteur. Voir {@link #shade}.
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
        if (live.isEmpty()) {
            // Plus d'ailes, mais peut-etre encore des grains : ils finissent leur vie tout seuls,
            // comme chez l'original, dont le semeur s'arretait avec le contexte. Voir WingDust.
            drawDustAlone(event);
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();
        float partialTick = event.getPartialTick();
        double seconds = Util.getMillis() / 1000.0;
        // La lumiere des anneaux est celle du monde : le soleil, et sa hauteur. Voir shade.
        Sun sun = sunOf(Minecraft.getInstance().level, partialTick);

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
                drawTornado(buffer, matrix, part.tornado(), seconds, alpha, sun);
            }
        }

        BufferUploader.drawWithShader(buffer.end());

        // Et la poussiere des ailes par-dessus : elle n'a pas la meme image, donc elle a son propre
        // passage. Voir WingDust.
        drawDust(event, base, camera);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** La poussiere seule, quand les ailes sont finies : elle pose son etat elle-meme. */
    private static void drawDustAlone(RenderLevelStageEvent event) {
        if (WingDust.live().isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        drawDust(event, event.getPoseStack().last().pose(), event.getCamera().getPosition());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Les grains des ailes : des carres noirs, sans image du tout.
     *
     * <p>C'est le rendu de l'original, et il surprend : sa poussiere de bloc ne resolvait pas son
     * image, et une particule sans image se dessine en carre plein. Le port ne lui cherche donc pas
     * une plus belle texture — il prend le nuanceur de couleur, qui ne lit rien, et dessine le
     * carre tel quel. Voir WingDust.
     */
    private static void drawDust(RenderLevelStageEvent event, Matrix4f base, Vec3 camera) {
        if (WingDust.live().isEmpty()) return;

        org.joml.Vector3f left = event.getCamera().getLeftVector();
        org.joml.Vector3f up = event.getCamera().getUpVector();
        Vec3 across = new Vec3(left.x, left.y, left.z);
        Vec3 upright = new Vec3(up.x, up.y, up.z);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (WingDust.Grain grain : WingDust.live()) {
            Vec3 centre = grain.at(event.getPartialTick());
            float alpha = grain.alpha(event.getPartialTick());
            Vec3 a = across.scale(WingDust.SIZE / 2);
            Vec3 b = upright.scale(WingDust.SIZE / 2);
            dustVertex(buffer, base, camera, centre.subtract(a).subtract(b), alpha);
            dustVertex(buffer, base, camera, centre.add(a).subtract(b), alpha);
            dustVertex(buffer, base, camera, centre.add(a).add(b), alpha);
            dustVertex(buffer, base, camera, centre.subtract(a).add(b), alpha);
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void dustVertex(BufferBuilder buffer, Matrix4f base, Vec3 camera, Vec3 at,
                                   float alpha) {
        buffer.vertex(base, (float) (at.x - camera.x), (float) (at.y - camera.y),
                        (float) (at.z - camera.z))
                .color(WingDust.RED, WingDust.GREEN, WingDust.BLUE, alpha)
                .endVertex();
    }

    /** Une tornade entiere : ses anneaux, du pied au sommet. */
    private static void drawTornado(BufferBuilder buffer, Matrix4f matrix,
                                    TornadoVisuals.Tornado tornado, double seconds, float alpha,
                                    Sun sun) {
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
                float shade = shade(normal(matrix, x0, y0), sun);

                vertex(buffer, matrix, alpha, shade, x0 + dx, y0, z0 + dz, (float) u0, 0f);
                vertex(buffer, matrix, alpha, shade, x0 + dx, y1, z0 + dz, (float) u0, 1f);
                vertex(buffer, matrix, alpha, shade, x1 + dx, y1, z1 + dz, (float) u1, 1f);
                vertex(buffer, matrix, alpha, shade, x1 + dx, y0, z1 + dz, (float) u1, 0f);
            }
        }
    }

    /** Le plancher d'ombre : une face detournee du soleil n'est jamais noire, la lueur se voit encore. */
    private static final float SHADE_MIN = 0.72f;
    static final float SHADE_MAX = 1.0f;

    /** Ce que la nuit retire aux ailes : jamais eteintes, elles emettent leur propre lueur. */
    public static final double NIGHT_MIN = 0.65;

    /**
     * Le soleil du monde : de quel cote il est, et combien il donne.
     *
     * @param direction ou il se trouve, depuis le monde — un vecteur unitaire
     * @param strength la lumiere qu'il donne, de {@link #NIGHT_MIN} la nuit a 1 en plein jour
     */
    public record Sun(Vec3 direction, double strength) {
    }

    /**
     * Le soleil d'un angle donne : sa direction, et sa lumiere.
     *
     * <p>Sa direction est celle du ciel de la 1.20.1 : le ciel tourne autour de l'axe des X, donc
     * le soleil se leve et se couche dans le plan <b>X/Y</b>, et l'angle vaut zero a midi. La
     * direction se lit donc {@code (sin, cos, 0)}, et sa <b>hauteur</b> — la composante Y — est ce
     * qui eteint les ailes la nuit : c'est elle qui dit si le soleil est au-dessus de l'horizon.
     */
    public static Sun sun(double angle) {
        Vec3 direction = new Vec3(Math.sin(angle), Math.cos(angle), 0);
        return new Sun(direction, NIGHT_MIN + (1 - NIGHT_MIN) * Mth.clamp(direction.y, 0, 1));
    }

    /** Le soleil du monde ou l'on est, a cet instant. */
    private static Sun sunOf(net.minecraft.world.level.Level level, float partialTick) {
        return sun(level.getTimeOfDay(partialTick) * Math.PI * 2);
    }

    /** La normale d'un quad : celle de l'original, {@code (x0, y0, 0)}, tournee avec le quad. */
    static Vector3f normal(Matrix4f matrix, double x0, double y0) {
        return matrix.transformDirection(new Vector3f((float) x0, (float) y0, 0f));
    }

    /**
     * L'ombre d'un quad : la lumiere du monde, qui vient du <b>soleil</b>.
     *
     * <p>Cette normale est bizarre, et c'est celle de l'original : le rayon du cercle a gauche, la
     * hauteur de l'anneau au milieu, et rien a droite — {@code (x0, y0, 0)}. Elle n'a donc pas
     * grand-chose a voir avec la vraie normale du ruban, mais elle suffit a son office : le long de
     * l'axe elle fait glisser la lumiere, et comme chaque aile est inclinee de son cote, les quatre
     * en prennent une differente.
     *
     * <p>L'original, lui, laissait GL l'eclairer — c'est de la que vient cette normale —, et le
     * port avait d'abord refait cette lumiere avec une lampe posee sur l'<b>oeil</b> : c'etait le
     * seul moyen de retrouver une ombre qui bougeait. Le joueur a demande mieux — « il y a un effet
     * d'eclairage dessus, mais il est un peu trop gros et bizarre [...] j'aimerais que ce soit un
     * peu plus subtil, et que ca depende aussi du soleil en jeu / notre position par rapport au
     * soleil » —, et c'est plus juste : la lampe est le soleil, sa direction et sa hauteur. Tourner
     * sur soi-meme eclaircit et assombrit donc encore les ailes — elles tiennent au dos —, et le
     * creux d'ombre est plus doux qu'avant : une face detournee garde 72 % de sa lumiere au lieu
     * de 55.
     */
    static float shade(Vector3f normal, Sun sun) {
        float length = normal.length();
        if (length < 1e-6f) return SHADE_MAX * (float) sun.strength();

        Vector3f toSun = new Vector3f((float) sun.direction().x, (float) sun.direction().y,
                (float) sun.direction().z);
        float lambert = Math.max(0f, normal.div(length).dot(toSun));
        double lit = SHADE_MIN + (SHADE_MAX - SHADE_MIN) * lambert;
        return (float) (lit * sun.strength());
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float alpha, float shade,
                               double x, double y, double z, float u, float v) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(shade, shade, shade, alpha)
                .uv(u, v)
                .endVertex();
    }
}
