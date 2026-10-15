package cn.academy.ability.client;

import cn.academy.AcademyCraft;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * La liste des endroits marques : l'ecran {@code loctele_new} de l'original.
 *
 * <p>C'est la seule competence dont la touche ouvre un ecran, et l'ecran est donc tout son geste :
 * on y marque l'endroit ou l'on se trouve, on y oublie une marque, et on y clique celle ou l'on
 * veut aller.
 *
 * <h2>La disposition est celle de l'original</h2>
 *
 * <p>L'original le tenait d'un fichier {@code guis/loctele_new.xml} — un arbre de widgets poses a
 * l'echelle {@code 0,24} — et le port le redessine avec ses propres primitives, sans rien inventer
 * des cotes : chaque nombre ci-dessous est celui du XML multiplie par cette echelle, arrondi. Le
 * panneau fait donc 106 sur 127, ses lignes 19 de haut, ses icones 8, et le champ de saisie 67 de
 * large.
 *
 * <p>Ce qui s'y voit, dans l'ordre ou l'original le posait : un panneau sombre cerne de gris, une
 * ligne par marque — son nom a gauche, l'icone de saut et celle qui l'oublie a droite — puis une
 * derniere ligne pour en ajouter une, avec son champ et sa coche. Un panneau d'<b>explications</b>
 * se pose a gauche de la ligne survolee, et dit ce que la marque vaut : sa dimension, ses
 * coordonnees, son prix en CP, et la raison du refus quand il y en a une.
 *
 * <h2>Ce que l'ecran ne decide pas</h2>
 *
 * <p>Il <b>grise</b> ce qui ne marchera pas et il dit pourquoi — c'est le panneau d'explications —
 * mais il ne fait que prevenir : c'est le serveur qui paie et qui deplace, et lui seul connait la
 * reserve au point pres. Un ecran qui refuserait a tort serait pire qu'un ecran qui laisse essayer.
 *
 * <p>Et il ne s'ouvre que pour une aptitude allumee : voir {@code HoldRefusal.refusesScreen}, ou
 * le refus est explique.
 */
@OnlyIn(Dist.CLIENT)
public class LocationTeleportScreen extends Screen {

    // --- Le panneau, et ses lignes : les nombres de `loctele_new.xml` fois 0,24. ---

    /** Le panneau : 442 sur 530 dans le XML. */
    private static final int MENU_W = 106;
    private static final int MENU_H = 127;

    /** La liste commence 18 plus bas que le haut du panneau, et fait 509,78 de haut. */
    private static final int LIST_TOP = 4;
    private static final int LIST_H = 122;

    /** Une ligne : 442 sur 80, son texte a 45,28, ses deux icones a 335 et 375. */
    private static final int ROW_H = 19;
    private static final int TEXT_X = 11;
    private static final int TELEPORT_X = 80;
    private static final int REMOVE_X = 90;
    private static final int ICON = 8;

    /** Le champ de saisie : 280,56 de large, et sa coche a 375,83. */
    private static final int FIELD_W = 67;
    private static final int FIELD_H = 13;

    /** Le panneau d'explications : 335,83 sur 191,94, ses lignes de 42 et ses marges de 20. */
    private static final int INFO_W = 81;
    private static final int INFO_LINE = 10;
    private static final int INFO_MARGIN = 5;

    /** Et le panneau s'ouvre en grandissant, sur 0,4 seconde. */
    private static final double OPEN_SECONDS = 0.4;

    /** Chaque ligne apparait un vingtieme de seconde apres la precedente. */
    private static final double ROW_STEP = 0.06;
    private static final double ROW_FADE = 0.2;

    // --- Les couleurs du XML. ---

    /** Le fond du panneau : 53, 53, 53, a 128. */
    private static final int FILL = 0x80535353;

    /** Son cerne : 122, 146, 156, a 158, sur deux dixiemes de pixel. */
    private static final int OUTLINE = 0x9E7A929C;

    /** Le survol d'une ligne : blanc a 102 ; au repos, a 25. */
    private static final int ROW_HOVER = 0x66FFFFFF;
    private static final int ROW_IDLE = 0x19FFFFFF;

    /** Le texte : 193, 207, 213 ; et son gris quand la marque ne mene nulle part. */
    private static final int TEXT = 0xFFC1CFD5;
    private static final int TEXT_OFF = 0xFFA2A2A2;
    private static final int TEXT_BAD = 0xFFE08080;

    /** Le champ de saisie, quand il n'est pas encore pris : 164, 212, 233, a 127. */
    private static final int FIELD = 0x7FA4D4E9;

    /** Une icone au repos, puis survolee : les 0,7 et 1,0 du XML. */
    private static final float ICON_IDLE = 0.7f;
    private static final float ICON_HOVER = 1.0f;

    private static final ResourceLocation ICON_TELEPORT = texture("icons/icon_location_on");
    private static final ResourceLocation ICON_REMOVE = texture("icons/icon_clear");
    private static final ResourceLocation ICON_CONFIRM = texture("check");

    private final Category category;
    private final Skill skill;

    /** L'instant de l'ouverture, pour l'animation du panneau et le fondu des lignes. */
    private final long openedAt = EffectClock.now();

    private EditBox nameField;
    private int scroll;

    public LocationTeleportScreen(Category category, Skill skill) {
        super(skill.getDisplayName());
        this.category = category;
        this.skill = skill;
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/" + path + ".png");
    }

    private static AbilityData data() {
        return ClientAbilityData.get();
    }

    private static LocationTeleportSkill teleport() {
        return TeleporterCategory.LOCATION_TELEPORT;
    }

    private static Player player() {
        return Minecraft.getInstance().player;
    }

    /** Les marques, dans l'ordre : le rang est l'identifiant, comme dans l'original. */
    private List<LocationMark> marks() {
        AbilityData data = data();
        return data == null ? List.of() : data.getMarks();
    }

    private int rowCount() {
        return marks().size() + 1;
    }

    /** Le rang de la ligne qui ajoute une marque : la derniere, comme dans l'original. */
    private int addRow() {
        return marks().size();
    }

    /** Le panneau commence vingt fois 0,24 a droite du centre, et 179 fois 0,24 au-dessus. */
    private int menuLeft() {
        return width / 2 + 5;
    }

    private int menuTop() {
        return height / 2 - 43;
    }

    /** L'age de l'ecran, en secondes. */
    private double age() {
        return (EffectClock.now() - openedAt) / 1000.0;
    }

    /** La hauteur du panneau a cet instant : il s'ouvre en grandissant, d'un coup de 0,4 s. */
    private int menuHeight() {
        double opened = Math.min(1.0, age() / OPEN_SECONDS);
        return (int) Math.round(MENU_H * opened);
    }

    private int rowY(int index) {
        return menuTop() + LIST_TOP + index * ROW_H - scroll;
    }

    /** L'opacite d'une ligne : elle apparait un peu apres la precedente. */
    private float rowFade(int index) {
        double local = age() - index * ROW_STEP;
        return (float) Math.max(0.0, Math.min(1.0, local / ROW_FADE));
    }

    private int maxScroll() {
        return Math.max(0, rowCount() * ROW_H - LIST_H);
    }

    private int iconAlpha(int index, boolean hovered) {
        float fade = rowFade(index);
        return (int) (255 * fade * (hovered ? ICON_HOVER : ICON_IDLE));
    }

    /** La ligne sous la souris, ou -1 : meslee aux coordonnees du dessin, donc ici. */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < menuLeft() || mouseX > menuLeft() + MENU_W) return -1;
        if (mouseY < menuTop() + LIST_TOP || mouseY > menuTop() + LIST_TOP + LIST_H) return -1;

        int index = (int) ((mouseY - menuTop() - LIST_TOP + scroll) / ROW_H);
        return index >= 0 && index < rowCount() ? index : -1;
    }

    @Override
    protected void init() {
        int left = menuLeft();
        int top = menuTop();

        // Ce que le joueur avait commence a taper survit a la reconstruction du widget : `init`
        // est rappele a chaque redimensionnement, et perdre son nom en cours de frappe serait
        // penible.
        String kept = nameField == null ? "" : nameField.getValue();

        nameField = new EditBox(font, left + TEXT_X, top + LIST_TOP + addRow() * ROW_H + 3,
                FIELD_W, FIELD_H, Component.empty());
        nameField.setMaxLength(LocationMark.MAX_NAME);
        nameField.setBordered(false);
        nameField.setTextColor(FIELD);
        nameField.setTextColorUneditable(FIELD);
        nameField.setHint(Component.translatable(
                "ac.ability.teleporter.location_teleport.name_hint"));
        nameField.setValue(kept);
        addRenderableWidget(nameField);
    }

    /** L'ajout du champ de saisie suit le defilement : il est pose sur sa ligne, pas sur l'ecran. */
    private void placeField() {
        if (nameField == null) return;
        nameField.setY(rowY(addRow()) + 3);
    }

    private static void send(LocationTeleportPacket.Action action, int markId, String name) {
        AbilityNetwork.CHANNEL.sendToServer(new LocationTeleportPacket(
                teleport().getCategory().getCategoryId(), teleport().getId(), action, markId, name));
    }

    /** Ajoute la marque nommee par le champ, puis vide le champ comme l'original. */
    private void confirm() {
        send(LocationTeleportPacket.Action.ADD, -1, nameField == null ? "" : nameField.getValue());
        if (nameField != null) {
            nameField.setValue("");
            nameField.setFocused(false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Entree valide le champ, comme le `ConfirmInputEvent` de l'original.
        if (nameField != null && nameField.isFocused()
                && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(delta * ROW_H)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        int index = rowAt(mouseX, mouseY);
        if (index < 0) return false;

        int y = rowY(index);
        boolean overControls = mouseY >= y && mouseY < y + ROW_H;

        if (index == addRow()) {
            // La ligne d'ajout : cliquer dessus prend le champ, comme l'original.
            if (nameField != null) nameField.setFocused(true);
            return true;
        }

        LocationMark mark = marks().get(index);
        if (overControls && mouseX >= menuLeft() + REMOVE_X - 2
                && mouseX <= menuLeft() + REMOVE_X + ICON + 2) {
            send(LocationTeleportPacket.Action.REMOVE, index, "");
            return true;
        }
        if (overControls && mouseX >= menuLeft() + TELEPORT_X - 2
                && mouseX <= menuLeft() + TELEPORT_X + ICON + 2) {
            if (!refusal(mark).isEmpty()) return true;
            send(LocationTeleportPacket.Action.PERFORM, index, "");
            onClose();
            return true;
        }
        return true;
    }

    /**
     * Pourquoi cette marque ne mene nulle part, ou rien.
     *
     * Les deux refus de l'original, dans son ordre : la dimension qu'on n'a pas le droit de
     * traverser, puis la reserve qui ne suffit pas. Le texte est celui de l'ecran — les prix, eux,
     * sont calcules avec la vraie distance, donc un voyage long s'annonce cher.
     */
    private String refusal(LocationMark mark) {
        AbilityData data = data();
        Player player = player();
        // La ligne qui ajoute n'a pas de marque : elle n'a donc rien a refuser.
        if (mark == null || data == null || player == null) return "";

        LocationTeleportSkill skill = teleport();
        boolean cross = LocationTeleportSkill.crossDimension(player, mark);
        if (cross && !skill.canCrossDimension(data)) {
            return Component.translatable(
                    "ac.ability.teleporter.location_teleport.err_exp").getString();
        }
        return price(mark) > data.getControlPoint()
                ? Component.translatable(
                        "ac.ability.teleporter.location_teleport.err_cp").getString()
                : "";
    }

    /** Ce que ce saut couterait, en CP. */
    private float price(LocationMark mark) {
        AbilityData data = data();
        Player player = player();
        if (mark == null || data == null || player == null) return 0f;

        return teleport().cpCost(data, LocationTeleportSkill.distanceTo(player, mark),
                LocationTeleportSkill.crossDimension(player, mark));
    }

    /** Les lignes du panneau d'explications, dans l'ordre de l'original. */
    private List<String> explanation(int index) {
        if (index == addRow()) {
            Player player = player();
            if (player == null) return List.of();
            return List.of(LocationMark.of(player.level()) + " " + coords(player.position().x,
                    player.position().y, player.position().z));
        }

        LocationMark mark = marks().get(index);
        String head = mark.dimensionName() + " " + coords(mark.x(), mark.y(), mark.z());
        String cost = (int) price(mark) + " CP";
        String no = refusal(mark);
        return no.isEmpty() ? List.of(head, cost) : List.of(head, cost, no);
    }

    private static String coords(double x, double y, double z) {
        return "(" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + ")";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        placeField();

        int left = menuLeft();
        int top = menuTop();
        int height = menuHeight();
        int hovered = rowAt(mouseX, mouseY);

        // Le panneau : le fond du XML, puis son cerne.
        graphics.fill(left, top, left + MENU_W, top + height, FILL);
        outline(graphics, left, top, MENU_W, height, OUTLINE);

        // Les lignes se dessinent dans la liste, et rien n'en sort : le panneau s'ouvre en
        // grandissant, donc une ligne qui depasserait se verrait au-dessus de lui.
        graphics.enableScissor(left, top + LIST_TOP, left + MENU_W, top + Math.min(height,
                LIST_TOP + LIST_H));
        for (int i = 0; i < rowCount(); i++) {
            drawRow(graphics, i, top, height, hovered == i);
        }
        graphics.disableScissor();

        drawScrollBar(graphics, top, height);

        if (hovered >= 0) drawExplanation(graphics, hovered, top, height);

        // Le champ de saisie est un widget : il se dessine apres le reste.
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Une ligne : son fond qui s'allume au survol, son nom, et ses deux icones. */
    private void drawRow(GuiGraphics graphics, int index, int top, int panelHeight,
                         boolean hovered) {
        int y = rowY(index);
        if (y + ROW_H < top + LIST_TOP || y > top + Math.min(panelHeight, LIST_TOP + LIST_H)) return;

        float fade = rowFade(index);
        if (fade <= 0f) return;

        int left = menuLeft();
        int tint = hovered ? ROW_HOVER : ROW_IDLE;
        graphics.fill(left + 1, y, left + MENU_W - 1, y + ROW_H - 1, alpha(tint, fade));

        if (index == addRow()) {
            // Sa ligne n'a que son champ : c'est un widget, dessine plus loin.
            drawIcon(graphics, ICON_CONFIRM, left + REMOVE_X, y, iconAlpha(index, hovered));
            return;
        }

        LocationMark mark = marks().get(index);
        String no = refusal(mark);
        graphics.drawString(font, mark.name(), left + TEXT_X, y + (ROW_H - 8) / 2,
                alpha(no.isEmpty() ? TEXT : TEXT_OFF, fade), false);

        if (no.isEmpty()) {
            drawIcon(graphics, ICON_TELEPORT, left + TELEPORT_X, y, iconAlpha(index, hovered));
        }
        drawIcon(graphics, ICON_REMOVE, left + REMOVE_X, y, iconAlpha(index, hovered));
    }

    private void drawIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y, int alpha) {
        if (alpha <= 0) return;
        graphics.setColor(1f, 1f, 1f, alpha / 255f);
        graphics.blit(icon, x, y + (ROW_H - ICON) / 2, ICON, ICON, 0f, 0f, 16, 16, 16, 16);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Le panneau d'explications, pose a gauche de la ligne survolee. */
    private void drawExplanation(GuiGraphics graphics, int index, int top, int panelHeight) {
        List<String> lines = explanation(index);
        if (lines.isEmpty()) return;

        int right = menuLeft() - 1;
        int left = right - INFO_W;
        int y = Math.max(0, Math.min(rowY(index), top + panelHeight - lines.size() * INFO_LINE));
        int height = lines.size() * INFO_LINE + 2 * INFO_MARGIN;

        graphics.fill(left, y, right, y + height, FILL);
        outline(graphics, left, y, INFO_W, height, OUTLINE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // La derniere ligne est celle du refus, quand il y en a un : elle se dit en rouge.
            boolean refused = i == lines.size() - 1
                    && !refusal(markOf(index)).isEmpty();
            graphics.drawString(font, line, right - INFO_MARGIN - font.width(line),
                    y + INFO_MARGIN + i * INFO_LINE, refused ? TEXT_BAD : TEXT, false);
        }
    }

    /** La marque de ce rang, ou {@code null} pour la ligne qui ajoute. */
    private LocationMark markOf(int index) {
        return index < 0 || index >= marks().size() ? null : marks().get(index);
    }

    /** La barre de defilement : un filet au bord droit, quand il y a de quoi defiler. */
    private void drawScrollBar(GuiGraphics graphics, int top, int height) {
        int max = maxScroll();
        if (max <= 0) return;

        int left = menuLeft() + MENU_W - 3;
        int listTop = top + LIST_TOP;
        int listHeight = Math.min(LIST_H, Math.max(0, height - LIST_TOP));
        int barHeight = Math.max(8, listHeight * LIST_H / (rowCount() * ROW_H));
        int barTop = listTop + (listHeight - barHeight) * scroll / max;

        graphics.fill(left, listTop, left + 1, listTop + listHeight, ROW_IDLE);
        graphics.fill(left, barTop, left + 1, barTop + barHeight, OUTLINE);
    }

    /** Un carre de la couleur demandee, a la maniere du trait du XML. */
    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + 1, argb);
        graphics.fill(x, y + height - 1, x + width, y + height, argb);
        graphics.fill(x, y + 1, x + 1, y + height - 1, argb);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    /** La meme couleur, avec son opacite multipliee par celle de la ligne. */
    private static int alpha(int argb, float factor) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
