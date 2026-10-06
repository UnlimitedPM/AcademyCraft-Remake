package cn.academy.ability.vecmanip;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La poussee de l'acceleration de vecteur : sa direction, et sa vitesse.
 *
 * <p>Ces deux nombres sont purs, donc verifiables — et ils doivent l'etre, parce que le <b>client
 * et le serveur les calculent tous les deux</b>. Le client pose la poussee au relachement (comme
 * l'original), le serveur la pose aussi pour les autres joueurs : une divergence entre les deux se
 * verrait comme un rebond.
 */
class VecAccelSkillTest {

    @Test
    @DisplayName("la poussee part du regard, et dix degres plus haut")
    void laPousseePartDuRegard() {
        // Regard vers +Z, a plat : la poussee monte donc de dix degres, et pas d'un seul.
        Vec3 droit = VecAccelSkill.boostDirection(0, 0);

        assertEquals(1.0, droit.length(), 1e-9, "une direction est unitaire");
        assertEquals(0.0, droit.x, 1e-9, "sans lacet, rien de travers");
        assertTrue(droit.y > 0, "et elle monte : le regard a ete incline de dix degres");
        assertEquals(Math.sin(Math.toRadians(10)), droit.y, 1e-9);
        assertEquals(Math.cos(Math.toRadians(10)), droit.z, 1e-9, "devant, et un peu en l'air");

        // Regard vers le bas de trente degres : la poussee ne plonge que de vingt.
        Vec3 pique = VecAccelSkill.boostDirection(30, 0);
        assertEquals(Math.sin(Math.toRadians(-20)), pique.y, 1e-9, "dix degres sont rendus");
        assertTrue(pique.y < 0, "elle plonge, mais moins que le regard");

        // Et le lacet tourne la poussee avec le regard : +X a quatre-vingt-dix degres. Le cote
        // vaut un peu moins que un, et c'est normal : la poussee monte de dix degres, donc ce
        // qu'elle gagne en hauteur, elle le prend sur l'horizontale.
        Vec3 cote = VecAccelSkill.boostDirection(0, 90);
        assertEquals(-Math.cos(Math.toRadians(10)), cote.x, 1e-9,
                "lacet quatre-vingt-dix : la poussee part vers -X");
        assertEquals(0.0, cote.z, 1e-9);
        assertEquals(droit.y, cote.y, 1e-9, "et elle monte toujours d'autant");
    }

    @Test
    @DisplayName("la poussee suit la charge, et plafonne avec elle")
    void laPousseeSuitLaCharge() {
        Vec3 breve = VecAccelSkill.boost(0, 0, 0);
        Vec3 pleine = VecAccelSkill.boost(0, 0, VecAccelSkill.MAX_CHARGE);

        assertEquals(VecAccelSkill.speedAt(0), breve.length(), 1e-9,
                "un appui bref pousse a la vitesse du minimum de charge");
        assertEquals(VecAccelSkill.speedAt(VecAccelSkill.MAX_CHARGE), pleine.length(), 1e-9,
                "et une charge pleine a la sienne");
        assertTrue(pleine.length() > breve.length(), "plus on charge, plus ca pousse");
        assertEquals(VecAccelSkill.MAX_VELOCITY * Math.sin(1.0), pleine.length(), 1e-9,
                "la charge pleine vaut sin(1) fois la vitesse maximale");

        // Et la direction ne depend pas de la charge : c'est le regard qui la donne, point.
        assertEquals(breve.normalize().x, pleine.normalize().x, 1e-9);
        assertEquals(breve.normalize().z, pleine.normalize().z, 1e-9);
    }
}
