package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.client.render.ObjModels;
import cn.academy.entity.EntitySilbarn;
import cn.academy.entity.SilbarnVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/**
 * Le rendu de la bille de silicium, portage de {@code RenderSibarn}.
 *
 * <p>L'original dessinait un modele au format OBJ — la barre elle-meme — tournant lentement sur
 * un axe tire au hasard, et il <b>cachait</b> la bille des qu'elle etait posee : c'est ce qui
 * donnait l'impression qu'elle restait plantee la ou elle avait touche, alors qu'elle
 * disparaissait dix ticks plus tard.
 *
 * <p>Le port a le modele — {@code models/silbarn.obj}, celui du mod, avec sa texture — mais il
 * tournait sans lui jusqu'ici, faute de chargeur : la bille etait rendue comme l'objet lui-meme.
 * Le port en a un depuis les machines, et la bille prend donc son vrai visage.
 *
 * <h2>La rotation</h2>
 *
 * <p>C'est toute l'animation : une rotation lente, trente degres par seconde, autour d'un axe
 * tire au hasard. Elle se voit d'autant mieux que la barre est longue et plate — vue de biais,
 * elle passe de la tranche a la face, et c'est ce qui la fait lire comme un objet qui flotte
 * plutot que comme une tache.
 *
 * <h2>Et les eclats</h2>
 *
 * <p>C'est aussi ici que se guette le passage a « posee » : la donnee est synchronisee, donc
 * chaque client voit la bascule, et c'est le seul endroit ou une bille passe chez un client sans
 * que le serveur ait a s'en meler. Le drapeau vit sur l'entite — un client qui reverrait la meme
 * bille ne semerait pas deux nuages.
 */
public class SilbarnRenderer extends EntityRenderer<EntitySilbarn> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "models/silbarn.obj");

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/models/silbarn_model.png");

    /** Le seul groupe du modele : {@code g pCube1}. */
    private static final String GROUP = "pCube1";

    public SilbarnRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(EntitySilbarn entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (entity.isHit()) {
            // Elle vient de se casser : les eclats, une fois.
            if (!entity.fragsDone()) {
                entity.markFragsDone();
                SilbarnFrags.spawn(entity.position());
            }
            // Et elle ne se dessine plus : elle est deja « plantee », comme l'original la
            // cachait. Sans cela, elle resterait visible pendant ses dix derniers ticks.
            return;
        }

        pose.pushPose();
        // La rotation d'abord, dans le repere du monde : c'est elle qui tourne sur elle-meme.
        Vec3 axis = entity.spinAxis();
        pose.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(SilbarnVisuals.spinDegrees(entity.tickCount + partialTick)),
                (float) axis.x, (float) axis.y, (float) axis.z)));
        // Puis le lacet de la tete, et la mise a plat du modele, comme l'original.
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(SilbarnVisuals.MODEL_SCALE, SilbarnVisuals.MODEL_SCALE,
                SilbarnVisuals.MODEL_SCALE);
        ObjModels.draw(ObjModels.get(MODEL), GROUP, TEXTURE, pose, buffers, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySilbarn entity) {
        return TEXTURE;
    }
}
