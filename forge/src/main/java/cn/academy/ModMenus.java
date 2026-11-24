package cn.academy;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AcademyCraft.MOD_ID);

    public static final RegistryObject<MenuType<SolarGenMenu>> SOLAR_GEN =
            MENUS.register("solar_gen", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new SolarGenMenu(ModMenus.SOLAR_GEN.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<MatrixMenu>> MATRIX =
            MENUS.register("matrix", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new MatrixMenu(ModMenus.MATRIX.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<MetalFormerMenu>> METAL_FORMER =
            MENUS.register("metal_former", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new MetalFormerMenu(ModMenus.METAL_FORMER.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<ImagFusorMenu>> IMAG_FUSOR =
            MENUS.register("imag_fusor", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new ImagFusorMenu(ModMenus.IMAG_FUSOR.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<WindgenBaseMenu>> WINDGEN_BASE =
            MENUS.register("windgen_base", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new WindgenBaseMenu(ModMenus.WINDGEN_BASE.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<WindgenMainMenu>> WINDGEN_MAIN =
            MENUS.register("windgen_main", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new WindgenMainMenu(ModMenus.WINDGEN_MAIN.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<DeveloperMenu>> DEVELOPER =
            MENUS.register("developer", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new DeveloperMenu(
                            ModMenus.DEVELOPER.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<PhaseGeneratorMenu>> PHASE_GENERATOR =
            MENUS.register("phase_gen", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new PhaseGeneratorMenu(ModMenus.PHASE_GENERATOR.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<AbilityInterfererMenu>> ABILITY_INTERFERER =
            MENUS.register("ability_interferer", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new AbilityInterfererMenu(
                            ModMenus.ABILITY_INTERFERER.get(), id, inventory, buf)));

    public static final RegistryObject<MenuType<NodeMenu>> NODE =
            MENUS.register("node", () -> IForgeMenuType.create(
                    (id, inventory, buf) -> new NodeMenu(ModMenus.NODE.get(), id, inventory, buf)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
