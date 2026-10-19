package cn.academy.client.render;

import cn.academy.AcademyCraft;
import cn.academy.energy.EnergyItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le developeur portable dans la main, portage du TEISR de l'original ({@code ItemDeveloper}).
 *
 * <h2>Pourquoi du code</h2>
 *
 * <p>L'original changeait le rendu de l'objet en main par un {@code TileEntityItemStackRenderer}
 * qui dessinait son modele OBJ : un boitier en volume, pas l'image plate de l'inventaire. Le
 * port dessinait l'image plate partout, et le joueur l'a demande : « normalement il a une
 * interface en 3D en main ».
 *
 * <p>Un modele JSON ne sait pas faire les deux : il montre la meme chose dans la case et dans
 * la main. D'ou ce rendu en code, par le moyen d'aujourd'hui
 * ({@code BlockEntityWithoutLevelRenderer}) : l'IMAGE dans la case, le MODELE en volume
 * partout ailleurs.
 *
 * <h2>Les poses, reprises telles quelles</h2>
 *
 * <p>Les trois chaines de {@code onModelBake} de l'original, dans le meme ordre :
 * <ul>
 *   <li>premiere personne : {@code rotate(0,180,0) . scale(0.3) . translate(0.34, -0.1, -0.1)} ;</li>
 *   <li>troisieme personne : {@code rotate(0,180,0) . scale(0.2)} ;</li>
 *   <li>objet pose : {@code scale(-0.15, -0.15, 0.15) . translate(0, 0.1, 0)}.</li>
 * </ul>
 *
 * <p>Et le meme recentrage que la 1.12.2 : son {@code ItemRenderer} translatait d'un demi-bloc
 * <b>negatif</b> avant d'appeler le TEISR, parce que le modele OBJ est centre sur l'origine. Un
 * rendu d'objet de 1.20.1 n'a pas ce demi-bloc — c'est a lui de le poser, comme le fait
 * {@code CoinItemRenderer} — mais dans l'AUTRE sens : le modele OBJ, lui, est deja centre, donc
 * c'est {@code -0,5} qui le remet dans le repere de l'original. Le modele JSON n'a donc AUCUNE
 * pose : elles sont toutes ici.
 */
@OnlyIn(Dist.CLIENT)
public class DeveloperPortableRenderer extends BlockEntityWithoutLevelRenderer {

    /** Le meme modele que la machine, dont le portable est la version de poche. */
    private static final ResourceLocation MODEL = ObjModels.model("developer_portable");

    /** La texture du modele, lue directement par le rendu (pas par l'atlas des blocs). */
    private static final ResourceLocation TEXTURE = ObjModels.texture("developer_portable_model");

    /** Les trois images de l'inventaire, comme les trois modeles de l'original. */
    private static final ResourceLocation ICON_EMPTY = icon("developer_portable_empty");
    private static final ResourceLocation ICON_HALF = icon("developer_portable_half");
    private static final ResourceLocation ICON_FULL = icon("developer_portable_full");

    /** Le plan de l'image, dans le repere du modele : celui des objets plats de vanilla. */
    private static final float SPRITE_Z = 7.5f / 16.0f - 0.5f;

    public DeveloperPortableRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // La case de l'inventaire : l'image, recentree sur le cube du modele. C'est le
            // demi-bloc POSITIF que la 1.12.2 donnait aux images d'objets, et que le joueur a
            // valide sur la piece du railgun.
            pose.translate(0.5f, 0.5f, 0.5f);
            icon(stack, pose, buffers, light, overlay);
        } else {
            // Le modele OBJ est centre sur l'origine : c'est le demi-bloc NEGATIF de la 1.12.2.
            pose.translate(-0.5f, -0.5f, -0.5f);
            handPose(context, pose);
            ObjModels.draw(ObjModels.get(MODEL).all(), pose,
                    buffers.getBuffer(ObjModels.type(TEXTURE)), light, overlay);
        }
        pose.popPose();
    }

    /** Les trois chaines de l'original, appliquees dans le meme ordre. */
    private static void handPose(ItemDisplayContext context, PoseStack pose) {
        switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                pose.mulPose(Axis.YP.rotationDegrees(180f));
                pose.scale(0.3f, 0.3f, 0.3f);
                pose.translate(0.34f, -0.1f, -0.1f);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                pose.mulPose(Axis.YP.rotationDegrees(180f));
                pose.scale(0.2f, 0.2f, 0.2f);
            }
            case GROUND -> {
                pose.scale(-0.15f, -0.15f, 0.15f);
                pose.translate(0.0f, 0.1f, 0.0f);
            }
            // Les autres contextes (cadre, tete, objet fixe) n'avaient aucune chaine : le
            // modele y est seulement recentre, comme dans l'original.
            default -> { }
        }
    }

    /**
     * L'image de l'inventaire, choisie sur la charge : la meme regle que les trois modeles de
     * l'original ({@code energy} a 0, 0,5 et 1).
     */
    private static void icon(ItemStack stack, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        double max = EnergyItems.maxEnergy(stack);
        double ratio = max <= 0.0d ? 0.0d : EnergyItems.getEnergy(stack) / max;
        ResourceLocation texture = ratio >= 1.0d ? ICON_FULL : ratio >= 0.5d ? ICON_HALF : ICON_EMPTY;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));

        // Face avant, dans le bon sens de lecture.
        vertex(consumer, pose, light, overlay, -0.5f, 0.5f, SPRITE_Z, 0f, 0f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, -0.5f, -0.5f, SPRITE_Z, 0f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, 0.5f, -0.5f, SPRITE_Z, 1f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, 0.5f, 0.5f, SPRITE_Z, 1f, 0f, 0f, 0f, 1f);

        // Et le revers, au meme endroit : l'image se lit dans le meme sens des deux cotes.
        vertex(consumer, pose, light, overlay, 0.5f, 0.5f, SPRITE_Z, 1f, 0f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, 0.5f, -0.5f, SPRITE_Z, 1f, 1f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, -0.5f, -0.5f, SPRITE_Z, 0f, 1f, 0f, 0f, -1f);
        vertex(consumer, pose, light, overlay, -0.5f, 0.5f, SPRITE_Z, 0f, 0f, 0f, 0f, -1f);
    }

    private static void vertex(VertexConsumer consumer, PoseStack pose, int light, int overlay,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        consumer.vertex(pose.last().pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.last().normal(), nx, ny, nz)
                .endVertex();
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/item/" + name + ".png");
    }
}
