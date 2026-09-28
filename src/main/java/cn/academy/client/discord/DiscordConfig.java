package cn.academy.client.discord;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Les reglages de la presence Discord, dans la config du client.
 *
 * <p>Ils appartiennent au client qui joue : c'est lui qui a un compte Discord, et lui seul
 * qui peut montrer quoi que ce soit. La config est donc de type {@code CLIENT} — sur un
 * serveur dedie, Forge ignore simplement ce fichier.
 *
 * <p>Son fichier est {@code config/academy-discord.toml}, et non {@code academy-client.toml} :
 * Forge range toutes les configs CLIENT d'un mod dans le meme fichier et refuse la deuxieme,
 * ce qui ferait echouer le chargement du mod. Le nom est donc donne a l'enregistrement.
 *
 * <p>Aucun type de client n'est nomme ici : cette classe se charge des deux cotes, puisque
 * la config s'enregistre a la construction du mod. Seules ses valeurs sont lues cote client.
 */
public final class DiscordConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.ConfigValue<String> APPLICATION_ID;
    private static final ForgeConfigSpec.BooleanValue SHOW_SERVER_ADDRESS;
    private static final ForgeConfigSpec.BooleanValue SHOW_DIMENSION;
    private static final ForgeConfigSpec.ConfigValue<String> BUTTON_LABEL;
    private static final ForgeConfigSpec.ConfigValue<String> BUTTON_URL;

    /**
     * Ce que les reglages disent, en clair.
     *
     * <p>Un instantane, et non la config elle-meme : le reste du mod lit ces valeurs depuis
     * son propre fil, sans se demander si un rechargement est en cours.
     */
    public record Settings(boolean enabled, String applicationId, boolean showServerAddress,
                           boolean showDimension, String buttonLabel, String buttonUrl) {}

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                        "Ce que vos amis voient sur votre profil Discord pendant que vous jouez.",
                        "",
                        "Rien n'est envoye tant qu'aucun identifiant d'application n'est colle ci-dessous.",
                        "Pour en obtenir un : creez une application sur",
                        "https://discord.com/developers/applications, puis copiez son « Application ID »",
                        "(dix-sept a vingt chiffres).",
                        "",
                        "Le mod fonctionne sans images ; pour en avoir, televersez sur cette meme page,",
                        "dans « Rich Presence > Art Assets », des images nommees academy, menu, solo",
                        "et multi. Sans elles, la fiche s'affiche quand meme, avec ses deux lignes.",
                        "",
                        "Aucune partie privee n'est envoyee : le mod ne dit que ce qui est ecrit sur la",
                        "fiche, et l'adresse de votre serveur reste cachee tant que vous ne l'autorisez",
                        "pas ci-dessous.")
                .push("discord");

        ENABLED = builder
                .comment("Montrer ce que vous faites sur Discord.",
                        "Eteint : rien n'est envoye, et la fiche deja posee est retiree.")
                .define("enabled", true);

        APPLICATION_ID = builder
                .comment("L'identifiant de l'application Discord.",
                        "Vide par defaut : la presence reste eteinte, et le journal le dit.",
                        "Le corriger en jeu prend effet dans la seconde, sans redemarrer.")
                .define("applicationId", "");

        SHOW_SERVER_ADDRESS = builder
                .comment("Montrer l'adresse du serveur sur lequel vous jouez.",
                        "Eteint par defaut : un profil Discord se lit par vos amis, et l'adresse",
                        "d'un serveur prive n'a rien a y faire.")
                .define("showServerAddress", false);

        SHOW_DIMENSION = builder
                .comment("Montrer dans quelle dimension vous etes.")
                .define("showDimension", true);

        BUTTON_LABEL = builder
                .comment("L'etiquette du lien affiche sous la fiche.",
                        "Vide par defaut : aucun lien.")
                .define("buttonLabel", "");

        BUTTON_URL = builder
                .comment("L'adresse de ce lien. Discord n'accepte que les adresses en https ;",
                        "une autre adresse est ignoree plutot que de faire refuser la fiche entiere.")
                .define("buttonUrl", "");

        builder.pop();
        SPEC = builder.build();
    }

    private DiscordConfig() {}

    /** Les reglages lus, ou les defauts si la config n'est pas encore lisible. */
    public static Settings read() {
        try {
            return new Settings(ENABLED.get(), APPLICATION_ID.get(), SHOW_SERVER_ADDRESS.get(),
                    SHOW_DIMENSION.get(), BUTTON_LABEL.get(), BUTTON_URL.get());
        } catch (Exception e) {
            // Une config pas encore chargee, ou un fichier en cours de reecriture : les
            // defauts suffisent, et la presence reste eteinte puisqu'aucun identifiant
            // n'est connu. Une presence ne vaut pas une exception chez le joueur.
            return new Settings(false, "", false, true, "", "");
        }
    }
}
