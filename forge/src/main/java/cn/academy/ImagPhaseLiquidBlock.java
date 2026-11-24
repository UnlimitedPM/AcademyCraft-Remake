package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Le bloc de l'imag phase liquide, cote Forge.
 *
 * <p>Un bloc de fluide, plus une chose : il <b>eteint le feu</b> qu'il touche. La logique est
 * commune — voir {@link PhaseLiquidDousing} — parce que le bloc de liquide n'a pas la meme forme
 * chez les deux chargeurs : ici Forge lui passe un fournisseur de fluide, puisque le fluide n'est
 * pas encore enregistre quand le bloc se construit ; Fabric lui passe le fluide lui-meme.
 *
 * <p>CE BLOCK ENTITY A ETE RETIRE : un par bloc de liquide, c'etait un objet, une entree dans la
 * carte du chunk et une entree dans la sauvegarde, pour ne rien porter du tout. Le rendu retrouve
 * maintenant les blocs par la palette du chunk ({@code LevelChunk.findBlocks}, voir
 * ImagPhaseLiquidRenderer), qui saute les sections ou le bloc n'est pas.
 */
public class ImagPhaseLiquidBlock extends LiquidBlock {

    public ImagPhaseLiquidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        PhaseLiquidDousing.onPlaced(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        PhaseLiquidDousing.neighbourChanged(level, fromPos);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        PhaseLiquidDousing.entityInside(level, entity);
    }
}
