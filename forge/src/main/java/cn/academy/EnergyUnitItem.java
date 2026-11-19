package cn.academy;

import cn.academy.energy.EnergyItems;
import cn.academy.energy.ImagEnergyItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * L'unite d'energie (portage de {@code ItemEnergyBase(10000, 20)}).
 *
 * <p>Deux choses que la 1.12.2 avait et que le port n'avait pas encore :
 * <ul>
 *   <li>l'<b>infobulle</b> de {@code IFItemManager.getDescription} :
 *       {@code 4200/10000 IF} ;</li>
 *   <li>la <b>barre de niveau</b> : l'original laissait le jeu dessiner la barre de
 *       durabilite ({@code setMaxDamage(13)} et des degats proportionnels a la
 *       charge). En 1.20.1 la meme chose se demande par
 *       {@code isBarVisible}/{@code getBarWidth}/{@code getBarColor}.</li>
 * </ul>
 *
 * <p>La regle de charge elle-meme vit dans {@link EnergyItems}, partagee avec le
 * developpeur portable.
 */
public class EnergyUnitItem extends Item implements ImagEnergyItem {

    /** Capacite de l'objet, en IF. L'original : {@code new ItemEnergyBase(10000, 20)}. */
    public static final double MAX_ENERGY = 10_000.0d;

    /**
     * Bande passante de l'objet, en IF par tick.
     *
     * <p>L'original valait 20 : une unite se rechargeait donc deux fois et demie
     * plus lentement que le developpeur portable (50), pour la meme capacite de
     * 10 000 IF. Le joueur l'a vu en jeu et veut les deux au meme rythme ; c'est
     * un ECART DEMANDE PAR LE JOUEUR, la valeur d'origine reste 20.
     */
    public static final double BANDWIDTH = 50.0d;

    public EnergyUnitItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public double getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    public double getBandwidth() {
        return BANDWIDTH;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(EnergyItems.describe(stack));
    }

    // La barre de durabilite de l'original (l'energie y tenait lieu de degats).
    // Et comme lui, elle DISPARAIT a pleine charge : les degats valaient zero, et Minecraft
    // cache une barre de durabilite sans degats. C'est la regle de EnergyItems.hasBar.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return EnergyItems.hasBar(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return EnergyItems.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return EnergyItems.barColor(stack);
    }
}
