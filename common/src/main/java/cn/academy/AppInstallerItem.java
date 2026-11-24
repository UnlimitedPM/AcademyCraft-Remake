package cn.academy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * L'objet qui installe une application du terminal.
 *
 * <p>Portage de {@code ItemApp}. Le nom de l'application est connu des la construction de l'objet,
 * mais l'application elle-meme ne l'est pas : les registres ne sont pas remplis au chargement des
 * classes. La recherche se fait donc a l'usage, et une application absente du registre — parce que
 * son contenu n'est pas encore porte — se contente de le dire au lieu de planter.
 */
public class AppInstallerItem extends Item {

    private final String appName;

    public AppInstallerItem(String appName) {
        super(new Item.Properties());
        this.appName = appName;
    }

    private cn.academy.terminal.App app() {
        return cn.academy.terminal.AppRegistry.INSTANCE.getByName(appName);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var app = app();
        if (app == null) {
            player.displayClientMessage(Component.translatable("ac.terminal.app_missing",
                    Component.translatable("ac.app." + appName + ".name")), false);
            return InteractionResultHolder.success(stack);
        }

        var data = cn.academy.platform.PlatformHooks.terminalData(player);
        if (data == null) return InteractionResultHolder.pass(stack);

        if (!data.isTerminalInstalled()) {
            player.displayClientMessage(Component.translatable("ac.terminal.notinstalled"), false);
            return InteractionResultHolder.success(stack);
        }
        if (data.isInstalled(app)) {
            player.displayClientMessage(Component.translatable("ac.terminal.app_alrdy_installed",
                    app.getDisplayName()), false);
            return InteractionResultHolder.success(stack);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        data.installApp(app);
        if (player instanceof ServerPlayer serverPlayer) {
            cn.academy.terminal.TerminalTracker.sync(serverPlayer);
        }
        player.displayClientMessage(Component.translatable("ac.terminal.app_installed",
                app.getDisplayName()), false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("ac.app." + appName + ".name")
                .withStyle(ChatFormatting.GRAY));
    }
}
