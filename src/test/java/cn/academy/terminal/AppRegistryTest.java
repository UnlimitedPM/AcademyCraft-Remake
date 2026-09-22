package cn.academy.terminal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le registre des applications : les identifiants se suivent, les doublons sont
 * refuses, et un registre ferme n'accepte plus rien.
 *
 * Le registre global n'est pas touche : les tests construisent le leur, comme
 * {@code CategoryManagerTest}.
 */
class AppRegistryTest {

    /** Une application sans contenu, juste pour peupler le registre. */
    private static final class FakeApp extends App {
        FakeApp(String name) {
            super(name);
        }
    }

    @Test
    void unRegistreVideNeContientRien() {
        AppRegistry registry = new AppRegistry();
        assertEquals(0, registry.size());
        assertNull(registry.get(0));
        assertNull(registry.getByName("skill_tree"));
    }

    @Test
    void lesIdentifiantsSeSuiventDansLOrdreDEnregistrement() {
        AppRegistry registry = new AppRegistry();
        App first = new FakeApp("first");
        App second = new FakeApp("second");
        registry.register(first);
        registry.register(second);

        assertEquals(0, first.getAppId());
        assertEquals(1, second.getAppId());
        assertEquals(2, registry.size());
        assertEquals(first, registry.get(0));
        assertEquals(second, registry.get(1));
    }

    @Test
    void getByNameRetrouveUneApplicationOuRenvoieNull() {
        AppRegistry registry = new AppRegistry();
        App app = new FakeApp("skill_tree");
        registry.register(app);

        assertEquals(app, registry.getByName("skill_tree"));
        assertNull(registry.getByName("settings"));
        assertNull(registry.getByName(null));
    }

    @Test
    void deuxApplicationsDuMemeNomSontRefusees() {
        AppRegistry registry = new AppRegistry();
        registry.register(new FakeApp("skill_tree"));

        assertThrows(IllegalStateException.class, () -> registry.register(new FakeApp("skill_tree")));
    }

    @Test
    void unIdentifiantHorsBornesRenvoieNull() {
        AppRegistry registry = new AppRegistry();
        registry.register(new FakeApp("skill_tree"));

        assertNull(registry.get(-1));
        assertNull(registry.get(1));
        assertNotNull(registry.get(0));
    }

    @Test
    void unRegistreFermeNAcceptePlusRien() {
        AppRegistry registry = new AppRegistry();
        registry.register(new FakeApp("skill_tree"));
        registry.bake();

        assertTrue(registry.isBaked());
        assertThrows(IllegalStateException.class, () -> registry.register(new FakeApp("settings")));
    }

    @Test
    void laListeDesApplicationsEstNonModifiable() {
        AppRegistry registry = new AppRegistry();
        registry.register(new FakeApp("skill_tree"));

        List<App> listed = registry.all();
        assertThrows(UnsupportedOperationException.class, () -> listed.add(new FakeApp("settings")));
        assertThrows(UnsupportedOperationException.class, listed::clear);
    }
}
