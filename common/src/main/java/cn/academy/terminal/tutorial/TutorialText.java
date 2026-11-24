package cn.academy.terminal.tutorial;

/**
 * Le texte d'un tutoriel : son titre, son resume, et son contenu.
 *
 * <p>Portage de la lecture que l'original faisait dans {@code ACTutorial}. Ses fichiers
 * sont du markdown tres simple, ou trois balises separent les morceaux — {@code ![title]},
 * {@code ![brief]} et {@code ![content]} — et ou une quatrieme, {@code ![misakaname]}, est
 * remplacee a l'affichage par le nom du joueur.
 *
 * <p>La classe ne connait ni police, ni ecran, ni Minecraft : elle rend trois chaines, et
 * la mise en page appartient a l'ecran. C'est ce decoupage qui permet de verifier le
 * contenu livre en JUnit plutot qu'a l'oeil dans un jeu.
 */
public record TutorialText(String title, String brief, String content) {

    public static final String TITLE = "![title]";
    public static final String BRIEF = "![brief]";
    public static final String CONTENT = "![content]";

    /** La balise que l'original remplacait par le nom du joueur. */
    private static final String PLAYER_NAME = "![misakaname]";

    /**
     * Lit un fichier de tutoriel.
     *
     * <p>Un texte sans balise du tout n'est pas un tutoriel : il vaut mieux rendre un
     * texte vide, que l'ecran annoncera, que d'afficher a leur place les balises brutes
     * d'un fichier casse. C'est ce que l'original faisait sous le nom d'un texte
     * {@code UNKNOWN}, choix que le port laisse a l'ecran pour que le message soit
     * traduisible.
     */
    public static TutorialText parse(String raw) {
        if (raw == null) return empty();

        String title = section(raw, TITLE, BRIEF);
        String brief = section(raw, BRIEF, CONTENT);
        String content = section(raw, CONTENT, null);
        if (title.isEmpty() && content.isEmpty()) return empty();

        return new TutorialText(title, brief, content);
    }

    /** Le contenu tel qu'il s'ecrit pour ce joueur. */
    public String contentFor(String playerName) {
        return content.replace(PLAYER_NAME, playerName);
    }

    /** Vrai quand il n'y a rien a montrer : fichier absent, ou balises absentes. */
    public boolean isEmpty() {
        return title.isEmpty() && content.isEmpty();
    }

    public static TutorialText empty() {
        return new TutorialText("", "", "");
    }

    /**
     * Le texte entre une balise et la suivante.
     *
     * <p>Un morceau sans balise de fin va jusqu'a la fin du fichier : c'est le cas du
     * contenu, qui ferme toujours le fichier. Une balise absente donne une chaine vide,
     * et non le texte entier — sans quoi un fichier sans titre afficherait tout son
     * contenu en guise de titre.
     */
    private static String section(String raw, String from, String to) {
        int start = raw.indexOf(from);
        if (start < 0) return "";

        int begin = start + from.length();
        int end = to == null ? raw.length() : raw.indexOf(to, begin);
        if (end < 0) end = raw.length();

        return raw.substring(begin, end).trim();
    }
}
