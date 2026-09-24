package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.DebugConsoleLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Le menu F4, celui de l'original : trois etats qui tournent a chaque appui.
 *
 * <ol>
 *   <li>{@code NONE} — rien ;</li>
 *   <li>{@code INFO} — les informations du joueur : categorie, niveau, points de controle,
 *       surcharge, brouillage, avancement du niveau ;</li>
 *   <li>{@code SKILLS} — l'etat des competences, une ligne par competence.</li>
 * </ol>
 *
 * <p>C'est <b>le seul endroit</b> ou les points de controle et la surcharge se lisent en jeu,
 * comme chez l'original.
 *
 * <p>Le texte est dessine <b>avec ombre</b> : l'original le dessinait deux fois, une passe
 * sombre puis la claire, ce qui donne exactement l'ombre de la police du jeu.
 */
@OnlyIn(Dist.CLIENT)
public final class DebugConsole {

    /** Les trois etats, dans l'ordre ou F4 les fait tourner. */
    public enum State { NONE, INFO, SKILLS }

    private static final int LEFT = 10;
    private static final int TOP = 10;
    private static final int COLOR = 0xFFFFFFFF;

    private static State state = State.NONE;

    private DebugConsole() {
    }

    /** L'appui sur F4 : etat suivant, dans le tour. */
    public static void cycle() {
        State[] states = State.values();
        state = states[(state.ordinal() + 1) % states.length];
    }

    /** L'etat courant, pour les tests et pour savoir si l'ecran occupe le clavier. */
    public static State getState() {
        return state;
    }

    public static void render(GuiGraphics graphics) {
        if (state == State.NONE) return;

        AbilityData data = ClientAbilityData.get();
        List<Category> categories = playerCategories(data);
        List<String> lines = state == State.SKILLS
                ? DebugConsoleLines.skills(data, categories)
                : DebugConsoleLines.info(data, highest(categories));

        int lineHeight = Minecraft.getInstance().font.lineHeight;
        int y = TOP;
        for (String line : lines) {
            graphics.drawString(Minecraft.getInstance().font, line, LEFT, y, COLOR, true);
            y += lineHeight;
        }
    }

    /**
     * Les categories que ce joueur connait, dans l'ordre du registre.
     *
     * <p>L'original n'en avait qu'une. Le port en autorise plusieurs : l'ecran des competences
     * les montre donc toutes, sans quoi celles des autres categories sembleraient manquer.
     */
    private static List<Category> playerCategories(AbilityData data) {
        List<Category> found = new ArrayList<>();
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            if (data.hasLearned(category)) {
                found.add(category);
            }
        }
        return found;
    }

    /**
     * La categorie que l'ecran d'informations montre : la plus haute.
     *
     * <p>C'est la meme regle que celle qui decide du plafond de surcharge.
     */
    private static Category highest(List<Category> categories) {
        Category best = null;
        int bestLevel = 0;
        for (Category category : categories) {
            int level = ClientAbilityData.get().getCategoryLevel(category);
            if (best == null || level > bestLevel) {
                best = category;
                bestLevel = level;
            }
        }
        return best;
    }
}
