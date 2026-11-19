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
        update(tag, false);
    }

    /**
     * Met le cache a jour, et lance l'animation d'installation si le terminal vient d'arriver.
     *
     * <p>C'est le {@code install} de l'original : son objet d'installation envoyait un message
     * a son client, qui ouvrait {@code TerminalInstallEffect}. Ici le meme drapeau voyage avec
     * l'etat, ce qui evite un second paquet pour un booleen.
     *
     * <p>L'animation est un <b>calque</b> : on continue de marcher pendant l'installation, et
     * c'est le terminal qui prend la main a la fin — comme chez l'original, ou l'effet etait un
     * {@code AuxGui} et le terminal un overlay qui detournait la souris.
     */
    public static void update(CompoundTag tag, boolean installedNow) {
        DATA.deserializeNBT(tag);

        if (installedNow) TerminalInstallOverlay.start();
    }

    public static TerminalData get() {
        return DATA;
    }
}
