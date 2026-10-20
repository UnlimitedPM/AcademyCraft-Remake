package cn.academy.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.util.HashSet;
import java.util.Set;

/**
 * Les reglages de filtrage des textures d'interface, poses une fois par texture.
 *
 * <p>Un blit de GUI echantillonne au plus proche voisin : une texture dessinee plus petite que sa
 * taille de fichier sort en escalier, et c'est ce que le joueur voit des qu'une page du mod est
 * reduite (« les bords de l'anti-crenelage se voient »). Deux remedes, selon l'echelle :
 *
 * <ul>
 *   <li>{@link #smooth} : lineaire simple, pour une reduction douce (0,8 a 0,9). Les mipmaps y
 *       melangeraient deja un niveau deux fois plus petit et feraient des gros blocs ;</li>
 *   <li>{@link #smoothMipmapped} : lineaire ENTRE niveaux, pour une forte reduction (0,2 a 0,25).
 *       C'est la chaine de {@code preloadMipmapTexture} de l'original — {@code GL_LINEAR_MIPMAP_LINEAR},
 *       {@code GL_LINEAR}, {@code CLAMP}, {@code glGenerateMipmap} — dont se servait la page
 *       « A propos ». Sur une page de 742 dessinee a 185, le niveau 2 est exactement la taille de
 *       l'ecran : c'est l'image la plus nette possible, et son flou est celui de l'original.</li>
 * </ul>
 *
 * <p>On pose les parametres a la main plutot que par {@code AbstractTexture.setFilter} : celui-ci
 * rappelle {@code TextureUtil.prepareImage}, qui REOUVRE les niveaux avec des donnees nulles et
 * VIDE la texture (vecu sur {@code line.png}). Et le repliement est TOUJOURS bloque : GL repete
 * une texture par defaut, donc un filtrage lineaire au bord lit le cote oppose.
 */
@OnlyIn(Dist.CLIENT)
public final class GuiTextures {

    /** Les textures deja reglees, pour ne pas refaire le travail a chaque image. */
    private static final Set<ResourceLocation> SMOOTH = new HashSet<>();
    private static final Set<ResourceLocation> MIPMAPPED = new HashSet<>();

    private GuiTextures() {}

    /** Filtrage lineaire simple, sans mipmaps : pour une reduction douce. */
    public static void smooth(ResourceLocation texture) {
        if (SMOOTH.contains(texture)) return;
        if (apply(texture, false)) SMOOTH.add(texture);
    }

    /** Filtrage lineaire entre niveaux, avec mipmaps : pour une forte reduction. */
    public static void smoothMipmapped(ResourceLocation texture) {
        if (MIPMAPPED.contains(texture)) return;
        if (apply(texture, true)) MIPMAPPED.add(texture);
    }

    /**
     * Pose les parametres du sampler, et rend false si la texture n'est pas encore chargee.
     *
     * <p>La premiere image d'un ecran arrive AVANT que le gestionnaire ait charge la texture : ne
     * marquer fait qu'apres le reglage, sinon la texture reste au plus proche voisin pour toute la
     * session (mesure sur une capture : 88 -> 214 d'un pixel a l'autre, sans valeur intermediaire).
     */
    private static boolean apply(ResourceLocation texture, boolean mipmapped) {
        var loaded = Minecraft.getInstance().getTextureManager().getTexture(texture, null);
        if (loaded == null) return false;

        int id = loaded.getId();
        if (id <= 0) return false;

        GlStateManager._bindTexture(id);

        if (mipmapped) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 4);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                    GL11.GL_LINEAR_MIPMAP_LINEAR);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GlStateManager._bindTexture(0);
        return true;
    }
}
