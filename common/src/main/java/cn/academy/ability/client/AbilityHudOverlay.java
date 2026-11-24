package cn.academy.ability.client;

import cn.academy.client.hud.BodyIntensifyHud;
import cn.academy.client.hud.CpBarHud;
import cn.academy.client.hud.KeyHintHud;
import cn.academy.client.hud.NotificationHud;
import cn.academy.platform.PlatformHooks;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Le HUD des competences : les elements dessines a leur place configuree.
 *
 * <p>Quatre elements sont poses ici, chacun lisant sa place dans {@code HudConfig} : le temoin de
 * points de controle (voir {@link CpBarHud}), le rappel des touches d'aptitude (voir
 * {@link KeyHintHud}), le lecteur media et les notifications (voir {@link NotificationHud}). Aucun
 * n'a de position en dur.
 *
 * <p>Le lecteur media est le seul qui ne soit pas commun : son moteur lit le flux sonore par une
 * methode que seul Forge ajoute a {@code Sound}. Il se dessine donc par le crochet
 * {@link PlatformHooks#drawMediaHud}, et ne fait rien la ou il n'existe pas.
 *
 * <p>Ce qui a ete retire, et pourquoi : au debut du port, cette classe dessinait deux barres grises
 * et des nombres de CP et de surcharge en bas a gauche. Ca ne venait PAS de l'original — chez lui il
 * n'y a jamais rien eu la. Ces nombres appartiennent au menu qui s'ouvre avec F4.
 */
public final class AbilityHudOverlay {

    private AbilityHudOverlay() {
    }

    /** Le dessin, pose par le HUD du chargeur au-dessus de la barre d'objets. */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        KeyHintHud.render(graphics, screenWidth, screenHeight);
        PlatformHooks.drawMediaHud(graphics, screenWidth, screenHeight);
        CpBarHud.render(graphics, screenWidth, screenHeight);
        NotificationHud.render(graphics, screenWidth, screenHeight);
        // Le voile du renfort se pose par-dessus tout le reste : chez l'original il etait une
        // AuxGui, donc dessine apres le HUD — voir BodyIntensifyHud.
        BodyIntensifyHud.render(graphics, screenWidth, screenHeight);
        DebugConsole.render(graphics);
    }
}
