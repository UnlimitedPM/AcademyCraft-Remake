package cn.academy.ability.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le compteur de charge du client : son age, et sa proportion quand il en a une.
 *
 * <p>Le port n'ouvrait ce compteur que pour les charges <b>bornees</b> — celles qui ont une barre
 * a remplir — et laissait les autres a zero. Or l'age d'une charge sert aussi ailleurs : la portee
 * du fantome de teleportation au marqueur grandit avec lui. Le joueur l'a vu tout de suite : « le
 * fantome est trop pres de moi et il n'avance pas ».
 */
class ClientChargeTest {

    @Test
    @DisplayName("une charge sans maximum compte quand meme ses ticks")
    void uneChargeSansMaximumCompteSesTicks() {
        ClientCharge.begin("mark_teleport", 0);

        assertTrue(ClientCharge.isActive());
        assertFalse(ClientCharge.isSustained(), "c'est une charge, pas un maintien");
        assertEquals("mark_teleport", ClientCharge.getSkill());
        assertEquals(0, ClientCharge.getTicks());
        assertEquals(0f, ClientCharge.getFraction(), 1e-6, "rien a remplir, donc rien a montrer");

        for (int i = 0; i < 20; i++) {
            ClientCharge.tick();
        }
        assertEquals(20, ClientCharge.getTicks(), "et le compteur avance d'un tick par tick");
        assertEquals(0f, ClientCharge.getFraction(), 1e-6,
                "sans maximum, il n'y a pas de proportion a montrer");

        ClientCharge.end();
        assertFalse(ClientCharge.isActive());
        assertNull(ClientCharge.getSkill(), "et la fin oublie tout");
        assertEquals(0, ClientCharge.getTicks());
    }

    @Test
    @DisplayName("une charge bornee remplit sa proportion")
    void uneChargeBorneeRemplitSaProportion() {
        ClientCharge.begin("thunder_clap", 20);

        for (int i = 0; i < 5; i++) {
            ClientCharge.tick();
        }
        assertEquals(0.25f, ClientCharge.getFraction(), 1e-6, "un quart de la charge");
        assertEquals(5, ClientCharge.getTicks());

        for (int i = 0; i < 30; i++) {
            ClientCharge.tick();
        }
        assertEquals(1f, ClientCharge.getFraction(), 1e-6, "et la proportion plafonne a un");
        assertEquals(35, ClientCharge.getTicks(), "meme si l'age, lui, continue");

        ClientCharge.end();
    }

    @Test
    @DisplayName("un maintien n'est pas une charge")
    void unMaintienNEstPasUneCharge() {
        ClientCharge.beginSustained("light_shield");

        assertTrue(ClientCharge.isSustained());
        assertFalse(ClientCharge.isActive());
        ClientCharge.tick();
        assertEquals(1, ClientCharge.getTicks(), "il s'age, lui aussi");
        assertEquals(0f, ClientCharge.getFraction(), 1e-6, "mais il n'a pas de proportion");

        ClientCharge.end();
        assertFalse(ClientCharge.isSustained());
    }
}
