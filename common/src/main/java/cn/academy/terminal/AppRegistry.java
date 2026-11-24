package cn.academy.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registre des applications du terminal.
 *
 * Portage de {@code AppRegistry}. L'original triait par priorite via
 * l'annotation {@code @RegApp} et une passe de reflexion ; ici l'ordre
 * d'enregistrement dans {@link TerminalInit} suffit, il est explicite et lisible.
 *
 * Instance plutot que classe statique, pour la meme raison que
 * {@code CategoryManager} : les tests unitaires partent d'un registre propre au
 * lieu de polluer celui du jeu.
 */
public final class AppRegistry {

    public static final AppRegistry INSTANCE = new AppRegistry();

    private final List<App> apps = new ArrayList<>();
    private boolean baked;

    /** Visible au paquet pour que les tests partent d'un registre vide. */
    AppRegistry() {}

    public void register(App app) {
        if (baked) throw new IllegalStateException("Cannot register app after bake()");
        if (getByName(app.getName()) != null) {
            throw new IllegalStateException("Duplicate app " + app.getName());
        }
        app.setAppId(apps.size());
        apps.add(app);
    }

    /**
     * Ferme le registre.
     *
     * Les identifiants sont attribues a l'enregistrement, donc l'ordre est deja
     * fixe : ce bake ne sert qu'a interdire un enregistrement tardif, quand une
     * application pourrait deja avoir ete sauvegardee dans la donnee d'un joueur
     * avec un identifiant qui ne voudrait plus rien dire.
     */
    public void bake() {
        baked = true;
    }

    public boolean isBaked() {
        return baked;
    }

    public App get(int id) {
        return id >= 0 && id < apps.size() ? apps.get(id) : null;
    }

    public App getByName(String name) {
        if (name == null) return null;
        for (App app : apps) {
            if (app.getName().equals(name)) return app;
        }
        return null;
    }

    public int size() {
        return apps.size();
    }

    public List<App> all() {
        return Collections.unmodifiableList(apps);
    }
}
