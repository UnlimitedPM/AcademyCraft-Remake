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

    /**
     * Les minerais qui ne disent pas leur nom.
     *
     * <p>Deux cas, et chacun a sa raison :
     *
     * <ul>
     *   <li>{@code academy:constraint_metal} — le minerai de la maison, et l'original le
     *       reconnaissait par sa <b>classe</b> : son {@code BlockGenericOre} descendait de
     *       {@code BlockOre}, ce qui suffisait a son premier test. La 1.20.1 n'a plus de classe
     *       de minerai du tout, donc le nom du bloc est la seule chose qui reste a lire — et
     *       celui-ci ne dit pas « ore ».</li>
     *   <li>{@code minecraft:ancient_debris} — les debris antiques, qui sont le minerai de la
     *       netherite : pas le mot « ore » dans leur nom, et aucune etoile de minerai dans leurs
     *       etiquettes non plus. L'original ne les connaissait pas, ils datent de la 1.16.</li>
     * </ul>
     */
    private static final List<ResourceLocation> UNNAMED_ORES = List.of(
            ResourceLocation.fromNamespaceAndPath("academy", "constraint_metal"),
            ResourceLocation.withDefaultNamespace("ancient_debris"));

    private static Set<Block> normal = Set.of();
    private static Set<Block> weak = Set.of();
    private static Set<ResourceLocation> entities = Set.of();

    /**
     * Les blocs metalliques que l'ORIGINAL ne pouvait pas connaitre, et qui s'ajoutent a la config.
     *
     * <p>C'est le joueur qui les a demandes : « avec les mises a jour de minecraft, maintenant il y a
     * aussi le cuivre de present dans le jeu, avec enormement de variantes de bloc en cuivre, et dans
     * la logique on devrait pouvoir s'y aimanter aussi ». Le fer, lui, etait la depuis toujours — la
     * porte et la trappe en fer manquaient simplement a la liste de l'original, « pour aucune raison
     * apparente ».
     *
     * <p>Et ils sont ici, en CODE, en plus d'etre dans les listes par defaut de la config, parce que
     * la config d'une installation deja ecrite ne les connaitra jamais : Forge ne complete pas une
     * liste existante, il ne remplit que les cles absentes. C'est la lecon du crochet magnetique, qui
     * avait echoue exactement la — voir {@link #isMetallic}.
     */
    private static final Set<Block> NEW_STRONG_METALS = Set.of(
            net.minecraft.world.level.block.Blocks.IRON_DOOR,
            net.minecraft.world.level.block.Blocks.IRON_TRAPDOOR);

    /**
     * Les memes, mais du metal non travaille : ils suivent le sort des minerais de fer.
     *
     * <p>Une pepite brute, un minerai, cela ne s'attrape pas du premier coup — l'original le disait
     * deja de son minerai de fer, et le bloc de fer brut et le minerai de cuivre sont exactement la
     * meme chose en plus recent.
     */
    private static final Set<Block> NEW_WEAK_METALS = Set.of(
            net.minecraft.world.level.block.Blocks.RAW_IRON_BLOCK,
            net.minecraft.world.level.block.Blocks.RAW_COPPER_BLOCK,
            net.minecraft.world.level.block.Blocks.COPPER_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_COPPER_ORE);

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
        return normal.contains(block) || NEW_STRONG_METALS.contains(block) || isWorkedCopper(block);
    }

    public static boolean isWeakMetalBlock(Block block) {
        ensureBuilt();
        return weak.contains(block) || NEW_WEAK_METALS.contains(block);
    }

    /**
     * Vrai si ce bloc est du cuivre <b>travaille</b> : le metal lui-meme, taille, en escalier, en
     * dalle, oxyde ou cire — bref, tout ce que la 1.17 a apporte.
     *
     * <p>Il se reconnait a son <b>nom</b>, et pas a une liste, exactement comme les minerais se
     * reconnaissent au mot « ore » : le cuivre a quatre etats d'oxydation, chacun decline en bloc,
     * bloc taille, escalier, dalle et dalle d'escalier, et chacun de ces cinq existe aussi en version
     * ciree. Une liste ecrite a la main en oublierait, alors que le nom les porte tous — et il portera
     * aussi ceux des versions suivantes, et ceux des autres mods.
     *
     * <p>Deux familles en sont ecartees, et c'est voulu : le cuivre <b>brut</b> et le <b>minerai</b> de
     * cuivre, qui sont du metal non travaille et rejoignent le minerai de fer parmi les blocs qui
     * demandent soixante pour cent d'experience. Voir {@link #NEW_WEAK_METALS}.
     *
     * <p>Et elle est <b>publique</b> parce que le railgun s'en sert aussi : le joueur a demande
     * qu'on « rajoute le cuivre pour le railgun », et sa munition de cuivre se reconnait par cette
     * meme regle — les deux competences ne peuvent donc pas diverger sur ce qu'est « du cuivre ».
     */
    public static boolean isWorkedCopper(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return false;
        String name = id.getPath();
        return name.contains("copper") && !name.contains("raw") && !name.contains("ore");
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

    /**
     * Vrai si cette entite est de celles que l'electromaster peut attirer.
     *
     * <p>Le crochet magnetique du mod l'est <b>par nature</b>, avant meme de regarder la config, et
     * cette precaution a une raison : sa liste vit dans le fichier de configuration du joueur, qui
     * a ete ecrit bien avant que le crochet existe. Forge ne rajoute pas une entree a une liste
     * existante — il ne complete que les cles absentes — donc un joueur qui met son mod a jour
     * aurait un crochet qu'on ne peut pas accrocher, sans que rien ne le lui dise. L'original
     * portait son entree dans cette liste ({@code academy:EntityMagHook}), et le port la garde
     * aussi, pour une installation neuve.
     */
    public static boolean isMetallic(Entity entity) {
        if (entity instanceof cn.academy.entity.EntityMagHook) return true;
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
     *
     * <p>Et deux minerais lui echappent tout a fait, parce qu'ils ne disent rien : son propre
     * minerai, que l'original tenait par sa classe, et les debris antiques. Voir
     * {@link #UNNAMED_ORES}.
     */
    public static boolean isOreBlock(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        if (UNNAMED_ORES.contains(id)) return true;
        if (id.getPath().contains("ore")) return true;
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
