package cn.academy;

import cn.academy.advancements.AcademyAdvancements;
import cn.academy.content.AcademyContent;
import cn.academy.platform.PlatformHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * L'unite vide : elle se remplit d'imag phase liquide.
 *
 * <p>L'originale regardait le bloc vise, et si c'etait son liquide, le retirait et rendait une unite
 * de phase pleine. C'est ce que fait celle-ci, avec deux differences qui viennent du port :
 *
 * <ul>
 *   <li>le liquide se reconnait par un crochet ({@link PlatformHooks#isPhaseLiquid}) : chez Forge
 *       c'est un {@code FluidType}, sous Fabric une sous-classe de {@code FlowingFluid}, et le code
 *       commun ne connait ni l'un ni l'autre ;</li>
 *   <li>le succes « une unite de matiere remplie de phase » se donne ici meme : l'unite va
 *       directement dans l'inventaire, donc aucun ramassage n'aura lieu, et l'evenement de moisson
 *       de l'original n'a pas d'equivalent.</li>
 * </ul>
 */
public class EmptyUnitItem extends Item {

    public EmptyUnitItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // On cherche le liquide sous le regard du joueur.
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);

        BlockPos pos = hit.getBlockPos();
        if (!PlatformHooks.isPhaseLiquid(level.getFluidState(pos))) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            stack.shrink(1);

            ItemStack filled = new ItemStack(AcademyContent.item("matter_unit_phase_liquid"));
            if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                AcademyAdvancements.award(serverPlayer, AcademyAdvancements.GETTING_PHASE);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
