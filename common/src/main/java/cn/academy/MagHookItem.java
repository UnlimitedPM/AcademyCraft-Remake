package cn.academy;

import cn.academy.entity.EntityMagHook;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Le grappin magnetique, sous forme d'objet : un clic droit le lance.
 *
 * <p>Portage de l'objet de l'original, qui portait le crochet et le lancait. Le bruit est celui de
 * l'oeuf lance, et il se fait des deux cotes — <b>celui qui lance compris</b> : une main qui ne
 * s'entend pas lancer est une main qui fait douter du clic. C'est la meme decision que pour la bille
 * de silicium.
 *
 * <p>Il vit ici, et non dans {@code ModItems}, parce que les deux chargeurs le posent.
 */
public class MagHookItem extends Item {

    public MagHookItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EGG_THROW, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide) {
            level.addFreshEntity(new EntityMagHook(level, player));
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
