package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class WindgenMainBlock extends HorizontalDirectionalBlock implements EntityBlock {
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

    // --- LOGIQUE DE PLACEMENT SÉCURISÉE ---
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        BlockPos frontPos = pos.relative(facing);
        BlockPos backPos = pos.relative(facing.getOpposite());

        // On s'assure à 100% que les deux côtés sont remplaçables avant d'autoriser le placement
        if (level.getBlockState(frontPos).canBeReplaced(context) &&
                level.getBlockState(backPos).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(FACING, facing).setValue(PART, Part.CENTER);
        }
        return null; // Annule le placement si un bloc gêne
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        BlockPos frontPos = pos.relative(facing);
        BlockPos backPos = pos.relative(facing.getOpposite());

        // Double sécurité : on ne pose que si le bloc cible n'est pas déjà un WindgenMain
        if (level.getBlockState(frontPos).getBlock() != this) {
            level.setBlock(frontPos, state.setValue(PART, Part.FRONT), 3);
        }
        if (level.getBlockState(backPos).getBlock() != this) {
            level.setBlock(backPos, state.setValue(PART, Part.BACK), 3);
        }
    }

    // --- ANTI-DUPLICATION & CASSE EN CASCADE ---
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Part part = state.getValue(PART);

            if (part != Part.CENTER) {
                // Si le joueur détruit un bout (FRONT ou BACK), on trouve le centre
                BlockPos centerPos = (part == Part.FRONT) ? pos.relative(facing.getOpposite()) : pos.relative(facing);
                BlockState centerState = level.getBlockState(centerPos);

                if (centerState.is(this) && centerState.getValue(PART) == Part.CENTER) {
                    // En créatif, on supprime juste le bloc. En survie, on force la destruction du centre pour faire tomber l'item
                    if (player.isCreative()) {
                        level.setBlock(centerPos, Blocks.AIR.defaultBlockState(), 35);
                    } else {
                        level.destroyBlock(centerPos, true); // Le "true" fait drop la loot table du centre
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
            Part part = state.getValue(PART);

            if (part == Part.CENTER) {
                // Le centre part, on nettoie les bouts en silence
                BlockPos front = pos.relative(facing);
                BlockPos back = pos.relative(facing.getOpposite());
                if (level.getBlockState(front).is(this)) level.setBlock(front, Blocks.AIR.defaultBlockState(), 35);
                if (level.getBlockState(back).is(this)) level.setBlock(back, Blocks.AIR.defaultBlockState(), 35);
            } else {
                // Un bout part (explosion ou autre), on détruit le centre SANS drop
                BlockPos centerPos = (part == Part.FRONT) ? pos.relative(facing.getOpposite()) : pos.relative(facing);
                BlockState centerState = level.getBlockState(centerPos);
                if (centerState.is(this) && centerState.getValue(PART) == Part.CENTER) {
                    level.setBlock(centerPos, Blocks.AIR.defaultBlockState(), 35); // Pas de drop
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
     * Seul le bloc central du rotor porte le block entity : les parties avant et
     * arriere ne sont la que pour l'affichage, et trois helices pour un seul rotor
     * n'aurait pas de sens.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.CENTER ? new WindgenMainBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(PART) != Part.CENTER) return null;
        if (type != ModBlockEntities.WINDGEN_MAIN.get()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof WindgenMainBlockEntity rotor) WindgenMainBlockEntity.tick(lvl, pos, st, rotor);
        };
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        // Le clic peut tomber sur un bout du rotor : l'ecran est sur le centre.
        BlockPos center = centerOf(pos, state);
        if (level.getBlockEntity(center) instanceof WindgenMainBlockEntity rotor) {
            NetworkHooks.openScreen(serverPlayer, rotor, buf -> buf.writeBlockPos(center));
        }
        return InteractionResult.CONSUME;
    }

    /** Position du bloc central a partir de n'importe laquelle des trois parties. */
    public static BlockPos centerOf(BlockPos pos, BlockState state) {
        Part part = state.getValue(PART);
        if (part == Part.CENTER) return pos;

        Direction facing = state.getValue(FACING);
        return part == Part.FRONT ? pos.relative(facing.getOpposite()) : pos.relative(facing);
    }
}