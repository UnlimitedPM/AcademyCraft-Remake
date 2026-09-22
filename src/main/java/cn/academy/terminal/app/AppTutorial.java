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
 * <p>Une difference avec l'original : le tirage a lieu <b>une fois</b>, a la
 * construction, et non a chaque appel. L'original tirait a chaque rendu, donc son icone
 * changeait plusieurs fois par seconde ; le port garde le hasard, pas le scintillement.
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

    private final int iconIndex;
    private final int iconSize;

    private AppTutorial() {
        super("tutorial");
        setPreInstalled();

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
