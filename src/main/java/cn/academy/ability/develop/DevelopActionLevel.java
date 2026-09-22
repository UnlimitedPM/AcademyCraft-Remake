package cn.academy.ability.develop;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import net.minecraft.world.entity.player.Player;

/**
 * Developper le niveau d'une categorie d'aptitude.
 *
 * Portage de {@code DevelopActionLevel}. C'est l'apprentissage principal du
 * developeur : il fait passer une categorie du niveau 0 (non apprise) au niveau 1,
 * puis la fait monter jusqu'au niveau 5.
 *
 * <h2>Ce qui a ete simplifie, et pourquoi</h2>
 *
 * Dans l'original, {@code canLevelUp()} exigeait non seulement d'etre sous le
 * niveau 5 mais aussi d'avoir appris toutes les competences du niveau courant, ce
 * qui demandait une progression par experience que le port n'a pas encore. Ici la
 * seule condition est donc le niveau, ce qui revient a dire qu'on peut monter
 * d'un cran des qu'on en a les moyens. C'est une simplification assumee : le jour
 * ou la progression par experience sera portée, c'est cette methode qu'il faudra
 * reprendre.
 *
 * Le cout suit la formule de l'original : {@code 5 * (niveau + 1)} stimulations,
 * donc de plus en plus cher a chaque cran.
 */
public class DevelopActionLevel implements DevelopAction {

    /** Niveau maximal d'une categorie, comme {@code canLevelUp()} dans l'original. */
    public static final int MAX_LEVEL = 5;

    private final Category category;

    public DevelopActionLevel(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public int getStimulations(Player player) {
        return 5 * (levelOf(player) + 1);
    }

    @Override
    public boolean validate(Player player, DeveloperType developer) {
        AbilityData data = dataOf(player);
        if (data == null) return false;
        // Reprend LearningHelper.canLevelUp de l'original : une categorie deja
        // apprise ne monte que si son palier est rempli, c'est-a-dire si le joueur a
        // utilise ses competences. Une categorie jamais apprise passe sans palier :
        // c'est ce qui la fait entrer dans le systeme.
        //
        // L'original passait la qualite de la machine a `canLevelUp(type, data)`... et la
        // fonction ne lisait que le joueur. Monter une categorie d'un cran ne demande donc
        // aucune machine en particulier : c'est le PALIER qui decide, et il se remplit en
        // utilisant ses competences. Le parametre reste la pour la signature.
        if (!data.hasLearned(category)) return true;
        return data.canLevelUp(category);
    }

    @Override
    public void onLearned(Player player) {
        AbilityData data = dataOf(player);
        if (data == null) return;
        data.setCategoryLevel(category, levelOf(player) + 1);
    }

    /** Niveau actuel de cette categorie pour ce joueur, 0 s'il ne l'a pas apprise. */
    public int levelOf(Player player) {
        AbilityData data = dataOf(player);
        return data == null ? 0 : data.getCategoryLevel(category);
    }

    private static AbilityData dataOf(Player player) {
        return player.getCapability(AbilityCapability.ABILITY_DATA).resolve().orElse(null);
    }
}
