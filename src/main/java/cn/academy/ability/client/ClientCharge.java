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
    private static int ticks;
    private static int maxTicks = 1;

    private ClientCharge() {}

    /** Ouvre une charge. {@code maxTicks} sert uniquement a calculer la proportion. */
    public static void begin(int maxTicks) {
        active = true;
        ticks = 0;
        ClientCharge.maxTicks = Math.max(1, maxTicks);
    }

    /** Avance d'un tick tant que la touche est tenue. */
    public static void tick() {
        if (active) ticks++;
    }

    /** Ferme la charge, au relachement de la touche. */
    public static void end() {
        active = false;
        ticks = 0;
    }

    public static boolean isActive() {
        return active;
    }

    /** Part de la charge maximale atteinte, entre 0 et 1. */
    public static float getFraction() {
        return active ? Math.min(1.0f, ticks / (float) maxTicks) : 0.0f;
    }
}
