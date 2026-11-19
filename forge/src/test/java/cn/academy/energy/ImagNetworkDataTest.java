package cn.academy.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du registre de liens Matrix &lt;-&gt; noeuds.
 *
 * La persistance s'ecrit et se relit sur un {@link CompoundTag}, sans niveau ni
 * block entity : tout se teste donc hors jeu, y compris le cas des fichiers de
 * sauvegarde incoherents.
 */
class ImagNetworkDataTest {

    private static final BlockPos MATRIX_A = new BlockPos(0, 64, 0);
    private static final BlockPos MATRIX_B = new BlockPos(100, 64, 100);
    private static final BlockPos NODE_1 = new BlockPos(5, 64, 5);
    private static final BlockPos NODE_2 = new BlockPos(6, 64, 6);

    // ------------------------------------------------------------------
    // Raccordement
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un noeud raccorde est retrouve dans les deux sens")
    void linkIsVisibleBothWays() {
        ImagNetworkData data = new ImagNetworkData();

        assertTrue(data.link(MATRIX_A, NODE_1));

        assertEquals(1, data.nodeCount(MATRIX_A));
        assertTrue(data.nodesOf(MATRIX_A).contains(NODE_1));
        assertEquals(MATRIX_A, data.matrixOf(NODE_1));
        assertTrue(data.isLinked(NODE_1));
    }

    @Test
    @DisplayName("raccorder deux fois au meme Matrix ne change rien")
    void relinkToSameMatrixIsNoop() {
        ImagNetworkData data = new ImagNetworkData();
        data.link(MATRIX_A, NODE_1);

        assertFalse(data.link(MATRIX_A, NODE_1), "le second appel doit signaler qu'il ne fait rien");
        assertEquals(1, data.nodeCount(MATRIX_A));
    }

    @Test
    @DisplayName("un noeud ne peut appartenir qu'a un seul Matrix")
    void nodeMovesToTheNewMatrix() {
        ImagNetworkData data = new ImagNetworkData();
        data.link(MATRIX_A, NODE_1);

        assertTrue(data.link(MATRIX_B, NODE_1));

        assertEquals(0, data.nodeCount(MATRIX_A), "l'ancien Matrix doit etre vide");
        assertEquals(1, data.nodeCount(MATRIX_B));
        assertEquals(MATRIX_B, data.matrixOf(NODE_1));
    }

    @Test
    @DisplayName("un Matrix vide de ses noeuds disparait du registre")
    void emptyMatrixIsForgotten() {
        ImagNetworkData data = new ImagNetworkData();
        data.link(MATRIX_A, NODE_1);

        assertTrue(data.unlink(NODE_1));

        assertEquals(0, data.matrixCount(), "un Matrix sans noeud n'a plus rien a stocker");
        assertNull(data.matrixOf(NODE_1));
        assertFalse(data.isLinked(NODE_1));
    }

    @Test
    @DisplayName("detacher un noeud inconnu ne fait rien")
    void unlinkUnknownNode() {
        ImagNetworkData data = new ImagNetworkData();

        assertFalse(data.unlink(NODE_1));
        assertEquals(0, data.matrixCount());
    }

    @Test
    @DisplayName("supprimer un Matrix detache tous ses noeuds et les renvoie")
    void removeMatrixReturnsItsNodes() {
        ImagNetworkData data = new ImagNetworkData();
        data.link(MATRIX_A, NODE_1);
        data.link(MATRIX_A, NODE_2);
        data.link(MATRIX_B, new BlockPos(200, 64, 200));

        var orphaned = data.removeMatrix(MATRIX_A);

        assertEquals(2, orphaned.size());
        assertTrue(orphaned.contains(NODE_1));
        assertTrue(orphaned.contains(NODE_2));
        assertFalse(data.isLinked(NODE_1), "les noeuds orphelins ne doivent plus etre raccordes");
        assertEquals(1, data.nodeCount(MATRIX_B), "l'autre Matrix ne doit pas bouger");
    }

    @Test
    @DisplayName("supprimer un Matrix inconnu renvoie un ensemble vide")
    void removeUnknownMatrix() {
        ImagNetworkData data = new ImagNetworkData();

        assertTrue(data.removeMatrix(MATRIX_A).isEmpty());
    }

    @Test
    @DisplayName("les positions sont copiees : modifier l'objet d'origine ne casse rien")
    void positionsAreDefensivelyCopied() {
        ImagNetworkData data = new ImagNetworkData();
        BlockPos mutableEquivalent = new BlockPos(3, 64, 3);

        data.link(mutableEquivalent, NODE_1);

        // BlockPos est immuable, mais on verifie tout de meme que la recherche
        // fonctionne avec une instance differente mais egale.
        assertEquals(1, data.nodeCount(new BlockPos(3, 64, 3)));
        assertTrue(data.isLinked(new BlockPos(5, 64, 5)));
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Test
    @DisplayName("les liens survivent a un aller-retour NBT")
    void linksSurviveNbtRoundTrip() {
        ImagNetworkData source = new ImagNetworkData();
        source.link(MATRIX_A, NODE_1);
        source.link(MATRIX_A, NODE_2);
        source.link(MATRIX_B, new BlockPos(200, 70, 200));

        ImagNetworkData restored = ImagNetworkData.load(source.save(new CompoundTag()));

        assertEquals(2, restored.nodeCount(MATRIX_A));
        assertEquals(1, restored.nodeCount(MATRIX_B));
        assertTrue(restored.nodesOf(MATRIX_A).contains(NODE_1));
        assertTrue(restored.nodesOf(MATRIX_A).contains(NODE_2));
        assertEquals(MATRIX_B, restored.matrixOf(new BlockPos(200, 70, 200)));
    }

    @Test
    @DisplayName("une donnee vide se sauvegarde et se recharge sans erreur")
    void emptyDataRoundTrip() {
        ImagNetworkData restored = ImagNetworkData.load(new ImagNetworkData().save(new CompoundTag()));

        assertEquals(0, restored.matrixCount());
    }

    @Test
    @DisplayName("un fichier incoherent (meme noeud sous deux Matrix) est nettoye au chargement")
    void inconsistentSaveIsRepaired() {
        // On fabrique a la main ce qu'un vieux fichier pourrait contenir : le
        // meme noeud declare sous deux Matrix.
        CompoundTag nodeTag = new CompoundTag();
        nodeTag.putLong("node", NODE_1.asLong());

        var nodes = new net.minecraft.nbt.ListTag();
        nodes.add(nodeTag);

        CompoundTag matrixA = new CompoundTag();
        matrixA.putLong("matrix", MATRIX_A.asLong());
        matrixA.put("nodes", nodes);
        CompoundTag matrixB = new CompoundTag();
        matrixB.putLong("matrix", MATRIX_B.asLong());
        matrixB.put("nodes", nodes);

        var matrices = new net.minecraft.nbt.ListTag();
        matrices.add(matrixA);
        matrices.add(matrixB);

        CompoundTag root = new CompoundTag();
        root.put("matrices", matrices);

        ImagNetworkData data = ImagNetworkData.load(root);

        // Le noeud ne doit compter que pour un seul Matrix, le dernier lu.
        int total = data.nodeCount(MATRIX_A) + data.nodeCount(MATRIX_B);
        assertEquals(1, total, "le noeud ne doit pas etre compte deux fois");
        assertEquals(MATRIX_B, data.matrixOf(NODE_1));
    }

    @Test
    @DisplayName("les positions sont conservees a l'identique (pas de perte de signe)")
    void negativeCoordinatesSurvive() {
        ImagNetworkData source = new ImagNetworkData();
        BlockPos deep = new BlockPos(-1234, -60, 987);
        BlockPos matrix = new BlockPos(-1200, -60, 1000);
        source.link(matrix, deep);

        ImagNetworkData restored = ImagNetworkData.load(source.save(new CompoundTag()));

        assertEquals(1, restored.nodeCount(matrix));
        assertTrue(restored.nodesOf(matrix).contains(deep));
    }
}
