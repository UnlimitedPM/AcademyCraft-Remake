package cn.academy.energy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Liens entre les Matrix et les noeuds sans fil d'un monde.
 *
 * Remplace {@code WiWorldData} de la 1.12.2, qui etait un {@code WorldSavedData}.
 * En 1.20.1 l'equivalent est {@link SavedData}, attache au niveau par
 * {@link #get(ServerLevel)}.
 *
 * La table est volontairement separee en deux sens :
 * <ul>
 *   <li>{@code nodesByMatrix} est la donnee utile, elle est sauvegardee ;</li>
 *   <li>{@code matrixByNode} est un index inverse, reconstruit au chargement,
 *       qui evite de parcourir tous les reseaux quand un noeud demande a qui il
 *       est raccorde.</li>
 * </ul>
 *
 * Le NBT est lisible et ecrivable sans niveau ni block entity : la classe se
 * teste donc entierement en JUnit (voir {@code ImagNetworkDataTest}).
 */
public class ImagNetworkData extends SavedData {

    /** Nom du fichier de sauvegarde, dans {@code data/} du monde. */
    public static final String FILE_ID = "academy_imag_network";

    private static final String TAG_MATRICES = "matrices";
    private static final String TAG_MATRIX_POS = "matrix";
    private static final String TAG_NODES = "nodes";
    private static final String TAG_NODE_POS = "node";

    /** Noeuds raccordes a chaque Matrix. Indexe par position du Matrix. */
    private final Map<BlockPos, Set<BlockPos>> nodesByMatrix = new HashMap<>();

    /** Index inverse : Matrix auquel chaque noeud est raccorde. */
    private final Map<BlockPos, BlockPos> matrixByNode = new HashMap<>();

    // ------------------------------------------------------------------
    // Acces au niveau
    // ------------------------------------------------------------------

    public static ImagNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ImagNetworkData::load, ImagNetworkData::new, FILE_ID);
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    /** Noeuds raccordes a ce Matrix, dans l'ordre d'ajout. */
    public Set<BlockPos> nodesOf(BlockPos matrix) {
        Set<BlockPos> nodes = nodesByMatrix.get(matrix);
        return nodes == null ? Collections.emptySet() : Collections.unmodifiableSet(nodes);
    }

    /** Matrix auquel ce noeud est raccorde, ou {@code null}. */
    public BlockPos matrixOf(BlockPos node) {
        return matrixByNode.get(node);
    }

    public boolean isLinked(BlockPos node) {
        return matrixByNode.containsKey(node);
    }

    public int nodeCount(BlockPos matrix) {
        return nodesOf(matrix).size();
    }

    /** Nombre de Matrix ayant au moins un noeud. */
    public int matrixCount() {
        return nodesByMatrix.size();
    }

    /** Toutes les positions de Matrix connues (pratique pour les tests et le debogage). */
    public List<BlockPos> matrices() {
        return new ArrayList<>(nodesByMatrix.keySet());
    }

    // ------------------------------------------------------------------
    // Ecriture
    // ------------------------------------------------------------------

    /**
     * Raccorde un noeud a un Matrix. Un noeud n'est jamais raccorde a deux
     * Matrix : s'il l'etait deja, il est d'abord detache de l'ancien.
     *
     * @return vrai si le raccordement a change quelque chose
     */
    public boolean link(BlockPos matrix, BlockPos node) {
        BlockPos previous = matrixByNode.get(node);
        if (matrix.equals(previous)) return false;

        if (previous != null) removeNodeFromMatrix(previous, node);

        nodesByMatrix.computeIfAbsent(matrix.immutable(), k -> new LinkedHashSet<>()).add(node.immutable());
        matrixByNode.put(node.immutable(), matrix.immutable());
        setDirty();
        return true;
    }

    /** Detache un noeud, quel que soit le Matrix auquel il etait raccorde. */
    public boolean unlink(BlockPos node) {
        BlockPos matrix = matrixByNode.remove(node);
        if (matrix == null) return false;
        removeNodeFromMatrix(matrix, node);
        setDirty();
        return true;
    }

    /**
     * Supprime un Matrix et detache tous ses noeuds.
     * @return les noeuds qui etaient raccordes, pour que l'appelant puisse les
     *         remettre en recherche d'un nouveau Matrix
     */
    public Set<BlockPos> removeMatrix(BlockPos matrix) {
        Set<BlockPos> removed = nodesByMatrix.remove(matrix);
        if (removed == null) return Collections.emptySet();

        for (BlockPos node : removed) {
            matrixByNode.remove(node);
        }
        setDirty();
        return new LinkedHashSet<>(removed);
    }

    private void removeNodeFromMatrix(BlockPos matrix, BlockPos node) {
        Set<BlockPos> nodes = nodesByMatrix.get(matrix);
        if (nodes == null) return;
        nodes.remove(node);
        if (nodes.isEmpty()) nodesByMatrix.remove(matrix);
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag matrices = new ListTag();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : nodesByMatrix.entrySet()) {
            CompoundTag matrixTag = new CompoundTag();
            matrixTag.putLong(TAG_MATRIX_POS, entry.getKey().asLong());

            ListTag nodes = new ListTag();
            for (BlockPos node : entry.getValue()) {
                CompoundTag nodeTag = new CompoundTag();
                nodeTag.putLong(TAG_NODE_POS, node.asLong());
                nodes.add(nodeTag);
            }
            matrixTag.put(TAG_NODES, nodes);
            matrices.add(matrixTag);
        }
        tag.put(TAG_MATRICES, matrices);
        return tag;
    }

    public static ImagNetworkData load(CompoundTag tag) {
        ImagNetworkData data = new ImagNetworkData();
        ListTag matrices = tag.getList(TAG_MATRICES, Tag.TAG_COMPOUND);

        for (int i = 0; i < matrices.size(); i++) {
            CompoundTag matrixTag = matrices.getCompound(i);
            BlockPos matrix = BlockPos.of(matrixTag.getLong(TAG_MATRIX_POS));

            ListTag nodes = matrixTag.getList(TAG_NODES, Tag.TAG_COMPOUND);
            for (int j = 0; j < nodes.size(); j++) {
                BlockPos node = BlockPos.of(nodes.getCompound(j).getLong(TAG_NODE_POS));
                // On passe par link() pour que les deux index restent coherents,
                // meme si le fichier sauvegarde est incoherent.
                data.link(matrix, node);
            }
        }

        // Le chargement n'est pas une modification : rien a sauvegarder tant que
        // personne ne touche aux liens.
        data.setDirty(false);
        return data;
    }
}
