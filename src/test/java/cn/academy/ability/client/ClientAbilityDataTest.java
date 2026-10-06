package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La reserve chez le client, entre deux synchronisations.
 *
 * <p>Le serveur n'envoie son etat que tous les dix ticks : sans ce rejeu, les nombres du
 * menu F4 et la barre de CP avanceraient par bonds (1410, puis 1420). Le client ne decide
 * de rien — il refait le meme calcul sur les memes donnees, et chaque synchronisation
 * reecrit la valeur vraie.
 *
 * <p>{@code ClientAbilityData} ne touche a aucun type Minecraft : il se relit ici, comme
 * le reste des decisions du port.
 */
class ClientAbilityDataTest {

    @Test
    void leClientRejoueLaRepriseEntreDeuxSynchronisations() {
        Category category = new Category("test");
        AbilityData source = new AbilityData();
        source.setCategoryLevel(category, 1);
        source.consumeControlPoint(source.getControlPoint() * 0.5f);
        ClientAbilityData.update(source.serializeNBT());

        float before = ClientAbilityData.get().getControlPoint();

        // Quinze ticks d'attente, puis la reprise : c'est le delai de l'original, et il
        // voyage dans la synchronisation.
        for (int i = 0; i < 15; i++) {
            ClientAbilityData.tick(false, 0f);
        }
        assertEquals(before, ClientAbilityData.get().getControlPoint(), 0.0001f,
                "le delai qui suit un paiement ne remonte pas non plus chez le client");

        ClientAbilityData.tick(false, 0f);

        float gain = ClientAbilityData.get().getControlPoint() - before;
        assertEquals(0.54f * (1f + before / source.getMaxControlPoint()), gain, 0.01f,
                "et la reprise est celle du serveur, au chiffre pres");
    }

    @Test
    void lEntretienDUnMaintienDescendDUnTickALAutre() {
        // Ce que le joueur a demande : « je vois mes cp diminuer de 20 en 20 par secondes, alors
        // que normalement ca devrait faire un affichage plus joli ou on voit les nombres defiler,
        // comme avec la recharge des cp ». Le serveur n'envoie son etat que tous les quatre ticks
        // pendant un maintien : sans ce rejeu, la reserve du client ne descendait qu'a ces
        // envois-la, par paquets de cinquante points.
        Category category = new Category("test");
        AbilityData source = new AbilityData();
        source.setCategoryLevel(category, 1);
        ClientAbilityData.update(source.serializeNBT());

        float before = ClientAbilityData.get().getControlPoint();

        // L'entretien de la deviation de vecteur a l'experience nulle : 15 points.
        ClientAbilityData.tick(true, 15f);
        assertEquals(before - 15f, ClientAbilityData.get().getControlPoint(), 0.0001f,
                "l'entretien se paie au tick, comme chez le serveur");

        // Et la reprise ne vient pas s'y meler : le paiement a arme son delai de quinze ticks,
        // exactement comme le `perform` du serveur.
        ClientAbilityData.tick(true, 15f);
        assertEquals(before - 30f, ClientAbilityData.get().getControlPoint(), 0.0001f,
                "et la reserve ne remonte pas entre deux paiements");

        // Un maintien qui ne paie rien (la manipulation, la visee du reacteur) ne descend pas.
        ClientAbilityData.tick(true, 0f);
        assertEquals(before - 30f, ClientAbilityData.get().getControlPoint(), 0.0001f,
                "une competence qui ne paie pas par tick ne fait rien descendre");
    }

    @Test
    void leSurcoutRemonteAussiChezLeClient() {
        Category category = new Category("test");
        AbilityData source = new AbilityData();
        source.setCategoryLevel(category, 1);
        source.perform(0f, 50f);
        ClientAbilityData.update(source.serializeNBT());

        float before = ClientAbilityData.get().getOverload();
        // Le delai de 32 ticks avant que le surcout ne redescende, puis sa chute.
        for (int i = 0; i < 40; i++) {
            ClientAbilityData.tick(false, 0f);
        }

        assertTrue(ClientAbilityData.get().getOverload() < before,
                "le surcout redescend chez le client aussi");
    }

    @Test
    void unMaintienFigeLeSurcout() {
        // Le serveur epingle la part de surcout d'un maintien (`isHoldingOverload`) et ne la
        // fait pas redescendre. Si le client la faisait redescendre quand meme, elle
        // plongerait entre deux envois puis remonterait a chaque synchronisation : un
        // clignotement.
        Category category = new Category("test");
        AbilityData source = new AbilityData();
        source.setCategoryLevel(category, 1);
        source.perform(0f, 50f);
        ClientAbilityData.update(source.serializeNBT());

        float before = ClientAbilityData.get().getOverload();
        for (int i = 0; i < 60; i++) {
            ClientAbilityData.tick(true, 0f);
        }

        assertEquals(before, ClientAbilityData.get().getOverload(), 0.0001f,
                "un maintien ne rend pas le surcout au client");
    }

    @Test
    void laReserveNeDepassePasSonPlafondChezLeClient() {
        AbilityData source = new AbilityData();
        source.setCategoryLevel(new Category("test"), 1);
        ClientAbilityData.update(source.serializeNBT());

        for (int i = 0; i < 4000; i++) {
            ClientAbilityData.tick(false, 0f);
        }

        assertEquals(ClientAbilityData.get().getMaxControlPoint(),
                ClientAbilityData.get().getControlPoint(), 0.0001f);
        assertTrue(ClientAbilityData.get().getControlPoint() > 0f);
    }
}
