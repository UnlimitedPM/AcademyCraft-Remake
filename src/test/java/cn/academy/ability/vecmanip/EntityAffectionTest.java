package cn.academy.ability.vecmanip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * La lecture de la config de vecmanip, {@code EntityAffection#parseEntry}.
 *
 * <p>C'est une fonction pure, et c'est ce qui compte : la classification elle-meme demande les
 * registres du jeu — donc un GameTest — mais sa <b>lecture</b> se verifie ici, y compris ses
 * refus. Une faute de frappe dans un fichier de config ne doit pas faire tomber le mod au
 * chargement, et ces tests sont la pour le dire.
 */
class EntityAffectionTest {

    @Test
    void uneEntreePorteUnNomEtUneDifficulte() {
        EntityAffection.Entry arrow = EntityAffection.parseEntry("minecraft:arrow=1.0");
        assertNotNull(arrow, "une entree valide doit se lire");
        assertEquals("minecraft:arrow", arrow.name());
        assertEquals(1.0f, arrow.difficulty(), 0.0001f);

        EntityAffection.Entry potion = EntityAffection.parseEntry("minecraft:potion=1.4");
        assertNotNull(potion);
        assertEquals(1.4f, potion.difficulty(), 0.0001f);

        // Les espaces autour ne comptent pas : une config ecrite a la main en contient.
        EntityAffection.Entry snowball = EntityAffection.parseEntry("  minecraft:snowball = 0.1  ");
        assertNotNull(snowball);
        assertEquals("minecraft:snowball", snowball.name());
        assertEquals(0.1f, snowball.difficulty(), 0.0001f);
    }

    @Test
    void unNomSeulVautLaDifficulteParDefaut() {
        // L'original avait une difficulte par defaut de 1,0, et une entite qu'il ne savait
        // pas classer la recevait. Ecrire un nom seul doit donc suffire.
        EntityAffection.Entry plain = EntityAffection.parseEntry("minecraft:egg");
        assertNotNull(plain);
        assertEquals("minecraft:egg", plain.name());
        assertEquals(EntityAffection.DEFAULT_DIFFICULTY, plain.difficulty(), 0.0001f);
    }

    @Test
    void uneEntreeIllisibleEstRefusee() {
        // Refuser, et non tomber : la ligne fautive est ignoree et l'entite tombe dans la
        // difficulte par defaut, comme chez l'original ou un nom qui ne se resolvait pas
        // etait simplement jete.
        assertNull(EntityAffection.parseEntry(null), "rien du tout");
        assertNull(EntityAffection.parseEntry(""), "vide");
        assertNull(EntityAffection.parseEntry("   "), "espaces");
        assertNull(EntityAffection.parseEntry("=1.0"), "sans nom");
        assertNull(EntityAffection.parseEntry("minecraft:arrow="), "sans nombre");
        assertNull(EntityAffection.parseEntry("minecraft:arrow=beaucoup"), "nombre illisible");
    }

    @Test
    void lesMotsClesDeLExclusionSontCeuxDeLOriginal() {
        // Deux mots-cles, et ils s'ecrivaient deja comme cela dans la config de la 1.12.2.
        assertEquals("living", EntityAffection.KEYWORD_LIVING);
        assertEquals("mob", EntityAffection.KEYWORD_MOB);

        // Et le repere d'une entite deja deviee, tag compris.
        assertEquals("ac_vm_deviated", EntityAffection.MARK);
    }
}
