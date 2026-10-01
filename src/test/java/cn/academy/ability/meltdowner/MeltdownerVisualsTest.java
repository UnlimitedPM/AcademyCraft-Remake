package cn.academy.ability.meltdowner;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le faisceau du meltdowner et l'essaim de sa charge.
 *
 * <p>Deux choses s'y figent, et deux seulement : <b>ou</b> le faisceau se pose, et ce que l'essaim
 * seme. Ni l'un ni l'autre ne se relit ailleurs — le faisceau se voit une seconde et demie en jeu,
 * et l'essaim est un nuage qu'aucun oeil ne compte.
 *
 * <p>Les nombres sont ceux de l'original, et l'un d'eux surprend assez pour valoir un test a lui
 * seul : ses deux bouts ne prennent pas la meme origine, et le faisceau penche donc de plus d'un
 * bloc sur sa longueur.
 */
class MeltdownerVisualsTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("le faisceau part des yeux et finit aux pieds")
    void leFaisceauPartDesYeuxEtFinitAuxPieds() {
        Vec3 eye = new Vec3(0, 71.62, 0);
        Vec3 feet = new Vec3(0, 70, 0);
        Vec3 look = new Vec3(0, 0, 1);

        // Le depart : les yeux, un bloc devant. Le regard de l'original est unitaire, donc c'est
        // bien un bloc qui est avance et non une distance tiree d'autre chose.
        Vec3 from = MeltdownerVisuals.beamFrom(eye, look);
        assertEquals(0, from.x, EPSILON);
        assertEquals(71.62, from.y, EPSILON, "a hauteur d'yeux");
        assertEquals(1.0, from.z, EPSILON, "et un bloc devant");

        // La pointe : les PIEDS, trente blocs plus loin. L'original prenait bien ses deux points
        // sur deux origines differentes.
        Vec3 to = MeltdownerVisuals.beamTo(feet, look, MeltdownerVisuals.BEAM_LENGTH);
        assertEquals(0, to.x, EPSILON);
        assertEquals(70, to.y, EPSILON, "aux pieds, et non aux yeux");
        assertEquals(30.0, to.z, EPSILON, "trente blocs plus loin");
    }

    @Test
    @DisplayName("un tir horizontal descend de 1,62 bloc sur sa longueur")
    void unTirHorizontalDescend() {
        // C'est la consequence des deux origines, et elle se voit : le faisceau part du visage et
        // arrive aux pieds, soit un peu plus de trois degres sous l'horizontale. Le port la
        // reprend telle quelle — c'est ce que le joueur a vu pendant des annees, et la redresser
        // serait une retouche, pas un portage.
        double eyeHeight = 1.62;
        Vec3 look = new Vec3(0, 0, 1);

        Vec3 from = MeltdownerVisuals.beamFrom(new Vec3(0, eyeHeight, 0), look);
        Vec3 to = MeltdownerVisuals.beamTo(Vec3.ZERO, look, MeltdownerVisuals.BEAM_LENGTH);

        assertEquals(eyeHeight, from.y - to.y, EPSILON, "toute la hauteur des yeux");
        assertEquals(29.0, to.z - from.z, EPSILON,
                "trente blocs, moins celui dont le depart est avance");
    }

    @Test
    @DisplayName("l'essaim seme trois grains par tick, et non deux")
    void lEssaimSemeTroisGrains() {
        // Le tirage de l'original rendait toujours deux : `rangei(2, 3)` vaut `2 + nextInt(1)`, sa
        // borne haute etant exclue — c'est deja fige pour les traits de la salve. Et sa boucle
        // descendait jusqu'a ZERO compris, soit un grain de plus : trois tours, sans hasard. En
        // semer deux se verrait tout de suite, l'essaim serait d'un tiers plus clair.
        var random = new Random(2718);

        for (int i = 0; i < 500; i++) {
            assertEquals(3, MeltdownerVisuals.swarmCount(random),
                    "le compte de l'original, toujours le meme");
        }

        assertEquals(2, MeltdownerVisuals.SWARM_BASE_MIN, "le tirage de l'original");
        assertEquals(3, MeltdownerVisuals.SWARM_BASE_MAX, "et sa borne haute, exclue");
    }

    @Test
    @DisplayName("un grain tourne autour du joueur et monte")
    void unGrainTourneEtMonte() {
        var random = new Random(7);

        for (int i = 0; i < 2000; i++) {
            double radius = MeltdownerVisuals.swarmRadius(random);
            double height = MeltdownerVisuals.swarmHeight(random);
            double angle = MeltdownerVisuals.swarmAngle(random);
            Vec3 offset = MeltdownerVisuals.swarmOffset(radius, angle, height);
            Vec3 velocity = MeltdownerVisuals.swarmVelocity(random);

            assertTrue(radius >= 0.7 && radius <= 1.0, "rayon hors bornes : " + radius);
            assertTrue(height >= -1.2 && height <= 0, "hauteur hors bornes : " + height);
            assertTrue(angle >= 0 && angle <= Math.PI * 2, "angle hors du tour : " + angle);

            // Le cercle est bien celui du tirage : sa distance horizontale est le rayon, et sa
            // hauteur est celle demandee.
            assertEquals(radius, Math.sqrt(offset.x * offset.x + offset.z * offset.z), 1e-9,
                    "le grain est sur son cercle");
            assertEquals(height, offset.y, 1e-9, "et a sa hauteur");

            // Il monte toujours : c'est ce qui fait que l'essaim s'eleve au lieu de tourner a plat.
            assertTrue(velocity.y >= 0.01 && velocity.y <= 0.05, "il monte : " + velocity);
            assertTrue(Math.abs(velocity.x) <= 0.03, "de biais, au plus : " + velocity);
            assertTrue(Math.abs(velocity.z) <= 0.03, "de biais, au plus : " + velocity);
        }
    }
}
