package cn.academy.content;

import cn.academy.AcademyCraftCommon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Acces neutre au contenu du mod, par identifiant.
 *
 * <p>Le code de {@code common/} ne peut pas nommer {@code ModItems} ni {@code ModBlocks}, qui sont
 * des registres de chargeur. Il passe donc par la registry vanilla : chaque chargeur y enregistre
 * les memes identifiants, et cette classe les retrouve au moment de l'emploi (construction des
 * recettes, par exemple), quand les registres sont remplis.
 */
public final class AcademyContent {

    private AcademyContent() {
    }

    /** L'objet du mod portant cet identifiant, ou {@link Items#AIR} s'il n'existe pas. */
    public static Item item(String path) {
        return BuiltInRegistries.ITEM.get(new ResourceLocation(AcademyCraftCommon.MOD_ID, path));
    }

    /** Le bloc du mod portant cet identifiant, ou {@link Blocks#AIR} s'il n'existe pas. */
    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(AcademyCraftCommon.MOD_ID, path));
    }

    /** L'objet-bloc du mod portant cet identifiant (les deux partagent l'identifiant). */
    public static Item blockItem(String path) {
        return item(path);
    }
}
