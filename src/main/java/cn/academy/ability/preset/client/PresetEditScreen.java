package cn.academy.ability.preset.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.preset.AbilityPreset;
import cn.academy.ability.preset.PresetData;
import cn.academy.ability.preset.network.PresetActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran qui regle les prereglages, ouvert par la touche dediee (et par l'application
 * Reglages du terminal).
 *
 * <p>Portage de {@code PresetEditUI}. L'original faisait du glisser-deposer, le port laisse
 * <b>choisir</b> une competence dans la grille, puis <b>cliquer</b> la touche ou on la veut —
 * c'est la meme idee, on montre des competences et on les pose sur quatre touches, sans le
 * suivi du curseur. Un clic droit sur une touche la libere.
 *
 * <p>La mise en page est celle de l'original : <b>quatre panneaux</b> « Preset #1 » a
 * « Preset #4 », celui en service plus clair que les autres, quatre lignes par panneau — une
 * par touche d'aptitude — et l'icone de la competence posee a cote de son nom. Cliquer un
 * numero met ce prereglage en service.
 *
 * <p>La grille de choix reprend aussi ses mesures : cases de 15 pixels, pas de 18, quatre par
 * rangee. Elle ne montre que les competences <b>apprises</b>, comme chez lui.
 *
 * <p>Rien n'est decide ici : chaque clic envoie une demande au serveur, qui range et renvoie
 * l'etat complet. L'ecran ne fait donc que lire {@code ClientPresetData}, ce qui rend une
 * divergence impossible.
 */
@OnlyIn(Dist.CLIENT)
public class PresetEditScreen extends Screen {

    private static final int PANEL_W = 84;
    private static final int PANEL_H = 100;
    private static final int PANEL_GAP = 12;
    private static final int PANEL_TOP = 46;

    private static final int PANEL_BACK = 0xFF333333;
    private static final int PANEL_BACK_CURRENT = 0xFF4A4A4A;
    private static final int PANEL_EDGE = 0xFF808080;
    private static final int PANEL_EDGE_CURRENT = 0xFFFFFFFF;
    private static final int SLOT_BACK = 0xFF222222;
    private static final int SLOT_HOVER = 0xFF6FA8DC;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM = 0xFF9A9A9A;

    private static final int SLOT = 18;
    private static final int SLOT_STEP = 22;
    private static final int SLOT_X = 6;
    private static final int SLOT_TOP = 8;
    private static final int ICON = 16;

    /** Les mesures de la grille de choix, celles de l'original. */
    private static final int PICK_SIZE = 15;
    private static final int PICK_STEP = 18;
    private static final int PICK_PER_ROW = 4;
    private static final int PICK_MARGIN = 3;

    /** Les competences apprises, dans l'ordre des categories. */
    private final List<Skill> learned = new ArrayList<>();

    /** Le panneau et la touche dont la grille de choix est ouverte, ou -1. */
    private int pickPreset = -1;
    private int pickKey = -1;

    public PresetEditScreen() {
        super(Component.translatable("academy.preset.title"));
    }

    @Override
    protected void init() {
        super.init();
        learned.clear();
        if (minecraft == null || minecraft.player == null) return;

        AbilityData data = ClientAbilityData.get();
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            for (Skill skill : category.getSkills()) {
                // Une competence passive ne se range pas sur une touche : l'original ne la
                // proposait pas non plus (`canControl`). Sans ce filtre, l'editeur laisse
                // croire qu'on peut lancer un cours de cerveau.
                if (!skill.canControl()) continue;
                if (data.isSkillLearned(skill)) learned.add(skill);
            }
        }
    }

    /** Le bord gauche du premier panneau : les quatre sont centres sur l'ecran. */
    private int panelsLeft() {
        return (width - (PresetData.MAX_PRESETS * PANEL_W
                + (PresetData.MAX_PRESETS - 1) * PANEL_GAP)) / 2;
    }

    private int panelLeft(int preset) {
        return panelsLeft() + preset * (PANEL_W + PANEL_GAP);
    }

    private int slotTop(int key) {
        return PANEL_TOP + SLOT_TOP + key * SLOT_STEP;
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        PresetData data = ClientPresetData.get();
        // Le titre, en haut a gauche, comme l'original.
        graphics.drawString(font, Component.literal("Preset Edit"), 8, 8, WHITE, true);

        for (int preset = 0; preset < PresetData.MAX_PRESETS; preset++) {
            drawPanel(graphics, data, preset, mouseX, mouseY);
        }
        drawPicker(graphics, data, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics, PresetData data, int preset,
                           int mouseX, int mouseY) {
        int left = panelLeft(preset);
        boolean current = preset == data.getCurrentId();

        // Le numero, au-dessus du panneau : cliquer dessus met ce prereglage en service.
        Component number = Component.literal("Preset #" + (preset + 1));
        graphics.drawString(font, number, left + (PANEL_W - font.width(number)) / 2,
                PANEL_TOP - 12, current ? WHITE : DIM, true);

        graphics.fill(left, PANEL_TOP, left + PANEL_W, PANEL_TOP + PANEL_H,
                current ? PANEL_BACK_CURRENT : PANEL_BACK);
        graphics.renderOutline(left, PANEL_TOP, PANEL_W, PANEL_H,
                current ? PANEL_EDGE_CURRENT : PANEL_EDGE);

        for (int key = 0; key < AbilityPreset.MAX_KEYS; key++) {
            int x = left + SLOT_X;
            int y = slotTop(key);
            boolean hovered = current && isIn(mouseX, mouseY, x, y, PANEL_W - SLOT_X * 2, SLOT);

            graphics.fill(x, y, x + SLOT, y + SLOT,
                    hovered ? SLOT_HOVER : SLOT_BACK);

            String name = data.getPreset(preset).nameAt(key);
            Skill skill = name == null ? null : skillByName(name);
            if (skill == null) continue;

            graphics.blit(skill.getHintIcon(), x + 1, y + 1, ICON, ICON, 0.0f, 0.0f,
                    ICON, ICON, ICON, ICON);

            // Le nom, coupe au bord du panneau : l'original le laissait deborder sur le
            // suivant, ce qui marchait chez lui et donne ici un fouillis.
            int textLeft = x + SLOT + 2;
            graphics.enableScissor(textLeft, y, left + PANEL_W - 2, y + SLOT);
            graphics.drawString(font, skill.getDisplayName(), textLeft, y + 5, WHITE, false);
            graphics.disableScissor();
        }
    }

    /** La grille de choix, ouverte par un clic sur une touche du prereglage en service. */
    private void drawPicker(GuiGraphics graphics, PresetData data, int mouseX, int mouseY) {
        if (pickPreset < 0) return;

        int gridW = pickerWidth();
        int gridH = pickerHeight();
        int left = pickerLeft();
        int top = pickerTop();

        graphics.fill(left, top, left + gridW, top + gridH, 0xFF2A2A2A);
        graphics.renderOutline(left, top, gridW, gridH, PANEL_EDGE_CURRENT);

        Component title = Component.translatable("academy.preset.select");
        graphics.drawString(font, title, left + (gridW - font.width(title)) / 2, top + PICK_MARGIN,
                WHITE, false);

        if (learned.isEmpty()) {
            graphics.drawString(font, Component.translatable("academy.preset.none"),
                    left + PICK_MARGIN, top + PICK_MARGIN + 14, DIM, false);
            return;
        }

        for (int i = 0; i < learned.size(); i++) {
            int x = pickLeft(left, i);
            int y = pickTop(top, i);
            if (isIn(mouseX, mouseY, x, y, PICK_SIZE, PICK_SIZE)) {
                graphics.fill(x - 1, y - 1, x + PICK_SIZE + 1, y + PICK_SIZE + 1, SLOT_HOVER);
            }
            graphics.blit(learned.get(i).getHintIcon(), x, y, PICK_SIZE, PICK_SIZE, 0.0f, 0.0f,
                    ICON, ICON, ICON, ICON);
        }
    }

    private static int pickLeft(int gridLeft, int index) {
        return gridLeft + PICK_MARGIN + (index % PICK_PER_ROW) * PICK_STEP;
    }

    private static int pickTop(int gridTop, int index) {
        return gridTop + PICK_MARGIN + 14 + (index / PICK_PER_ROW) * PICK_STEP;
    }

    private int pickerRows() {
        return Math.max(1, (learned.size() + PICK_PER_ROW - 1) / PICK_PER_ROW);
    }

    private int pickerWidth() {
        return PICK_MARGIN * 2 + (PICK_PER_ROW - 1) * PICK_STEP + PICK_SIZE;
    }

    private int pickerHeight() {
        return PICK_MARGIN * 2 + 14 + (pickerRows() - 1) * PICK_STEP + PICK_SIZE;
    }

    private int pickerLeft() {
        return panelLeft(pickPreset) + (PANEL_W - pickerWidth()) / 2;
    }

    private int pickerTop() {
        return Math.max(4, slotTop(pickKey) - pickerHeight() / 2);
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        PresetData data = ClientPresetData.get();

        // La grille de choix ouverte : un clic y pose la competence, un clic a cote la referme.
        if (pickPreset >= 0) {
            for (int i = 0; i < learned.size(); i++) {
                int x = pickLeft(pickerLeft(), i);
                int y = pickTop(pickerTop(), i);
                if (!isIn(mouseX, mouseY, x, y, PICK_SIZE, PICK_SIZE)) continue;
                send(PresetActionPacket.assign(pickPreset, pickKey, learned.get(i).getName()));
                closePicker();
                return true;
            }

            if (!isIn(mouseX, mouseY, pickerLeft(), pickerTop(), pickerWidth(), pickerHeight())) {
                closePicker();
            } else {
                return true;
            }
        }

        // Le numero d'un prereglage : cliquer dessus le met en service, comme la touche de
        // changement le ferait. C'est une commodite du port, l'original le faisait ailleurs.
        for (int preset = 0; preset < PresetData.MAX_PRESETS; preset++) {
            Component number = Component.literal("Preset #" + (preset + 1));
            int labelLeft = panelLeft(preset) + (PANEL_W - font.width(number)) / 2;
            if (isIn(mouseX, mouseY, labelLeft, PANEL_TOP - 12, font.width(number), 10)) {
                send(PresetActionPacket.switchTo(preset));
                return true;
            }
        }

        // Une touche d'un panneau. Seul celui en service se regle ; les autres se lisent.
        for (int preset = 0; preset < PresetData.MAX_PRESETS; preset++) {
            for (int key = 0; key < AbilityPreset.MAX_KEYS; key++) {
                int x = panelLeft(preset) + SLOT_X;
                int y = slotTop(key);
                if (!isIn(mouseX, mouseY, x, y, PANEL_W - SLOT_X * 2, SLOT)) continue;
                if (preset != data.getCurrentId()) return true;

                if (button == 1) {
                    send(PresetActionPacket.assign(preset, key, null));
                } else {
                    pickPreset = preset;
                    pickKey = key;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void closePicker() {
        pickPreset = -1;
        pickKey = -1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isIn(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Le serveur range et renvoie l'etat : l'ecran ne decide rien. */
    private static void send(PresetActionPacket packet) {
        AbilityNetwork.CHANNEL.sendToServer(packet);
    }

    @Nullable
    private static Skill skillByName(String name) {
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            Skill skill = category.getSkill(name);
            if (skill != null) return skill;
        }
        return null;
    }
}

