package cn.academy.terminal.tutorial;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Les tutoriels livres avec le mod, et la lecture de leur contenu.
 *
 * <p>Portage de {@code TutorialInit} et de {@code ACTutorial.getContent()}. L'original
 * declarait ses tutoriels en code, chacun avec la liste des objets qui l'ouvrent
 * ({@code itemObtained} : fabrique, ramasse ou cuit) et un apercu de recettes ; le port
 * reprend la liste et les conditions, sans les apercus — le rendu des recettes est un
 * affichage a lui seul.
 *
 * <h2>Ce que l'ecran a, et ce qu'il n'a pas</h2>
 *
 * L'ecran recoit ici des textes deja separes en titre, resume et contenu, et la liste
 * des objets qui ouvrent chaque tutoriel. Il ne connait ni la langue de repli ni les
 * chemins de ressources : tout cela vit ici, et se relit en JUnit — le contenu livre
 * est donc verifie par le build, comme le fichier des credits.
 *
 * <p>{@code energy_bridge} n'est pas livre : l'original ne le declarait que si un mod
 * d'energie etait present, et le port n'a pas de pont d'energie.
 */
public final class TutorialLibrary {

    /** La langue de repli, celle de l'original. */
    public static final String DEFAULT_LANGUAGE = "en_us";

    private static final String FOLDER = "/assets/academy/tutorials/";

    /**
     * Un tutoriel : son identifiant — c'est aussi le nom de son fichier — et les objets
     * qui l'ouvrent.
     *
     * <p>Une liste vide signifie « toujours ouvert » : c'est le cas des tutoriels de
     * fond, que l'original n'associait a aucun objet.
     */
    public record Entry(String id, List<String> requiredItems) {

        public boolean alwaysOpen() {
            return requiredItems.isEmpty();
        }
    }

    /**
     * Les tutoriels, dans l'ordre ou l'original les declarait.
     *
     * <p>Les objets sont nommes comme les enregistre le port, qui a garde les noms de la
     * 1.12.2 ({@code dev_normal}, {@code dev_advanced}, {@code phase_gen}).
     * Le tutoriel du terminal demande aussi l'objet de la seule application non
     * preinstallee que le port livre : l'original y ajoutait, en boucle, l'objet de
     * chaque application, et les autres n'existent pas encore.
     */
    private static final List<Entry> ENTRIES = List.of(
            new Entry("welcome", List.of()),
            new Entry("ores", List.of("academy:constraint_metal", "academy:imagsil_ore",
                    "academy:crystal_ore", "academy:reso_ore")),
            new Entry("phase_generator", List.of("academy:phase_gen")),
            new Entry("solar_generator", List.of("academy:solar_gen")),
            new Entry("wind_generator", List.of("academy:windgen_base", "academy:windgen_fan",
                    "academy:windgen_main", "academy:windgen_pillar")),
            new Entry("metal_former", List.of("academy:metal_former")),
            new Entry("imag_fusor", List.of("academy:imag_fusor")),
            new Entry("terminal", List.of("academy:terminal_installer", "academy:app_skill_tree")),
            new Entry("ability_developer", List.of("academy:developer_portable",
                    "academy:dev_normal", "academy:dev_advanced")),
            new Entry("ability_basis", List.of()),
            new Entry("misc", List.of()),
            new Entry("develop_ability", List.of()),
            new Entry("wireless_network", List.of()));

    private TutorialLibrary() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** Le tutoriel de cet identifiant, ou {@code null}. */
    public static Entry byId(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id().equals(id)) return entry;
        }
        return null;
    }

    /**
     * Le texte d'un tutoriel, dans la langue demandee.
     *
     * <p>Comme l'original, une langue sans fichier retombe sur l'anglais : un tutoriel
     * traduit a moitie ne doit pas disparaitre. Et un fichier absent rend un texte vide
     * plutot qu'une exception, pour que l'ecran le dise au lieu de rester muet.
     */
    public static TutorialText load(String id, String language) {
        if (byId(id) == null) return TutorialText.empty();

        String raw = read(id, language);
        if (raw == null) raw = read(id, DEFAULT_LANGUAGE);
        return TutorialText.parse(raw);
    }

    /**
     * Lit un fichier de tutoriel depuis les ressources, ou rend {@code null} s'il manque.
     *
     * <p>Ne leve jamais : une ressource absente ou illisible rend {@code null}, comme le
     * {@code unknown} de l'original. Un fichier de contenu casse ne doit pas empecher
     * d'ouvrir le terminal.
     */
    private static String read(String id, String language) {
        if (language == null) return null;
        String path = FOLDER + language + "/" + id + ".md";
        try (InputStream in = TutorialLibrary.class.getResourceAsStream(path)) {
            if (in == null) return null;
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                StringBuilder out = new StringBuilder();
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) >= 0) {
                    out.append(buffer, 0, count);
                }
                return out.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
