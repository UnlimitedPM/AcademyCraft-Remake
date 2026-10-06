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
 * <p>Et le dessin lui-meme suit les memes regles que les autres effets du port : <b>aucun
 * eclairage de la scene</b> — une tornade de plasma emet la sienne —, aucune face cachee (un ruban
 * n'a qu'une face), et <b>pas d'ecriture de profondeur</b> : les anneaux se croisent en permanence,
 * et s'ils ecrivaient la profondeur ils se decouperaient les uns les autres au hasard.
 *
 * <p>Reste l'ombre, prise a la main : chaque quad porte la lumiere <b>standard du jeu</b> — deux
 * lampes opposees posees dans le repere de l'oeil, et leur ambiance. C'est celle que l'original
 * laissait faire. Voir {@link #shade}.
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
                drawTornado(buffer, matrix, part.tornado(), seconds, alpha);
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
                                    TornadoVisuals.Tornado tornado, double seconds, float alpha) {
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
                float shade = shade(normal(matrix, x0, y0));

                vertex(buffer, matrix, alpha, shade, x0 + dx, y0, z0 + dz, (float) u0, 0f);
                vertex(buffer, matrix, alpha, shade, x0 + dx, y1, z0 + dz, (float) u0, 1f);
                vertex(buffer, matrix, alpha, shade, x1 + dx, y1, z1 + dz, (float) u1, 1f);
                vertex(buffer, matrix, alpha, shade, x1 + dx, y0, z1 + dz, (float) u1, 0f);
            }
        }
    }

    /**
     * La lumiere standard du jeu, celle que l'original laissait faire.
     *
     * <p>Elle vient des objets tenus en main, et c'est la MEME pour les entites en 1.12.2 : deux
     * lampes <b>exactement opposees</b> de 0,8, posees en (0,2 ; 0,2 ; -1) dans le repere de
     * l'<b>oeil</b>, plus une ambiance de 0,4. Comme la facture de l'original ne coupait pas
     * l'eclairage, c'est elle qui dessinait ses rubans — et c'est pour cela que leur ombre bouge
     * quand on tourne la tete, ce que le joueur avait remarque.
     */
    public static final float AMBIENT = 0.4f;
    public static final float DIFFUSE = 0.8f;

    /** La position de la premiere lampe, telle que le jeu la pose — la seconde est son opposee. */
    public static final double LIGHT_X = 0.2;
    public static final double LIGHT_Y = 0.2;
    public static final double LIGHT_Z = -1.0;

    /** La normale d'un quad : celle de l'original, {@code (x0, y0, 0)}, tournee avec le quad. */
    static Vector3f normal(Matrix4f matrix, double x0, double y0) {
        return matrix.transformDirection(new Vector3f((float) x0, (float) y0, 0f));
    }

    /**
     * L'ombre d'un quad : la lumiere standard du jeu, telle quelle.
     *
     * <p>Cette normale est bizarre, et c'est celle de l'original : le rayon du cercle a gauche, la
     * hauteur de l'anneau au milieu, et rien a droite — {@code glNormal3d(x0, y0, 0)}. Radiale, en
     * fait, comme celle d'un cylindre, et c'est ce qui suffit : le long de l'axe elle fait glisser
     * la lumiere, et comme chaque aile est inclinee de son cote, les quatre en prennent une
     * differente.
     *
     * <p>La normale arrive <b>deja dans le repere de l'oeil</b> : la matrice du port est celle de
     * la camera, il n'y a donc rien a reprojeter, et la lampe s'y lit directement. Les deux lampes
     * etant opposees, la plus eclairante des deux vaut la valeur <b>absolue</b> du cosinus : une
     * face qui regarde l'oeil et une face qui lui tourne le dos prennent donc <b>la meme</b>
     * lumiere — c'est ce qui separe la lumiere du jeu d'une lampe unique, et ce qui manquait au
     * premier portage. Le tout sature a 1 des que la face regarde une des deux lampes, et le creux
     * ne descend jamais sous l'ambiance, 0,4.
     */
    static float shade(Vector3f normal) {
        float length = normal.length();
        if (length < 1e-6f) return 1f;

        double light = Math.sqrt(LIGHT_X * LIGHT_X + LIGHT_Y * LIGHT_Y + LIGHT_Z * LIGHT_Z);
        double cos = (normal.x * LIGHT_X + normal.y * LIGHT_Y + normal.z * LIGHT_Z)
                / (length * light);
        double lambert = Math.min(1.0, Math.abs(cos));
        return (float) Math.min(1.0, AMBIENT + DIFFUSE * lambert);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float alpha, float shade,
                               double x, double y, double z, float u, float v) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(shade, shade, shade, alpha)
                .uv(u, v)
                .endVertex();
    }
}
