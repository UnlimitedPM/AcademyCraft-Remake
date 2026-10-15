package cn.academy.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Bloc de noeud sans fil, commun aux trois qualites.
 *
 * Remplace les trois classes identiques {@code NodeBasicBlock},
 * {@code NodeStandardBlock} et {@code NodeAdvancedBlock} du port, qui
 * dupliquaient les memes proprietes et les memes paliers de texture codes en
 * dur. Les noms de proprietes ({@code energy_level}, {@code connected}) et leur
 * domaine (0 a 4) sont conserves : les blockstates et les modeles existants
 * continuent de fonctionner sans modification.
 */
public class NodeBlock extends Block implements EntityBlock {

    /** 0 = vide ... 4 = plein, paliers calcules depuis {@link NodeType}. */
    public static final IntegerProperty ENERGY_LEVEL = IntegerProperty.create("energy_level", 0, 4);

    /** Vrai quand le noeud est raccorde a un Matrix. */
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    private final NodeType type;

    public NodeBlock(NodeType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any()
                .setValue(ENERGY_LEVEL, 0)
                .setValue(CONNECTED, false));
    }

    public NodeType getType() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENERGY_LEVEL, CONNECTED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof NodeBlockEntity node) NodeBlockEntity.serverTick(lvl, pos, st, node);
        };
    }

    /**
     * Le clic droit ouvre l'interface du noeud.
     *
     * L'original l'ouvrait depuis son item de bloc ; le port n'avait rien a montrer, donc rien a
     * ouvrir. C'est l'ecran — la page de l'inventaire, son animation et son panneau — qui justifie
     * cette methode.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof NodeBlockEntity node) {
            NetworkHooks.openScreen(serverPlayer, node, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Detache le noeud de son Matrix et de ses generateurs / recepteurs.
     *
     * Le detachement se fait sur la donnee de sauvegarde et non sur le block
     * entity : au moment ou onRemove est appele, rien ne garantit que celui-ci
     * soit encore en place.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            ImagNetworkData.get(server).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Palier de texture correspondant a un taux de remplissage, selon la qualite
     * de ce noeud.
     */
    public int energyLevelFor(double energy) {
        return type.energyLevelFor(energy);
    }
}
