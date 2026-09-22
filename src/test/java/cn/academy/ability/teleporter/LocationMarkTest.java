package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les endroits marques d'un joueur, et ce qu'ils coutent a rejoindre.
 *
 * <p>C'est une donnee du joueur comme les competences : elle se sauvegarde, elle voyage
 * jusqu'au client, et tout ce qui decide si une marque mene quelque part se lit ici — la
 * liste elle-meme, la porte de la dimension, et le prix. Les trois se verifient sans monde,
 * donc ils se verifient ici.
 */
class LocationMarkTest {

    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private static AbilityData withMarks(String... names) {
        AbilityData data = new AbilityData();
        for (int i = 0; i < names.length; i++) {
            data.addMark(names[i], OVERWORLD, i * 10.0, 64.0, 0);
        }
        return data;
    }

    @Test
    void leRangDansLaListeEstLIdentifiant() {
        AbilityData data = withMarks("maison", "mine", "portail");

        assertEquals(3, data.getMarks().size());
        assertEquals("mine", data.getMark(1).name());
        assertEquals(10.0, data.getMark(1).x(), 0.0001);
        assertNull(data.getMark(3), "un rang hors de la liste n'existe pas");

        // Oublier la deuxieme renumerote celles qui suivent : c'est ce que faisait
        // l'original, et sans cela l'ecran enverrait un rang pour en designer un autre.
        data.removeMark(1);
        assertEquals(2, data.getMarks().size());
        assertEquals("maison", data.getMark(0).name());
        assertEquals("portail", data.getMark(1).name());
        assertEquals(20.0, data.getMark(1).x(), 0.0001);

        // Un rang absurde ne casse rien : il vient d'un client, ou d'un ecran ouvert avant
        // qu'une autre marque ne soit oubliee.
        data.removeMark(-1);
        data.removeMark(9);
        assertEquals(2, data.getMarks().size());
    }

    @Test
    void lesNomsVidesOuTropLongsSontRattrapes() {
        // Le nom vient du client : sans ce nettoyage, un nom de mille caracteres casserait
        // l'affichage d'une ligne, et un nom vide rendrait la marque introuvable.
        assertEquals("Point 1", LocationMark.cleanName("", 0));
        assertEquals("Point 3", LocationMark.cleanName("   ", 2));
        assertEquals("Point 2", LocationMark.cleanName(null, 1));
        assertEquals("maison", LocationMark.cleanName("  maison  ", 0));

        String longName = "x".repeat(50);
        assertEquals(LocationMark.MAX_NAME, LocationMark.cleanName(longName, 0).length());
    }

    @Test
    void lesMarquesSurviventALaSauvegarde() {
        AbilityData data = withMarks("maison", "mine");
        data.addMark("enfer", NETHER, 100.0, 30.0, -200.0);

        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertEquals(3, reloaded.getMarks().size());
        assertEquals("enfer", reloaded.getMark(2).name());
        assertEquals(100.0, reloaded.getMark(2).x(), 0.0001);
        assertEquals(-200.0, reloaded.getMark(2).z(), 0.0001);
        // La dimension aussi : sans elle, une marque de l'enfer renverrait dans le monde
        // normal, aux memes coordonnees.
        assertEquals(NETHER, reloaded.getMark(2).dimension());
        assertEquals("the_nether", reloaded.getMark(2).dimensionName());
        assertEquals("overworld", reloaded.getMark(0).dimensionName());
    }

    @Test
    void uneDimensionIllisibleEstOublieeSansEmporterLesAutres() {
        AbilityData data = withMarks("maison", "mine");
        CompoundTag tag = data.serializeNBT();

        // Un texte de dimension qui n'en est pas un — une sauvegarde abimee, un mod qui
        // ecrivait autre chose : la marque doit disparaitre, et non faire echouer la lecture
        // de la sauvegarde entiere.
        var list = new net.minecraft.nbt.ListTag();
        CompoundTag broken = tag.getList("marks", CompoundTag.TAG_COMPOUND).getCompound(0).copy();
        broken.putString("dim", "pas une dimension");
        list.add(broken);
        list.add(tag.getList("marks", CompoundTag.TAG_COMPOUND).getCompound(1));
        tag.put("marks", list);

        AbilityData reloaded = new AbilityData();
        reloaded.deserializeNBT(tag);

        assertEquals(1, reloaded.getMarks().size(), "la marque lisible doit survivre");
        assertEquals("mine", reloaded.getMark(0).name());
    }

    @Test
    void lePrixGranditAvecLaDistanceEtDoubleEntreDeuxMondes() {
        var skill = TeleporterCategory.LOCATION_TELEPORT;
        AbilityData fresh = new AbilityData();
        AbilityData expert = new AbilityData();
        expert.setCategoryLevel(skill.getCategory(), 1);
        expert.learnSkill(skill);
        expert.addSkillExp(skill, 1f);

        // Un saut court coute son minimum : huit fois la base, jamais moins.
        assertClose(7.14 * 8, skill.cpCost(fresh, 5, false));
        assertClose(7.14 * 8, skill.cpCost(fresh, 64, false), "64 blocs donnent aussi huit");
        // Au-dela, c'est la racine de la distance.
        assertClose(7.14 * 10, skill.cpCost(fresh, 100, false));
        assertClose(5.36 * 10, skill.cpCost(expert, 100, false), "un expert paie moins");
        // Le prix plafonne a 800 blocs : au-dela, la note ne grandit plus.
        assertClose(7.14 * Math.sqrt(800), skill.cpCost(fresh, 5000, false));
        // Changer de monde double la note.
        assertClose(skill.cpCost(fresh, 100, false) * 2, skill.cpCost(fresh, 100, true));

        // Sur la reserve du port, un saut court mange donc plus de la moitie de la barre :
        // c'est un voyage, pas un deplacement — et c'est ce que l'original vendait.
        assertTrue(skill.cpCost(fresh, 5, false) > 50,
                "un saut meme court coute la moitie de la reserve");

        assertEquals(240f, skill.getOverloadCost(fresh), 0.0001f, "surcout fixe de l'original");
        assertEquals(0f, skill.getCpCost(), 0.0001f, "rien a l'allumage : la touche ouvre une liste");
        assertEquals(0, skill.getCooldownTicks(fresh),
                "le paquet ne pose rien : la recharge vient du saut lui-meme");
        assertEquals(30, skill.cooldown(fresh));
        assertEquals(20, skill.cooldown(expert));
        assertEquals(0.015f, skill.expFor(199), 0.0001f);
        assertEquals(0.03f, skill.expFor(200), 0.0001f, "un saut long rapporte deux fois plus");

        // La traversee de dimension s'ouvre a 80 % d'experience, comme dans l'original.
        assertFalse(skill.canCrossDimension(fresh));
        AbilityData almost = new AbilityData();
        almost.setCategoryLevel(skill.getCategory(), 1);
        almost.learnSkill(skill);
        almost.addSkillExp(skill, 0.79f);
        assertFalse(skill.canCrossDimension(almost), "80 % sont un seuil, pas une approximation");
        assertTrue(skill.canCrossDimension(expert));
    }

    @Test
    void laCompetenceOuvreUnEcranAuLieuDePartir() {
        var skill = TeleporterCategory.LOCATION_TELEPORT;

        // C'est ce que lit le client : sans ce drapeau, il enverrait une activation pour une
        // competence qui n'en veut pas.
        assertTrue(skill.opensScreen());
        assertFalse(skill.isHeld());
        assertFalse(skill.isChargeable());
        assertTrue(skill.earnsExpOnEffect(), "l'experience se verse au saut, pas a l'appui");
        assertEquals(0f, skill.getExpGain(new AbilityData()), 0.0001f);
    }

    @Test
    void lesMarquesSontRenduesEnCopie() {
        AbilityData data = withMarks("maison");

        // La liste rendue ne doit pas etre celle du joueur : l'ecran la parcourt, et une
        // liste modifiable finirait par l'etre ailleurs.
        assertNotNull(data.getMarks());
        try {
            data.getMarks().add(new LocationMark("intrus", OVERWORLD, 0, 0, 0));
            assertEquals(1, data.getMarks().size(), "la liste rendue doit etre une copie");
        } catch (UnsupportedOperationException expected) {
            // Une copie non modifiable est une autre bonne reponse.
        }
    }

    private static void assertClose(double expected, double actual) {
        assertClose(expected, actual, "prix du saut");
    }

    private static void assertClose(double expected, double actual, String what) {
        assertEquals(expected, actual, 0.01, what + " : attendu " + expected + ", trouve " + actual);
    }
}
