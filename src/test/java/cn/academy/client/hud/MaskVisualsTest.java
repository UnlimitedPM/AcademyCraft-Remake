package cn.academy.client.hud;

import cn.academy.ability.Category;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Le voile d'ecran de l'original : sa couleur visee et son glissement.
 *
 * <p>Ce sont les nombres de {@code BackgroundMask} et de {@code Category} de l'original, et rien
 * d'autre. Les figer ici evite qu'un ajustement de dessin ne les fasse deriver — et la teinte de
 * chaque categorie se relit d'un coup d'oeil au lieu de se chercher dans quatre fichiers.
 */
class MaskVisualsTest {

    /** La teinte de l'electromaster, telle que l'original la posait : (20, 113, 208, 100). */
    private static final int ELECTRO = 0x641471D0;

    @Test
    void laSurchargeTeintToutEnRouge() {
        // Elle prime sur la couleur de la categorie : le joueur voit qu'il est en surcharge.
        // Son opacite d'origine, 170 sur 255, est doublee et plafonne donc a 255 — la teinte, elle,
        // ne bouge pas d'un chiffre.
        assertEquals(0xFFD01414, MaskVisuals.target(ELECTRO, true, true, ELECTRO));
        assertEquals(170, alpha(MaskVisuals.OVERLOAD_COLOR), "170 sur 255 chez l'original");
        assertEquals(208, channel(MaskVisuals.OVERLOAD_COLOR, 16));
        assertEquals(20, channel(MaskVisuals.OVERLOAD_COLOR, 8));
        assertEquals(20, channel(MaskVisuals.OVERLOAD_COLOR, 0));
    }

    @Test
    void lAptitudeAllumeePrendLaCouleurDeSaCategorie() {
        // La teinte de la categorie, opacite densifiee : 100 sur 255 devient 150.
        assertEquals(0x961471D0, MaskVisuals.target(0x00000000, false, true, ELECTRO));
    }

    @Test
    void leVoileEstDensifieSansChangerDeTeinte() {
        assertEquals(0x961471D0, MaskVisuals.boost(ELECTRO), "100 devient 150");
        assertEquals(0x787EFF84, MaskVisuals.boost(0x507EFF84), "80 devient 120");
        assertEquals(0xDAA4A4A4, MaskVisuals.boost(0x91A4A4A4), "145 devient 218");
        assertEquals(0xFFD01414, MaskVisuals.boost(0xAAD01414), "170 monte a 255, sans depasser");
        assertEquals(0x00000000, MaskVisuals.boost(0), "un voile sans opacite le reste");
    }

    @Test
    void eteinteLeVoilePerdSonOpaciteSansChangerDeTeinte() {
        // L'original gardait les trois canaux et ne baissait que l'opacite : le voile s'efface en
        // fondu, sans passer par le noir. La teinte de la categorie reste donc lisible pendant
        // tout le fondu.
        assertEquals(0x001471D0, MaskVisuals.target(ELECTRO, false, false, ELECTRO));
    }

    @Test
    void leVoileGlisseDUneUniteParSeconde() {
        assertEquals(255.0f, MaskVisuals.step(1.0f), 1e-4, "un plein aller en une seconde");
        assertEquals(25.5f, MaskVisuals.step(0.1f), 1e-4, "l'image d'un dixieme de seconde");
        assertEquals(0.0f, MaskVisuals.step(-1.0f), 1e-4, "un temps negatif ne fait rien");
    }

    @Test
    void leFonduAvanceSansJamaisDepasser() {
        // Une seconde pleine : l'opacite est arrivee a zero, et la teinte n'a pas bouge.
        assertEquals(0x00FFFFFF, MaskVisuals.smooth(0xFFFFFFFF, 0x00FFFFFF, MaskVisuals.step(1.0f)));
        // Une demi-seconde : la moitie du chemin, arrondie au canal.
        assertEquals(0x80FFFFFF, MaskVisuals.smooth(0xFFFFFFFF, 0x00FFFFFF, MaskVisuals.step(0.5f)));
        // Plus que le reste a parcourir : on s'arrete sur la visee, sans la depasser.
        assertEquals(0x00FFFFFF, MaskVisuals.smooth(0x20FFFFFF, 0x00FFFFFF, MaskVisuals.step(5.0f)));
        // Et au repos, rien ne bouge.
        assertEquals(0x961471D0, MaskVisuals.smooth(0x961471D0, 0x961471D0, MaskVisuals.step(1.0f)));
    }

    @Test
    void lesCouleursDesCategoriesSontCellesDeLoriginal() {
        // Les quatre `setColorStyle` de l'original, dans l'ordre ou il les posait.
        assertEquals(0x641471D0, ElectromasterCategory.INSTANCE.getColorStyle());
        assertEquals(0x507EFF84, MeltdownerCategory.INSTANCE.getColorStyle());
        assertEquals(0x91A4A4A4, TeleporterCategory.INSTANCE.getColorStyle());
        // vecmanip n'a que trois canaux chez lui — donc aucune opacite, donc aucun voile.
        assertEquals(0x00000000, VecmanipCategory.INSTANCE.getColorStyle());
    }

    @Test
    void uneCategorieSansTeinteResteBlancheEtPleine() {
        // Comme chez l'original, la valeur par defaut est un blanc opaque : c'est une teinte
        // posee, pas un oubli. Les quatre categories du port la remplacent toutes.
        assertEquals(0xFFFFFFFF, new Category("essai").getColorStyle());
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int channel(int argb, int shift) {
        return (argb >> shift) & 0xFF;
    }
}
