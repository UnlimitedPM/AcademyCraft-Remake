package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.LocationTeleportPacket;
import cn.academy.ability.teleporter.LocationMark;
import cn.academy.ability.teleporter.LocationTeleportSkill;
import cn.academy.ability.teleporter.TeleporterCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * La liste des endroits marques, portage de l'ecran {@code loctele} de l'original.
 *
 * <p>C'est la seule competence dont la touche ouvre un ecran, et l'ecran est donc tout son
 * geste : on y marque l'endroit ou l'on se trouve, on y oublie une marque, et on y clique
 * celle ou l'on veut aller.
 *
 * <h2>Ce que l'ecran ne decide pas</h2>
 *
 * Il <b>grise</b> ce qui ne marchera pas — une marque d'une autre dimension, ou un voyage que
 * la reserve ne peut pas payer — et il dit pourquoi, comme l'original qui affichait une
 * ligne d'explication sous la liste. Mais il ne fait que prevenu : c'est le serveur qui paie
 * et qui deplace, et lui seul connait la reserve au point pres. Un ecran qui refuserait a
 * tort serait pire qu'un ecran qui laisse essayer.
 *
 * <p>La geometrie est fixe et dessinee a la main, comme les autres ecrans du port : une zone
 * qui defile a la molette, une ligne par marque, et une barre de defilement.
 */
public class LocationTeleportScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 150;
    private static final int ROW_HEIGHT = 14;
    private static final int LIST_TOP = 30;
    private static final int LIST_HEIGHT = 96;
    private static final int BAR_WIDTH = 3;

    private static final int BACKGROUND = 0xF0101014;
    private static final int FRAME = 0xFF6FA8DC;
    private static final int ROW = 0x20FFFFFF;
    private static final int ROW_HOVER = 0x40FFFFFF;
    private static final int TEXT = 0xFFC1CFD5;
    private static final int TEXT_OFF = 0xFF7A7A7A;
    private static final int TEXT_BAD = 0xFFE08080;

    private final Category category;
    private final Skill skill;

    private EditBox nameField;
    private int scroll;

    public LocationTeleportScreen(Category category, Skill skill) {
        super(skill.getDisplayName());
        this.category = category;
        this.skill = skill;
    }

    private static AbilityData data() {
        return ClientAbilityData.get();
    }

    private static LocationTeleportSkill teleport() {
        return TeleporterCategory.LOCATION_TELEPORT;
    }

    private List<LocationMark> marks() {
        AbilityData data = data();
        return data == null ? List.of() : data.getMarks();
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();

        nameField = new EditBox(font, left + 8, top + PANEL_HEIGHT - 22, 120, 14,
                Component.translatable("ac.ability.teleporter.location_teleport.name_hint"));
        nameField.setMaxLength(LocationMark.MAX_NAME);
        nameField.setHint(Component.translatable(
                "ac.ability.teleporter.location_teleport.name_hint"));
        addRenderableWidget(nameField);

        addRenderableWidget(Button.builder(
                        Component.translatable("ac.ability.teleporter.location_teleport.mark_here"),
                        button -> send(LocationTeleportPacket.Action.ADD, -1, nameField.getValue()))
                .bounds(left + 134, top + PANEL_HEIGHT - 23, 78, 16)
                .build());
    }

    private static void send(LocationTeleportPacket.Action action, int markId, String name) {
        AbilityNetwork.CHANNEL.sendToServer(new LocationTeleportPacket(
                teleport().getCategory().getCategoryId(), teleport().getId(), action, markId, name));
    }

    private int rowY(int index) {
        return top() + LIST_TOP + index * ROW_HEIGHT - scroll;
    }

    private int maxScroll() {
        return Math.max(0, marks().size() * ROW_HEIGHT - LIST_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(delta * ROW_HEIGHT)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Les boutons ont la priorite : sans cela, cliquer le champ de saisie ou le bouton
        // d'ajout pourrait aussi toucher la ligne qui se trouve dessous.
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        int left = left();
        if (mouseX < left + 6 || mouseX > left + PANEL_WIDTH - 6) return false;

        List<LocationMark> marks = marks();
        for (int i = 0; i < marks.size(); i++) {
            int y = rowY(i);
            if (mouseY < y || mouseY >= y + ROW_HEIGHT) continue;
            if (mouseY < top() + LIST_TOP || mouseY >= top() + LIST_TOP + LIST_HEIGHT) return false;

            // Le petit carre a droite de la ligne oublie la marque ; le reste de la ligne
            // part vers elle.
            if (mouseX >= left + PANEL_WIDTH - 20) {
                send(LocationTeleportPacket.Action.REMOVE, i, "");
            } else if (canTravel(i, marks.get(i), data())) {
                send(LocationTeleportPacket.Action.PERFORM, i, "");
                onClose();
            }
            return true;
        }
        return false;
    }

    /**
     * Ce voyage est-il jouable ?
     *
     * L'ecran reprend les deux refus de l'original : la dimension qu'on n'a pas le droit de
     * traverser, et la reserve qui ne suffit pas. Le prix se calcule avec la distance au
     * carre, donc il grandit avec la distance reelle a vol d'oiseau.
     */
    private static boolean canTravel(int id, LocationMark mark, AbilityData data) {
        if (data == null) return false;
        LocationTeleportSkill skill = teleport();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        boolean cross = minecraft.player != null
                && !LocationMark.of(minecraft.player.level()).equals(mark.dimension());
        if (cross) return skill.canCrossDimension(data);

        double distance = Math.sqrt(minecraft.player.position()
                .distanceToSqr(mark.x(), mark.y(), mark.z()));
        return data.getControlPoint() >= skill.cpCost(data, distance, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int left = left();
        int top = top();
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, BACKGROUND);
        graphics.drawString(font, Component.translatable("ac.ui.location_teleport.title"),
                left + 8, top + 8, FRAME, false);

        AbilityData data = data();
        List<LocationMark> marks = marks();

        if (marks.isEmpty()) {
            graphics.drawString(font, Component.translatable("ac.ui.location_teleport.empty"),
                    left + 8, top + LIST_TOP, TEXT_OFF, false);
        } else {
            // La liste est decoupee aux bords : une ligne a moitie sortie se verrait au-dessus
            // du titre ou sous les boutons, et l'ecran a une geometrie fixe.
            graphics.enableScissor(left, top + LIST_TOP, left + PANEL_WIDTH, top + LIST_TOP + LIST_HEIGHT);
            for (int i = 0; i < marks.size(); i++) {
                drawRow(graphics, i, marks.get(i), data, mouseX, mouseY);
            }
            graphics.disableScissor();
        }

        drawScrollBar(graphics);
        drawHint(graphics, marks, data);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRow(GuiGraphics graphics, int index, LocationMark mark, AbilityData data,
                         int mouseX, int mouseY) {
        int left = left();
        int y = rowY(index);
        if (y + ROW_HEIGHT < top() + LIST_TOP || y > top() + LIST_TOP + LIST_HEIGHT) return;

        boolean hovered = mouseX >= left + 4 && mouseX <= left + PANEL_WIDTH - 4
                && mouseY >= y && mouseY < y + ROW_HEIGHT;
        graphics.fill(left + 4, y, left + PANEL_WIDTH - 4, y + ROW_HEIGHT,
                hovered ? ROW_HOVER : ROW);

        boolean allowed = canTravel(index, mark, data);
        int colour = allowed ? TEXT : TEXT_OFF;
        graphics.drawString(font, mark.name(), left + 8, y + 3, colour, false);

        // Les coordonnees et la dimension, sur la meme ligne : c'est ce qui distingue deux
        // marques du meme nom, et l'original affichait la meme chose.
        String where = mark.dimensionName() + " " + (int) mark.x() + " " + (int) mark.y()
                + " " + (int) mark.z();
        graphics.drawString(font, where, left + 92, y + 3, TEXT_OFF, false);

        // Le petit carre qui oublie la marque.
        int boxX = left + PANEL_WIDTH - 16;
        graphics.fill(boxX, y + 2, boxX + 8, y + 10, hovered ? FRAME : TEXT_OFF);
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int max = maxScroll();
        if (max <= 0) return;

        int left = left();
        int top = top();
        int track = LIST_HEIGHT;
        int barHeight = Math.max(8, track * LIST_HEIGHT / (marks().size() * ROW_HEIGHT));
        int barTop = top + LIST_TOP + (track - barHeight) * scroll / max;
        graphics.fill(left + PANEL_WIDTH - 4, top + LIST_TOP, left + PANEL_WIDTH - 2,
                top + LIST_TOP + track, ROW);
        graphics.fill(left + PANEL_WIDTH - 4, barTop, left + PANEL_WIDTH - 2, barTop + barHeight, FRAME);
    }

    /** La ligne d'explication sous la liste, quand une marque ne mene nulle part. */
    private void drawHint(GuiGraphics graphics, List<LocationMark> marks, AbilityData data) {
        int left = left();
        int top = top();
        String hint = null;
        int colour = TEXT_OFF;

        for (int i = 0; i < marks.size(); i++) {
            LocationMark mark = marks.get(i);
            if (canTravel(i, mark, data)) continue;
            boolean cross = Minecraft.getInstance().player != null
                    && !LocationMark.of(Minecraft.getInstance().player.level()).equals(mark.dimension());
            hint = Component.translatable(cross
                    ? "ac.ability.teleporter.location_teleport.err_exp"
                    : "ac.ability.teleporter.location_teleport.err_cp").getString();
            colour = TEXT_BAD;
            break;
        }

        if (hint != null) {
            graphics.drawString(font, hint, left + 8, top + PANEL_HEIGHT - 38, colour, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
