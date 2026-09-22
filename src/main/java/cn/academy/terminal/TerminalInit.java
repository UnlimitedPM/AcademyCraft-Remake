package cn.academy.terminal;

import cn.academy.terminal.app.AppAbout;
import cn.academy.terminal.app.AppSkillTree;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Enregistrement des applications du terminal.
 *
 * L'original decouvrait ses applications par reflexion, en triant les champs
 * annotes {@code @RegApp} par priorite. Ici l'ordre est ecrit a la main : il est
 * court, il ne change pas, et il fixe l'ordre d'affichage dans le terminal.
 *
 * Meme ordre que les priorites de l'original : « A propos » d'abord (priorite -2),
 * puis les applications du joueur. L'identifiant d'une application etant sa place
 * dans cette liste, une application doit s'ajouter a la fin.
 */
public class TerminalInit {

    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AppRegistry.INSTANCE.register(AppAbout.INSTANCE);
            AppRegistry.INSTANCE.register(AppSkillTree.INSTANCE);
            AppRegistry.INSTANCE.bake();
        });
    }
}
