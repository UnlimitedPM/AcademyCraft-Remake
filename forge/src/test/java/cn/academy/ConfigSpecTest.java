package cn.academy;

import cn.academy.platform.config.ConfigSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aller-retour du fichier de configuration neutre.
 *
 * <p>Le format doit rester celui que Forge ecrivait : un fichier existant est relu tel quel, et
 * ce que l'ecran de reglage ecrit doit se retrouver au demarrage suivant.
 */
class ConfigSpecTest {

    /** Un jeu de valeurs qui couvre chaque type declare par le mod. */
    private static final class Sample {
        final ConfigSpec.BooleanValue enabled;
        final ConfigSpec.IntValue cooldown;
        final ConfigSpec.DoubleValue scale;
        final ConfigSpec.StringValue name;
        final ConfigSpec.DoubleListValue table;
        final ConfigSpec.ListValue blocks;

        Sample(Path file) {
            ConfigSpec.Builder builder = new ConfigSpec.Builder();
            builder.push("general");
            enabled = builder.define("enabled", true);
            cooldown = builder.defineInRange("cooldown", 15, 0, 100);
            builder.pop();
            builder.push("ability");
            scale = builder.defineInRange("scale", 1.0d, 0.0d, 10.0d);
            name = builder.define("name", "academy");
            table = builder.defineList("table", List.of(1.0, 2.0, 3.0), 0.0d, 100.0d);
            blocks = builder.defineList("blocks", List.of("minecraft:iron_block"), entry -> true);
            builder.pop();
            spec = builder.build(file);
        }

        final ConfigSpec spec;
    }

    @Test
    void valuesSurviveAnAllerRetour(@TempDir Path dir) {
        Path file = dir.resolve("academy-common.toml");
        Sample first = new Sample(file);
        first.enabled.set(false);
        first.cooldown.set(42);
        first.scale.set(2.5d);
        first.name.set("monde \"cite\"");
        first.table.set(List.of(4.0, 5.0));
        first.blocks.set(List.of("minecraft:iron_block", "minecraft:hopper"));
        first.spec.save();

        Sample second = new Sample(file);
        second.spec.load();

        assertFalse(second.enabled.get(), "le booleen doit etre relu");
        assertEquals(42, second.cooldown.get());
        assertEquals(2.5d, second.scale.get(), 1.0e-9);
        assertEquals("monde \"cite\"", second.name.get(), "les guillemets doivent survivre");
        assertEquals(List.of(4.0, 5.0), second.table.get());
        assertEquals(List.of("minecraft:iron_block", "minecraft:hopper"), second.blocks.get());
    }

    @Test
    void outOfRangeAndGarbageKeepTheDefault(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("academy-common.toml");
        java.nio.file.Files.writeString(file, "[general]\n"
                + "enabled = maybe\n"
                + "cooldown = 999\n"
                + "[ability]\n"
                + "scale = -1.0\n"
                + "name = \"lu\"\n");

        Sample sample = new Sample(file);
        sample.spec.load();

        assertTrue(sample.enabled.get(), "une valeur illisible laisse le defaut");
        assertEquals(15, sample.cooldown.get(), "hors bornes, le defaut revient");
        assertEquals(1.0d, sample.scale.get(), 1.0e-9);
        assertEquals("lu", sample.name.get());
    }
}
