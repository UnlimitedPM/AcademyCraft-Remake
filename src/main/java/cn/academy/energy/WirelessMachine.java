package cn.academy.energy;

import net.minecraft.core.BlockPos;

/**
 * Une machine qui porte un onglet sans fil.
 *
 * <p>L'original laissait l'onglet parler au serveur par son propre canal, en designant la machine par
 * son <b>objet</b> de tuile. Un paquet ne transporte pas d'objet : il faut donc un moyen de savoir de
 * quelle machine le joueur parle. Le conteneur ouvert le sait, et c'est la seule source qui ne
 * puisse pas mentir — le client n'envoie aucune position, donc aucune machine n'est designee par
 * quelqu'un qui n'est pas devant elle.
 */
public interface WirelessMachine {

    /**
     * La position de la machine dont ce conteneur est l'ecran.
     *
     * <p>{@code null} pour un conteneur qui n'a pas de machine — le developpeur portable, qui est un
     * objet tenu. L'onglet sans fil n'a alors rien a demander, et le serveur ne repond rien.
     */
    @javax.annotation.Nullable
    BlockPos machinePos();
}
