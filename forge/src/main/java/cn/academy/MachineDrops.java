package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Ce qu'une machine rend quand on la casse.
 *
 * <p>Bug signale par le joueur : « si on a des items dedans et qu'on casse la machine, elle ne
 * refait pas tomber les items qu'elle stockait ». C'etait vrai de toutes les machines — aucune ne
 * rendait son inventaire. Les blocs multiblocs savaient deja casser leurs autres parties et se
 * detacher du reseau, mais personne ne vidait l'inventaire, et le block entity disparaissait avec.
 *
 * <p>Le point d'accroche est celui du four de Minecraft ({@code AbstractFurnaceBlock.onRemove}) :
 * le block entity existe encore a ce moment-la, et {@code onRemove} n'est appele qu'une fois par
 * bloc casse — contrairement a {@code playerWillDestroy}, qui ne voit pas un bloc pousse par un
 * piston ni un bloc remplace.
 */
public final class MachineDrops {

    private MachineDrops() {}

    /**
     * Jette le contenu de la machine posee la, s'il y en a un.
     *
     * <p>Appele par le {@code onRemove} de chaque machine, et par lui seul : c'est le seul endroit ou
     * l'inventaire est encore lisible. Rien n'est fait cote client, et rien n'est jete si la machine
     * ne range pas d'objets.
     */
    public static void dropContents(Level level, BlockPos pos) {
        if (level.isClientSide) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof StoresItems machine)) return;

        ItemStackHandler inventory = machine.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            // La pile est RETIREE avant d'etre jetee : deux appels ne doivent pas la doubler, et un
            // inventaire vide dit la verite si quelque chose relit la machine dans la foulee.
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            Containers.dropItemStack(level, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d,
                    stack);
        }
    }
}
