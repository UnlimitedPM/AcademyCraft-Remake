package cn.academy.energy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Les raccordements sans fil d'un monde.
 *
 * Remplace {@code WiWorldData} de la 1.12.2, qui etait un {@code WorldSavedData}.
 * En 1.20.1 l'equivalent est {@link SavedData}, attache au niveau par
 * {@link #get(ServerLevel)}.
 *
 * <h2>Deux tables</h2>
 *
 * <ul>
 *   <li>Matrix vers noeuds : c'est le reseau energetique proprement dit, celui
 *       qui equilibre les energies ;</li>
 *   <li>noeud vers utilisateurs : les generateurs et recepteurs raccordes a un
 *       noeud. Une meme position n'y figure qu'une fois, puisque un bloc est
 *       soit generateur soit recepteur, jamais les deux. Le type se reconnait a
 *       l'interface implementee par le block entity.</li>
 * </ul>
 *
 * Les deux tables sont tenues dans les deux sens : {@code byOwner} est la donnee
 * sauvegardee, {@code ownerOf} est un index inverse reconstruit au chargement.
 * Sans lui il faudrait parcourir tous les raccordements du monde pour repondre a
 * "a qui cette machine est-elle raccordee ?", question posee a chaque fois qu'un
 * bloc est casse.
 */
public class ImagNetworkData extends SavedData {

    /** Nom du fichier de sauvegarde, dans {@code data/} du monde. */
    public static final String FILE_ID = "academy_imag_network";

    private static final String TAG_MATRICES = "matrices";
    private static final String TAG_MATRIX_POS = "matrix";
    private static final String TAG_NODES = "nodes";
    private static final String TAG_NODE_POS = "node";

    private static final String TAG_NODE_USERS = "node_users";
    private static final String TAG_USER_POS = "user";

    /** Noeuds raccordes a chaque Matrix. */
    private final Links matrixToNodes = new Links();

    /** Generateurs et recepteurs raccordes a chaque noeud. */
    private final Links nodeToUsers = new Links();

    // ------------------------------------------------------------------
    // Acces au niveau
    // ------------------------------------------------------------------

    public static ImagNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ImagNetworkData::load, ImagNetworkData::new, FILE_ID);
    }

    // ------------------------------------------------------------------
    // Matrix vers noeuds
    // ------------------------------------------------------------------

    /** Noeuds raccordes a ce Matrix. */
    public Set<BlockPos> nodesOf(BlockPos matrix) {
        return matrixToNodes.childrenOf(matrix);
    }

    /** Matrix auquel ce noeud est raccorde, ou {@code null}. */
    public BlockPos matrixOf(BlockPos node) {
        return matrixToNodes.ownerOf(node);
    }

    public boolean isLinked(BlockPos node) {
        return matrixToNodes.isLinked(node);
    }

    public int nodeCount(BlockPos matrix) {
        return matrixToNodes.childCount(matrix);
    }

    /** Nombre de Matrix ayant au moins un noeud. */
    public int matrixCount() {
        return matrixToNodes.ownerCount();
    }

    /**
     * Raccorde un noeud a un Matrix. Un noeud n'est jamais raccorde a deux
     * Matrix : s'il l'etait deja, il est d'abord detache de l'ancien.
     *
     * @return vrai si le raccordement a change quelque chose
     */
    public boolean link(BlockPos matrix, BlockPos node) {
        return matrixToNodes.link(matrix, node);
    }

    /** Detache un noeud de son Matrix, quel qu'il soit. */
    public boolean unlink(BlockPos node) {
        return matrixToNodes.unlink(node);
    }

    /**
     * Supprime un Matrix et detache tous ses noeuds.
     *
     * @return les noeuds qui etaient raccordes, pour que l'appelant puisse les
     *         remettre en recherche d'un nouveau Matrix
     */
    public Set<BlockPos> removeMatrix(BlockPos matrix) {
        return matrixToNodes.removeOwner(matrix);
    }

    /** Toutes les positions de Matrix connues. */
    public List<BlockPos> matrices() {
        return matrixToNodes.owners();
    }

    // ------------------------------------------------------------------
    // Noeud vers generateurs / recepteurs
    // ------------------------------------------------------------------

    /** Generateurs et recepteurs raccordes a ce noeud. */
    public Set<BlockPos> usersOf(BlockPos node) {
        return nodeToUsers.childrenOf(node);
    }

    /** Noeud auquel cette machine est raccordee, ou {@code null}. */
    public BlockPos nodeOf(BlockPos user) {
        return nodeToUsers.ownerOf(user);
    }

    public boolean isUserLinked(BlockPos user) {
        return nodeToUsers.isLinked(user);
    }

    public int userCount(BlockPos node) {
        return nodeToUsers.childCount(node);
    }

    /**
     * Raccorde un generateur ou un recepteur a un noeud. Une machine n'est
     * jamais raccordee a deux noeuds.
     *
     * @return vrai si le raccordement a change quelque chose
     */
    public boolean linkUser(BlockPos node, BlockPos user) {
        return nodeToUsers.link(node, user);
    }

    /** Detache une machine de son noeud, quel qu'il soit. */
    public boolean unlinkUser(BlockPos user) {
        return nodeToUsers.unlink(user);
    }

    /**
     * Supprime un noeud, detache ses machines et le retire de son Matrix.
     *
     * @return les machines qui etaient raccordees, pour qu'elles puissent s'en
     *         chercher un autre
     */
    public Set<BlockPos> removeNode(BlockPos node) {
        Set<BlockPos> detached = nodeToUsers.removeOwner(node);
        matrixToNodes.unlink(node);
        return detached;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(TAG_MATRICES, matrixToNodes.save(TAG_MATRIX_POS, TAG_NODES, TAG_NODE_POS));
        tag.put(TAG_NODE_USERS, nodeToUsers.save(TAG_NODE_POS, TAG_NODES, TAG_USER_POS));
        return tag;
    }

    public static ImagNetworkData load(CompoundTag tag) {
        ImagNetworkData data = new ImagNetworkData();
        data.matrixToNodes.load(tag.getList(TAG_MATRICES, Tag.TAG_COMPOUND),
                TAG_MATRIX_POS, TAG_NODES, TAG_NODE_POS);
        data.nodeToUsers.load(tag.getList(TAG_NODE_USERS, Tag.TAG_COMPOUND),
                TAG_NODE_POS, TAG_NODES, TAG_USER_POS);

        // Le chargement n'est pas une modification : rien a sauvegarder tant que
        // personne ne touche aux raccordements.
        data.setDirty(false);
        return data;
    }

    // ------------------------------------------------------------------
    // Table de raccordements, dans les deux sens
    // ------------------------------------------------------------------

    /**
     * Une table proprietaire vers enfants, plus son index inverse.
     *
     * Les deux sens sont maintenus ensemble et jamais separes : c'est la seule
     * facon d'eviter qu'ils divergent, et c'est ce qui permet de reparer un
     * fichier de sauvegarde incoherent au chargement plutot que de le charger
     * tel quel.
     */
    private static final class Links {

        private final java.util.Map<BlockPos, Set<BlockPos>> byOwner = new java.util.HashMap<>();
        private final java.util.Map<BlockPos, BlockPos> ownerOf = new java.util.HashMap<>();

        Set<BlockPos> childrenOf(BlockPos owner) {
            Set<BlockPos> children = byOwner.get(owner);
            return children == null ? Collections.emptySet() : Collections.unmodifiableSet(children);
        }

        BlockPos ownerOf(BlockPos child) {
            return ownerOf.get(child);
        }

        boolean isLinked(BlockPos child) {
            return ownerOf.containsKey(child);
        }

        int childCount(BlockPos owner) {
            return childrenOf(owner).size();
        }

        int ownerCount() {
            return byOwner.size();
        }

        List<BlockPos> owners() {
            return new ArrayList<>(byOwner.keySet());
        }

        boolean link(BlockPos owner, BlockPos child) {
            BlockPos previous = ownerOf.get(child);
            if (owner.equals(previous)) return false;

            if (previous != null) detach(previous, child);

            byOwner.computeIfAbsent(owner.immutable(), k -> new LinkedHashSet<>()).add(child.immutable());
            ownerOf.put(child.immutable(), owner.immutable());
            return true;
        }

        boolean unlink(BlockPos child) {
            BlockPos owner = ownerOf.remove(child);
            if (owner == null) return false;
            detach(owner, child);
            return true;
        }

        Set<BlockPos> removeOwner(BlockPos owner) {
            Set<BlockPos> removed = byOwner.remove(owner);
            if (removed == null) return Collections.emptySet();

            for (BlockPos child : removed) ownerOf.remove(child);
            return new LinkedHashSet<>(removed);
        }

        private void detach(BlockPos owner, BlockPos child) {
            Set<BlockPos> children = byOwner.get(owner);
            if (children == null) return;
            children.remove(child);
            if (children.isEmpty()) byOwner.remove(owner);
        }

        ListTag save(String ownerKey, String childrenKey, String childKey) {
            ListTag list = new ListTag();
            for (java.util.Map.Entry<BlockPos, Set<BlockPos>> entry : byOwner.entrySet()) {
                CompoundTag ownerTag = new CompoundTag();
                ownerTag.putLong(ownerKey, entry.getKey().asLong());

                ListTag children = new ListTag();
                for (BlockPos child : entry.getValue()) {
                    CompoundTag childTag = new CompoundTag();
                    childTag.putLong(childKey, child.asLong());
                    children.add(childTag);
                }
                ownerTag.put(childrenKey, children);
                list.add(ownerTag);
            }
            return list;
        }

        void load(ListTag owners, String ownerKey, String childrenKey, String childKey) {
            for (int i = 0; i < owners.size(); i++) {
                CompoundTag ownerTag = owners.getCompound(i);
                BlockPos owner = BlockPos.of(ownerTag.getLong(ownerKey));

                ListTag children = ownerTag.getList(childrenKey, Tag.TAG_COMPOUND);
                for (int j = 0; j < children.size(); j++) {
                    BlockPos child = BlockPos.of(children.getCompound(j).getLong(childKey));
                    // On passe par link() plutot que de remplir les deux index a
                    // la main : un fichier incoherent, par exemple le meme enfant
                    // declare sous deux proprietaires, est ainsi nettoye.
                    link(owner, child);
                }
            }
        }
    }
}
