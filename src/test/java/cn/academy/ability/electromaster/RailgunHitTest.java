package cn.academy.ability.electromaster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La zone du tir du railgun : son cylindre, et l'attenuation de ses degats.
 *
 * <p>C'est la partie du railgun qui se relit sans monde — la geometrie de {@link RailgunHit} — et
 * elle vaut la peine d'etre verrouillee : le port ne prenait qu'une cible a trente blocs, et le
 * joueur a trouve le trou tout seul, en jeu, avec un monstre a quarante-six.
 *
 * <p>Toutes les distances sont en blocs, et le tir part de l'origine vers le nord (+Z) : c'est le
 * repere le plus simple a lire.
 */
class RailgunHitTest {

    /** Le tir de reference : depuis l'origine, tout droit vers +Z. */
    private static final double DX = 0.0, DY = 0.0, DZ = 1.0;

    @Test
    @DisplayName("un monstre a quarante-six blocs est touche, et un a cinquante-et-un ne l'est pas")
    void laPorteeEstCelleDeLoriginal() {
        // C'est le cas que le joueur a rapporte : 46 blocs, tue dans le vrai mod.
        assertTrue(RailgunHit.hitDistance(0.0, 0.0, 46.0, DX, DY, DZ) >= 0.0,
                "46 blocs, bien dans l'axe : la cible doit etre touchee");
        assertTrue(RailgunHit.hitDistance(0.0, 0.0, 50.0, DX, DY, DZ) >= 0.0, "et a 50, la limite");
        assertEquals(-1.0, RailgunHit.hitDistance(0.0, 0.0, 50.1, DX, DY, DZ), 1e-9,
                "au-dela de cinquante, elle est hors de portee");

        // Derriere le tireur, non plus : un railgun ne tire pas en arriere.
        assertEquals(-1.0, RailgunHit.hitDistance(0.0, 0.0, -1.0, DX, DY, DZ), 1e-9,
                "derriere le canon, rien");
    }

    @Test
    @DisplayName("le cylindre fait deux blocs de rayon, elargi du cinquieme de l'original")
    void leRayonEstDeDeuxBlocs() {
        // Le rayon, elargi de 1,2 comme son filtre : deux blocs quatre.
        assertTrue(RailgunHit.hitDistance(2.3, 0.0, 10.0, DX, DY, DZ) >= 0.0,
                "a 2,3 blocs de l'axe, on est encore dans le faisceau");
        assertEquals(-1.0, RailgunHit.hitDistance(2.6, 0.0, 10.0, DX, DY, DZ), 1e-9,
                "a 2,6, on est passe a cote");
        // Et la tolerance vaut dans les deux sens de l'ecart.
        assertTrue(RailgunHit.hitDistance(0.0, -2.3, 10.0, DX, DY, DZ) >= 0.0, "vers le bas aussi");

        // La distance perpendiculaire est bien celle-la, et c'est elle qui attenue les degats.
        assertEquals(2.3, RailgunHit.hitDistance(2.3, 0.0, 10.0, DX, DY, DZ), 1e-9,
                "la distance rendue est la perpendiculaire");
    }

    @Test
    @DisplayName("les degats tombent de un a deux dixiemes sur les cinquante blocs")
    void lAttenuationTombeAvecLaDistance() {
        assertEquals(1.0, RailgunHit.damageFactor(0.0), 1e-9, "plein tarif dans l'axe");
        assertEquals(0.6, RailgunHit.damageFactor(25.0), 1e-9, "a mi-chemin, six dixiemes");
        assertEquals(0.2, RailgunHit.damageFactor(50.0), 1e-9, "et deux dixiemes au bout");

        // Plafonnee : c'est le `min(maxIncrement, ...)` de l'original, donc au-dela de la longueur
        // l'attenuation ne descend plus.
        assertEquals(0.2, RailgunHit.damageFactor(80.0), 1e-9, "et elle ne descend pas plus bas");

        // Ce sont les degats de l'original, mot pour mot.
        assertEquals(1.0, RailgunHit.FULL, 1e-9);
        assertEquals(0.2, RailgunHit.AT_END, 1e-9);
        assertEquals(50.0, RailgunHit.LENGTH, 1e-9);
        assertEquals(2.0, RailgunHit.RADIUS, 1e-9);
    }

    @Test
    @DisplayName("le rayon et la longueur se donnent : c'est le meltdowner qui les fait varier")
    void leRayonEtLaLongueurSeDonnent() {
        // Le meltdowner tire un faisceau plus LARGE que le railgun, et de deux a trois blocs selon
        // l'experience. La cible qui compte est celle qui rase le bord : a deux blocs et demi de
        // l'axe, elle est manquee par le railgun et touchee par le meltdowner au sommet.
        double perp = 2.5;
        assertEquals(-1.0, RailgunHit.hitDistance(perp, 0.0, 20.0, DX, DY, DZ), 1e-9,
                "a deux blocs et demi de l'axe, le railgun manque : son rayon vaut deux");
        assertTrue(RailgunHit.hitDistance(perp, 0.0, 20.0, DX, DY, DZ, 3.0, 50.0) >= 0.0,
                "mais le meltdowner au sommet l'attrape : son rayon vaut trois");
        assertEquals(perp, RailgunHit.hitDistance(perp, 0.0, 20.0, DX, DY, DZ, 3.0, 50.0), 1e-9,
                "et il rend la meme distance perpendiculaire, donc la meme attenuation");

        // La longueur, elle aussi, se donne : c'est ce qui separe le faisceau qu'on VOIT (trente
        // blocs, la longueur du dessin) de celui qui frappe (cinquante, le maxIncrement).
        assertEquals(-1.0, RailgunHit.hitDistance(0.0, 0.0, 35.0, DX, DY, DZ, 3.0, 30.0), 1e-9,
                "au-dela de la longueur donnee, la cible est hors de portee");
        assertTrue(RailgunHit.hitDistance(0.0, 0.0, 35.0, DX, DY, DZ, 3.0, 50.0) >= 0.0,
                "et dans les cinquante blocs de l'original, elle est dans le faisceau");

        // L'attenuation suit la longueur DONNEE, elle aussi : sans cela, un faisceau court ne
        // tomberait jamais a ses deux dixiemes.
        assertEquals(1.0, RailgunHit.damageFactor(0.0, 30.0), 1e-9, "plein tarif dans l'axe");
        assertEquals(0.2, RailgunHit.damageFactor(30.0, 30.0), 1e-9, "deux dixiemes au bout");
        assertEquals(0.2, RailgunHit.damageFactor(90.0, 30.0), 1e-9, "et cela ne descend pas plus bas");
    }
}
