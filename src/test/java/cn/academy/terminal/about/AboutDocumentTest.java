package cn.academy.terminal.about;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le document « A propos » : sa lecture, et la mise en page qu'il propose.
 *
 * Le deuxieme test lit le fichier reellement livre avec le mod : c'est le controle
 * qui remplace le fait de lancer le jeu pour regarder l'ecran. Un fichier de
 * credits casse fait donc echouer le build, et non l'affichage chez le joueur.
 */
class AboutDocumentTest {

    private static final String SAMPLE = """
            {
              "header": ["Presented by Lambda Innovation"],
              "staff": [
                { "role": "Programming", "names": ["WeAthFolD", "acaly"] },
                { "role": "Art", "names": ["Nolife_M"] }
              ],
              "donators": ["a", "b", "c", "d"]
            }
            """;

    @Test
    void leFichierLivreSeLit() {
        AboutDocument document = AboutDocument.load();

        assertFalse(document.isEmpty(), "le fichier de credits doit se lire depuis les ressources du mod");
        assertEquals(2, document.getHeader().size(), "l'en-tete de l'original fait deux lignes");
        assertTrue(document.getStaff().size() >= 8, "l'equipe compte au moins huit roles");
        // 93 est le compte exact de `about.conf` dans l'original : une liste
        // tronquee a la recopie fait donc echouer ce test.
        assertTrue(document.getDonators().size() >= 93, "la liste des donateurs de l'original fait 93 noms");
    }

    @Test
    void unChampAbsentResteVide() {
        AboutDocument document = AboutDocument.parse("{}");

        assertTrue(document.isEmpty());
        assertTrue(document.getHeader().isEmpty());
        assertTrue(document.getStaff().isEmpty());
        assertTrue(document.getDonators().isEmpty());
        assertTrue(document.toLines("hint").isEmpty(), "un document vide ne propose aucune ligne");
    }

    @Test
    void unRoleSansNomEstIgnore() {
        String json = "{ \"staff\": [ { \"role\": \"Sans nom\" }, { \"names\": [\"orphelin\"] } ] }";

        AboutDocument document = AboutDocument.parse(json);

        assertEquals(1, document.getStaff().size());
        assertEquals("Sans nom", document.getStaff().get(0).role());
        assertTrue(document.getStaff().get(0).names().isEmpty());
    }

    @Test
    void unJsonCasseLeveUneErreur() {
        // Volontaire : load() est tolerance, mais lire un document qu'on vous tend
        // doit dire quand il est illisible plutot que de rendre du vide en silence.
        assertThrows(RuntimeException.class, () -> AboutDocument.parse("{ ceci n'est pas du json"));
    }

    @Test
    void laMiseEnPageSuitLOrdreDeLOriginal() {
        AboutDocument document = AboutDocument.parse(SAMPLE);

        List<AboutDocument.Line> lines = document.toLines("In no particular order");

        // L'en-tete, centre et en gras.
        assertEquals("Presented by Lambda Innovation", lines.get(0).text());
        assertEquals(AboutDocument.Align.CENTER, lines.get(0).align());
        assertTrue(lines.get(0).bold());

        // Chaque role est aligne a droite, ses noms a gauche, dans l'ordre du fichier.
        int role = indexOf(lines, "Programming");
        assertTrue(role > 0, "le role doit apparaitre");
        assertEquals(AboutDocument.Align.RIGHT, lines.get(role).align());
        assertTrue(lines.get(role).bold());
        assertEquals(AboutDocument.Align.LEFT, lines.get(role + 1).align());
        assertEquals("WeAthFolD", lines.get(role + 1).text());
        assertEquals("acaly", lines.get(role + 2).text());

        // Les deux roles sont la, et le titre des donateurs vient apres eux.
        int art = indexOf(lines, "Art");
        int donators = indexOf(lines, "Donators");
        assertTrue(art > role, "l'ordre du fichier est conserve");
        assertTrue(donators > art, "les donateurs viennent apres l'equipe");
        assertTrue(lines.get(donators).bold());
    }

    @Test
    void sansDonateurIlNyAPasDeSectionDonateurs() {
        AboutDocument document = AboutDocument.parse("{ \"header\": [\"titre\"] }");

        List<AboutDocument.Line> lines = document.toLines("In no particular order");

        assertTrue(indexOf(lines, "Donators") < 0, "pas de titre sans liste");
    }

    @Test
    void lExplicationSeDecoupeSurSesRetoursALigne() {
        AboutDocument document = AboutDocument.parse(SAMPLE);

        // Deux ecritures possibles de la meme cle de langue : la barre oblique
        // inversee litterale, comme dans l'original, et le vrai retour a la ligne.
        List<AboutDocument.Line> literal = document.toLines("ligne 1\\nligne 2");
        List<AboutDocument.Line> real = document.toLines("ligne 1\nligne 2");

        assertEquals(2, countAfter(literal, "Donators"), "deux lignes d'explication");
        assertEquals(2, countAfter(real, "Donators"), "les deux ecritures doivent donner le meme resultat");
        assertEquals(0.7f, literal.get(indexOf(literal, "Donators") + 1).scale(), 0.001f);
    }

    @Test
    void lesDonateursSontRepartisEnColonnes() {
        AboutDocument document = AboutDocument.parse(SAMPLE);

        List<List<String>> rows = document.donatorRows();

        assertEquals(2, rows.size(), "quatre noms tiennent sur deux lignes de trois colonnes");
        assertEquals(List.of("a", "b", "c"), rows.get(0));
        assertEquals(List.of("d"), rows.get(1), "la derniere ligne peut etre plus courte");
    }

    @Test
    void packRepartitEtRefuseLesCasLimites() {
        assertTrue(AboutDocument.pack(List.of(), 3).isEmpty());
        assertTrue(AboutDocument.pack(List.of("a"), 0).isEmpty(), "un paquet de taille nulle n'a pas de sens");

        List<List<String>> rows = AboutDocument.pack(List.of("a", "b", "c", "d", "e"), 2);
        assertEquals(3, rows.size());
        assertEquals(List.of("a", "b"), rows.get(0));
        assertEquals(List.of("e"), rows.get(2));
    }

    @Test
    void laListeLivreTousLesDonateursSansPerte() {
        AboutDocument document = AboutDocument.load();

        int packed = document.donatorRows().stream().mapToInt(List::size).sum();
        assertEquals(document.getDonators().size(), packed, "aucun nom ne doit etre perdu au rangement");
    }

    @Test
    void leDocumentNeSePartagePas() {
        AboutDocument document = AboutDocument.parse(SAMPLE);

        assertThrows(UnsupportedOperationException.class, () -> document.getDonators().add("intrus"));
        assertThrows(UnsupportedOperationException.class, () -> document.getStaff().clear());
    }

    private static int indexOf(List<AboutDocument.Line> lines, String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).text().equals(text)) return i;
        }
        return -1;
    }

    /** Combien de lignes suivent celle qui porte ce texte. */
    private static int countAfter(List<AboutDocument.Line> lines, String text) {
        int index = indexOf(lines, text);
        return index < 0 ? -1 : lines.size() - index - 1;
    }
}
