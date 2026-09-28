package cn.academy.discord;

/**
 * Le rythme des envois.
 *
 * <p>Discord refuse plus d'une mise a jour toutes les quinze secondes : la demande est
 * rejetee, et la fiche ne change pas. Attendre ne coute donc rien, et evite de se faire
 * refuser : la fiche voulue est gardee, et elle part des que la fenetre est ouverte.
 *
 * <p>Deux regles seulement :
 * <ul>
 *   <li>une fiche identique a la derniere envoyee ne repart jamais — c'est le cas courant,
 *       puisque la situation est relue plusieurs fois par seconde ;</li>
 *   <li>un changement qui tombe trop tot attend la fin de la fenetre, il n'est pas perdu.</li>
 * </ul>
 *
 * <p>Le temps est passe en parametre : le rythme se verifie donc sans attendre pour de vrai.
 */
public final class PresenceGate {

    /** Le delai minimal entre deux envois, en millisecondes : la limite de Discord. */
    public static final long MIN_INTERVAL_MILLIS = 15_000L;

    /** La derniere fiche envoyee, et quand. */
    private DiscordPresence sent;
    private long sentAt;

    /**
     * Oublie tout.
     *
     * <p>A appeler apres une reconnexion : la nouvelle conversation a sa propre fenetre, et
     * Discord attend une fiche avant de montrer quoi que ce soit.
     */
    public void reset() {
        sent = null;
        sentAt = 0L;
    }

    /** La fiche a envoyer maintenant, ou {@code null} s'il n'y a rien a faire. */
    public DiscordPresence due(DiscordPresence wanted, long nowMillis) {
        if (wanted == null || wanted.equals(sent)) return null;
        if (sent != null && nowMillis - sentAt < MIN_INTERVAL_MILLIS) return null;

        sent = wanted;
        sentAt = nowMillis;
        return wanted;
    }
}
