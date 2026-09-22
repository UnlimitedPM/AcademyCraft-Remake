package cn.academy.terminal;

import cn.academy.terminal.app.AppSkillTree;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Enregistrement des applications du terminal.
 *
 * L'original decouvrait ses applications par reflexion, en triant les champs
 * annotes {@code @RegApp} par priorite. Ici l'ordre est ecrit a la main : il est
 * court, il ne change pas, et il fixe l'ordre d'affichage dans le terminal.
 */
public class TerminalInit {

    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AppRegistry.INSTANCE.register(AppSkillTree.INSTANCE);
            AppRegistry.INSTANCE.bake();
        });
    }
}
