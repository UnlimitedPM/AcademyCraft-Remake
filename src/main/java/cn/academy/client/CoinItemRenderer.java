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
     * La teinte du recto : soixante-quinze pour cent du blanc.
     *
     * <p>C'est ce que le joueur a demande — « dans le vrai mod la piece a l'air plus sombre » — et
     * c'est la couleur des sommets qui la donne : la lumiere, elle, reste celle du monde, et elle est
     * juste. Premier reglage a 0,85, encore « un peu trop clair », donc 0,75. Le verso, lui, garde
     * toute sa couleur : c'est le seul point que le joueur a valide du premier coup.
     */
    private static final float RECT_SHADE = 0.75f;

    /**
     * Les pixels du dessin, lus une fois pour toutes : c'est eux qui disent ou passe le bord.
     *
     * <p>Nul tant qu'ils n'ont pas ete lus — voir {@link #opaque}.
     */
    private static boolean[] pixels;

    /** Le plan de l'image, dans le repere du modele : celui des objets plats de vanilla (7,5 sur 16). */
    private static final float SPRITE_Z = 7.5f / 16.0f - 0.5f;

    /** L'image de la piece fait trente-deux pixels de cote, et son bord touche le bord du carre. */
    private static final float TEXELS = 32.0f;

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

        // A plat, ou en volume ? L'image pour ce qui se REGARDE de face — la case de l'inventaire et le
        // cadre a objet — et la piece partout ailleurs : en main, sur la tete, et PAR TERRE, ou le
        // joueur voulait « vraiment la piece en 3D », pas une image.
        boolean flat = context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED;
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
     * <p>Sa TRANCHE se lit comme celle de Minecraft : un panneau plat par pixel du bord du dessin,
     * chacun avec la couleur de ce pixel-la. C'est ce qui donne a la piece son bord en ESCALIER — « un
     * rond fait de carres » — au lieu du carre des quatre bandes d'un modele JSON, ou du cercle lisse
     * d'un anneau de facettes.
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

        // Et sa TRANCHE : un panneau plat par pixel de son bord, exactement comme l'image que Minecraft
        // fabrique lui-meme pour un objet plat — c'est le rendu que le joueur a reconnu : « hier tu
        // avais reussi a le faire bien ».
        rim(pose, buffers, light, overlay);
    }

    /**
     * Sa tranche : un panneau plat par pixel du bord du dessin.
     *
     * <p>Chaque pixel OPAQUE qui touche la transparence est un morceau de bord : selon le cote par
     * lequel il touche le vide, il recoit un panneau perpendiculaire, large d'un pixel et haut de
     * l'epaisseur de la piece, et ce panneau porte la couleur de ce pixel-la, sans melange. C'est
     * exactement ce que fait Minecraft pour une image d'objet plate, et c'est ce qui donne a la piece
     * son bord en escalier.
     */
    private static void rim(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(FRONT));
        float step = 2f * RADIUS / TEXELS;
        for (int px = 0; px < (int) TEXELS; px++) {
            for (int py = 0; py < (int) TEXELS; py++) {
                if (!opaque(px, py)) continue;

                // Le pixel dans le plan du dessin. L'abscisse est MIROIR, comme les faces : plus px
                // avance, plus on va vers les x negatifs.
                float x1 = RADIUS - px * step;
                float x0 = x1 - step;
                float y1 = RADIUS - py * step;
                float y0 = y1 - step;
                float u = (px + 0.5f) / TEXELS;
                float v = (py + 0.5f) / TEXELS;

                // Un panneau pour chaque cote qui donne sur le vide.
                if (!opaque(px, py - 1)) {
                    panelH(consumer, pose, light, overlay, x0, x1, y1, u, v, 1f);
                }
                if (!opaque(px, py + 1)) {
                    panelH(consumer, pose, light, overlay, x0, x1, y0, u, v, -1f);
                }
                if (!opaque(px - 1, py)) {
                    panelV(consumer, pose, light, overlay, y0, y1, x1, u, v, 1f);
                }
                if (!opaque(px + 1, py)) {
                    panelV(consumer, pose, light, overlay, y0, y1, x0, u, v, -1f);
                }
            }
        }
    }

    /** Un morceau de tranche pose a plat : une bande d'un pixel de large, a une ordonnee donnee. */
    private static void panelH(VertexConsumer consumer, PoseStack pose, int light, int overlay,
                               float x0, float x1, float y, float u, float v, float ny) {
        vertex(consumer, pose, light, overlay, x0, y, HALF_THICKNESS, u, v, 0f, ny, 0f);
        vertex(consumer, pose, light, overlay, x1, y, HALF_THICKNESS, u, v, 0f, ny, 0f);
        vertex(consumer, pose, light, overlay, x1, y, -HALF_THICKNESS, u, v, 0f, ny, 0f);
        vertex(consumer, pose, light, overlay, x0, y, -HALF_THICKNESS, u, v, 0f, ny, 0f);
    }

    /** Et un morceau de cote : la meme bande, mais debout, a une abscisse donnee. */
    private static void panelV(VertexConsumer consumer, PoseStack pose, int light, int overlay,
                               float y0, float y1, float x, float u, float v, float nx) {
        vertex(consumer, pose, light, overlay, x, y0, HALF_THICKNESS, u, v, nx, 0f, 0f);
        vertex(consumer, pose, light, overlay, x, y1, HALF_THICKNESS, u, v, nx, 0f, 0f);
        vertex(consumer, pose, light, overlay, x, y1, -HALF_THICKNESS, u, v, nx, 0f, 0f);
        vertex(consumer, pose, light, overlay, x, y0, -HALF_THICKNESS, u, v, nx, 0f, 0f);
    }

    /** Le pixel (px, py) du dessin est-il opaque ? Hors de l'image, il ne l'est pas. */
    private static boolean opaque(int px, int py) {
        if (pixels == null) {
            pixels = readPixels();
        }
        if (px < 0 || py < 0 || px >= (int) TEXELS || py >= (int) TEXELS) return false;
        return pixels[py * (int) TEXELS + px];
    }

    /**
     * Lit l'image une fois pour toutes, et retient ou elle est opaque.
     *
     * <p>Si elle ne se lit pas, tout est transparent : la piece reste dessinee, sans tranche, plutot
     * que de faire tomber le rendu.
     */
    private static boolean[] readPixels() {
        boolean[] result = new boolean[(int) (TEXELS * TEXELS)];
        try (var stream = Minecraft.getInstance().getResourceManager().getResource(FRONT)
                .orElseThrow().open();
             var image = NativeImage.read(stream)) {
            for (int py = 0; py < (int) TEXELS; py++) {
                for (int px = 0; px < (int) TEXELS; px++) {
                    result[py * (int) TEXELS + px] = (image.getPixelRGBA(px, py) >>> 24) != 0;
                }
            }
        } catch (Exception e) {
            // Une image illisible ne doit pas empecher la piece d'exister.
        }
        return result;
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
