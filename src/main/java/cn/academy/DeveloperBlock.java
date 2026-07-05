package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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

public class DeveloperBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<DevPart> PART = EnumProperty.create("part", DevPart.class);

    public DeveloperBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, DevPart.BASE));
    }

    public enum DevPart implements StringRepresentable {
        // Profondeur (dist) : 0=Front, 1=Mid, 2=Back | Hauteur (h)
        BASE(0,0), F_TOP(0,1),
        M_BOT(1,0), M_MID(1,1), M_TOP(1,2),
        B_BOT(2,0), B_MID(2,1), B_TOP(2,2);

        public final int dist, h;
        DevPart(int dist, int h) { this.dist = dist; this.h = h; }
        @Override public String getSerializedName() { return this.name().toLowerCase(); }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // On pose la BASE, le reste suit derrière
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        Direction back = facing.getOpposite();

        for (DevPart p : DevPart.values()) {
            if (p == DevPart.BASE) continue;
            // On calcule la position : monter de 'h' et reculer de 'dist'
            BlockPos target = pos.above(p.h).relative(back, p.dist);
            level.setBlock(target, state.setValue(PART, p), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            DevPart part = state.getValue(PART);

            // On remonte à la BASE (bloc bleu)
            BlockPos anchor = pos.below(part.h).relative(facing, part.dist);

            // On nettoie tout
            for (DevPart p : DevPart.values()) {
                BlockPos target = anchor.above(p.h).relative(facing.getOpposite(), p.dist);
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