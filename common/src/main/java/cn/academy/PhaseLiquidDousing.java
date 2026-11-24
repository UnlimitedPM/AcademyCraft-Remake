package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

/**
 * Ce que fait l'imag phase liquide en plus d'etre un liquide : il eteint le feu.
 *
 * <p>L'original n'avait rien a ecrire pour cela — il construisait son {@code BlockFluidClassic} avec
 * <b>{@code Material.WATER}</b>, et le feu de 1.12.2 mourait de lui-meme a cote de tout ce qui
 * portait ce materiau (son {@code updateTick} regardait ses six voisins). En 1.20.1 ce n'est plus
 * une question de materiau : l'eau eteint par sa propre mecanique, et notre fluide n'est pas de
 * l'eau. Le port n'eteignait donc rien du tout — « l'imag liquid ne nous permet pas de retirer le
 * feu alors qu'il devrait pouvoir le faire ».
 *
 * <p>La logique vit ici, et non dans le bloc, parce que le bloc de liquide n'a pas la meme forme
 * chez les deux chargeurs : Forge lui passe un fournisseur de fluide (le fluide n'est pas encore
 * enregistre quand le bloc se construit), Fabric lui passe le fluide lui-meme. Chacun garde donc sa
 * classe, et les deux ne font qu'appeler ces trois methodes.
 */
public final class PhaseLiquidDousing {

    private PhaseLiquidDousing() {
    }

    /**
     * Le liquide arrive : il eteint le feu qui l'entoure.
     *
     * <p>Les six directions, et pas seulement celles du dessous : une nappe qui se pose sur de
     * l'herbe en feu doit l'eteindre, et un feu qui brule contre la paroi d'une colonne de liquide
     * aussi.
     */
    public static void onPlaced(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Direction direction : Direction.values()) {
            douse(level, pos.relative(direction));
        }
    }

    /**
     * Le feu nait (ou un voisin change) : s'il s'agit d'un feu, il meurt.
     *
     * <p>On ne regarde que le voisin annonce — {@code fromPos} — et pas les six : c'est l'appel le
     * plus frequent du jeu, et le feu qui apparait est toujours celui qui se signale.
     */
    public static void neighbourChanged(Level level, BlockPos fromPos) {
        if (level.isClientSide) return;
        douse(level, fromPos);
    }

    /**
     * Un corps dans le liquide : le feu s'eteint.
     *
     * <p>Signale par le joueur : « quand je me mets en feu et que je rentre dans l'imag liquid, il ne
     * me retire pas le feu ». L'eau eteint les corps qui la traversent depuis toujours — c'est
     * {@code Entity#baseTick} qui le fait, et il ne regarde que l'eau. Notre fluide n'en est pas,
     * donc rien ne s'eteignait.
     *
     * <p>La methode est appelee a chaque tick pour chaque bloc que le corps touche : c'est le meme
     * crochet que celui du feu, qui embrase, mais a l'envers.
     */
    public static void entityInside(Level level, Entity entity) {
        if (!entity.isOnFire() && entity.getRemainingFireTicks() <= 0) return;
        entity.clearFire();
        // Le bruit que le jeu fait quand on s'eteint — « il n'y a pas le bruit de minecraft qui dit
        // qu'on n'est plus en feu ». C'est celui de {@code Entity#baseTick}, a la meme hauteur de
        // son, mais un peu plus fort : le joueur ne l'entendait pas.
        float pitch = 1.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.4f;
        if (level.isClientSide) {
            // Celui qui brule a le sien ici, tout de suite : le paquet du serveur arrive apres le
            // changement d'etat, et c'est justement ce qui le rendait inaudible. Voir ClientExtinguish.
            cn.academy.client.ClientExtinguish.playFor(entity, pitch);
            return;
        }
        // Les autres l'entendent d'ici ; le joueur en feu en est exclu, il a deja le sien.
        if (entity instanceof Player) return;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0f, pitch);
    }

    /**
     * Eteint un feu, et rien d'autre.
     *
     * <p>{@link BaseFireBlock} couvre le feu et le feu des ames, qui sont les deux seuls blocs dont
     * l'eau a raison. Le bloc part sans butin — le feu n'en a pas, et le joueur n'a rien casse :
     * c'est le liquide qui l'a eteint.
     */
    private static void douse(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
            level.removeBlock(pos, false);
        }
    }
}
