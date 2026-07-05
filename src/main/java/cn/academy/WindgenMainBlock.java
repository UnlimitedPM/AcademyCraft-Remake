package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;

public class WindgenMainBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public WindgenMainBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.CENTER));
    }

    public enum Part implements StringRepresentable {
        FRONT("front"), CENTER("center"), BACK("back");
        private final String name;
        Part(String name) { this.name = name; }
        @Override public String getSerializedName() { return this.name; }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        // On vérifie les deux côtés pour les 4 directions (Nord, Sud, Est, Ouest)
        if (level.getBlockState(pos.relative(facing)).canBeReplaced(context) &&
                level.getBlockState(pos.relative(facing.getOpposite())).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(FACING, facing).setValue(PART, Part.CENTER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        // On force le placement relatif précis
        level.setBlock(pos.relative(facing), state.setValue(PART, Part.FRONT), 3);
        level.setBlock(pos.relative(facing.getOpposite()), state.setValue(PART, Part.BACK), 3);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            Part part = state.getValue(PART);

            if (part == Part.CENTER) {
                // SI ON CASSE LE CENTRE : On nettoie directement devant et derrière
                BlockPos front = pos.relative(facing);
                BlockPos back = pos.relative(facing.getOpposite());

                if (level.getBlockState(front).is(this)) level.setBlock(front, Blocks.AIR.defaultBlockState(), 3);
                if (level.getBlockState(back).is(this)) level.setBlock(back, Blocks.AIR.defaultBlockState(), 3);
            } else {
                // SI ON CASSE UN DUMMY : On trouve le centre et on le détruit
                // Cela va alors déclencher la partie "Part.CENTER" ci-dessus
                BlockPos centerPos = (part == Part.FRONT) ? pos.relative(facing.getOpposite()) : pos.relative(facing);
                if (level.getBlockState(centerPos).is(this)) {
                    level.destroyBlock(centerPos, false);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}