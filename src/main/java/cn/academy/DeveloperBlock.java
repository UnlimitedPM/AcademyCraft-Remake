package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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
import org.jetbrains.annotations.Nullable;

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

    // --- LOGIQUE DE PLACEMENT SÉCURISÉE ---
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Direction back = facing.getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        // On vérifie si tout l'espace requis par la structure asymétrique est libre
        for (DevPart p : DevPart.values()) {
            BlockPos target = pos.above(p.h).relative(back, p.dist);
            if (target.getY() >= level.getMaxBuildHeight() || !level.getBlockState(target).canBeReplaced(context)) {
                return null; // Quelque chose gêne, on annule le placement !
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
            // Sécurité : évite d'écraser un bloc s'il a changé entre-temps
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
            DevPart part = state.getValue(PART);

            // On recalcule la position de la BASE (le bloc d'ancrage)
            BlockPos anchor = pos.below(part.h).relative(facing, part.dist);
            BlockState anchorState = level.getBlockState(anchor);

            // Si le joueur détruit n'importe quel DUMMY, on brise la BASE pour déclencher le drop unique
            if (part != DevPart.BASE) {
                if (anchorState.is(this) && anchorState.getValue(PART) == DevPart.BASE) {
                    if (player.isCreative()) {
                        level.setBlock(anchor, Blocks.AIR.defaultBlockState(), 35);
                    } else {
                        level.destroyBlock(anchor, true); // Déclenche la Loot Table de la base
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

            // Si c'est la base qui s'en va, on nettoie le reste de la structure sans bruit
            if (part == DevPart.BASE) {
                BlockPos anchor = pos; // Pos est déjà l'anchor si part == BASE
                for (DevPart p : DevPart.values()) {
                    if (p == DevPart.BASE) continue;
                    BlockPos target = anchor.above(p.h).relative(facing.getOpposite(), p.dist);

                    if (level.getBlockState(target).is(this)) {
                        // Flag 35 : supprime les blocs sans re-déclencher l'événement onRemove
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
}