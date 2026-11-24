package cn.academy;

import cn.academy.content.AcademyContent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * L'unite de phase liquide : un bloc de liquide qu'on pose, et qui rend l'unite vide.
 *
 * <p>Portage de {@code ItemPhaseLiquid} de l'original : ce n'est pas un seau, c'est une <b>unite</b>
 * qui se consomme. L'original posait son fluide, puis rendait une unite vide au joueur en mode
 * survie — et c'est exactement ce que fait {@link #useOn} apres la pose.
 *
 * <p>Elle vit ici, et non dans {@code ModItems}, parce que les deux chargeurs la posent : seul le
 * bloc de liquide change de provenance (Forge le tient dans un {@code RegistryObject}, Fabric dans
 * son propre registre), et il est donc donne au constructeur.
 */
public class PhaseUnitItem extends BlockItem {

    public PhaseUnitItem(Block block, Properties props) {
        super(block, props);
    }

    /** Le nom de l'objet, celui de l'original : « unite de phase liquide ». */
    @Override
    public String getDescriptionId() {
        return "item.academy.matter_unit_phase_liquid";
    }

    /**
     * Pose le liquide, puis rend l'unite vide au joueur.
     *
     * <p>L'unite rendue est {@code matter_unit}, celle de l'energie : c'est le meme recipient, vide.
     * En creatif il n'y a rien a rendre — l'original ne le faisait pas non plus.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        InteractionResult result = super.useOn(context);

        if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                ItemStack empty = new ItemStack(AcademyContent.item("matter_unit"));
                if (!player.getInventory().add(empty)) {
                    player.drop(empty, false);
                }
            }
        }
        return result;
    }
}
