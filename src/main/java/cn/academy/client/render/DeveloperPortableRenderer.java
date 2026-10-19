package cn.academy.client.render;

import cn.academy.AcademyCraft;
import cn.academy.energy.EnergyItems;
import com.mojang.blaze3d.platform.Lighting;
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
            // ET L'ECLAIRAGE PLAT, sinon l'image sort SOMBRE : un modele `builtin/entity`
            // repond toujours `isGui3d() == true` (`BuiltInModel.isGui3d`, quelle que soit la
            // valeur de `gui_light`), donc la case de l'inventaire est dessinee avec les deux
            // lumieres directionnelles des objets en volume — et notre quad, dont la normale
            // regarde la camera, n'en recoit qu'une fraction. Le joueur l'a vu tout de suite :
            // « l'item dans mon inventaire est sombre comme s'il y avait de l'ombre dessus ».
            Lighting.setupForFlatItems();
            icon(stack, pose, buffers, light, overlay);
            Lighting.setupFor3DItems();
        } else {
            // LE CUBE EST DEJA DECALE D'UN DEMI-BLOC NEGATIF quand on arrive ici :
            // `ItemRenderer.render` fait `translate(-0,5, -0,5, -0,5)` AVANT d'appeler le rendu
            // en code (ligne 113 de sa source), exactement comme la 1.12.2 avant son TEISR.
            // Ce demi-bloc POSITIF l'annule donc, et le modele se retrouve dans le repere de
            // l'original ; puis la chaine de l'original, puis SON propre demi-bloc negatif.
            //
            // C'est le piege qui a coute un aller-retour : sans ce +0,5 le modele tombait un
            // bloc a gauche et sous les pieds (deux fois -0,5), ce que le joueur a vu tout de
            // suite — « il se trouve en dessous de mes pieds et sur la gauche ».
            pose.translate(0.5f, 0.5f, 0.5f);
            handPose(context, pose);
            pose.translate(-0.5f, -0.5f, -0.5f);
            // ET LE MODELE SE REDRESSE ICI. Mesure au rendu hors jeu (projection des 60 faces
            // avec leurs UV) : vu depuis la face de l'ecran, le panneau est dessine a
            // 180 degres — le texte se lit a l'envers ET en miroir. Un demi-tour autour de
            // l'axe du regard (Z) le remet droit, sans lui faire lacher la camera, et c'est
            // aussi ce que montre l'icone de l'objet (l'antenne en haut). Le joueur : « il est
            // encore a l'envers ».
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            ObjModels.draw(ObjModels.get(MODEL).all(), pose,
                    buffers.getBuffer(ObjModels.type(TEXTURE)), light, overlay);
        }
        pose.popPose();
    }

    /**
     * Les trois chaines de l'original, dans SON ordre.
     *
     * <p>{@code TransformChain} PRE-MULTIPLIE chaque operation (`Matrix4f.mul(temp, result)`),
     * donc la chaine de l'originale {@code rotate().scale().translate()} vaut la matrice
     * {@code T * S * R}. Ici c'est la pose qui pre-multiplie pareil : on ecrit donc les
     * operations dans l'ordre de la chaine, {@code translate} d'abord, {@code rotate} en
     * dernier — l'inverse de l'ordre « d'application » habituel.
     */
    private static void handPose(ItemDisplayContext context, PoseStack pose) {
        switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // ECART DEMANDE PAR LE JOUEUR, en deux temps : la chaine de l'original pose
                // l'objet a (0,34; -0,1; -0,1), et en main il tombait trop a DROITE et trop
                // BAS. Deux descentes de 15 puis 11 cm a gauche, et de 12 puis 12 cm vers le
                // haut. C'est le seul nombre a tourner si la main le veut encore autrement.
                pose.translate(0.08f, 0.14f, -0.1f);
                pose.scale(0.3f, 0.3f, 0.3f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                pose.scale(0.2f, 0.2f, 0.2f);
                pose.mulPose(Axis.YP.rotationDegrees(180f));
            }
            case GROUND -> {
                pose.translate(0.0f, 0.1f, 0.0f);
                pose.scale(-0.15f, -0.15f, 0.15f);
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
        RenderType type = RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = buffers.getBuffer(type);

        // UNE SEULE FACE, normale vers la camera. Les deux faces coplanaires se disputent la
        // profondeur (test LEQUAL) et c'est la DERNIERE dessinee qui gagne : c'etait la face
        // arriere, dont la normale regarde ailleurs, donc l'icone sortait SOMBRE malgre
        // l'eclairage plat. Un objet vu de face n'a besoin que d'une face.
        vertex(consumer, pose, light, overlay, -0.5f, 0.5f, SPRITE_Z, 0f, 0f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, -0.5f, -0.5f, SPRITE_Z, 0f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, 0.5f, -0.5f, SPRITE_Z, 1f, 1f, 0f, 0f, 1f);
        vertex(consumer, pose, light, overlay, 0.5f, 0.5f, SPRITE_Z, 1f, 0f, 0f, 0f, 1f);

        // ET LE TAMPON SE VIDE MAINTENANT, PENDANT QUE L'ECLAIRAGE EST PLAT. Un sommet est
        // ecrit dans un tampon et la lumiere directionnelle n'est appliquee qu'AU MOMENT OU
        // le tampon se vide : changer l'eclairage autour de l'ecriture des sommets ne suffit
        // donc pas (c'etait la faute du premier essai, et l'icone restait sombre).
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
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
