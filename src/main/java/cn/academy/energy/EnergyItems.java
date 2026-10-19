package cn.academy.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Regle commune des objets energetiques (portage de {@code IFItemManager}).
 *
 * <p>Une seule definition pour : le tag, la charge, la decharge, l'infobulle et la
 * barre de niveau. L'unite d'energie et le developpeur portable s'en servent donc
 * de la meme facon, et le noeud sans fil n'a pas a connaitre les classes.
 *
 * <p>Le tag est celui de l'original dans l'esprit ({@code energy}) mais garde la
 * cle du port, {@code ac_energy}, pour que les piles deja sauvegardees dans un
 * monde ne perdent pas leur charge. Le type NBT est {@code double} : lire une
 * ancienne valeur ecrite en {@code float} fonctionne toujours, l'inverse aussi
 * chez les versions precedentes du port.
 */
public final class EnergyItems {

    /** Cle NBT de la charge. */
    public static final String KEY = "ac_energy";

    /** Nombre de crans de la barre de durabilite vanilla ({@code Item.MAX_BAR_WIDTH}). */
    public static final int BAR_STEPS = 13;

    private EnergyItems() {
    }

    /** L'objet energetique porte par cette pile, ou {@code null}. */
    public static ImagEnergyItem itemOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.getItem() instanceof ImagEnergyItem item ? item : null;
    }

    public static boolean supports(ItemStack stack) {
        return itemOf(stack) != null;
    }

    public static double maxEnergy(ItemStack stack) {
        ImagEnergyItem item = itemOf(stack);
        return item == null ? 0.0d : item.getMaxEnergy();
    }

    public static double bandwidth(ItemStack stack) {
        ImagEnergyItem item = itemOf(stack);
        return item == null ? 0.0d : item.getBandwidth();
    }

    /** Charge portee, 0 si la pile n'est pas un objet energetique. */
    public static double getEnergy(ItemStack stack) {
        ImagEnergyItem item = itemOf(stack);
        if (item == null) return 0.0d;
        CompoundTag tag = stack.getTag();
        return tag == null ? 0.0d : tag.getDouble(KEY);
    }

    /** Pose la charge, bornee a la capacite de l'objet. */
    public static void setEnergy(ItemStack stack, double amount) {
        ImagEnergyItem item = itemOf(stack);
        if (item == null) return;
        stack.getOrCreateTag().putDouble(KEY, Mth.clamp(amount, 0.0d, item.getMaxEnergy()));
    }

    /**
     * Ajoute de l'energie sans depasser la capacite.
     *
     * @return la charge de la pile APRES l'operation.
     */
    public static double charge(ItemStack stack, double amount) {
        double updated = getEnergy(stack) + amount;
        setEnergy(stack, updated);
        return getEnergy(stack);
    }

    /**
     * Retire de l'energie.
     *
     * @return la quantite REELLEMENT retiree (jamais plus que la charge).
     */
    public static double discharge(ItemStack stack, double amount) {
        double available = getEnergy(stack);
        double taken = Math.min(available, Math.max(0.0d, amount));
        setEnergy(stack, available - taken);
        return taken;
    }

    public static boolean isFull(ItemStack stack) {
        ImagEnergyItem item = itemOf(stack);
        return item != null && getEnergy(stack) >= item.getMaxEnergy();
    }

    /**
     * Retire de l'energie sans depasser la bande passante de l'objet.
     *
     * <p>C'est le {@code IFItemManager.pull} de l'original : les machines qui
     * s'alimentent sur un objet (formeur, fusor, brouilleur) passent par la.
     *
     * @return la quantite reellement retiree.
     */
    public static double pull(ItemStack stack, double amount) {
        return discharge(stack, Math.min(Math.max(0.0d, amount), bandwidth(stack)));
    }

    /**
     * Ajoute de l'energie sans depasser la bande passante de l'objet.
     *
     * <p>C'est le {@code IFItemManager.charge} de l'original, cote machine :
     * les generateurs et le noeud recharge l'objet ainsi.
     *
     * @return la quantite reellement transmise.
     */
    public static double push(ItemStack stack, double available) {
        double before = getEnergy(stack);
        charge(stack, Math.min(Math.max(0.0d, available), bandwidth(stack)));
        return getEnergy(stack) - before;
    }

    /** L'infobulle de l'original : {@code 4200/10000 IF}. */
    public static Component describe(ItemStack stack) {
        return Component.translatable("ac.energy.item_level",
                Long.toString(Math.round(getEnergy(stack))),
                Long.toString(Math.round(maxEnergy(stack))));
    }

    // ------------------------------------------------------------------
    // Barre de niveau (la barre de durabilite de l'original)
    // ------------------------------------------------------------------

    public static boolean hasBar(ItemStack stack) {
        return supports(stack);
    }

    /** Largeur de barre entre 0 et {@link #BAR_STEPS}, comme la durabilite vanilla. */
    public static int barWidth(ItemStack stack) {
        return barWidth(getEnergy(stack), maxEnergy(stack));
    }

    /**
     * La meme chose, sans pile : c'est la partie pure, donc testable en JUnit.
     * Une capacite nulle ne montre aucune barre (objet non energetique).
     */
    public static int barWidth(double energy, double max) {
        if (max <= 0.0d) return 0;
        double ratio = Mth.clamp(energy / max, 0.0d, 1.0d);
        return Mth.ceil(ratio * BAR_STEPS);
    }

    /** Rouge a vide, vert a plein (la courbe de la durabilite de Minecraft). */
    public static int barColor(ItemStack stack) {
        double max = maxEnergy(stack);
        return barColor(max <= 0.0d ? 0.0d : getEnergy(stack) / max);
    }

    /** Couleur de la barre pour un remplissage entre 0 et 1, sans pile. */
    public static int barColor(double ratio) {
        return Mth.hsvToRgb((float) (Mth.clamp(ratio, 0.0d, 1.0d) / 3.0d), 1.0f, 1.0f);
    }
}
