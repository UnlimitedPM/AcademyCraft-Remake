package cn.academy.ability.develop;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Le changement de categorie : quelle categorie on quitte, et a quel prix.
 *
 * <p>Tout le reste de cette action se lit sur le joueur — ce qu'il a en main, ce qu'il a
 * dans son sac — donc en jeu. Ce qui se relit ici est la seule decision un peu nouee :
 * l'original n'avait <b>qu'une</b> categorie par joueur, le port en tient une par
 * categorie et doit donc choisir laquelle on sacrifie. C'est pour cela que la decision ne
 * regarde que ce que la donnee du joueur retient, et jamais le registre.
 */
class DevelopActionResetTest {

    private static Category category(String name) {
        return new Category(name);
    }

    @Test
    void laCategorieSacrifieeEstLaPlusHaute() {
        AbilityData data = new AbilityData();
        Category first = category("electromaster");
        Category second = category("meltdowner");

        data.setCategoryLevel(first, 3);
        data.setCategoryLevel(second, 4);

        assertEquals("meltdowner",
                DevelopActionReset.abandonedCategoryName(data, DevelopActionReset.MIN_LEVEL),
                "c'est la categorie la plus haute qu'on quitte");
    }

    @Test
    void deuxCategoriesAuMemeNiveauDonnentToujoursLaMemeReponse() {
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category("vecmanip"), 4);
        data.setCategoryLevel(category("electromaster"), 4);

        // Le nom tranche : sans cela, l'ordre d'une table les departagerait au hasard, et
        // le meme joueur pourrait sacrifier l'une ou l'autre d'un clic a l'autre.
        assertEquals("electromaster",
                DevelopActionReset.abandonedCategoryName(data, DevelopActionReset.MIN_LEVEL));
    }

    @Test
    void unJoueurTropBasNePeutRienChanger() {
        AbilityData data = new AbilityData();
        Category single = category("electromaster");

        assertNull(DevelopActionReset.abandonedCategoryName(data, DevelopActionReset.MIN_LEVEL),
                "sans aucune categorie apprise, rien a sacrifier");

        data.setCategoryLevel(single, 2);
        assertNull(DevelopActionReset.abandonedCategoryName(data, DevelopActionReset.MIN_LEVEL),
                "le seuil de l'original est le niveau 3");
        assertEquals(0, DevelopActionReset.stimulationsFor(data),
                "et il n'y a donc rien a payer");

        data.setCategoryLevel(single, 3);
        assertEquals("electromaster",
                DevelopActionReset.abandonedCategoryName(data, DevelopActionReset.MIN_LEVEL),
                "au niveau 3, c'est ouvert");
    }

    @Test
    void lePrixSuitLeNiveauQuOnOublie() {
        AbilityData data = new AbilityData();
        data.setCategoryLevel(category("electromaster"), 5);

        // L'original comptait dix stimulations par niveau a oublier : oublier plus haut
        // coute donc plus cher, et c'est tout l'arbitrage du changement de categorie.
        assertEquals(50, DevelopActionReset.stimulationsFor(data),
                "cinq niveaux, cinquante stimulations");
    }

    @Test
    void laCibleDUneAnneeSeRangeDansLesMemesNombres() {
        // Un apprentissage se designe par (categorie, competence), avec -1 pour le niveau
        // de la categorie. Le changement de categorie est la troisieme sorte : -2.
        assertEquals(-2, DevelopActionReset.SKILL_ID);
        assertEquals(3, DevelopActionReset.MIN_LEVEL, "le seuil de l'original");
    }
}

