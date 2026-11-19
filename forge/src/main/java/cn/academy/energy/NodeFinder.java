package cn.academy.energy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Raccordement d'une machine a un noeud sans fil.
 *
 * <p>L'original laissait le joueur faire ce lien a la main depuis l'interface de sa machine : une
 * liste des noeuds a portee, et un mot de passe pour ceux qui en demandent un. Le port a d'abord fait
 * le lien tout seul, en cherchant le noeud le plus proche — c'etait un substitut, et il ne l'est plus :
 * l'onglet sans fil est la (voir {@code CGuiWireless}), et c'est lui qui decide. Le raccordement
 * automatique reste le filet : une machine qui n'a jamais rien choisi se raccroche au noeud le plus
 * proche, et une machine que le joueur a detachee reste detachee.
 *
 * <p>Le raccordement est mene par la <b>machine</b> et non par le noeud : le noeud ignore qui vient
 * s'y brancher, il ne fait que pousser et tirer de l'energie sur les machines qu'on lui a declarees.
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
     * Verifie le raccordement d'une machine : detache d'un noeud disparu, hors de portee ou plein.
     *
     * <p>Et rien de plus. Le port avait ajoute un filet — la machine se raccrochait d'elle-meme au
     * noeud le plus proche — et c'est ce filet que le joueur a fait retirer : « quand on pose une
     * machine au sol, elle n'essaie pas de se connecter automatiquement au reseau le plus proche. Je
     * veux qu'on soit oblige de le mettre a la main. » Le raccordement se fait dans l'onglet sans
     * fil, et nulle part ailleurs ; ce controle ne sert plus qu'a oublier un lien qui ne vaut plus
     * rien, et il ne peut pas en creer un.
     *
     * @return vrai si la machine est encore raccordee a un noeud apres l'appel
     */
    public static boolean ensureLinked(ServerLevel level, BlockPos pos) {
        ImagNetworkData data = ImagNetworkData.get(level);

        BlockPos nodePos = data.nodeOf(pos);
        if (nodePos == null) return false;

        NodeBlockEntity node = nodeAt(level, nodePos);
        if (node != null && accepts(node, pos, data)) return true;

        // Le noeud a ete casse, ou il ne peut plus accueillir cette machine.
        data.unlinkUser(pos);
        return false;
    }

    /**
     * Le nom du noeud auquel une machine est raccordee, ou {@code null} si elle ne l'est pas.
     *
     * <p>C'est ce que la console du developeur ecrit sous « Current Node: ». Le nom est celui que
     * le joueur a donne au noeud (voir {@code NodeBlockEntity.getNodeName}) : sans noeud il n'y a
     * rien a dire, et l'ecran ecrit « Not Connected ».
     */
    @Nullable
    public static String nodeName(ServerLevel level, BlockPos machinePos) {
        BlockPos nodePos = ImagNetworkData.get(level).nodeOf(machinePos);
        if (nodePos == null) return null;
        NodeBlockEntity node = nodeAt(level, nodePos);
        return node == null ? null : node.getNodeName();
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

    // ------------------------------------------------------------------
    // L'onglet sans fil : ce que le joueur voit, et ce qu'il decide
    // ------------------------------------------------------------------

    /**
     * Un noeud joignable, tel que l'onglet sans fil le montre.
     *
     * <p>{@code encrypted} dit s'il demande un mot de passe : c'est ce que l'original envoyait a
     * l'ecran pour decider d'afficher la petite cle et le champ de saisie. {@code room} dit s'il lui
     * reste une place — la seule chose qui puisse refuser un raccordement a portee. Le noeud deja lie
     * n'est pas dans la liste : l'ecran le montre a part, dans son cadre « Connected ».
     */
    public record Candidate(BlockPos pos, String name, boolean encrypted, boolean room) {}

    /** Ce que vaut une demande de raccordement : de quoi le dire au joueur. */
    public enum LinkOutcome { LINKED, WRONG_PASSWORD, NO_ROOM, OUT_OF_RANGE, NOT_FOUND }

    /**
     * Les noeuds joignables par une machine, du plus proche au plus loin.
     *
     * <p>C'est le {@code getNodesInRange} de l'original : la recherche dans vingt blocs, puis le
     * filtre sur la portee reelle du noeud — un noeud basique (portee neuf) ne raccroche rien a
     * quinze blocs, meme s'il a ete trouve par la recherche. L'original retirait aussi le noeud deja
     * lie, parce que son ecran le montre a part dans son cadre « Connected » ; le port fait pareil.
     */
    public static List<Candidate> candidates(ServerLevel level, BlockPos machinePos) {
        ImagNetworkData data = ImagNetworkData.get(level);
        BlockPos linked = data.nodeOf(machinePos);

        List<Candidate> found = new ArrayList<>();
        for (NodeBlockEntity node : BlockEntityScan.around(level, machinePos, SEARCH_RANGE,
                NodeBlockEntity.class)) {
            BlockPos pos = node.getBlockPos();
            if (pos.equals(linked)) continue;
            if (pos.distSqr(machinePos) > node.getRange() * node.getRange()) continue;

            found.add(new Candidate(pos, node.getNodeName(), !node.getPassword().isEmpty(),
                    data.userCount(pos) < node.getCapacity()));
        }
        found.sort(Comparator.comparingDouble(candidate -> candidate.pos().distSqr(machinePos)));
        return found;
    }

    /**
     * Raccorde une machine au noeud que le joueur a designe, avec le mot de passe qu'il a tape.
     *
     * <p>C'est le {@code linkUser} de l'original : la portee, la place, et le mot de passe du noeud.
     * C'est le SEUL chemin qui raccorde une machine : rien ne se raccorde tout seul, ni a la pose ni
     * au tick suivant.
     */
    public static LinkOutcome link(ServerLevel level, BlockPos machinePos, BlockPos nodePos,
                                   String password) {
        NodeBlockEntity node = nodeAt(level, nodePos);
        if (node == null) return LinkOutcome.NOT_FOUND;

        if (nodePos.distSqr(machinePos) > node.getRange() * node.getRange()) {
            return LinkOutcome.OUT_OF_RANGE;
        }
        if (!node.getPassword().isEmpty() && !node.getPassword().equals(password)) {
            return LinkOutcome.WRONG_PASSWORD;
        }

        ImagNetworkData data = ImagNetworkData.get(level);
        boolean sameNode = nodePos.equals(data.nodeOf(machinePos));
        if (!sameNode && data.userCount(nodePos) >= node.getCapacity()) {
            return LinkOutcome.NO_ROOM;
        }

        data.unlinkUser(machinePos);
        if (!data.linkUser(nodePos, machinePos)) return LinkOutcome.NO_ROOM;
        return LinkOutcome.LINKED;
    }

    /** Detache une machine. Rien ne la raccordera ensuite : c'est le joueur qui choisit. */
    public static void unlink(ServerLevel level, BlockPos machinePos) {
        ImagNetworkData.get(level).unlinkUser(machinePos);
    }

    // ------------------------------------------------------------------
    // L'onglet sans fil d'un NOEUD : le Matrix qu'il rejoint
    // ------------------------------------------------------------------

    /**
     * Les reseaux qu'un noeud peut rejoindre, du plus proche au plus loin.
     *
     * <p>C'est le {@code hFindNetworks} de l'original : la recherche dans vingt blocs, filtree par
     * {@link #canJoin} — donc sur la <b>portee du noeud</b> et sur celle du Matrix —, et le reseau deja
     * rejoint retire de la liste. Le nom d'un reseau est son SSID, et il compte comme « chiffre »
     * quand son Matrix demande un mot de passe.
     *
     * <p>Un Matrix sans SSID n'est pas un reseau : il n'apparait donc pas (chez l'original non plus,
     * ou le reseau nait de l'initialisation du Matrix).
     */
    public static List<Candidate> matrices(ServerLevel level, BlockPos nodePos) {
        ImagNetworkData data = ImagNetworkData.get(level);
        BlockPos linked = data.matrixOf(nodePos);
        NodeBlockEntity node = nodeAt(level, nodePos);
        double range = node == null ? SEARCH_RANGE : node.getRange();

        List<Candidate> found = new ArrayList<>();
        for (MatrixBlockEntity matrix : BlockEntityScan.around(level, nodePos, SEARCH_RANGE,
                MatrixBlockEntity.class)) {
            BlockPos pos = matrix.getBlockPos();
            if (pos.equals(linked)) continue;
            if (!canJoin(nodePos, matrix, range)) continue;

            found.add(new Candidate(pos, matrix.getSsid(), !matrix.getPassword().isEmpty(),
                    data.nodeCount(pos) < matrix.getCapacity()));
        }
        found.sort(Comparator.comparingDouble(candidate -> candidate.pos().distSqr(nodePos)));
        return found;
    }

    /**
     * Vrai si ce noeud peut rejoindre ce Matrix : un reseau doit exister, et les deux doivent
     * s'atteindre.
     *
     * <p>C'est la regle des deux cotes de l'original — sa boite de recherche est la portee du NOEUD,
     * puis le reseau verifie lui-meme que le noeud est dans SA portee — et c'est la seule definition,
     * parce qu'elle sert aux deux chemins : l'onglet sans fil liste ce qu'elle accepte, et le
     * raccordement automatique du noeud ne prend qu'elle. Separes, un noeud se liait a un reseau qu'il
     * n'aurait jamais du voir : cherche dans la portee maximale theorique d'un Matrix, il se raccordait
     * a un Matrix SANS reseau, des le premier tick. L'onglet cache ensuite le reseau deja rejoint —
     * donc l'ecran paraissait ne rien trouver, alors que le noeud etait raccorde sans qu'on le lui ait
     * demande.
     */
    public static boolean canJoin(BlockPos nodePos, MatrixBlockEntity matrix, double nodeRange) {
        if (!matrix.isInitialized()) return false;
        if (!matrix.canReach(nodePos)) return false;
        return matrix.reachSqr(nodePos) <= nodeRange * nodeRange;
    }

    /**
     * Rejoint un reseau, avec le mot de passe qu'on a tape.
     *
     * <p>C'est le {@code addNode} du {@code WirelessNet} de l'original, dans son ordre : le mot de
     * passe, la place libre dans le reseau, puis la portee du Matrix. Un noeud n'appartient qu'a un
     * seul reseau, donc en rejoindre un detache du precedent.
     */
    public static LinkOutcome linkMatrix(ServerLevel level, BlockPos nodePos, BlockPos matrixPos,
                                         String password) {
        MatrixBlockEntity matrix = matrixAt(level, matrixPos);
        if (matrix == null) return LinkOutcome.NOT_FOUND;

        if (!matrix.getPassword().isEmpty() && !matrix.getPassword().equals(password)) {
            return LinkOutcome.WRONG_PASSWORD;
        }

        ImagNetworkData data = ImagNetworkData.get(level);
        boolean sameNetwork = matrixPos.equals(data.matrixOf(nodePos));
        if (!sameNetwork && data.nodeCount(matrixPos) >= matrix.getCapacity()) {
            return LinkOutcome.NO_ROOM;
        }
        if (!matrix.canReach(nodePos)) return LinkOutcome.OUT_OF_RANGE;

        data.unlink(nodePos);
        if (!data.link(matrixPos, nodePos)) return LinkOutcome.NO_ROOM;
        return LinkOutcome.LINKED;
    }

    /** Detache un noeud de son reseau. Rien ne l'y raccrochera ensuite : c'est le joueur qui choisit. */
    public static void unlinkMatrix(ServerLevel level, BlockPos nodePos) {
        ImagNetworkData.get(level).unlink(nodePos);
    }

    /** Le SSID du reseau qu'un noeud a rejoint, ou {@code null}. */
    @Nullable
    public static String matrixName(ServerLevel level, BlockPos nodePos) {
        BlockPos matrixPos = ImagNetworkData.get(level).matrixOf(nodePos);
        if (matrixPos == null) return null;
        MatrixBlockEntity matrix = matrixAt(level, matrixPos);
        return matrix == null ? null : matrix.getSsid();
    }

    @Nullable
    private static MatrixBlockEntity matrixAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        return level.getBlockEntity(pos) instanceof MatrixBlockEntity matrix ? matrix : null;
    }
}
