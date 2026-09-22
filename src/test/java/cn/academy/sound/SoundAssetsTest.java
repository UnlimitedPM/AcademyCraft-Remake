package cn.academy.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le fichier des sons, et les fichiers qu'il annonce.
 *
 * Un son se declare deux fois : une fois dans {@code sounds.json}, qui dit quels
 * fichiers jouer, et une fois dans le registre, qui lui donne un nom. Les deux doivent
 * s'accorder, et un desaccord ne fait rien tomber en jeu — l'evenement se joue dans un
 * silence total. D'ou ces tests, qui tiennent la moitie « fichiers » de la question :
 * l'autre moitie, celle du registre, demande un jeu demarre et vit dans un GameTest.
 */
class SoundAssetsTest {

    /** Les evenements du {@code sounds.json} de l'original. */
    private static final int EVENTS = 44;

    private static final String ROOT = "/assets/academy/";
    private static final String FILE = ROOT + "sounds.json";

    private static JsonObject read() {
        try (InputStream in = SoundAssetsTest.class.getResourceAsStream(FILE)) {
            assertNotNull(in, "le fichier des sons doit etre livre");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                assertTrue(root.isJsonObject(), "le fichier des sons doit etre un objet");
                return root.getAsJsonObject();
            }
        } catch (Exception e) {
            throw new AssertionError("le fichier des sons doit se lire", e);
        }
    }

    @Test
    void lesEvenementsDeLOriginalSontTousLa() {
        JsonObject json = read();

        assertEquals(EVENTS, json.entrySet().size(), "les evenements de l'original");
        for (String name : new String[] { "ability.deny", "em.minedetect", "md.meltdowner",
                "terminal.select", "tp.tp_flashing", "vecmanip.groundshock",
                "machine.machine_work" }) {
            assertTrue(json.has(name), "evenement manquant : " + name);
        }
    }

    @Test
    void chaqueEvenementTrouveSesFichiers() {
        JsonObject json = read();
        Set<String> missing = new HashSet<>();

        for (var entry : json.entrySet()) {
            JsonObject event = entry.getValue().getAsJsonObject();
            assertTrue(event.has("sounds"), "evenement sans son : " + entry.getKey());
            assertTrue(event.getAsJsonArray("sounds").size() > 0,
                    "evenement sans son : " + entry.getKey());

            for (JsonElement element : event.getAsJsonArray("sounds")) {
                String name = element.getAsJsonObject().get("name").getAsString();
                // Le nom est celui de l'original, prefixe du mod : `academy:em/arc_weak`.
                String path = name.startsWith("academy:") ? name.substring("academy:".length()) : name;
                if (SoundAssetsTest.class.getResourceAsStream(ROOT + "sounds/" + path + ".ogg") == null) {
                    missing.add(name);
                }
            }
        }

        assertTrue(missing.isEmpty(), "fichiers de son absents : " + missing);
    }

    @Test
    void lesDeuxBouclesDeMachineSontEnFlux() {
        // Les deux seuls `stream` de l'original : ses deux boucles de machine, qu'il
        // lisait en flux plutot que de les charger en memoire.
        JsonObject json = read();

        for (String name : new String[] { "machine.imag_fusor_work", "machine.machine_work" }) {
            assertTrue(json.getAsJsonObject(name).getAsJsonArray("sounds").get(0)
                            .getAsJsonObject().get("stream").getAsBoolean(),
                    name + " doit rester en flux, comme dans l'original");
        }
        assertTrue(!json.getAsJsonObject("em.arc_weak").getAsJsonArray("sounds").get(0)
                        .getAsJsonObject().get("stream").getAsBoolean(),
                "un son court n'est pas en flux");
    }

    @Test
    void lesSonsDesPresetsSontLivresMaisPasAnnonces() {
        // Deux fichiers de plus que le fichier des sons ne cite : ceux du systeme de
        // prereglages de touches, qui n'est pas porte. Les livrer ne coute rien et evite
        // d'y revenir ; ce test dit surtout qu'on n'en a pas oublie en chemin.
        for (String path : new String[] { "ability/preset_confirm", "ability/preset_switch" }) {
            assertNotNull(SoundAssetsTest.class.getResourceAsStream(ROOT + "sounds/" + path + ".ogg"),
                    path + " doit etre livre");
        }
    }
}
