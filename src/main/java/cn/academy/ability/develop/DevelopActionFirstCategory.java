package cn.academy.ability.develop;

import cn.academy.FactorItem;
import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * La PREMIERE aptitude : le joueur n'en a aucune, et le developpeur lui en donne une.
 *
 * <p>Portage du {@code chooseCategory} de {@code DevelopActionLevel} de l'original. Chez lui,
 * cette methode vivait dans l'action de montee de niveau, parce que son {@code AbilityData}
 * ne portait qu'UNE categorie : monter un cran quand on n'en avait pas, c'etait la choisir.
 * Le port en a fait une action a part, parce que ses actions portent leur categorie et qu'une
 * action qui n'en a pas encore ne peut pas passer par la meme porte. Le reste est identique,
 * jusqu'au prix.
 *
 * <p>Le choix est celui de l'original, et c'est le joueur qui decide : un <b>facteur
 * d'induction</b> dans l'inventaire designe sa categorie — et il est consomme — sinon le sort
 * en donne une au hasard. C'est le seul chemin d'entree dans le mod : sans lui, un joueur qui
 * ouvre son developpeur ne peut rien apprendre et croit la machine cassee.
 */
public class DevelopActionFirstCategory implements DevelopAction {

    /**
     * L'identifiant de competence qui dit « la premiere aptitude ».
     *
     * <p>Un apprentissage se designe par {@code (categorie, competence)} : -1 pour le niveau de
     * la categorie, -2 pour un changement de categorie, -3 pour celle-ci.
     */
    public static final int SKILL_ID = -3;

    private final Category chosen;

    private DevelopActionFirstCategory(Category chosen) {
        this.chosen = chosen;
    }

    /** La categorie que le facteur ou le sort a designee. */
    public Category getChosen() {
        return chosen;
    }

    /**
     * L'action telle que le joueur la declenche : ce qu'il a dans l'inventaire decide, sinon le
     * hasard.
     *
     * <p>{@code otherCategoryIn(player, null)} rend le premier facteur de l'inventaire, quelle
     * que soit sa categorie : c'est exactement la lecture que faisait l'original.
     *
     * @return {@code null} si le registre des categories est vide
     */
    @Nullable
    public static DevelopActionFirstCategory of(Player player) {
        Category fromInventory = FactorItem.otherCategoryIn(player, null);
        if (fromInventory != null) return new DevelopActionFirstCategory(fromInventory);

        CategoryManager manager = CategoryManager.INSTANCE;
        if (manager.getCategories().isEmpty()) return null;
        int index = player.getRandom().nextInt(manager.getCategories().size());
        return new DevelopActionFirstCategory(manager.getCategories().get(index));
    }

    /**
     * Donne la premiere aptitude tout de suite, sans machine et sans attente, et rend celle qui a
     * ete donnee.
     *
     * <p>C'est ce que fait la commande {@code /learn}. Le developpeur, lui, passe par le
     * deroulement normal : la machine chauffe, paie son energie, et l'aptitude tombe a la fin.
     *
     * @return la categorie apprise, ou {@code null} si le joueur en avait deja une
     */
    @Nullable
    public static Category grantIfNone(Player player) {
        AbilityData data = dataOf(player);
        if (data == null || data.getHighestLevel() > 0) return null;

        DevelopActionFirstCategory action = of(player);
        if (action == null) return null;
        action.onLearned(player);
        return action.getChosen();
    }

    @Override
    public int getCategoryId() {
        return chosen.getCategoryId();
    }

    @Override
    public int getSkillId() {
        return SKILL_ID;
    }

    /** Cinq stimulations : le prix d'un premier niveau, puisque c'en est un. */
    @Override
    public int getStimulations(Player player) {
        return 5;
    }

    /**
     * Vrai tant que le joueur n'a aucune aptitude.
     *
     * <p>La machine n'a pas a etre avancee pour cela : n'importe quel developpeur ouvre la porte
     * du mod, comme dans l'original.
     */
    @Override
    public boolean validate(Player player, DeveloperType developer) {
        AbilityData data = dataOf(player);
        return data != null && data.getHighestLevel() == 0;
    }

    @Override
    public void onLearned(Player player) {
        AbilityData data = dataOf(player);
        if (data == null) return;

        data.setCategoryLevel(chosen, 1);
        // Le facteur qui a designe la categorie y passe : l'original le consommait au moment du
        // choix, et le joueur s'attend a le voir disparaitre de son inventaire.
        FactorItem.consume(player);
    }

    private static AbilityData dataOf(Player player) {
        return player.getCapability(AbilityCapability.ABILITY_DATA).resolve().orElse(null);
    }
}
