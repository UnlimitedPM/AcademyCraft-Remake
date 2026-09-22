package cn.academy.client;

import cn.academy.ModItems;
import cn.academy.entity.EntitySilbarn;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Le rendu de la bille de silicium.
 *
 * <p>L'original dessinait un modele au format OBJ, tournant lentement sur un axe tire au
 * hasard ({@code axis = new Vec3d(rand.nextInt(), ...)}), et il <b>cachait</b> la bille
 * des qu'elle etait posee — c'est ce qui donnait l'impression qu'elle restait plantee la
 * ou elle avait touche, alors qu'elle disparaissait dix ticks plus tard.
 *
 * <p>Le port n'a pas de chargeur OBJ : la bille est donc rendue comme l'objet lui-meme,
 * en trois dimensions. Le cote est de 0,5 bloc — l'original dessinait a 0,05 d'un modele
 * d'un bloc — et la bille tourne avec son lacet, comme l'original.
 */
public class SilbarnRenderer extends EntityRenderer<EntitySilbarn> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            cn.academy.AcademyCraft.MOD_ID, "textures/item/silbarn.png");

    private final ItemRenderer itemRenderer;

    public SilbarnRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(EntitySilbarn entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        // Une bille posee ne se dessine plus : elle est deja « plantee », et l'original la
        // cachait aussi. Sans cela, elle resterait visible pendant ses dix derniers ticks.
        if (entity.isHit()) return;

        pose.pushPose();
        pose.translate(0.0, 0.2, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(new ItemStack(ModItems.SILBARN.get()), ItemDisplayContext.GROUND,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySilbarn entity) {
        return TEXTURE;
    }
}
