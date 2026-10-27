package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Le bloc de l'imag phase liquide.
 *
 * <p>Un bloc de fluide, plus une chose : il <b>eteint le feu</b> qu'il touche.
 *
 * <p>L'original n'avait rien a ecrire pour cela — il construisait son {@code BlockFluidClassic}
 * avec <b>{@code Material.WATER}</b>, et le feu de 1.12.2 mourait de lui-meme a cote de tout ce
 * qui portait ce materiau (son {@code updateTick} regardait ses six voisins). En 1.20.1 ce n'est
 * plus une question de materiau : l'eau eteint par sa propre mecanique, et notre fluide n'est pas
 * de l'eau. Le port n'eteignait donc rien du tout — « l'imag liquid ne nous permet pas de retirer
 * le feu alors qu'il devrait pouvoir le faire ».
 *
 * <p>D'ou ces deux accroches : le feu qui nait a cote d'un bloc de liquide (il le signale par
 * {@link #neighborChanged}), et le liquide qui arrive a cote d'un feu (il regarde autour de lui
 * en {@link #onPlace}). Les deux sens sont necessaires : le joueur pose tantot le feu puis
 * asperge, tantot l'inverse.
 *
 * <p>Un bloc de fluide ordinaire pour le reste. L'original lui posait un {@code TileImagPhase}
 * sur chaque bloc, et le port a fait pareil : c'etait le seul moyen de retrouver les blocs de
 * liquide pour dessiner leurs nappes.
 *
 * <p>CE BLOCK ENTITY A ETE RETIRE : un par bloc de liquide, c'etait un objet, une entree dans la
 * carte du chunk et une entree dans la sauvegarde, pour ne rien porter du tout. Le rendu retrouve
 * maintenant les blocs par la palette du chunk (`LevelChunk.findBlocks`, voir
 * ImagPhaseLiquidRenderer), qui saute les sections ou le bloc n'est pas.
 */
public class ImagPhaseLiquidBlock extends LiquidBlock {

    public ImagPhaseLiquidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    /**
     * Le liquide arrive : il eteint le feu qui l'entoure.
     *
     * <p>Les six directions, et pas seulement celles du dessous : une nappe qui se pose sur de
     * l'herbe en feu doit l'eteindre, et un feu qui brule contre la paroi d'une colonne de
     * liquide aussi.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) return;
        for (Direction direction : Direction.values()) {
            douse(level, pos.relative(direction));
        }
    }

    /**
     * Le feu nait (ou un voisin change) : s'il s'agit d'un feu, il meurt.
     *
     * <p>Le port ne regarde que le voisin annonce — {@code fromPos} — et pas les six : c'est
     * l'appel le plus frequent du jeu, et le feu qui apparait est toujours celui qui se signale.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.isClientSide) return;
        douse(level, fromPos);
    }

    /**
     * Un corps dans le liquide : le feu s'eteint.
     *
     * <p>Signale par le joueur : « quand je me mets en feu et que je rentre dans l'imag liquid, il
     * ne me retire pas le feu ». L'eau eteint les corps qui la traversent depuis toujours — c'est
     * {@code Entity#baseTick} qui le fait, et il ne regarde que l'eau. Notre fluide n'en est pas,
     * donc rien ne s'eteignait.
     *
     * <p>La methode est appelee a chaque tick pour chaque bloc que le corps touche : c'est le
     * meme crochet que celui du feu, qui embrase, mais a l'envers.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!entity.isOnFire() && entity.getRemainingFireTicks() <= 0) return;
        entity.clearFire();
        // Le bruit que le jeu fait quand on s'eteint — « il n'y a pas le bruit de minecraft qui
        // dit qu'on n'est plus en feu ». C'est celui de {@code Entity#baseTick}, a la meme hauteur
        // et a la meme hauteur de son : le joueur doit entendre la meme chose que dans l'eau.
        if (!level.isClientSide) {
            entity.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7f,
                    1.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.4f);
        }
    }

    /**
     * Eteint un feu, et rien d'autre.
     *
     * <p>{@link BaseFireBlock} couvre le feu et le feu des ames, qui sont les deux seuls blocs
     * dont l'eau a raison. Le bloc part sans butin — le feu n'en a pas, et le joueur n'a rien
     * casse : c'est le liquide qui l'a eteint.
     */
    private static void douse(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
            level.removeBlock(pos, false);
        }
    }
}
