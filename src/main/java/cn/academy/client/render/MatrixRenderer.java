package cn.academy.client.render;

import cn.academy.MatrixBlock;
import cn.academy.energy.MatrixBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le matrix sans fil : son socle, et ses plaques.
 *
 * <p>Portage de {@code RenderMatrix}. Le socle et le coeur se voient toujours ; les trois
 * plaques ne se montrent que sur un matrix complet — trois plaques dans la machine et un
 * coeur — ou elles tournent lentement au-dessus, en flottant.
 *
 * <p>C'est ce que l'original faisait, et c'est aussi ce que le port ne faisait pas : son
 * modele de bloc contenait les plaques, donc le blockstate les dessinait en permanence,
 * meme sur une machine vide, et toujours a la meme place. Les plaques sont maintenant
 * sorties du modele de bloc : seul le rendu les dessine, et seulement quand il faut.
 */
public class MatrixRenderer implements BlockEntityRenderer<MatrixBlockEntity> {

    private static final ResourceLocation MODEL = ObjModels.model("matrix");
    private static final ResourceLocation TEXTURE = ObjModels.texture("matrix_model");

    /** Les morceaux du fichier : le socle, le coeur, et une plaque. */
    private static final String BASE = "Main";
    private static final String CORE = "Core";
    private static final String PLATE = "Shield";

    /** Les plaques sont trois, a cent vingt degres : {@code 360.0 / plateCount}. */
    private static final int PLATES = 3;

    /** Vitesse de rotation des plaques, en degres par seconde : 50, comme l'original. */
    private static final double SPIN_PER_SECOND = 50.0d;

    /** Flottement vertical des plaques : un dixieme de bloc, toujours comme l'original. */
    private static final double FLOAT_HEIGHT = 0.1d;
    private static final double FLOAT_SPEED = 1.111d;

    /** Et son decalage d'une plaque a l'autre, en radians — l'original ne le convertissait pas. */
    private static final double FLOAT_PHASE = 40.0d;

    public MatrixRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MatrixBlockEntity matrix, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = matrix.getBlockState();
        if (!(state.getBlock() instanceof MatrixBlock)) return;

        ObjMesh mesh = ObjModels.get(MODEL);
        VertexConsumer out = buffers.getBuffer(ObjModels.type(TEXTURE));

        pose.pushPose();
        // L'origine du modele : le centre du multi-bloc, au sol, et donc un bloc devant le
        // coin du bloc d'ancrage.
        pose.translate(0.5d, 0.0d, 0.5d);
        pose.mulPose(Axis.YP.rotationDegrees(-degreesFor(state.getValue(MatrixBlock.FACING))));
        pose.translate(-0.5d, 0.0d, -0.5d);
        pose.translate(0.0d, 0.0d, 1.0d);

        ObjModels.draw(mesh.group(BASE), pose, out, light, overlay);
        ObjModels.draw(mesh.group(CORE), pose, out, light, overlay);

        if (matrix.isWorking()) {
            double time = matrix.getLevel() == null
                    ? 0.0d
                    : (matrix.getLevel().getGameTime() + partialTick) / 20.0d;
            double phase = (time * SPIN_PER_SECOND) % 360.0d;

            for (int i = 0; i < PLATES; i++) {
                pose.pushPose();
                pose.translate(0.0d,
                        FLOAT_HEIGHT * Math.sin(time * FLOAT_SPEED + FLOAT_PHASE * i), 0.0d);
                pose.mulPose(Axis.YP.rotationDegrees(
                        (float) (phase + 360.0d / PLATES * i)));
                ObjModels.draw(mesh.group(PLATE), pose, out, light, overlay);
                pose.popPose();
            }
        }
        pose.popPose();
    }

    /**
     * De combien le modele tourne pour cette orientation.
     *
     * <p>Les chiffres sont ceux du blockstate ({@code blockstates/matrix.json}) : le modele
     * est dessine tel quel pour {@code north}, et tourne d'un quart de tour par direction
     * suivante, dans le sens des aiguilles d'une montre vu de dessus.
     */
    private static int degreesFor(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
