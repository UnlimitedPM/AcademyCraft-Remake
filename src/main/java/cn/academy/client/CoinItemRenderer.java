package cn.academy.client;

import cn.academy.AcademyCraft;
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
 *   <li><b>En main</b> : la piece en volume — son blason devant, mais lu en miroir comme chez
 *       l'original, son disque uni derriere, et une TRAN CHE faite d'un anneau de facettes, donc
 *       ronde, et qui prend ses couleurs sur le bord de l'image.</li>
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
     * Sa tranche : quatre centiemes de bloc de chaque cote, donc huit pour cent de sa largeur.
     *
     * <p>C'est l'original — son {@code drawEquippedItem(0,04)} — et c'est deux fois l'epaisseur que le
     * port lui donnait : la tranche etant prise sur le BORD sombre de l'image, c'est aussi ce qui
     * donne a la piece son air un peu plus mat que celui d'une image posee a plat.
     */
    private static final float HALF_THICKNESS = 0.04f;

    /**
     * Les facettes de la tranche : vingt-quatre.
     *
     * <p>En dessous d'une douzaine, le rond se voit ; au-dela, cela ne change rien a l'ecran et
     * chaque facette coute quatre sommets.
     */
    private static final int RIM_SEGMENTS = 24;

    /** Le plan de l'image, dans le repere du modele : celui des objets plats de vanilla (7,5 sur 16). */
    private static final float SPRITE_Z = 7.5f / 16.0f - 0.5f;

    /** L'image de la piece fait trente-deux pixels de cote, et son anneau touche le bord. */
    private static final float TEXELS = 32.0f;

    /** Le rayon de cet anneau dans l'image, un demi-pixel en dedans du bord. */
    private static final float RING_RADIUS = TEXELS / 2.0f - 0.5f;

    public CoinItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        // A plat, ou en volume ? L'image pour tout ce qui se regarde de face (l'inventaire, l'objet
        // pose, l'objet fixe), et la piece pour tout ce qui se tient (les deux mains, la tete).
        boolean flat = context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED
                || context == ItemDisplayContext.GROUND;
        if (flat) {
            sprite(pose, buffers, light, overlay);
        } else {
            coin(pose, buffers, light, overlay);
        }
    }

    // --- L'IMAGE, TELLE QUELLE ---

    /**
     * L'image de la piece : un carre plat, faces avant et arriere.
     *
     * <p>Les deux faces sont au MEME endroit, avec chacune son sens de lecture : c'est exactement ce
     * que fait vanilla pour un objet plat ({@code ItemModelGenerator} pose une face sud et une face
     * nord superposees), donc l'image ne se lit jamais a l'envers ni en miroir. Le type de rendu
     * cache les faces arriere : une seule des deux se dessine, selon l'ou on regarde.
     */
    private static void sprite(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.itemEntityTranslucentCull(FRONT));

        // Face avant : son coin haut-gauche lit le coin haut-gauche de l'image.
        vertex(consumer, pose, light, overlay, -RADIUS, RADIUS, SPRITE_Z, 0f, 0f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, -RADIUS, -RADIUS, SPRITE_Z, 0f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, RADIUS, -RADIUS, SPRITE_Z, 1f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, RADIUS, RADIUS, SPRITE_Z, 1f, 0f, 0f, 0f, 1f);

        // Et le revers, lu dans l'autre sens : vu de derriere, l'image est retournee sans lui.
        vertex(consumer, pose, light, overlay, RADIUS, RADIUS, SPRITE_Z, 0f, 0f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, RADIUS, -RADIUS, SPRITE_Z, 0f, 1f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, -RADIUS, -RADIUS, SPRITE_Z, 1f, 1f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, -RADIUS, RADIUS, SPRITE_Z, 1f, 0f, 0f, 0f, -1f);
    }

    // --- LA PIECE, EN VOLUME ---

    /**
     * La piece de biais : le blason devant, le disque uni derriere, et sa tranche ronde.
     *
     * <p>LE BLASON EST DEVANT, ET A L'ENVERS. Les deux faces de l'original sont dessinees avec
     * l'abscisse RETOURNEE — son {@code drawEquippedItem} fait {@code u = 1 - x} des deux cotes — donc
     * la piece du vrai mod se lit en miroir, exactement comme le joueur l'a decrit : « le recto est la
     * meme image mais en inverse ». Le port, lui, la lisait a l'endroit.
     *
     * <p>La tranche est un <b>anneau de facettes</b>, et chacune prend sa couleur sur le BORD de
     * l'image, a l'angle ou elle se trouve — c'est ce qui fait qu'elle suit l'anneau de la piece au
     * lieu de dessiner un carre. Le joueur avait vu ce carre deux fois : il venait des quatre bandes
     * droites d'un modele JSON.
     */
    private static void coin(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        // Le blason, sur la face que le joueur a devant lui, lu A L'ENVERS — u va de un a zero pendant
        // que x va de moins a plus. SANS CULL, comme l'autre face : aucune des deux ne peut plus
        // disparaitre selon le sens dans lequel on la regarde, ni se faire passer pour l'autre.
        VertexConsumer front = buffers.getBuffer(RenderType.entityCutoutNoCull(FRONT));
        vertex(front, pose, light, overlay, -RADIUS, RADIUS, HALF_THICKNESS, 1f, 0f, 0f, 0f, 1f);
        vertex(front, pose, light, overlay, -RADIUS, -RADIUS, HALF_THICKNESS, 1f, 1f, 0f, 0f, 1f);
        vertex(front, pose, light, overlay, RADIUS, -RADIUS, HALF_THICKNESS, 0f, 1f, 0f, 0f, 1f);
        vertex(front, pose, light, overlay, RADIUS, RADIUS, HALF_THICKNESS, 0f, 0f, 0f, 0f, 1f);

        // Et le disque uni derriere, dans le meme sens que lui : c'est le verso de la piece.
        VertexConsumer back = buffers.getBuffer(RenderType.entityCutoutNoCull(BACK));
        vertex(back, pose, light, overlay, RADIUS, RADIUS, -HALF_THICKNESS, 0f, 0f, 0f, 0f, -1f);
        vertex(back, pose, light, overlay, RADIUS, -RADIUS, -HALF_THICKNESS, 0f, 1f, 0f, 0f, -1f);
        vertex(back, pose, light, overlay, -RADIUS, -RADIUS, -HALF_THICKNESS, 1f, 1f, 0f, 0f, -1f);
        vertex(back, pose, light, overlay, -RADIUS, RADIUS, -HALF_THICKNESS, 1f, 0f, 0f, 0f, -1f);

        // La tranche : sans cull, donc le sens d'enroulement n'a pas d'importance, et chaque
        // facette lit un pixel du bord de l'image.
        VertexConsumer rim = buffers.getBuffer(
                RenderType.entityCutoutNoCull(FRONT));
        for (int i = 0; i < RIM_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / RIM_SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / RIM_SEGMENTS;
            float x0 = (float) (Math.cos(a0) * RADIUS);
            float y0 = (float) (Math.sin(a0) * RADIUS);
            float x1 = (float) (Math.cos(a1) * RADIUS);
            float y1 = (float) (Math.sin(a1) * RADIUS);
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

    /** L'abscisse, dans l'image, du bord de l'anneau a cet angle. */
    private static float texelU(double angle) {
        return (TEXELS / 2.0f + (float) (Math.cos(angle) * RING_RADIUS)) / TEXELS;
    }

    /** Et son ordonnee : l'image a son zero EN HAUT, le modele a son zero en bas. */
    private static float texelV(double angle) {
        return (TEXELS / 2.0f - (float) (Math.sin(angle) * RING_RADIUS)) / TEXELS;
    }

    // --- LES SOMMETS ---

    /** Un sommet du format des objets : position, couleur, image, ecran, lumiere et normale. */
    private static void vertex(VertexConsumer consumer, PoseStack pose, int light, int overlay,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        consumer.vertex(pose.last().pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.last().normal(), nx, ny, nz)
                .endVertex();
    }
}
