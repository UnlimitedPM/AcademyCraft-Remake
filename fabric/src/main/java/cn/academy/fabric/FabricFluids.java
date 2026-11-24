package cn.academy.fabric;

import cn.academy.AcademyCraftCommon;
import cn.academy.PhaseUnitItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * L'imag phase liquide, cote Fabric.
 *
 * <p>Le fluide n'est pas une donnee que le code commun peut porter : Forge le decrit par un
 * {@code FluidType} et un {@code ForgeFlowingFluid}, qui n'existent pas sous Fabric. Ce qui est
 * commun, c'est <b>ce qui se voit</b> : le comportement du bloc ({@code PhaseLiquidDousing}) et
 * l'unite qui le pose ({@link PhaseUnitItem}). Ce fichier ne fait donc que declarer le fluide
 * lui-meme, ses deux variantes et le bloc.
 *
 * <p>Sans ces declarations le liquide n'existe pas du tout : l'unite de phase se posait comme un
 * objet inerte, et il n'y avait rien a couler dans les machines — le joueur l'a vu.
 */
public final class FabricFluids {

    /** La variante posee : celle qu'on trouve a la source. */
    public static FlowingFluid SOURCE;

    /** La variante qui coule, quand la nappe descend. */
    public static FlowingFluid FLOWING;

    /** Le bloc de liquide, celui que l'unite de phase pose. */
    public static LiquidBlock PHASE_LIQUID_BLOCK;

    /** L'unite de phase elle-meme, pour l'onglet creatif. */
    public static Item UNIT;

    private FabricFluids() {
    }

    /**
     * Declare le fluide, son bloc et l'objet qui le pose.
     *
     * <p>Les deux variantes d'abord, le bloc ensuite : la variante posee fabrique son bloc de
     * remplacement a partir de {@link #PHASE_LIQUID_BLOCK}, donc il doit exister avant le premier
     * tick du jeu — et pas avant sa construction, ou il n'est pas encore la.
     */
    public static void register() {
        SOURCE = Registry.register(BuiltInRegistries.FLUID, id("phase_liquid"),
                new FabricPhaseLiquid.Still());
        FLOWING = Registry.register(BuiltInRegistries.FLUID, id("phase_liquid_flowing"),
                new FabricPhaseLiquid.Flowing());

        PHASE_LIQUID_BLOCK = Registry.register(BuiltInRegistries.BLOCK, id("phase_liquid"),
                new FabricPhaseLiquidBlock(SOURCE,
                        BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()
                                .lightLevel(state -> 8)));

        // L'unite de phase pose le bloc : c'est un objet de bloc, pas un seau — comme chez Forge.
        UNIT = Registry.register(BuiltInRegistries.ITEM, id("matter_unit_phase_liquid"),
                new PhaseUnitItem(PHASE_LIQUID_BLOCK, new Item.Properties().stacksTo(16)));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AcademyCraftCommon.MOD_ID, path);
    }
}
