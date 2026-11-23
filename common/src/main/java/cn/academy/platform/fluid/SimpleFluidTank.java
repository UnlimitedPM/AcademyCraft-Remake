package cn.academy.platform.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Predicate;

/**
 * Cuve de fluide neutre : une quantite, un fluide, une capacite.
 *
 * <p>Forge fournit {@code FluidTank} et {@code FluidStack} ; Fabric n'a rien de tel en 1.20.1.
 * Les machines du mod n'ont besoin que de remplir, vider et lire une quantite, donc cette classe
 * suffit aux deux, et un adaptateur cote Forge expose une {@code IFluidHandler} par-dessus.
 */
public final class SimpleFluidTank {

    private final int capacity;
    private final Predicate<Fluid> validator;
    private final Runnable onChanged;

    private Fluid fluid = Fluids.EMPTY;
    private int amount;

    public SimpleFluidTank(int capacity, Predicate<Fluid> validator, Runnable onChanged) {
        this.capacity = capacity;
        this.validator = validator;
        this.onChanged = onChanged;
    }

    public int getCapacity() {
        return capacity;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public int getFluidAmount() {
        return amount;
    }

    public boolean isEmpty() {
        return amount <= 0 || fluid == Fluids.EMPTY;
    }

    /** Le fluide accepte par cette cuve. */
    public boolean accepts(Fluid candidate) {
        return validator.test(candidate);
    }

    /**
     * Remplit la cuve.
     *
     * @return la quantite effectivement acceptee
     */
    public int fill(Fluid candidate, int wanted, boolean simulate) {
        if (wanted <= 0 || candidate == Fluids.EMPTY || !accepts(candidate)) return 0;
        if (!isEmpty() && !fluid.equals(candidate)) return 0;
        int accepted = Math.min(wanted, capacity - amount);
        if (accepted <= 0) return 0;
        if (!simulate) {
            fluid = candidate;
            amount += accepted;
            onChanged.run();
        }
        return accepted;
    }

    /**
     * Vide la cuve.
     *
     * @return la quantite effectivement retiree
     */
    public int drain(int wanted, boolean simulate) {
        if (wanted <= 0 || isEmpty()) return 0;
        int given = Math.min(wanted, amount);
        if (!simulate) {
            amount -= given;
            if (amount <= 0) {
                amount = 0;
                fluid = Fluids.EMPTY;
            }
            onChanged.run();
        }
        return given;
    }

    /** Impose le contenu (tests, commandes, sauvegarde). */
    public void set(Fluid value, int quantity) {
        if (value == null || value == Fluids.EMPTY || quantity <= 0) {
            fluid = Fluids.EMPTY;
            amount = 0;
        } else {
            fluid = value;
            amount = Math.min(quantity, capacity);
        }
        onChanged.run();
    }

    public void save(CompoundTag tag) {
        if (!isEmpty()) {
            tag.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            tag.putInt("amount", amount);
        }
        tag.putInt("capacity", capacity);
    }

    public void load(CompoundTag tag) {
        amount = Math.min(tag.getInt("amount"), capacity);
        if (amount <= 0 || !tag.contains("fluid")) {
            fluid = Fluids.EMPTY;
            amount = 0;
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("fluid"));
        fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
        if (fluid == Fluids.EMPTY) amount = 0;
    }
}
