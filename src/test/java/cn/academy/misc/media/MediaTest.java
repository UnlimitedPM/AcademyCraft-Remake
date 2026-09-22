package cn.academy.misc.media;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le lecteur media : tout ce qui se decide sans joueur ni client.
 *
 * <p>Un morceau est un nom range dans une donnee de joueur, et une suite de noms qui se
 * sauvegarde. Les deux se relisent ici — c'est le genre de chose qu'on ne veut pas decouvrir
 * en jeu, ou le seul symptome serait une liste vide.
 */
class MediaTest {

    @Test
    void lesTroisMorceauxDuModSontLaEtDansLOrdre() {
        List<Media> medias = MediaManager.internalMedias();
        assertEquals(3, medias.size(), "l'original livre trois morceaux");

        // L'ordre de l'original : c'est aussi celui de l'ecran.
        assertEquals("only_my_railgun", medias.get(0).id());
        assertEquals("level5_judgelight", medias.get(1).id());
        assertEquals("sisters_noise", medias.get(2).id());

        Set<String> seen = new HashSet<>();
        for (Media media : medias) {
            assertTrue(seen.add(media.id()), "un morceau ne peut pas etre livre deux fois");
            assertTrue(media.internal(), "les trois morceaux sont livres avec le mod");
        }
    }

    @Test
    void unMorceauSeRetrouveParSonNomEtParSonObjet() {
        assertNotNull(MediaManager.get("sisters_noise"));
        assertNull(MediaManager.get("inconnu"), "un morceau inconnu ne rend rien");
        assertNull(MediaManager.get(null), "et un nom nul non plus");

        for (Media media : MediaManager.internalMedias()) {
            assertNotNull(MediaManager.ofItemName(media.itemName()),
                    "l'objet d'un morceau doit rendre le morceau : " + media.itemName());
            assertEquals(media, MediaManager.ofItemName(media.itemName()));
        }
        assertNull(MediaManager.ofItemName("media_inconnu"));
    }

    @Test
    void leSonEtLobjetSuiventLeNomDuMorceau() {
        Media media = MediaManager.get("only_my_railgun");
        assertNotNull(media);
        assertEquals("academy:media/only_my_railgun", media.sound().toString(),
                "le fichier se resout depuis le nom, sans declaration");
        assertEquals("media_only_my_railgun", media.itemName(),
                "et l'objet porte le meme nom, precede de media_");
        assertEquals("ac.media.only_my_railgun.name", media.titleKey());
        assertEquals("ac.media.only_my_railgun.desc", media.descKey());
    }

    @Test
    void unMorceauSInstalleEtNeSePerdPas() {
        MediaAcquireData data = new MediaAcquireData();
        Media railgun = MediaManager.get("only_my_railgun");

        assertFalse(data.isInstalled(railgun), "on ne possede rien au depart");
        assertTrue(data.install(railgun), "installer un morceau doit changer quelque chose");
        assertTrue(data.isInstalled(railgun));
        assertFalse(data.install(railgun), "l'installer deux fois ne change plus rien");
        assertEquals(1, data.count());

        assertEquals(List.of(railgun), data.installed(),
                "un seul morceau, et c'est celui-la");
    }

    @Test
    void lesMorceauxPossedesSuiventLOrdreDesMorceauxLivres() {
        MediaAcquireData data = new MediaAcquireData();
        // Installes a l'envers : c'est l'ordre du mod qui doit gagner, pas celui des clics.
        data.install(MediaManager.get("sisters_noise"));
        data.install(MediaManager.get("only_my_railgun"));

        assertEquals(List.of("only_my_railgun", "sisters_noise"),
                data.installed().stream().map(Media::id).toList());
    }

    @Test
    void laDonneeSeSauvegardeEtSeRelit() {
        MediaAcquireData saved = new MediaAcquireData();
        saved.install(MediaManager.get("level5_judgelight"));
        saved.install(MediaManager.get("only_my_railgun"));

        MediaAcquireData reloaded = new MediaAcquireData();
        reloaded.deserializeNBT(saved.serializeNBT());

        assertEquals(2, reloaded.count(), "les morceaux doivent survivre a la sauvegarde");
        assertTrue(reloaded.isInstalled("only_my_railgun"));
        assertTrue(reloaded.isInstalled("level5_judgelight"));
        assertFalse(reloaded.isInstalled("sisters_noise"));
    }

    @Test
    void unMorceauQuiNExistePlusEstIgnoreALaRelecture() {
        // Le cas d'un contenu retire, ou d'une sauvegarde ecrite a la main : un nom qui ne
        // correspond a rien ne doit pas empecher le reste de se lire.
        MediaAcquireData data = new MediaAcquireData();
        data.install("morceau_disparu");
        data.install("only_my_railgun");

        MediaAcquireData reloaded = new MediaAcquireData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertEquals(1, reloaded.count(), "seul le morceau connu doit revenir");
        assertTrue(reloaded.isInstalled("only_my_railgun"));
        assertFalse(reloaded.isInstalled("morceau_disparu"));
    }

    @Test
    void uneDonneeVideSeRelitSansRienCasser() {
        MediaAcquireData data = new MediaAcquireData();
        data.deserializeNBT(new net.minecraft.nbt.CompoundTag());
        assertEquals(0, data.count());

        data.install(MediaManager.get("only_my_railgun"));
        data.deserializeNBT(new net.minecraft.nbt.CompoundTag());
        assertEquals(0, data.count(), "relire une donnee vide efface ce qui etait la");
    }
}
