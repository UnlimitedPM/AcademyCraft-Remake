package cn.academy.ability.preset;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les prereglages : quatre touches, quatre prereglages, et ce qui les allume.
 *
 * <p>Tout est pur : des noms de competences, pas des competences. C'est ce qui permet de
 * relire ici la seule regle un peu nouee du systeme — une competence n'occupe qu'une touche
 * a la fois — alors que l'original la verifiait dans son interface.
 */
class PresetDataTest {

    @Test
    void unPrereglageNeufNALaRien() {
        AbilityPreset preset = new AbilityPreset();

        assertEquals(AbilityPreset.MAX_KEYS, 4, "l'original en donnait quatre");
        for (int key = 0; key < AbilityPreset.MAX_KEYS; key++) {
            assertNull(preset.nameAt(key), "la touche " + key + " doit etre libre");
            assertFalse(preset.hasMapping(key));
        }
        assertTrue(preset.isEmpty());
        assertTrue(preset.mappedSkills().isEmpty());
    }

    @Test
    void uneCompetenceNOccupeQuUneTouche() {
        AbilityPreset preset = new AbilityPreset();
        preset.assign(0, "arc_gen");
        preset.assign(2, "railgun");

        assertEquals("arc_gen", preset.nameAt(0));
        assertEquals("railgun", preset.nameAt(2));

        // La meme competence posee ailleurs quitte sa place : sinon elle partirait de deux
        // touches, et le joueur ne saurait plus laquelle allume quoi.
        preset.assign(1, "arc_gen");
        assertEquals("arc_gen", preset.nameAt(1));
        assertNull(preset.nameAt(0), "l'ancienne touche doit etre rendue");
        assertEquals(2, preset.mappedSkills().size());
        assertTrue(preset.contains("railgun"));
        assertFalse(preset.contains("arc_gen_inexistant"));
        assertFalse(preset.contains(null));
    }

    @Test
    void uneToucheSeLibere() {
        AbilityPreset preset = new AbilityPreset();
        preset.assign(3, "storm_wing");
        assertTrue(preset.hasMapping(3));

        preset.clear(3);
        assertFalse(preset.hasMapping(3));
        assertTrue(preset.isEmpty());

        // Une touche hors bornes ne fait rien du tout, plutot que de tomber.
        preset.assign(9, "arc_gen");
        preset.assign(-1, "arc_gen");
        assertTrue(preset.isEmpty());
        assertNull(preset.nameAt(9));
    }

    @Test
    void lesQuatrePrereglagesTournent() {
        PresetData data = new PresetData();

        assertEquals(PresetData.MAX_PRESETS, 4);
        assertEquals(0, data.getCurrentId(), "le premier prereglage est en service");

        data.switchNext();
        assertEquals(1, data.getCurrentId());
        data.switchNext();
        data.switchNext();
        assertEquals(3, data.getCurrentId());

        // Et on revient au premier : la touche de changement fait le tour.
        data.switchNext();
        assertEquals(0, data.getCurrentId());

        // Un identifiant hors bornes est ramene dans le tour plutot que de tomber, et un
        // identifiant negatif ne fait rien.
        data.switchTo(6);
        assertEquals(2, data.getCurrentId());
        data.switchTo(-1);
        assertEquals(2, data.getCurrentId(), "rien ne change");
    }

    @Test
    void chaquePrereglageARetenuSesTouches() {
        PresetData data = new PresetData();
        data.getPreset(0).assign(0, "arc_gen");
        data.getPreset(1).assign(0, "dir_shock");

        assertEquals("arc_gen", data.getPreset(0).nameAt(0));
        assertEquals("dir_shock", data.getPreset(1).nameAt(0));

        data.switchTo(1);
        assertEquals("dir_shock", data.getCurrent().nameAt(0),
                "changer de prereglage change ce que les touches allument");
    }

    @Test
    void toutSEffaceDUnCoup() {
        PresetData data = new PresetData();
        data.getPreset(0).assign(0, "arc_gen");
        data.getPreset(3).assign(2, "railgun");
        data.switchTo(3);

        // C'est ce que fait un changement de categorie : les competences rangees ne sont
        // plus apprises, donc les garder n'aurait pas de sens.
        data.clearAll();

        assertTrue(data.getPreset(0).isEmpty());
        assertTrue(data.getPreset(3).isEmpty());
        assertEquals(0, data.getCurrentId(), "et le premier prereglage reprend le service");
    }

    @Test
    void lesPrereglagesFontUnAllerRetourParLaSauvegarde() {
        PresetData data = new PresetData();
        data.getPreset(0).assign(0, "arc_gen");
        data.getPreset(2).assign(1, "storm_wing");
        data.getPreset(2).assign(3, "plasma_cannon");
        data.switchTo(2);

        CompoundTag tag = data.serializeNBT();

        PresetData reread = new PresetData();
        reread.deserializeNBT(tag);

        assertEquals(2, reread.getCurrentId());
        assertEquals("arc_gen", reread.getPreset(0).nameAt(0));
        assertEquals("storm_wing", reread.getPreset(2).nameAt(1));
        assertEquals("plasma_cannon", reread.getPreset(2).nameAt(3));
        assertTrue(reread.getPreset(1).isEmpty());
        assertTrue(reread.getPreset(3).isEmpty());
    }
}
