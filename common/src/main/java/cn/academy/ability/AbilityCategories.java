package cn.academy.ability;

import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;

/**
 * Les quatre cursus du mod, dans un ordre stable.
 *
 * <p>Les categories s'enregistrent une fois, au demarrage, puis {@link CategoryManager#bake()}
 * leur donne leur identifiant — celui qui voyage dans les paquets. L'ordre d'appel ici n'a pas
 * d'importance ({@code bake} trie par nom), mais il est ecrit une seule fois pour les trois
 * chargeurs : c'est la liste qui compte, pas le chargeur.
 */
public final class AbilityCategories {

    private AbilityCategories() {
    }

    public static void registerAll() {
        CategoryManager.INSTANCE.register(VecmanipCategory.INSTANCE);
        CategoryManager.INSTANCE.register(ElectromasterCategory.INSTANCE);
        CategoryManager.INSTANCE.register(TeleporterCategory.INSTANCE);
        CategoryManager.INSTANCE.register(MeltdownerCategory.INSTANCE);
        CategoryManager.INSTANCE.bake();
    }
}
