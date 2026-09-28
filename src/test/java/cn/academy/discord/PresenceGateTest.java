package cn.academy.discord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Le rythme des fiches, relu sans attendre.
 *
 * <p>Le temps est passe en parametre : c'est ce qui permet de verifier la fenetre de quinze
 * secondes de Discord sans faire durer le test.
 */
class PresenceGateTest {

    private static DiscordPresence fiche(String details) {
        return new DiscordPresence(details, null, 0L, null, null, null, null, List.of());
    }

    @Test
    @DisplayName("la premiere fiche part tout de suite")
    void laPremierePartToutDeSuite() {
        PresenceGate gate = new PresenceGate();

        assertEquals(fiche("menu"), gate.due(fiche("menu"), 0L));
        assertNull(gate.due(fiche("menu"), 1L), "et elle ne repart pas une deuxieme fois");
    }

    @Test
    @DisplayName("un changement trop tot attend la fin de la fenetre, sans se perdre")
    void unChangementTropTotAttend() {
        PresenceGate gate = new PresenceGate();
        assertEquals(fiche("menu"), gate.due(fiche("menu"), 0L));

        assertNull(gate.due(fiche("solo"), 1_000L), "trop tot : Discord refuserait la fiche");
        assertNull(gate.due(fiche("solo"), 14_999L), "toujours dans la fenetre");

        assertEquals(fiche("solo"), gate.due(fiche("solo"), 15_000L), "la fiche attendue part enfin");
        assertNull(gate.due(fiche("solo"), 30_000L), "et elle ne part qu'une fois");
    }

    @Test
    @DisplayName("la fiche la plus recente gagne : un retour en arriere ne part pas")
    void laDerniereVoulueGagne() {
        PresenceGate gate = new PresenceGate();
        gate.due(fiche("menu"), 0L);

        assertNull(gate.due(fiche("solo"), 1_000L));
        assertNull(gate.due(fiche("menu"), 2_000L), "revenue au menu : rien a changer");
        assertNull(gate.due(fiche("menu"), 20_000L), "et rien ne repart, la fiche en place est la bonne");
    }

    @Test
    @DisplayName("apres une reconnexion, la fiche repart sans attendre")
    void apresReconnexionToutRepart() {
        PresenceGate gate = new PresenceGate();
        gate.due(fiche("menu"), 0L);

        gate.reset();

        assertEquals(fiche("menu"), gate.due(fiche("menu"), 1L),
                "une conversation neuve attend une fiche, meme la meme");
    }

    @Test
    @DisplayName("sans fiche voulue, il n'y a rien a envoyer")
    void sansFicheRienNestEnvoye() {
        PresenceGate gate = new PresenceGate();
        assertNull(gate.due(null, 0L));
    }
}
