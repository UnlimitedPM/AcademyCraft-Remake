package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.client.MineDetectVisuals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce que l'eclat de la detection de minerais dessine, {@code MineDetectVisuals}.
 *
 * <p>Trois regles, et toutes les trois viennent de l'original : le plafond de portee, la
 * transparence selon la distance, et la couleur selon le palier de pioche — celle-la valant
 * seulement une fois la competence complete, cinquante pour cent d'experience et une
 * categorie de niveau 4.
 */
class MineDetectTest {

    @Test
    void laPorteeEstPlafonneeAVingtHuit() {
        // La competence monte jusqu'a trente, mais le balayage de l'original s'arretait a
        // vingt-huit : le volume d'un rayon de trente se relit trop lentement.
        assertEquals(15.0, MineDetectVisuals.capRange(15.0), 1.0e-9);
        assertEquals(28.0, MineDetectVisuals.capRange(28.0), 1.0e-9);
        assertEquals(28.0, MineDetectVisuals.capRange(30.0), 1.0e-9, "et pas au-dela");
    }

    @Test
    void laTransparenceSEffaceAvecLaDistance() {
        double range = 20.0;
        // Sur le minerai : pleine. C'est le `0.3 + 0.7` de l'original.
        assertEquals(1.0f, MineDetectVisuals.alpha(0.0, range), 1.0e-6);
        // A mi-portee : deja presque plein, l'effacement valant 2,2 fois la portee.
        assertTrue(MineDetectVisuals.alpha(9.0, range) > MineDetectVisuals.alpha(14.0, range),
                "plus pres veut dire plus visible");
        // Et loin : le plancher de 0,3, jamais moins.
        assertEquals(MineDetectVisuals.BASE_ALPHA, MineDetectVisuals.alpha(20.0, range), 1.0e-6);
        assertEquals(MineDetectVisuals.BASE_ALPHA, MineDetectVisuals.alpha(100.0, range), 1.0e-6,
                "et il ne descend jamais plus bas");
    }

    @Test
    void lesCouleursSuiventLePalierDePioche() {
        // Sans l'eclat complet, tous les minerais se ressemblent : le palier zero.
        assertEquals(0, MineDetectVisuals.tierOf(false, 3),
                "sans eclat complet, le palier de pioche n'est pas lu");
        assertEquals(0, MineDetectVisuals.tierOf(false, 0));

        // Avec : le palier est decale d'un cran, la premiere couleur restant celle des
        // minerais ordinaires. C'est le `min(3, harvest + 1)` de l'original.
        assertEquals(1, MineDetectVisuals.tierOf(true, 0), "un minerai qui se creuse a la main");
        assertEquals(2, MineDetectVisuals.tierOf(true, 1), "a la pierre");
        assertEquals(3, MineDetectVisuals.tierOf(true, 2), "au fer");
        assertEquals(3, MineDetectVisuals.tierOf(true, 3),
                "au diamant — et la cinquieme couleur de l'original n'etait jamais atteinte");

        // Les cinq couleurs de l'original, dans l'ordre.
        assertColor(115, 200, 227, 0);
        assertColor(161, 181, 188, 1);
        assertColor(87, 231, 248, 2);
        assertColor(97, 204, 94, 3);
        assertColor(235, 109, 84, 4);

        // Et un palier hors bornes est ramene dans la table plutot que de la depasser.
        assertColor(115, 200, 227, -1);
        assertColor(235, 109, 84, 99);
    }

    private static void assertColor(int r, int g, int b, int tier) {
        int[] color = MineDetectVisuals.colorFor(tier);
        assertEquals(r, color[0], "rouge du palier " + tier);
        assertEquals(g, color[1], "vert du palier " + tier);
        assertEquals(b, color[2], "bleu du palier " + tier);
    }

    @Test
    void lEclatCompletDemandeExperienceEtNiveau() {
        var detect = ElectromasterCategory.MINE_DETECT;
        var category = detect.getCategory();

        cn.academy.ability.AbilityData data = new AbilityData();
        data.setCategoryLevel(category, 4);
        data.learnSkill(detect);
        data.addSkillExp(detect, 0.6f);

        assertTrue(detect.advanced(data, data.getCategoryLevel(category)),
                "0,6 d'experience et une categorie de niveau 4 : l'eclat est complet");

        // L'un sans l'autre ne suffit pas, et c'est la double condition de l'original.
        cn.academy.ability.AbilityData young = new AbilityData();
        young.setCategoryLevel(category, 3);
        young.learnSkill(detect);
        young.addSkillExp(detect, 1f);
        assertFalse(detect.advanced(young, young.getCategoryLevel(category)),
                "toute l'experience du monde ne remplace pas un niveau 4");

        cn.academy.ability.AbilityData novice = new AbilityData();
        novice.setCategoryLevel(category, 5);
        novice.learnSkill(detect);
        novice.addSkillExp(detect, 0.4f);
        assertFalse(detect.advanced(novice, novice.getCategoryLevel(category)),
                "et un niveau 5 ne remplace pas l'experience");
    }
}
