package cn.academy.ability.client;

import cn.academy.client.hud.CpBarHud;
import cn.academy.client.hud.KeyHintHud;
import cn.academy.client.hud.MediaHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Le HUD des competences : les elements dessines a leur place configuree.
 *
 * <p>Trois elements sont poses ici, chacun lisant sa place dans {@code HudConfig} : le temoin de
 * points de controle (voir {@link CpBarHud}), le rappel des touches d'aptitude (voir
 * {@link KeyHintHud}) et le lecteur media (voir {@link MediaHud}). Les notifications restent a
 * faire : elles viendront s'ajouter de la meme facon, jamais avec une position en dur.
 *
 * <p>Ce qui a ete retire, et pourquoi : au debut du port, cette classe dessinait deux barres
 * grises et des nombres de CP et de surcharge en bas a gauche. Ca ne venait PAS de l'original —
 * chez lui il n'y a jamais rien eu la. Ces nombres appartiennent au menu qui s'ouvre avec F4.
 */
public class AbilityHudOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        KeyHintHud.render(graphics, screenWidth, screenHeight);
        MediaHud.render(graphics, screenWidth, screenHeight);
        CpBarHud.render(graphics, screenWidth, screenHeight);
        DebugConsole.render(graphics);
    }
}
