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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * La piece en vol : un disque qui tourne sur lui-meme, avec sa face et son revers.
 *
 * <p>Portage de {@code RendererCoinThrowing}. L'original la dessinait avec
 * {@code RenderUtils.drawEquippedItem(0.0625, FRONT, BACK)} — l'image d'objet du mod, une face en
 * {@code coin_front} et l'autre en {@code coin_back} — tournee d'un angle qui avance de
 * {@link #SPIN} degres par seconde autour de l'axe tire au hasard par l'entite, le tout a l'echelle
 * 0,3.
 *
 * <h2>Une piece, pas un carre face a l'oeil</h2>
 *
 * <p>Elle n'est PAS tournee vers la camera : une piece qui flippe se voit de la tranche a la face,
 * et c'est ce qui la fait lire comme un objet qui tourne plutot que comme une image posee dans le
 * vide. Les deux faces se dessinent donc l'une apres l'autre, ecartees d'un millieme de bloc le
 * long de la normale — deux quads exactement coplanaires se disputeraient la profondeur, et la
 * piece scintillerait.
 *
 * <p>Sa face est celle qui porte le blason : la face avant regarde le haut quand elle monte,
 * comme une piece lancee du plat de la main.
 */
public class CoinRenderer extends EntityRenderer<EntityCoinThrowing> {

    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/item/coin_front.png");
    private static final ResourceLocation BACK = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/item/coin_back.png");

    /** L'echelle de l'original : 0,3 de l'image d'objet, soit un disque d'environ trente centimetres. */
    private static final float SCALE = 0.3f;

    /** Son demi-cote, une fois mise a l'echelle : de quoi poser les quatre coins. */
    private static final float HALF = SCALE / 2f;

    /** Le demi-millieme qui separe les deux faces, en blocs. */
    private static final float FACES_GAP = 0.001f;

    /**
     * Degres par seconde : l'original en faisait un tour en 300 millisecondes.
     *
     * <p>ECART ASSUME : son angle repartait de zero toutes les 150 millisecondes — son
     * {@code (temps * 1000) % 150} — donc la piece faisait un demi-tour puis revenait d'un coup.
     * Ici l'angle avance sans jamais se remettre a zero : c'est ce que l'auteur voulait, et c'est
     * invisible sur une piece, mais deux pieces lancees ensemble ne se ressemblent pas pour autant.
     */
    private static final double SPIN = 1200.0;

    public CoinRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(EntityCoinThrowing entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        // L'angle du moment : il part du tick courant et avance avec le temps partiel, sinon la
        // piece avancerait par saccades d'un tick.
        double millis = (entity.tickCount + partialTick) * 50.0;
        Vec3 axis = entity.spinAxis();

        pose.pushPose();
        // ELLE EST LA OU EST L'ENTITE, et nulle part ailleurs : le serveur la deplace a chaque tick
        // — elle suit son lanceur, et sa hauteur est celle de son age — et le client l'interpole
        // entre deux positions recues. Le rendu n'a donc RIEN a recalculer, et c'est ce qui l'empeche
        // de trembler, de reculer, ou de disparaitre pendant que le joueur court.
        pose.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(millis * SPIN / 1000.0),
                (float) axis.x, (float) axis.y, (float) axis.z)));

        VertexConsumer front = buffers.getBuffer(
                RenderType.entityCutoutNoCull(FRONT));
        vertex(front, pose, light, +FACES_GAP);
        VertexConsumer back = buffers.getBuffer(
                RenderType.entityCutoutNoCull(BACK));
        vertex(back, pose, light, -FACES_GAP);

        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /**
     * Un carre de la taille de la piece, pose a plat et decale le long de sa normale.
     *
     * @param gap {@code +} pour la face du dessus, {@code -} pour celle du dessous
     */
    private static void vertex(VertexConsumer consumer, PoseStack pose, int light, float gap) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        // Le carre est pose dans le plan horizontal : ses quatre coins, dans l'ordre de l'image
        // (haut-gauche, bas-gauche, bas-droit, haut-droit), avec `v` qui vaut zero en haut.
        float y = gap;
        quad(consumer, matrix, normal, light, -HALF, y, -HALF, 0f, 0f);
        quad(consumer, matrix, normal, light, -HALF, y, +HALF, 0f, 1f);
        quad(consumer, matrix, normal, light, +HALF, y, +HALF, 1f, 1f);
        quad(consumer, matrix, normal, light, +HALF, y, -HALF, 1f, 0f);
    }

    /** Un coin, avec sa lumiere plate : une piece fine n'a pas de relief a ombrer. */
    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light,
                             float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0f, 1f, 0f)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCoinThrowing entity) {
        return FRONT;
    }
}
