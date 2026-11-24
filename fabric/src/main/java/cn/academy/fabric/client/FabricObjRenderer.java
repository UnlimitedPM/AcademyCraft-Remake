package cn.academy.fabric.client;

import cn.academy.client.render.ObjMesh;
import cn.academy.client.render.ObjModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import java.util.function.Function;

/**
 * Dessine un modele OBJ a la place d'un modele de bloc.
 *
 * <p>Les machines du mod ne sont pas des cubes : ce sont des fichiers {@code .obj}, et Forge les
 * dessine par son <b>chargeur de modele</b> ({@code "loader": "forge:obj"}), qui n'existe pas sous
 * Fabric. Le resultat y etait donc invisible : ni la generatrice solaire, ni la machine a phase,
 * ni l'eolienne ne se voyaient.
 *
 * <p>Le port avait deja tout ce qu'il faut pour les dessiner sans lui — {@link ObjModels} et
 * {@link ObjMesh}, ecrits pour les pales et le matrix — et c'est ce que fait cette classe : elle
 * relit le meme fichier et le pose au meme endroit, avec la meme image, depuis le rendu du block
 * entity.
 *
 * <p>Ce qui change par rapport au chargeur de Forge, et pourquoi : un modele de bloc est cuit une
 * fois pour toutes, alors qu'ici le dessin se refait a chaque image. Le repere, lui, est le meme —
 * l'origine au centre du bas du bloc, comme {@code WindgenMainRenderer} le fait deja cote Forge.
 */
public final class FabricObjRenderer implements BlockEntityRenderer<BlockEntity> {

    /** Ou et comment poser un modele : le peu qui differe d'une machine a l'autre. */
    public record Placement(ResourceLocation model,
                            Function<BlockState, ResourceLocation> texture,
                            double x, double y, double z,
                            float scale,
                            DirectionProperty facing,
                            boolean facingIsReversed) {

        /** Le modele centre sur le bloc, tourne comme la face du bloc s'il en a une. */
        public static Placement of(String model, Function<BlockState, ResourceLocation> texture,
                                   DirectionProperty facing, boolean reversed) {
            return new Placement(ObjModels.model(model), texture, 0.5d, 0.0d, 0.5d, 1.0f,
                    facing, reversed);
        }
    }

    /**
     * Le rendu d'un type de block entity, avec son placement.
     *
     * <p>Le transtypage est celui de tous les rendus generiques : le jeu ne connait que
     * {@code BlockEntityRenderer<T>}, et le placement, lui, ne depend pas du type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityRendererProvider<T> provider(Placement placement) {
        return context -> (BlockEntityRenderer<T>) (BlockEntityRenderer<?>) new FabricObjRenderer(placement);
    }

    private final Placement placement;

    /** Package-prive : l'eolienne compose ce rendu avec celui des pales. */
    FabricObjRenderer(Placement placement) {
        this.placement = placement;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        ObjMesh mesh = ObjModels.get(placement.model());
        if (mesh.isEmpty()) return;

        BlockState state = blockEntity.getBlockState();
        ResourceLocation texture = placement.texture().apply(state);
        if (texture == null) return;

        pose.pushPose();
        pose.translate(placement.x(), placement.y(), placement.z());

        // La rotation du modele de bloc : elle tourne autour du centre du bas du bloc, et c'est
        // exactement la ou la translation ci-dessus nous a mis. Le sens depend du modele — celui de
        // l'eolienne regarde vers le nord dans son fichier, celui du generateur solaire regarde
        // vers le sud — et c'est pour cela que le placement le porte.
        DirectionProperty facing = placement.facing();
        if (facing != null) {
            Direction direction = state.getValue(facing);
            float degrees = placement.facingIsReversed()
                    ? -direction.toYRot()
                    : direction.toYRot();
            pose.mulPose(Axis.YP.rotationDegrees(degrees));
        }
        if (placement.scale() != 1.0f) {
            pose.scale(placement.scale(), placement.scale(), placement.scale());
        }

        ObjModels.drawAll(mesh, texture, pose, buffers, light, overlay);
        pose.popPose();
    }
}
