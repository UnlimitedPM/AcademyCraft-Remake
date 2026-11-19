package cn.academy.ability.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Ce que coute l'eclat de la detection de minerais : compteurs et chronometre.
 *
 * <p>Eteint par defaut, et il ne coute alors qu'une lecture de booleen final : pas de
 * {@code nanoTime}, pas de compteur, pas de ligne de journal. On l'allume par la variable
 * d'environnement {@code AC_MD_STATS=1} (ou la propriete
 * {@code -Dacademy.minedetect.stats=true}), et il ecrit une ligne toutes les cent ticks — cinq
 * secondes — avec, par balayage et par image, ce que le regard a pris.
 *
 * <p>Les deux chiffres qui comptent sont le nombre de <b>blocs lus</b> par balayage et le
 * nombre de <b>quads</b> par image : le premier dit le prix du balayage, le second celui du
 * dessin. Un minerai dessine vaut six quads (six faces de cube), donc les deux se deduisent
 * l'un de l'autre — c'est pour ca que le rapport donne les deux.
 *
 * <p>Une fenetre, et pas un total : la moyenne depuis le debut melange une caverne pleine de
 * minerais et un couloir vide, donc elle ne dit rien de ce qu'on vient de vivre.
 */
final class MineDetectStats {

    private static final Logger LOGGER = LoggerFactory.getLogger("academy-minedetect");

    /** Toutes les cent ticks : cinq secondes, assez pour lire sans noyer le journal. */
    private static final int REPORT_TICKS = 100;

    /** Les faces d'un minerai dessine : six, comme le cube de {@code MineDetectRenderer}. */
    private static final int QUADS_PER_ORE = 6;

    /** Vrai quand le joueur a demande les chiffres — voir la javadoc de la classe. */
    static final boolean ENABLED;

    static {
        String env = System.getenv("AC_MD_STATS");
        ENABLED = (env != null && !env.isEmpty() && !env.equals("0"))
                || Boolean.getBoolean("academy.minedetect.stats");
    }

    private static int ticks;
    private static long scans;
    private static long scanNanos;
    private static long blocksRead;
    private static long oresFound;
    private static long frames;
    private static long drawnOres;
    private static long drawnQuads;
    private static long drawNanos;

    private MineDetectStats() {}

    static boolean enabled() {
        return ENABLED;
    }

    /** Un balayage : son cout en nanosecondes, les blocs lus et les minerais retenus. */
    static void scan(long nanos, int blocks, int found) {
        if (!ENABLED) return;
        scans++;
        scanNanos += nanos;
        blocksRead += blocks;
        oresFound += found;
    }

    /** Une image de rendu : les minerais dessines, leurs quads, et le temps passe. */
    static void draw(int ores, long nanos) {
        if (!ENABLED) return;
        frames++;
        drawnOres += ores;
        drawnQuads += (long) ores * QUADS_PER_ORE;
        drawNanos += nanos;
    }

    /** Un tick du client : compte les secondes, et ecrit le rapport au bout de cent ticks. */
    static void tick() {
        if (!ENABLED) return;
        if (++ticks < REPORT_TICKS) return;

        double seconds = ticks / 20.0;
        double blocksPerScan = scans == 0 ? 0 : blocksRead / (double) scans;
        double msPerScan = scans == 0 ? 0 : scanNanos / 1_000_000.0 / scans;
        double oresPerFrame = frames == 0 ? 0 : drawnOres / (double) frames;
        double msPerFrame = frames == 0 ? 0 : drawNanos / 1_000_000.0 / frames;

        LOGGER.info(String.format(Locale.ROOT,
                "[mine_detect] %.1f s : %d balayages, %d blocs lus (%.0f par balayage), "
                        + "%d minerais, %.3f ms par balayage | dessin %d images, %.0f minerais "
                        + "(%d quads), %.3f ms par image",
                seconds, scans, blocksRead, blocksPerScan, oresFound, msPerScan,
                frames, oresPerFrame, drawnQuads, msPerFrame));

        reset();
    }

    /** Remet la fenetre a zero, sans toucher au drapeau. */
    private static void reset() {
        ticks = 0;
        scans = 0;
        scanNanos = 0;
        blocksRead = 0;
        oresFound = 0;
        frames = 0;
        drawnOres = 0;
        drawnQuads = 0;
        drawNanos = 0;
    }
}
