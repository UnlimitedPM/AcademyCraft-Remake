package cn.academy.platform.storage;

import cn.academy.platform.fluid.SimpleFluidTank;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Adaptateur : expose une {@link SimpleFluidTank} neutre comme {@code IFluidHandler} Forge.
 */
public final class ForgeFluidHandlerAdapter implements IFluidHandler {
    private final SimpleFluidTank tank;

    public ForgeFluidHandlerAdapter(SimpleFluidTank tank) {
        this.tank = tank;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tankIndex) {
        return tank.isEmpty() ? FluidStack.EMPTY : new FluidStack(tank.getFluid(), tank.getFluidAmount());
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        return tank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tankIndex, FluidStack stack) {
        return tank.accepts(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return tank.fill(resource.getFluid(), resource.getAmount(),
                action == FluidAction.SIMULATE);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (tank.isEmpty() || !tank.getFluid().equals(resource.getFluid())) return FluidStack.EMPTY;
        int drained = tank.drain(resource.getAmount(), action == FluidAction.SIMULATE);
        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(resource.getFluid(), drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (tank.isEmpty()) return FluidStack.EMPTY;
        int drained = tank.drain(maxDrain, action == FluidAction.SIMULATE);
        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(tank.getFluid(), drained);
    }
}
