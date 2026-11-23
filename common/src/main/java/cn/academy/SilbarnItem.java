package cn.academy;

import cn.academy.entity.EntitySilbarn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * La bille de silicium : l'objet qu'on lance, portage de {@code ItemSilbarn}.
 *
 * <p>Il ne sert a rien tout seul, et n'existe que pour la salve de rayons : on le jette ou l'on
 * veut que la salve explose. Il se consomme au lancer, comme l'original, sauf en creatif.
 *
 * <p>Le son est celui d'un oeuf lance, entendu par tout le monde — celui qui lance compris : une
 * main qui ne s'entend pas lancer est une main qui fait douter du clic.
 */
public class SilbarnItem extends Item {

    public SilbarnItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW,
                SoundSource.PLAYERS, 0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide) {
            level.addFreshEntity(new EntitySilbarn(level, player));
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
