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

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
