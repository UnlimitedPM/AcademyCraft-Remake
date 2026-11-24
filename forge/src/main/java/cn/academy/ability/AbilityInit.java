package cn.academy.ability;

import cn.academy.ability.network.AbilityNetwork;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class AbilityInit {

    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Les quatre cursus sont communs : la liste vit dans AbilityCategories.
            AbilityCategories.registerAll();
            AbilityNetwork.register();
        });
    }
}
