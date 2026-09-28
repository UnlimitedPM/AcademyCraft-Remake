package cn.academy.ability.client.arc;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ou part un eclair.
 *
 * <p>C'est le seul ecart assume avec l'original de tout le moteur d'arcs : il posait son arc
 * aux yeux, donc au milieu de l'ecran. Le sens du decalage — quel cote est la main droite —
 * est une erreur de signe invisible en test comme en jeu, sauf qu'elle mettrait l'eclair dans
 * la mauvaise main : elle est donc verifiee ici, en clair.
 */
class ArcOriginsTest {

    private static final Vec3 EYE = new Vec3(100, 70, 200);

    @Test
    @DisplayName("regard vers le sud : la main est a l'ouest, sous les yeux, devant")
    void regardVersLeSud() {
        // Vers le sud, c'est-a-dire vers +Z. Le joueur regarde alors vers +Z, et sa main
        // droite est vers -X : c'est la convention de Minecraft (X vers l'est, Z vers le
        // sud), et l'inverse se verrait tout de suite a l'ecran.
        Vec3 hand = ArcOrigins.hand(EYE, new Vec3(0, 0, 1));

        assertEquals(EYE.x - ArcOrigins.HAND_RIGHT, hand.x, 1e-9, "la main est a l'ouest");
        assertEquals(EYE.y - ArcOrigins.HAND_DOWN, hand.y, 1e-9, "et un peu plus bas que les yeux");
        assertEquals(EYE.z + ArcOrigins.HAND_FORWARD, hand.z, 1e-9, "et devant le visage");
    }

    @Test
    @DisplayName("regard vers le nord : la main passe de l'autre cote")
    void regardVersLeNord() {
        Vec3 hand = ArcOrigins.hand(EYE, new Vec3(0, 0, -1));

        assertEquals(EYE.x + ArcOrigins.HAND_RIGHT, hand.x, 1e-9, "la main est a l'est");
        assertEquals(EYE.z - ArcOrigins.HAND_FORWARD, hand.z, 1e-9, "et devant, vers -Z");
    }

    @Test
    @DisplayName("l'arc ne part plus de la camera")
    void lArcNePartPlusDeLaCamera() {
        // C'est tout l'objet de cette classe, et c'est ce que le joueur a demande : un
        // depart visible, decale du cote du bras, et non le milieu de l'ecran.
        Vec3 hand = ArcOrigins.hand(EYE, new Vec3(0, 0, 1));

        assertTrue(hand.distanceTo(EYE) > 0.3, "le depart est franchement hors des yeux");
        assertTrue(hand.y < EYE.y, "et sous la ligne des yeux");
    }

    @Test
    @DisplayName("regarder le sol ou le ciel ne fait pas disparaitre la main")
    void regardVertical() {
        // Le cote se calcule sur la direction horizontale du regard, donc un regard droit
        // au sol le laisse indefini : il faut une direction de repli, et surtout un point
        // de depart qui reste devant le joueur.
        Vec3 down = ArcOrigins.hand(EYE, new Vec3(0, -1, 0));
        Vec3 up = ArcOrigins.hand(EYE, new Vec3(0, 1, 0));

        for (Vec3 hand : new Vec3[] { down, up }) {
            assertTrue(Double.isFinite(hand.x) && Double.isFinite(hand.y) && Double.isFinite(hand.z),
                    "aucune coordonnee ne doit devenir infinie ni nulle part");
            assertTrue(hand.distanceTo(EYE) < 1.2, "et la main reste a portee de main");
        }
        // La main est SOUS les yeux dans les deux cas : le decalage vers le bas l'emporte
        // sur l'avance, sinon le bras se tiendrait au-dessus du visage.
        assertTrue(down.y < EYE.y, "regarder le sol pose la main plus bas");
        assertTrue(up.y < EYE.y, "et regarder le ciel la garde sous les yeux");
        assertTrue(down.y < up.y, "le regard au sol la pose plus bas que le regard au ciel");
    }

    @Test
    @DisplayName("la main se tient sous l'epaule, pas a sa hauteur")
    void laMainEstSousLEpaule() {
        // C'est ce que le joueur a vu de travers en F5 : le depart etait a 1,40 bloc, soit
        // la hauteur de l'epaule (le modele l'attache a 1,375), et l'eclair semblait donc
        // flotter a cote du joueur au lieu de sortir de sa main. Le modele donne la main
        // tendue vers 1,05 bloc : le depart doit se tenir entre les deux.
        //
        // Les yeux sont a 1,62 bloc au-dessus des pieds, sur un joueur d'1,8 bloc.
        double hauteurMain = 1.62 - ArcOrigins.HAND_DOWN;

        assertTrue(hauteurMain < 1.2, "sous les 1,2 bloc d'une main tendue : " + hauteurMain);
        assertTrue(hauteurMain > 0.9, "mais au-dessus de la taille, pas aux genoux : " + hauteurMain);
        assertEquals(0.31, ArcOrigins.HAND_RIGHT, 1e-9,
                "le cote est celui du modele : 0,3125 bloc du corps");
    }

    @Test
    @DisplayName("un regard en biais suit le cote du joueur, pas l'axe du monde")
    void regardEnBiais() {
        // Vers l'est (+X) : la main droite est alors vers +Z (le sud).
        Vec3 hand = ArcOrigins.hand(EYE, new Vec3(1, 0, 0));

        assertEquals(EYE.z + ArcOrigins.HAND_RIGHT, hand.z, 1e-9);
        assertEquals(EYE.x + ArcOrigins.HAND_FORWARD, hand.x, 1e-9);
    }
}
