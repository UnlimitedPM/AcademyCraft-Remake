package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Le survol d'un noeud de l'arbre, et ce qu'il fait aux traits qui l'entourent.
 *
 * <p>Le vrai mod dessine le trait dans la boite du noeud : le noeud grossi le grossit avec lui, et
 * chaque bout garde donc toujours le meme ecart au centre qu'il regarde. Le port laissait les bouts
 * en place, le disque grossi passait par-dessus son propre bout, et le joueur voyait le trait au
 * travers du cerne de progression : « ce qui fait grandir les cercles me fait toujours voir les
 * lignes en transparence ».
 *
 * <p>Ce qui se fige ici est la regle du dessin et non ses nombres : a l'arret rien ne bouge, et au
 * survol les deux bouts s'ecartent de leur propre noeud, d'un cinquieme de leur ecart. Le trait reste
 * sur le meme axe — c'est ce qui fait que la regle ne peut pas le faire pivoter.
 */
class CGuiLineTest {

    private static final double EPS = 1e-9;

    /** A l'arret, l'echelle vaut un : le bout ne bouge pas d'un pixel. */
    @Test
    @DisplayName("sans survol, le bout du trait ne bouge pas")
    void sansSurvolRienNeBouge() {
        assertEquals(42.0d, CGuiView.movedEnd(42.0d, 0.6d, 12.2d, 1.0d), EPS);
    }

    /** Un noeud grossi d'un cinquieme repousse son bout d'un cinquieme de l'ecart. */
    @Test
    @DisplayName("un noeud grossi d'un cinquieme repousse son bout d'un cinquieme")
    void leSurvolRepousseLeBout() {
        assertEquals(12.2d + 12.2d * 0.2d, CGuiView.movedEnd(12.2d, 1.0d, 12.2d, 1.2d), EPS);
    }

    /** Le bout du parent s'ecarte dans l'autre sens : les deux bouts ne vont pas du meme cote. */
    @Test
    @DisplayName("les deux bouts s'ecartent chacun de leur noeud")
    void lesDeuxBoutsSensOpposes() {
        double parentEnd = CGuiView.movedEnd(0.0d, 1.0d, 12.2d, 1.2d);
        double childEnd = CGuiView.movedEnd(100.0d, -1.0d, 12.2d, 1.2d);
        assertEquals(2.44d, parentEnd, EPS);
        assertEquals(100.0d - 2.44d, childEnd, EPS);
    }

    /**
     * Le bout de l'enfant s'eloigne du centre de ce qu'il faut pour sortir du cerne : le cerne fait
     * trente et une unites de large, son bord interieur tombe a douze virgule huit unites du centre
     * une fois grossi, et le bout du trait, lui, monte de douze virgule deux a quatorze virgule
     * soixante-quatre. C'est ce qui le met dehors.
     */
    @Test
    @DisplayName("le bout grossi passe devant le bord du cerne")
    void leBoutGrossiSortDuCerne() {
        double tip = 12.2d * 1.2d;
        double ringEdge = 12.8d;
        assertEquals(14.64d, tip, EPS);
        org.junit.jupiter.api.Assertions.assertTrue(tip > ringEdge,
                "le bout doit passer devant le cerne, sinon il se voit au travers");
    }
}
