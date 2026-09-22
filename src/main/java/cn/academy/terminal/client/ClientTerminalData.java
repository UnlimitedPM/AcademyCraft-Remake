package cn.academy.terminal.client;

import cn.academy.terminal.TerminalData;
import net.minecraft.nbt.CompoundTag;

/**
 * Cache client de {@link TerminalData}, tenu a jour par paquets.
 *
 * Le client a besoin de savoir si le terminal est installe et quelles
 * applications le sont pour dessiner la grille. Il n'a pas la capacite : seule la
 * copie envoyee par le serveur fait foi.
 */
public final class ClientTerminalData {

    private static final TerminalData DATA = new TerminalData();

    private ClientTerminalData() {}

    public static void update(CompoundTag tag) {
        DATA.deserializeNBT(tag);
    }

    public static TerminalData get() {
        return DATA;
    }
}
