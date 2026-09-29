package cn.academy.ability.client.arc;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.arc.ArcMesh.Quad;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
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
 * se construit directement dans {@link ArcFrame} : l'axe X du motif devient la direction de
 * l'arc, et les deux autres directions du motif sont posees sur celles de l'ecran. C'est le
 * meme dessin, sans angles d'Euler — et sans le cas ou viser droit vers le haut les rend
 * indetermines.
 *
 * <p>Le rendu n'a plus aucune decision a prendre : un motif est une suite de quads deja
 * habilles, avec leurs quatre coins et l'ordre de la texture. Tout ce qu'il fait est un
 * changement de repere. C'est ce qui garantit que deux quads voisins, calcules par
 * {@code ArcGenerator} pour partager leur bord, le partagent encore une fois dessines.
 *
 * <p>Trois choses different des types de rendu de vanilla, et les trois sont necessaires :
 * <ul>
 *   <li>les <b>faces arriere ne sont pas ecartees</b>. Un ruban n'a qu'une face, et sans
 *       cela il disparait des qu'on le regarde de l'autre cote. L'original desactivait le
 *       meme tri ({@code glDisable(GL_CULL_FACE)}) le temps de dessiner ses eclairs ;</li>
 *   <li>un <b>seul</b> quad par ruban. Dessiner la face avant ET la face arriere au meme
 *       endroit les fait se disputer la profondeur et se melanger deux fois : l'eclair se
 *       retrouve parseme de morceaux plus clairs que d'autres ;</li>
 *   <li>l'eclair <b>se melange normalement</b>, comme l'original ({@code SRC_ALPHA},
 *       {@code ONE_MINUS_SRC_ALPHA}), et non en ajoutant sa lumiere. Ajouter avait ete tente
 *       pour eclaircir l'arc : sur une bande de texture au degrade doux, cela fait surtout
 *       briller ses bords presque transparents, donc l'eclair s'epaissit. Le trait parait
 *       alors plus gros que celui de l'original, ce qui se voit au premier coup d'oeil.</li>
 * </ul>
 *
 * <p>Et l'eclair <b>n'est pas eclaire du tout</b>. C'est ce que faisait l'original, qui
 * desactivait purement et simplement l'eclairage ({@code glDisable(GL_LIGHTING)}) le temps de
 * dessiner ses arcs, et ce n'est pas un detail : le shader des entites eclaircit et assombrit
 * ses sommets selon leur normale, par {@code 0,4 + 0,6 x max(dot(normale, lumiere), 0)} — un
 * eclair y vaut donc au mieux 40 % de sa couleur, et bien moins selon l'orientation. Un arc
 * qui emet sa lumiere n'a rien a faire d'une normale, d'une lumiere ni d'une superposition.
 *
 * <p>Le programme de la <b>balise de phare</b> dit exactement cela, et c'est celui qui est
 * employe ici : une bande texturee, teintee par la couleur du sommet, transparente et
 * eclairante. Il n'y a pas de programme plus juste dans la 1.20.1 pour ce dessin-la. Celui de
 * {@code position_color_tex} a ete essaye d'abord, et n'affichait rien du tout — d'ou le
 * format de sommet, qui ne porte plus que la position, la couleur et la texture.
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
    public static void spawn(String pattern, Vec3 from, Vec3 to, int lifeTicks, boolean lengthFixed,
                             int ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0 : minecraft.level.getGameTime();

        ClientArcs.spawn(ArcPattern.byName(pattern),
                new double[] { from.x, from.y, from.z },
                new double[] { to.x, to.y, to.z },
                lifeTicks, lengthFixed, ownerId, gameTime, RANDOM);
    }

    /**
     * Le meme eclair, deplace sur sa cible : voir {@link ClientArcs#sustain}.
     *
     * <p>A appeler a chaque tick d'un maintien, au lieu de re-poser un eclair neuf : c'est ce
     * qui le fait suivre le regard sans jamais en poser un deuxieme.
     */
    public static void sustain(String pattern, Vec3 from, Vec3 to, int lifeTicks, int ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0 : minecraft.level.getGameTime();

        ClientArcs.sustain(ArcPattern.byName(pattern),
                new double[] { from.x, from.y, from.z },
                new double[] { to.x, to.y, to.z },
                lifeTicks, ownerId, gameTime, RANDOM);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ClientArcs.live().isEmpty()) return;

        // Les sommets se posent relativement a la camera : c'est ce que fait la pose du
        // rendu du monde, et un eclair pose en coordonnees du monde partirait a la derive
        // des qu'on s'eloigne de l'origine.
        Vec3 camera = event.getCamera().getPosition();
        org.joml.Vector3f aboveIsUp = event.getCamera().getUpVector();
        double[] above = { aboveIsUp.x, aboveIsUp.y, aboveIsUp.z };
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(arc(TEXTURE));

        // Quel decalage de vue s'applique : celui de la vue interne, et seulement pour l'eclair
        // du tireur lui-meme — c'est la condition de l'original, « thirdPersonView == 0 &&
        // clientPlayer == entity.getPlayer() ». Tout le reste est pose sur la main : la sienne
        // vue de l'exterieur, comme celle des autres joueurs.
        Minecraft minecraft = Minecraft.getInstance();
        int ownId = minecraft.player == null ? -1 : minecraft.player.getId();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();

        for (ClientArcs.LiveArc arc : ClientArcs.live()) {
            if (arc.visible()) {
                draw(out, pose.last(), camera, above, arc,
                        firstPerson && arc.ownerId() == ownId);
            }
        }
        buffers.endBatch();
    }

    /**
     * Un eclair entier : chacun de ses rubans, pose entre les deux points vises.
     *
     * <p>Le repere vient de {@link ArcFrame}, ou il est verifie : trois directions unitaires
     * qui se coupent a angle droit. C'est la que s'etait glissee la faute qui faisait partir
     * l'eclair de travers — deux fois la meme direction au lieu de deux perpendiculaires.
     *
     * <p>L'eclair se pose dans le repere de la main de son tireur ({@link ArcView}), avec le
     * decalage de l'original : celui de la vue interne pour l'eclair du tireur dans sa propre
     * vue, et celui de la main pour tout le reste.
     */
    private static void draw(VertexConsumer out, PoseStack.Pose pose, Vec3 camera, double[] above,
                             ClientArcs.LiveArc arc, boolean ownFirstPerson) {
        double[][] fixed = ArcView.fix(arc.from(), arc.to(), above,
                ownFirstPerson ? ArcView.FIRST_PERSON : ArcView.THIRD_PERSON);
        double[] from = fixed[0];
        double[] to = fixed[1];

        ArcFrame frame = ArcFrame.between(from, to, above);
        if (frame == null) return;

        for (Quad quad : arc.mesh().quads()) {
            // Une seule face : le tri des faces arriere est desactive par le type de rendu,
            // donc ce quad se voit des deux cotes.
            vertex(out, pose, camera, frame, from, quad.ax(), quad.ay(), quad.az(), 0f, 0f, quad.alpha());
            vertex(out, pose, camera, frame, from, quad.bx(), quad.by(), quad.bz(), 0f, 1f, quad.alpha());
            vertex(out, pose, camera, frame, from, quad.cx(), quad.cy(), quad.cz(), 1f, 1f, quad.alpha());
            vertex(out, pose, camera, frame, from, quad.dx(), quad.dy(), quad.dz(), 1f, 0f, quad.alpha());
        }
    }

    /** Un coin de ruban : du repere du motif a celui du monde, puis sous la camera. */
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                               ArcFrame frame, double[] from,
                               double x, double y, double z, float u, float v, double alpha) {
        double[] world = frame.point(from, x, y, z);

        // Trois attributs : position, couleur, texture. Ni normale, ni lumiere du monde, ni
        // superposition — un eclair emet sa lumiere, il n'en recoit pas, et le programme de la
        // balise ne demande rien de plus.
        out.vertex(pose.pose(),
                        (float) (world[0] - camera.x),
                        (float) (world[1] - camera.y),
                        (float) (world[2] - camera.z))
                .color(1f, 1f, 1f, (float) alpha)
                .uv(u, v)
                .endVertex();
    }

    /**
     * Le type de rendu des eclairs : une bande texturee sans eclairage, transparente, et sans
     * tri des faces arriere.
     *
     * <p>Les constantes de vanilla sont protegees, mais les constructeurs de ses morceaux ne
     * le sont pas : on rebatit donc le meme etat, avec ce qui compte ici — le programme de la
     * balise, qui ne connait ni normale ni lumiere.
     */
    private static RenderType arc(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, tex -> RenderType.create("academy_arc",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeBeaconBeamShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        // Le melange de l'original, mot pour mot : SRC_ALPHA / ONE_MINUS_SRC_ALPHA.
                        .setTransparencyState(new RenderStateShard.TransparencyStateShard("academy_arc",
                                () -> {
                                    RenderSystem.enableBlend();
                                    RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                                },
                                () -> {
                                    RenderSystem.disableBlend();
                                    RenderSystem.defaultBlendFunc();
                                }))
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(true)));
    }
}
