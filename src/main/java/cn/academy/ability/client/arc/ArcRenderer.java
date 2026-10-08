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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // ------------------------------------------------------------------
    // Les faisceaux
    // ------------------------------------------------------------------
    //
    // Le railgun ne dessine pas un ruban mais un CYLINDRE, et c'est l'original qui le dit : son
    // RendererRayComposite posait deux cylindres concentriques — un coeur clair et un halo
    // orange — plus un ruban large face a l'ecran par-dessus. Un ruban plat, essaye d'abord, se
    // voit toujours de face et ne tourne pas avec la vue ; un cylindre, si.
    //
    // Ils ne passent pas par ClientArcs : leur geometrie n'est pas un ruban de motif, et leur
    // vie se lit en alpha et en largeur — voir drawBeam.

    /** La texture du faisceau, celle de l'original. */
    private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/railgun.png");

    /** Dix cotes : assez pour que la section ronde se lise, et rien de plus a calculer. */
    private static final int BEAM_SIDES = 10;

    /**
     * Le coeur : 241, 240, 222, rayon 0,09 — les nombres de l'original.
     *
     * <p>C'est le PLUS PETIT des deux cylindres, et le joueur le decrit comme un laser blanc a
     * l'interieur du gros, qui prend la couleur jaune de celui-ci : « pour donner l'impression
     * que c'est la piece qui est foncee en ligne droite ». C'est exactement ca — la piece de
     * l'original, celle qu'on lance et qu'on tire, et son trait dans le vide.
     *
     * <p>Son opacite est donc pleine, et celle du halo bien plus faible : le blanc se voit a
     * travers l'orange, qui le rechauffe. Le premier essai le laissait a 150 sur 255, donc sous
     * son halo — il ne s'y lisait pas.
     *
     * <p>Le joueur a d'abord cru qu'on parlait d'un AUTRE rayon que celui-ci : il a demande
     * « un coeur encore plus petit a l'interieur de ce coeur la ». Le trait fin et blanc essaye
     * entre-temps etait donc la mauvaise reponse — ce coeur-ci reprend sa taille et sa teinte
     * d'origine, et le trait blanc devient un troisieme cylindre, encore plus petit — voir
     * INNER_RADIUS.
     */
    private static final double CORE_RADIUS = 0.09;
    private static final float[] CORE_COLOR = { 241 / 255f, 240 / 255f, 222 / 255f, 255 / 255f };

    /**
     * Le troisieme cylindre, tout au fond : le trait blanc pur du joueur.
     *
     * <p>Il est plus PETIT que le coeur — 0,035 contre 0,09 — et opaque a fond, donc il se lit
     * comme la ligne du milieu, celle qui donne l'impression que la piece fonce en ligne droite.
     * Le coeur le rechauffe de sa teinte, le halo l'adoucit, et le trait reste blanc.
     */
    private static final double INNER_RADIUS = 0.035;
    private static final float[] INNER_COLOR = { 1f, 1f, 1f, 1f };

    /**
     * Le halo : 236, 170, 93, rayon 0,13, part 60 sur 255 — les nombres de l'original.
     *
     * <p>Un premier essai l'avait descendu a 50, puis un deuxieme monte a 150 avec un orange
     * beaucoup plus dense (236, 140, 45). C'etait une erreur de diagnostic : le joueur trouvait
     * « le rayon jaune du milieu trop clair, pas assez orange », mais ce n'etait pas le halo qui
     * manquait — c'etait le RUBAN, dont les bandes orange disparaissaient sous le halo. Une fois
     * le ruban remis a sa largeur (voir GLOW_WIDTH), le halo denature n'avait plus aucune raison
     * d'etre, et le joueur l'a dit aussitot : « la couleur n'a plus rien a voir ». Il reprend
     * donc la teinte et la part de l'original.
     */
    private static final double HALO_RADIUS = 0.13;
    private static final float[] HALO_COLOR = { 236 / 255f, 170 / 255f, 93 / 255f, 60 / 255f };

    /** Les temps de l'original, en ticks : entree en matiere 150 ms, retrecissement 800, effacement 1000. */
    private static final int BEAM_BLEND_IN = 3;
    private static final int BEAM_SHRINK = 16;
    private static final int BEAM_FADE = 20;

    /**
     * La pulsation de la largeur pendant la vie du rayon, et sa periode en ticks.
     *
     * <p>Le joueur decrit exactement ce que faisait l'original : « pendant environ deux
     * secondes le laser grossit et retrecis tres rapidement, pour donner une impression de
     * mouvement ». C'etaient ses {@code widthWiggleRadius} de 0,3 et {@code maxWiggleSpeed} de
     * 0,8, mais l'amplitude a ete reduite deux fois : 0,15, puis 0,08, et le joueur a demande
     * 4 % au final.
     */
    private static final double WIGGLE_RADIUS = 0.04;
    private static final double WIGGLE_TICKS = 3.0;

    /**
     * La ligne du milieu de la texture du faisceau : blanche, donc sans effet sur la couleur.
     *
     * <p>Les cylindres de l'original ne sont PAS textures — son {@code RendererRayCylinder} les
     * dessine avec un shader sans texture, et sa couleur est une couleur pleine. Notre bande de
     * sommets demande bien une texture, alors on lui donne la ligne blanche du milieu :
     * multipliee par la couleur du sommet, elle ne la change pas. C'est ce placage qui manquait,
     * et qui etirait le degrade jaune sur toute la longueur du tir.
     */
    private static final float BEAM_FLAT_V = 0.5f;

    /**
     * La largeur du ruban de lueur, et son opacite.
     *
     * <p>C'est la SEULE partie texturee. {@code railgun.png} est une bande dont la colonne du
     * milieu est blanche et opaque, encadree de deux bandes orange qui s'effacent vers les
     * bords : le ruban n'est donc pas un halo pose AUTOUR du rayon, c'est le rayon lui-meme,
     * coeur compris — sa bande blanche doit tomber sur le coeur. C'est pour ca que l'original
     * annoncait 1,1, onze fois le rayon du coeur.
     *
     * <p>Un premier essai l'avait ramene a 0,45 pour ne pas remplir l'ecran : trop court. Ses
     * bandes orange se retrouvaient a l'INTERIEUR du halo, qui les cachait, et il ne restait
     * que la queue transparente du degrade. Le joueur l'a vu tout de suite : « on ne voit plus
     * du tout le ruban a cote, ce qui change la couleur du rayon le plus en dehors en le rendant
     * plus transparent que le vrai ». Il fait donc 0,85 : la bande orange la plus dense tombe
     * juste au bord du halo, et le degrade s'efface vers l'exterieur.
     */
    private static final double GLOW_WIDTH = 0.85;
    private static final float GLOW_ALPHA = 0.55f;

    /**
     * De combien le rayon est pousse vers l'avant.
     *
     * <p>Le joueur l'a rappele : dans l'original, le tir part de la PIECE lancee, donc de la
     * main mais un rien en avant. Le premier essai l'avait monte de 0,2 — c'etait pire : c'est
     * une avancee, pas une hauteur. Les DEUX bouts avancent, donc le rayon reste dans l'axe.
     */
    private static final double BEAM_FORWARD = 0.2;

    /** Le nombre d'anneaux de chaque bout arrondi, comme les quatre etapes de l'original. */
    private static final int CAP_STEPS = 3;

    /** Un faisceau vivant : ses deux bouts, sa naissance, sa duree, et son tireur. */
    private record Beam(double[] from, double[] to, long birth, int life, int ownerId) {}

    private static final List<Beam> BEAMS = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // Quitter un monde emporte ses eclairs : ils appartiennent au monde, pas au jeu.
            ClientArcs.clear();
            return;
        }
        // Pause ouverte, les eclairs ne scintillent plus : c'est ce que le joueur voyait derriere
        // son menu — « l'animation des eclairs tourne toujours en arriere plan ». Voir ClientPause.
        if (cn.academy.ability.client.ClientPause.frozen()) return;
        ClientArcs.tick(minecraft.level.getGameTime(), RANDOM);
        BEAMS.removeIf(beam -> minecraft.level.getGameTime() - beam.birth() >= beam.life());
    }

    /**
     * Pose le faisceau du railgun : il passe par le MOTEUR DES RAYONS, comme les mine ray.
     *
     * <p>Le joueur l'a demande ainsi, et il a raison : l'original dessinait son railgun avec le MEME
     * composeur que tous ses autres rayons — {@code RendererRayComposite} — et non avec un dessin a
     * lui. Le port, lui, s'etait fabrique celui d'a cote (trois cylindres et une lueur, voir
     * {@code drawBeam}), avec ses propres nombres ; c'est ce qui faisait que « le railgun ne ressemble
     * pas au vrai ».
     *
     * <p>Il y a donc maintenant un genre de rayon, {@code MdRayKind.RAILGUN}, avec les nombres de
     * l'original — lueur de 1,1 bloc, coeur blanc chaud de 9 cm, gaine orange de 13 — et c'est lui qui
     * le dessine, etincelles et vue comprises.
     *
     * <p>Ce qui reste ici — {@code BEAMS} et son dessin — n'est donc plus JAMAIS alimente : c'est du
     * code mort, garde le temps de la bascule, et a supprimer. Voir le cerveau du projet.
     */
    public static void spawnBeam(Vec3 from, Vec3 to, int lifeTicks, int ownerId) {
        cn.academy.ability.client.md.MdRayView.spawn(cn.academy.ability.client.md.MdRayKind.RAILGUN,
                from, to, ownerId);
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
        if (ClientArcs.live().isEmpty() && BEAMS.isEmpty()) return;

        // Les sommets se posent relativement a la camera : c'est ce que fait la pose du
        // rendu du monde, et un eclair pose en coordonnees du monde partirait a la derive
        // des qu'on s'eloigne de l'origine.
        Vec3 camera = event.getCamera().getPosition();
        org.joml.Vector3f aboveIsUp = event.getCamera().getUpVector();
        double[] above = { aboveIsUp.x, aboveIsUp.y, aboveIsUp.z };
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        // Les faisceaux d'abord : ils sont deja figes dans le monde — voir spawnBeam — et ils
        // s'ecrivent AVANT les arcs pour que ceux-ci passent par-dessus. Ils gardent le test de
        // profondeur du monde : sans lui, le joueur voyait son propre tir a travers son corps et
        // a travers les blocs, ce qu'il a signale.
        if (!BEAMS.isEmpty()) {
            VertexConsumer beams = buffers.getBuffer(arc(BEAM_TEXTURE));
            long gameTime = Minecraft.getInstance().level == null
                    ? 0 : Minecraft.getInstance().level.getGameTime();
            for (Beam beam : BEAMS) {
                drawBeam(beams, pose.last(), camera, above, beam, gameTime);
            }
            buffers.endBatch();
        }

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
     * Un faisceau : trois cylindres concentriques — le trait blanc, le coeur qui le rechauffe,
     * puis le halo orange par-dessus.
     *
     * <p>La largeur se retrecis un peu avant la fin et l'alpha suit : c'est l'animation de
     * l'original, ou le rayon diminuait avant de disparaitre.
     */
    private static void drawBeam(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                 double[] above, Beam beam, long gameTime) {
        double age = gameTime - beam.birth();
        if (age < 0) return;

        // Les deux bouts sont DEJA decales a la main et figes : voir spawnBeam.
        double[] from = beam.from();
        double[] to = beam.to();

        double[] axis = normalize(to[0] - from[0], to[1] - from[1], to[2] - from[2]);
        if (axis == null) return;

        // Deux directions perpendiculaires a l'axe : le cylindre se construit dessus. La
        // premiere vient du haut de la camera, donc la section ronde tourne avec la vue —
        // c'est ce qu'un ruban plat ne sait pas faire.
        double[] u = perpendicular(axis, above);
        double[] v = cross(axis, u);

        float alpha = fade(age, beam.life());
        if (alpha <= 0f) return;

        double size = width(age, beam.life()) * wiggle(age);
        double core = CORE_RADIUS * size;
        double inner = INNER_RADIUS * size;
        double halo = HALO_RADIUS * size;
        cylinder(out, pose, camera, from, to, u, v, core, CORE_COLOR, alpha);
        cylinder(out, pose, camera, from, to, u, v, inner, INNER_COLOR, alpha);
        cylinder(out, pose, camera, from, to, u, v, halo, HALO_COLOR, alpha);
        // Les deux bouts sont arrondis : c'est une boule tres allongee, pas un tuyau coupe.
        caps(out, pose, camera, from, to, u, v, axis, core, CORE_COLOR, alpha);
        caps(out, pose, camera, from, to, u, v, axis, inner, INNER_COLOR, alpha);
        caps(out, pose, camera, from, to, u, v, axis, halo, HALO_COLOR, alpha);
        glow(out, pose, camera, from, to, axis, alpha, size);
    }

    /** Les deux bouts arrondis d'un cylindre. */
    private static void caps(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                             double[] from, double[] to, double[] u, double[] v, double[] axis,
                             double radius, float[] color, float alpha) {
        roundedEnd(out, pose, camera, from, u, v, axis, radius, color, alpha, 1.0);
        roundedEnd(out, pose, camera, to, u, v, axis, radius, color, alpha, -1.0);
    }

    /**
     * Un bout arrondi, construit comme la « tete » de l'original.
     *
     * <p>Sa tete suivait {@code y = racine(x)} : du rayon plein au point, en anneaux. C'est ce
     * qui donne au tir son bout en ogive plutot qu'un disque plat — le joueur le decrit comme
     * une boule tres allongee, et c'est exactement ca.
     *
     * <p>{@code way} vaut 1 pour le bout de depart et -1 pour celui d'arrivee : les deux
     * s'etendent vers l'EXTERIEUR du rayon, donc il s'allonge d'un rayon de chaque cote.
     */
    private static void roundedEnd(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                   double[] end, double[] u, double[] v, double[] axis,
                                   double radius, float[] color, float alpha, double way) {
        for (int step = 0; step < CAP_STEPS; step++) {
            double t0 = step / (double) CAP_STEPS;
            double t1 = (step + 1) / (double) CAP_STEPS;
            double r0 = radius * Math.sqrt(1.0 - t0);
            double r1 = radius * Math.sqrt(1.0 - t1);

            double[] c0 = { end[0] - axis[0] * radius * t0 * way,
                            end[1] - axis[1] * radius * t0 * way,
                            end[2] - axis[2] * radius * t0 * way };
            double[] c1 = { end[0] - axis[0] * radius * t1 * way,
                            end[1] - axis[1] * radius * t1 * way,
                            end[2] - axis[2] * radius * t1 * way };

            for (int i = 0; i < BEAM_SIDES; i++) {
                double a0 = Math.PI * 2 * i / BEAM_SIDES;
                double a1 = Math.PI * 2 * (i + 1) / BEAM_SIDES;
                double[] d0 = { u[0] * Math.cos(a0) + v[0] * Math.sin(a0),
                                u[1] * Math.cos(a0) + v[1] * Math.sin(a0),
                                u[2] * Math.cos(a0) + v[2] * Math.sin(a0) };
                double[] d1 = { u[0] * Math.cos(a1) + v[0] * Math.sin(a1),
                                u[1] * Math.cos(a1) + v[1] * Math.sin(a1),
                                u[2] * Math.cos(a1) + v[2] * Math.sin(a1) };

                float at0 = i / (float) BEAM_SIDES;
                float at1 = (i + 1) / (float) BEAM_SIDES;

                beamVertex(out, pose, camera, c0, d0, r0, at0, BEAM_FLAT_V, color, alpha);
                beamVertex(out, pose, camera, c0, d1, r0, at1, BEAM_FLAT_V, color, alpha);
                beamVertex(out, pose, camera, c1, d1, r1, at1, BEAM_FLAT_V, color, alpha);
                beamVertex(out, pose, camera, c1, d0, r1, at0, BEAM_FLAT_V, color, alpha);
            }
        }
    }

    /**
     * Le ruban large qui enveloppe le faisceau : la « lueur » de l'original.
     *
     * <p>C'est la seule partie texturee. {@code railgun.png} est une bande dont le milieu est
     * blanc et les bords orange, et c'est ce degrade-la qui se plaque EN TRAVERS du ruban — le
     * long de la longueur, la texture ne varie pas, donc l'etirement ne se voit pas.
     *
     * <p>Le ruban se voit de face : sa largeur est perpendiculaire a l'axe ET au regard, donc
     * il tourne avec la camera. C'est lui qui donne au tir son epaisseur lumineuse, les trois
     * cylindres n'etant larges que de quelques centimetres.
     *
     * <p>Sa bande blanche tombe sur les cylindres, qui la cachent : c'est voulu, et c'est ce
     * que faisait l'original. Ce qui reste visible, ce sont ses bandes orange, de part et
     * d'autre du halo — le « ruban a cote » que le joueur veut revoir.
     */
    private static void glow(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                             double[] from, double[] to, double[] axis, float alpha,
                             double size) {
        double[] view = normalize(camera.x - from[0], camera.y - from[1], camera.z - from[2]);
        double[] side = view == null ? null : cross(axis, view);
        if (side == null) return;

        double half = GLOW_WIDTH / 2.0 * size;
        float[] color = { 1f, 1f, 1f, GLOW_ALPHA };

        beamVertex(out, pose, camera, from, side, half, 0f, 0f, color, alpha);
        beamVertex(out, pose, camera, from, side, -half, 0f, 1f, color, alpha);
        beamVertex(out, pose, camera, to, side, -half, 1f, 1f, color, alpha);
        beamVertex(out, pose, camera, to, side, half, 1f, 0f, color, alpha);
    }

    /** L'alpha du faisceau selon son age : entree en matiere, puis effacement. */
    private static float fade(double age, int life) {
        float alpha = 1f;
        if (age < BEAM_BLEND_IN) alpha = (float) ((age + 1) / (double) BEAM_BLEND_IN);
        double left = life - age;
        if (left < BEAM_FADE) alpha *= (float) Math.max(0.0, left / BEAM_FADE);
        return Math.max(0f, Math.min(1f, alpha));
    }

    /**
     * La largeur du rayon : pleine pendant sa vie, puis elle TOMBE a zero dans les dernieres
     * 800 millisecondes.
     *
     * <p>Le premier essai la faisait retrecir au DEBUT, a 60 pour cent : c'etait une erreur de
     * lecture de {@code widthShrinkTime}, qui se compte a partir de la FIN, comme son nom ne le
     * dit pas. Le joueur l'a vu : « a la fin du pouvoir, presque en meme temps que le son part,
     * le laser devient petit jusqu'a disparaitre ».
     */
    private static double width(double age, int life) {
        double left = life - age;
        if (left >= BEAM_SHRINK) return 1.0;
        return Math.max(0.0, left / BEAM_SHRINK);
    }

    /** La pulsation rapide : plus ou moins huit pour cent, une oscillation toutes les trois images. */
    private static double wiggle(double age) {
        return 1.0 + WIGGLE_RADIUS * Math.sin(age * Math.PI * 2.0 / WIGGLE_TICKS);
    }

    /** Un cylindre : {@code BEAM_SIDES} quadrilateres entre les deux cercles. */
    private static void cylinder(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                 double[] from, double[] to, double[] u, double[] v,
                                 double radius, float[] color, float alpha) {
        for (int i = 0; i < BEAM_SIDES; i++) {
            double a0 = Math.PI * 2 * i / BEAM_SIDES;
            double a1 = Math.PI * 2 * (i + 1) / BEAM_SIDES;
            double cos0 = Math.cos(a0);
            double sin0 = Math.sin(a0);
            double cos1 = Math.cos(a1);
            double sin1 = Math.sin(a1);

            double[] d0 = { u[0] * cos0 + v[0] * sin0, u[1] * cos0 + v[1] * sin0,
                            u[2] * cos0 + v[2] * sin0 };
            double[] d1 = { u[0] * cos1 + v[0] * sin1, u[1] * cos1 + v[1] * sin1,
                            u[2] * cos1 + v[2] * sin1 };

            float u0 = i / (float) BEAM_SIDES;
            float u1 = (i + 1) / (float) BEAM_SIDES;

            // Le long de l'axe, u varie ; en travers, v reste sur la ligne blanche du milieu —
            // voir BEAM_FLAT_V. Un cylindre n'a pas de degrade, il a une couleur.
            beamVertex(out, pose, camera, from, d0, radius, u0, BEAM_FLAT_V, color, alpha);
            beamVertex(out, pose, camera, from, d1, radius, u1, BEAM_FLAT_V, color, alpha);
            beamVertex(out, pose, camera, to, d1, radius, u1, BEAM_FLAT_V, color, alpha);
            beamVertex(out, pose, camera, to, d0, radius, u0, BEAM_FLAT_V, color, alpha);
        }
    }

    /** Un coin de faisceau : un bout, une direction de section, et son rayon. */
    private static void beamVertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                   double[] end, double[] direction, double radius,
                                   float u, float v, float[] color, float alpha) {
        double x = end[0] + direction[0] * radius;
        double y = end[1] + direction[1] * radius;
        double z = end[2] + direction[2] * radius;

        out.vertex(pose.pose(), (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z))
                .color(color[0], color[1], color[2], color[3] * alpha)
                .uv(u, v)
                .endVertex();
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
                        // ET IL N'ECRIT PAS LA PROFONDEUR, comme les effets du plasma : un eclair est
                        // une lueur, et s'il ecrit la profondeur il fait disparaitre ce qu'il croise.
                        // C'est ce que le joueur a vu sur le railgun : « les eclairs qui passent
                        // devant le railgun lui donnent de la transparence » — un arc passant devant
                        // le faisceau, donc plus pres, le rayait de la profondeur, et le faisceau
                        // n'etait plus dessine derriere lui.
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(true)));
    }
}
