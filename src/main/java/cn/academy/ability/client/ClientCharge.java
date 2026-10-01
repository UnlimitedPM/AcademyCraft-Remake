package cn.academy.ability.client;

/**
 * La charge en cours, du point de vue du client, pour l'afficher.
 *
 * <p>Le serveur tient son propre compteur ({@code AbilityData#tickCharges}) : ce n'est
 * pas ce nombre-la qui decide de la puissance de la competence, seulement ce que le
 * joueur voit pendant qu'il tient la touche. Sans cet affichage, une competence qui se
 * charge serait invisible : le joueur n'aurait aucun moyen de savoir que sa touche
 * fait quelque chose, ni quand l'effet atteint son maximum.
 */
public final class ClientCharge {

    private static boolean active;
    private static boolean sustained;
    private static int ticks;
    private static int maxTicks = 1;
    private static String skill;

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
        active = true;
        sustained = false;
        ticks = 0;
        ClientCharge.skill = skill;
        ClientCharge.maxTicks = Math.max(0, maxTicks);
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
        active = false;
        sustained = true;
        ticks = 0;
        ClientCharge.skill = skill;
    }

    /** Avance d'un tick tant que la touche est tenue. */
    public static void tick() {
        if (active || sustained) ticks++;
    }

    /** Ferme la charge, au relachement de la touche. */
    public static void end() {
        active = false;
        sustained = false;
        ticks = 0;
        skill = null;
    }

    /**
     * Le nom de la competence en cours, ou {@code null}.
     *
     * Sert a la boucle sonore du maintien : c'est elle qui sait quel son suivre et quand
     * le couper, sans que la competence ait a le dire.
     */
    public static String getSkill() {
        return skill;
    }

    public static boolean isActive() {
        return active;
    }

    /** Vrai pendant un maintien : le bouclier tient, sans duree programmee. */
    public static boolean isSustained() {
        return sustained;
    }

    /** Part de la charge maximale atteinte, entre 0 et 1. Nulle si la charge n'a pas de maximum. */
    public static float getFraction() {
        return active && maxTicks > 0 ? Math.min(1.0f, ticks / (float) maxTicks) : 0.0f;
    }

    /**
     * Ticks ecoules depuis l'appui.
     *
     * Sert a la barre de charge, et au rendu du bouclier qui grossit et accelere avec
     * l'age du maintien.
     */
    public static int getTicks() {
        return ticks;
    }
}
