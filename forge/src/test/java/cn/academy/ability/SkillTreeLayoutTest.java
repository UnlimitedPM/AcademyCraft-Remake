package cn.academy.ability;

import cn.academy.client.gui.cgui.CGuiDocument;
import cn.academy.client.gui.cgui.CWidget;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires de la figure de l'arbre des competences.
 *
 * <h2>Pourquoi figer des coordonnees par un test</h2>
 *
 * <p>Ces nombres ne sont calcules par personne : l'auteur de l'original les avait regles a la main,
 * un par un, dans un editeur de position, puis recopies dans la classe de sa categorie. Le port les
 * reprend tels quels — et c'est exactement ce qui les rend fragiles : rien dans le code ne dit
 * pourquoi {@code mine_detect} est a {@code 225, 12} plutot qu'ailleurs, donc rien ne s'opposerait a
 * ce qu'une reecriture les deplace sans le dire. Le test est cette memoire-la.
 *
 * <h2>Ce que le test ne fige pas</h2>
 *
 * <p>Il ne juge pas la beaute de la figure : il verifie qu'elle est celle de l'original, que rien
 * n'est reste au coin, que deux noeuds ne se recouvrent pas, et que tout tient dans la zone — quatre
 * proprietes qu'un deplacement malheureux casserait tout de suite.
 */
class SkillTreeLayoutTest {

    /**
     * Le diametre dessine d'un noeud, en unites.
     *
     * <p>La boite d'une competence fait seize unites, mais le cercle qui la represente en fait
     * vingt-trois et deborde de trois et demie de chaque cote (voir {@link Skill#setPosition}). Deux
     * noeuds plus proches que cela se recouvrent a l'ecran, meme si leurs boites ne se touchent pas.
     */
    private static final double NODE_DIAMETER = 23.0;

    /** Le rayon du meme disque : c'est lui qui doit tenir dans la zone de l'arbre. */
    private static final double NODE_RADIUS = NODE_DIAMETER / 2;

    /**
     * La boite d'une competence, en unites : c'est elle que {@code setPosition} place.
     *
     * <p>Le disque est centre sur le milieu de cette boite, donc huit unites plus loin que le point
     * du fichier. C'est ce centre-la qui doit tenir dans la zone.
     */
    private static final double NODE_BOX = 16.0;

    private static List<Category> categories() {
        return List.of(
                cn.academy.ability.electromaster.ElectromasterCategory.INSTANCE,
                cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE,
                cn.academy.ability.teleporter.TeleporterCategory.INSTANCE,
                cn.academy.ability.vecmanip.VecmanipCategory.INSTANCE);
    }

    /**
     * La figure d'une categorie, sous une forme qui se lit d'un coup d'oeil et se compare telle
     * quelle : {@code "arc_gen=24,46 charging=55,18 ..."}, dans l'ordre d'enregistrement.
     */
    private static String figure(Category category) {
        StringBuilder out = new StringBuilder();
        for (Skill skill : category.getSkills()) {
            if (out.length() > 0) out.append(' ');
            out.append(skill.getName())
                    .append('=')
                    .append((int) skill.getGuiX())
                    .append(',')
                    .append((int) skill.getGuiY());
        }
        return out.toString();
    }

    /** La zone ou les competences se dessinent, lue dans le vrai document. */
    private static CWidget area() {
        CWidget main = CGuiDocument.read("rework/page_developer");
        CWidget area = main.child("parent_right") == null
                ? null : main.child("parent_right").child("area");
        assertNotNull(area, "le document de la Developer doit porter la zone de l'arbre");
        // La zone ne se met pas a l'echelle de son contenu : ses 257 sur 139 unites sont donc
        // exactement les unites des coordonnees de l'arbre. Si un jour elle en changeait, les
        // chiffres figes plus bas ne voudraient plus dire la meme chose — d'ou ce garde-fou.
        assertEquals(1.0, area.transform.scale, "les coordonnees de l'arbre sont en unites de la zone");
        return area;
    }

    @Test
    @DisplayName("la figure de chaque categorie est celle de l'original, au chiffre pres")
    void figuresAreTheOriginals() {
        // Les coordonnees de CatElectromaster, CatMeltdowner, CatTeleporter et CatVecManip de la
        // 1.12.2, dans l'ordre ou le port enregistre ses competences. Les trois derniers de chaque
        // ligne sont les cursus generiques, poses une fois pour toutes a la meme hauteur.
        String courses = " brain_course=30,110 brain_course_advanced=115,110 mind_course=205,110";

        assertEquals("arc_gen=24,46 charging=55,18 mag_movement=137,35 mag_manip=204,33"
                        + " mine_detect=225,12 body_intensify=97,15 thunder_bolt=86,67"
                        + " railgun=164,59 thunder_clap=204,80" + courses,
                figure(cn.academy.ability.electromaster.ElectromasterCategory.INSTANCE));

        assertEquals("electron_bomb=15,45 rad_intensify=35,75 scatter_bomb=70,50"
                        + " light_shield=55,15 meltdowner=115,40 mine_ray_basic=140,70"
                        + " ray_barrage=140,10 jet_engine=170,32 mine_ray_expert=172,70"
                        + " mine_ray_luck=205,82 electron_missile=210,35" + courses,
                figure(cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE));

        assertEquals("threatening_teleport=14,42 dim_folding_theorem=50,75"
                        + " penetrate_teleport=60,46 mark_teleport=70,16 flesh_ripping=130,12"
                        + " location_teleport=118,50 shift_tp=175,47 space_fluct=160,80"
                        + " flashing=220,20" + courses,
                figure(cn.academy.ability.teleporter.TeleporterCategory.INSTANCE));

        assertEquals("dir_shock=16,45 ground_shock=64,85 vec_accel=76,40 vec_deviation=145,53"
                        + " dir_blast=136,80 storm_wing=130,20 blood_retro=204,83"
                        + " vec_reflection=210,50 plasma_cannon=175,14" + courses,
                figure(cn.academy.ability.vecmanip.VecmanipCategory.INSTANCE));
    }

    @Test
    @DisplayName("aucune competence ne reste au coin de l'arbre")
    void noSkillLeftAtTheOrigin() {
        // Le coin, c'est le point par defaut d'un Skill. Une competence ajoutee sans position s'y
        // pose, et elle s'y superpose alors a la racine de sa categorie : deux cercles l'un sur
        // l'autre, une competence qu'on ne voit plus et qu'on ne peut plus cliquer. C'est le risque
        // exact que ce test surveille.
        for (Category category : categories()) {
            for (Skill skill : category.getSkills()) {
                assertTrue(skill.getGuiX() != 0 || skill.getGuiY() != 0,
                        category.getName() + "." + skill.getName() + " est reste au coin");
            }
        }
    }

    @Test
    @DisplayName("deux noeuds d'une meme categorie ne se recouvrent jamais")
    void nodesNeverOverlap() {
        for (Category category : categories()) {
            List<Skill> skills = category.getSkills();
            for (int i = 0; i < skills.size(); i++) {
                for (int j = i + 1; j < skills.size(); j++) {
                    Skill a = skills.get(i);
                    Skill b = skills.get(j);
                    double dx = a.getGuiX() - b.getGuiX();
                    double dy = a.getGuiY() - b.getGuiY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    assertTrue(distance >= NODE_DIAMETER, category.getName() + " : " + a.getName()
                            + " et " + b.getName() + " ne sont qu'a " + Math.round(distance)
                            + " unites, et un noeud en fait " + (int) NODE_DIAMETER);
                }
            }
        }
    }

    @Test
    @DisplayName("tous les noeuds tiennent dans la zone de l'arbre")
    void nodesFitInTheArea() {
        // Le disque d'un noeud est centre sur le milieu de sa boite : c'est ce centre, et non le
        // coin donne par setPosition, qui doit laisser son rayon entre les bords.
        CWidget area = area();
        double width = area.transform.width;
        double height = area.transform.height;

        for (Category category : categories()) {
            for (Skill skill : category.getSkills()) {
                double centerX = skill.getGuiX() + NODE_BOX / 2;
                double centerY = skill.getGuiY() + NODE_BOX / 2;
                assertTrue(centerX - NODE_RADIUS >= 0 && centerX + NODE_RADIUS <= width,
                        category.getName() + "." + skill.getName() + " sort de la zone en largeur");
                assertTrue(centerY - NODE_RADIUS >= 0 && centerY + NODE_RADIUS <= height,
                        category.getName() + "." + skill.getName() + " sort de la zone en hauteur");
            }
        }
    }

    @Test
    @DisplayName("les trois cursus generiques sont a la meme place dans les quatre categories")
    void genericCoursesShareTheSameRow() {
        // Ils sont construits a neuf par categorie (une competence n'appartient qu'a une seule), donc
        // rien n'obligerait leurs trois positions a rester les memes — alors que la figure, elle, doit
        // l'etre : c'est le meme cours, propose dans les quatre pouvoirs.
        List<String> figures = new ArrayList<>();
        for (Category category : categories()) {
            StringBuilder row = new StringBuilder();
            for (String name : List.of("brain_course", "brain_course_advanced", "mind_course")) {
                Skill skill = category.getSkill(name);
                assertNotNull(skill, category.getName() + " n'a pas " + name);
                if (row.length() > 0) row.append(' ');
                row.append(name)
                        .append('=')
                        .append((int) skill.getGuiX())
                        .append(',')
                        .append((int) skill.getGuiY());
            }
            figures.add(row.toString());
        }

        for (String figure : figures) {
            assertEquals("brain_course=30,110 brain_course_advanced=115,110"
                    + " mind_course=205,110", figure);
        }
    }

    /**
     * La parente d'une categorie, sous la meme forme lisible que la figure : {@code "nom=parent"}, et
     * un tiret pour une racine.
     */
    private static String parents(Category category) {
        StringBuilder out = new StringBuilder();
        for (Skill skill : category.getSkills()) {
            if (out.length() > 0) out.append(' ');
            Skill parent = skill.getParent();
            out.append(skill.getName()).append('=').append(parent == null ? "-" : parent.getName());
        }
        return out.toString();
    }

    @Test
    @DisplayName("chaque competence part de celle dont l'original la faisait partir")
    void parentsAreTheOriginals() {
        // LE TEST QUI MANQUAIT, ET QUI AURAIT VU LA FAUTE. Le port avait oublie
        // `shiftTP.setParent(locTP, 0.5f)` : shift_teleport se retrouvait racine de l'arbre, donc
        // dessine sans trait vers son parent — et c'est le joueur qui l'a vu avant le test : « il n'y a
        // pas de trait qui relie le location teleport au shift teleport ». La parente se lit maintenant
        // ici, categorie par categorie, dans l'ordre d'enregistrement.
        String courses = " brain_course=- brain_course_advanced=brain_course"
                + " mind_course=brain_course_advanced";

        assertEquals("arc_gen=- charging=arc_gen mag_movement=arc_gen mag_manip=mag_movement"
                        + " mine_detect=mag_manip body_intensify=arc_gen thunder_bolt=arc_gen"
                        + " railgun=thunder_bolt thunder_clap=thunder_bolt" + courses,
                parents(cn.academy.ability.electromaster.ElectromasterCategory.INSTANCE));

        assertEquals("electron_bomb=- rad_intensify=electron_bomb scatter_bomb=electron_bomb"
                        + " light_shield=electron_bomb meltdowner=scatter_bomb"
                        + " mine_ray_basic=meltdowner ray_barrage=meltdowner jet_engine=meltdowner"
                        + " mine_ray_expert=mine_ray_basic mine_ray_luck=mine_ray_expert"
                        + " electron_missile=jet_engine" + courses,
                parents(cn.academy.ability.meltdowner.MeltdownerCategory.INSTANCE));

        assertEquals("threatening_teleport=- dim_folding_theorem=threatening_teleport"
                        + " penetrate_teleport=threatening_teleport mark_teleport=threatening_teleport"
                        + " flesh_ripping=mark_teleport location_teleport=penetrate_teleport"
                        + " shift_tp=location_teleport space_fluct=shift_tp flashing=shift_tp"
                        + courses,
                parents(cn.academy.ability.teleporter.TeleporterCategory.INSTANCE));

        assertEquals("dir_shock=- ground_shock=dir_shock vec_accel=dir_shock"
                        + " vec_deviation=vec_accel dir_blast=ground_shock storm_wing=vec_accel"
                        + " blood_retro=dir_blast vec_reflection=vec_deviation"
                        + " plasma_cannon=storm_wing" + courses,
                parents(cn.academy.ability.vecmanip.VecmanipCategory.INSTANCE));
    }

    /** Le fichier de langue du port, tel que le client le lit. */
    private static JsonObject language() {
        String path = "assets/academy/lang/en_us.json";
        try (InputStream in = SkillTreeLayoutTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "le fichier " + path + " doit exister");
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("Le fichier " + path + " est illisible", e);
        }
    }

    @Test
    @DisplayName("chaque competence a son nom et sa description dans les langues")
    void everySkillIsNamedAndDescribed() {
        // Une cle manquante ne casse rien : le client affiche la cle elle-meme, en petit et en anglais
        // de machine — `ac.ability.electromaster.railgun.desc` en travers de la surface. C'est laid, et
        // cela ne se voit qu'en jeu ; d'ou ce test, qui relit le fichier de langue comme le client.
        JsonObject lang = language();

        for (Category category : categories()) {
            for (Skill skill : category.getSkills()) {
                String who = category.getName() + "." + skill.getName();
                assertTrue(lang.has(skill.getDisplayKey()), who + " n'a pas de nom");
                assertTrue(lang.has(skill.getDescriptionKey()), who + " n'a pas de description");
                assertFalse(lang.get(skill.getDescriptionKey()).getAsString().isBlank(),
                        who + " a une description vide");
            }
        }
    }
}
