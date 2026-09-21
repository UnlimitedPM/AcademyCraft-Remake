package cn.academy.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Raccordement automatique d'une machine a un noeud sans fil.
 *
 * L'original laissait le joueur faire ce lien a la main depuis l'interface du
 * noeud, avec un SSID et un mot de passe. Ces interfaces ne sont pas portees :
 * la machine cherche donc son noeud toute seule, comme un noeud cherche son
 * Matrix. C'est un substitut assume, mais il a l'avantage d'etre verifiable.
 *
 * Le raccordement est mene par la <b>machine</b> et non par le noeud : le noeud
 * ignore qui vient s'y brancher, il ne fait que pousser et tirer de l'energie
 * sur les machines qu'on lui a declarees.
 */
public final class NodeFinder {

    private NodeFinder() {}

    /**
     * Rayon de recherche d'un noeud, en blocs.
     *
     * Repris de {@code WirelessHelper.getNodesInRange}, qui cherchait dans 20
     * blocs puis filtrait sur la portee reelle du noeud. La portee reelle est
     * verifiee ici aussi : un noeud basique (portee 9) ne raccroche rien a 15
     * blocs, meme s'il a ete trouve par la recherche.
     */
    public static final double SEARCH_RANGE = 20.0d;

    /**
     * Verifie le raccordement d'une machine et le repare au besoin : detache
     * d'un noeud disparu, hors de portee ou plein, puis cherche le noeud le plus
     * proche capable de l'accueillir.
     *
     * @param pos position de la machine a raccorder
     * @return vrai si la machine est raccordee a un noeud apres l'appel
     */
    public static boolean ensureLinked(ServerLevel level, BlockPos pos) {
        ImagNetworkData data = ImagNetworkData.get(level);

        BlockPos nodePos = data.nodeOf(pos);
        if (nodePos != null) {
            NodeBlockEntity node = nodeAt(level, nodePos);
            if (node != null && accepts(node, pos, data)) return true;
            data.unlinkUser(pos);
        }

        NodeBlockEntity found = BlockEntityScan.nearest(level, pos, SEARCH_RANGE,
                NodeBlockEntity.class, node -> accepts(node, pos, data));
        return found != null && data.linkUser(found.getBlockPos(), pos);
    }

    /** Detache une machine de son noeud, quel qu'il soit. */
    public static void detach(ServerLevel level, BlockPos pos) {
        ImagNetworkData.get(level).unlinkUser(pos);
    }

    /**
     * Un noeud accepte une machine s'il est a portee et qu'il lui reste de la
     * place. Un noeud plein refuse : c'est ce qui pousse la machine a chercher
     * ailleurs plutot qu'a s'entasser sur le premier trouve.
     */
    public static boolean accepts(NodeBlockEntity node, BlockPos machinePos, ImagNetworkData data) {
        double range = node.getRange();
        if (node.getBlockPos().distSqr(machinePos) > range * range) return false;
        return data.userCount(node.getBlockPos()) < node.getCapacity();
    }

    @Nullable
    private static NodeBlockEntity nodeAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        return level.getBlockEntity(pos) instanceof NodeBlockEntity node ? node : null;
    }
}
