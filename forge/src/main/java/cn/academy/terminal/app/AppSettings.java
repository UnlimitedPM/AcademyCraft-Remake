package cn.academy.terminal.app;

import cn.academy.AcademyCraft;
import cn.academy.terminal.App;
import net.minecraft.resources.ResourceLocation;

/**
 * L'application « Reglages » : les touches, et ce que les prereglages allument.
 *
 * <p>Portage de {@code AppSettings}, dont l'original faisait un ecran de proprietes — les
 * touches configurables, les reglages du mod. Le port n'a pas d'editeur de configuration en
 * jeu, et n'en aura pas besoin tant que les options du jeu suffisent ; ce qu'il garde de
 * l'application, c'est sa vraie matiere : <b>les prereglages de touches</b>, qui n'ont pas
 * d'autre endroit naturel dans le terminal.
 *
 * <p>Elle ouvre donc l'ecran des prereglages, celui que la touche dediee ouvre aussi — le
 * meme ecran, atteint par deux chemins, comme l'original qui l'ouvrait depuis son interface
 * de reglages et depuis une touche.
 *
 * <p>Elle s'installe avec un objet, comme l'arbre de competences : l'original en faisait un
 * objet a fabriquer, et rien ne justifie de l'offrir d'office a un joueur qui n'a pas encore
 * d'aptitudes a ranger.
 */
public final class AppSettings extends App {

    public static final AppSettings INSTANCE = new AppSettings();

    private AppSettings() {
        super("settings");
        // L'original l'installait d'office (`setPreInstalled` dans son AppSettings) : sans
        // cela, l'application n'existe que si on installe son objet, et cet objet n'a
        // aucune recette — l'ecran de reglage du mod serait donc introuvable en jeu.
        setPreInstalled();
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/apps/settings/icon.png");
    }

    @Override
    public int getIconSize() {
        return 110;
    }
}
