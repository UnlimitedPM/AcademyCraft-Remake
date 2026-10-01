package cn.academy.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les boucles sonores : qui en a une, laquelle, et a quel volume.
 *
 * <p>Le choix se fait sur le nom de la competence, sans registre ni Minecraft — c'est ce
 * qui permet de le relire ici, alors que le son lui-meme ne s'entend qu'en jeu. Une faute
 * dans un nom d'evenement ne ferait rien tomber en jeu : la boucle tournerait simplement
 * dans un silence total.
 */
class HeldLoopsTest {

    /** Les competences de l'original qui font tourner un son tant qu'on tient la touche. */
    private static final int LOOPS = 10;

    @Test
    void lesCompetencesALBoucleSontCellesDeLOriginal() {
        assertEquals(LOOPS, HeldLoops.loopingSkills().size());
        assertEquals(LOOPS, HeldLoops.loopingSkills().stream()
                .filter(s -> HeldLoops.forSkill(s) != null).count(), "toutes doivent rendre une boucle");

        // Les huit competences d'origine, et les trois rayons miniers qui partagent la leur.
        for (String skill : new String[] { "light_shield", "body_intensify", "mag_movement",
                "mag_manip", "storm_wing", "meltdowner", "charging", "mine_ray_basic",
                "mine_ray_expert", "mine_ray_luck" }) {
            assertNotNull(HeldLoops.forSkill(skill), "boucle attendue : " + skill);
        }

        assertNull(HeldLoops.forSkill("arc_gen"), "un arc ne tient pas de son");
        assertNull(HeldLoops.forSkill(null), "rien n'est tenu, rien ne tourne");
        assertNull(HeldLoops.forSkill("une_competence_qui_n_existe_pas"));
    }

    @Test
    void leMeltdownerFaitTournerSonSonDeCharge() {
        // Il ne se tient pas, il se CHARGE : l'original faisait suivre le joueur a son son de
        // charge tant que la touche restait enfoncee, et le port le retrouve donc sous le nom de
        // la competence, comme les autres maintiens.
        HeldLoops.Loop meltdowner = HeldLoops.forSkill("meltdowner");

        assertEquals("md.md_charge", meltdowner.event());
        assertEquals(1.0f, meltdowner.volume(), "l'original le mettait a plein");
        assertFalse(meltdowner.hasStartup(), "la charge est son propre son de mise en route");
    }

    @Test
    void lesTroisRayonsMiniersPartagentLaMemeBoucle() {
        // L'original n'avait qu'un `md.mine_loop` pour les trois : ce sont leurs sons de
        // MISE EN ROUTE qui diffèrent, pas leur entretien.
        HeldLoops.Loop basic = HeldLoops.forSkill("mine_ray_basic");
        HeldLoops.Loop expert = HeldLoops.forSkill("mine_ray_expert");
        HeldLoops.Loop luck = HeldLoops.forSkill("mine_ray_luck");

        assertEquals(basic.event(), expert.event());
        assertEquals(expert.event(), luck.event());
        assertEquals("md.mine_loop", basic.event());
    }

    @Test
    void leBouclierEstLeSeulAAnnoncerSaMiseEnRoute() {
        HeldLoops.Loop shield = HeldLoops.forSkill("light_shield");

        assertTrue(shield.hasStartup(), "l'original jouait un son d'ouverture");
        assertEquals("md.shield_startup", shield.startup());

        for (String skill : HeldLoops.loopingSkills()) {
            if (skill.equals("light_shield")) continue;
            assertFalse(HeldLoops.forSkill(skill).hasStartup(),
                    skill + " n'a pas de son de mise en route");
        }
    }

    @Test
    void lesVolumesSontCeuxDeLOriginal() {
        // L'original n'en donnait que deux : le plein pour les effets, et 0,3 pour le
        // branchement et les rayons miniers, qui sont des sons d'entretien.
        assertEquals(1.0f, HeldLoops.forSkill("light_shield").volume());
        assertEquals(1.0f, HeldLoops.forSkill("storm_wing").volume());
        assertEquals(1.0f, HeldLoops.forSkill("meltdowner").volume());
        assertEquals(HeldLoops.QUIET, HeldLoops.forSkill("charging").volume());
        assertEquals(HeldLoops.QUIET, HeldLoops.forSkill("mine_ray_luck").volume());
    }

    @Test
    void chaqueBoucleExisteDansLeFichierDesSons() {
        Set<String> declared = new HashSet<>();
        try (InputStream in = HeldLoopsTest.class.getResourceAsStream("/assets/academy/sounds.json")) {
            assertNotNull(in, "le fichier des sons doit etre livre");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (var entry : json.entrySet()) {
                    declared.add(entry.getKey());
                }
            }
        } catch (Exception e) {
            throw new AssertionError("le fichier des sons doit se lire", e);
        }

        for (String skill : HeldLoops.loopingSkills()) {
            HeldLoops.Loop loop = HeldLoops.forSkill(skill);
            assertTrue(declared.contains(loop.event()),
                    "evenement inconnu pour " + skill + " : " + loop.event());
            if (loop.hasStartup()) {
                assertTrue(declared.contains(loop.startup()),
                        "evenement inconnu pour " + skill + " : " + loop.startup());
            }
        }
    }
}
