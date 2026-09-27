package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le block entity du bloc de fluide imag phase.
 *
 * <p>Il ne porte rien : ni niveau, ni inventaire, ni minuteur. Sa seule raison d'etre est
 * de faire dessiner les nappes du liquide ({@code ImagPhaseLiquidRenderer}) — un bloc de
 * fluide n'a pas de modele, donc pas d'autre moyen d'y ajouter quoi que ce soit.
 *
 * <p>L'original faisait exactement pareil : {@code TileImagPhase} ne contenait rien non
 * plus, et son rendu lisait la hauteur du fluide du bloc ou il se trouvait.
 */
public class ImagPhaseLiquidBlockEntity extends BlockEntity {

    public ImagPhaseLiquidBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMAG_PHASE.get(), pos, state);
    }
}
