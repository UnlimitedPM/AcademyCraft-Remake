package cn.academy.client;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;

/**
 * La piece du railgun dans la main, portage du {@code TileEntityItemStackRenderer} de l'original.
 *
 * <h2>Pourquoi du code, et pas un modele</h2>
 *
 * <p>Un modele JSON ne sait dessiner que des boites : sa tranche est forcement faite de quatre
 * bandes DROITES, et le joueur y a vu « un bord carre » autour d'une piece ronde — deux fois. Il ne
 * sait pas non plus montrer autre chose dans l'inventaire que ce qu'il montre en main, et c'est
 * exactement ce qu'il a demande : « dans l'inventaire, je veux juste l'image de la piece telle
 * quelle ».
 *
 * <p>L'original avait la meme contrainte, et la meme reponse : son {@code ItemCoin} remplacait le
 * rendu de l'objet par un TEISR — du code — qui dessinait une plaque fine avec sa bordure. C'est ce
 * que fait cette classe, avec le moyen d'aujourd'hui ({@code BlockEntityWithoutLevelRenderer}).
 *
 * <h2>Deux dessins, selon l'endroit</h2>
 *
 * <ul>
 *   <li><b>L'inventaire, l'objet pose, l'objet fixe</b> : l'IMAGE, telle quelle — le meme carre plat
 *       que les objets de vanilla, avec ses deux faces dans le bon sens. Rien qu'elle.</li>
 *   <li><b>En main</b> : la piece en volume — son blason devant, un peu plus mat, son disque uni
 *       derriere, et une TRAN CHE faite d'un anneau de facettes, donc ronde, et qui prend ses couleurs
 *       sur le bord de l'image.</li>
 * </ul>
 *
 * <p>Le repere est celui que Forge donne a un rendu d'objet : le cube du modele, de un bloc de cote,
 * centre sur l'origine (une unite = 1/16 de bloc dans un modele JSON, un bloc ici). Les echelles de
 * main vivent dans {@code coin.json}, et c'est lui qui les pose avant d'arriver ici.
 */
@OnlyIn(Dist.CLIENT)
public class CoinItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/item/coin_front.png");
    private static final ResourceLocation BACK = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/item/coin_back.png");

    /** Le rayon du disque : la piece remplit le cube du modele, ses coins sont transparents. */
    private static final float RADIUS = 0.5f;

    /**
     * Sa tranche : deux centiemes de bloc de chaque cote, donc quatre pour cent de sa largeur.
     *
     * <p>Le joueur a trouve la piece « trop epaisse » a huit pour cent — c'est pourtant le {@code w}
     * de l'original — donc la tranche est deux fois plus fine que lui. Elle est prise sur le BORD de
     * l'image, et c'est elle qui donne a la piece sa matiere.
     */
    private static final float HALF_THICKNESS = 0.02f;

    /**
     * La teinte du recto : quatre-vingt-cinq pour cent du blanc.
     *
     * <p>C'est ce que le joueur a demande — « dans le vrai mod la piece a l'air plus sombre » — et
     * c'est la couleur des sommets qui la donne : la lumiere, elle, reste celle du monde, et elle est
     * juste. Le verso, lui, garde toute sa couleur : le joueur l'a valide tel quel.
     */
    private static final float RECT_SHADE = 0.85f;

    /**
     * Les facettes de la tranche : quarante-huit, deux par pixel de son bord.
     *
     * <p>Le bord du dessin fait le tour de la piece en une centaine de pixels : a quarante-huit
     * facettes, chacune en couvre deux, et la silhouette suit l'escalier du dessin au lieu de le
     * lisser.
     */
    private static final int RIM_SEGMENTS = 48;

    /** Le plan de l'image, dans le repere du modele : celui des objets plats de vanilla (7,5 sur 16). */
    private static final float SPRITE_Z = 7.5f / 16.0f - 0.5f;

    /** L'image de la piece fait trente-deux pixels de cote, et son anneau touche le bord. */
    private static final float TEXELS = 32.0f;

    /** Le rayon de cet anneau dans l'image, un demi-pixel en dedans du bord. */
    private static final float RING_RADIUS = TEXELS / 2.0f - 0.5f;

    /**
     * Le rayon de secours de la tranche : le bord du DESSIN, un demi-pixel en dedans du carre.
     *
     * <p>C'est aussi celui de l'anneau dont les facettes tirent leur couleur ({@link #RING_RADIUS}),
     * mais la hauteur reelle de la tranche est relevee dans l'image — voir {@link #edgeRadii}. Cette
     * valeur-la ne sert que si l'image ne se lit pas.
     */
    private static final float RIM_RADIUS = RADIUS * RING_RADIUS / (TEXELS / 2.0f);

    /**
     * Le rayon du bord du DESSIN, facette par facette, mesure dans l'image une fois pour toutes.
     *
     * <p>Nul tant qu'il n'a pas ete mesure — voir {@link #edgeRadii}.
     */
    private static float[] edgeRadii;

    public CoinItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        // LE CUBE DU MODELE A SON COIN SUR L'ORIGINE, et c'est a nous de nous y placer.
        //
        // C'est le piege de ce rendu, et le joueur l'a vu tout de suite : une image d'objet fabriquee
        // par Minecraft recoit un translate(-0,5, -0,5, -0,5) qui la recentre sur le cube, mais un
        // rendu en code n'y a PAS droit — c'est a lui de se decaler, exactement comme le font ceux de
        // vanilla (le coffre, la shulker). Sans ce demi-cube, la piece partait en haut a gauche de la
        // case (« la piece n'est absolument plus centree ») et sortait de la main.
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);

        // A plat, ou en volume ? L'image pour tout ce qui se regarde de face (l'inventaire, l'objet
        // pose, l'objet fixe), et la piece pour tout ce qui se tient (les deux mains, la tete).
        boolean flat = context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED
                || context == ItemDisplayContext.GROUND;
        if (flat) {
            sprite(pose, buffers, light, overlay);
        } else {
            coin(pose, buffers, light, overlay);
        }

        pose.popPose();
    }

    // --- L'IMAGE, TELLE QUELLE ---

    /**
     * L'image de la piece : un carre plat, faces avant et arriere.
     *
     * <p>Les deux faces sont au MEME endroit, avec chacune son sens de lecture : c'est exactement ce
     * que fait vanilla pour un objet plat ({@code ItemModelGenerator} pose une face sud et une face
     * nord superposees), donc l'image ne se lit jamais a l'envers. Le type de rendu cache les faces
     * arriere : une seule des deux se dessine, selon l'ou on regarde.
     *
     * <p>ET ELLE SE LIT EN MIROIR — voir le miroir sur X explique a {@link #coin}.
     */
    private static void sprite(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.itemEntityTranslucentCull(FRONT));

        // Face avant, le miroir applique : l'abscisse part de un, l'ordonnee de zero.
        vertex(consumer, pose, light, overlay, -RADIUS, RADIUS, SPRITE_Z, 1f, 0f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, -RADIUS, -RADIUS, SPRITE_Z, 1f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, RADIUS, -RADIUS, SPRITE_Z, 0f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, RADIUS, RADIUS, SPRITE_Z, 0f, 0f, 0f, 0f, 1f);

        // Et le revers, retourne de la meme facon : vu de derriere, le miroir se compose avec la
        // symetrie du regard, donc l'image se lit juste des deux cotes.
        vertex(consumer, pose, light, overlay, RADIUS, RADIUS, SPRITE_Z, 1f, 0f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, RADIUS, -RADIUS, SPRITE_Z, 1f, 1f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, -RADIUS, -RADIUS, SPRITE_Z, 0f, 1f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, -RADIUS, RADIUS, SPRITE_Z, 0f, 0f, 0f, 0f, -1f);
    }

    // --- LA PIECE, EN VOLUME ---

    /**
     * La piece de biais : le blason devant, le disque uni derriere, et sa tranche ronde.
     *
     * <p>LE MIROIR SUR X. L'image se lit retournee de gauche a droite, et rien d'autre : le joueur a
     * demande deux fois « dans le sens inverse », puis a precise — « je voulais qu'elle ne soit
     * inversee que sur l'axe X, pas sur Z aussi ». C'est aussi ce que fait l'original, dont l'abscisse
     * part de un quand l'ordonnee part de zero. L'inventaire et la main montrent la meme face du
     * modele, donc la meme image dans le meme sens.
     *
     * <p>La tranche est un <b>anneau de facettes</b>, et chacune prend sa couleur sur le BORD de
     * l'image, a l'angle ou elle se trouve — c'est ce qui fait qu'elle suit l'anneau de la piece au
     * lieu de dessiner un carre. Le joueur avait vu ce carre deux fois : il venait des quatre bandes
     * droites d'un modele JSON.
     */
    private static void coin(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        // Le blason, sur la face que le joueur a devant lui, retourne de gauche a droite, et un peu
        // plus mat que le reste. SANS CULL, comme l'autre face : aucune des deux ne peut plus
        // disparaitre selon le sens dans lequel on la regarde.
        VertexConsumer front = buffers.getBuffer(RenderType.entityCutoutNoCull(FRONT));
        shaded(front, pose, light, overlay, -RADIUS, RADIUS, HALF_THICKNESS, 1f, 0f, 0f, 0f, 1f);
        shaded(front, pose, light, overlay, -RADIUS, -RADIUS, HALF_THICKNESS, 1f, 1f, 0f, 0f, 1f);
        shaded(front, pose, light, overlay, RADIUS, -RADIUS, HALF_THICKNESS, 0f, 1f, 0f, 0f, 1f);
        shaded(front, pose, light, overlay, RADIUS, RADIUS, HALF_THICKNESS, 0f, 0f, 0f, 0f, 1f);

        // Et le disque uni derriere, avec le meme miroir : c'est le verso de la piece.
        VertexConsumer back = buffers.getBuffer(RenderType.entityCutoutNoCull(BACK));
        vertex(back, pose, light, overlay, RADIUS, RADIUS, -HALF_THICKNESS, 1f, 0f, 0f, 0f, -1f);
        vertex(back, pose, light, overlay, RADIUS, -RADIUS, -HALF_THICKNESS, 1f, 1f, 0f, 0f, -1f);
        vertex(back, pose, light, overlay, -RADIUS, -RADIUS, -HALF_THICKNESS, 0f, 1f, 0f, 0f, -1f);
        vertex(back, pose, light, overlay, -RADIUS, RADIUS, -HALF_THICKNESS, 0f, 0f, 0f, 0f, -1f);

        // La tranche : sans cull, donc le sens d'enroulement n'a pas d'importance. Chaque facette
        // monte a la hauteur du DESSIN a son angle — voir edgeRadii — et prend la couleur d'un pixel
        // entier, sans melange.
        float[] radii = edgeRadii();
        VertexConsumer rim = buffers.getBuffer(
                RenderType.entityCutoutNoCull(FRONT));
        for (int i = 0; i < RIM_SEGMENTS; i++) {
            int next = (i + 1) % RIM_SEGMENTS;
            double a0 = Math.PI * 2.0 * i / RIM_SEGMENTS;
            double a1 = Math.PI * 2.0 * next / RIM_SEGMENTS;
            float x0 = (float) (Math.cos(a0) * radii[i]);
            float y0 = (float) (Math.sin(a0) * radii[i]);
            float x1 = (float) (Math.cos(a1) * radii[next]);
            float y1 = (float) (Math.sin(a1) * radii[next]);
            float u0 = texelU(a0);
            float v0 = texelV(a0);
            float u1 = texelU(a1);
            float v1 = texelV(a1);

            // La normale pointe vers l'exterieur : c'est ce qui donne son eclairage a la tranche.
            float nx = (float) Math.cos((a0 + a1) / 2.0);
            float ny = (float) Math.sin((a0 + a1) / 2.0);

            vertex(rim, pose, light, overlay, x0, y0, HALF_THICKNESS, u0, v0, nx, ny, 0f);
            vertex(rim, pose, light, overlay, x1, y1, HALF_THICKNESS, u1, v1, nx, ny, 0f);
            vertex(rim, pose, light, overlay, x1, y1, -HALF_THICKNESS, u1, v1, nx, ny, 0f);
            vertex(rim, pose, light, overlay, x0, y0, -HALF_THICKNESS, u0, v0, nx, ny, 0f);
        }
    }

    /** L'abscisse, dans l'image, du bord de l'anneau a cet angle — MIROIR sur X, comme les faces. */
    private static float texelU(double angle) {
        return snap((TEXELS / 2.0f - (float) (Math.cos(angle) * RING_RADIUS)) / TEXELS);
    }

    /** Et son ordonnee, dans le sens du dessin : l'image a son zero EN HAUT, le modele aussi. */
    private static float texelV(double angle) {
        return snap((TEXELS / 2.0f - (float) (Math.sin(angle) * RING_RADIUS)) / TEXELS);
    }

    /**
     * Le CENTRE du pixel qui contient cette abscisse.
     *
     * <p>C'est ce qui donne a la tranche son air de mosaique : chaque facette prend la couleur d'un
     * pixel entier, sans melange, comme le dessin qui l'entoure — « un rond fait de carres ».
     */
    private static float snap(float uv) {
        return (float) ((Math.floor(uv * TEXELS) + 0.5) / TEXELS);
    }

    /**
     * Le rayon du bord du DESSIN, facette par facette — et c'est la demande du joueur : « les bords
     * sont en forme de rond alors que la piece c'est un rond oui, mais fait de carres ».
     *
     * <p>Le dessin de la piece est un cercle de PIXELS : son bord est donc un escalier, avec les
     * petites billes de la bordure. Une tranche posee sur un cercle parfait lissait tout cela ;
     * celle-ci se pose sur le bord reel, releve dans l'image une fois pour toutes.
     */
    private static float[] edgeRadii() {
        if (edgeRadii == null) {
            edgeRadii = measureEdge();
        }
        return edgeRadii;
    }

    /** Releve le bord de l'image dans tous les sens, et retombe sur le cercle du dessin si elle manque. */
    private static float[] measureEdge() {
        float[] radii = new float[RIM_SEGMENTS];
        try (var stream = Minecraft.getInstance().getResourceManager().getResource(FRONT)
                .orElseThrow().open();
             var image = NativeImage.read(stream)) {
            for (int i = 0; i < RIM_SEGMENTS; i++) {
                radii[i] = measureEdgeAt(image, Math.PI * 2.0 * i / RIM_SEGMENTS);
            }
        } catch (Exception e) {
            // Une image illisible ne doit pas empecher la piece d'exister : on revient au cercle.
            Arrays.fill(radii, RIM_RADIUS);
        }
        return radii;
    }

    /** Le dernier pixel OPAQUE rencontre en partant du centre, dans ce sens-la. */
    private static float measureEdgeAt(NativeImage image, double angle) {
        float half = TEXELS / 2.0f;
        float radius = RIM_RADIUS;
        for (float step = 1.0f; step < half; step += 1.0f) {
            int px = (int) Math.floor(half - Math.cos(angle) * step);
            int py = (int) Math.floor(half - Math.sin(angle) * step);
            if (px < 0 || py < 0 || px >= TEXELS || py >= TEXELS) break;
            if ((image.getPixelRGBA(px, py) >>> 24) == 0) break;
            radius = RADIUS * step / half;
        }
        return radius;
    }

    // --- LES SOMMETS ---

    /** Un sommet du format des objets : position, couleur, image, ecran, lumiere et normale. */
    private static void vertex(VertexConsumer consumer, PoseStack pose, int light, int overlay,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        vertex(consumer, pose, light, overlay, 255, x, y, z, u, v, nx, ny, nz);
    }

    /**
     * Un sommet teinte, pour le blason : c'est ce qui le rend plus mat que le reste.
     *
     * <p>La teinte est une couleur de sommet, donc elle ne touche pas a la lumiere : la piece reste
     * eclairee par le monde comme les autres objets, elle est simplement un peu plus grise.
     */
    private static void shaded(VertexConsumer consumer, PoseStack pose, int light, int overlay,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        vertex(consumer, pose, light, overlay, Math.round(RECT_SHADE * 255f),
                x, y, z, u, v, nx, ny, nz);
    }

    /** Et le sommet lui-meme, avec la couleur demandee. */
    private static void vertex(VertexConsumer consumer, PoseStack pose, int light, int overlay,
                               int color, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        consumer.vertex(pose.last().pose(), x, y, z)
                .color(color, color, color, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.last().normal(), nx, ny, nz)
                .endVertex();
    }
}
