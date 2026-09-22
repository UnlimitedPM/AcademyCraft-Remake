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
 * L'ecran qui regle les prereglages, ouvert par la touche dediee (et, plus tard, par
 * l'application Reglages du terminal).
 *
 * <p>Portage de {@code PresetEditUI}, en plus simple : l'original faisait du glisser-deposer,
 * le port laisse <b>choisir</b> une competence dans la liste apprise, puis <b>cliquer</b> la
 * touche ou on la veut. Un clic droit sur une touche la libere. C'est la meme idee — on
 * montre des competences, on les pose sur quatre touches — sans le suivi du curseur.
 *
 * <p>Rien n'est decide ici : chaque clic envoie une demande au serveur, qui range et renvoie
 * l'etat complet. L'ecran ne fait donc que lire {@code ClientPresetData}, ce qui rend une
 * divergence impossible.
 */
@OnlyIn(Dist.CLIENT)
public class PresetEditScreen extends Screen {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int HOLE = 0xFF373737;
    private static final int TEXT = 0x404040;
    private static final int DIM = 0xFF707070;
    private static final int SELECTED = 0xFF4CAF50;
    private static final int CURRENT = 0xFFFFD54F;
    private static final int BUTTON_ON = 0xFF6FA8DC;

    private static final int SKILL_TOP = 40;
    private static final int SKILL_ROW = 12;
    private static final int SKILL_VISIBLE = 14;
    private static final int SKILL_WIDTH = 120;

    private static final int GRID_X = 150;
    private static final int GRID_Y = 40;
    private static final int CELL_WIDTH = 52;
    private static final int CELL_HEIGHT = 16;
    private static final int ROW_GAP = 4;

    /** Les competences apprises, dans l'ordre des categories. */
    private final List<Skill> learned = new ArrayList<>();

    /** Celle qu'on s'apprete a poser, ou {@code null}. */
    @Nullable
    private Skill picked;

    private int scroll;

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
                if (data.isSkillLearned(skill)) learned.add(skill);
            }
        }
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        PresetData data = ClientPresetData.get();
        int left = (width - 280) / 2;
        int top = 20;
        graphics.fill(left - 8, top - 12, left + 288, top + 200, PANEL);

        graphics.drawString(font, title, left, top, TEXT, false);
        graphics.drawString(font, Component.translatable("academy.preset.hint"), left,
                top + 12, DIM, false);

        drawSkillList(graphics, left, mouseX, mouseY);
        drawGrid(graphics, left, top, data, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSkillList(GuiGraphics graphics, int left, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("academy.preset.learned"), left,
                SKILL_TOP - 12, TEXT, false);

        graphics.fill(left, SKILL_TOP, left + SKILL_WIDTH, SKILL_TOP + SKILL_VISIBLE * SKILL_ROW,
                HOLE);

        for (int i = 0; i < SKILL_VISIBLE; i++) {
            int index = i + scroll;
            if (index >= learned.size()) break;
            Skill skill = learned.get(index);
            int y = SKILL_TOP + i * SKILL_ROW;
            boolean hovered = isIn(mouseX, mouseY, left, y, SKILL_WIDTH, SKILL_ROW);
            if (skill == picked) {
                graphics.fill(left, y, left + SKILL_WIDTH, y + SKILL_ROW, SELECTED);
            } else if (hovered) {
                graphics.fill(left, y, left + SKILL_WIDTH, y + SKILL_ROW, BUTTON_ON);
            }
            graphics.drawString(font, skill.getDisplayName(), left + 3, y + 2,
                    skill == picked ? 0xFFFFFFFF : TEXT, false);
        }

        if (learned.isEmpty()) {
            graphics.drawString(font, Component.translatable("academy.preset.none"), left + 3,
                    SKILL_TOP + 2, DIM, false);
        }
    }

    private void drawGrid(GuiGraphics graphics, int left, int top, PresetData data,
                          int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("academy.preset.slots"), left + GRID_X,
                SKILL_TOP - 12, TEXT, false);

        for (int preset = 0; preset < PresetData.MAX_PRESETS; preset++) {
            int y = top + GRID_Y + preset * (CELL_HEIGHT + ROW_GAP);
            boolean current = preset == data.getCurrentId();

            // Le numero du prereglage : cliquer dessus le met en service, comme la touche
            // de changement le ferait.
            graphics.fill(left + GRID_X - 16, y, left + GRID_X - 4, y + CELL_HEIGHT, HOLE);
            graphics.drawString(font, Integer.toString(preset + 1), left + GRID_X - 12, y + 4,
                    current ? CURRENT : TEXT, false);

            for (int key = 0; key < AbilityPreset.MAX_KEYS; key++) {
                int x = left + GRID_X + key * (CELL_WIDTH + 2);
                graphics.fill(x, y, x + CELL_WIDTH, y + CELL_HEIGHT, HOLE);
                if (isIn(mouseX, mouseY, x, y, CELL_WIDTH, CELL_HEIGHT)) {
                    graphics.fill(x + 1, y + 1, x + CELL_WIDTH - 1, y + CELL_HEIGHT - 1, BUTTON_ON);
                }

                String name = data.getPreset(preset).nameAt(key);
                Skill skill = name == null ? null : skillByName(name);
                graphics.drawString(font,
                        skill == null ? Component.literal("-") : skill.getDisplayName(),
                        x + 3, y + 4, skill == null ? DIM : TEXT, false);
            }
        }
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft == null || minecraft.player == null) return super.mouseClicked(mouseX, mouseY, button);

        int left = (width - 280) / 2;
        int top = 20;

        // La liste des competences : un clic choisit, un second la repose.
        if (isIn(mouseX, mouseY, left, SKILL_TOP, SKILL_WIDTH, SKILL_VISIBLE * SKILL_ROW)) {
            int index = scroll + (int) ((mouseY - SKILL_TOP) / SKILL_ROW);
            if (index >= 0 && index < learned.size()) {
                Skill skill = learned.get(index);
                picked = skill == picked ? null : skill;
            }
            return true;
        }

        // Le numero d'un prereglage : le mettre en service.
        for (int preset = 0; preset < PresetData.MAX_PRESETS; preset++) {
            int y = top + GRID_Y + preset * (CELL_HEIGHT + ROW_GAP);
            if (isIn(mouseX, mouseY, left + GRID_X - 16, y, 12, CELL_HEIGHT)) {
                send(PresetActionPacket.switchTo(preset));
                return true;
            }

            for (int key = 0; key < AbilityPreset.MAX_KEYS; key++) {
                int x = left + GRID_X + key * (CELL_WIDTH + 2);
                if (!isIn(mouseX, mouseY, x, y, CELL_WIDTH, CELL_HEIGHT)) continue;

                // Clic droit : la touche se libere. Clic gauche : on y pose la competence
                // choisie, ou on la libere si rien n'est choisi.
                String name = button == 1 ? null
                        : (picked == null ? null : picked.getName());
                send(PresetActionPacket.assign(preset, key, name));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, learned.size() - SKILL_VISIBLE);
        scroll = Math.max(0, Math.min(max, scroll - (int) delta));
        return true;
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
