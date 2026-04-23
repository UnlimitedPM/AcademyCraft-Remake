package cn.academy;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    // On crée le registre pour les onglets
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AcademyCraft.MOD_ID);

    // On définit notre onglet "AcademyCraft: Core"
    public static final RegistryObject<CreativeModeTab> ACADEMY_TAB = CREATIVE_MODE_TABS.register("academy_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.RESO_CRYSTAL.get())) // L'icône de l'onglet
                    .title(Component.translatable("itemGroup.academy_tab")) // Le nom (clé de traduction)
                    .displayItems((parameters, output) -> {
                        // Minerais et Lingots de base
                        output.accept(ModBlocks.CONSTRAINT_METAL_ORE.get());
                        output.accept(ModBlocks.CRYSTAL_ORE.get());
                        output.accept(ModBlocks.IMAGSIL_ORE.get());
                        output.accept(ModBlocks.RESO_ORE.get());
                        output.accept(ModItems.RESO_CRYSTAL.get());
                        output.accept(ModItems.CONSTRAINT_INGOT.get());

                        // --- NOUVEAUX COMPOSANTS ---
                        output.accept(ModItems.BRAIN_COMPONENT.get());
                        output.accept(ModItems.INFO_COMPONENT.get());
                        output.accept(ModItems.RESONANCE_COMPONENT.get());
                        output.accept(ModItems.ENERGY_CONVERT_COMPONENT.get());
                        output.accept(ModItems.CALC_CHIP.get());
                        output.accept(ModItems.DATA_CHIP.get());
                        output.accept(ModItems.CONSTRAINT_PLATE.get());
                        output.accept(ModItems.REINFORCED_IRON_PLATE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}