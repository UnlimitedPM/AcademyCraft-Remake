package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Draws the local player's Control Point bar above the hotbar. */
public class AbilityHudOverlay implements IGuiOverlay {

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        AbilityData data = ClientAbilityData.get();
        float maxCp = data.getMaxControlPoint();
        if (maxCp <= 0) return;

        float cp = data.getControlPoint();
        int x = 10;
        int y = screenHeight - 50;
        int filled = Math.round(BAR_WIDTH * (cp / maxCp));

        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF404040);
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, 0xFF2090FF);
        graphics.drawString(Minecraft.getInstance().font,
                "CP: " + (int) cp + " / " + (int) maxCp, x, y - 10, 0xFFFFFF);

        int nextY = y + BAR_HEIGHT + 2;

        // Une competence qui se charge ne montrerait rien du tout sans cette barre :
        // le joueur n'aurait aucun moyen de savoir que sa touche fait quelque chose,
        // ni quand la charge atteint son maximum.
        if (ClientCharge.isActive()) {
            int chargeFilled = Math.round(BAR_WIDTH * ClientCharge.getFraction());
            graphics.fill(x, nextY, x + BAR_WIDTH, nextY + BAR_HEIGHT, 0xFF404040);
            graphics.fill(x, nextY, x + chargeFilled, nextY + BAR_HEIGHT, 0xFFFF8020);
            graphics.drawString(Minecraft.getInstance().font, "CHARGE",
                    x + BAR_WIDTH + 4, nextY - 2, 0xFFB060);
            nextY += BAR_HEIGHT + 2;
        }

        // Un brouilleur empeche d'utiliser ses competences : sans cet avertissement
        // le joueur n'aurait aucun moyen de comprendre pourquoi ses touches ne
        // repondent plus.
        if (data.isInterfered()) {
            graphics.drawString(Minecraft.getInstance().font, "JAMMED", x, nextY, 0xFF5050);
        }
    }
}
