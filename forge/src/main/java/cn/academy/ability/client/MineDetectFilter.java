package cn.academy.ability.client;

import cn.academy.client.hud.HudConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le minerai que la detection met en <b>vert</b>, et lui seul.
 *
 * <p>Demande du joueur le 26/10 : pouvoir filtrer ce que l'eclat montre. Les minerais qu'il n'a
 * pas choisis gardent leur bleu — l'eclat reste donc lisible, on ne cache rien — et celui qu'il a
 * designe passe au vert, pour le retrouver d'un coup d'oeil dans une paroi.
 *
 * <p>Le choix appartient au <b>client</b> : l'eclat est dessine chez le joueur, sur des minerais
 * qu'il balaie lui-meme (voir {@code MineDetectOverlay}), et le serveur n'a pas a savoir ce qui
 * l'interesse. Il vit donc dans la config du client, a cote de la place du HUD — voir
 * {@link HudConfig#mineDetectOre}.
 *
 * <p>Il est range en <b>cle d'enregistrement</b> ({@code minecraft:iron_ore}) et non en numero de
 * bloc : c'est la seule forme qui survit a un rechargement de ressources, a un pack de donnees, et
 * a un monde ouvert avec d'autres mods que celui ou le choix a ete fait.
 */
public final class MineDetectFilter {

    private MineDetectFilter() {}

    /** La cle du minerai choisi, ou une chaine vide quand le joueur n'a rien choisi. */
    public static String selected() {
        return HudConfig.mineDetectOre();
    }

    /** Choisit un minerai, ou aucun avec une chaine vide, et l'ecrit dans la config. */
    public static void select(String id) {
        HudConfig.writeMineDetectOre(id);
    }

    /** La cle d'enregistrement d'un bloc, telle qu'elle est ecrite dans la config. */
    public static String keyOf(BlockState state) {
        if (state == null) return "";
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "" : key.toString();
    }

    /** Ce bloc est-il celui que le joueur a choisi ? */
    public static boolean isSelected(BlockState state) {
        String id = selected();
        return !id.isEmpty() && id.equals(keyOf(state));
    }

    /**
     * Le bloc choisi, resolu en objet, ou {@code null} si le joueur n'a rien choisi — ou si la
     * cle ne correspond a aucun bloc (un mod retire, un pack de donnees change).
     *
     * <p>C'est la forme que le balayage emploie : {@link #isSelected} refaisait une lecture de
     * registre ET une chaine par minerai trouve, la ou une comparaison de bloc ne coute rien.
     * La resolution, elle, se fait une fois par balayage.
     */
    public static Block selectedBlock() {
        String id = selected();
        if (id.isEmpty()) return null;
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.BLOCK.getOptional(key).orElse(null);
    }
}
