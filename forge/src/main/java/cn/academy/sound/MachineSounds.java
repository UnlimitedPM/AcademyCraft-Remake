package cn.academy.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Le son des machines, du cote qui sait le jouer.
 *
 * <p>Un block entity est une classe <b>partagee</b> : un serveur dedie la charge aussi bien
 * qu'un client, et elle ne doit donc jamais nommer une classe cliente. C'est pourtant de
 * la que vient l'information — c'est lui qui tick, et lui seul qui sait que la machine
 * travaille. Le port a deja rencontre ce probleme avec les applications du terminal, et
 * repond de la meme facon : une table neutre, remplie par le client au demarrage
 * ({@code ClientModEvents}), consultee ici.
 *
 * <p>Sans client installe — un serveur dedie, ou un test — {@link #tick} ne fait rien du
 * tout, et personne ne charge jamais la classe cliente.
 */
public final class MachineSounds {

    /** Ce que le client doit faire d'une machine : faire vivre sa boucle, ou l'eteindre. */
    @FunctionalInterface
    public interface Hook {
        void tick(Level level, BlockPos pos, MachineLoops.Loop loop, boolean working);
    }

    private static Hook hook;

    private MachineSounds() {}

    /** Installe le cote client. Appele une fois, au demarrage du client. */
    public static void install(Hook value) {
        hook = value;
    }

    /**
     * Annonce l'etat d'une machine au son, a chaque tick de son block entity.
     *
     * <p>L'etat vient de la balise de synchronisation et non d'un calcul local : le client
     * n'a ni recette ni energie, il ne peut pas savoir si la machine travaille. Le serveur
     * le lui dit, dix fois par seconde, dans le meme paquet que sa barre de progression.
     */
    public static void tick(Level level, BlockPos pos, MachineLoops.Loop loop, boolean working) {
        if (hook != null) hook.tick(level, pos, loop, working);
    }
}
