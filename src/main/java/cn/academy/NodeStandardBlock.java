package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class NodeStandardBlock extends Block {
    public static final IntegerProperty ENERGY_LEVEL = IntegerProperty.create("energy_level", 0, 4);
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public NodeStandardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ENERGY_LEVEL, 0)
                .setValue(CONNECTED, false));
    }

    public void updateEnergyState(Level level, BlockPos pos, int currentEnergy, boolean isConnected) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this)) {
            int nextLevel = 0;
            if (currentEnergy >= 43750) nextLevel = 4;
            else if (currentEnergy >= 31250) nextLevel = 3;
            else if (currentEnergy >= 18750) nextLevel = 2;
            else if (currentEnergy >= 6250) nextLevel = 1;

            level.setBlock(pos, state.setValue(ENERGY_LEVEL, nextLevel).setValue(CONNECTED, isConnected), 3);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENERGY_LEVEL, CONNECTED);
    }
}