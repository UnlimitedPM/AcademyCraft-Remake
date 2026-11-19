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
 * <p>Les nombres sont ceux de l'original, sauf la longueur du dessin : l'original peignait trente
 * blocs et frappait cinquante, et le joueur a trouve cet ecart illogique. Le dessin fait donc
 * maintenant toute la portee, et un test tient les deux nombres ensemble.
 *
 * <p>Et l'un des nombres de l'original surprend assez pour valoir un test a lui seul : ses deux bouts
 * ne prennent pas la meme origine, et le faisceau penche donc de plus d'un bloc sur sa longueur.
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

        // La pointe : les PIEDS, toute la portee plus loin. L'original prenait bien ses deux points
        // sur deux origines differentes.
        Vec3 to = MeltdownerVisuals.beamTo(feet, look, MeltdownerVisuals.BEAM_LENGTH);
        assertEquals(0, to.x, EPSILON);
        assertEquals(70, to.y, EPSILON, "aux pieds, et non aux yeux");
        assertEquals(MeltdownerVisuals.BEAM_LENGTH, to.z, EPSILON, "toute la portee plus loin");
    }

    @Test
    @DisplayName("le dessin fait la portee des degats : le faisceau ne ment plus")
    void leDessinFaitLaPorteeDesDegats() {
        // L'original dessinait trente blocs et frappait cinquante ; le port avait repris cet ecart, et
        // le joueur a trouve ca illogique — « c'est pas logique visuellement ». Les deux nombres sont
        // donc le MEME, et ce test les tient ensemble : toucher a l'un sans l'autre casserait cette
        // promesse-la, et personne ne s'en apercevrait avant de tirer dans le vide.
        assertEquals(38.0, MeltdownerVisuals.BEAM_LENGTH, EPSILON, "les trente-huit du joueur");
        assertEquals(MeltdownerSkill.LENGTH, MeltdownerVisuals.BEAM_LENGTH, EPSILON,
                "et le dessin fait exactement la portee du tir, lueur comprise : le port a essaye "
                        + "de la faire plus courte, et le joueur l'a refuse");
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
        assertEquals(MeltdownerVisuals.BEAM_LENGTH - 1.0, to.z - from.z, EPSILON,
                "toute la portee, moins le bloc dont le depart est avance");
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
    @DisplayName("le dezoom de la charge vaut la moitie de celui de l'orage")
    void leDezoomVautLaMoitieDeCeluiDeLOrage() {
        // L'orage en prend quarante degres a plein ; le meltdowner en prend vingt, a la demande
        // du joueur. C'est un NOMBRE DE DEGRES ajoute au champ, donc il se voit quel que soit le
        // reglage du joueur, et il grandit avec la charge — la seule chose qui reste ici est le
        // maximum, la ou la charge s'arrete de monter.
        assertEquals(20f, MeltdownerVisuals.FOV_DEGREES, 1e-6);
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
