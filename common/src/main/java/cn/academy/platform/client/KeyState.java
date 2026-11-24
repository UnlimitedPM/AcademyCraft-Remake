package cn.academy.platform.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * L'etat d'une touche, lu sans les methodes que seul Forge ajoute.
 *
 * <p>Vanilla donne a {@code KeyMapping} deux choses que le code commun ne peut pas appeler :
 * {@code getKey()} (la touche liee) et {@code isDown()} (l'appui physique). Forge les ajoute, elles
 * n'existent pas dans les mappings communs — et sans elles une competence ne peut ni reconnaitre le
 * bouton de souris qu'elle occupe, ni savoir qu'une direction de deplacement est tenue.
 *
 * <p>Les deux se refont donc a la main :
 *
 * <ul>
 *   <li>la touche liee se lit par reflexion sur le champ prive {@code key}, comme la fabrique de
 *       {@code RenderType} le fait ailleurs dans le port ;</li>
 *   <li>l'appui se lit <b>physiquement</b>, sur la fenetre : {@code InputConstants.isKeyDown} pour
 *       une touche, GLFW pour un bouton de souris. C'est exactement ce que fait {@code isDown()}
 *       de Forge, et cela vaut donc pour une touche que le joueur a rebindee.</li>
 * </ul>
 */
public final class KeyState {

    private static java.lang.reflect.Field keyField;

    private KeyState() {
    }

    /** La touche liee, ou {@code null} si le champ n'est pas accessible. */
    @Nullable
    public static InputConstants.Key bound(KeyMapping mapping) {
        if (mapping == null) return null;
        try {
            if (keyField == null) {
                java.lang.reflect.Field found = KeyMapping.class.getDeclaredField("key");
                found.setAccessible(true);
                keyField = found;
            }
            return (InputConstants.Key) keyField.get(mapping);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    /** Deux touches sont-elles liees au meme bouton ? */
    public static boolean sameKey(KeyMapping first, KeyMapping second) {
        InputConstants.Key a = bound(first);
        return a != null && a.equals(bound(second));
    }

    /**
     * La touche est-elle enfoncee, physiquement ?
     *
     * <p>Une touche du clavier se lit par {@code InputConstants.isKeyDown} et un bouton de souris par
     * GLFW : le joueur peut rebinder n'importe quelle action sur n'importe quel bouton, et une seule
     * des deux lectures ne connaitrait pas l'autre.
     */
    public static boolean isDown(KeyMapping mapping) {
        InputConstants.Key key = bound(mapping);
        if (key == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() == null) return false;
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue())
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }
}
