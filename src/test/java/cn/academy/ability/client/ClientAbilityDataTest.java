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
            ClientAbilityData.tick(false);
        }
        assertEquals(before, ClientAbilityData.get().getControlPoint(), 0.0001f,
                "le delai qui suit un paiement ne remonte pas non plus chez le client");

        ClientAbilityData.tick(false);

        float gain = ClientAbilityData.get().getControlPoint() - before;
        assertEquals(0.54f * (1f + before / source.getMaxControlPoint()), gain, 0.01f,
                "et la reprise est celle du serveur, au chiffre pres");
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
            ClientAbilityData.tick(false);
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
            ClientAbilityData.tick(true);
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
            ClientAbilityData.tick(false);
        }

        assertEquals(ClientAbilityData.get().getMaxControlPoint(),
                ClientAbilityData.get().getControlPoint(), 0.0001f);
        assertTrue(ClientAbilityData.get().getControlPoint() > 0f);
    }
}
