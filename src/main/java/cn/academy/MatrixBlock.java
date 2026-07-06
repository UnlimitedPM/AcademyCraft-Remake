package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;

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

    // --- LOGIQUE DE PLACEMENT SÉCURISÉE ---
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Direction left = facing.getCounterClockWise();
        Direction back = facing.getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        // On vérifie si l'intégralité de l'espace 2x2x2 est remplaçable
        for (MatrixPart p : MatrixPart.values()) {
            BlockPos target = pos.above(p.y).relative(left, p.l).relative(back, p.b);
            // Si la coordonnée dépasse la hauteur max ou n'est pas libre, on annule tout
            if (target.getY() >= level.getMaxBuildHeight() || !level.getBlockState(target).canBeReplaced(context)) {
                return null;
            }
        }

        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, MatrixPart.B_F_R);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        Direction left = facing.getCounterClockWise();
        Direction back = facing.getOpposite();

        for (MatrixPart p : MatrixPart.values()) {
            if (p == MatrixPart.B_F_R) continue;
            BlockPos target = pos.above(p.y).relative(left, p.l).relative(back, p.b);
            // Double sécurité : on ne pose que si le bloc cible n'est pas déjà notre bloc
            if (level.getBlockState(target).getBlock() != this) {
                level.setBlock(target, state.setValue(PART, p), 3);
            }
        }
    }

    // --- GESTION DE LA CASSE ET ANTI-DUPLICATION ---
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction left = facing.getCounterClockWise();
            Direction back = facing.getOpposite();
            MatrixPart part = state.getValue(PART);

            // On retrouve le point d'ancrage principal (B_F_R)
            BlockPos anchor = pos.below(part.y)
                    .relative(left.getOpposite(), part.l)
                    .relative(back.getOpposite(), part.b);

            BlockState anchorState = level.getBlockState(anchor);

            // Si le joueur casse un DUMMY, on force la destruction de la BASE (B_F_R)
            if (part != MatrixPart.B_F_R) {
                if (anchorState.is(this) && anchorState.getValue(PART) == MatrixPart.B_F_R) {
                    if (player.isCreative()) {
                        level.setBlock(anchor, Blocks.AIR.defaultBlockState(), 35);
                    } else {
                        level.destroyBlock(anchor, true); // Fait drop l'item de la base
                    }
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            Direction left = facing.getCounterClockWise();
            Direction back = facing.getOpposite();
            MatrixPart part = state.getValue(PART);

            if (part == MatrixPart.B_F_R) {
                // La base part, on nettoie tout le reste en silence
                for (MatrixPart p : MatrixPart.values()) {
                    if (p == MatrixPart.B_F_R) continue;
                    BlockPos target = pos.above(p.y).relative(left, p.l).relative(back, p.b);
                    if (level.getBlockState(target).is(this)) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 35);
                    }
                }
            } else {
                // Un dummy part, on détruit la base SANS drop
                BlockPos anchor = pos.below(part.y)
                        .relative(left.getOpposite(), part.l)
                        .relative(back.getOpposite(), part.b);
                BlockState anchorState = level.getBlockState(anchor);
                if (anchorState.is(this) && anchorState.getValue(PART) == MatrixPart.B_F_R) {
                    level.setBlock(anchor, Blocks.AIR.defaultBlockState(), 35); // Pas de drop
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