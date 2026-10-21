package cn.academy.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Un texte trop large pour sa boite : il defile au lieu de deborder.
 *
 * <p>Constate par le joueur sur l'ecran du noeud sans fil : en francais, « Mot de passe » remplit plus
 * que les quarante unites de la colonne des noms et passe <b>par-dessus</b> la colonne des valeurs,
 * donc sur le champ du mot de passe. L'original ne pouvait pas le voir : ses libelles anglais
 * tenaient dans la meme boite, et son {@code TextBox} ne coupait rien — il dessinait son texte entier,
 * quelle que soit sa largeur. La boite de quarante unites vient du document ({@code page_node} et ses
 * pareils), donc elargir la colonne d'une langue casserait les autres.
 *
 * <p>La regle retenue, et c'est celle que le joueur a demandee : <b>un texte qui ne tient pas dans sa
 * boite est coupe a la boite et defile</b>. L'anglais tient, donc rien ne bouge ; une traduction plus
 * longue se lit en entier, sans jamais empieter sur ce qui est a cote, et elle garde sa taille — un
 * texte retreci se lit mal, alors qu'un texte qui defile se lit en entier. Seul compte le fait de
 * tenir dans la boite : c'est ce que « plus grand que le texte anglais » veut dire une fois mesure, et
 * cela vaut aussi pour un nom de reseau que le joueur a tape lui-meme.
 *
 * <h2>Le rythme</h2>
 *
 * <p>Une pause au debut — le temps de lire les premieres lettres —, un defilement lent vers la
 * gauche, une pause a la fin, puis le texte revient a son debut. Les deux pauses sont ce qui rend la
 * chose lisible : un defilement continu sans arret ne laisse jamais le temps de lire le debut d'un
 * libelle de huit pixels de haut.
 *
 * <p>{@link #offset(long, int)} est separable du dessin, et c'est fait expres : la seule chose qui
 * puisse se tromper ici est cette arithmetique, et elle se verifie sans jeu.
 */
public final class MarqueeText {

    private MarqueeText() {}

    /** Combien de temps le texte reste immobile avant de partir, et une fois arrive. */
    public static final long PAUSE_MS = 1_500L;

    /** Millisecondes par pixel de defilement : lent, pour une cellule de huit a dix pixels. */
    public static final long MS_PER_PIXEL = 25L;

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
     * @param frozen vrai pour ne pas defiler : un champ en cours de saisie garde son texte en place,
     *     sinon le curseur et les lettres qu'on vient de taper glisseraient sous les yeux
     * @param travel la largeur du texte moins celle de la boite
     */
    public static void drawScrolled(GuiGraphics graphics, Font font, String content, int x, int y,
                                    int boxWidth, int cell, int colour, int travel, boolean frozen) {
        int shift = frozen ? 0 : offset(System.currentTimeMillis(), travel);

        // La coupure : le rectangle de mise en scene du jeu, qui rogne tout ce qui est dessine
        // dedans. Une unite de marge en haut et en bas, pour ne pas entamer les jambages et les
        // accents qui touchent les bords de la cellule.
        graphics.enableScissor(x, y - 1, x + boxWidth, y + cell + 1);
        AcademyFonts.drawTop(graphics, font, content, x - shift, y, colour, cell);
        graphics.disableScissor();
    }
}
