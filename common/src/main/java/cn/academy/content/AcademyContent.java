package cn.academy.content;

import cn.academy.AcademyCraftCommon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;

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

    /** Le fluide du mod portant cet identifiant, ou {@link Fluids#EMPTY} s'il n'existe pas. */
    public static Fluid fluid(String path) {
        return BuiltInRegistries.FLUID.get(new ResourceLocation(AcademyCraftCommon.MOD_ID, path));
    }

    /**
     * Le type d'entite du mod portant cet identifiant, ou {@code null} s'il n'existe pas.
     *
     * <p>Le cast est celui que le chargeur garantit : l'entite demandee est bien celle dont on
     * declare le type au moment de le lire.
     */
    @SuppressWarnings("unchecked")
    public static <T extends net.minecraft.world.entity.Entity> EntityType<T> entityType(String path) {
        ResourceLocation id = new ResourceLocation(AcademyCraftCommon.MOD_ID, path);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                ? (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.get(id) : null;
    }

    /** Le son du mod portant cet identifiant, ou {@code null} s'il n'existe pas. */
    public static SoundEvent sound(String path) {
        ResourceLocation id = new ResourceLocation(AcademyCraftCommon.MOD_ID, path);
        return BuiltInRegistries.SOUND_EVENT.containsKey(id)
                ? BuiltInRegistries.SOUND_EVENT.get(id) : null;
    }

    /**
     * Vrai si ce fluide est celui de la phase liquide du mod.
     *
     * <p>Forge distingue la source et le courant ({@code phase_liquid} et
     * {@code phase_liquid_flowing}) ; les deux sont acceptes, comme le faisait le test sur le
     * {@code FluidType}.
     */
    public static boolean isPhaseLiquid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        if (!AcademyCraftCommon.MOD_ID.equals(id.getNamespace())) return false;
        String path = id.getPath();
        return path.equals("phase_liquid") || path.equals("phase_liquid_flowing");
    }
}
