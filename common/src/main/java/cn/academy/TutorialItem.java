package cn.academy;

import cn.academy.advancements.AcademyAdvancements;
import cn.academy.terminal.App;
import cn.academy.terminal.app.AppTutorial;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * L'objet MisakaCloud : les tutoriels du mod, ouverts directement.
 *
 * <p>Portage de {@code ItemTutorial}. L'original l'attachait a l'application du meme nom, mais
 * l'ouvrait sans passer par le terminal — et c'est ce que fait le port : l'application est installee
 * d'office, donc l'objet ne sert qu'a l'ouvrir. L'ecran est atteint par {@code App.requestOpen}, qui
 * ne nomme aucun type client.
 *
 * <p>Le succes se declenche sur le serveur : c'est l'usage de l'objet qui compte, pas l'ecran qui
 * s'ouvre.
 *
 * <p>Il vit ici, et non dans {@code ModItems}, parce que les deux chargeurs le posent.
 */
public class TutorialItem extends Item {

    public TutorialItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            App.requestOpen(AppTutorial.INSTANCE);
        } else if (player instanceof ServerPlayer serverPlayer) {
            AcademyAdvancements.award(serverPlayer, AcademyAdvancements.OPEN_MISAKA_CLOUD);
        }
        return InteractionResultHolder.success(stack);
    }
}
