package cn.academy.sound;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les boucles sonores des machines : qui en a une, laquelle, et a quel volume.
 *
 * <p>Le choix se fait sur le nom de la machine, sans registre ni Minecraft — c'est ce qui
 * permet de le relire ici, alors que le son lui-meme ne s'entend qu'en jeu. Une faute dans
 * un nom d'evenement ne ferait rien tomber en jeu : la machine tournerait simplement dans
 * un silence total.
 */
class MachineLoopsTest {

    /** L'original ne sonorisait que ces deux machines-la. */
    private static final int LOOPS = 2;

    @Test
    void lesDeuxMachinesQuiSonnentSontCellesDeLOriginal() {
        assertEquals(LOOPS, MachineLoops.machines().size());
        for (String machine : MachineLoops.machines()) {
            assertNotNull(MachineLoops.forMachine(machine), "boucle attendue : " + machine);
        }

        assertNull(MachineLoops.forMachine(null), "pas de machine, pas de son");
        // Les autres machines du port se taisent : le generateur solaire, l'eolienne et
        // l'atelier de developpement n'avaient aucune boucle chez l'original.
        assertNull(MachineLoops.forMachine("solar_gen"));
        assertNull(MachineLoops.forMachine("windgen"));
        assertNull(MachineLoops.forMachine("developer"));
    }

    @Test
    void lesEvenementsSontCeuxDeLOriginal() {
        assertEquals("machine.imag_fusor_work",
                MachineLoops.forMachine(MachineLoops.IMAG_FUSOR).event());
        assertEquals("machine.machine_work",
                MachineLoops.forMachine(MachineLoops.METAL_FORMER).event());
    }

    @Test
    void lesDeuxMachinesOntLeVolumeEtLaCategorieDeLOriginal() {
        // `new TileEntitySound(this, ...).setLoop().setVolume(0.6f)`, dans les deux block
        // entities, et `SoundCategory.BLOCKS`, que le constructeur de `TileEntitySound`
        // passait toujours — c'est le seul son du mod qui appartienne a un bloc.
        for (String machine : MachineLoops.machines()) {
            MachineLoops.Loop loop = MachineLoops.forMachine(machine);
            assertEquals(0.6f, loop.volume(), machine + " : volume de l'original");
            assertEquals(MachineLoops.BLOCKS, loop.source(), machine + " : categorie de l'original");
        }
    }

    @Test
    void chaqueBoucleExisteDansLeFichierDesSons() {
        Set<String> declared = new HashSet<>();
        try (InputStream in = MachineLoopsTest.class.getResourceAsStream("/assets/academy/sounds.json")) {
            assertNotNull(in, "le fichier des sons doit etre livre");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (var entry : json.entrySet()) {
                    declared.add(entry.getKey());
                }
            }
        } catch (Exception e) {
            throw new AssertionError("le fichier des sons doit se lire", e);
        }

        for (String machine : MachineLoops.machines()) {
            String event = MachineLoops.forMachine(machine).event();
            assertTrue(declared.contains(event),
                    "evenement inconnu pour " + machine + " : " + event);
        }
    }
}
