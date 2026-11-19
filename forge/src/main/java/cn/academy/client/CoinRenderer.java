package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.entity.EntityCoinThrowing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/**
 * La piece en vol : un disque epais qui tourne sur lui-meme, avec sa face, son revers et sa tranche.
 *
 * <p>Portage de {@code RendererCoinThrowing}. L'original la dessinait avec
 * {@code RenderUtils.drawEquippedItem(0.0625, FRONT, BACK)} — l'image d'objet du mod, une face en
 * {@code coin_front} et l'autre en {@code coin_back} — tournee de son angle d'horloge autour de l'axe
 * tire au hasard par l'entite, le tout a l'echelle 0,3.
 *
 * <h2>Une piece, pas un carre face a l'oeil</h2>
 *
 * <p>Elle n'est PAS tournee vers la camera : une piece qui flippe se voit de la tranche a la face, et
 * c'est ce qui la fait lire comme un objet qui tourne plutot que comme une image posee dans le vide.
 * Le disque a donc une <b>epaisseur</b>, et sa tranche est un anneau de facettes : sans lui, la piece
 * n'etait que deux quads plats, et elle s'effacait a chaque fois qu'elle passait de profil — deux fois
 * par tour, soit treize clignotements par seconde. C'est le « un peu de bug » que le joueur voyait
 * dans son animation.
 *
 * <p>Sa face est celle qui porte le blason : la face avant regarde le haut quand elle monte,
 * comme une piece lancee du plat de la main.
 */
public class CoinRenderer extends EntityRenderer<EntityCoinThrowing> {

    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/item/coin_front.png");
    private static final ResourceLocation BACK = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/item/coin_back.png");

    /**
     * Le rayon du disque : une piece de vingt-deux centimetres de diametre.
     *
     * <p>Troisieme reglage, et le joueur a encadre la bonne valeur lui-meme : l'original la dessinait
     * a l'echelle 0,3, soit trente centimetres, et le port la trouvait « bien plus grande » que dans
     * le vrai mod ; passe a quinze, elle n'etait plus « pas assez grande dans l'animation ». Vingt-deux
     * est entre les deux.
     */
    private static final float RADIUS = 0.11f;

    /**
     * Son demi-millimetre d'epaisseur : un peu moins de deux centimetres de tranche.
     *
     * <p>C'est la proportion de l'original, qui la dessinait dans une boite de un sur seize d'epais.
     */
    private static final float HALF_THICKNESS = 0.01f;

    /**
     * Les facettes de sa tranche : vingt-quatre, comme la piece en main.
     */
    private static final int RIM_SEGMENTS = 24;

    /** L'image fait trente-deux pixels de cote, et son anneau touche le bord. */
    private static final float TEXELS = 32.0f;

    /** Le rayon de cet anneau dans l'image, un demi-pixel en dedans du bord. */
    private static final float RING_RADIUS = TEXELS / 2.0f - 0.5f;

    /**
     * Degres par seconde : dix-huit cents, soit un demi-tour toutes les cent millisecondes.
     *
     * <p>HISTORIQUE, et c'est le troisieme reglage de cette rotation. L'original fait un demi-tour
     * toutes les cent cinquante millisecondes PUIS revient a zero d'un coup — son
     * {@code (temps * 1000) % 150} multiplie par {@code 360/300} — et c'est ce retour brusque qui donne
     * a sa piece son air de tourner vite. Le port l'avait reproduit au mot pres ; le joueur a alors
     * trouve l'animation « encore un peu buguee » : ce saut de cent quatre-vingts degres se voit, a
     * soixante images par seconde. La rotation est donc de nouveau CONTINUE, mais plus rapide que
     * l'original — cent quatre-vingts degres en cent millisecondes au lieu de cent cinquante — pour
     * garder la vitesse qu'il avait reconnue.
     */
    private static final double SPIN = 1800.0;

    public CoinRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(EntityCoinThrowing entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        // L'AGE DU VOL A CETTE IMAGE : le tick courant, recule d'un tick et avance du temps partiel —
        // voir CoinToss.frameAge. La place et la rotation lisent la MEME horloge, continue, et jamais
        // celle du dernier tick.
        double age = entity.flightAgeAt(partialTick);
        double millis = age * 50.0;
        // L'angle du moment, une rotation CONTINUE : voir SPIN, et l'historique de ses trois reglages.
        float degrees = (float) (millis * SPIN / 1000.0);
        Vec3 axis = entity.spinAxis();

        pose.pushPose();
        // LA PLACE EXACTE, relue elle aussi a chaque image — le suivi au temps partiel, et la hauteur
        // du vol a l'age de l'image : voir EntityCoinThrowing.flightPointAt. Ce n'est donc plus la
        // position interpolee de l'entite, que les paquets du serveur remettent en arriere d'un tick a
        // chaque tick. Le joueur y voyait « son animation toujours un peu moins fluide que dans le vrai
        // mod », et c'est la seule chose qui l'etait : la ou le suivi se relisait deja a chaque image,
        // la HAUTEUR, elle, venait de deux positions de ticks — donc d'un vol hache en morceaux de
        // cinquante millisecondes, et de deux, chaque fois qu'un paquet venait de tomber.
        Vec3 at = entity.flightPointAt(partialTick);
        Vec3 base = entity.getPosition(partialTick);
        pose.translate((float) (at.x - base.x), (float) (at.y - base.y), (float) (at.z - base.z));
        pose.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees),
                (float) axis.x, (float) axis.y, (float) axis.z)));

        coin(pose, buffers, light);

        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** La piece : sa face, son revers, et sa tranche. */
    private static void coin(PoseStack pose, MultiBufferSource buffers, int light) {
        // Les deux faces se dessinent l'une apres l'autre, ecartees de l'epaisseur : deux quads
        // exactement coplanaires se disputeraient la profondeur, et la piece scintillerait.
        face(buffers.getBuffer(RenderType.entityCutoutNoCull(FRONT)), pose, light,
                +HALF_THICKNESS, 0f, 1f, 0f);
        face(buffers.getBuffer(RenderType.entityCutoutNoCull(BACK)), pose, light,
                -HALF_THICKNESS, 0f, -1f, 0f);
        rim(pose, buffers, light);
    }

    /**
     * Une face : un carre de la taille de la piece, pose a plat dans le plan horizontal.
     *
     * <p>Ses quatre coins, dans l'ordre de l'image (haut-gauche, bas-gauche, bas-droit, haut-droit),
     * avec `v` qui vaut zero en haut.
     */
    private static void face(VertexConsumer consumer, PoseStack pose, int light, float y,
                             float nx, float ny, float nz) {
        quad(consumer, pose, light, -RADIUS, y, -RADIUS, 0f, 0f, nx, ny, nz);
        quad(consumer, pose, light, -RADIUS, y, +RADIUS, 0f, 1f, nx, ny, nz);
        quad(consumer, pose, light, +RADIUS, y, +RADIUS, 1f, 1f, nx, ny, nz);
        quad(consumer, pose, light, +RADIUS, y, -RADIUS, 1f, 0f, nx, ny, nz);
    }

    /**
     * Sa tranche : un anneau de facettes, chacune prenant sa couleur sur le BORD de l'image.
     *
     * <p>C'est ce qui remplace les deux quads plats du debut, qui laissaient la piece s'effacer chaque
     * fois qu'elle passait de profil.
     */
    private static void rim(PoseStack pose, MultiBufferSource buffers, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(FRONT));
        for (int i = 0; i < RIM_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / RIM_SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / RIM_SEGMENTS;
            float x0 = (float) (Math.cos(a0) * RADIUS);
            float z0 = (float) (Math.sin(a0) * RADIUS);
            float x1 = (float) (Math.cos(a1) * RADIUS);
            float z1 = (float) (Math.sin(a1) * RADIUS);
            // La normale pointe vers l'exterieur : c'est ce qui donne son eclairage a la tranche.
            float nx = (float) Math.cos((a0 + a1) / 2.0);
            float nz = (float) Math.sin((a0 + a1) / 2.0);

            quad(consumer, pose, light, x0, +HALF_THICKNESS, z0, texelU(a0), texelV(a0), nx, 0f, nz);
            quad(consumer, pose, light, x1, +HALF_THICKNESS, z1, texelU(a1), texelV(a1), nx, 0f, nz);
            quad(consumer, pose, light, x1, -HALF_THICKNESS, z1, texelU(a1), texelV(a1), nx, 0f, nz);
            quad(consumer, pose, light, x0, -HALF_THICKNESS, z0, texelU(a0), texelV(a0), nx, 0f, nz);
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

    /** Un sommet de la piece : position, couleur, image, ecran, lumiere et normale. */
    private static void quad(VertexConsumer consumer, PoseStack pose, int light,
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

    @Override
    public ResourceLocation getTextureLocation(EntityCoinThrowing entity) {
        return FRONT;
    }
}
