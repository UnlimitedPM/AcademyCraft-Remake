package cn.academy;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Un facteur d'induction : l'objet qui dit quelle categorie on veut prendre.
 *
 * <p>Portage de {@code ItemInductionFactor}, avec une difference de forme : l'original
 * gardait sa categorie dans les <b>metadonnees</b> de la pile, ce que la 1.20.1 n'a plus.
 * Le port a donc eclate l'objet en quatre — un par categorie — et c'est la classe qui
 * retient laquelle. Le nom de la categorie est resolu <b>a la demande</b>, parce que les
 * objets se declarent avant que les categories soient enregistrees.
 */
public class FactorItem extends ModItems.TooltipItem {

    private final String categoryName;

    public FactorItem(String categoryName) {
        // L'infobulle reste celle de l'objet, comme avant l'eclatement.
        super("ac.ability." + categoryName + ".name");
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    /** La categorie de ce facteur, ou {@code null} si le registre ne la connait pas. */
    @Nullable
    public Category getCategory() {
        return CategoryManager.INSTANCE.getCategory(categoryName);
    }

    /** La categorie que designe une pile, ou {@code null} si ce n'est pas un facteur. */
    @Nullable
    public static Category categoryOf(ItemStack stack) {
        return stack.getItem() instanceof FactorItem factor ? factor.getCategory() : null;
    }

    /**
     * Le premier facteur de l'inventaire qui designe une categorie <b>autre</b> que celle
     * qu'on veut quitter.
     *
     * <p>L'original faisait exactement cela : il parcourait l'inventaire a la recherche
     * d'un facteur dont la categorie n'etait pas celle du joueur, et c'est lui qui decidait
     * de la categorie d'arrivee.
     */
    @Nullable
    public static Category otherCategoryIn(Player player, Category excluded) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            Category category = categoryOf(player.getInventory().getItem(slot));
            if (category != null && category != excluded) return category;
        }
        return null;
    }

    /** Retire le premier facteur de l'inventaire, et dit s'il y en avait un. */
    public static boolean consume(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof FactorItem) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }
}
