package cn.academy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les langues du mod, relues sans Minecraft.
 *
 * <p>Une traduction se casse en silence : une cle oubliee affiche son identifiant brut en jeu, un
 * {@code %s} perdu fait disparaitre un nom de la phrase, un saut de ligne oublie colle la banniere
 * du terminal au copyright. Aucune compilation ne s'en plaint, et le joueur le voit avant nous —
 * c'est arrive avec quatre noms d'aptitudes, et c'est pour cela que ce fichier existe.
 */
class LangFilesTest {

    /** La langue de reference : c'est elle qui definit les cles. */
    private static final String REFERENCE = "en_us";

    /** Les langues livrees avec le mod, dans l'ordre de la liste. */
    private static final List<String> LANGUAGES = List.of("en_us", "fr_fr", "de_de", "es_es", "zh_cn");

    /** Tout ce qui doit survivre a une traduction : les formats et les codes de couleur. */
    private static final Pattern TOKEN = Pattern.compile("%(?:s|d|%)|§.");

    @Test
    @DisplayName("chaque langue a exactement les cles de l'anglais")
    void chaqueLangueALesClesDeLAnglais() {
        JsonObject reference = langue(REFERENCE);

        for (String language : LANGUAGES) {
            if (language.equals(REFERENCE)) continue;
            JsonObject translated = langue(language);

            List<String> missing = new ArrayList<>();
            for (String key : reference.keySet()) {
                if (!translated.has(key)) missing.add(key);
            }
            assertTrue(missing.isEmpty(), language + " n'a pas : " + missing);

            List<String> extra = new ArrayList<>();
            for (String key : translated.keySet()) {
                if (!reference.has(key)) extra.add(key);
            }
            assertTrue(extra.isEmpty(), language + " a des cles inconnues : " + extra);
        }
    }

    @Test
    @DisplayName("aucun texte traduit n'est vide")
    void aucunTexteNEstVide() {
        for (String language : LANGUAGES) {
            for (var entry : langue(language).entrySet()) {
                assertFalse(entry.getValue().getAsString().isBlank(),
                        language + " : " + entry.getKey() + " est vide");
            }
        }
    }

    @Test
    @DisplayName("les %s, les %% et les codes de couleur sont ceux de l'anglais")
    void lesJetonsDeFormatageSontCeuxDeLAnglais() {
        JsonObject reference = langue(REFERENCE);

        for (String language : LANGUAGES) {
            if (language.equals(REFERENCE)) continue;
            JsonObject translated = langue(language);

            for (String key : reference.keySet()) {
                assertTrue(translated.has(key), language + " n'a pas " + key);
                assertEquals(tokens(reference.get(key).getAsString()),
                        tokens(translated.get(key).getAsString()),
                        language + " : les jetons de " + key + " ne sont plus les memes");
                assertEquals(newlines(reference.get(key).getAsString()),
                        newlines(translated.get(key).getAsString()),
                        language + " : " + key + " n'a plus le meme nombre de lignes");
            }
        }
    }

    @Test
    @DisplayName("une traduction traduit vraiment")
    void uneTraductionTraduitVraiment() {
        JsonObject reference = langue(REFERENCE);

        for (String language : LANGUAGES) {
            if (language.equals(REFERENCE)) continue;
            JsonObject translated = langue(language);

            // Les noms propres survivent, et c'est normal : Academy Craft, Railgun, Meltdowner, les
            // titres des musiques. Le reste doit avoir bouge.
            long changed = translated.keySet().stream()
                    .filter(key -> !translated.get(key).getAsString()
                            .equals(reference.get(key).getAsString()))
                    .count();
            assertTrue(changed >= reference.size() * 9 / 10,
                    language + " ne traduit que " + changed + " textes sur " + reference.size());
        }
    }

    /** Les jetons d'un texte, dans l'ordre : {@code %s,%s} n'est pas {@code %s}. */
    private static List<String> tokens(String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) found.add(matcher.group());
        return found;
    }

    private static int newlines(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }
        return count;
    }

    private static JsonObject langue(String language) {
        String path = "assets/academy/lang/" + language + ".json";
        try (InputStream in = LangFilesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path + " est introuvable");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError(path + " n'a pas pu etre lu", failure);
        }
    }
}
