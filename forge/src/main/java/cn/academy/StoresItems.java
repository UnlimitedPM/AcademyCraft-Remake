package cn.academy;

import net.minecraftforge.items.ItemStackHandler;

/**
 * Une machine qui range des objets dans un inventaire.
 *
 * <p>Les machines du port ont toutes la meme chose — un {@link ItemStackHandler} et un
 * {@code getInventory()} public — mais aucune ne le disait a un type commun : rien ne pouvait donc
 * les traiter ensemble. C'est ce qui a laisse passer le bug vu par le joueur : casser une machine
 * pleine perdait tout ce qu'elle contenait, parce que personne ne savait lui demander son
 * inventaire au moment du cassage.
 *
 * <p>Seules les machines qui RANGENT des objets l'implementent : le developpeur et le moteur de
 * chat n'en ont pas.
 */
public interface StoresItems {

    /** L'inventaire de la machine, tel qu'il est. */
    ItemStackHandler getInventory();
}
