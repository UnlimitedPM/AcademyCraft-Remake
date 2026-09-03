package cn.academy.ability;

import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class AbilityInit {

    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CategoryManager.INSTANCE.register(VecmanipCategory.INSTANCE);
            CategoryManager.INSTANCE.register(ElectromasterCategory.INSTANCE);
            CategoryManager.INSTANCE.register(TeleporterCategory.INSTANCE);
            CategoryManager.INSTANCE.bake();
            AbilityNetwork.register();
        });
    }
}
