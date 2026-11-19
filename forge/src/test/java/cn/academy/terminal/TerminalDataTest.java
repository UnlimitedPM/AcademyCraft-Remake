package cn.academy.terminal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'etat du terminal d'un joueur : installation, applications, sauvegarde.
 *
 * Les applications sont des doublures locales, donc le test ne depend pas du
 * registre du jeu et ne le remplit pas.
 */
class TerminalDataTest {

    private static final class FakeApp extends App {
        FakeApp(String name) {
            super(name);
        }
    }

    private static AppRegistry registryWith(String... names) {
        AppRegistry registry = new AppRegistry();
        for (String name : names) {
            registry.register(new FakeApp(name));
        }
        registry.bake();
        return registry;
    }

    @Test
    void unJoueurNeufNAPasDeTerminal() {
        TerminalData data = new TerminalData();

        assertFalse(data.isTerminalInstalled());
        assertEquals(0, data.getInstalledCount());
        assertTrue(data.getInstalledApps(registryWith("skill_tree")).isEmpty());
    }

    @Test
    void installerLeTerminalNeMarcheQuUneFois() {
        TerminalData data = new TerminalData();

        assertTrue(data.install(), "la premiere installation change l'etat");
        assertTrue(data.isTerminalInstalled());
        assertFalse(data.install(), "la seconde ne change rien");
    }

    @Test
    void installerUneApplicationNeMarcheQuUneFois() {
        TerminalData data = new TerminalData();
        App app = new FakeApp("skill_tree");

        assertTrue(data.installApp(app));
        assertTrue(data.isInstalled(app));
        assertFalse(data.installApp(app), "installer deux fois la meme application ne change rien");
        assertEquals(1, data.getInstalledCount());
    }

    @Test
    void uneApplicationPreinstalleeCompteSansObjetDInstallation() {
        TerminalData data = new TerminalData();
        App app = new FakeApp("about").setPreInstalled();

        assertTrue(app.isPreInstalled());
        assertTrue(data.isInstalled(app));
        // Elle n'est pas dans la liste installee par le joueur : c'est le drapeau
        // de l'application qui la rend disponible, pas sa donnee.
        assertEquals(0, data.getInstalledCount());
    }

    @Test
    void laListeDesApplicationsSuitLOrdreDuRegistre() {
        AppRegistry registry = registryWith("skill_tree", "settings", "media_player");
        TerminalData data = new TerminalData();
        data.installApp(registry.getByName("media_player"));
        data.installApp(registry.getByName("skill_tree"));

        List<App> installed = data.getInstalledApps(registry);

        assertEquals(2, installed.size());
        assertEquals("skill_tree", installed.get(0).getName(), "le registre donne l'ordre, pas l'installation");
        assertEquals("media_player", installed.get(1).getName());
    }

    @Test
    void uneApplicationNonInstalleeNEstPasDansLaListe() {
        AppRegistry registry = registryWith("skill_tree", "settings");
        TerminalData data = new TerminalData();
        data.installApp(registry.getByName("skill_tree"));

        List<App> installed = data.getInstalledApps(registry);

        assertEquals(1, installed.size());
        assertFalse(data.isInstalled(registry.getByName("settings")));
    }

    @Test
    void laSauvegardeConserveLInstallationEtLesApplications() {
        AppRegistry registry = registryWith("skill_tree");
        TerminalData data = new TerminalData();
        data.install();
        data.installApp(registry.getByName("skill_tree"));

        TerminalData reloaded = new TerminalData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertTrue(reloaded.isTerminalInstalled());
        assertTrue(reloaded.isInstalled(registry.getByName("skill_tree")));
        assertEquals(1, reloaded.getInstalledCount());
    }

    @Test
    void uneSauvegardeVideNeSeRelitPasCommeInstallee() {
        TerminalData reloaded = new TerminalData();
        reloaded.deserializeNBT(new CompoundTag());

        assertFalse(reloaded.isTerminalInstalled());
        assertEquals(0, reloaded.getInstalledCount());
    }

    @Test
    void unNomDApplicationInconnuEstConserve() {
        // Cas reel : une application retiree du mod apres qu'un joueur l'a installee.
        // La donnee ne doit pas exploser a la relecture, ni perdre les autres.
        TerminalData data = new TerminalData();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("installed", true);
        ListTag apps = new ListTag();
        apps.add(StringTag.valueOf("disparue"));
        apps.add(StringTag.valueOf("skill_tree"));
        tag.put("apps", apps);

        data.deserializeNBT(tag);

        assertEquals(2, data.getInstalledCount());
        assertTrue(data.isInstalled(new FakeApp("disparue")));
    }

    @Test
    void resetOublieTout() {
        AppRegistry registry = registryWith("skill_tree");
        TerminalData data = new TerminalData();
        data.install();
        data.installApp(registry.getByName("skill_tree"));

        data.reset();

        assertFalse(data.isTerminalInstalled());
        assertEquals(0, data.getInstalledCount());
    }

    @Test
    void copyFromTransfereLEtatSansPartagerLaListe() {
        AppRegistry registry = registryWith("skill_tree", "settings");
        TerminalData original = new TerminalData();
        original.install();
        original.installApp(registry.getByName("skill_tree"));

        TerminalData copy = new TerminalData();
        copy.copyFrom(original);
        original.installApp(registry.getByName("settings"));

        assertTrue(copy.isTerminalInstalled());
        assertTrue(copy.isInstalled(registry.getByName("skill_tree")));
        assertFalse(copy.isInstalled(registry.getByName("settings")),
                "la copie ne doit pas suivre les changements de l'original");
        assertEquals(1, copy.getInstalledCount());
    }
}
