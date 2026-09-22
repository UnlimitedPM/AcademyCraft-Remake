package cn.academy;

import javax.annotation.Nullable;

import cn.academy.energy.NodeFinder;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Brouilleur d'aptitudes.
 *
 * Le bloc etait decoratif : son clic droit se contentait d'allumer et d'eteindre
 * la propriete {@code on}. Elle suit maintenant l'etat reel de la machine, et le
 * clic ouvre l'ecran.
 *
 * C'etait le dernier bloc du mod a ne porter aucun block entity.
 */
public class AbilityInterfererBlock extends Block implements EntityBlock {

    public static final BooleanProperty ON = BooleanProperty.create("on");

    public AbilityInterfererBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ON, false));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // Le poseur est retenu : il ne sera jamais brouille par sa propre machine.
        if (placer instanceof Player player
                && level.getBlockEntity(pos) instanceof AbilityInterfererBlockEntity machine) {
            machine.setPlacer(player);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AbilityInterfererBlockEntity machine) {
            NetworkHooks.openScreen(serverPlayer, machine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------
    // Block entity
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AbilityInterfererBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.ABILITY_INTERFERER.get()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof AbilityInterfererBlockEntity machine) {
                AbilityInterfererBlockEntity.tick(lvl, pos, st, machine);
            }
        };
    }

    /**
     * Retire la machine du reseau. Les joueurs qu'elle brouillait sont relaches par
     * {@code setRemoved} du block entity, qui est le seul moment ou l'on est sur
     * qu'elle ne brouille plus.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            NodeFinder.detach(server, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ON);
    }
}
