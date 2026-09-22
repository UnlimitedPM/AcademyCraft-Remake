package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class PhaseGeneratorBlock extends Block implements EntityBlock {
    // Gestion de la rotation (Face au joueur)
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Gestion des 5 niveaux de textures (ip_gen0 à ip_gen4)
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);

    public PhaseGeneratorBlock(Properties properties) {
        super(properties);
        // État par défaut : Face au Nord, Niveau 0
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LEVEL, 0));
    }

    /**
     * Définit l'orientation du bloc lors de sa pose par un joueur.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * Enregistre les propriétés du bloc pour que Minecraft les reconnaisse.
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEVEL);
    }

    /**
     * Interaction au clic droit : ouvre l'ecran.
     *
     * Le bloc faisait defiler les cinq paliers de texture a chaque clic, ce qui
     * n'etait qu'un banc d'essai pour verifier les modeles. Les paliers servent
     * desormais a montrer le remplissage de la cuve, et le clic a ce que fait
     * toute machine.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PhaseGeneratorBlockEntity generator) {
            NetworkHooks.openScreen(serverPlayer, generator, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------
    // Block entity
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhaseGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.PHASE_GENERATOR.get()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof PhaseGeneratorBlockEntity generator) {
                PhaseGeneratorBlockEntity.tick(lvl, pos, st, generator);
            }
        };
    }

    /**
     * Retire la machine du reseau. Le detachement se fait sur la donnee de
     * sauvegarde et non sur le block entity, qui peut deja avoir disparu.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            cn.academy.energy.NodeFinder.detach(server, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
}