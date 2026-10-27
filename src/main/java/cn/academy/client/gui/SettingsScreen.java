package cn.academy.client.gui;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.AbilityKeyBindings;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.terminal.client.TerminalKeyBindings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * L'ecran de reglage du mod : la page de l'original.
 *
 * <p>Portage de {@code SettingsUI}, sur son document {@code guis/settings.xml} : une page de
 * <b>742 sur 923</b> que le document dessine a <b>0,2</b> — donc 148 sur 185 a l'ecran, centree.
 * Le port dessinait un panneau plein ecran de son invention ; tout ce qui suit vient du document,
 * en unites de page :
 *
 * <ul>
 *   <li>le fond {@code guis/settings.png}, qui porte le cadre et le titre ;</li>
 *   <li>la <b>liste</b> de 614 sur 720 a (60 ; 120), ou chaque ligne fait 611 sur 60 : un
 *       <i>titre de categorie</i> (son texte a 8, souligne d'un trait blanc de 4 unites en bas),
 *       une <i>case a cocher</i> (le libelle a 15 et la case de 35 a 550), une <i>touche</i> (le
 *       libelle, puis le nom de la touche dans 160 sur 40 a 440) ou un <i>bouton</i>;</li>
 *   <li>la <b>glissiere</b> de 9 sur 96 a (673 ; 119), {@code life_record/rollbar.png}, qui
 *       glisse jusqu'a 760.</li>
 * </ul>
 *
 * <p>Rien de tout cela n'apparait dans les options de Minecraft : les touches du mod se
 * choisissent ici, et nulle part ailleurs.
 *
 * <h2>Un ecart</h2>
 *
 * <p>Dans le document, le bouton d'une ligne d'action est dessine vide (son {@code TextBox} n'a
 * pas de contenu) — un rectangle gris de 160 sur 40 qui ne dit pas ce qu'il fait. Le port y ecrit
 * le mot de l'action : un bouton muet se lit comme un bug.
 */
@OnlyIn(Dist.CLIENT)
public class SettingsScreen extends Screen {

    // ------------------------------------------------------------------
    // Le document `guis/settings.xml`, en unites de page.
    // ------------------------------------------------------------------

    private static final double PAGE_W = 742.0;
    private static final double PAGE_H = 923.0;
    /** L'echelle que le document donne a sa page : 742 sur 923 devient 148 sur 185. */
    private static final double PAGE_SCALE = 0.2;

    private static final double AREA_X = 60.0;
    private static final double AREA_Y = 120.0;
    private static final double AREA_W = 614.0;
    private static final double AREA_H = 720.0;

    /** Une ligne de la liste, et son trait de categorie. */
    private static final double ROW_W = 611.0;
    private static final double ROW_H = 60.0;
    private static final double LINE_H = 4.0;
    private static final int HEAD_CELL = 42;
    private static final int CELL = 40;

    /** Le libelle, la case a cocher et le controle de droite. */
    private static final double LABEL_X = 15.0;
    /**
     * Le retrait d'une sous-ligne.
     *
     * <p>Une sous-option se lit comme telle ou ne se lit pas : c'est le retrait qui dit qu'elle
     * appartient a la ligne du dessus, et pas qu'elle est une competence de plus.
     */
    private static final double INDENT = 26.0;
    /** La petite fleche des lignes depliables, dans la gouttiere a gauche des libelles. */
    private static final double ARROW_X = 4.0;
    private static final double ARROW_W = 30.0;
    private static final double ARROW_H = 30.0;
    /**
     * La largeur du libelle.
     *
     * <p>Le document dit 300, et le texte s'y coupe net (regle du {@code TextBox}). Mais sa police
     * est PLUS ETROITE que celle du port : « Mouse wheel adjust tp distance » tient chez lui dans
     * ces 300 et sortait coupe ici (« Mouse wheel adjust t »), alors que son ecran le montre en
     * entier. La boite va donc jusqu'au controle, qui commence a 440 — le resultat visible est le
     * sien, sans deborder sur la colonne des touches.
     */
    private static final double LABEL_W = 415.0;
    private static final double BOX_X = 550.0;
    private static final double BOX_SIZE = 35.0;
    private static final double CTRL_X = 440.0;
    private static final double CTRL_W = 160.0;
    private static final double CTRL_H = 40.0;
    /** Les controles sont centres dans la hauteur de la ligne : (60 - 40) / 2. */
    private static final double CTRL_Y = (ROW_H - CTRL_H) / 2;
    /** Le titre de categorie se pose a 8 unites du bord de la ligne. */
    private static final double HEAD_X = 8.0;

    /** La glissiere : 9 sur 96, qui descend de 119 a 760 (`lower` / `upper` du document). */
    private static final double SCROLL_X = 673.0;
    private static final double SCROLL_W = 9.0;
    private static final double SCROLL_H = 96.0;
    private static final double SCROLL_LOWER = 119.0;
    private static final double SCROLL_UPPER = 760.0;

    /** Couleurs du document : libelle, titre de categorie, trait, bouton. */
    private static final int TEXT_COLOUR = 0xFFFFFFFF;
    private static final int HEAD_COLOUR = 0xAAFFFFFF;
    private static final int LINE_COLOUR = 0x96FFFFFF;
    private static final int BUTTON_COLOUR = 0xFF333333;
    private static final int BUTTON_HOVER = 0xFF555555;
    private static final int HOVER_VEIL = 0x33FFFFFF;
    /** La touche en cours de capture : l'orange de son {@code EditKey}. */
    private static final int EDIT_COLOUR = 0xFFFB8525;

    private static final ResourceLocation TEX_BACK = texture("guis/settings.png");
    private static final ResourceLocation TEX_ROLLBAR = texture("guis/life_record/rollbar.png");
    private static final ResourceLocation TEX_CHECK_ON = texture("guis/check_true.png");
    private static final ResourceLocation TEX_CHECK_OFF = texture("guis/check_false.png");

    /** Ce qu'une ligne fait quand on clique sur sa partie droite. */
    private enum Kind { HEADER, KEY, TOGGLE, ACTION }

    private record Row(String labelKey, Kind kind, String category, KeyMapping key,
                       java.util.function.BooleanSupplier getter, Runnable action,
                       boolean indent, boolean disclosure, boolean open) {

        static Row header(String category) {
            return new Row("ac.settings.cat." + category, Kind.HEADER, category, null, null, null,
                    false, false, false);
        }

        static Row key(String category, String labelKey, KeyMapping mapping) {
            return new Row(labelKey, Kind.KEY, category, mapping, null, null, false, false, false);
        }

        static Row toggle(String category, String labelKey,
                          java.util.function.BooleanSupplier getter, Runnable action) {
            return new Row(labelKey, Kind.TOGGLE, category, null, getter, action, false, false, false);
        }

        /**
         * Une ligne depliable : sa petite fleche ouvre et ferme les lignes du dessous.
         *
         * <p>{@code open} n'est lu que pour dessiner la fleche — la liste des lignes est
         * refaite a chaque bascule, donc la valeur ne peut pas mentir.
         */
        static Row disclose(String category, String labelKey,
                            java.util.function.BooleanSupplier getter, Runnable action, boolean open) {
            return new Row(labelKey, Kind.TOGGLE, category, null, getter, action, false, true, open);
        }

        /** Une sous-ligne : decalee sous la ligne qu'elle precise, sans rien y ajouter. */
        static Row subToggle(String category, String labelKey,
                             java.util.function.BooleanSupplier getter, Runnable action) {
            return new Row(labelKey, Kind.TOGGLE, category, null, getter, action, true, false, false);
        }

        static Row action(String category, String labelKey, Runnable action) {
            return new Row(labelKey, Kind.ACTION, category, null, null, action, false, false, false);
        }
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();

    /**
     * Les competences de la casse sont-elles depliees ? La fleche les ouvre et les ferme.
     *
     * <p>Ouvertes par defaut : les sous-options ne servent a rien si personne ne sait qu'elles
     * existent. La fleche est la pour les ranger, pas pour les cacher.
     */
    private boolean breakOpen = true;

    /** Position de la glissiere, de 0 a 1. */
    private double progress;

    private boolean dragging;
    private double dragGrip;

    /** La ligne dont la touche est en cours de capture, ou -1. */
    private int capturing = -1;

    public SettingsScreen() {
        this(null);
    }

    public SettingsScreen(Screen parent) {
        super(Component.translatable("ac.app.settings.name"));
        this.parent = parent;
        buildRows();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/" + path);
    }

    /**
     * Le contenu de la page, dans l'ordre de l'original : un titre de categorie, puis ses
     * lignes. Le titre est une ligne a part entiere chez lui (le {@code t_cathead} de 611 sur 60,
     * avec son trait sous le texte), pas un simple libelle pose au-dessus.
     *
     * <p>Les libelles sont ses CLES de langue ({@code ac.settings.prop.*}), pas les noms de
     * touches du jeu : c'est ce qui affiche « Ability #1 », « Switch Preset », « Activate
     * Ability » ou « Open Data Terminal », comme sur son ecran.
     */
    private void buildRows() {
        if (!rows.isEmpty()) return;

        rows.add(Row.header("keys"));
        rows.add(Row.key("keys", "ac.settings.prop.ability_0", AbilityKeyBindings.ABILITY_1));
        rows.add(Row.key("keys", "ac.settings.prop.ability_1", AbilityKeyBindings.ABILITY_2));
        rows.add(Row.key("keys", "ac.settings.prop.ability_2", AbilityKeyBindings.ABILITY_3));
        rows.add(Row.key("keys", "ac.settings.prop.ability_3", AbilityKeyBindings.ABILITY_4));
        rows.add(Row.key("keys", "ac.settings.prop.debug_console", AbilityKeyBindings.DEBUG_CONSOLE));
        rows.add(Row.key("keys", "ac.settings.prop.switch_preset", AbilityKeyBindings.PRESET_NEXT));
        rows.add(Row.key("keys", "ac.settings.prop.ability_activation", AbilityKeyBindings.TOGGLE_ABILITY));
        rows.add(Row.key("keys", "ac.settings.prop.edit_preset", AbilityKeyBindings.PRESET_EDIT));
        rows.add(Row.key("keys", "ac.settings.prop.open_data_terminal",
                TerminalKeyBindings.OPEN_TERMINAL));

        rows.add(Row.header("generic"));
        rows.add(Row.toggle("generic", "ac.settings.prop.attackPlayer",
                () -> cn.academy.Config.attackPlayer, () -> cn.academy.Config.toggleAttackPlayer()));
        // La casse : ce n'est plus le reglage du serveur, c'est celui du JOUEUR. Le serveur garde
        // son propre verrou ({@code general.destroyBlocks}) ; ici le joueur dit ce qu'il accepte,
        // et son choix part au serveur, qui l'applique et le renvoie.
        //
        // La case montre l'etat EFFECTIF (verrou du serveur ET choix du joueur) : une case cochee
        // qui ne casse rien serait un mensonge. En solo le joueur est le serveur, donc la case
        // leve aussi le verrou du serveur — c'est ce qu'il attend d'un reglage nomme « detruire
        // les blocs ».
        rows.add(Row.disclose("generic", "ac.settings.prop.destroyBlocks",
                SettingsScreen::breaksBlocksNow, SettingsScreen::toggleBreaksBlocks, breakOpen));
        // Puis une sous-ligne par competence qui creuse, pour celles qu'il connait : c'est ce qui
        // permet de garder la mine ray et de refuser le meltdowner, par exemple. Cochee = elle
        // casse, et c'est l'etat par defaut puisqu'une case neuve l'est aussi. Derriere la fleche :
        // sept competences sous chaque reglage feraient une liste interminable.
        if (breakOpen) {
            for (cn.academy.ability.Skill skill : cn.academy.ability.BlockBreaking.prefSkills()) {
                if (!ClientAbilityData.get().isSkillLearned(skill)) continue;
                rows.add(Row.subToggle("generic", skill.getDisplayKey(),
                        () -> breaksBlocksNow() && ClientAbilityData.get().skillBreaksBlocks(skill),
                        () -> toggleSkill(skill)));
            }
        }
        rows.add(Row.toggle("generic", "ac.settings.prop.headsOrTails",
                () -> cn.academy.Config.headsOrTails, () -> cn.academy.Config.toggleHeadsOrTails()));
        rows.add(Row.toggle("generic", "ac.settings.prop.useMouseWheel",
                () -> cn.academy.Config.useMouseWheel, () -> cn.academy.Config.toggleUseMouseWheel()));

        rows.add(Row.header("misc"));
        rows.add(Row.action("misc", "ac.settings.prop.edit_ui",
                () -> minecraft.setScreen(new CustomizeUiScreen(this))));
        // Le filtre de la detection de minerais : une piece du port, demandee le 26/10. Il vit
        // dans Misc, a cote du reglage du HUD, parce que c'est un reglage de presentation.
        rows.add(Row.action("misc", "ac.settings.prop.filter_ores",
                () -> minecraft.setScreen(new OreFilterScreen(this))));
    }

    /**
     * Ouvre ou ferme la liste des competences de la casse.
     *
     * <p>La liste entiere est refaite, parce que les lignes sont une liste plate : c'est ce qui
     * garde la geometrie, le clic et le defilement d'accord avec ce qui est dessine.
     */
    private void toggleBreakDisclosure() {
        breakOpen = !breakOpen;
        rows.clear();
        buildRows();
        capturing = -1;
        progress = Math.max(0, Math.min(1, progress));
    }

    /** Le drapeau du joueur, tel que le serveur vient de le synchroniser. */
    private static boolean playerBreaksBlocks() {
        return ClientAbilityData.get().playerBreaksBlocks();
    }

    /** L'etat effectif : le verrou du serveur, puis le choix du joueur. */
    private static boolean breaksBlocksNow() {
        return cn.academy.Config.destroyBlocks && playerBreaksBlocks();
    }

    /**
     * La case generale : le choix du joueur, et en solo le verrou du serveur avec lui.
     *
     * <p>En solo le joueur <b>est</b> le serveur : sans ce deuxieme geste, une case cochee
     * laisserait {@code general.destroyBlocks} ferme et rien ne casserait — le joueur l'a
     * signale.
     */
    private static void toggleBreaksBlocks() {
        setBreaksBlocks(!breaksBlocksNow());
    }

    private static void setBreaksBlocks(boolean value) {
        sendBreakPrefs(null, value);
        if (net.minecraft.client.Minecraft.getInstance().hasSingleplayerServer()) {
            cn.academy.Config.setDestroyBlocks(value);
        }
    }

    /**
     * Une sous-case : elle coche la competence, et ouvre le general si besoin.
     *
     * <p>Une case cochee derriere un general ferme ne casserait rien — l'ecran dirait le
     * contraire de ce que le jeu fait. Le clic ouvre donc le general avec elle.
     */
    private static void toggleSkill(cn.academy.ability.Skill skill) {
        boolean next = !ClientAbilityData.get().skillBreaksBlocks(skill);
        if (next && !breaksBlocksNow()) {
            setBreaksBlocks(true);
        }
        sendBreakPrefs(skill, next);
    }

    /**
     * Le choix part au serveur ; c'est lui qui l'ecrit et qui le renvoie.
     *
     * <p>{@code skill} nul vaut le drapeau general. Le client ne touche pas a son exemplaire :
     * l'affichage suivra la synchronisation, comme pour l'allumage de l'aptitude.
     */
    private static void sendBreakPrefs(cn.academy.ability.Skill skill, boolean value) {
        String key = skill == null ? "" : cn.academy.ability.BlockBreaking.key(skill);
        cn.academy.ability.network.AbilityNetwork.CHANNEL.sendToServer(
                new cn.academy.ability.network.BreakPrefsPacket(key, value));
    }

    // ------------------------------------------------------------------
    // Geometrie
    // ------------------------------------------------------------------

    private double originX() {
        return (width - PAGE_W * PAGE_SCALE) / 2.0;
    }

    private double originY() {
        return (height - PAGE_H * PAGE_SCALE) / 2.0;
    }

    /** La hauteur de la liste entiere, titres de categorie compris, en unites de page. */
    private double contentHeight() {
        return rows.size() * ROW_H;
    }

    private boolean scrollable() {
        return contentHeight() > AREA_H;
    }

    /** Le defilement du moment, en unites de page. */
    private double shift() {
        if (!scrollable()) return 0;
        return progress * (contentHeight() - AREA_H);
    }

    /** Le haut de la glissiere, en coordonnees de page. */
    private double thumbY() {
        return SCROLL_LOWER + progress * (SCROLL_UPPER - SCROLL_LOWER);
    }

    private void scrollBy(double pageOffset) {
        if (!scrollable()) return;
        double max = contentHeight() - AREA_H;
        double now = shift() + pageOffset;
        progress = Math.max(0, Math.min(1, now / max));
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // SANS lissage, comme l'original : cette page n'est pas dans ses `preloadMipmapTexture`, et
        // les mipmaps DILUENT ce qu'elle a de plus fin — le titre « Settings » et le trait blanc
        // qui le souligne sortaient gris, la ou le vrai mod les montre francs. Au plus proche
        // voisin chaque trait garde sa valeur pleine.

        graphics.pose().pushPose();
        graphics.pose().translate((float) originX(), (float) originY(), 0f);
        graphics.pose().scale((float) PAGE_SCALE, (float) PAGE_SCALE, 1f);

        image(graphics, TEX_BACK, 0, 0, PAGE_W, PAGE_H, 742, 923);

        graphics.flush();
        graphics.enableScissor(
                (int) Math.floor(originX() + AREA_X * PAGE_SCALE),
                (int) Math.floor(originY() + AREA_Y * PAGE_SCALE),
                (int) Math.ceil(originX() + (AREA_X + AREA_W) * PAGE_SCALE),
                (int) Math.ceil(originY() + (AREA_Y + AREA_H) * PAGE_SCALE));
        drawRows(graphics, mouseX, mouseY);
        graphics.disableScissor();

        drawScrollBar(graphics);

        graphics.pose().popPose();
        graphics.flush();
        graphics.setColor(1f, 1f, 1f, 1f);

        // La fleche est la SEULE chose dessinee hors de l'echelle de la page : a 0,2, ses bords
        // tombaient sur des quarts de pixel et sortaient en escalier — « il faudrait lui rajouter
        // de l'anti aliasing ». En pixels d'ecran, chaque pixel recoit l'alpha de sa couverture.
        graphics.enableScissor(
                (int) Math.floor(originX() + AREA_X * PAGE_SCALE),
                (int) Math.floor(originY() + AREA_Y * PAGE_SCALE),
                (int) Math.ceil(originX() + (AREA_X + AREA_W) * PAGE_SCALE),
                (int) Math.ceil(originY() + (AREA_Y + AREA_H) * PAGE_SCALE));
        drawArrows(graphics);
        graphics.disableScissor();
        graphics.flush();
    }

    /**
     * La petite fleche des lignes depliables, en pixels d'ecran.
     *
     * <p>Vers le bas quand la liste est ouverte, vers la droite quand elle est fermee : c'est la
     * convention des sites, et le document de l'original ne connaissait pas ces sous-options.
     */
    private void drawArrows(GuiGraphics graphics) {
        double shift = shift();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (!row.disclosure()) continue;
            double y = AREA_Y + i * ROW_H - shift;
            if (y > AREA_Y + AREA_H || y + ROW_H < AREA_Y) continue;
            triangle(graphics,
                    originX() + (AREA_X + ARROW_X) * PAGE_SCALE,
                    originY() + (y + (ROW_H - ARROW_H) / 2.0) * PAGE_SCALE,
                    ARROW_W * PAGE_SCALE, ARROW_H * PAGE_SCALE, row.open());
        }
    }

    /**
     * Un triangle plein, lisse par sur-echantillonnage.
     *
     * <p>Chaque pixel d'ecran est echantillonne seize fois : le compte des points tombes dans le
     * triangle donne son alpha. C'est un vrai anti-aliasing, pas un arrondi — quatre pixels de
     * large ne suffisent pas a cacher un escalier autrement.
     */
    private static void triangle(GuiGraphics graphics, double x, double y, double w, double h,
                                 boolean open) {
        int samples = 4;
        int px0 = (int) Math.floor(x);
        int py0 = (int) Math.floor(y);
        int px1 = (int) Math.ceil(x + w);
        int py1 = (int) Math.ceil(y + h);
        for (int py = py0; py < py1; py++) {
            for (int px = px0; px < px1; px++) {
                int inside = 0;
                for (int sy = 0; sy < samples; sy++) {
                    for (int sx = 0; sx < samples; sx++) {
                        if (inTriangle(x, y, w, h, open,
                                px + (sx + 0.5) / samples, py + (sy + 0.5) / samples)) {
                            inside++;
                        }
                    }
                }
                if (inside == 0) continue;
                int alpha = Math.round(255f * inside / (samples * samples));
                fill(graphics, px, py, 1, 1, (alpha << 24) | 0xFFFFFF);
            }
        }
    }

    /** Le point est-il dans le triangle ? Base a gauche (ou en haut), pointe a l'oppose. */
    private static boolean inTriangle(double x, double y, double w, double h, boolean open,
                                      double px, double py) {
        double u = (px - x) / w;
        double v = (py - y) / h;
        if (u < 0 || u > 1 || v < 0 || v > 1) return false;
        if (open) return Math.abs(u - 0.5) * 2 <= 1 - v;
        return Math.abs(v - 0.5) * 2 <= 1 - u;
    }

    private void drawRows(GuiGraphics graphics, int mouseX, int mouseY) {
        double pageX = (mouseX - originX()) / PAGE_SCALE;
        double pageY = (mouseY - originY()) / PAGE_SCALE;
        double shift = shift();

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            double y = AREA_Y + i * ROW_H - shift;
            if (y > AREA_Y + AREA_H || y + ROW_H < AREA_Y) continue;

            boolean overRow = pageY >= y && pageY < y + ROW_H
                    && pageX >= AREA_X && pageX <= AREA_X + ROW_W;

            if (row.kind() == Kind.HEADER) {
                // Le titre de categorie : son texte a 8, en blanc a 170/255, et son trait de
                // 4 unites colle au bas de la ligne (`alignHeight>BOTTOM`).
                AcademyFonts.drawTop(graphics, font,
                        Component.translatable(row.labelKey()).getString(),
                        (int) Math.round(AREA_X + HEAD_X), (int) Math.round(y), HEAD_COLOUR,
                        HEAD_CELL);
                fill(graphics, AREA_X, y + ROW_H - LINE_H, ROW_W, LINE_H, LINE_COLOUR);
                continue;
            }

            // Le libelle de la ligne, et sa case ou son controle a droite.
            boolean overControl = overRow && pageX >= AREA_X + CTRL_X - 20;
            drawControl(graphics, row, i, y, overControl);
            drawLabel(graphics, row, y, overRow);
        }
    }

    private void drawLabel(GuiGraphics graphics, Row row, double y, boolean over) {
        double labelY = y + CTRL_Y;
        if (over && capturing < 0) {
            fill(graphics, AREA_X, y, ROW_W, ROW_H, HOVER_VEIL);
        }
        boolean shifted = row.indent() || row.disclosure();
        double indent = shifted ? INDENT : 0;
        String label = fit(Component.translatable(row.labelKey()).getString(), LABEL_W - indent, CELL);
        AcademyFonts.drawTop(graphics, font, label,
                (int) Math.round(AREA_X + LABEL_X + indent),
                (int) Math.round(labelY), TEXT_COLOUR, CELL);
    }

    /**
     * Un texte coupe a la largeur de sa boite.
     *
     * <p>C'est la regle du {@code TextBox} de l'original : la chaine s'arrete des que la largeur
     * est atteinte, sans points de suspension. Sans elle, « Right Button » deborde de la page.
     */
    private static String fit(String text, double boxWidth, int cell) {
        double acc = 0;
        int i = 0;
        while (i < text.length() && acc < boxWidth) {
            int codePoint = text.codePointAt(i);
            acc += AcademyFonts.width(null, new String(Character.toChars(codePoint)), cell);
            i += Character.charCount(codePoint);
        }
        return text.substring(0, i);
    }

    private void drawControl(GuiGraphics graphics, Row row, int index, double y, boolean over) {
        switch (row.kind()) {
            case TOGGLE -> {
                boolean on = row.getter() != null && row.getter().getAsBoolean();
                double boxX = AREA_X + BOX_X;
                double boxY = y + CTRL_Y;
                image(graphics, on ? TEX_CHECK_ON : TEX_CHECK_OFF, boxX, boxY, BOX_SIZE, BOX_SIZE,
                        48, 48);
            }
            case KEY -> {
                boolean editing = capturing == index;
                if (over || editing) {
                    // Le Tint de la touche : transparent, puis un voile blanc a 20 % au survol.
                    fill(graphics, AREA_X + CTRL_X, y + CTRL_Y, CTRL_W, CTRL_H, HOVER_VEIL);
                }
                Component text = editing
                        ? Component.translatable("ac.settings.press_key")
                        : row.key().getTranslatedKeyMessage();
                // Son TextBox est aligne a GAUCHE dans les 160 de large, pas centre, et coupe a
                // cette largeur.
                AcademyFonts.drawTop(graphics, font, fit(text.getString(), CTRL_W, CELL),
                        (int) Math.round(AREA_X + CTRL_X), (int) Math.round(y + CTRL_Y),
                        editing ? EDIT_COLOUR : TEXT_COLOUR, CELL);
            }
            case ACTION -> {
                fill(graphics, AREA_X + CTRL_X, y + CTRL_Y, CTRL_W, CTRL_H,
                        over ? BUTTON_HOVER : BUTTON_COLOUR);
                Component text = Component.translatable("ac.settings.ok");
                String shown = fit(text.getString(), CTRL_W, CELL);
                int x = (int) Math.round(AREA_X + CTRL_X + CTRL_W / 2.0
                        - AcademyFonts.width(null, shown, CELL) / 2.0);
                AcademyFonts.drawTop(graphics, font, shown, x,
                        (int) Math.round(y + CTRL_Y), TEXT_COLOUR, CELL);
            }
        }
    }

    private void drawScrollBar(GuiGraphics graphics) {
        if (!scrollable()) return;
        image(graphics, TEX_ROLLBAR, SCROLL_X, thumbY(), SCROLL_W, SCROLL_H, 9, 96);
    }

    // ------------------------------------------------------------------
    // La souris et le clavier
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        double pageX = (mouseX - originX()) / PAGE_SCALE;
        double pageY = (mouseY - originY()) / PAGE_SCALE;

        // La glissiere se prend par ou on la touche.
        if (scrollable() && pageX >= SCROLL_X && pageX <= SCROLL_X + SCROLL_W
                && pageY >= thumbY() - 4 && pageY <= thumbY() + SCROLL_H + 4) {
            dragging = true;
            dragGrip = pageY - thumbY();
            return true;
        }

        // Une ligne : seule sa partie droite repond, comme chez l'original, ou la case et la
        // touche sont des widgets a part. L'exception est la fleche d'une ligne depliable, qui a
        // sa propre zone a gauche du libelle.
        double shift = shift();
        for (int i = 0; i < rows.size(); i++) {
            double y = AREA_Y + i * ROW_H - shift;
            if (pageY < y || pageY >= y + ROW_H) continue;

            Row row = rows.get(i);
            if (row.disclosure() && pageX >= AREA_X + ARROW_X - 6
                    && pageX <= AREA_X + ARROW_X + ARROW_W + 6) {
                toggleBreakDisclosure();
                return true;
            }
            if (pageX < AREA_X + CTRL_X - 20 || pageX > AREA_X + ROW_W) continue;

            switch (row.kind()) {
                case KEY -> capturing = i;
                case TOGGLE -> {
                    if (row.action() != null) row.action().run();
                }
                case ACTION -> {
                    if (row.action() != null) row.action().run();
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (dragging) {
            double top = (mouseY - originY()) / PAGE_SCALE - dragGrip - SCROLL_LOWER;
            progress = Math.max(0, Math.min(1, top / (SCROLL_UPPER - SCROLL_LOWER)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scrollBy(-delta * ROW_H);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing >= 0) {
            // Echap annule la capture, comme partout ailleurs.
            if (keyCode == 256) {
                capturing = -1;
                return true;
            }
            Row row = rows.get(capturing);
            if (row != null && row.key() != null) {
                row.key().setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                        .getOrCreate(keyCode));
                KeyMapping.resetMapping();
                if (minecraft != null) minecraft.options.save();
            }
            capturing = -1;
            return true;
        }

        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        // PAS de `Config.save()` ici. Chaque bascule ecrit DEJA sa valeur et le fichier
        // (`Config.toggle*`) ; sauvegarder encore une fois n'apportait rien et pouvait meme
        // effacer une modification faite a la main dans le .toml pendant la partie : une
        // sauvegarde reecrit le fichier ENTIER depuis ce que le jeu a lu au demarrage.
        // Ouverte depuis le terminal, l'application le rend en se fermant ; ouverte depuis
        // l'ecran de personnalisation du HUD, elle rend cet ecran-la.
        if (parent != null) {
            minecraft.setScreen(parent);
        } else {
            cn.academy.terminal.client.AppScreens.close();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // Le dessin
    // ------------------------------------------------------------------

    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void fill(GuiGraphics graphics, double x, double y, double width,
                             double height, int colour) {
        blend();
        graphics.fill((int) Math.round(x), (int) Math.round(y),
                (int) Math.round(x + width), (int) Math.round(y + height), colour);
    }

    private static void image(GuiGraphics graphics, ResourceLocation texture, double x, double y,
                             double width, double height, int texWidth, int texHeight) {
        blend();
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.blit(texture, (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height),
                0f, 0f, texWidth, texHeight, texWidth, texHeight);
    }
}
