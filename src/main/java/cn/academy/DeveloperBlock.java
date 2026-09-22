package cn.academy;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import cn.academy.ability.develop.DeveloperType;
import cn.academy.energy.NodeFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
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
import net.minecraftforge.network.NetworkHooks;

/**
 * Developeur d'aptitudes.
 *
 * Le bloc etait decoratif : il ne portait ni block entity ni ecran. C'est la
 * machine qui transforme de l'energie en aptitudes, et c'est la seule vraiment
 * indispensable du mod — sans elle, le systeme de competences reste inutilisable.
 *
 * Le developeur est un multi-bloc asymetrique de huit parties, dont l'ancrage
 * {@code BASE} est en bas devant. L'ancrage et le seul a porter le block entity et
 * l'ecran.
 */
public class DeveloperBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final EnumProperty<DevPart> PART = EnumProperty.create("part", DevPart.class);

    private final Supplier<DeveloperType> type;

    public DeveloperBlock(Properties props, Supplier<DeveloperType> type) {
        super(props);
        this.type = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, DevPart.BASE));
    }

    public DeveloperType getDeveloperType() {
        return type.get();
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

    // --- LOGIQUE DE PLACEMENT SECURISEE ---
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Direction back = facing.getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        // On verifie si tout l'espace requis par la structure asymetrique est libre
        for (DevPart p : DevPart.values()) {
            BlockPos target = pos.above(p.h).relative(back, p.dist);
            if (target.getY() >= level.getMaxBuildHeight() || !level.getBlockState(target).canBeReplaced(context)) {
                return null; // Quelque chose gene, on annule le placement
            }
        }

        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, DevPart.BASE);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        Direction back = facing.getOpposite();

        for (DevPart p : DevPart.values()) {
            if (p == DevPart.BASE) continue;
            BlockPos target = pos.above(p.h).relative(back, p.dist);
            // Securite : evite d'ecraser un bloc s'il a change entre-temps
            if (level.getBlockState(target).getBlock() != this) {
                level.setBlock(target, state.setValue(PART, p), 3);
            }
        }
    }

    /**
     * Position de la partie d'ancrage, vue depuis n'importe laquelle des huit.
     *
     * Tout ce qui veut atteindre le block entity (ouverture de l'ecran, destruction
     * en cascade) doit passer par ici, quelle que soit la partie cliquee.
     */
    public static BlockPos anchorOf(BlockPos pos, BlockState state) {
        DevPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        return pos.below(part.h).relative(facing, part.dist);
    }

    // --- GESTION DE LA CASSE ET ANTI-DUPLICATION ---
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos anchor = anchorOf(pos, state);
            BlockState anchorState = level.getBlockState(anchor);

            // Si le joueur detruit n'importe quel leurre, on brise l'ancrage pour
            // declencher le drop unique.
            if (state.getValue(PART) != DevPart.BASE) {
                if (anchorState.is(this) && anchorState.getValue(PART) == DevPart.BASE) {
                    if (player.isCreative()) {
                        level.setBlock(anchor, Blocks.AIR.defaultBlockState(), 35);
                    } else {
                        level.destroyBlock(anchor, true);
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
            DevPart part = state.getValue(PART);

            // L'ancrage s'en va : on detache la machine du reseau et on nettoie le
            // reste de la structure sans bruit.
            if (part == DevPart.BASE) {
                if (level instanceof ServerLevel server) NodeFinder.detach(server, pos);

                for (DevPart p : DevPart.values()) {
                    if (p == DevPart.BASE) continue;
                    BlockPos target = pos.above(p.h).relative(facing.getOpposite(), p.dist);
                    if (level.getBlockState(target).is(this)) {
                        // Flag 35 : supprime sans re-declencher onRemove
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 35);
                    }
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

    /** Seul l'ancrage porte le block entity : les sept autres parties sont des leurres. */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == DevPart.BASE
                ? new DeveloperBlockEntity(pos, state, getDeveloperType())
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(PART) != DevPart.BASE) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof DeveloperBlockEntity dev) DeveloperBlockEntity.tick(lvl, pos, st, dev);
        };
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos anchor = anchorOf(pos, state);
            if (level.getBlockEntity(anchor) instanceof DeveloperBlockEntity dev) {
                NetworkHooks.openScreen(serverPlayer, dev, buf -> buf.writeBlockPos(anchor));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
