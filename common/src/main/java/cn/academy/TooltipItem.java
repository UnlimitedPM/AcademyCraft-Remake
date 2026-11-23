package cn.academy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Objet dont l'infobulle est une cle de traduction, une ligne grise par ligne de texte.
 *
 * <p>Extrait de {@code ModItems} pour que le contenu partage avec Fabric puisse s'en servir.
 */
public class TooltipItem extends Item {
    private final String tooltipKey;

    public TooltipItem(String tooltipKey) {
        this(tooltipKey, new Item.Properties());
    }

    public TooltipItem(String tooltipKey, Item.Properties properties) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        for (String line : Component.translatable(tooltipKey).getString().split("\n")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
