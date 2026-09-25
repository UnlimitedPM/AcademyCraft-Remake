package cn.academy.misc.media;

import cn.academy.client.hud.MediaHudVisuals;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La duree des morceaux, lue dans les fichiers livres.
 *
 * <p>L'original decodait l'Ogg pour la connaitre ; le port lit ses en-tetes. Ce test le fait
 * sur les <b>vrais fichiers</b> du mod : c'est la seule facon de savoir que la lecture tombe
 * juste, et qu'un remplacement de morceau ne la casse pas.
 */
class OggDurationTest {

    private static final File MEDIA_FOLDER =
            new File("src/main/resources/assets/academy/sounds/media");

    @Test
    void lesMorceauxLivresOntLeurDuree() throws Exception {
        File[] files = MEDIA_FOLDER.listFiles((dir, name) -> name.endsWith(".ogg"));
        assertTrue(files != null && files.length > 0, "les morceaux livres doivent etre la");

        for (File file : files) {
            float seconds = OggDuration.seconds(Files.readAllBytes(file.toPath()));
            // Les trois morceaux du mod sont des chansons de quatre a cinq minutes. La borne
            // est large : ce qui est verifie, c'est que la lecture tombe sur une duree
            // PLAUSIBLE, et non sur zero ou sur un nombre absurde.
            assertTrue(seconds > 60f && seconds < 600f,
                    file.getName() + " : duree lue " + seconds + " s");
        }
    }

    @Test
    void laDureeEstCelleDuDernierEchantillon() throws Exception {
        // Verification croisee, faite a la main sur les en-tetes : le morceau dure
        // 11 333 999 echantillons a 44 100 Hz, soit 257,0 secondes.
        byte[] data = Files.readAllBytes(new File(MEDIA_FOLDER, "only_my_railgun.ogg").toPath());

        assertEquals(44100, OggDuration.sampleRate(data), "le taux du paquet d'identification");
        assertEquals(11333999L, OggDuration.lastGranule(data), "le granule de la derniere page");
        assertEquals(257.0f, OggDuration.seconds(data), 0.05f);
    }

    @Test
    void unFichierQuiNEstPasUnOggNeDonneRien() {
        assertEquals(0f, OggDuration.seconds(null));
        assertEquals(0f, OggDuration.seconds(new byte[0]));
        assertEquals(0f, OggDuration.seconds("ceci n'est pas un ogg".getBytes()));
        // Un Ogg coupe avant son identification : pas de taux, donc pas de duree.
        assertEquals(0f, OggDuration.seconds(new byte[] {'O', 'g', 'g', 'S', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
    }

    @Test
    void lHeureS_EcritEnDeuxChiffres() {
        // Portage de getDisplayTime : 04:30, et non 4:30.
        assertEquals("00:00", MediaHudVisuals.formatTime(0f));
        assertEquals("00:04", MediaHudVisuals.formatTime(4.9f));
        assertEquals("04:30", MediaHudVisuals.formatTime(270f));
        assertEquals("01:00", MediaHudVisuals.formatTime(60f));
        assertEquals("00:00", MediaHudVisuals.formatTime(-3f), "une position negative n'existe pas");
        assertEquals("00:00", MediaHudVisuals.formatTime(Float.NaN));
    }

    @Test
    void laBarreResteDansSesBornes() {
        assertEquals(0f, MediaHudVisuals.progress(0f, 100f));
        assertEquals(0.5f, MediaHudVisuals.progress(50f, 100f), 0.0001f);
        assertEquals(1f, MediaHudVisuals.progress(150f, 100f), 0.0001f);
        // Une duree inconnue laisse la barre vide : c'est le seul choix qui ne mente pas.
        assertEquals(0f, MediaHudVisuals.progress(50f, 0f));
        assertEquals(0f, MediaHudVisuals.progress(-1f, 100f));
    }
}
