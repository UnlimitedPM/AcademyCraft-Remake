package cn.academy.terminal.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Les images des tutoriels : leur taille, lue une fois.
 *
 * <p>Le markdown donne un chemin ({@code academy:textures/tutorial/skill_tree_ui.png}) et
 * rien d'autre : c'est l'ecran qui decide de la largeur, et il lui faut la hauteur pour
 * garder les proportions. L'original la lisait par son {@code GLMarkdownRenderer} ; ici, on
 * ouvre l'image avec le gestionnaire de ressources et on retient les deux nombres.
 *
 * <p>Une image absente rend {@code null}, et l'ecran la saute : un tutoriel sans sa capture
 * reste lisible, plutot que de faire tomber l'ecran.
 */
@OnlyIn(Dist.CLIENT)
public final class TutorialImages {

    private static final Map<String, int[]> SIZES = new HashMap<>();

    private TutorialImages() {}

    /** La taille en pixels de l'image, ou {@code null} si elle ne se lit pas. */
    public static int[] size(String path) {
        int[] known = SIZES.get(path);
        if (known != null) return known;

        int[] size = null;
        ResourceLocation location = ResourceLocation.tryParse(path);
        if (location != null) {
            try (var resource = Minecraft.getInstance().getResourceManager()
                    .getResource(location).orElseThrow().open();
                 NativeImage image = NativeImage.read(resource)) {
                size = new int[] { image.getWidth(), image.getHeight() };
            } catch (Exception e) {
                // Un fichier absent ne doit pas empecher l'ecran de s'ouvrir.
                size = null;
            }
        }
        SIZES.put(path, size);
        return size;
    }
}
