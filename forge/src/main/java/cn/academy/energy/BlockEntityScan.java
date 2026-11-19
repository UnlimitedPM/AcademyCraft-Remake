package cn.academy.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Recherche de block entities dans un rayon, sans parcourir le monde bloc par
 * bloc.
 *
 * Le besoin vient du raccordement automatique : un noeud cherche un Matrix, un
 * generateur cherche un noeud. Dans un rayon de 42 blocs, tester chaque position
 * ferait environ 700 000 lectures de bloc ; lire la liste des block entities des
 * chunks concernes en fait au pire quelques dizaines.
 *
 * Le rayon est applique <b>en trois dimensions</b> apres coup, car un chunk
 * couvre toute la hauteur du monde : c'est au filtre de l'appelant de verifier
 * la distance verticale, ce que {@code distSqr} fait naturellement.
 */
public final class BlockEntityScan {

    private BlockEntityScan() {}

    /**
     * Block entities du type demande autour d'une position, dans les chunks
     * deja charges uniquement.
     *
     * <p>Aucun chunk n'est charge pour les besoins de la recherche : un Matrix
     * dans un chunk non charge est simplement invisible, et le raccordement se
     * fera plus tard.</p>
     *
     * @param level  niveau a parcourir
     * @param pos    centre de la recherche
     * @param radius rayon horizontal, en blocs
     * @param type   type de block entity recherche
     */
    public static <T extends BlockEntity> List<T> around(Level level, BlockPos pos, double radius, Class<T> type) {
        int r = Mth.ceil(radius);
        ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(pos.getX() - r),
                                    SectionPos.blockToSectionCoord(pos.getZ() - r));
        ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(pos.getX() + r),
                                    SectionPos.blockToSectionCoord(pos.getZ() + r));

        List<T> found = new ArrayList<>();
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                // La carte appartient au chunk : on la copie avant de la
                // parcourir, le filtre pouvant declencher une mise a jour de bloc.
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (type.isInstance(be)) found.add(type.cast(be));
                }
            }
        }
        return found;
    }

    /**
     * Le plus proche block entity du type demande qui satisfait le filtre.
     *
     * @return le plus proche, ou {@code null} si aucun ne convient
     */
    @Nullable
    public static <T extends BlockEntity> T nearest(Level level, BlockPos pos, double radius,
                                                    Class<T> type, Predicate<T> filter) {
        double maxDistSq = radius * radius;
        T best = null;
        double bestDistSq = maxDistSq;

        for (T candidate : around(level, pos, radius, type)) {
            double distSq = candidate.getBlockPos().distSqr(pos);
            if (distSq > maxDistSq) continue;
            if (!filter.test(candidate)) continue;
            // A egalite de distance on garde le premier trouve : deux blocs ne
            // peuvent pas etre a la meme distance sans que ce soit le meme.
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }
        return best;
    }
}
