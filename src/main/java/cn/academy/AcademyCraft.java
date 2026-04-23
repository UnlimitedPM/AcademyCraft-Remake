package cn.academy;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AcademyCraft.MOD_ID)
public class AcademyCraft {
    public static final String MOD_ID = "academy";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AcademyCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);        // 1. Les Blocs d'abord
        ModItems.register(modEventBus);         // 2. Les Items ensuite
        ModCreativeTabs.register(modEventBus);  // 3. L'onglet en dernier

        MinecraftForge.EVENT_BUS.register(this);
    }
}