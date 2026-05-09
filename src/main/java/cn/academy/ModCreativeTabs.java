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
                    .icon(() -> new ItemStack(ModItems.LOGO.get())) // L'icône de l'onglet
                    .title(Component.translatable("itemGroup.academy_tab")) // Le nom (clé de traduction)
                    .displayItems((parameters, output) -> {
                        // BLOCKS

                        // Minerais
                        output.accept(ModBlocks.CONSTRAINT_METAL_ORE.get());
                        output.accept(ModBlocks.CRYSTAL_ORE.get());
                        output.accept(ModBlocks.IMAGSIL_ORE.get());
                        output.accept(ModBlocks.RESO_ORE.get());

                        // Structures et Machines
                        output.accept(ModBlocks.MACHINE_FRAME.get());
                        output.accept(ModBlocks.IMAG_FUSOR.get());
                        output.accept(ModBlocks.METAL_FORMER.get());
                        output.accept(ModBlocks.PHASE_GEN.get());
                        output.accept(ModBlocks.SOLAR_GEN.get());
                        output.accept(ModBlocks.WINDGEN_MAIN.get());

                        // Réseau
                        output.accept(ModBlocks.MATRIX.get());
                        output.accept(ModBlocks.NODE_BASIC.get());
                        output.accept(ModBlocks.NODE_STANDARD.get());
                        output.accept(ModBlocks.NODE_ADVANCED.get());

                        // Développement
                        output.accept(ModBlocks.DEV_NORMAL.get());
                        output.accept(ModBlocks.DEV_ADVANCED.get());
                        output.accept(ModBlocks.ABILITY_INTERFERER.get());
                        output.accept(ModBlocks.CAT_ENGINE.get());

                        // ITEMS

                        // Cristaux et Métaux[cite: 1]
                        output.accept(ModItems.CRYSTAL_LOW.get());
                        output.accept(ModItems.CRYSTAL_NORMAL.get());
                        output.accept(ModItems.CRYSTAL_PURE.get());
                        output.accept(ModItems.RESO_CRYSTAL.get());
                        output.accept(ModItems.CONSTRAINT_INGOT.get());
                        output.accept(ModItems.CONSTRAINT_PLATE.get());
                        output.accept(ModItems.IMAG_SILICON_INGOT.get());
                        output.accept(ModItems.WAFER.get());
                        output.accept(ModItems.IMAG_SILICON_PIECE.get());

                        // Composants et Cores[cite: 1]
                        output.accept(ModItems.BRAIN_COMPONENT.get());
                        output.accept(ModItems.INFO_COMPONENT.get());
                        output.accept(ModItems.RESONANCE_COMPONENT.get());
                        output.accept(ModItems.ENERGY_CONVERT_COMPONENT.get());
                        output.accept(ModItems.REINFORCED_IRON_PLATE.get());
                        output.accept(ModItems.CALC_CHIP.get());
                        output.accept(ModItems.DATA_CHIP.get());
                        output.accept(ModItems.MAGNETIC_COIL.get());
                        output.accept(ModItems.MAT_CORE_0.get());
                        output.accept(ModItems.MAT_CORE_1.get());
                        output.accept(ModItems.MAT_CORE_2.get());

                        // Unités et DIVERS[cite: 1]
                        output.accept(ModItems.MATTER_UNIT.get());
                        output.accept(ModItems.MATTER_UNIT_PHASE.get());
                        output.accept(ModItems.IMAG_PHASE.get());
                        output.accept(ModItems.ENERGY_UNIT.get());
                        output.accept(ModItems.WINDGEN_FAN.get());
                        output.accept(ModItems.COIN.get());
                        output.accept(ModItems.SILBARN.get());
                        output.accept(ModItems.NEEDLE.get());
                        output.accept(ModItems.MAG_HOOK.get());

                        // Système et Apps[cite: 1]
                        output.accept(ModItems.TERMINAL_INSTALLER.get());
                        output.accept(ModItems.DEVELOPER_PORTABLE.get());
                        output.accept(ModItems.TUTORIAL.get());
                        output.accept(ModItems.APP_SKILL_TREE.get());
                        output.accept(ModItems.APP_MEDIA_PLAYER.get());
                        output.accept(ModItems.APP_FREQ_TRANSMITTER.get());
                        output.accept(ModItems.APP_SETTINGS.get());

                        // Médias
                        output.accept(ModItems.MEDIA_RAILGUN.get());
                        output.accept(ModItems.MEDIA_JUDGELIGHT.get());
                        output.accept(ModItems.MEDIA_SISTERS_NOISE.get());

                        // Facteurs d'Induction
                        output.accept(ModItems.FACTOR_ELECTRO.get());
                        output.accept(ModItems.FACTOR_MELT.get());
                        output.accept(ModItems.FACTOR_TELE.get());
                        output.accept(ModItems.FACTOR_VEC.get());

                        // Phase Liquid et Logo
                        output.accept(ModItems.IMAG_PHASE.get());
                        output.accept(ModItems.LOGO.get());

                        // --- ÉNERGIE ET DEBUG ---
                        output.accept(ModItems.ENERGY_UNIT.get());
                        output.accept(ModItems.DEBUG_CHARGER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}