package cn.academy.fabric;

import cn.academy.PhaseLiquidDousing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Le bloc de l'imag phase liquide, cote Fabric.
 *
 * <p>Le meme bloc que chez Forge, avec un constructeur different : le {@code LiquidBlock} de
 * vanilla prend le fluide lui-meme, quand celui de Forge accepte un fournisseur. Le comportement,
 * lui, est commun — voir {@link PhaseLiquidDousing}.
 */
public class FabricPhaseLiquidBlock extends LiquidBlock {

    public FabricPhaseLiquidBlock(FlowingFluid fluid, Properties properties) {
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
