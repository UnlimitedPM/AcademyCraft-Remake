package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.client.hud.CpBarHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Les valeurs du joueur, sous forme de barres, et le temoin de points de controle.
 *
 * <p>Le <b>temoin</b> est l'element du HUD : il se dessine a la place que le joueur lui a
 * donnee (voir {@link CpBarHud} et {@code HudElement}).
 *
 * <p>Les <b>valeurs</b> — les nombres de CP et de surcharge, avec leurs petites barres — sont
 * autre chose : l'original les tient en <b>bas a gauche</b>, fixes, et non dans un des quatre
 * elements reglables. Elles restent donc ici, en bas a gauche, ou elles etaient.
 */
public class AbilityHudOverlay implements IGuiOverlay {

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        CpBarHud.render(graphics, screenWidth, screenHeight);

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

        // Le surcout, sous les CP : une seconde reserve qui se remplit a chaque
        // activation et qui bloque tout quand elle est pleine. Toujours affichee, comme
        // la barre de l'original, pour que le joueur voie venir la surcharge au lieu de
        // la decouvrir en pleine action.
        float maxOverload = data.getMaxOverload();
        if (maxOverload > 0) {
            float overload = data.getOverload();
            int overloadFilled = Math.round(BAR_WIDTH * (overload / maxOverload));
            graphics.fill(x, nextY, x + BAR_WIDTH, nextY + BAR_HEIGHT, 0xFF404040);
            if (overloadFilled > 0) {
                graphics.fill(x, nextY, x + overloadFilled, nextY + BAR_HEIGHT,
                        data.isOverloaded() ? 0xFFFF3030 : 0xFFFFA030);
            }
            graphics.drawString(Minecraft.getInstance().font,
                    "Overload: " + (int) overload + " / " + (int) maxOverload,
                    x + BAR_WIDTH + 4, nextY - 2, 0xFFC080);
            nextY += BAR_HEIGHT + 2;
        }

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
        // repondent plus. La surcharge, elle, a sa barre rouge.
        if (data.isInterfered()) {
            graphics.drawString(Minecraft.getInstance().font, "JAMMED", x, nextY, 0xFF5050);
        }
    }
}
