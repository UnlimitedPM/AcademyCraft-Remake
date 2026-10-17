package cn.academy.client.render;

import cn.academy.DeveloperBlock;
import cn.academy.DeveloperBlockEntity;
import cn.academy.ability.develop.DeveloperType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Les deux machines developeur, dessinees comme dans l'original : par un rendu de block entity.
 *
 * <p>C'etait un modele de bloc jusqu'ici, et ce n'etait pas seulement une question de gout. Une
 * couche de bloc translucide ne touche pas a la profondeur : les surfaces cachees derriere la vitre
 * se dessinaient donc aussi, et l'on voyait la vitre ARRIERE au travers de celle de devant — une
 * grande paroi bleue qui bouchait toute la vue. L'original ecrivait la profondeur, donc seule la
 * vitre de devant se melangeait au monde, deja peint. Le joueur a decrit les deux : « dans le vrai
 * mod, a l'interieur de la transparence on voit un peu le haut de l'exterieur dedans, alors que la
 * pour nous non ».
 *
 * <p>Le rendu ne dessine QUE la partie d'ancrage : les sept autres sont des briques invisibles du
 * meme multi-bloc.
 */
public class DeveloperRenderer implements BlockEntityRenderer<DeveloperBlockEntity> {

    private static final ResourceLocation ADVANCED_MODEL = ObjModels.model("developer_advanced");
    private static final ResourceLocation NORMAL_MODEL = ObjModels.model("developer_normal");
    private static final ResourceLocation ADVANCED_TEXTURE = ObjModels.texture("developer_advanced_model");
    private static final ResourceLocation NORMAL_TEXTURE = ObjModels.texture("developer_normal_model");

    /**
     * L'echelle du modele, et rien de plus.
     *
     * <p>Le modele de bloc, lui, appliquait son decalage et son echelle <b>autour du centre de la
     * brique</b> ({@code BlockMath.blockCenterToCorner} : {@code +0,5 ... -0,5}). L'original 1.12.2
     * ne faisait pas cela : son rendu de block entity translatait d'un demi-bloc, tournait, puis
     * mettait a l'echelle depuis l'ORIGINE du modele. C'est cette mise en place-la qu'on reprend,
     * car c'est la seule qui pose la machine sur le sol de la brique et l'aligne sur son multi-bloc.
     */
    private static final float SCALE = 0.5f;

    /**
     * Les deux rattrapages mesures sur le pied du modele, en coordonnees du modele.
     *
     * <p>Le pied — les faces a moins de 0,05 du sol — couvre {@code x -1,072..0,928} dans les deux
     * fichiers OBJ, soit un centre a {@code -0,072} : le vrai mod le laisse donc deborder d'un cote
     * et la machine parait poussee vers la droite, avec un pied qui ne tombe pas au milieu de la
     * brique. On le recentre ({@code +0,072} modele = {@code +0,036} bloc sur le cote), puis on
     * avance la machine d'un cheveu ({@code -0,04} modele = {@code +0,0185} bloc vers l'avant).
     * Le pied fait pile deux unites de large, donc une fois recentre il couvre exactement la
     * brique.
     */
    private static final float SHIFT_SIDE = 0.072f;
    private static final float SHIFT_FORWARD = -0.04f;

    public DeveloperRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DeveloperBlockEntity developer, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = developer.getBlockState();
        if (!(state.getBlock() instanceof DeveloperBlock block)) return;
        if (state.getValue(DeveloperBlock.PART) != DeveloperBlock.DevPart.BASE) return;

        boolean advanced = block.getDeveloperType() == DeveloperType.ADVANCED;
        ResourceLocation model = advanced ? ADVANCED_MODEL : NORMAL_MODEL;
        ResourceLocation texture = advanced ? ADVANCED_TEXTURE : NORMAL_TEXTURE;

        pose.pushPose();
        // La chaine d'origine, dans le meme ordre : un demi-bloc d'ecart (le centre de rotation du
        // multi-bloc), le quart de tour du blockstate — les 180 degres de drawAtOrigin y sont deja
        // fondus, c'est le sens INVERSE qu'attend un modele de bloc — puis l'echelle depuis
        // l'origine du modele.
        pose.translate(0.5d, 0.0d, 0.5d);
        pose.mulPose(Axis.YP.rotationDegrees(-degrees(state.getValue(DeveloperBlock.FACING))));
        pose.scale(SCALE, SCALE, SCALE);

        // Dans le repere du modele, donc dans le bon axe quelle que soit la direction : le pied
        // recentre sur la brique, puis le leger surplus vers l'avant.
        pose.translate(SHIFT_SIDE, 0.0d, SHIFT_FORWARD);

        ObjModels.draw(ObjModels.get(model).all(), pose,
                buffers.getBuffer(ObjModels.translucent(texture)), light, overlay);
        pose.popPose();
    }

    /**
     * Les memes quarts de tour que les fichiers de blockstate des deux machines, augmentes des 180
     * degres que le rendu d'origine appliquait lui-meme ({@code drawAtOrigin}).
     */
    private static int degrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
