package cn.academy.terminal.app;

import cn.academy.terminal.App;

/**
 * L'application « A propos » : l'en-tete, l'equipe du mod et les donateurs.
 *
 * Portage de {@code AppAbout}. L'original l'installait d'office
 * ({@code setPreInstalled()}), et c'est le cas ici aussi : le terminal doit avoir
 * quelque chose a montrer des sa premiere ouverture, sinon il s'ouvre sur une
 * grille vide et on croit qu'il est casse.
 *
 * L'onglet des dons de l'original n'est pas porte : il ne contenait qu'un texte de
 * campagne, et le service qui alimentait la liste des donateurs n'existe plus.
 * L'ecran s'arrete donc a un onglet, ce qui rend la barre d'onglets inutile.
 */
public final class AppAbout extends App {

    public static final AppAbout INSTANCE = new AppAbout();

    private AppAbout() {
        super("about");
        setPreInstalled();
    }

    @Override
    public int getIconSize() {
        return 128;
    }
}
