package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les deux regles de la console qui se sont cassees en jeu.
 *
 * <p>Elles ne se voient ni a la compilation ni dans un GameTest : ce sont des questions d'ordre et
 * de longueur de texte, et le joueur les a vues avant nous.
 */
class DeveloperConsoleOrderTest {

    private static DeveloperConsole.Texts texts() {
        return new DeveloperConsole.Texts(
                "Welcome %s", "Boot Failed.\n", "FATAL\n", "Type `learn`.\n", "OVERRIDE\n",
                "Invalid command.\n", percent -> "Progress: " + percent + "%",
                "Start\n", "Success.\n", "Failed.\n",
                "Reset...", "Reset ok.\n", "Reset failed.\n", "Need advanced.\n", "Need factor.\n");
    }

    private static DeveloperConsole console() {
        return new DeveloperConsole(texts(), "Steve", false, true, new Random(1));
    }

    private static void run(DeveloperConsole console, double seconds) {
        for (double t = 0; t <= seconds; t += 0.01) console.tick(t);
    }

    @Test
    void laCommandeTapeeResteDansLaConsole() {
        // Le joueur : « quand je tape reset, visuellement il supprime le fait que j'avais ecrit
        // reset ». La ligne se vidait AVANT que la commande ne s'execute, donc `enqueue` recopiait
        // une ligne deja vide. L'original vidait apres.
        DeveloperConsole console = console();
        // La commande fait ce que fait la vraie : elle empile une tache. C'est cet `enqueue` qui
        // recopie la ligne courante — donc l'ordre entre lui et le vidage de la saisie est tout.
        console.command("reset", () -> console.enqueue(console.print("Reset...")));
        run(console, 6.0);

        for (char c : "reset".toCharArray()) console.type(c);
        console.submit();
        run(console, 1.0);

        String all = String.join("\n", console.lines());
        assertTrue(all.contains("reset"), "la commande tapee doit rester visible : " + all);
        assertEquals("", console.input(), "et la ligne de frappe doit etre vide");
    }

    @Test
    void lePourcentageNAPasDeZeroInutile() {
        // Le joueur : « normalement il n'y a pas de 0 affiche pour rien, juste 1% pas 001% ». Le
        // signe appartient a la phrase de la langue, pas a ce nombre : le mettre des deux cotes
        // donnait « 0%%49% » sur sa capture.
        assertEquals("0", DeveloperConsole.percent(0));
        assertEquals("1", DeveloperConsole.percent(1));
        assertEquals("9", DeveloperConsole.percent(9));
        assertEquals("10", DeveloperConsole.percent(10));
        assertEquals("100", DeveloperConsole.percent(100));
    }

    @Test
    void leDemarrageNeSeTerminePasEntreDeuxTaches() {
        // La banniere et la sequence de nombres sont DEUX taches, et entre les deux la file est
        // vide une image. Une condition « la file est vide » rendait donc la main au milieu de
        // l'animation — le joueur : « elle dure a peine 1 seconde ».
        DeveloperConsole console = console();

        // Juste apres la banniere, la file contient encore la pause et la sequence.
        run(console, 0.5);
        assertFalse(console.startupFinished(), "le demarrage ne doit pas etre fini si tot");

        run(console, 8.0);
        assertTrue(console.startupFinished(), "et il doit l'etre une fois tout joue");
    }
}
