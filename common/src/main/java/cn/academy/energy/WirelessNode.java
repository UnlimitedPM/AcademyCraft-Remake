package cn.academy.energy;

import net.minecraft.core.BlockPos;

/**
 * Un noeud sans fil qui porte un onglet sans fil — le sien, celui ou il choisit son reseau.
 *
 * <p>L'onglet d'une machine cherche un <b>noeud</b> ({@link WirelessMachine}) ; celui d'un noeud
 * cherche un <b>Matrix</b>, et c'est l'autre moitie de la meme page. L'original avait les deux :
 * {@code userPage} pour les machines, {@code nodePage} pour le noeud, avec la meme mise en page et
 * une seule icone de difference.
 *
 * <p>Comme pour la machine, le serveur prend la position du conteneur ouvert et non celle qu'un
 * paquet annoncerait : un client ne peut donc pas rejoindre un reseau depuis un noeud devant lequel
 * il n'est pas.
 */
public interface WirelessNode {

    /** La position du noeud dont ce conteneur est l'ecran. */
    BlockPos nodePos();
}
