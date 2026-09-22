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

    private AboutDocument(List<String> header, List<Staff> staff, List<String> donators) {
        this.header = List.copyOf(header);
        this.staff = List.copyOf(staff);
        this.donators = List.copyOf(donators);
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
        return new AboutDocument(List.of(), List.of(), List.of());
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
        return new AboutDocument(header, staff, donators);
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
