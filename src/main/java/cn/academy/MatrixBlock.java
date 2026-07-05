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

public class MatrixBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<MatrixPart> PART = EnumProperty.create("part", MatrixPart.class);

    public MatrixBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, MatrixPart.B_F_R));
    }

    public enum MatrixPart implements StringRepresentable {
        B_F_R(0,0,0), B_F_L(1,0,0), B_B_R(0,0,1), B_B_L(1,0,1),
        T_F_R(0,1,0), T_F_L(1,1,0), T_B_R(0,1,1), T_B_L(1,1,1);

        public final int l, y, b; // l=left offset, y=up, b=back
        MatrixPart(int l, int y, int b) { this.l = l; this.y = y; this.b = b; }
        @Override public String getSerializedName() { return this.name().toLowerCase(); }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(PART, MatrixPart.B_F_R);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        Direction left = facing.getCounterClockWise(); // La gauche par rapport au devant
        Direction back = facing.getOpposite();

        for (MatrixPart p : MatrixPart.values()) {
            if (p == MatrixPart.B_F_R) continue;
            // On place les dummies à gauche, en arrière et au dessus du clic
            BlockPos target = pos.above(p.y).relative(left, p.l).relative(back, p.b);
            level.setBlock(target, state.setValue(PART, p), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            Direction left = facing.getCounterClockWise();
            Direction back = facing.getOpposite();
            MatrixPart part = state.getValue(PART);

            // On retrouve le clic d'origine (B_F_R) en inversant les décalages
            BlockPos anchor = pos.below(part.y)
                    .relative(left.getOpposite(), part.l)
                    .relative(back.getOpposite(), part.b);

            // On nettoie les 8 blocs
            for (MatrixPart p : MatrixPart.values()) {
                BlockPos target = anchor.above(p.y).relative(left, p.l).relative(back, p.b);
                if (level.getBlockState(target).is(this)) {
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
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