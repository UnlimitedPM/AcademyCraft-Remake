package cn.academy;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class PhaseGeneratorBlock extends Block {
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
     * Interaction au clic droit : permet de tester les 5 niveaux de textures.
     * Chaque clic fait passer au niveau suivant (0 -> 1 -> 2 -> 3 -> 4 -> 0).
     */
    @Override
    public InteractionResult use(BlockState state, Level world, net.minecraft.core.BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide()) {
            int nextLevel = (state.getValue(LEVEL) + 1) % 5;
            world.setBlock(pos, state.setValue(LEVEL, nextLevel), 3);

            // Petit message de debug (optionnel) pour confirmer le niveau
            // player.displayClientMessage(net.minecraft.network.chat.Component.literal("Niveau : " + nextLevel), true);
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter reader, net.minecraft.core.BlockPos pos) {
        return true;
    }
}