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
            MENUS.register("solar_gen", () -> IForgeMenuType.create(SolarGenMenu::new));

    public static final RegistryObject<MenuType<MatrixMenu>> MATRIX =
            MENUS.register("matrix", () -> IForgeMenuType.create(MatrixMenu::new));

    public static final RegistryObject<MenuType<MetalFormerMenu>> METAL_FORMER =
            MENUS.register("metal_former", () -> IForgeMenuType.create(MetalFormerMenu::new));

    public static final RegistryObject<MenuType<ImagFusorMenu>> IMAG_FUSOR =
            MENUS.register("imag_fusor", () -> IForgeMenuType.create(ImagFusorMenu::new));

    public static final RegistryObject<MenuType<WindgenBaseMenu>> WINDGEN_BASE =
            MENUS.register("windgen_base", () -> IForgeMenuType.create(WindgenBaseMenu::new));

    public static final RegistryObject<MenuType<WindgenMainMenu>> WINDGEN_MAIN =
            MENUS.register("windgen_main", () -> IForgeMenuType.create(WindgenMainMenu::new));

    public static final RegistryObject<MenuType<DeveloperMenu>> DEVELOPER =
            MENUS.register("developer", () -> IForgeMenuType.create(DeveloperMenu::new));

    public static final RegistryObject<MenuType<PhaseGeneratorMenu>> PHASE_GENERATOR =
            MENUS.register("phase_gen", () -> IForgeMenuType.create(PhaseGeneratorMenu::new));

    public static final RegistryObject<MenuType<AbilityInterfererMenu>> ABILITY_INTERFERER =
            MENUS.register("ability_interferer", () -> IForgeMenuType.create(AbilityInterfererMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
