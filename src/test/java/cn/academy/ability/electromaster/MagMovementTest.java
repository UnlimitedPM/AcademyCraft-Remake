package cn.academy.ability.electromaster;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La traction de {@code mag_movement}.
 *
 * Le portage de {@code tryAdjust} et de la vitesse visee : c'est ce qui distingue une
 * traction d'un coup de vent, et c'est verifiable sans lancer un jeu — les deux methodes
 * ne prennent que des nombres.
 */
class MagMovementTest {

    @Test
    void laVitesseViseeVaVersLAncreALaVitesseVoulue() {
        Vec3 wanted = MagMovementSkill.wantedVelocity(new Vec3(0, 0, 0), new Vec3(10, 0, 0));

        assertEquals(1.0, wanted.length(), 0.0001, "la norme est celle de l'original");
        assertEquals(1.0, wanted.x, 0.0001);
        assertEquals(0.0, wanted.y, 0.0001);
        assertEquals(0.0, wanted.z, 0.0001);
    }

    @Test
    void laVitesseViseeTientCompteDesTroisAxes() {
        // Une ancre en diagonale : la traction monte autant qu'elle avance.
        Vec3 wanted = MagMovementSkill.wantedVelocity(new Vec3(0, 0, 0), new Vec3(3, 4, 0));

        assertEquals(1.0, wanted.length(), 0.0001);
        assertEquals(0.6, wanted.x, 0.0001);
        assertEquals(0.8, wanted.y, 0.0001);
    }

    @Test
    void surLAncreIlNyAPlusRienAViser() {
        Vec3 position = new Vec3(1, 2, 3);

        assertNull(MagMovementSkill.wantedVelocity(position, position),
                "une direction nulle n'a pas de sens");
    }

    @Test
    void laVitesseSeRapprocheDUnePas() {
        // Un pas de 0,16 — deux fois ACCEL, comme l'original qui rapprochait sa vitesse deux
        // fois par tick : la vitesse voulue est atteinte en une demi-seconde.
        assertEquals(0.16, MagMovementSkill.approach(0, 1), 0.0001);
        assertEquals(0.32, MagMovementSkill.approach(0.16, 1), 0.0001);
        assertEquals(-0.16, MagMovementSkill.approach(0, -1), 0.0001);
    }

    @Test
    void laVitesseNEstPasDepassee() {
        // A moins d'un pas de la valeur voulue, on la prend telle quelle : sans cela la
        // traction oscillerait autour de sa cible sans jamais l'atteindre.
        assertEquals(1.0, MagMovementSkill.approach(0.95, 1), 0.0001);
        assertEquals(-1.0, MagMovementSkill.approach(-0.95, -1), 0.0001);
    }

    @Test
    void lExperienceSuitLaDistanceParcourue() {
        // 0,0011 par bloc, avec un plancher de 0,005 : un trajet trop court ne rapporte
        // presque rien, mais rapporte quand meme.
        assertEquals(0.005f, MagMovementSkill.getExpIncr(0), 0.00001f);
        assertEquals(0.005f, MagMovementSkill.getExpIncr(2), 0.00001f);
        assertEquals(0.011f, MagMovementSkill.getExpIncr(10), 0.00001f);
        assertEquals(0.0275f, MagMovementSkill.getExpIncr(25), 0.00001f);
    }

    @Test
    void laMonteeNeFreinePas() {
        // L'approche de l'original freine : arrive sous le bloc, la traction retirait la
        // vitesse qui allait le depasser, et on s'arretait a sa hauteur — « je ne peux jamais
        // depasser cette hauteur ».
        assertEquals(0.84, MagMovementSkill.approach(1.0, 0.7), 0.0001);
        // La montee, elle, laisse aller.
        assertEquals(1.0, MagMovementSkill.lift(1.0, 0.7), 0.0001);
        assertEquals(0.7, MagMovementSkill.lift(0.7, 0.7), 0.0001, "a la vitesse voulue, on la garde");
    }

    @Test
    void laMonteeSurvitALaGraviteDuTickSuivant() {
        // La traction est posee en fin de tick : la gravite du tick qui vient n'a pas encore ete
        // retiree. Au repos, la composante posee doit donc depasser la gravite — sans quoi le
        // joueur ne montait JAMAIS, ce qui est exactement le defaut signale.
        double montee = MagMovementSkill.pulledVertical(-0.08, 1.0, false);
        assertTrue(montee > MagMovementSkill.GRAVITY, "la montee se voit : " + montee);
        assertTrue(montee - MagMovementSkill.GRAVITY > 0, "et il reste une vitesse ascendante");

        // En vol, le jeu ne lui retire rien : la traction ne compense donc rien non plus.
        assertEquals(montee - MagMovementSkill.GRAVITY,
                MagMovementSkill.pulledVertical(-0.08, 1.0, true), 1.0e-9,
                "rien a compenser en vol");
    }

    @Test
    void laMonteePousseAuMemePasQueLOriginal() {
        // Le depart ne change pas : sous la valeur voulue, la montee avance d'un pas par
        // tick et ne la depasse pas.
        assertEquals(0.16, MagMovementSkill.lift(0, 0.7), 0.0001);
        assertEquals(0.7, MagMovementSkill.lift(0.62, 0.7), 0.0001);
    }

    @Test
    void unNouveauBlocVautUnDixiemeDePourcent() {
        // Mille fois moins que le plancher du premier bloc : une lignee ne se monnaie pas
        // en trajets. Aucun surcout ne s'y ajoute — le joueur a retire cette regle.
        assertEquals(0.001f, MagMovementSkill.EXP_PER_NEW_BLOCK, 0.000001f);
        assertEquals(5f, MagMovementSkill.getExpIncr(0) / MagMovementSkill.EXP_PER_NEW_BLOCK,
                0.0001f, "le premier bloc vaut cinq nouveaux blocs");
    }
}
