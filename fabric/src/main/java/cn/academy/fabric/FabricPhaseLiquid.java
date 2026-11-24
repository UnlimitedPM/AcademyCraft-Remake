package cn.academy.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Le fluide de l'imag phase, cote Fabric.
 *
 * <p>Vanilla ne fournit pas de « fluide coulant » pret a l'emploi : {@code FlowingFluid} est abstrait
 * et c'est a chaque fluide d'en donner les deux variantes et les quatre chiffres qui comptent. Cette
 * classe suit donc {@code WaterFluid}, mot pour mot, en changeant ce qui distingue le liquide du mod.
 *
 * <p>Les chiffres sont ceux que Forge portait deja : une nappe qui s'etale de deux blocs, qui perd
 * trois niveaux par bloc et qui coule d'un cran toutes les trente ticks. Le liquide est epais, et
 * c'est ce qui le rend lisible dans une machine.
 *
 * <p>Ce qui n'est pas repris de Forge, faute d'equivalent : la viscosite et la densite (declarations
 * de {@code FluidType}), la nage et la descente recopiee de la 1.12.2. Sous Fabric le corps suit le
 * deplacement par defaut du fluide — celui de l'eau —, ce qui se voit surtout a la vitesse de
 * descente.
 */
public abstract class FabricPhaseLiquid extends FlowingFluid {

    @Override
    public Fluid getFlowing() {
        return FabricFluids.FLOWING;
    }

    @Override
    public Fluid getSource() {
        return FabricFluids.SOURCE;
    }

    /** Pas de seau : c'est l'unite de phase qui pose le liquide, comme chez Forge. */
    @Override
    public Item getBucket() {
        return Items.BUCKET;
    }

    /** Une source ne se refait pas toute seule : l'eau, elle, le fait entre deux blocs. */
    @Override
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    /** Le liquide emporte ce qu'il traverse, comme l'eau : le bloc est casse sans butin. */
    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 2;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 3;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 30;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0f;
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == FabricFluids.SOURCE || fluid == FabricFluids.FLOWING;
    }

    /** Le bloc que le liquide laisse derriere lui : celui du mod, et son niveau. */
    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return FabricFluids.PHASE_LIQUID_BLOCK.defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                     Fluid fluid, net.minecraft.core.Direction direction) {
        return direction == net.minecraft.core.Direction.DOWN
                && !isSame(fluid);
    }

    /** La variante qui coule : elle porte un niveau, et n'est jamais une source. */
    public static class Flowing extends FabricPhaseLiquid {

        @Override
        protected void createFluidStateDefinition(
                StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    /** La variante posee : pleine, et seule source du liquide. */
    public static class Still extends FabricPhaseLiquid {

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
