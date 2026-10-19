package cn.academy;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Le bloc de l'imag phase liquide.
 *
 * <p>Un bloc de fluide ordinaire, et rien d'autre. L'original lui posait un
 * {@code TileImagPhase} sur chaque bloc, et le port a fait pareil : c'etait le seul moyen de
 * retrouver les blocs de liquide pour dessiner leurs nappes.
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
}
