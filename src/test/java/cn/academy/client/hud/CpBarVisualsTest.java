package cn.academy.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le temoin de points de controle : sa geometrie et ses couleurs.
 *
 * <p>Ce sont les nombres de {@code CPBar} de l'original, et rien d'autre. Les figer ici evite
 * qu'un ajustement de dessin ne les fasse deriver sans qu'on s'en apercoive — c'est exactement
 * le genre de valeur qu'on « corrige » a l'oeil et qu'on regrette ensuite.
 */
class CpBarVisualsTest {

    @Test
    void leRemplissageNePartJamaisDeZeroNiNArriveAuBout() {
        // L'original le fait vivre entre 16 % et 96 % de ses 883 pixels : une barre vide montre
        // donc encore un morceau, et une barre pleine n'atteint pas le bout du fond.
        assertEquals(883 * 0.16, CpBarVisuals.fillWidth(0.0f), 0.01);
        assertEquals(883 * 0.96, CpBarVisuals.fillWidth(1.0f), 0.01);
        assertEquals(883 * 0.56, CpBarVisuals.fillWidth(0.5f), 0.01);
        assertEquals(CpBarVisuals.fillWidth(0.0f), CpBarVisuals.fillWidth(-3.0f), 0.001,
                "un niveau negatif ne descend pas plus bas");
        assertEquals(CpBarVisuals.fillWidth(1.0f), CpBarVisuals.fillWidth(7.0f), 0.001,
                "un niveau trop haut ne monte pas plus haut");
    }

    @Test
    void leRemplissagePousseDepuisLaDroite() {
        // Le bord droit est fixe : c'est le gauche qui recule quand le niveau monte.
        double right = CpBarVisuals.FILL_X + CpBarVisuals.FILL_W;
        assertEquals(right, CpBarVisuals.fillLeft(1.0f) + CpBarVisuals.fillWidth(1.0f), 0.01);
        assertEquals(right, CpBarVisuals.fillLeft(0.0f) + CpBarVisuals.fillWidth(0.0f), 0.01);
        assertTrue(CpBarVisuals.fillLeft(1.0f) < CpBarVisuals.fillLeft(0.0f),
                "un niveau plus haut commence plus a gauche");
    }

    @Test
    void lExtremiteEstCoupeeEnBiais() {
        // 103 pixels pour 44 degres : le bas du remplissage est donc decale vers la droite par
        // rapport au haut, d'un peu moins de 72 pixels. C'est ce biais que le dessin respecte
        // ligne par ligne, et c'est pour cela qu'un simple rectangle ne suffit pas.
        assertEquals(103 * Math.sin(Math.toRadians(44)), CpBarVisuals.CUT, 0.001);
        assertEquals(CpBarVisuals.fillLeft(0.5f), CpBarVisuals.fillLeftAt(0.5f, 0), 0.001,
                "la premiere ligne part du bord");
        assertEquals(CpBarVisuals.fillLeft(0.5f) + CpBarVisuals.CUT,
                CpBarVisuals.fillLeftAt(0.5f, CpBarVisuals.FILL_H), 0.001,
                "la derniere ligne est decalee de toute la coupe");
        assertTrue(CpBarVisuals.fillLeftAt(0.5f, 42) > CpBarVisuals.fillLeftAt(0.5f, 0),
                "le bord descend vers la droite");
    }

    @Test
    void laSurchargePousseAussiVersLaGauche() {
        assertEquals(943.0, CpBarVisuals.overloadWidth(1.0f), 0.01);
        assertEquals(0.0, CpBarVisuals.overloadWidth(-1.0f), 0.01, "borne a vide");
        assertEquals(943.0, CpBarVisuals.overloadWidth(4.0f), 0.01, "borne a plein");
        assertEquals(0.0, CpBarVisuals.OVER_X + CpBarVisuals.OVER_W
                - CpBarVisuals.overloadLeft(0.0f), 0.01, "vide : la bande n'a aucune largeur");
        assertEquals(CpBarVisuals.OVER_X, CpBarVisuals.overloadLeft(1.0f), 0.01,
                "pleine : la bande touche le bord gauche");
    }

    @Test
    void lesCouleursSontCellesDeLoriginal() {
        // Le remplissage : rouge a vide, orange a un tiers, blanc a plein.
        assertEquals(0xFFF06767, CpBarVisuals.fillColor(0.0f));
        assertEquals(0xFFFFAE44, CpBarVisuals.fillColor(0.35f));
        assertEquals(0xFFFFFFFF, CpBarVisuals.fillColor(1.0f));

        // La surcharge : presque transparente, puis doree, puis rouge. Son opacite monte, donc
        // la bande se voit de plus en plus.
        assertEquals(0x0ADFDFDF, CpBarVisuals.overloadColor(0.0f));
        assertEquals(0x23F0D49D, CpBarVisuals.overloadColor(0.55f));
        assertEquals(0x50F56464, CpBarVisuals.overloadColor(1.0f));
        assertTrue(alpha(CpBarVisuals.overloadColor(1.0f))
                        > alpha(CpBarVisuals.overloadColor(0.0f)),
                "la surcharge s'opacifie en montant");

        // Entre deux arrets, la couleur est melangee : rouge et orange a parts egales pour le
        // remplissage a 0,175.
        int middle = CpBarVisuals.fillColor(0.175f);
        assertEquals(0xFF, alpha(middle), "l'opacite du remplissage ne bouge pas");
        assertTrue(red(middle) > red(CpBarVisuals.fillColor(0.0f)) && red(middle) < 0xFF,
                "le rouge monte entre les deux arrets");
    }

    @Test
    void laBarreALaTailleAnnoncee() {
        // L'image fait 964x147 et l'original la dessine a 0,2 : cela doit tomber sur la taille
        // que HudElement.CP_BAR annonce, sinon le cadre de l'ecran de reglage serait faux.
        assertEquals(HudElement.CP_BAR.getWidth(),
                Math.round(CpBarVisuals.TEX_W * CpBarVisuals.SCALE));
        assertEquals(HudElement.CP_BAR.getHeight(),
                Math.round(CpBarVisuals.TEX_H * CpBarVisuals.SCALE));
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }
}
