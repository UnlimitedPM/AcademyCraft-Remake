package cn.academy.sound;

import java.util.List;
import java.util.Map;

/**
 * Quelles machines font une boucle sonore, et laquelle.
 *
 * <p>Portage des deux {@code TileEntitySound(this, ...).setLoop()} de l'original : le
 * fusionneur d'Imag et le formeur de metal font tourner un son <b>a leur position</b> tant
 * qu'ils travaillent. C'est le seul son du port qui ne vienne pas d'un joueur : tous les
 * autres appartiennent a une competence, donc a quelqu'un, et celui-la appartient a un
 * bloc.
 *
 * <h2>Pourquoi une table de noms, et pas des machines</h2>
 *
 * La classe est <b>pure</b> : elle rend des chaines, pas des {@code SoundEvent} — ceux-ci
 * vivent dans un registre, que JUnit ne charge pas. C'est le meme decoupage que pour les
 * competences tenues ({@link HeldLoops}), et pour la meme raison : le choix se verifie en
 * test, la lecture se fait en jeu.
 *
 * <p>Une faute dans un nom d'evenement ne ferait rien tomber : la machine tournerait dans
 * un silence total. D'ou les tests, qui relisent la table et le fichier des sons.
 */
public final class MachineLoops {

    /** Le fusionneur d'Imag, dont la recette se lit dans l'onglet du terminal. */
    public static final String IMAG_FUSOR = "imag_fusor";

    /** Le formeur de metal, le premier consommateur du reseau energetique. */
    public static final String METAL_FORMER = "metal_former";

    /** Le volume de l'original, pour les deux : {@code setVolume(0.6f)}. */
    public static final float WORK_VOLUME = 0.6f;

    /**
     * La categorie de l'original : {@code SoundCategory.BLOCKS}, que le constructeur de
     * {@code TileEntitySound} passait toujours.
     */
    public static final String BLOCKS = "block";

    /**
     * Une boucle de machine : l'evenement, son volume, et sa categorie.
     *
     * <p>Pas de son de mise en route : l'original n'en donnait aucun aux machines. Leur
     * boucle demarre d'un coup et s'arrete d'un coup — un moteur, pas une competence.
     */
    public record Loop(String event, float volume, String source) {}

    private static final Map<String, Loop> LOOPS = Map.of(
            IMAG_FUSOR, new Loop("machine.imag_fusor_work", WORK_VOLUME, BLOCKS),
            METAL_FORMER, new Loop("machine.machine_work", WORK_VOLUME, BLOCKS));

    private MachineLoops() {}

    /** La boucle d'une machine, ou {@code null} s'il n'y en a pas. */
    public static Loop forMachine(String machine) {
        return machine == null ? null : LOOPS.get(machine);
    }

    /** Les machines qui sonnent, pour le test. */
    public static List<String> machines() {
        return List.of(IMAG_FUSOR, METAL_FORMER);
    }
}
