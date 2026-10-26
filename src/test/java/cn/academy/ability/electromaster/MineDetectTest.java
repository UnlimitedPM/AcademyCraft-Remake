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
        // A mi-portee : deja presque efface, l'effacement valant 2,2 fois la portee.
        assertTrue(MineDetectVisuals.alpha(9.0, range) > MineDetectVisuals.alpha(14.0, range),
                "plus pres veut dire plus visible");
        // Et au-dela des deux tiers de la portee : plus rien du tout. C'est la borne FINALE de
        // l'original, qui laissait son alpha passer sous zero et se faire ramener a zero par la
        // couleur qui le recevait.
        assertEquals(0.0f, MineDetectVisuals.alpha(13.0, range), 1.0e-6,
                "a 0,65 portee, la formule tombe juste sur zero");
        assertEquals(0.0f, MineDetectVisuals.alpha(range, range), 1.0e-6,
                "et un minerai a la limite ne se dessine plus du tout");
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

        // Tout est BLEU, quel que soit le palier : les cinq teintes de l'original sont parties
        // le 26/10 — « certains minerais apparaissent en vert et je ne sais pas pourquoi » —, et
        // seul le minerai choisi dans l'ecran de filtre passe au vert (voir MineDetectFilter).
        assertColor(115, 200, 227, 0);
        assertColor(115, 200, 227, 1);
        assertColor(115, 200, 227, 2);
        assertColor(115, 200, 227, 3);
        assertColor(115, 200, 227, 4);

        // Et un palier hors bornes rend la meme chose plutot que de sortir de la table.
        assertColor(115, 200, 227, -1);
        assertColor(115, 200, 227, 99);

        // Le choix du joueur, lui, passe AVANT le palier : c'est une couleur de choix, pas une
        // couleur de pioche.
        int[] chosen = MineDetectVisuals.colorFor(3, true);
        assertEquals(90, chosen[0], "rouge du minerai choisi");
        assertEquals(230, chosen[1], "vert du minerai choisi");
        assertEquals(120, chosen[2], "bleu du minerai choisi");
        int[] plain = MineDetectVisuals.colorFor(3, false);
        assertEquals(115, plain[0], "et le minerai non choisi reste bleu");
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

    @Test
    void leBalayageSArreteOuLeDessinSArrete() {
        // L'eclat ne peint rien au-dela de 0,6494 fois sa portee : `alpha` y vaut deja zero.
        // Le balayage s'arrete donc la, et c'est ce qui divise les blocs lus par 3,8 sur 28
        // (91 965 positions avant, 24 405 apres).
        assertEquals(0.64935, MineDetectVisuals.VISIBLE_RANGE_FRACTION, 0.0005,
                "la fraction ou l'opacite tombe a zero");
        assertEquals(0.0f, MineDetectVisuals.alpha(MineDetectVisuals.visibleRange(20.0), 20.0),
                1.0e-6, "a la portee visible, le minerai ne se peint plus");
        assertTrue(MineDetectVisuals.alpha(MineDetectVisuals.visibleRange(20.0) - 0.01, 20.0) > 0f,
                "juste en deca, il se peint encore");
        assertEquals(28.0 * MineDetectVisuals.VISIBLE_RANGE_FRACTION,
                MineDetectVisuals.visibleRange(28.0), 1.0e-9,
                "et la portee visible suit la portee de l'eclat");
    }
}
