package cn.academy;

import cn.academy.advancements.AcademyAdvancements;
import cn.academy.platform.PlatformHooks;
import cn.academy.terminal.TerminalData;
import cn.academy.terminal.TerminalTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * L'installeur de terminal : un objet qui installe le terminal du joueur.
 *
 * <p>L'original le faisait depuis son gestionnaire de paquets ; ici l'objet demande la donnee au
 * chargeur, l'installe, et pousse l'etat au client avec le drapeau qui declenche l'animation —
 * c'est ce drapeau qui fait aussi partir le message qui donne la touche, a la fin de l'animation,
 * comme dans l'original.
 *
 * <p>Il vit ici, et non dans {@code ModItems}, parce que les deux chargeurs le posent.
 */
public class TerminalInstallerItem extends Item {

    public TerminalInstallerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        TerminalData data = PlatformHooks.terminalData(player);
        if (data == null) return InteractionResultHolder.pass(stack);

        if (data.isTerminalInstalled()) {
            player.displayClientMessage(Component.translatable("ac.terminal.alrdy_installed"), false);
            return InteractionResultHolder.success(stack);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        data.install();
        if (player instanceof ServerPlayer serverPlayer) {
            TerminalTracker.sync(serverPlayer, true);
            AcademyAdvancements.award(serverPlayer, AcademyAdvancements.TERMINAL_INSTALLED);
        }
        return InteractionResultHolder.success(stack);
    }
}
