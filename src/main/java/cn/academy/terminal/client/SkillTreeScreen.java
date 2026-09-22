package cn.academy.terminal.client;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.develop.DevelopActionLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * L'application Arbre de competences : les categories, leur niveau, et les points
 * de controle du joueur.
 *
 * <h2>Ce que cet ecran n'est pas</h2>
 *
 * L'original dessinait un vrai arbre : des noeuds relies par des arcs, avec
 * l'experience de chaque competence et une icone par competence. Le port n'a ni
 * experience par competence ni icones de competence, donc un arbre n'aurait rien
 * a dessiner. Ce qui est montre ici est le resume que le port peut reellement
 * remplir : par categorie, le niveau atteint sur les cinq possibles.
 *
 * C'est un ecran honnete, pas une reproduction : quand la progression par
 * experience sera portee (voir {@code DevelopActionLevel}), c'est ici qu'il
 * faudra la dessiner.
 */
@OnlyIn(Dist.CLIENT)
public class SkillTreeScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int ROW_HEIGHT = 18;
    private static final int HEADER = 28;

    private static final int PANEL = 0xF0202020;
    private static final int PANEL_EDGE = 0xFF6FA8DC;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF808080;
    private static final int LEARNED = 0xFF7FD37F;
    private static final int CP = 0xFFE0A030;
    private static final int BAR_BG = 0xFF373737;

    private int leftPos;
    private int topPos;
    private int panelHeight;

    public SkillTreeScreen() {
        super(Component.translatable("ac.app.skill_tree.name"));
    }

    @Override
    protected void init() {
        panelHeight = HEADER + Math.max(1, categories().size()) * ROW_HEIGHT + 26;
        this.leftPos = (width - PANEL_WIDTH) / 2;
        this.topPos = (height - panelHeight) / 2;
    }

    private List<Category> categories() {
        return CategoryManager.INSTANCE.getCategories();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fill(leftPos - 1, topPos - 1, leftPos + PANEL_WIDTH + 1, topPos + panelHeight + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + panelHeight, PANEL);

        graphics.drawString(font, title, leftPos + 8, topPos + 7, TEXT, false);

        List<Category> categories = categories();
        if (categories.isEmpty()) {
            graphics.drawString(font, Component.literal("?"), leftPos + 8, topPos + HEADER, DIM, false);
        }

        int y = topPos + HEADER;
        for (Category category : categories) {
            int level = ClientAbilityData.get().getCategoryLevel(category);
            graphics.drawString(font, Component.translatable(category.getDisplayKey()),
                    leftPos + 10, y + 4, level > 0 ? TEXT : DIM, false);
            graphics.drawString(font, Component.literal(level + " / " + DevelopActionLevel.MAX_LEVEL),
                    leftPos + PANEL_WIDTH - 44, y + 4, level > 0 ? LEARNED : DIM, false);
            y += ROW_HEIGHT;
        }

        drawControlPoint(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * La barre des points de controle, en bas.
     *
     * L'original l'affichait dans le HUD ; ici elle est utile au meme titre, parce
     * que c'est la ressource que consomment les competences actives.
     */
    private void drawControlPoint(GuiGraphics graphics) {
        var data = ClientAbilityData.get();
        float current = data.getControlPoint();
        float max = Math.max(1f, data.getMaxControlPoint());

        int barY = topPos + panelHeight - 18;
        int barX = leftPos + 10;
        int barWidth = PANEL_WIDTH - 20;

        graphics.drawString(font, Component.literal((long) current + " / " + (long) max + " CP"),
                barX, barY - 10, TEXT, false);
        graphics.fill(barX, barY, barX + barWidth, barY + 6, BAR_BG);
        int filled = (int) (barWidth * Math.min(1f, current / max));
        graphics.fill(barX, barY, barX + filled, barY + 6, CP);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
