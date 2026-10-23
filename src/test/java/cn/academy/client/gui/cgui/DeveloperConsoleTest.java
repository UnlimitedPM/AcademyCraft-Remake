package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La console du developpeur, relue sans Minecraft.
 *
 * <p>Elle porte trois choses qui se cassent en silence : le texte qui s'ecrit lettre par lettre, les
 * deux caracteres de controle de l'original ({@code \b} efface, {@code \n} ouvre une ligne), et la
 * fenetre de dix lignes. Aucune des quatre portes ne voit un ecran, donc c'est ici que la mecanique
 * se fige.
 */
class DeveloperConsoleTest {

    private static DeveloperConsole.Texts texts() {
        return new DeveloperConsole.Texts(
                "Welcome %s", "Boot Failed.\n", "FATAL\n", "Type `learn`.\n", "OVERRIDE\n",
                "Invalid command.\n", percent -> "Progress: " + percent + "%",
                "Start\n", "Success.\n", "Failed.\n",
                "Reset...", "Reset ok.\n", "Reset failed.\n", "Need advanced.\n", "Need factor.\n");
    }

    private static DeveloperConsole console(boolean emergency, boolean hasDeveloper) {
        // Un tirage fixe : la sequence de demarrage tire six nombres, et un test qui les compare ne
        // peut pas dependre du hasard.
        return new DeveloperConsole(texts(), "Steve", emergency, hasDeveloper, new Random(1));
    }

    /** Fait avancer la console jusqu'a ce qu'il n'y ait plus rien a jouer. */
    private static void run(DeveloperConsole console, double seconds) {
        double step = 0.01;
        for (double t = 0; t <= seconds; t += step) {
            console.tick(t);
        }
    }

    @Test
    void leTexteSEcritLettreParLettre() {
        DeveloperConsole console = console(false, true);

        console.tick(0.0);
        String first = console.lines().get(0);
        console.tick(0.05);
        String later = console.lines().get(0);

        assertTrue(later.length() > first.length(),
                "le texte doit avancer d'une image a l'autre : " + first + " -> " + later);
        assertTrue("Welcome Steve".startsWith(later), "et rester le debut du texte");
    }

    @Test
    void laSequenceDeDemarrageSEfface() {
        DeveloperConsole console = console(false, true);
        run(console, 6.0);

        String all = String.join("\n", console.lines());
        assertTrue(all.contains("Boot Failed."), "l'echec du demarrage doit rester : " + all);
        assertFalse(all.contains("%"), "et les nombres de la sequence doivent s'etre effaces : " + all);
    }

    @Test
    void laConsoleAttendLaFrappeEtPorteLeCurseur() {
        DeveloperConsole console = console(false, true);
        run(console, 6.0);

        String last = console.lines().get(console.lines().size() - 1);
        assertTrue(last.startsWith(DeveloperConsole.HEAD), "l'invite doit etre la : " + last);

        console.type('l');
        console.type('e');
        assertTrue(console.lines().get(console.lines().size() - 1).contains("le"),
                "la frappe doit s'afficher");
        console.backspace();
        assertEquals("l", console.input(), "et le retour arriere doit effacer une lettre");
    }

    @Test
    void uneCommandeInconnueEstRefusee() {
        DeveloperConsole console = console(false, true);
        run(console, 6.0);

        console.type('x');
        console.submit();
        run(console, 1.0);

        assertTrue(String.join("\n", console.lines()).contains("Invalid command."),
                "une commande inconnue doit le dire");
    }

    @Test
    void uneCommandeConnuePart() {
        DeveloperConsole console = console(false, true);
        boolean[] ran = { false };
        console.command("learn", () -> ran[0] = true);
        run(console, 6.0);

        for (char c : "learn".toCharArray()) console.type(c);
        console.submit();

        assertTrue(ran[0], "la commande doit s'executer");
        assertEquals("", console.input(), "et la ligne doit se vider");
    }

    @Test
    void laFenetreNeGardeQueDixLignes() {
        DeveloperConsole console = console(false, true);
        for (int i = 0; i < 30; i++) {
            console.outputln("ligne " + i);
        }

        List<String> lines = console.lines();
        assertEquals(DeveloperConsole.MAX_LINES, lines.size(), "dix lignes, comme l'original");
        assertTrue(String.join("\n", lines).contains("ligne 29"), "les dernieres restent");
        assertFalse(String.join("\n", lines).contains("ligne 0"), "les premieres sont parties");
    }

    @Test
    void leRetourArriereEffaceEtLeSautOuvreUneLigne() {
        DeveloperConsole console = console(false, true);

        console.output("abc\b\bX");
        assertEquals("aX", console.lines().get(0), "deux retours arriere effacent deux lettres");

        // `outputln` ecrit a la SUITE de la ligne courante, comme l'original : c'est ce qui fait
        // qu'une commande tapee s'ajoute a l'invite au lieu de la remplacer.
        console.outputln("fin");
        assertEquals(2, console.lines().size(), "le saut de ligne ouvre une ligne");
        assertEquals("aXfin", console.lines().get(0), "et le texte s'ajoute a la ligne courante");
    }

    @Test
    void laBobineAnnonceLaSurcharge() {
        DeveloperConsole emergency = console(true, true);
        run(emergency, 6.0);
        assertTrue(String.join("\n", emergency.lines()).contains("OVERRIDE"),
                "la bobine doit annoncer la surcharge du systeme");

        DeveloperConsole normal = console(false, true);
        run(normal, 6.0);
        assertFalse(String.join("\n", normal.lines()).contains("OVERRIDE"),
                "et un joueur sans bobine ne doit pas la voir");
    }

    @Test
    void sansMachineLaConsoleNeProposePasLearn() {
        DeveloperConsole app = console(false, false);
        run(app, 6.0);

        String all = String.join("\n", app.lines());
        assertTrue(all.contains("FATAL"), "l'application dit que la categorie est invalide");
        assertFalse(all.contains("Type `learn`."),
                "mais elle ne propose pas une commande qu'elle ne sait pas executer : " + all);
    }

    @Test
    void laReconstructionNEstDemandeeQuUneFois() {
        DeveloperConsole console = console(false, true);
        assertFalse(console.rebuildRequested(), "au depart, rien a reconstruire");

        console.enqueueRebuild();
        // La file de demarrage passe avant : banniere, pause, sequence, verdict. Dix secondes
        // suffisent largement, et c'est le seul moyen de laisser la tache arriver a son tour.
        run(console, 10.0);

        assertTrue(console.rebuildRequested(), "la tache de reconstruction doit se voir");
    }
}
