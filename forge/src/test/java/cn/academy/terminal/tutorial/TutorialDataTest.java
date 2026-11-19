package cn.academy.terminal.tutorial;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce que le joueur a ouvert, et ce qui reste ouvert.
 *
 * L'ensemble s'ouvre une fois et ne se referme plus : c'est toute la difference entre un
 * tutoriel « obtenu » et un tutoriel « porte en ce moment ». Le second se refermerait en
 * rangeant son lingot dans un coffre.
 */
class TutorialDataTest {

    @Test
    void unTutorielSOuvreUneFoisEtResteOuvert() {
        TutorialData data = new TutorialData();

        assertFalse(data.isUnlocked("ores"));
        assertTrue(data.unlock("ores"), "la premiere ouverture change quelque chose");
        assertFalse(data.unlock("ores"), "la seconde, non");
        assertEquals(1, data.count());

        assertTrue(data.isUnlocked("ores"));
        assertFalse(data.isUnlocked("solar_generator"));
        assertFalse(data.isUnlocked(null), "un identifiant absent ne peut pas etre ouvert");
    }

    @Test
    void lEnsembleSurvitAUneSauvegarde() {
        TutorialData data = new TutorialData();
        data.unlock("ores");
        data.unlock("terminal");
        data.setTerminalGiven(true);

        CompoundTag tag = data.serializeNBT();
        TutorialData reloaded = new TutorialData();
        reloaded.deserializeNBT(tag);

        assertEquals(data.getUnlocked(), reloaded.getUnlocked());
        assertTrue(reloaded.isTerminalGiven(), "le cadeau ne se fait qu'une fois, meme apres un relog");
        assertEquals(2, reloaded.count());
    }

    @Test
    void uneCopieNePartagePasSonEnsemble() {
        TutorialData source = new TutorialData();
        source.unlock("ores");
        source.setTerminalGiven(true);

        TutorialData copy = new TutorialData();
        copy.copyFrom(source);
        source.unlock("misc");

        assertEquals(1, copy.count(), "la copie s'arrete a l'etat du moment");
        assertFalse(copy.isUnlocked("misc"));
    }

    @Test
    void leRemiseAZeroOublieTout() {
        TutorialData data = new TutorialData();
        data.unlock("ores");
        data.setTerminalGiven(true);

        data.reset();

        assertEquals(0, data.count());
        assertFalse(data.isTerminalGiven(), "sert aux tests, qui repartent d'un etat neuf");
    }
}
