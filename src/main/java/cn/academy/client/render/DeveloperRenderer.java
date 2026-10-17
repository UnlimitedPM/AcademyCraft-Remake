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
     * La transformation du modele de bloc, reprise telle quelle de ses fichiers JSON.
     *
     * <p>Un modele de bloc applique son decalage et son echelle <b>autour du centre de la brique</b>
     * (c'est ce que fait {@code BlockMath.blockCenterToCorner} : {@code +0,5 ... -0,5}), et c'est
     * pourquoi la pose commence et finit par un demi-bloc.
     */
    private static final float ADVANCED_TX = -0.2138f, ADVANCED_TY = -0.746f, ADVANCED_TZ = -0.2518f;
    private static final float NORMAL_TX = -0.214f, NORMAL_TY = -0.75f, NORMAL_TZ = -0.256f;
    private static final float SCALE = 0.5f;

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
        float tx = advanced ? ADVANCED_TX : NORMAL_TX;
        float ty = advanced ? ADVANCED_TY : NORMAL_TY;
        float tz = advanced ? ADVANCED_TZ : NORMAL_TZ;

        pose.pushPose();
        // L'orientation du blockstate : quart de tour autour du CENTRE de la brique, et dans le sens
        // INVERSE de la rotation de la pose — la regle de tous les modeles de blocs du port.
        pose.translate(0.5d, 0.0d, 0.5d);
        pose.mulPose(Axis.YP.rotationDegrees(-degrees(state.getValue(DeveloperBlock.FACING))));
        pose.translate(-0.5d, 0.0d, -0.5d);

        pose.translate(0.5d + tx, 0.5d + ty, 0.5d + tz);
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-0.5d, -0.5d, -0.5d);

        ObjModels.draw(ObjModels.get(model).all(), pose,
                buffers.getBuffer(ObjModels.translucent(texture)), light, overlay);
        pose.popPose();
    }

    /** Les memes quarts de tour que les fichiers de blockstate des deux machines. */
    private static int degrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
