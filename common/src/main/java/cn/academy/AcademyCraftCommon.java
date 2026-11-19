package cn.academy;

/**
 * Point d'entree neutre du mod, appele par chaque module de chargeur.
 *
 * <p>Ce module ne connait ni Forge, ni NeoForge, ni Fabric. C'est ici que viendront les
 * initialisations partagees (registres abstraits, config, reseau) au fur et a mesure du portage.
 * Pour l'instant il ne fait rien : l'echafaudage doit d'abord prouver que les trois jars se
 * construisent et se chargent.
 */
public final class AcademyCraftCommon {
    public static final String MOD_ID = "academy";

    private AcademyCraftCommon() {
    }

    /**
     * Initialisation commune, appelee une fois par le chargeur au demarrage.
     */
    public static void init() {
        // Vide volontairement : voir la feuille de route dans MULTILOADER.md.
    }
}
