package cn.academy.client.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les nappes de l'imag phase liquide, relues sans Minecraft.
 *
 * <p>Ce sont les chiffres de l'original : trois hauteurs, six vitesses de defilement, deux
 * densites. Une inversion se verrait a l'oeil nu en jeu — mais seulement en jeu, justement.
 */
class ImagPhaseLayersTest {

    private static final double PLEIN = 0.875d; // une source de fluide pleine

    @Test
    @DisplayName("un lac profond porte trois nappes, un fluide mince deux")
    void leNombreDeNappesSuitLaProfondeur() {
        assertEquals(3, ImagPhaseLayers.layers(PLEIN).length, "source pleine");
        assertEquals(2, ImagPhaseLayers.layers(0.1d).length, "une flaque");
        // La bascule est sur la hauteur de la PILE (1.2 * sqrt(h) > 0.5), donc a h > 0.1736.
        assertEquals(2, ImagPhaseLayers.layers(0.17d).length);
        assertEquals(3, ImagPhaseLayers.layers(0.18d).length);
    }

    @Test
    @DisplayName("la pile est plus haute que le liquide")
    void laPileDebordeDuLiquide() {
        assertEquals(0.0d, ImagPhaseLayers.stackHeight(0.0d), 0.0001d);
        // 1.2 * sqrt(0.875) = 1.1225 : les nappes montent au-dessus du bloc de fluide.
        assertEquals(1.1225d, ImagPhaseLayers.stackHeight(PLEIN), 0.001d);
        assertEquals(1.2d, ImagPhaseLayers.stackHeight(1.0d), 0.0001d);
    }

    @Test
    @DisplayName("les nappes sont a leurs hauteurs d'origine")
    void lesHauteursSontCellesDeLOriginal() {
        ImagPhaseLayers.Layer[] couches = ImagPhaseLayers.layers(PLEIN);
        double ht = ImagPhaseLayers.stackHeight(PLEIN);

        // La basse nage SOUS la surface : elle est a -0.3 * ht, donc invisible dans le bloc.
        assertEquals(-0.3d * ht, couches[0].height(), 0.0001d);
        assertEquals(0.35d * ht, couches[1].height(), 0.0001d);
        assertEquals(0.7d * ht, couches[2].height(), 0.0001d);

        // Et chaque nappe a sa vitesse de defilement et la meme densite.
        assertEquals(0.3d, couches[0].speedU(), 0.0001d);
        assertEquals(0.2d, couches[0].speedV(), 0.0001d);
        assertEquals(0.3d, couches[1].speedU(), 0.0001d);
        assertEquals(0.05d, couches[1].speedV(), 0.0001d);
        assertEquals(0.1d, couches[2].speedU(), 0.0001d);
        assertEquals(0.25d, couches[2].speedV(), 0.0001d);
        for (ImagPhaseLayers.Layer couche : couches) {
            assertEquals(0.7d, couche.density(), 0.0001d, "la densite ne change pas");
        }
    }

    @Test
    @DisplayName("sur une surface pleine, la hauteur par coin redonne la hauteur d'origine")
    void heightAtRedonneLaHauteurSurUneSurfacePleine() {
        for (ImagPhaseLayers.Layer couche : ImagPhaseLayers.layers(PLEIN)) {
            assertEquals(couche.height(), couche.heightAt(PLEIN), 0.000001d,
                    "une surface plate ne doit rien changer");
        }
    }

    @Test
    @DisplayName("sur un liquide qui coule, la nappe suit la hauteur de chaque coin")
    void laNappeSuitLaPente() {
        ImagPhaseLayers.Layer basse = ImagPhaseLayers.layers(PLEIN)[0];
        ImagPhaseLayers.Layer haute = ImagPhaseLayers.layers(PLEIN)[1];

        // Un coin a moitie plein ne se pose pas a la meme hauteur qu'un coin plein : c'est ce qui
        // fait suivre la pente a la nappe au lieu de la laisser flotter a plat au-dessus.
        assertNotEquals(basse.heightAt(1.0d), basse.heightAt(0.5d), 0.0001d, "la nappe basse se penche");
        assertNotEquals(haute.heightAt(1.0d), haute.heightAt(0.5d), 0.0001d, "la nappe haute se penche aussi");
        // La basse est SOUS la surface : un coin plus bas la remonte donc vers zero.
        assertTrue(basse.heightAt(0.5d) > basse.heightAt(1.0d));
        // La haute est AU-DESSUS : un coin plus bas la descend.
        assertTrue(haute.heightAt(0.5d) < haute.heightAt(1.0d));
    }

    @Test
    @DisplayName("la texture defile et revient a zero")
    void laTextureDefileSansDerive() {
        ImagPhaseLayers.Layer basse = ImagPhaseLayers.layers(PLEIN)[0];

        assertEquals(0.0d, basse.offsetU(0.0d), 0.0001d);
        assertEquals(0.3d, basse.offsetU(1.0d), 0.0001d);
        // Au bout d'un tour, elle repart de zero : la texture est carree, elle s'y raccorde.
        assertEquals(0.0d, basse.offsetU(1.0d / 0.3d), 0.0001d);
        assertEquals(0.0d, basse.offsetU(10.0d * (1.0d / 0.3d)), 0.0001d);
        // Sur l'autre axe, la vitesse n'est pas la meme : les nappes ne glissent pas droit.
        assertTrue(basse.offsetV(1.0d) != basse.offsetU(1.0d));
    }

    @Test
    @DisplayName("l'opacite decroit avec la distance et disparait au loin")
    void lOpaciteDecroitAvecLaDistance() {
        assertEquals(1.0f, ImagPhaseLayers.alpha(0.0d), 0.0001f);
        assertEquals(1f / 1.2f, ImagPhaseLayers.alpha(1.0d), 0.0001f);
        // Et sous le dixieme, l'original ne dessinait plus rien : a 45 blocs.
        assertEquals(ImagPhaseLayers.MIN_ALPHA, ImagPhaseLayers.alpha(45.0d), 0.001f);
        assertTrue(ImagPhaseLayers.alpha(46.0d) < ImagPhaseLayers.MIN_ALPHA);
        // Un liquide plus loin doit s'effacer, jamais se demonter.
        assertTrue(ImagPhaseLayers.alpha(10.0d) < ImagPhaseLayers.alpha(2.0d));
    }
}
