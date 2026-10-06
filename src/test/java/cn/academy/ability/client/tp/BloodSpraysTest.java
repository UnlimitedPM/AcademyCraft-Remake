package cn.academy.ability.client.tp;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les taches de sang du retour de sang.
 *
 * <p>Ce qui se relit sans Minecraft, c'est la pose : la taille selon la face, l'echange des deux
 * familles d'images, et le plan de la face. Le reste — le rayon des neuf directions, la duree de
 * vie — se voit en jeu.
 */
class BloodSpraysTest {

    @Test
    @DisplayName("une paroi prend une tache plus petite que le sol")
    void uneParoiEstPlusPetite() {
        // La taille se tire au hasard : les deux tirages sont donc faits avec la MEME graine, et
        // seule la face change. Le sol garde ce qu'il a tire, une paroi en prend huit dixiemes.
        double sol = BloodSprays.size(RandomSource.create(1234L), Direction.UP);
        double paroi = BloodSprays.size(RandomSource.create(1234L), Direction.NORTH);

        assertTrue(sol >= BloodSprays.SIZE_MIN && sol <= BloodSprays.SIZE_MAX,
                "le sol reste dans les bornes de l'original : " + sol);
        assertEquals(BloodSprays.WALL_SHRINK, paroi / sol, 1e-9,
                "et une paroi vaut huit dixiemes du meme tirage");
    }

    @Test
    @DisplayName("le sol et le plafond sont horizontaux, les quatre parois non")
    void lesFacesHorizontales() {
        assertTrue(BloodSprays.floor(Direction.UP), "le sol");
        assertTrue(BloodSprays.floor(Direction.DOWN), "et le plafond");
        assertFalse(BloodSprays.floor(Direction.NORTH), "une paroi ne l'est pas");
        assertFalse(BloodSprays.floor(Direction.EAST), "aucune des quatre ne l'est");
    }

    @Test
    @DisplayName("le plan d'une face suit la face, et ses deux axes sont perpendiculaires")
    void lePlanSuitLaFace() {
        Vec3 u = BloodSprays.inPlane(Direction.UP, 1, 0);
        Vec3 v = BloodSprays.inPlane(Direction.UP, 0, 1);

        assertEquals(0.0, u.dot(new Vec3(0, 1, 0)), 1e-9, "aucun axe ne sort du plan du sol");
        assertEquals(0.0, v.dot(new Vec3(0, 1, 0)), 1e-9, "ni l'autre");
        assertEquals(0.0, u.dot(v), 1e-9, "et ils se coupent a angle droit");

        Vec3 centre = BloodSprays.inPlane(Direction.NORTH, 0, 0);
        assertEquals(0.0, centre.length(), 1e-9, "et le zero du plan reste le centre de la face");
    }

    @Test
    @DisplayName("une tache s'en va au bout d'une minute, pas avant")
    void uneTacheVieUneMinute() {
        BloodSprays.Spray spray = new BloodSprays.Spray(null, Vec3.ZERO, Direction.UP, 1.2, 0f, 0, 0);

        assertTrue(spray.alive(), "elle nait vivante");
        assertTrue(spray.aged().alive(), "et vit au tick suivant");
        BloodSprays.Spray vieille = new BloodSprays.Spray(null, Vec3.ZERO, Direction.UP, 1.2, 0f, 0,
                BloodSprays.LIFE_TICKS);
        assertFalse(vieille.alive(), "et s'en va a la minute pile");
    }

    /**
     * L'ombre d'une tache : la lumiere de l'original venait de GL, et elle tombait sur la normale
     * de la face.
     *
     * <p>C'est ce qui decide si le sang parait rouge vif ou sombre, et le port le dessinait au
     * plein : une tache vue de biais sortait delavee. Le joueur a vu la difference — « les taches
     * de sang ne sont pas assez sombres par rapport au vrai mod ».
     */
    @Test
    @DisplayName("une tache est d'autant plus sombre que sa face se detourne du regard")
    void lOmbreSuitLeRegard() {
        Vec3 versLeBas = new Vec3(0, -1, 0);
        Vec3 versLeNord = new Vec3(0, 0, -1);

        // Le sol sous nos pieds, vu droit dessus : le regard tombe dessus, pleine lumiere.
        assertEquals(BloodSprays.SHADE_MAX, BloodSprays.shade(Direction.UP, versLeBas), 1e-6f,
                "le sol regarde l'oeil quand on le regarde");
        // Et le meme sol, vu de l'horizon : il se detourne, et tombe au plancher.
        assertEquals(BloodSprays.SHADE_MIN, BloodSprays.shade(Direction.UP, versLeNord), 1e-6f,
                "vu de l'horizon, il ne prend plus rien");
        // Une paroi en face de soi, elle, est toujours en pleine lumiere : son regard a le regard.
        assertEquals(BloodSprays.SHADE_MAX, BloodSprays.shade(Direction.SOUTH, versLeNord), 1e-6f,
                "un mur qu'on regarde est en pleine lumiere");
        assertEquals(BloodSprays.SHADE_MIN, BloodSprays.shade(Direction.NORTH, versLeNord), 1e-6f,
                "et celui qu'on tourne, non");
        // Le plafond, lui, est detourne des qu'on regarde devant soi.
        assertEquals(BloodSprays.SHADE_MIN, BloodSprays.shade(Direction.DOWN, versLeNord), 1e-6f,
                "et le plafond est sombre des qu'on baisse le nez");

        // Entre les deux, l'ombre monte avec le regard, sans jamais sortir des bornes.
        Vec3 deBiais = new Vec3(0, -0.5, -0.866).normalize();
        float moyenne = BloodSprays.shade(Direction.UP, deBiais);
        assertTrue(moyenne > BloodSprays.SHADE_MIN && moyenne < BloodSprays.SHADE_MAX,
                "un sol vu de biais prend une part de la lumiere : " + moyenne);
        for (Direction face : Direction.values()) {
            float ombre = BloodSprays.shade(face, deBiais);
            assertTrue(ombre >= BloodSprays.SHADE_MIN && ombre <= BloodSprays.SHADE_MAX,
                    "et jamais hors des bornes : " + face + " en " + ombre);
        }
    }
}
