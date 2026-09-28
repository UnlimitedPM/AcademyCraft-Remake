package cn.academy.ability.client.arc;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.arc.ArcMesh.Quad;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Dessine les eclairs des competences.
 *
 * <p>Portage du rendu d'{@code EntityArc} : l'original posait une entite au depart, l'orientait
 * vers la cible (lacet puis tangage), et dessinait son motif le long de l'axe. Ici, le repere
 * se construit directement : l'axe X du motif devient la direction de l'arc, et les deux
 * autres axes du motif sont poses sur les deux perpendiculaires. C'est le meme dessin, sans
 * angles d'Euler — et sans le cas ou viser droit vers le haut les rend indetermines.
 *
 * <p>Le rendu n'a plus aucune decision a prendre : un motif est une suite de quads deja
 * habilles, avec leurs quatre coins et l'ordre de la texture. Tout ce qu'il fait est un
 * changement de repere. C'est ce qui garantit que deux quads voisins, calcules par
 * {@code ArcGenerator} pour partager leur bord, le partagent encore une fois dessines.
 *
 * <p>Deux choses different des types de rendu de vanilla, et les deux sont necessaires :
 * <ul>
 *   <li>les <b>faces arriere ne sont pas ecartees</b>. Un ruban n'a qu'une face, et sans
 *       cela il disparait des qu'on le regarde de l'autre cote. L'original desactivait le
 *       meme tri ({@code glDisable(GL_CULL_FACE)}) le temps de dessiner ses eclairs ;</li>
 *   <li>un <b>seul</b> quad par ruban. Dessiner la face avant ET la face arriere au meme
 *       endroit les fait se disputer la profondeur et se melanger deux fois : l'eclair se
 *       retrouve parseme de morceaux plus clairs que d'autres. C'est exactement ce que
 *       l'original ne faisait pas.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class ArcRenderer {

    /** La texture d'un bout de trait : celle de l'original, inchangee. */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/arc/line_segment.png");

    /** Un type de rendu par texture : le construire a chaque image allouerait pour rien. */
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

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
        VertexConsumer out = buffers.getBuffer(arc(TEXTURE));

        for (ClientArcs.LiveArc arc : ClientArcs.live()) {
            if (arc.visible()) {
                draw(out, pose.last().pose(), camera, arc);
            }
        }
        buffers.endBatch();
    }

    /**
     * Un eclair entier : chacun de ses rubans, pose entre les deux points vises.
     *
     * <p>Le repere est construit pour que le plan du motif regarde la camera : l'eclair est
     * une surface plate, et un plan vu de profil disparaitrait. Le repere ne change pas
     * d'un segment a l'autre — c'est ce qui evite le vrillage, ou un ruban tourne vers
     * l'oeil et son voisin de travers.
     */
    private static void draw(VertexConsumer out, Matrix4f matrix, Vec3 camera, ClientArcs.LiveArc arc) {
        double[] from = arc.from();
        double[] to = arc.to();

        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1e-6) return;

        double[] axis = { dx / length, dy / length, dz / length };
        // Perpendiculaire a l'axe et tournee vers l'oeil : le plan du motif lui fait face.
        double[] facing = normalize(camera.x - from[0], camera.y - from[1], camera.z - from[2]);
        double[] side = cross(axis, facing);
        if (length(side) < 1e-4) {
            // L'arc vise droit dans l'oeil : il n'y a plus de direction privilegiee, et un
            // repere quelconque vaut mieux qu'un repere nul.
            side = perpendicular(axis);
        } else {
            side = normalize(side[0], side[1], side[2]);
        }
        double[] up = cross(axis, side);

        for (Quad quad : arc.mesh().quads()) {
            // Une seule face : le tri des faces arriere est desactive par le type de rendu,
            // donc ce quad se voit des deux cotes.
            vertex(out, matrix, camera, from, axis, side, up, quad, quad.ax(), quad.ay(), quad.az(), 0f, 0f);
            vertex(out, matrix, camera, from, axis, side, up, quad, quad.bx(), quad.by(), quad.bz(), 0f, 1f);
            vertex(out, matrix, camera, from, axis, side, up, quad, quad.cx(), quad.cy(), quad.cz(), 1f, 1f);
            vertex(out, matrix, camera, from, axis, side, up, quad, quad.dx(), quad.dy(), quad.dz(), 1f, 0f);
        }
    }

    /** Un coin de ruban : du repere du motif a celui du monde, puis sous la camera. */
    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 camera,
                               double[] from, double[] axis, double[] side, double[] up,
                               Quad quad, double x, double y, double z, float u, float v) {
        double worldX = from[0] + axis[0] * x + side[0] * y + up[0] * z;
        double worldY = from[1] + axis[1] * x + side[1] * y + up[1] * z;
        double worldZ = from[2] + axis[2] * x + side[2] * y + up[2] * z;

        // La normale du ruban : le format de sommet des entites l'exige, et sans elle
        // Minecraft refuse le sommet et fait tomber le jeu des la premiere image
        // ("Not filled all elements of the vertex"). L'eclairage ne s'en sert pas — l'arc
        // est dessine en pleine lumiere — mais elle doit etre la.
        //
        // Elle est celle du plan du motif, donc celle de toute la surface : un ruban n'a
        // qu'une face, et lui donner une normale par coin ne changerait rien a l'ecran.
        double[] normal = cross(axis, side);

        out.vertex(matrix,
                        (float) (worldX - camera.x),
                        (float) (worldY - camera.y),
                        (float) (worldZ - camera.z))
                .color(1f, 1f, 1f, (float) quad.alpha())
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                // L'arc eclaire : pleine lumiere, comme l'original, qui le dessinait sans
                // jamais interroger la lumiere du monde.
                .uv2(LightTexture.FULL_BRIGHT)
                .normal((float) normal[0], (float) normal[1], (float) normal[2])
                .endVertex();
    }

    /**
     * Le type de rendu des eclairs : translucide, sans tri des faces arriere, et assombri
     * par rien.
     *
     * <p>Les constantes de vanilla sont protegees, mais les constructeurs de ses morceaux ne
     * le sont pas : on rebatit donc le meme etat, avec les deux differences qui comptent ici.
     */
    private static RenderType arc(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, tex -> RenderType.create("academy_arc",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(new RenderStateShard.TransparencyStateShard("academy_arc",
                                () -> {
                                    RenderSystem.enableBlend();
                                    RenderSystem.defaultBlendFunc();
                                },
                                () -> RenderSystem.disableBlend()))
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .setLightmapState(new RenderStateShard.LightmapStateShard(false))
                        .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                        .createCompositeState(true)));
    }

    private static double[] perpendicular(double[] axis) {
        double[] reference = Math.abs(axis[1]) > 0.9 ? new double[] { 1, 0, 0 } : new double[] { 0, 1, 0 };
        double[] side = cross(axis, reference);
        return normalize(side[0], side[1], side[2]);
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0] };
    }

    private static double[] normalize(double x, double y, double z) {
        double size = Math.sqrt(x * x + y * y + z * z);
        if (size < 1e-9) return new double[] { 0, 1, 0 };
        return new double[] { x / size, y / size, z / size };
    }

    private static double length(double[] vector) {
        return Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
    }
}
