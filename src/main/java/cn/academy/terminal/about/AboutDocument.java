package cn.academy.terminal.about;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le contenu de l'application « A propos » : l'en-tete, l'equipe par role, et les
 * donateurs.
 *
 * Portage de {@code AppAbout.initTexts()}, qui lisait un fichier HOCON de
 * l'original. Le port n'a pas de bibliotheque HOCON ; le meme contenu est donc
 * range en JSON, lu avec Gson — qui est deja la, Minecraft l'embarque.
 *
 * <h2>Ce qui est calculable ici, et ce qui ne l'est pas</h2>
 *
 * La classe ne connait ni police, ni ecran, ni Minecraft : elle rend une liste de
 * lignes decrivant ce qu'il faut ecrire, et la mise en page appartient a l'ecran.
 * C'est ce qui permet de verifier la structure du document en JUnit plutot qu'a
 * l'oeil dans un jeu.
 *
 * <h2>Deux ecarts assumes</h2>
 *
 * - L'original melangeait la liste des donateurs a chaque ouverture
 *   ({@code Collections.shuffle}). Ici l'ordre du fichier est conserve : melanger
 *   ferait sauter les noms d'un affichage a l'autre sans rien apporter, la liste
 *   etant de toute facon figee depuis que le service de donateurs n'existe plus.
 * - L'onglet des dons de l'original n'est pas porte : il ne contenait qu'un texte
 *   de campagne.
 */
public final class AboutDocument {

    /** Ou l'on aligne une ligne. Le centrage sert aux titres de section. */
    public enum Align { LEFT, CENTER, RIGHT }

    /**
     * Une ligne a ecrire.
     *
     * @param scale facteur de taille : 1 = taille normale, 0.7 = les petits textes
     *              d'explication sous les titres.
     */
    public record Line(String text, Align align, boolean bold, float scale) {

        public static Line of(String text, Align align) {
            return new Line(text, align, false, 1.0f);
        }

        public static Line title(String text) {
            return new Line(text, Align.CENTER, true, 1.0f);
        }
    }

    /** Un role de l'equipe et les personnes qui l'occupent. */
    public record Staff(String role, List<String> names) {}

    /** Nombre de noms de donateurs par ligne a l'ecran. */
    public static final int DONATORS_PER_ROW = 3;

    private static final String RESOURCE = "/assets/academy/config/about.json";

    private static AboutDocument cached;

    private final List<String> header;
    private final List<Staff> staff;
    private final List<String> donators;

    /**
     * La ligne du haut : qui a fait ce portage.
     *
     * <p>Elle n'existe pas dans le {@code about.conf} de l'original — c'est le seul ajout de
     * contenu du port, demande par le joueur : « tout en haut, au dessus du reste, ecris
     * quelque chose comme Remake made by UnlimitedPM ». Le document la porte donc, plutot que
     * l'ecran, pour qu'elle se change sans toucher au code.
     */
    private final String remake;

    /**
     * Le texte de l'onglet des dons, par langue.
     *
     * <p>Repris de la section {@code donation} de {@code about.conf} : une liste de lignes, ou
     * {@code "]"} en tete aligne la ligne a droite et {@code "!!<texte>|<url>"} fait un lien.
     * L'original retombait sur {@code en_us} quand sa langue n'y etait pas ; le port garde les
     * deux langues qu'il livrait.
     */
    private final java.util.Map<String, List<String>> donation;

    /**
     * Les noms de role, par langue : {@code roles[fr_fr]["Programming"] = "Programmation"}.
     *
     * <p>Le document de l'original nommait ses roles en anglais et rien ne les traduisait —
     * l'onglet des credits restait donc anglais dans les quatre langues du port. La table est
     * indexee par le role ANGLAIS du document, ce qui laisse le document lisible et permet a une
     * langue d'en oublier un : il s'affiche alors en anglais, comme le reste du document.
     */
    private final java.util.Map<String, java.util.Map<String, String>> roles;

    private AboutDocument(List<String> header, List<Staff> staff, List<String> donators,
                          java.util.Map<String, List<String>> donation, String remake,
                          java.util.Map<String, java.util.Map<String, String>> roles) {
        this.header = List.copyOf(header);
        this.staff = List.copyOf(staff);
        this.donators = List.copyOf(donators);
        this.donation = java.util.Map.copyOf(donation);
        this.remake = remake == null ? "" : remake;
        this.roles = java.util.Map.copyOf(roles);
    }

    /**
     * Le document livre avec le mod, lu une seule fois.
     *
     * Ne leve jamais : si la ressource manque ou est illisible, le document est
     * vide et l'ecran le dira. Un fichier de credits absent ne doit pas empecher
     * d'ouvrir le terminal.
     */
    public static AboutDocument load() {
        if (cached == null) {
            cached = readResource();
        }
        return cached;
    }

    /** Oublie le document en cache. Sert aux tests. */
    static void invalidateCache() {
        cached = null;
    }

    private static AboutDocument readResource() {
        try (InputStream in = AboutDocument.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return empty();
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(reader);
            }
        } catch (Exception e) {
            return empty();
        }
    }

    public static AboutDocument empty() {
        return new AboutDocument(List.of(), List.of(), List.of(), java.util.Map.of(), "",
                java.util.Map.of());
    }

    /** Lit un document depuis un lecteur JSON. Les champs absents restent vides. */
    public static AboutDocument parse(Reader json) {
        JsonElement root = JsonParser.parseReader(json);
        if (root == null || !root.isJsonObject()) return empty();
        return fromJson(root.getAsJsonObject());
    }

    /** Lit un document depuis une chaine JSON. */
    public static AboutDocument parse(String json) {
        return parse(new StringReader(json));
    }

    private static AboutDocument fromJson(JsonObject root) {
        List<String> header = strings(root.getAsJsonArray("header"));
        List<String> donators = strings(root.getAsJsonArray("donators"));

        List<Staff> staff = new ArrayList<>();
        JsonArray roles = root.getAsJsonArray("staff");
        if (roles != null) {
            for (JsonElement element : roles) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("role")) continue;
                staff.add(new Staff(entry.get("role").getAsString(), strings(entry.getAsJsonArray("names"))));
            }
        }
        return new AboutDocument(header, staff, donators, donation(root), string(root, "remake"),
                roles(root));
    }

    /**
     * Les noms de role par langue : {@code { "fr_fr": { "Programming": "Programmation", ... } }}.
     *
     * <p>Absent du document de l'original : c'est un ajout du port, dans la meme forme que
     * {@code donation} — un objet par langue.
     */
    private static java.util.Map<String, java.util.Map<String, String>> roles(JsonObject root) {
        JsonElement element = root.get("roles");
        if (element == null || !element.isJsonObject()) return java.util.Map.of();
        java.util.Map<String, java.util.Map<String, String>> out = new java.util.LinkedHashMap<>();
        for (String lang : element.getAsJsonObject().keySet()) {
            JsonElement table = element.getAsJsonObject().get(lang);
            if (!table.isJsonObject()) continue;
            java.util.Map<String, String> translated = new java.util.LinkedHashMap<>();
            for (String role : table.getAsJsonObject().keySet()) {
                JsonElement text = table.getAsJsonObject().get(role);
                if (text.isJsonPrimitive()) translated.put(role, text.getAsString());
            }
            out.put(lang, java.util.Map.copyOf(translated));
        }
        return java.util.Map.copyOf(out);
    }

    /** Un champ texte du document, ou une chaine vide. */
    private static String string(JsonObject root, String key) {
        JsonElement element = root.get(key);
        return element == null || !element.isJsonPrimitive() ? "" : element.getAsString();
    }

    /** Les listes de lignes de l'onglet des dons, une par langue. */
    private static java.util.Map<String, List<String>> donation(JsonObject root) {
        JsonElement element = root.get("donation");
        if (element == null || !element.isJsonObject()) return java.util.Map.of();
        java.util.Map<String, List<String>> out = new java.util.LinkedHashMap<>();
        for (String lang : element.getAsJsonObject().keySet()) {
            out.put(lang, strings(element.getAsJsonObject().getAsJsonArray(lang)));
        }
        return out;
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) out.add(element.getAsString());
        }
        return out;
    }

    public List<String> getHeader() {
        return header;
    }

    public List<Staff> getStaff() {
        return staff;
    }

    public List<String> getDonators() {
        return donators;
    }

    /** La ligne du haut, celle du portage. Vide si le document ne la donne pas. */
    public String getRemake() {
        return remake;
    }

    /**
     * Le texte de l'onglet des dons dans une langue, ou celui d'{@code en_us} a defaut.
     *
     * <p>C'est la regle de l'original, qui ne connaissait que {@code zh_cn} et {@code en_us} :
     * tout autre langue lit l'anglais.
     */
    public List<String> donation(String lang) {
        List<String> lines = donation.get(lang);
        if (lines != null) return lines;
        List<String> fallback = donation.get("en_us");
        return fallback == null ? List.of() : fallback;
    }

    /**
     * Un role d'equipe, dans la langue demandee, ou en anglais s'il n'y est pas.
     *
     * <p>Indexe par le nom anglais du document : c'est ce que le fichier ecrit, et une langue
     * qui oublie un role affiche celui-la plutot qu'une case vide.
     */
    public String role(String english, String lang) {
        java.util.Map<String, String> table = roles.get(lang);
        if (table != null) {
            String translated = table.get(english);
            if (translated != null && !translated.isBlank()) return translated;
        }
        return english;
    }

    public boolean isEmpty() {
        return header.isEmpty() && staff.isEmpty() && donators.isEmpty();
    }

    /**
     * Les lignes du document, dans l'ordre d'affichage.
     *
     * Reprend la disposition de {@code initTexts()} : l'en-tete, puis chaque role
     * aligne a droite avec ses noms a gauche, puis le titre des donateurs et son
     * explication. Les noms eux-memes sont rendus a part, par
     * {@link #donatorRows()}, parce qu'ils s'affichent en colonnes — chose qu'une
     * ligne unique ne sait pas faire.
     *
     * @param donatorsHint l'explication a mettre sous le titre, deja traduite ;
     *                     ses retours a la ligne sont respectes, comme dans
     *                     l'original qui decoupait la cle de langue sur {@code \n}.
     */
    public List<Line> toLines(String donatorsHint) {
        List<Line> lines = new ArrayList<>();

        for (String text : header) {
            lines.add(Line.title(text));
        }

        if (!staff.isEmpty()) {
            lines.add(Line.of("", Align.LEFT));
            for (Staff entry : staff) {
                lines.add(new Line(entry.role(), Align.RIGHT, true, 1.0f));
                for (String name : entry.names()) {
                    lines.add(Line.of(name, Align.LEFT));
                }
                lines.add(Line.of("", Align.LEFT));
            }
        }

        if (!donators.isEmpty()) {
            lines.add(Line.title("Donators"));
            // La cle de langue de l'original contient des « \n » litteraux, pas de
            // vrais retours a la ligne. On accepte les deux : la premiere forme
            // reste celle du fichier livre, la seconde evite un piege si quelqu'un
            // ecrit un jour la vraie.
            for (String hint : donatorsHint.split("\\\\n|\\n")) {
                lines.add(new Line(hint, Align.CENTER, false, 0.7f));
            }
        }
        return lines;
    }

    /** Les noms des donateurs groupes par lignes, {@link #DONATORS_PER_ROW} par ligne. */
    public List<List<String>> donatorRows() {
        return pack(donators, DONATORS_PER_ROW);
    }

    /**
     * Repartit une liste en lignes de {@code perRow} elements. La derniere ligne
     * peut etre plus courte.
     */
    public static List<List<String>> pack(List<String> values, int perRow) {
        if (values.isEmpty() || perRow <= 0) return List.of();
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < values.size(); i += perRow) {
            rows.add(Collections.unmodifiableList(new ArrayList<>(
                    values.subList(i, Math.min(values.size(), i + perRow)))));
        }
        return List.copyOf(rows);
    }
}
