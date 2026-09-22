package cn.academy.ability.electromaster;

import cn.academy.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ce que l'electromaster peut attirer : les blocs et les entites metalliques, et ce que
 * l'oeil de la detection de minerais reperera.
 *
 * <p>Portage de {@code CatElectromaster} : l'original tenait trois listes dans sa
 * config — blocs franchement metalliques, blocs faiblement metalliques, entites
 * metalliques — et deux competences ne visaient que cela. Les listes sont ici aussi en
 * config, aux memes valeurs par defaut, avec les noms de la 1.20.1.
 *
 * <p>Deux listes de blocs et pas une, parce que l'original distinguait les deux : sous
 * soixante pour cent d'experience, un debutant ne s'accroche qu'aux blocs franchement
 * metalliques. Un rail, oui ; une machine ou un minerai de fer, pas encore.
 *
 * <p>Et une quatrieme classification, qui n'a pas de liste : ce qui est un <b>minerai</b>.
 * L'original le demandait au dictionnaire de minerais, en cherchant le mot « ore » dans ses
 * noms — le port demande la meme chose au nom d'enregistrement du bloc, ce qui marche pour
 * les minerais de la 1.20.1 comme pour ceux des autres mods.
 */
public final class MetalTargets {

    /** Experience a partir de laquelle les blocs faiblement metalliques accrochent. */
    public static final float WEAK_EXP = 0.6f;

    /**
     * Les etiquettes de minerais de la 1.20.1.
     *
     * Elles ne servent qu'a couvrir ce que le nom ne dit pas — un bloc qui serait un minerai
     * sans le mot dans son identifiant — mais elles ne coutent rien.
     */
    private static final List<net.minecraft.tags.TagKey<Block>> ORE_TAGS = List.of(
            BlockTags.COAL_ORES, BlockTags.COPPER_ORES, BlockTags.DIAMOND_ORES,
            BlockTags.EMERALD_ORES, BlockTags.GOLD_ORES, BlockTags.IRON_ORES,
            BlockTags.LAPIS_ORES, BlockTags.REDSTONE_ORES);

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

    /**
     * Vrai si ce bloc est un minerai.
     *
     * Portage de {@code CatElectromaster.isOreBlock} : l'original regardait la classe du
     * bloc, puis tous les noms que le dictionnaire de minerais lui donnait, et retenait ceux
     * qui contenaient « ore ». Le port regarde le nom d'enregistrement, ce qui donne le meme
     * resultat pour les minerais de la 1.20.1 — <code>iron_ore</code>,
     * <code>deepslate_iron_ore</code>, <code>nether_quartz_ore</code> — et pour ceux des
     * autres mods, qui suivent la meme convention. Les etiquettes completes viennent en
     * secours, pour un minerai dont le nom ne dirait rien.
     */
    public static boolean isOreBlock(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id != null && id.getPath().contains("ore")) return true;
        for (net.minecraft.tags.TagKey<Block> tag : ORE_TAGS) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    /**
     * Le palier de pioche d'un bloc, transpose des niveaux de la 1.12.2.
     *
     * C'est ce que la detection de minerais colore quand elle est complete : un minerai de
     * charbon ne se creuse pas comme un minerai de diamant. Les etiquettes de la 1.20.1
     * disent quelle pioche est <b>necessaire</b>, dans l'ordre : rien, la pierre, le fer, le
     * diamant — les quatre niveaux de l'original.
     */
    public static int harvestTier(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
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
