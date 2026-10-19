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
import cn.academy.client.gui.AcademyFonts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran qui regle les prereglages, ouvert par la touche dediee (et par l'application
 * Reglages du terminal).
 *
 * <p>Portage de {@code PresetEditUI}, qui est un CARROUSEL : quatre panneaux « Preset #1 » a
 * « Preset #4 » poses a cent vingt-cinq unites les uns des autres, dont un seul est au centre
 * — celui que le jeu utilise, a pleine taille et pleine opacite. Cliquer une ligne d'un
 * panneau voisin le fait glisser au centre en trente-cinq centiemes de seconde ; cliquer une
 * ligne du panneau du centre ouvre le selecteur de competences, sous la souris comme chez lui.
 *
 * <p>Toutes les mesures viennent de son document, {@code guis/preset_edit.xml}, et de ses
 * images : {@code back.png}, {@code selected.png} (la ligne sous la souris) et
 * {@code cancel.png} (le premier choix du selecteur). Le port dessinait a leur place des
 * rectangles gris de son invention.
 *
 * <p>Son selecteur montre les competences <b>apprises</b> qui ne sont pas deja posees sur ce
 * prereglage, et son premier choix — l'image « cancel » — retire celle de la touche. Son nom
 * s'ecrit dans un cartouche au-dessus, et la case survolee blanchit.
 *
 * <p>Rien n'est decide ici : chaque clic envoie une demande au serveur, qui range et renvoie
 * l'etat complet. L'ecran ne fait donc que lire {@code ClientPresetData}, ce qui rend une
 * divergence impossible.
 */
@OnlyIn(Dist.CLIENT)
public class PresetEditScreen extends Screen {

    // ------------------------------------------------------------------
    // Les mesures du document de l'original, `guis/preset_edit.xml`.
    //
    // Le port dessinait des rectangles gris de son invention ; tout ce qui suit est le
    // document lui-meme : un panneau de 116,25 sur 141,5, quatre lignes de 110 sur 34,75
    // (une par touche), l'icone incrustee dans la plaque du document, le nom a 36,25 dans
    // une boite de 62,5 sur 15, et le titre du panneau quinze unites au-dessus.
    // ------------------------------------------------------------------

    private static final double PAGE_W = 116.25;
    private static final double PAGE_H = 141.5;
    private static final double ROW_W = 110.0;
    private static final double ROW_H = 34.75;
    // Le document donne aux lignes `x = 0,5` ET `alignWidth = CENTER` : le moteur ajoute le
    // centrage `(116,25 - 110) / 2` a ce x, donc leur bord gauche est a 3,625. Le port ne
    // prenait que le 0,5, ce qui tirait tout de 3,125 vers la gauche — « les images sont
    // legerement trop a gauche », dit le joueur.
    private static final double ROW_X = (PAGE_W - ROW_W) / 2 + 0.5;
    private static final double ROW_TOP = 1.5;

    // La plaque que `back.png` dessine dans chaque ligne : 27,25 sur 27,25 a (5,25 ; 3,25)
    // de la page (mesuree dans la texture, qui fait quatre pixels par unite — les carres
    // vont de 19 a 127 px, puis tous les 139). L'image de la competence s'y INCRUSTE, a la
    // taille du carre : elle le remplit au lieu d'etre posee par-dessus, desalignee.
    private static final double SLOT_X = 5.25;
    private static final double SLOT_Y = 3.25;
    private static final double SLOT_W = 27.25;
    private static final double SLOT_H = 27.25;
    /** Le filet de plaque laisse autour de l'image : aucun, elle fait la taille du carre. */
    private static final double ICON_INSET = 0.0;
    private static final double ICON_SIZE = SLOT_W - ICON_INSET * 2;

    private static final double NAME_X = 36.25;
    private static final double NAME_Y = 10.0;
    private static final double NAME_H = 15.0;
    private static final double NAME_W = 62.5;
    private static final double TITLE_BOX = 35.0;
    private static final double TITLE_X = (PAGE_W - TITLE_BOX) / 2 + 0.5;
    private static final double TITLE_Y = -15.0;

    /** Le pas entre deux panneaux, et la duree du glissement (0,35 s chez l'original). */
    private static final double STEP = 125.0;
    private static final double TRANSIT_TIME = 0.35;
    private static final float MAX_SCALE = 1.0f;
    private static final float MIN_SCALE = 0.8f;
    private static final int MAX_ALPHA = 255;
    private static final int MIN_ALPHA = 76;

    /** Le selecteur de competence : cases de 15, pas de 18, quatre par rangee. */
    private static final int PICK_SIZE = 15;
    private static final int PICK_STEP = 18;
    private static final int PICK_PER_ROW = 4;
    private static final double PICK_MARGIN = 2.5;

    /** Le noir a 70 % que l'original pose sur le jeu (`RenderUtils.drawBlackout`). */
    private static final int BLACKOUT = 0xB3000000;

    /** Le fond du selecteur, et sa lueur : ceux de l'original. */
    private static final int PICK_BACK = 0xC8313131;
    private static final int PICK_GLOW = 0x33FFFFFF;
    private static final int PICK_HOVER = 0x33FFFFFF;
    private static final int PICK_HINT = 0xFFBBBBBB;

    private static final ResourceLocation TEX_BACK = presetTexture("back");
    private static final ResourceLocation TEX_SELECTED = presetTexture("selected");
    private static final ResourceLocation TEX_CANCEL = presetTexture("cancel");

    /** La taille des images source : les icones de competence font 32, « cancel » en fait 23. */
    private static final int ICON_TEX = 32;
    private static final int CANCEL_TEX = 23;

    /** Les competences apprises, dans l'ordre des categories. */
    private final List<Skill> learned = new ArrayList<>();

    /** Le panneau au centre, celui qu'on quitte pendant le glissement, et son debut. */
    private int active;
    private int lastActive;
    private boolean transiting;
    private long transitStart;

    /** La touche dont le selecteur est ouvert, et la ou il s'est ouvert. */
    private int pickKey = -1;
    private double pickX;
    private double pickY;

    public PresetEditScreen() {
        super(Component.translatable("academy.preset.title"));
    }

    private static ResourceLocation presetTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(cn.academy.AcademyCraft.MOD_ID,
                "textures/guis/preset_settings/" + name + ".png");
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

        // L'original ouvrait toujours le premier panneau ; le port ouvre celui qui est en
        // service — editer un prereglage que le jeu n'utilise pas serait un piege.
        active = Math.max(0, Math.min(PresetData.MAX_PRESETS - 1,
                ClientPresetData.get().getCurrentId()));
        lastActive = active;
        transiting = false;
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Le noir de l'original : un voile de 70 % sur le jeu (`RenderUtils.drawBlackout`).
        graphics.fill(0, 0, width, height, BLACKOUT);

        PresetData data = ClientPresetData.get();
        double progress = transitProgress();

        drawScreenTitle(graphics);
        for (int preset = 0; preset < PresetData.MAX_PRESETS; preset++) {
            drawPage(graphics, data, preset, progress, mouseX, mouseY);
        }
        drawSelector(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Le nom de l'ecran, en haut a gauche : douze unites, comme le document. */
    private void drawScreenTitle(GuiGraphics graphics) {
        AcademyFonts.drawTop(graphics, font,
                Component.translatable("ac.gui.preset_edit.name").getString(),
                5, 5, 0xFFFFFFFF, 12);
    }

    /** L'avancement du glissement, de zero a un. A un, il est fini. */
    private double transitProgress() {
        if (!transiting) return 1.0;

        double elapsed = (System.nanoTime() - transitStart) / 1.0e9;
        double progress = elapsed / TRANSIT_TIME;
        if (progress >= 1.0) {
            transiting = false;
            lastActive = active;
            return 1.0;
        }
        return progress;
    }

    /** Ou tombe un panneau : cent vingt-cinq unites par rang, et il glisse d'un rang a l'autre. */
    private double pageX(int preset, double progress) {
        double from = preset - lastActive;
        double to = preset - active;
        return STEP * (from + (to - from) * progress);
    }

    private void startTransit(int to) {
        if (to == active) return;

        lastActive = active;
        active = to;
        transiting = true;
        transitStart = System.nanoTime();

        // Le panneau du centre est celui que le jeu utilise : l'original changeait de
        // prereglage avec sa touche dediee, et l'editeur ne pouvait que le regarder.
        send(PresetActionPacket.switchTo(to));
    }

    private void drawPage(GuiGraphics graphics, PresetData data, int preset, double progress,
                          int mouseX, int mouseY) {
        double x = pageX(preset, progress);
        // L'eloignement au centre donne l'echelle et l'opacite : un panneau voisin est a
        // quatre cinquiemes et a 30 % d'opacite, comme chez l'original.
        double near = Math.min(1.0, Math.abs(x) / STEP);
        float scale = (float) (MAX_SCALE + (MIN_SCALE - MAX_SCALE) * near);
        int alpha = (int) Math.round(MAX_ALPHA + (MIN_ALPHA - MAX_ALPHA) * near);
        boolean centred = near <= 0.0001;

        double centreX = width / 2.0 + x;
        double centreY = height / 2.0;

        graphics.pose().pushPose();
        graphics.pose().translate((float) centreX, (float) centreY, 0f);
        graphics.pose().scale(scale, scale, 1f);
        graphics.pose().translate((float) (-PAGE_W / 2), (float) (-PAGE_H / 2), 0f);

        // Le fond du panneau.
        image(graphics, TEX_BACK, 0, 0, PAGE_W, PAGE_H, 465, 566, alpha);

        // Son titre, centre au-dessus. Le document le pose a `x = 0,5` avec un alignement
        // CENTER, et son `TextBox` ecrit par le HAUT de sa cellule (`heightAlign = CENTER`
        // dans une boite de 15 pour une police de 10, donc deux et demi plus bas).
        String title = Component.translatable("ac.gui.preset_edit.tag").getString()
                + (preset + 1);
        int titleCell = 10;
        AcademyFonts.drawTop(graphics, font, title,
                (int) Math.round(TITLE_X),
                (int) Math.round(TITLE_Y + (15 - titleCell) / 2.0),
                shaded(0xFFFFFFFF, alpha), titleCell);

        for (int key = 0; key < AbilityPreset.MAX_KEYS; key++) {
            double rowY = ROW_TOP + key * ROW_H;

            // La ligne ne s'allume que sur le panneau du centre, sous la souris.
            boolean hovered = centred && insidePage(mouseX, mouseY, centreX, centreY, scale,
                    ROW_X, rowY, ROW_W, ROW_H);
            if (hovered) {
                image(graphics, TEX_SELECTED, ROW_X, rowY, ROW_W, ROW_H, 440, 149, alpha);
            }

            Skill skill = skillOf(data, preset, key);
            if (skill == null) continue;

            blend();
            graphics.setColor(1f, 1f, 1f, alpha / 255f);
            graphics.blit(skill.getHintIcon(),
                    (int) Math.round(SLOT_X + ICON_INSET),
                    (int) Math.round(rowY + SLOT_Y + (SLOT_H - ICON_SIZE) / 2),
                    (int) Math.round(ICON_SIZE), (int) Math.round(ICON_SIZE),
                    0f, 0f, 32, 32, 32, 32);
            graphics.setColor(1f, 1f, 1f, 1f);

            // Le nom, coupe a la largeur de sa boite de 62,5 — c'est ce que fait son
            // `TextBox` : la chaine s'arrete des que la largeur est atteinte, sans points de
            // suspension. Le port le laissait deborder sur le panneau voisin.
            AcademyFonts.drawTop(graphics, font, fit(skill.getDisplayName().getString()),
                    (int) Math.round(ROW_X + NAME_X),
                    (int) Math.round(rowY + NAME_Y + (NAME_H - 10) / 2.0),
                    shaded(0xFFFFFFFF, alpha), 10);
        }

        graphics.pose().popPose();
    }

    /**
     * Le selecteur de competence, ouvert sous la souris.
     *
     * <p>Celui de l'original : une petite fenetre noire a 78 %, cernee d'une lueur blanche,
     * des cases de quinze unites au pas de dix-huit, quatre par rangee — la premiere etant
     * « retirer ». Le nom de la competence survolee s'ecrit au-dessus, dans un cartouche gris.
     */
    private void drawSelector(GuiGraphics graphics, int mouseX, int mouseY) {
        if (pickKey < 0) return;

        List<Pick> picks = picks();
        int columns = Math.max(1, Math.min(picks.size(), PICK_PER_ROW));
        int rows = Math.max(1, (picks.size() + PICK_PER_ROW - 1) / PICK_PER_ROW);

        double windowW = PICK_MARGIN * 2 + PICK_SIZE + PICK_STEP * (columns - 1);
        double windowH = PICK_MARGIN * 2 + PICK_SIZE + PICK_STEP * (rows - 1);

        int left = (int) Math.round(pickX);
        int top = (int) Math.round(pickY);
        int right = (int) Math.round(pickX + windowW);
        int bottom = (int) Math.round(pickY + windowH);

        blend();
        graphics.renderOutline(left - 1, top - 1, right - left + 2, bottom - top + 2, PICK_GLOW);
        graphics.fill(left, top, right, bottom, PICK_BACK);

        // Le cartouche du nom, au-dessus de la fenetre.
        Pick hovered = pickAt(mouseX, mouseY);
        String hint = hovered == null
                ? Component.translatable("ac.gui.preset_edit.skill_select").getString()
                : hovered.hint();
        int hintWidth = AcademyFonts.width(font, hint, 9) + 6;
        int hintTop = top - 14;
        graphics.renderOutline(left - 1, hintTop - 1, hintWidth + 2, 13, PICK_GLOW);
        graphics.fill(left, hintTop, left + hintWidth, hintTop + 12, PICK_BACK);
        AcademyFonts.drawTop(graphics, font, hint, left + 3, hintTop + 2, PICK_HINT, 9);

        for (int i = 0; i < picks.size(); i++) {
            int x = pickCellX(left, i);
            int y = pickCellY(top, i);
            boolean over = isIn(mouseX, mouseY, x, y, PICK_SIZE, PICK_SIZE);

            blend();
            graphics.setColor(1f, 1f, 1f, 1f);
            if (picks.get(i).icon() != null) {
                int tex = picks.get(i).tex();
                graphics.blit(picks.get(i).icon(), x, y, PICK_SIZE, PICK_SIZE,
                        0f, 0f, tex, tex, tex, tex);
            }
            if (over) graphics.fill(x, y, x + PICK_SIZE, y + PICK_SIZE, PICK_HOVER);
        }
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** Les choix du selecteur : « retirer », puis les competences pas encore posees. */
    private List<Pick> picks() {
        PresetData data = ClientPresetData.get();
        AbilityPreset preset = data.getPreset(active);

        List<Pick> out = new ArrayList<>();
        out.add(new Pick(null, TEX_CANCEL, CANCEL_TEX,
                Component.translatable("ac.gui.preset_edit.skill_remove").getString()));
        for (Skill skill : learned) {
            if (preset != null && preset.contains(skill.getName())) continue;
            out.add(new Pick(skill, skill.getHintIcon(), ICON_TEX, skill.getDisplayName().getString()));
        }
        return out;
    }

    private Pick pickAt(double mouseX, double mouseY) {
        if (pickKey < 0) return null;

        List<Pick> picks = picks();
        int left = (int) Math.round(pickX);
        int top = (int) Math.round(pickY);
        for (int i = 0; i < picks.size(); i++) {
            if (isIn(mouseX, mouseY, pickCellX(left, i), pickCellY(top, i),
                    PICK_SIZE, PICK_SIZE)) {
                return picks.get(i);
            }
        }
        return null;
    }

    private static int pickCellX(int left, int index) {
        return left + (int) Math.round(PICK_MARGIN + (index % PICK_PER_ROW) * PICK_STEP);
    }

    private static int pickCellY(int top, int index) {
        return top + (int) Math.round(PICK_MARGIN + (index / PICK_PER_ROW) * PICK_STEP);
    }

    /** Un choix du selecteur : une competence, ou {@code null} pour « retirer ». */
    private record Pick(Skill skill, ResourceLocation icon, int tex, String hint) {}

    // ------------------------------------------------------------------
    // Outils de dessin
    // ------------------------------------------------------------------

    /** Une image du document, a sa taille, avec l'opacite du panneau. */
    private static void image(GuiGraphics graphics, ResourceLocation texture, double x,
                              double y, double width, double height, int texWidth,
                              int texHeight, int alpha) {
        blend();
        graphics.setColor(1f, 1f, 1f, alpha / 255f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    /** La meme couleur, a l'opacite du panneau : c'est l'{@code AlphaAssign} de l'original. */
    private static int shaded(int colour, int alpha) {
        return (colour & 0x00FFFFFF) | (alpha << 24);
    }

    /**
     * Le texte coupe a la largeur de sa boite de nom.
     *
     * <p>C'est la regle de son {@code TextBox} : il additionne les largeurs de caracteres et
     * s'arrete des que le total atteint la largeur du widget — pas de points de suspension,
     * pas de debordement. « Le nom des competences ne doit pas pouvoir depasser », dit le
     * joueur.
     */
    private static String fit(String text) {
        double acc = 0;
        int i = 0;
        while (i < text.length() && acc < NAME_W) {
            int codePoint = text.codePointAt(i);
            acc += AcademyFonts.width(null, new String(Character.toChars(codePoint)), 10);
            i += Character.charCount(codePoint);
        }
        return text.substring(0, i);
    }

    private static void blend() {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }

    /** La competence posee sur cette touche, ou {@code null}. */
    @Nullable
    private static Skill skillOf(PresetData data, int preset, int key) {
        AbilityPreset own = data.getPreset(preset);
        if (own == null) return null;

        String name = own.nameAt(key);
        return name == null ? null : skillByName(name);
    }

    /** Vrai si ce point de l'ecran tombe dans une boite du panneau, echelle comprise. */
    private static boolean insidePage(double mouseX, double mouseY, double centreX,
                                      double centreY, float scale, double x, double y,
                                      double width, double height) {
        double localX = (mouseX - centreX) / scale + PAGE_W / 2;
        double localY = (mouseY - centreY) / scale + PAGE_H / 2;
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Le selecteur ouvert : une icone pose la competence (la premiere la retire), et un
        // clic ailleurs referme — c'est ce que faisait son ecouteur de clic.
        if (pickKey >= 0) {
            if (button == 0) {
                Pick picked = pickAt(mouseX, mouseY);
                if (picked != null) {
                    send(PresetActionPacket.assign(active, pickKey,
                            picked.skill() == null ? null : picked.skill().getName()));
                }
            }
            pickKey = -1;
            return true;
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Une ligne d'un panneau : celle du panneau du centre ouvre le selecteur, celle d'un
        // voisin fait glisser ce panneau au centre.
        double progress = transitProgress();
        for (int preset = 0; preset < PresetData.MAX_PRESETS; preset++) {
            double x = pageX(preset, progress);
            double near = Math.min(1.0, Math.abs(x) / STEP);
            float scale = (float) (MAX_SCALE + (MIN_SCALE - MAX_SCALE) * near);
            double centreX = width / 2.0 + x;
            double centreY = height / 2.0;

            for (int key = 0; key < AbilityPreset.MAX_KEYS; key++) {
                double rowY = ROW_TOP + key * ROW_H;
                if (!insidePage(mouseX, mouseY, centreX, centreY, scale,
                        ROW_X, rowY, ROW_W, ROW_H)) {
                    continue;
                }

                if (preset == active && !transiting) {
                    pickKey = key;
                    pickX = mouseX;
                    pickY = mouseY;
                } else {
                    startTransit(preset);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        // L'ecran de l'original arretait le jeu : en solo, ouvrir les prereglages met en pause.
        return true;
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

