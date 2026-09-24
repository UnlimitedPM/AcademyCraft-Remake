package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import net.minecraft.nbt.CompoundTag;

/** Client-side read-only cache of the local player's AbilityData, kept in sync via packets. */
public class ClientAbilityData {

    private static final AbilityData DATA = new AbilityData();

    public static void update(CompoundTag tag) {
        DATA.deserializeNBT(tag);
    }

    public static AbilityData get() {
        return DATA;
    }

    /**
     * Rejoue la reprise de la reserve et du surcout, un tick a la fois, entre deux
     * synchronisations.
     *
     * <p>Le serveur n'envoie son etat que tous les dix ticks (la cadence de l'original),
     * et l'original ne le montrait pas : sa barre etait large et ses nombres ne vivaient
     * que dans un ecran de debogage. Le port, lui, affiche ces nombres et sa reserve est
     * plus petite (1800 au niveau 1), donc la valeur sautait de dix ticks en dix ticks —
     * 1410, puis 1420 — et l'oeil n'y voyait qu'une saccade.
     *
     * <p>Ce n'est <b>pas</b> une decision de jeu : le serveur reste seul juge, chaque
     * synchronisation reecrit la valeur vraie, et le client ne fait ici que la suivre entre
     * deux envois. Meme formule, meme plafond, et le compteur d'attente qui suit un
     * paiement voyage dans la synchronisation — la reprise ne peut donc pas commencer plus
     * tot ici que chez lui.
     */
    public static void tick(boolean holding) {
        if (DATA.getControlPoint() < DATA.getMaxControlPoint()) {
            DATA.tickRegen();
        }
        // Le surcout aussi, pour la meme raison : le serveur ne l'envoie que tous les dix
        // ticks, donc sa barre descendait par bonds de sept points (une reserve de 100).
        //
        // SAUF pendant un maintien : le serveur epingle alors sa part de surcout
        // (`isHoldingOverload`) et ne la fait donc pas redescendre. La rejouer ici la ferait
        // plonger entre deux envois, puis remonter a chaque synchronisation — un
        // clignotement, pire que la saccade qu'on corrige.
        if (!holding) {
            DATA.tickOverload();
        }
    }
}
