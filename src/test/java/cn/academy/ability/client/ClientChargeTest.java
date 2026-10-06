package cn.academy.ability.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les charges du client : leur age, leur proportion, et le fait qu'elles vivent ENSEMBLE.
 *
 * <p>Le port n'ouvrait ce compteur que pour les charges <b>bornees</b> — celles qui ont une barre
 * a remplir — et laissait les autres a zero. Or l'age d'une charge sert aussi ailleurs : la portee
 * du fantome de teleportation au marqueur grandit avec lui. Le joueur l'a vu tout de suite : « le
 * fantome est trop pres de moi et il n'avance pas ».
 *
 * <p>Et il n'y avait qu'un compteur pour tout le jeu, ce que le dernier test fige : ouvrir une
 * seconde charge ecrasait la premiere, donc les ailes de tempete retombaient des qu'on lancait
 * autre chose. Voir {@code ClientCharge}.
 */
class ClientChargeTest {

    @Test
    @DisplayName("une charge sans maximum compte quand meme ses ticks")
    void uneChargeSansMaximumCompteSesTicks() {
        ClientCharge.clear();
        ClientCharge.begin("mark_teleport", 0);

        assertTrue(ClientCharge.isOpen("mark_teleport"));
        assertFalse(ClientCharge.isSustained("mark_teleport"), "c'est une charge, pas un maintien");
        assertEquals("mark_teleport", ClientCharge.getSkill());
        assertEquals(0, ClientCharge.getTicks("mark_teleport"));
        assertEquals(0f, ClientCharge.getFraction("mark_teleport"), 1e-6,
                "rien a remplir, donc rien a montrer");

        for (int i = 0; i < 20; i++) {
            ClientCharge.tick("mark_teleport");
        }
        assertEquals(20, ClientCharge.getTicks("mark_teleport"),
                "et le compteur avance d'un tick par tick");
        assertEquals(0f, ClientCharge.getFraction("mark_teleport"), 1e-6,
                "sans maximum, il n'y a pas de proportion a montrer");

        ClientCharge.end("mark_teleport");
        assertFalse(ClientCharge.isOpen("mark_teleport"));
        assertNull(ClientCharge.getSkill(), "et la fin oublie tout");
        assertEquals(0, ClientCharge.getTicks("mark_teleport"), "plus rien a lire");
    }

    @Test
    @DisplayName("une charge bornee remplit sa proportion")
    void uneChargeBorneeRemplitSaProportion() {
        ClientCharge.clear();
        ClientCharge.begin("thunder_clap", 20);

        for (int i = 0; i < 5; i++) {
            ClientCharge.tick("thunder_clap");
        }
        assertEquals(0.25f, ClientCharge.getFraction("thunder_clap"), 1e-6, "un quart de la charge");
        assertEquals(5, ClientCharge.getTicks("thunder_clap"));

        for (int i = 0; i < 30; i++) {
            ClientCharge.tick("thunder_clap");
        }
        assertEquals(1f, ClientCharge.getFraction("thunder_clap"), 1e-6,
                "et la proportion plafonne a un");
        assertEquals(35, ClientCharge.getTicks("thunder_clap"), "meme si l'age, lui, continue");

        ClientCharge.end("thunder_clap");
    }

    @Test
    @DisplayName("un maintien n'est pas une charge")
    void unMaintienNEstPasUneCharge() {
        ClientCharge.clear();
        ClientCharge.beginSustained("light_shield");

        assertTrue(ClientCharge.isSustained("light_shield"));
        assertFalse(ClientCharge.isOpen("jet_engine"), "et aucun autre maintien n'est ouvert");
        ClientCharge.tick("light_shield");
        assertEquals(1, ClientCharge.getTicks("light_shield"), "il s'age, lui aussi");
        assertEquals(0f, ClientCharge.getFraction("light_shield"), 1e-6,
                "mais il n'a pas de proportion");

        ClientCharge.end("light_shield");
        assertFalse(ClientCharge.isSustained("light_shield"));
    }

    /**
     * Deux competences ouvertes EN MEME TEMPS, chacune avec son age.
     *
     * <p>C'est le vol qui a menti : les ailes de tempete se tiennent, et le joueur peut charger
     * autre chose pendant qu'il vole. Avec un seul compteur, la seconde charge ecrasait la
     * premiere — les ailes lisaient l'age de l'autre competence, leur vol retombait, et le verrou
     * de leurs touches de deplacement restait ferme. Le joueur : « si j'utilise les ailes et que
     * pendant que je les utilise j'utilise un autre pouvoir, ca casse le pouvoir des ailes et je
     * suis dans un etat presque fige ou je ne peut rien faire ».
     */
    @Test
    @DisplayName("deux charges vivent cote a cote")
    void deuxChargesViventCoteACote() {
        ClientCharge.clear();
        ClientCharge.beginSustained("storm_wing");
        for (int i = 0; i < 40; i++) {
            ClientCharge.tick("storm_wing");
        }

        // Le joueur charge autre chose pendant que les ailes volent. Sa charge a lui s'ouvre, et
        // celle des ailes ne bouge pas d'un tick.
        ClientCharge.begin("directed_blastwave", 40);
        ClientCharge.tick("directed_blastwave");

        assertEquals(40, ClientCharge.getTicks("storm_wing"), "les ailes gardent leur age");
        assertEquals(1, ClientCharge.getTicks("directed_blastwave"), "la nouvelle a le sien");
        assertTrue(ClientCharge.isSustained("storm_wing"), "et les ailes tiennent toujours");
        assertTrue(ClientCharge.isOpen("directed_blastwave"));

        // C'est la plus recente qui est « en cours » — le son de boucle et le temoin n'en suivent
        // qu'une — et l'ordre est celui qu'attend RippleOverlay : la plus recente d'abord.
        assertEquals("directed_blastwave", ClientCharge.getSkill());
        assertEquals(java.util.List.of("directed_blastwave", "storm_wing"), ClientCharge.openSkills());

        // Et la precedente reprend la main quand celle-la se referme, sans avoir rien perdu.
        ClientCharge.end("directed_blastwave");
        assertEquals("storm_wing", ClientCharge.getSkill(), "les ailes reprennent la main");
        assertEquals(40, ClientCharge.getTicks("storm_wing"), "sans avoir rien perdu");

        ClientCharge.end("storm_wing");
        assertFalse(ClientCharge.anyOpen(), "et rien ne reste ouvert");
        assertNull(ClientCharge.getSkill());
    }
}
