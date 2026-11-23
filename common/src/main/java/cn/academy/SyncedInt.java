package cn.academy;

import java.util.function.IntSupplier;

import net.minecraft.world.inventory.DataSlot;

/**
 * Un entier du block entity, synchronise au client.
 *
 * Cote serveur {@link #get()} interroge directement le block entity ; cote
 * client, ou le paquet de synchronisation appelle {@link #set(int)}, c'est la
 * valeur recue qui est renvoyee. La valeur du client n'est jamais reinjectee
 * dans le block entity : le serveur reste la seule source de verite.
 *
 * <h2>Pourquoi pas juste lire le block entity</h2>
 *
 * Le contenu d'un inventaire n'est pas synchronise. Un ecran qui appellerait
 * directement une methode du block entity lisant son inventaire afficherait
 * toujours la valeur par defaut cote client, sans que rien ne signale l'erreur.
 * Le piege a ete rencontre avec l'ecran du Matrix : il annoncait "inactif" en
 * permanence, quel que soit le contenu reel.
 *
 * <h2>Quand ne pas s'en servir</h2>
 *
 * Pour un simple drapeau du block entity (par exemple "ce generateur est-il
 * raccorde ?"), preferer une mise a jour de bloc envoyee par le block entity :
 * l'ecran lit alors directement le block entity, et il n'y a rien a ajouter au
 * menu pour une information qui ne concerne pas le conteneur.
 */
public class SyncedInt extends DataSlot {

    private final IntSupplier serverValue;
    private int clientValue;
    private boolean hasClientValue;

    public SyncedInt(IntSupplier serverValue) {
        this.serverValue = serverValue;
    }

    @Override
    public int get() {
        return hasClientValue ? clientValue : serverValue.getAsInt();
    }

    @Override
    public void set(int value) {
        clientValue = value;
        hasClientValue = true;
    }

    /** Lecture directe, pour eviter de passer par les accesseurs de l'ecran. */
    public int value() {
        return get();
    }
}
