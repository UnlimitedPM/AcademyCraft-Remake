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
        // la bande se voit de plus en plus. Memes teintes que l'original, opacite densifiee
        // d'un facteur 1,5 a la demande du joueur (voir CpBarVisuals).
        assertEquals(0x0FDFDFDF, CpBarVisuals.overloadColor(0.0f));
        assertEquals(0x34F0D49D, CpBarVisuals.overloadColor(0.55f));
        assertEquals(0x78F56464, CpBarVisuals.overloadColor(1.0f));
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

    @Test
    void laSurchargeEstBienPlusTransparenteQueLeRemplissage() {
        // C'est le point que le joueur a vu : la bande de surcharge n'est pas opaque. Ses trois
        // arrets portent 10, 35 puis 80 sur 255 chez l'original — a peine visible au debut,
        // rouge pale a la fin — densifies d'un facteur 1,5 a sa demande (30/09), soit 15, 52
        // et 120. Le dessin doit lire ces nombres-la, et non imposer les siens.
        assertEquals(15, alpha(CpBarVisuals.overloadColor(0.0f)));
        assertEquals(52, alpha(CpBarVisuals.overloadColor(0.55f)));
        assertEquals(120, alpha(CpBarVisuals.overloadColor(1.0f)));
        assertEquals(255, alpha(CpBarVisuals.fillColor(0.5f)), "le remplissage, lui, est opaque");

        assertEquals(15 / 255.0f, CpBarVisuals.alphaOf(CpBarVisuals.overloadColor(0.0f)), 1e-6);
        assertEquals(1.0f, CpBarVisuals.alphaOf(0xFFFFFFFF), 1e-6);
        assertEquals(0.0f, CpBarVisuals.alphaOf(0x00FF0000), 1e-6);
        // Un blanc sans ses deux chiffres d'opacite n'en a aucune : c'est le piege qui a rendu
        // invisible tout ce que la barre teintait en blanc — son fond et l'etat de surcharge
        // entier, dessines avec 0xFFFFFF.
        assertEquals(0.0f, CpBarVisuals.alphaOf(0xFFFFFF), 1e-6);
        assertTrue(CpBarVisuals.alphaOf(CpBarVisuals.overloadColor(1.0f)) < 0.6f,
                "meme pleine, la surcharge reste plus transparente que le remplissage");
    }

    @Test
    void laBarreSuitSaValeurAuLieuDeLaSauter() {
        // Le balance de l'original : un pas lineaire, jamais de depassement, et le meme dans les
        // deux sens. C'est ce qui fait glisser la surcharge — et c'est ce glissement vers le bas,
        // couleur comprise, que le joueur appelle « l'overload qui va a l'envers ».
        assertEquals(0.2f, CpBarVisuals.balance(0.0f, 1.0f, 0.2f), 1e-6, "un pas vers le haut");
        assertEquals(0.8f, CpBarVisuals.balance(1.0f, 0.0f, 0.2f), 1e-6, "et un vers le bas");
        assertEquals(1.0f, CpBarVisuals.balance(0.9f, 1.0f, 0.2f), 1e-6,
                "sans jamais depasser la valeur visee");
        assertEquals(0.0f, CpBarVisuals.balance(0.1f, 0.0f, 0.2f), 1e-6);
        assertEquals(0.5f, CpBarVisuals.balance(0.5f, 0.5f, 0.2f), 1e-6, "immobile si rien ne bouge");
    }

    @Test
    void lAnimationVaADeuxUnitesParSeconde() {
        // La vitesse de l'original, la meme pour les deux barres : 2,0. Une seconde de jeu fait
        // donc traverser la barre en un demi-seconde, au lieu de la faire sauter d'un coup.
        assertEquals(2.0f, CpBarVisuals.BALANCE_SPEED, 1e-6);
        assertEquals(0.02f, CpBarVisuals.balanceStep(0.01f), 1e-6, "dix millisecondes");
        assertEquals(2.0f, CpBarVisuals.balanceStep(1.0f), 1e-6, "une seconde entiere");
        assertEquals(0.0f, CpBarVisuals.balanceStep(-1.0f), 1e-6,
                "un temps negatif ne fait pas reculer la barre");
    }

    @Test
    void leRappelDesPrereglagesSortEtRentreEnDeuxSecondes() {
        long far = Long.MAX_VALUE;
        // Le premier rappel nait en fondu sur le premier cinquieme de sa duree, puis tient, puis
        // s'efface sur le dernier cinquieme — la courbe du drawPresetHint de l'original.
        assertEquals(0.0f, CpBarVisuals.presetHintAlpha(0L, far), 1e-6, "a l'instant du changement");
        assertEquals(0.75f * 0.5f, CpBarVisuals.presetHintAlpha(200L, far), 1e-6, "un dixieme");
        assertEquals(0.75f, CpBarVisuals.presetHintAlpha(1000L, far), 1e-6, "au milieu, plein");
        assertEquals(0.75f * 0.5f, CpBarVisuals.presetHintAlpha(1800L, far), 1e-6, "neuf dixiemes");
        assertEquals(0.0f, CpBarVisuals.presetHintAlpha(CpBarVisuals.PRESET_HOLD_MS, far), 1e-6,
                "et il s'arrete net au bout de deux secondes");

        // Deux changements coup sur coup : le second n'a pas de fondu d'entree, sinon la barre
        // clignoterait a chaque appui repete de la touche.
        assertEquals(0.75f, CpBarVisuals.presetHintAlpha(100L, 100L), 1e-6);

        // Le numero reste lisible meme quand le carre s'efface.
        assertEquals(0.05f, CpBarVisuals.presetLabelAlpha(0.0f), 1e-6);
        assertEquals(0.6f, CpBarVisuals.presetLabelAlpha(0.75f), 1e-6);
    }

    @Test
    void lesQuatreCarresDuRappelNeSeChevauchentPas() {
        // 580 et 62 chez l'original : quatre carres de 52, espaces de 10.
        assertTrue(CpBarVisuals.PRESET_X + 3 * CpBarVisuals.PRESET_STEP + CpBarVisuals.PRESET_SIZE
                        <= CpBarVisuals.TEX_W,
                "le quatrieme carre tient dans la largeur de l'image");
        assertTrue(CpBarVisuals.PRESET_STEP > CpBarVisuals.PRESET_SIZE,
                "les carres sont separes, pas colles");
    }

    @Test
    void lEmblemeDuPouvoirTombeDansLeRemplissage() {
        // Le trou de l'original (cpbar_cp.frag) : une boite de 65 posee a (857, 43), donc dans le
        // remplissage (47..930 x 30..114) et couverte meme quand il est au plus court.
        assertTrue(CpBarVisuals.ICON_X >= CpBarVisuals.FILL_X, "l'embleme est dans le remplissage");
        assertTrue(CpBarVisuals.ICON_X + CpBarVisuals.ICON_SIZE
                        <= CpBarVisuals.FILL_X + CpBarVisuals.FILL_W,
                "et il ne depasse pas son bord droit");
        assertTrue(CpBarVisuals.ICON_Y >= CpBarVisuals.FILL_Y, "ni en haut");
        assertTrue(CpBarVisuals.ICON_Y + CpBarVisuals.ICON_SIZE
                        <= CpBarVisuals.FILL_Y + CpBarVisuals.FILL_H,
                "ni en bas");
        double shortest = CpBarVisuals.fillLeftAt(0.0f, CpBarVisuals.FILL_H - 1);
        assertTrue(shortest <= CpBarVisuals.ICON_X + 4,
                "meme au plus court, la coupe ne mord que le coin de la boite de l'embleme");
        // Et c'est pour ce coin-la que le dessin ramene le bord de la boite sur celui du
        // remplissage : a barre vide, le trou ne doit pas deborder dans le fond.
        assertEquals(CpBarVisuals.ICON_X, CpBarVisuals.emblemLeft(1.0f), 1e-6, "barre pleine");
        assertEquals(CpBarVisuals.ICON_X, CpBarVisuals.emblemLeft(0.5f), 1e-6, "a moitie");
        assertTrue(CpBarVisuals.emblemLeft(0.0f) > CpBarVisuals.ICON_X,
                "barre vide : le bord est ramene dans le remplissage");
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }
}
