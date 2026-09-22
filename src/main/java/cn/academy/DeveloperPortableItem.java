package cn.academy;

import cn.academy.ability.develop.PortableDevCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Le developeur portable : un developeur d'aptitudes dans la poche.
 *
 * <p>Portage de {@code ItemDeveloper}, qui etait un {@code ItemEnergyBase} : l'energie
 * vivait dans l'objet, comme dans le port ou elle vit dans l'etiquette de la pile. Le
 * tampon et la bande passante sont ceux de {@link cn.academy.ability.develop.DeveloperType#PORTABLE}
 * — 10 000 et 50 — et l'objet s'en sert lui-meme, sans passer par une machine.
 *
 * <h2>Ce que l'objet fait</h2>
 *
 * <p>Un clic droit ouvre le <b>meme ecran</b> que la machine : c'est tout l'interet d'avoir
 * separe {@code Developer} de la machine, comme l'original ou `DeveloperUI` prenait un
 * `IDeveloper` quel qu'il soit. L'ecran ne sait pas ce qu'il developpe, et c'est tant
 * mieux : rien a maintenir en double.
 *
 * <p>L'energie, elle, est dans l'objet et y reste : un portable ne se raccorde a rien, et
 * il faut le recharger ailleurs — dans un noeud, comme n'importe quelle unite d'energie.
 */
public class DeveloperPortableItem extends Item {

    /** Tampon de l'objet. {@code DeveloperType.PORTABLE.getEnergy()}. */
    public static final double MAX_ENERGY = 10_000.0d;

    /** Energie acceptee ou rendue par tick. {@code LATENCY_MK1}. */
    public static final double BANDWIDTH = 50.0d;

    private static final String ENERGY_KEY = "ac_energy";

    public DeveloperPortableItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // ------------------------------------------------------------------
    // Energie
    // ------------------------------------------------------------------

    public static double getEnergy(ItemStack stack) {
        return stack.getTag() == null ? 0.0d : stack.getTag().getDouble(ENERGY_KEY);
    }

    /** Ajoute de l'energie sans depasser le tampon. Rend le nouveau niveau. */
    public static double charge(ItemStack stack, double amount) {
        double updated = Math.min(getEnergy(stack) + amount, MAX_ENERGY);
        stack.getOrCreateTag().putDouble(ENERGY_KEY, updated);
        return updated;
    }

    /** Retire de l'energie. Rend la quantite reellement retiree. */
    public static double discharge(ItemStack stack, double amount) {
        double available = getEnergy(stack);
        double taken = Math.min(available, amount);
        stack.getOrCreateTag().putDouble(ENERGY_KEY, available - taken);
        return taken;
    }

    public static boolean isFull(ItemStack stack) {
        return getEnergy(stack) >= MAX_ENERGY;
    }

    // ------------------------------------------------------------------
    // Usage
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer server) {
            // L'ecran est celui de la machine : seule la source de l'energie change, et
            // c'est le drapeau du paquet qui le dira au client.
            server.getCapability(PortableDevCapability.PORTABLE_DEV).ifPresent(data ->
                    NetworkHooks.openScreen(server,
                            new SimpleMenuProvider(
                                    (id, inventory, p) -> new DeveloperMenu(id, inventory, data),
                                    stack.getHoverName()),
                            buf -> buf.writeBoolean(true)));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("academy.developer.portable.energy",
                (int) getEnergy(stack), (int) MAX_ENERGY));
    }
}
