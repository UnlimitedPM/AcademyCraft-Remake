package cn.academy.client.render;

import cn.academy.AcademyCraftCommon;
import cn.academy.entity.EntityShadowCaster;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Le rendu du porteur d'ombre : la machine elle-meme, et rien qu'en carte d'ombre.
 *
 * <p>A l'ecran, ce rendu ne dessine RIEN : la machine est deja dessinee par son propre rendu de
 * block entity, et la dessiner deux fois ne ferait qu'un scintillement de profondeur. C'est la
 * carte d'ombre qui a besoin de ce porteur : la, il delegue au rendu de la machine — la meme pose,
 * le meme modele, la meme rotation des pales — pour que l'ombre soit exactement celle de l'objet.
 *
 * <p>Sans shader, {@code inShadowPass()} est faux : l'entite est donc invisible, et elle ne coute
 * qu'un appel par image.
 */
public class ShadowCasterRenderer extends EntityRenderer<EntityShadowCaster> {

    /** Jamais liee : ce rendu ne dessine aucune texture a lui. */
    private static final ResourceLocation NONE =
            new ResourceLocation(AcademyCraftCommon.MOD_ID,
                    "textures/block/developer_normal_model.png");

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("academy-oculus");

    /** Ce qui a deja ete dit au journal, une fois pour chaque cas. */
    private static boolean shadowReported;
    private static boolean screenReported;

    public ShadowCasterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityShadowCaster caster, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        boolean shadowPass = cn.academy.platform.PlatformHooks.inShadowPass();
        report(shadowPass);

        if (!shadowPass) return;

        BlockEntity machine = caster.level().getBlockEntity(caster.blockPosition());
        if (!EntityShadowCaster.casts(machine)) return;

        // La pose est deja amenee sur le coin du bloc (l'entite y est posee) : c'est exactement
        // ce qu'attend le rendu de block entity.
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .render(machine, partialTick, pose, buffers);
    }

    /** Ce que le porteur a reellement fait, dit une fois par cas — dans le journal du jeu. */
    private static void report(boolean shadowPass) {
        if (shadowPass) {
            if (shadowReported) return;
            shadowReported = true;
            LOGGER.info("Oculus : un porteur d'ombre dessine sa machine dans la carte d'ombre.");
        } else {
            if (screenReported) return;
            screenReported = true;
            LOGGER.info("Oculus : un porteur d'ombre est rendu a l'ecran (invisible par nature).");
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityShadowCaster caster) {
        return NONE;
    }
}
