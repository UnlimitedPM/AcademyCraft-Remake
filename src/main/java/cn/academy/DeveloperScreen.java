package cn.academy;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.develop.condition.LearningCondition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ecran du developpeur d'aptitudes.
 *
 * Une liste de ce qu'on peut y apprendre : chaque categorie, puis les competences
 * qu'elle contient. Un bouton par ligne lance l'apprentissage correspondant.
 *
 * <h2>Pourquoi une liste qui defile</h2>
 *
 * Le port compte quatre categories et onze competences, soit quinze lignes : elles
 * ne tiennent pas sous une barre de progression et un inventaire. La zone de liste
 * garde donc une hauteur fixe et defile a la molette, plutot que de faire grandir
 * le panneau — un panneau de trois cents pixels deborderait de l'ecran a la plupart
 * des echelles d'interface.
 *
 * La hauteur fixe a un second merite : <b>la geometrie ne depend pas de ce que le
 * joueur a appris</b>. Le serveur et le client placent donc leurs emplacements au
 * meme endroit sans avoir a se mettre d'accord sur autre chose que le registre.
 *
 * <h2>Les lignes sont dessinees a la main</h2>
 *
 * Pas d'objet bouton par ligne : un bouton est un element d'interface a position
 * fixe, et il faudrait le deplacer a chaque cran de molette. Les lignes sont donc
 * dessinees, et le clic est calcule a partir de la meme position que le dessin, ce
 * qui rend les deux impossibles a desynchroniser. Le clic part ensuite par le
 * chemin vanilla des boutons de conteneur, comme {@code DeveloperMenu} l'attend.
 *
 * <h2>Griser n'est pas interdire</h2>
 *
 * Une ligne grisee l'est d'apres l'etat que le client connait ; le serveur
 * revalide tout au clic. Le client ne decide donc jamais de ce qui est appris — il
 * evite seulement de proposer l'impossible.
 */
public class DeveloperScreen extends AbstractContainerScreen<DeveloperMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIST_BG = 0xFFA8A8A8;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int DIM = 0xFF707070;
    private static final int ENERGY = 0xFFE0A030;
    private static final int PROGRESS = 0xFF4CAF50;
    private static final int OK = 0x2E7D32;
    private static final int OFF = 0x9E2B25;
    private static final int LEARNED = 0x2E7D32;
    private static final int BUTTON_ON = 0xFF6FA8DC;

    private static final int LIST_TOP = 18;
    private static final int LIST_BOTTOM = 88;
    private static final int LIST_LEFT = 22;
    private static final int LIST_RIGHT = 174;
    private static final int LIST_HEIGHT = LIST_BOTTOM - LIST_TOP;

    private static final int ROW_HEIGHT = 12;
    private static final int CATEGORY_X = 26;
    private static final int SKILL_X = 32;
    private static final int STATE_X = 74;
    private static final int LEVEL_X = 104;
    private static final int BAR_X_ROW = 124;
    private static final int BAR_WIDTH_ROW = 28;
    private static final int BAR_HEIGHT_ROW = 6;
    private static final int BUTTON_X = 156;
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 10;

    private static final int ENERGY_X = 8;
    private static final int ENERGY_WIDTH = 10;

    private static final int BAR_X = 24;
    private static final int BAR_Y = 96;
    private static final int BAR_WIDTH = 136;
    private static final int BAR_HEIGHT = 8;

    /** Une ligne de la liste : une categorie, ou une de ses competences. */
    private record Row(int categoryId, int skillId) {

        boolean isCategory() {
            return skillId < 0;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    /** Defilement courant, en pixels. */
    private int scroll;

    /** Ligne sous la souris, ou {@code null}. Recalculee a chaque image. */
    private Row hovered;

    public DeveloperScreen(DeveloperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        buildRows();
        scroll = 0;
    }

    /**
     * La liste des lignes, toujours la meme : toutes les categories, puis toutes
     * leurs competences. Rien n'est masque selon la progression, sinon la geometrie
     * dependrait de l'etat du joueur et les deux cotes pourraient ne plus tomber
     * d'accord sur la place des emplacements.
     */
    private void buildRows() {
        rows.clear();
        for (int categoryId = 0; categoryId < menu.getCategoryCount(); categoryId++) {
            Category category = menu.getCategory(categoryId);
            if (category == null) continue;
            rows.add(new Row(categoryId, -1));
            for (Skill skill : category.getSkills()) {
                rows.add(new Row(categoryId, skill.getId()));
            }
        }
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);

        drawEnergyBar(graphics);
        drawList(graphics, mouseX, mouseY);
        drawProgressBar(graphics);
    }

    private void drawEnergyBar(GuiGraphics graphics) {
        int x = leftPos + ENERGY_X;
        int y = topPos + LIST_TOP;
        graphics.fill(x, y, x + ENERGY_WIDTH, y + LIST_HEIGHT, HOLE);
        int max = Math.max(1, menu.getMaxEnergyStored());
        int filled = (int) (LIST_HEIGHT * Math.min(1.0f, (float) menu.getEnergyStored() / max));
        graphics.fill(x, y + LIST_HEIGHT - filled, x + ENERGY_WIDTH, y + LIST_HEIGHT, ENERGY);
    }

    private void drawProgressBar(GuiGraphics graphics) {
        int x = leftPos + BAR_X;
        int y = topPos + BAR_Y;
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, HOLE);
        int done = (int) (BAR_WIDTH * menu.getProgress());
        graphics.fill(x, y, x + done, y + BAR_HEIGHT, PROGRESS);
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(leftPos + LIST_LEFT, topPos + LIST_TOP,
                leftPos + LIST_RIGHT, topPos + LIST_BOTTOM, LIST_BG);

        hovered = null;
        int top = topPos + LIST_TOP;
        for (int i = 0; i < rows.size(); i++) {
            int y = top + i * ROW_HEIGHT - scroll;
            // Les lignes hors de la zone ne sont pas dessinees : c'est toute la
            // decoupe. Rien n'est dessine en dehors du cadre, donc rien ne depasse.
            if (y + ROW_HEIGHT <= top || y >= top + LIST_HEIGHT) continue;

            Row row = rows.get(i);
            boolean overRow = mouseY >= y && mouseY < y + ROW_HEIGHT
                    && mouseX >= leftPos + LIST_LEFT && mouseX < leftPos + LIST_RIGHT;
            if (overRow) hovered = row;

            if (row.isCategory()) {
                drawCategoryRow(graphics, row, y);
            } else {
                drawSkillRow(graphics, row, y);
            }
        }

        drawScrollBar(graphics);
    }

    private void drawCategoryRow(GuiGraphics graphics, Row row, int y) {
        Category category = menu.getCategory(row.categoryId());
        int level = menu.getCategoryLevel(row.categoryId());
        boolean active = menu.isDeveloping() && menu.getDevelopingCategory() == row.categoryId()
                && menu.getDevelopingSkill() < 0;
        boolean maxed = level >= menu.getMaxLevel();

        graphics.drawString(font, category == null ? "?" : category.getName(),
                leftPos + CATEGORY_X, y + 2, active ? OK : TEXT, false);
        graphics.drawString(font, level + "/" + menu.getMaxLevel(),
                leftPos + LEVEL_X, y + 2, level > 0 ? OK : DIM, false);

        // Le palier du niveau en cours. Sans lui, le bouton refuserait de faire monter
        // la categorie sans que rien n'explique pourquoi : le joueur croirait la
        // machine cassee, alors qu'il lui manque seulement d'avoir utilise ses
        // competences.
        if (!maxed) {
            float progress = menu.getLevelProgress(row.categoryId());
            drawBar(graphics, y, progress, menu.canLevelUp(row.categoryId()) ? OK : BUTTON_ON);
        }

        drawPlus(graphics, y, !maxed && menu.canLevelUp(row.categoryId()) && !menu.isDeveloping());
    }

    private void drawSkillRow(GuiGraphics graphics, Row row, int y) {
        Skill skill = menu.getSkill(row.categoryId(), row.skillId());
        if (skill == null) return;

        AbilityData data = ClientAbilityData.get();
        boolean learned = data.isSkillLearned(skill);
        boolean learnable = LearningHelper.canLearn(data, skill, menu.getDeveloperType());
        boolean active = menu.isDeveloping() && menu.getDevelopingCategory() == row.categoryId()
                && menu.getDevelopingSkill() == row.skillId();

        int colour = active ? OK : learned ? LEARNED : learnable ? TEXT : DIM;
        graphics.drawString(font, skill.getName(), leftPos + SKILL_X, y + 2, colour, false);
        graphics.drawString(font, "niv. " + skill.getLevel(), leftPos + STATE_X, y + 2, DIM, false);

        // L'experience de la competence : c'est elle qui remplit le palier du niveau,
        // donc c'est elle que le joueur doit voir pour comprendre ou il en est.
        if (learned || learnable) {
            drawBar(graphics, y, data.getSkillExp(skill), learned ? LEARNED : BUTTON_ON);
        }

        // L'apprentissage n'est propose que s'il a un sens. La ligne reste visible,
        // avec sa raison de ne pas l'etre dans l'infobulle.
        drawPlus(graphics, y, learnable && !learned && !menu.isDeveloping());
    }

    /**
     * La barre d'une ligne : le palier d'une categorie, ou l'experience d'une
     * competence.
     *
     * Les deux occupent la meme colonne, pour que l'oeil compare des choses
     * comparables : ce qui est rempli ici est ce qui ouvre la suite la-bas.
     */
    private void drawBar(GuiGraphics graphics, int y, float ratio, int colour) {
        int x = leftPos + BAR_X_ROW;
        int barY = y + 3;
        graphics.fill(x, barY, x + BAR_WIDTH_ROW, barY + BAR_HEIGHT_ROW, HOLE);
        int filled = (int) (BAR_WIDTH_ROW * Math.max(0f, Math.min(1f, ratio)));
        if (filled > 0) {
            graphics.fill(x, barY, x + filled, barY + BAR_HEIGHT_ROW, colour);
        }
    }

    /** La case « + » d'une ligne, dessinee pareil partout. */
    private void drawPlus(GuiGraphics graphics, int y, boolean actionable) {
        int x = leftPos + BUTTON_X;
        int boxY = y + (ROW_HEIGHT - BUTTON_HEIGHT) / 2;
        graphics.fill(x, boxY, x + BUTTON_WIDTH, boxY + BUTTON_HEIGHT, HOLE);
        if (actionable) {
            graphics.fill(x + 1, boxY + 1, x + BUTTON_WIDTH - 1, boxY + BUTTON_HEIGHT - 1, BUTTON_ON);
        }
        graphics.drawString(font, "+", x + 5, boxY + 1,
                actionable ? 0xFFFFFFFF : 0xFF909090, false);
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int max = maxScroll();
        if (max <= 0) return;

        int x = leftPos + LIST_RIGHT - 2;
        int top = topPos + LIST_TOP;
        graphics.fill(x, top, x + 2, top + LIST_HEIGHT, HOLE);
        int content = rows.size() * ROW_HEIGHT;
        int barHeight = Math.max(8, LIST_HEIGHT * LIST_HEIGHT / Math.max(1, content));
        int barTop = top + (LIST_HEIGHT - barHeight) * scroll / max;
        graphics.fill(x, barTop, x + 2, barTop + barHeight, BUTTON_ON);
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW_HEIGHT - LIST_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int amount = (int) (-delta * ROW_HEIGHT * 2);
            scroll = Math.max(0, Math.min(maxScroll(), scroll + amount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered != null && isActionable(hovered)) {
            int x = leftPos + BUTTON_X;
            int y = rowY(hovered) + (ROW_HEIGHT - BUTTON_HEIGHT) / 2;
            if (mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= y && mouseY < y + BUTTON_HEIGHT) {
                develop(buttonIdOf(hovered));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** La position verticale d'une ligne, en coordonnees d'ecran. */
    private int rowY(Row row) {
        return topPos + LIST_TOP + rows.indexOf(row) * ROW_HEIGHT - scroll;
    }

    private boolean isActionable(Row row) {
        if (menu.isDeveloping()) return false;
        if (row.isCategory()) {
            return menu.getCategoryLevel(row.categoryId()) < menu.getMaxLevel();
        }
        Skill skill = menu.getSkill(row.categoryId(), row.skillId());
        if (skill == null) return false;
        AbilityData data = ClientAbilityData.get();
        return !data.isSkillLearned(skill)
                && LearningHelper.canLearn(data, skill, menu.getDeveloperType());
    }

    private int buttonIdOf(Row row) {
        if (row.isCategory()) return row.categoryId();
        Skill skill = menu.getSkill(row.categoryId(), row.skillId());
        return skill == null ? -1 : DeveloperMenu.skillButton(skill);
    }

    /**
     * Les boutons de conteneur passent par le chemin vanilla : aucun paquet
     * maison, le clic est route vers {@code DeveloperMenu.clickMenuButton}.
     */
    private void develop(int buttonId) {
        if (buttonId < 0) return;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    // ------------------------------------------------------------------
    // Textes
    // ------------------------------------------------------------------

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);
        graphics.drawString(font, statusText(), BAR_X, BAR_Y + BAR_HEIGHT + 4,
                menu.getState() == cn.academy.ability.develop.DevelopProgress.DevState.FAILED ? OFF : TEXT, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }

    /** Message d'etat, dans la langue du joueur. */
    private Component statusText() {
        cn.academy.ability.develop.DevelopProgress.DevState state = menu.getState();
        if (state == cn.academy.ability.develop.DevelopProgress.DevState.DEVELOPING) {
            return Component.translatable("academy.developer.state.developing")
                    .append(" : " + (long) (menu.getProgress() * 100) + " %");
        }
        return Component.translatable("academy.developer.state."
                + state.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (hovered != null) {
            List<Component> lines = tooltipFor(hovered);
            if (!lines.isEmpty()) {
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** Ce que dit une ligne au survol : son prix, ou pourquoi elle est bloquee. */
    private List<Component> tooltipFor(Row row) {
        List<Component> lines = new ArrayList<>();

        if (row.isCategory()) {
            Category category = menu.getCategory(row.categoryId());
            int index = row.categoryId();
            lines.add(Component.literal(category == null ? "?" : category.getName()));

            if (menu.getCategoryLevel(index) >= menu.getMaxLevel()) {
                lines.add(Component.translatable("academy.developer.maxed").withStyle(ChatFormatting.GRAY));
                return lines;
            }
            if (!menu.canLevelUp(index)) {
                // La raison exacte du refus, en clair : c'est la seule chose qui
                // manque au joueur pour comprendre ce qu'il doit faire.
                lines.add(Component.translatable("academy.developer.progress",
                        (int) (menu.getLevelProgress(index) * 100)).withStyle(ChatFormatting.RED));
            }
            lines.add(Component.translatable("academy.developer.cost",
                    menu.getStimulationsFor(index), (long) menu.getCostFor(index)));
            return lines;
        }

        Skill skill = menu.getSkill(row.categoryId(), row.skillId());
        if (skill == null) return lines;

        lines.add(skill.getDisplayName());
        lines.add(Component.translatable("academy.developer.cost", skill.getLearningStims(),
                (long) menu.getDeveloperType().getTotalCost(skill.getLearningStims())));

        AbilityData data = ClientAbilityData.get();
        if (data.isSkillLearned(skill)) {
            int percent = (int) (data.getSkillExp(skill) * 100f);
            lines.add(Component.translatable("academy.developer.skill_exp", percent)
                    .withStyle(percent >= 100 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            return lines;
        }

        // La raison exacte du blocage : l'ecran ne rejoue pas les regles, il demande
        // a la meme condition que le serveur laquelle ne passe pas.
        LearningCondition blocker = LearningHelper.firstBlocker(data, skill,
                menu.getDeveloperType());
        if (blocker != null) {
            lines.add(blocker.describe(skill).copy().withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
