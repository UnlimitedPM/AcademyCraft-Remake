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
     * Rejoue entre deux synchronisations ce que le serveur, lui, fait a chaque tick : la
     * reprise de la reserve et du surcout, et — depuis que le joueur l'a demande — la
     * <b>depense</b> de l'entretien d'un maintien.
     *
     * <p>Le serveur n'envoie son etat que tous les dix ticks en regime normal (la cadence de
     * l'original), et tous les quatre ticks pendant un maintien, parce qu'un paiement le marque
     * comme « a envoyer tout de suite ». L'original ne le montrait pas : sa barre etait large et
     * ses nombres ne vivaient que dans un ecran de debogage. Le port, lui, affiche ces nombres et
     * sa reserve est plus petite (1800 au niveau 1), donc la valeur sautait de paquet en paquet —
     * 1410, puis 1420 — et l'oeil n'y voyait qu'une saccade.
     *
     * <p>Ce n'est <b>pas</b> une decision de jeu : le serveur reste seul juge, chaque
     * synchronisation reecrit la valeur vraie, et le client ne fait ici que la suivre entre
     * deux envois. Meme formule, meme plafond, et le compteur d'attente qui suit un
     * paiement voyage dans la synchronisation — la reprise ne peut donc pas commencer plus
     * tot ici que chez lui.
     *
     * @param holding un maintien ou une charge est ouvert (le surcout est alors epingle par le
     *                serveur, et c'est le client qui rejoue l'entretien)
     * @param upkeep  ce que cet entretien coute a ce tick : voir {@code Skill#getTickUpkeep}
     */
    public static void tick(boolean holding, float upkeep) {
        // L'entretien d'un maintien se paie par tick, et c'est ici qu'il se rejoue : sans cela la
        // reserve du client ne descendait qu'a chaque synchronisation — tous les quatre ticks — et
        // le joueur voyait ses points partir une cinquantaine a la fois. « Je vois mes cp diminuer
        // de 20 en 20 par secondes, alors que normalement ca devrait faire un affichage plus joli
        // ou on voit les nombres defiler, comme avec la recharge des cp. » Le montant vient de la
        // competence elle-meme ({@code Skill#getTickUpkeep}) : c'est celui que le serveur paie, donc
        // les deux nombres ne divergent pas — et la synchronisation suivante reecrit la valeur vraie
        // de toute facon.
        if (holding && upkeep > 0f) {
            DATA.replayUpkeep(upkeep);
        }
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
        // Les recharges avancent ici AUSSI, et c'est ce qui manquait le plus : le client
        // recevait bien la valeur posee par le serveur, mais ne la faisait jamais descendre
        // entre deux envois. Le rappel restait donc fige sur la valeur recue, puis sautait
        // d'un cran au paquet suivant — et son dernier cran trainait jusqu'a dix ticks apres
        // que le serveur a rouvert la competence. Le joueur a decrit exactement cela :
        // « il reste encore un peu gris alors que je peux deja la refaire ».
        //
        // Ce n'est pas une decision de jeu, pour la meme raison que ci-dessus : les deux
        // cotes avancent d'un tick par tick, donc le client arrive a zero au meme moment que
        // le serveur, et chaque synchronisation reecrit de toute facon la valeur vraie.
        DATA.tickCooldowns();
    }
}
