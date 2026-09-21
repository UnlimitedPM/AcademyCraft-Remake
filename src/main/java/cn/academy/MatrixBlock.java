package cn.academy;

import cn.academy.energy.ImagNetworkData;
import cn.academy.energy.MatrixBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MatrixBlock extends HorizontalDirectionalBlock implements EntityBlock {
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

    /**
     * Position de la partie d'ancrage ({@code B_F_R}) du multi-bloc, vue depuis
     * n'importe laquelle de ses huit parties.
     *
     * C'est la seule partie qui porte le block entity : tout ce qui veut
     * l'atteindre (ouverture de l'ecran, destruction, detachement du reseau)
     * doit passer par ici, quelle que soit la partie cliquee.
     */
    public static BlockPos anchorOf(BlockPos pos, BlockState state) {
        MatrixPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        Direction left = facing.getCounterClockWise();
        Direction back = facing.getOpposite();
        return pos.below(part.y)
                .relative(left.getOpposite(), part.l)
                .relative(back.getOpposite(), part.b);
    }

    // --- OUVERTURE DE L'ECRAN ---
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos anchor = anchorOf(pos, state);
            if (level.getBlockEntity(anchor) instanceof MatrixBlockEntity matrix) {
                NetworkHooks.openScreen(serverPlayer, matrix, buf -> buf.writeBlockPos(anchor));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
            BlockPos anchor = anchorOf(pos, state);

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

            // Le reseau est detache en premier, et sans passer par le block
            // entity : au moment ou onRemove est appele, rien ne garantit que
            // celui-ci soit encore en place. La donnee de sauvegarde suffit.
            // Les noeuds se rendront compte tout seuls au bout de quelques
            // secondes et chercheront un autre Matrix.
            if (part == MatrixPart.B_F_R && level instanceof ServerLevel server) {
                ImagNetworkData.get(server).removeMatrix(pos);
            }

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
                BlockPos anchor = anchorOf(pos, state);
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

    // ------------------------------------------------------------------
    // Block entity
    // ------------------------------------------------------------------

    /**
     * Un seul block entity pour le multi-bloc : celui de la partie d'ancrage
     * {@code B_F_R}. Les sept autres parties n'en ont pas, sinon chacune aurait
     * son propre tampon et son propre reseau.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == MatrixPart.B_F_R ? new MatrixBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(PART) != MatrixPart.B_F_R) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof MatrixBlockEntity matrix) MatrixBlockEntity.serverTick(lvl, pos, st, matrix);
        };
    }
}