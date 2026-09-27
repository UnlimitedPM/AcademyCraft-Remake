package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Le bloc de l'imag phase liquide.
 *
 * <p>Un bloc de fluide ordinaire, avec un block entity en plus : c'est lui qui fait
 * dessiner les nappes du liquide. L'original faisait de meme ({@code BlockImagPhase}
 * posait un {@code TileImagPhase} sur chacun de ses blocs).
 */
public class ImagPhaseLiquidBlock extends LiquidBlock implements net.minecraft.world.level.block.EntityBlock {

    public ImagPhaseLiquidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImagPhaseLiquidBlockEntity(pos, state);
    }
}
