package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbilityHudEvents {

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Le voile de l'original passe AVANT le HUD vanilla : il se pose donc derriere la barre
        // d'objets, le chat et les elements de l'Academy — comme chez lui, ou il etait dessine
        // en premier. C'est lui qui donne au temoin de CP sa densite d'origine.
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "academy_screen_mask",
                new cn.academy.client.hud.BackgroundMask());

        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "academy_cp", new AbilityHudOverlay());
    }
}
