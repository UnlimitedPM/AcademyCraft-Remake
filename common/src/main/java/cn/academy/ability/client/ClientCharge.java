package cn.academy.ability.client;

/**
 * Les charges en cours, du point de vue du client, pour les afficher.
 *
 * <p>Le serveur tient ses propres compteurs ({@code AbilityData#tickCharges}, une entree par
 * competence) : ce n'est pas ce nombre-la qui decide de la puissance de la competence, seulement ce
 * que le joueur voit pendant qu'il tient la touche. Sans cet affichage, une competence qui se charge
 * serait invisible : le joueur n'aurait aucun moyen de savoir que sa touche fait quelque chose, ni
 * quand l'effet atteint son maximum.
 *
 * <h2>UNE CHARGE PAR COMPETENCE</h2>
 *
 * <p>Et pas une seule pour tout le jeu, ce que le port avait fait au depart. L'original tenait une
 * <b>liste de contextes vivants</b> ({@code ContextManager.LocalManager.alive}), donc un joueur
 * pouvait voler avec les ailes de tempete ET charger autre chose en meme temps — et le serveur du
 * port fait deja pareil (une charge par competence). Avec un seul compteur, ouvrir la seconde
 * charge ecrasait la premiere : le vol retombait (les ailes lisaient l'age de l'autre competence),
 * et le verrou de deplacement des ailes restait ferme — « un etat presque fige ou je ne peux rien
 * faire ». Le joueur : « si j'utilise les ailes et que pendant que je les utilise j'utilise un autre
 * pouvoir, ca casse le pouvoir des ailes ». Et la meme faute annulait le geste du poing quand une
 * seconde charge s'ouvrait par-dessus — voir {@code HandSwing}.
 */
public final class ClientCharge {

    /** Une competence ouverte : sa charge ou son maintien, et son age en ticks. */
    private static final class State {
        boolean active;
        boolean sustained;
        int ticks;
        int maxTicks;
    }

    /**
     * Les competences ouvertes, par nom.
     *
     * <p>Un {@code LinkedHashMap} parce que l'ordre d'ouverture compte : c'est lui qui dit laquelle
     * est « la charge en cours » quand on demande le nom sans le preciser — le son de boucle et le
     * temoin n'en suivent qu'une, faute de savoir en jouer deux.
     */
    private static final java.util.Map<String, State> OPEN = new java.util.LinkedHashMap<>();

    private ClientCharge() {}

    /**
     * Ouvre une charge. {@code maxTicks} sert uniquement a calculer la proportion.
     *
     * <p>Un maximum de zero ou moins dit que la charge n'en a pas : le compteur avance quand meme,
     * parce que l'<b>age</b> d'une charge sert a autre chose qu'a remplir une barre. La portee du
     * fantome de teleportation de la marque grandit avec lui, par exemple — et le port n'ouvrait
     * le compteur que pour les charges bornees, donc ce fantome restait cloue a deux blocs.
     */
    public static void begin(String skill, int maxTicks) {
        State state = new State();
        state.active = true;
        state.maxTicks = Math.max(0, maxTicks);
        // Une charge rouverte repasse en tete : elle est la plus recente, et c'est elle que suit le
        // nom « en cours ».
        OPEN.remove(skill);
        OPEN.put(skill, state);
    }

    /**
     * Ouvre un maintien, qui n'a pas de fin programmee.
     *
     * Le bouclier de l'original se voyait : il avait son entite et ses sons. Le port
     * n'a pas encore de rendu pour lui, donc ce temoin est ce qui dit au joueur que sa
     * touche tient quelque chose — sans lui, le maintien serait invisible jusqu'a ce
     * que ses ressources s'epuisent.
     */
    public static void beginSustained(String skill) {
        State state = new State();
        state.sustained = true;
        OPEN.remove(skill);
        OPEN.put(skill, state);
    }

    /** Avance d'un tick la charge de cette competence-la, et d'elle seule. */
    public static void tick(String skill) {
        State state = OPEN.get(skill);
        if (state != null) state.ticks++;
    }

    /**
     * Ferme la charge de cette competence. Les autres ne bougent pas : c'est justement ce que le
     * port faisait de travers.
     */
    public static void end(String skill) {
        OPEN.remove(skill);
    }

    /** Tout oublier : un monde quitte n'a plus aucune charge ouverte. */
    public static void clear() {
        OPEN.clear();
    }

    /**
     * Les competences dont une charge est ouverte, la plus recente d'abord.
     *
     * <p>Sert aux rares effets qui doivent retrouver <b>leur</b> competence dans la pile : une
     * ondulation d'ecran n'appartient pas forcement a la derniere charge ouverte, puisque le joueur
     * peut tenir une veille et charger autre chose en meme temps. Voir {@code RippleOverlay}.
     */
    public static java.util.List<String> openSkills() {
        java.util.List<String> names = new java.util.ArrayList<>(OPEN.keySet());
        java.util.Collections.reverse(names);
        return names;
    }

    /**
     * Le nom de la competence en cours, ou {@code null}.
     *
     * <p>C'est la plus recemment ouverte qui est encore ouverte : si elle se referme, la precedente
     * reprend la main, et rien ne reste bloque.
     *
     * <p>Sert a la boucle sonore du maintien : c'est elle qui sait quel son suivre et quand
     * le couper, sans que la competence ait a le dire.
     */
    public static String getSkill() {
        String last = null;
        for (String name : OPEN.keySet()) last = name;
        return last;
    }

    /** Vrai si une charge est ouverte pour cette competence. */
    public static boolean isOpen(String skill) {
        return OPEN.containsKey(skill);
    }

    /** Vrai si une charge OU un maintien est ouvert : voir {@code ClientAbilityData.tick}. */
    public static boolean anyOpen() {
        return !OPEN.isEmpty();
    }

    /** Vrai pendant un maintien de cette competence : le bouclier tient, sans duree programmee. */
    public static boolean isSustained(String skill) {
        State state = OPEN.get(skill);
        return state != null && state.sustained;
    }

    /** Part de la charge maximale atteinte, entre 0 et 1. Nulle si la charge n'a pas de maximum. */
    public static float getFraction(String skill) {
        State state = OPEN.get(skill);
        return state != null && state.active && state.maxTicks > 0
                ? Math.min(1.0f, state.ticks / (float) state.maxTicks)
                : 0.0f;
    }

    /**
     * Ticks ecoules depuis l'appui de cette competence.
     *
     * Sert a la barre de charge, et au rendu du bouclier qui grossit et accelere avec
     * l'age du maintien.
     */
    public static int getTicks(String skill) {
        State state = OPEN.get(skill);
        return state == null ? 0 : state.ticks;
    }
}
