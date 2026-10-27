package cn.academy.ability;

import cn.academy.ability.meltdowner.MeltdownerCategory;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires de la casse des blocs cote joueur : les deux reglages de {@link AbilityData},
 * leur persistance, et le double verrou de {@link BlockBreaking}.
 *
 * <p>La regle que ces tests defendent est celle du joueur : « si le serveur permet de casser les
 * blocs, les joueurs doivent quand meme avoir la possibilite de faire en sorte que leur pouvoir
 * ne casse pas les blocs ». Le refus d'un joueur ne peut donc que retirer, jamais ajouter.
 */
class BreakPrefsTest {

    /** Un joueur neuf casse : c'est l'etat d'avant le reglage, et celui d'une vieille sauvegarde. */
    @Test
    @DisplayName("par defaut, tout casse")
    void defautToutCasse() {
        AbilityData data = new AbilityData();
        assertTrue(data.playerBreaksBlocks());
        assertTrue(data.breaksBlocks(MeltdownerCategory.MELTDOWNER));
        assertTrue(data.breaksBlocks(MeltdownerCategory.MINE_RAY_BASIC));
    }

    /** Le drapeau general ferme toutes les competences, y compris celles qu'on n'a pas exclues. */
    @Test
    @DisplayName("le drapeau general ferme tout")
    void drapeauGeneralFermeTout() {
        AbilityData data = new AbilityData();
        data.setPlayerBreaksBlocks(false);
        assertFalse(data.breaksBlocks(MeltdownerCategory.MELTDOWNER));
        assertFalse(data.breaksBlocks(MeltdownerCategory.MINE_RAY_BASIC));
        assertFalse(data.breaksBlocks(null));
    }

    /** Le refus par competence : garder la mine ray et refuser le meltdowner, l'exemple du joueur. */
    @Test
    @DisplayName("un refus par competence ne touche pas les autres")
    void refusParCompetence() {
        AbilityData data = new AbilityData();
        data.setBreaksBlocks(MeltdownerCategory.MELTDOWNER, false);

        assertFalse(data.breaksBlocks(MeltdownerCategory.MELTDOWNER));
        assertTrue(data.breaksBlocks(MeltdownerCategory.MINE_RAY_BASIC));
        assertTrue(data.playerBreaksBlocks());

        data.setBreaksBlocks(MeltdownerCategory.MELTDOWNER, true);
        assertTrue(data.breaksBlocks(MeltdownerCategory.MELTDOWNER));
    }

    /** Un refus survit a la traversee du NBT : c'est lui qui part au serveur et qui revient. */
    @Test
    @DisplayName("les deux reglages voyagent dans le NBT")
    void persistance() {
        AbilityData data = new AbilityData();
        data.setPlayerBreaksBlocks(false);
        data.setBreaksBlocks(MeltdownerCategory.MINE_RAY_BASIC, false);

        AbilityData relu = new AbilityData();
        relu.deserializeNBT(data.serializeNBT());

        assertFalse(relu.playerBreaksBlocks());
        assertFalse(relu.breaksBlocks(MeltdownerCategory.MINE_RAY_BASIC));
        assertFalse(relu.breaksBlocks(MeltdownerCategory.MELTDOWNER));
    }

    /** Une sauvegarde d'avant le reglage n'a pas les cles : elle doit rester « tout casse ». */
    @Test
    @DisplayName("une vieille sauvegarde casse encore")
    void vieilleSauvegarde() {
        AbilityData data = new AbilityData();
        data.setPlayerBreaksBlocks(false);
        data.setBreaksBlocks(MeltdownerCategory.MINE_RAY_BASIC, false);

        CompoundTag tag = data.serializeNBT();
        tag.remove("breaksBlocks");
        tag.remove("noBreak");
        data.deserializeNBT(tag);

        assertTrue(data.playerBreaksBlocks());
        assertTrue(data.breaksBlocks(MeltdownerCategory.MINE_RAY_BASIC));
    }

    /** {@code copyFrom} passe par le NBT : la mort ne remet pas les reglages a zero. */
    @Test
    @DisplayName("copyFrom emporte les reglages")
    void copyFrom() {
        AbilityData source = new AbilityData();
        source.setBreaksBlocks(MeltdownerCategory.MELTDOWNER, false);

        AbilityData copie = new AbilityData();
        copie.copyFrom(source);
        assertFalse(copie.breaksBlocks(MeltdownerCategory.MELTDOWNER));
    }

    /** Les cles sont celles du reseau et du NBT : une seule ecriture pour les deux. */
    @Test
    @DisplayName("les cles de competence se relisent")
    void cles() {
        assertEquals("meltdowner.mine_ray_basic", BlockBreaking.key(MeltdownerCategory.MINE_RAY_BASIC));
        assertSame(MeltdownerCategory.MINE_RAY_BASIC, BlockBreaking.byKey("meltdowner.mine_ray_basic"));
        assertNull(BlockBreaking.byKey("meltdowner.inconnue"));
        assertNull(BlockBreaking.byKey(""));
        assertNull(BlockBreaking.byKey(null));
    }

    /** Le verrou du serveur passe avant celui du joueur, et il n'est jamais contournable. */
    @Test
    @DisplayName("le verrou du serveur passe avant")
    void verrouServeur() {
        boolean avant = cn.academy.Config.destroyBlocks;
        try {
            cn.academy.Config.destroyBlocks = false;
            AbilityData data = new AbilityData();
            assertFalse(BlockBreaking.allowed(data, MeltdownerCategory.MELTDOWNER));
            // Sans donnee (une machine, un test) le refus du serveur tient toujours.
            assertFalse(BlockBreaking.allowed(null, MeltdownerCategory.MELTDOWNER));

            cn.academy.Config.destroyBlocks = true;
            assertTrue(BlockBreaking.allowed(data, MeltdownerCategory.MELTDOWNER));
            // Et le joueur peut toujours retirer ce que le serveur autorise.
            data.setBreaksBlocks(MeltdownerCategory.MELTDOWNER, false);
            assertFalse(BlockBreaking.allowed(data, MeltdownerCategory.MELTDOWNER));
            assertTrue(BlockBreaking.allowed(data, MeltdownerCategory.MINE_RAY_BASIC));
        } finally {
            cn.academy.Config.destroyBlocks = avant;
        }
    }

    /** La liste des sous-reglages ne contient que des competences connues de leur categorie. */
    @Test
    @DisplayName("chaque sous-reglage a une cle lisible")
    void listeDesSousReglages() {
        for (Skill skill : BlockBreaking.prefSkills()) {
            assertNotNull(skill.getCategory(), skill.getName());
            assertFalse(BlockBreaking.key(skill).isEmpty());
            assertNotNull(BlockBreaking.byKey(BlockBreaking.key(skill)));
        }
    }
}
