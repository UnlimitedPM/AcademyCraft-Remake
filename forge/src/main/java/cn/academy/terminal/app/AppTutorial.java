package cn.academy.terminal.app;

import cn.academy.AcademyCraft;
import cn.academy.terminal.App;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * L'application « MisakaCloud » : les tutoriels du mod.
 *
 * <p>Portage de {@code AppTutorial}. Comme l'original, elle est installee d'office —
 * c'est la documentation, et un terminal qui n'aurait qu'un ecran de credits ne
 * montrerait rien de ce que le mod sait faire — et son icone est tiree au hasard parmi
 * les trois de l'original, « pour le plaisir » comme il le disait lui-meme.
 *
 * <p>Le tirage a lieu <b>a chaque ouverture du terminal</b> : le joueur l'a demande — « l'icone
 * de MisakaCloud ne change pas aleatoirement a chaque fois que je ferme et que je reouvre le
 * terminal ». L'original tirait a chaque RENDU, donc son icone changeait plusieurs fois par
 * seconde ; ici le hasard est garde, le scintillement non.
 */
public final class AppTutorial extends App {

    /**
     * Les trois icones de l'original, avec la taille de chacune.
     *
     * <p>Declarees <b>avant</b> l'instance : les initialiseurs statiques s'executent dans
     * l'ordre du texte, et l'instance les lit dans son constructeur. Placees apres, elles
     * valent encore {@code null}, et le mod ne demarre pas — c'est arrive.
     */
    private static final int[][] ICONS = { { 0, 110 }, { 1, 110 }, { 2, 100 } };

    public static final AppTutorial INSTANCE = new AppTutorial();

    private int iconIndex;
    private int iconSize;

    private AppTutorial() {
        super("tutorial");
        setPreInstalled();
        rerollIcon();
    }

    /** Tire une des trois icones au hasard. Appele a chaque ouverture du terminal. */
    public void rerollIcon() {
        int[] picked = ICONS[new Random().nextInt(ICONS.length)];
        iconIndex = picked[0];
        iconSize = picked[1];
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/apps/tutorial/icon_" + iconIndex + ".png");
    }

    @Override
    public int getIconSize() {
        return iconSize;
    }
}
