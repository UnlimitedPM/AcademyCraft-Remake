package cn.academy; // <--- TRÈS IMPORTANT : Doit être identique à AcademyCraft.java

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems { // <--- Le nom ici doit être EXACTEMENT ModItems
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AcademyCraft.MOD_ID);

    public static final RegistryObject<Item> RESO_CRYSTAL = ITEMS.register("reso_crystal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CONSTRAINT_INGOT = ITEMS.register("constraint_ingot",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final RegistryObject<Item> BRAIN_COMPONENT = ITEMS.register("ac_brain_component",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INFO_COMPONENT = ITEMS.register("ac_info_component",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RESONANCE_COMPONENT = ITEMS.register("ac_resonance_component",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ENERGY_CONVERT_COMPONENT = ITEMS.register("ac_energy_convert_component",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CALC_CHIP = ITEMS.register("ac_calc_chip",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DATA_CHIP = ITEMS.register("ac_data_chip",
            () -> new Item(new Item.Properties()));

    // Plaques et métaux
    public static final RegistryObject<Item> CONSTRAINT_PLATE = ITEMS.register("ac_constraint_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REINFORCED_IRON_PLATE = ITEMS.register("ac_reinforced_iron_plate",
            () -> new Item(new Item.Properties()));
}