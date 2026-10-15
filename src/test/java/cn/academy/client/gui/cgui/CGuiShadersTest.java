package cn.academy.client.gui.cgui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires des deux programmes de {@link CGuiShaders}.
 *
 * <h2>Pourquoi ces tests existent</h2>
 *
 * <p>Un programme de rendu se declare deux fois : une fois dans le code GLSL, une fois dans le fichier
 * JSON qui dit au jeu quels attributs il lit, quels samplers il prend et quels uniformes il porte. Rien
 * ne relie les deux : un nom qui ne correspond pas — un {@code SkillProgress} ecrit autrement, un
 * sampler oublie — ne se voit <b>qu'au lancement du client</b>, et sous la forme la plus desagreable,
 * une exception au chargement des ressources.
 *
 * <p>Le port ne peut pas lancer de client dans un test. Il peut, en revanche, relire les deux fichiers
 * et les confronter, et c'est ce que fait ce test : chaque attribut, chaque uniforme et chaque sampler
 * declares d'un cote doivent se retrouver de l'autre. C'est peu, et c'est exactement la faute qu'on
 * risque.
 */
class CGuiShadersTest {

    private static final String PATH = "assets/academy/shaders/core/";

    /** Le nom des deux uniformes que le code Java pose lui-meme, et du remplissage qu'il lit. */
    private static final String PROGRESS = CGuiShaders.RING_PROGRESS_UNIFORM;

    private static String read(String file) {
        try (InputStream in = CGuiShadersTest.class.getClassLoader().getResourceAsStream(PATH + file)) {
            assertNotNull(in, "le fichier " + PATH + file + " doit exister");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Le fichier " + PATH + file + " est illisible", e);
        }
    }

    private static JsonObject manifest(String program) {
        return JsonParser.parseString(read(program + ".json")).getAsJsonObject();
    }

    /** Les noms declares, dans un tableau d'objets du JSON — c'est la forme des samplers. */
    private static Set<String> names(JsonArray array, String key) {
        Set<String> out = new LinkedHashSet<>();
        for (var element : array) {
            out.add(element.getAsJsonObject().get(key).getAsString());
        }
        return out;
    }

    /** Et celle des attributs, qui sont des noms tout court — comme dans le manifeste du jeu. */
    private static Set<String> strings(JsonArray array) {
        Set<String> out = new LinkedHashSet<>();
        for (var element : array) {
            out.add(element.getAsString());
        }
        return out;
    }

    /** Ce qu'un programme lit : ses entrees de vertex. */
    private static Set<String> inputs(String source) {
        return matches(source, "\\bin\\s+\\w+\\s+(\\w+)\\s*;");
    }

    /** Et ses uniformes. */
    private static Set<String> uniforms(String source) {
        return matches(source, "\\buniform\\s+\\w+\\s+(\\w+)\\s*;");
    }

    /** Et les samplers qu'il utilise vraiment, dans le corps du fragment. */
    private static Set<String> samplers(String source) {
        return matches(source, "\\btexture\\s*\\(\\s*(Sampler\\d+)");
    }

    private static Set<String> matches(String source, String regex) {
        Set<String> out = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile(regex).matcher(source);
        while (matcher.find()) {
            out.add(matcher.group(1));
        }
        return out;
    }

    /**
     * Ce que les deux programmes doivent avoir en commun.
     *
     * <p>Tout dessin passe par la matrice du projet et celle de la vue : sans elles un sommet n'a pas de
     * place a l'ecran. Et la couleur modulee est ce qui permet de teindre et de faire un fondu.
     */
    private static void commonGround(JsonObject json, String vertex, String fragment) {
        Set<String> declared = names(json.getAsJsonArray("uniforms"), "name");
        for (String uniform : List.of("ModelViewMat", "ProjMat", "ColorModulator")) {
            assertTrue(declared.contains(uniform),
                    "le manifeste doit declarer l'uniforme " + uniform);
            assertTrue(uniforms(vertex).contains(uniform) || uniforms(fragment).contains(uniform),
                    "le programme doit lire l'uniforme " + uniform);
        }
    }

    @Test
    @DisplayName("le programme de l'anneau : ce qu'il lit est ce qui est declare")
    void ringProgramAgreesWithItsManifest() {
        JsonObject json = manifest(CGuiShaders.RING_PROGRAM);
        String vertex = read(CGuiShaders.RING_PROGRAM + ".vsh");
        String fragment = read(CGuiShaders.RING_PROGRAM + ".fsh");

        assertEquals("academy:" + CGuiShaders.RING_PROGRAM, json.get("vertex").getAsString());
        assertEquals("academy:" + CGuiShaders.RING_PROGRAM, json.get("fragment").getAsString());

        commonGround(json, vertex, fragment);

        // Les entrees du sommet, dans les deux sens : un attribut declare et jamais lu serait une
        // donnee par sommet que le format fournit pour rien, et un attribut lu et jamais declare
        // donnerait un sommet a zero.
        Set<String> attributes = strings(json.getAsJsonArray("attributes"));
        assertEquals(Set.of("Position", "Color", "UV0"), attributes);
        assertEquals(attributes, inputs(vertex), "le sommet doit lire les attributs declares");

        // Les samplers : le fragment en utilise deux, le manifeste doit en declarer deux.
        Set<String> used = samplers(fragment);
        assertEquals(Set.of("Sampler0", "Sampler1"), used,
                "l'anneau se lit dans un sampler et son masque dans l'autre");
        Set<String> declared = names(json.getAsJsonArray("samplers"), "name");
        assertEquals(used, declared, "tout sampler lu doit etre declare");

        // Et le remplissage, qui est ce que le code Java vient poser.
        assertTrue(names(json.getAsJsonArray("uniforms"), "name").contains(PROGRESS),
                "le manifeste doit declarer " + PROGRESS + ", que le code pose");
        assertTrue(uniforms(fragment).contains(PROGRESS),
                "le fragment doit lire " + PROGRESS);
    }

    @Test
    @DisplayName("le programme du gris : ce qu'il lit est ce qui est declare")
    void monoProgramAgreesWithItsManifest() {
        JsonObject json = manifest(CGuiShaders.MONO_PROGRAM);
        String vertex = read(CGuiShaders.MONO_PROGRAM + ".vsh");
        String fragment = read(CGuiShaders.MONO_PROGRAM + ".fsh");

        assertEquals("academy:" + CGuiShaders.MONO_PROGRAM, json.get("vertex").getAsString());
        assertEquals("academy:" + CGuiShaders.MONO_PROGRAM, json.get("fragment").getAsString());

        commonGround(json, vertex, fragment);

        Set<String> attributes = strings(json.getAsJsonArray("attributes"));
        assertEquals(attributes, inputs(vertex), "le sommet doit lire les attributs declares");

        Set<String> used = samplers(fragment);
        assertEquals(Set.of("Sampler0"), used, "le gris ne lit que l'icone");
        assertEquals(used, names(json.getAsJsonArray("samplers"), "name"));
    }

    @Test
    @DisplayName("les uniformes d'un manifeste ont la bonne forme")
    void uniformsHaveTheRightShape() {
        for (String program : List.of(CGuiShaders.RING_PROGRAM, CGuiShaders.MONO_PROGRAM)) {
            for (var element : manifest(program).getAsJsonArray("uniforms")) {
                JsonObject uniform = element.getAsJsonObject();
                int count = uniform.get("count").getAsInt();
                int values = uniform.getAsJsonArray("values").size();

                // Une matrice se declare en seize nombres, un vecteur en quatre : c'est la forme que le
                // chargeur attend, et une matrice a quinze cases ne se lit pas.
                List<Integer> allowed = uniform.get("type").getAsString().equals("matrix4x4")
                        ? List.of(16) : List.of(1, 4);
                assertTrue(allowed.contains(count),
                        program + " : " + uniform.get("name").getAsString() + " annonce " + count
                                + " valeurs, ce qui ne se lit pas");
                assertEquals(count, values,
                        program + " : " + uniform.get("name").getAsString()
                                + " annonce " + count + " valeurs et en porte " + values);
            }
        }
    }

    @Test
    @DisplayName("les deux programmes sont declares avec le degre de melange du jeu")
    void programsBlendLikeTheGame() {
        // Un programme qui ne declare pas son melange est dessine sans transparence : l'anneau
        // apparaitrait sur un carre noir, et l'icone grisee cacherait son disque.
        for (String program : List.of(CGuiShaders.RING_PROGRAM, CGuiShaders.MONO_PROGRAM)) {
            JsonObject blend = manifest(program).getAsJsonObject("blend");
            assertNotNull(blend, program + " doit declarer son melange");
            assertEquals("srcalpha", blend.get("srcrgb").getAsString());
            assertEquals("1-srcalpha", blend.get("dstrgb").getAsString());
        }
    }

    @Test
    @DisplayName("les deux programmes se chargent depuis des fichiers nommes comme eux")
    void filesAreNamedAfterThePrograms() {
        List<String> programs = List.of(CGuiShaders.RING_PROGRAM, CGuiShaders.MONO_PROGRAM);
        for (String program : programs) {
            // read() echoue si le fichier n'est pas la : c'est le seul moyen de verifier qu'un renommage
            // du programme dans le code ne laisse pas ses fichiers derriere lui.
            assertTrue(read(program + ".vsh").contains("#version 150"),
                    program + " doit etre ecrit pour cette version du jeu");
            assertTrue(read(program + ".fsh").contains("fragColor"),
                    program + " doit ecrire sa couleur de fragment");
        }
    }
}
