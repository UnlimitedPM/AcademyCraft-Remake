package cn.academy.ability.electromaster;

import cn.academy.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ce que l'electromaster peut attirer : les blocs et les entites metalliques.
 *
 * <p>Portage de {@code CatElectromaster} : l'original tenait trois listes dans sa
 * config — blocs franchement metalliques, blocs faiblement metalliques, entites
 * metalliques — et deux competences ne visaient que cela. Les listes sont ici aussi en
 * config, aux memes valeurs par defaut, avec les noms de la 1.20.1.
 *
 * <p>Deux listes de blocs et pas une, parce que l'original distinguait les deux : sous
 * soixante pour cent d'experience, un debutant ne s'accroche qu'aux blocs franchement
 * metalliques. Un rail, oui ; une machine ou un minerai de fer, pas encore.
 */
public final class MetalTargets {

    /** Experience a partir de laquelle les blocs faiblement metalliques accrochent. */
    public static final float WEAK_EXP = 0.6f;

    private static Set<Block> normal = Set.of();
    private static Set<Block> weak = Set.of();
    private static Set<ResourceLocation> entities = Set.of();

    /**
     * Les listes dont les ensembles ont ete construits.
     *
     * La config remplace ses listes par de nouvelles instances a chaque chargement :
     * comparer les references suffit donc a savoir qu'il faut reconstruire, sans avoir
     * a s'inscrire a quoi que ce soit.
     */
    private static List<String> normalFrom;
    private static List<String> weakFrom;
    private static List<String> entitiesFrom;

    private MetalTargets() {}

    public static boolean isNormalMetalBlock(Block block) {
        ensureBuilt();
        return normal.contains(block);
    }

    public static boolean isWeakMetalBlock(Block block) {
        ensureBuilt();
        return weak.contains(block);
    }

    public static boolean isMetalBlock(Block block) {
        return isNormalMetalBlock(block) || isWeakMetalBlock(block);
    }

    /**
     * Vrai si l'experience du joueur lui permet de s'accrocher a ce bloc.
     *
     * Reprend la regle de {@code toTarget} : les blocs faiblement metalliques demandent
     * {@link #WEAK_EXP}, les autres sont toujours accessibles.
     */
    public static boolean canHook(Block block, float exp) {
        return isNormalMetalBlock(block) || (exp >= WEAK_EXP && isWeakMetalBlock(block));
    }

    /** Vrai si cette entite est de celles que l'electromaster peut attirer. */
    public static boolean isMetallic(Entity entity) {
        ensureBuilt();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && entities.contains(id);
    }

    private static void ensureBuilt() {
        if (normalFrom == Config.metalBlocks && weakFrom == Config.weakMetalBlocks
                && entitiesFrom == Config.metalEntities) {
            return;
        }
        normal = resolveBlocks(Config.metalBlocks);
        weak = resolveBlocks(Config.weakMetalBlocks);

        Set<ResourceLocation> ids = new HashSet<>();
        for (String name : Config.metalEntities) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id != null) ids.add(id);
        }
        entities = Set.copyOf(ids);

        normalFrom = Config.metalBlocks;
        weakFrom = Config.weakMetalBlocks;
        entitiesFrom = Config.metalEntities;
    }

    /**
     * Traduit une liste de noms en blocs.
     *
     * Un nom inconnu est ignore plutot que fatal : la config est ecrite a la main, et
     * une faute de frappe ne doit pas empecher le jeu de demarrer — c'est deja la
     * decision du validateur d'assets, qui signale sans casser.
     */
    private static Set<Block> resolveBlocks(List<String> names) {
        Set<Block> blocks = new HashSet<>();
        for (String name : names) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null) continue;
            if (BuiltInRegistries.BLOCK.containsKey(id)) {
                blocks.add(BuiltInRegistries.BLOCK.get(id));
            }
        }
        return Set.copyOf(blocks);
    }
}
