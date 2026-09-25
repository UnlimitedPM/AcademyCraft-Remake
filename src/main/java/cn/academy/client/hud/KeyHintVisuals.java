package cn.academy.client.hud;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;

/**
 * La geometrie du rappel des touches (Control Hint), et ce qu'une ligne montre.
 *
 * <p>Portage de {@code KeyHintUI} de l'original : une <b>ligne par touche d'aptitude</b>, une
 * plaque en degrade, le capuchon de la touche a gauche et l'icone de la competence a droite.
 * Ses mesures sont les siennes, a l'unite pres ({@code drawSingle}) :
 *
 * <ul>
 *   <li>plaque 185x83, une ligne tous les 92 ;</li>
 *   <li>capuchon 70x70 a (24, 10), cadre de l'icone 72x72 a (94, 5), icone 62x62 a (99, 10) ;</li>
 *   <li>le tout a l'echelle {@code 0,23}, celle de son {@code SCALE}.</li>
 * </ul>
 *
 * <p>Une difference assumee : l'original ne listait que les delegues <b>existants</b>, donc une
 * touche sans competence ne s'affichait pas du tout. Le port, lui, dessine toujours ses quatre
 * touches — celles de son systeme de prereglages — et une touche sans competence montre son
 * capuchon et un cadre vide : le joueur voit ainsi quelles touches existent.
 *
 * <p>Aucun type Minecraft ici : c'est un modele, donc relisible en JUnit.
 */
public final class KeyHintVisuals {

    /** L'echelle du rappel, chez l'original ({@code SCALE = 0.23f}). */
    public static final float SCALE = 0.23f;

    /** Une ligne par touche d'aptitude. */
    public static final int ROWS = 4;

    /** Le pas vertical d'une ligne, et la taille de sa plaque. */
    public static final int ROW_STEP = 92;
    public static final int PLATE_W = 185;
    public static final int PLATE_H = 83;

    /** Le capuchon de la touche, dans la ligne. */
    public static final int CAP_X = 24;
    public static final int CAP_Y = 10;
    public static final int CAP_SIZE = 70;

    /** Le cadre de l'icone, et l'icone dedans. */
    public static final int FRAME_X = 94;
    public static final int FRAME_Y = 5;
    public static final int FRAME_SIZE = 72;
    public static final int ICON_X = 99;
    public static final int ICON_Y = 10;
    public static final int ICON_SIZE = 62;

    /** La couleur de l'etiquette d'une touche, chez l'original ({@code 0xff194246}). */
    public static final int LABEL_COLOR = 0xFF194246;

    /** Le gris qui noircit le bas d'une icone en recharge : gris 0,6 a 30 %. */
    public static final int COOLDOWN_OVERLAY = 0x4C999999;

    /** De quel genre de touche il s'agit : c'est l'image du capuchon qui change. */
    public enum Cap {
        MOUSE_LEFT, MOUSE_RIGHT, MOUSE_OTHER, KEY
    }

    private KeyHintVisuals() {}

    /** Le genre de touche, tel que le dit son {@code KeyMapping}. */
    public static Cap cap(boolean mouse, int button) {
        if (!mouse) return Cap.KEY;
        return switch (button) {
            case 0 -> Cap.MOUSE_LEFT;
            case 1 -> Cap.MOUSE_RIGHT;
            default -> Cap.MOUSE_OTHER;
        };
    }

    /** Le nom du fichier du capuchon, dans {@code textures/guis/key_hint/}. */
    public static String capTexture(Cap cap, String label) {
        return switch (cap) {
            case MOUSE_LEFT -> "mouse_left";
            case MOUSE_RIGHT -> "mouse_right";
            case MOUSE_OTHER -> "mouse_generic";
            case KEY -> label.length() <= 2 ? "key_short" : "key_long";
        };
    }

    /** Ce que le capuchon porte : la lettre, le numero du bouton, ou rien du tout. */
    public static String capLabel(Cap cap, String label, int button) {
        return switch (cap) {
            case MOUSE_LEFT, MOUSE_RIGHT -> "";
            case MOUSE_OTHER -> String.valueOf(button);
            case KEY -> label;
        };
    }

    /**
     * La part de recharge restante, entre 0 et 1.
     *
     * <p>L'original noircissait le bas de l'icone de cette part, ce qui dit d'un coup d'oeil
     * combien de temps il reste — voir {@link #COOLDOWN_OVERLAY}.
     */
    public static float cooldownFraction(int ticksLeft, int totalTicks) {
        if (ticksLeft <= 0 || totalTicks <= 0) return 0f;
        return Math.min(1f, ticksLeft / (float) totalTicks);
    }

    /**
     * Une competence par son nom, ou {@code null} si aucune categorie ne la porte.
     *
     * <p>Le prereglage ne garde que le <b>nom</b> de la competence : les quatre categories
     * sont donc fouillees, comme partout ailleurs dans le port.
     */
    public static Skill skillByName(String name) {
        if (name == null) return null;
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            Skill skill = category.getSkill(name);
            if (skill != null) return skill;
        }
        return null;
    }
}
