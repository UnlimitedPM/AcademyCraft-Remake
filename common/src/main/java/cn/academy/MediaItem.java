package cn.academy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * L'objet d'un morceau du lecteur media.
 *
 * <p>Portage de l'{@code ItemMedia} de l'original, a trois conditions pres — l'application doit etre
 * installee, le morceau ne doit pas etre deja possede, et il faut alors en perdre un. Les trois
 * reponses de l'original sont gardees telles quelles, avec ses trois messages.
 *
 * <p>L'original portait tous ses morceaux dans un seul objet et rangeait le morceau choisi dans les
 * metadonnees. La 1.20.1 n'en a plus : chaque morceau a donc son objet, comme les quatre facteurs
 * d'induction.
 */
public class MediaItem extends TooltipItem {

    private final String mediaId;

    public MediaItem(String mediaId) {
        super("ac.media." + mediaId + ".desc");
        this.mediaId = mediaId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        cn.academy.misc.media.Media media = cn.academy.misc.media.MediaManager.get(mediaId);
        if (media == null) return InteractionResultHolder.pass(stack);

        cn.academy.terminal.TerminalData terminal = cn.academy.platform.PlatformHooks.terminalData(player);
        boolean hasApp = terminal != null
                && terminal.isInstalled(cn.academy.terminal.app.AppMediaPlayer.INSTANCE);
        if (!hasApp) {
            player.displayClientMessage(Component.translatable("ac.media.notinstalled"), false);
            return InteractionResultHolder.success(stack);
        }

        cn.academy.misc.media.MediaAcquireData data = cn.academy.misc.media.MediaTracker.of(player);
        if (data == null) return InteractionResultHolder.pass(stack);

        Component name = Component.translatable(media.titleKey());
        if (data.isInstalled(media)) {
            player.displayClientMessage(Component.translatable("ac.media.haveone", name), false);
            return InteractionResultHolder.success(stack);
        }

        data.install(media);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.translatable("ac.media.acquired", name), false);

        // Le client affiche ce qu'il possede : c'est le serveur seul qui vient de decider, et lui
        // seul peut le lui dire.
        if (player instanceof ServerPlayer serverPlayer) {
            cn.academy.misc.media.MediaTracker.sync(serverPlayer);
        }
        return InteractionResultHolder.success(stack);
    }
}
