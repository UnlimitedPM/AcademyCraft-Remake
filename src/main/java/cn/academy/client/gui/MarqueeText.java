package cn.academy.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * Un texte trop large pour sa boite : il retrecit un peu, puis il defile, au lieu de deborder.
 *
 * <p>Constate par le joueur sur l'ecran du noeud sans fil : en francais, « Mot de passe » remplit plus
 * que la largeur de la colonne des noms et passe <b>par-dessus</b> la colonne des valeurs, donc sur le
 * champ du mot de passe. L'original ne pouvait pas le voir : ses libelles anglais tenaient dans la
 * meme boite, et son {@code TextBox} ne coupait rien — il dessinait son texte entier, quelle que soit
 * sa largeur. La largeur de cette colonne vient du document ({@code page_node} et ses pareils), donc
 * l'elargir pour une langue casserait les autres.
 *
 * <h2>Deux remedes, dans cet ordre</h2>
 *
 * <ol>
 *   <li><b>Retrecir un peu.</b> Un debordement de quelques pour cent se corrige en reduisant la
 *       cellule du texte juste assez pour qu'il tienne — jusqu'a {@link #MIN_SHRINK}. Un libelle qui
 *       passe de huit a sept unites ne se voit pas ; un texte qui defile pour gagner quatre pixels,
 *       si : le joueur y perd sa premiere lettre.</li>
 *   <li><b>Faire defiler.</b> Au-dela, le texte est coupe a sa boite et defile : pause au debut, un
 *       pixel toutes les {@link #MS_PER_PIXEL} millisecondes, pause a la fin, puis retour au debut.
 *       C'est le cas des textes dont on ne connait pas la longueur d'avance — le nom d'un reseau, par
 *       exemple — et c'est ce que le joueur a demande.</li>
 * </ol>
 *
 * <p>Dans les deux cas le texte <b>ne sort plus de sa boite</b> : c'est la seule regle qui compte, et
 * c'est celle qui manquait.
 *
 * <p>Seul compte le fait de tenir dans la boite, jamais la langue : « plus grand que le texte anglais »
 * se mesure ainsi, et cela vaut aussi pour un nom que le joueur a tape lui-meme.
 *
 * <p>{@link #plan} et {@link #offset(long, int)} sont separables du dessin, et c'est fait expres : ce
 * sont les deux seules choses qui puissent se tromper ici, et elles se verifient sans jeu.
 */
public final class MarqueeText {

    private MarqueeText() {}

    /** Combien de temps le texte reste immobile avant de partir, et une fois arrive. */
    public static final long PAUSE_MS = 1_500L;

    /** Millisecondes par pixel de defilement : lent, pour une cellule de sept a dix pixels. */
    public static final long MS_PER_PIXEL = 25L;

    /**
     * La plus petite cellule acceptable pour un texte retreci, en part de la cellule demandee.
     *
     * <p>Quatre cinquiemes : un libelle de huit unites peut descendre a 6,4, pas plus. En dessous, il
     * ne ressemble plus aux autres lignes du panneau — mieux vaut alors le faire defiler.
     */
    public static final double MIN_SHRINK = 0.8;

    /**
     * Comment dessiner un texte : a quelle cellule, a quelle position, et faut-il le faire defiler.
     *
     * @param cell   la cellule a employer, en pixels
     * @param x      l'abscisse du debut du texte
     * @param y      le haut de sa cellule
     * @param scroll vrai si le texte doit defiler dans sa boite (il commence alors au bord gauche)
     */
    public record Plan(int cell, int x, int y, boolean scroll) {}

    /**
     * Le plan de dessin d'un texte dans sa boite.
     *
     * @param frozen vrai pour ne rien reduire ni faire defiler : un champ en cours de saisie garde sa
     *     taille et sa place, sinon ses lettres et son curseur glisseraient sous les yeux
     */
    public static Plan plan(Font font, String content, String align, String heightAlign,
                            int x, int y, int w, int h, int cell, boolean frozen) {
        int textWidth = AcademyFonts.width(font, content, cell);

        // Il tient, ou il ne doit pas bouger : rien a faire.
        if (textWidth <= w || frozen) {
            return new Plan(cell, alignX(align, x, w, textWidth), alignY(heightAlign, y, h, cell),
                    false);
        }

        // Un petit debordement se corrige en retrecissant : le texte reste entier et lisible.
        int fitted = fittedCell(font, content, cell, w);
        if (fitted > 0) {
            int width = AcademyFonts.width(font, content, fitted);
            return new Plan(fitted, alignX(align, x, w, width), alignY(heightAlign, y, h, fitted),
                    false);
        }

        // Trop long pour cela : il defile, coupe a sa boite, depuis le bord gauche.
        return new Plan(cell, x, alignY(heightAlign, y, h, cell), true);
    }

    /**
     * La cellule qui fait tenir le texte dans la largeur donnee, ou 0 s'il faudrait trop le reduire.
     *
     * <p>Les avances de la police sont proportionnelles a la cellule, donc le rapport des largeurs
     * donne la reponse du premier coup ; les arrondis de glyphe peuvent la faire echouer d'un pixel,
     * d'ou les quelques pas de verification.
     */
    public static int fittedCell(Font font, String content, int cell, int boxWidth) {
        if (content.isEmpty() || boxWidth <= 0 || cell <= 1) return 0;

        int textWidth = AcademyFonts.width(font, content, cell);
        if (textWidth <= boxWidth) return cell;

        int floor = (int) Math.floor(cell * MIN_SHRINK);
        int candidate = (int) Math.floor((double) cell * boxWidth / textWidth);
        candidate = Math.max(floor, Math.min(cell - 1, candidate));

        for (int size = candidate; size >= floor; size--) {
            if (AcademyFonts.width(font, content, size) <= boxWidth) return size;
        }
        return 0;
    }

    /**
     * De combien le texte est decale vers la gauche, a cet instant.
     *
     * @param now    l'horloge, en millisecondes
     * @param travel combien de pixels il reste a parcourir pour montrer la fin du texte — donc la
     *     largeur du texte moins celle de la boite, et jamais negatif
     */
    public static int offset(long now, int travel) {
        if (travel <= 0) return 0;

        long scroll = travel * MS_PER_PIXEL;
        long cycle = PAUSE_MS + scroll + PAUSE_MS;
        long phase = Math.floorMod(now, cycle);

        if (phase < PAUSE_MS) return 0;
        if (phase < PAUSE_MS + scroll) return (int) ((phase - PAUSE_MS) / MS_PER_PIXEL);
        return travel;
    }

    /**
     * Le texte, coupe a sa boite, decale comme dit {@link #offset(long, int)}.
     *
     * @param travel la largeur du texte moins celle de la boite
     */
    public static void drawScrolled(GuiGraphics graphics, Font font, String content, int x, int y,
                                    int boxWidth, int cell, int colour, int travel) {
        int shift = offset(System.currentTimeMillis(), travel);

        // La coupure : le rectangle de mise en scene du jeu, qui rogne tout ce qui est dessine
        // dedans. Une unite de marge en haut et en bas, pour ne pas entamer les jambages et les
        // accents qui touchent les bords de la cellule.
        graphics.enableScissor(x, y - 1, x + boxWidth, y + cell + 1);
        AcademyFonts.drawTop(graphics, font, content, x - shift, y, colour, cell);
        graphics.disableScissor();
    }

    /** {@code LEFT}, {@code CENTER} ou {@code RIGHT}, dans la boite. */
    private static int alignX(String align, int x, int w, int textWidth) {
        return switch (align.toUpperCase(Locale.ROOT)) {
            case "CENTER" -> x + (w - textWidth) / 2;
            case "RIGHT" -> x + w - textWidth;
            default -> x;
        };
    }

    /** {@code TOP}, {@code CENTER} ou {@code BOTTOM} : le haut de la cellule dans la boite. */
    private static int alignY(String heightAlign, int y, int h, int cell) {
        return switch (heightAlign.toUpperCase(Locale.ROOT)) {
            case "CENTER" -> y + Math.max(0, h - cell) / 2;
            case "BOTTOM" -> y + Math.max(0, h - cell);
            default -> y;
        };
    }
}
