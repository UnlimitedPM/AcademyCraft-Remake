package cn.academy.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

/**
 * Le lissage de la police du mod.
 *
 * <p>C'est ce qui rend son texte lisible. Minecraft peint une police TTF dans un atlas deux
 * fois plus grand que la taille ou elle la dessine, puis la reduit — et sa texture de police
 * garde le filtrage <b>au plus proche voisin</b> des interfaces. Cette reduction-la detruit les
 * traits : a l'ecran les lettres sortent cisailees et pales. L'original, lui, reduisait sa
 * police lue par AWT avec un filtrage lisse, d'ou son texte plein et net. Compare au banc
 * d'essai : la meme image reduite des deux facons ne se ressemble pas du tout.
 *
 * <p>L'atlas est enregistre dans le gestionnaire de textures sous le nom de la police suivi du
 * numero de sa page — {@code academy:ac_gui/0} (voir {@code FontSet}). On lui repose donc un
 * filtrage lisse et son drapeau, puis on le refait a chaque image : deux appels par page, ce
 * qui survit aussi bien au rechargement des ressources qu'a l'ajout de glyphes.
 */
@OnlyIn(Dist.CLIENT)
public final class FontSmoothing {

    /** La police du mod, telle qu'elle est declaree dans assets/academy/font/ac_gui.json. */
    private static final ResourceLocation FONT =
            ResourceLocation.fromNamespaceAndPath("academy", "ac_gui");

    /** Le nombre de pages d'atlas a surveiller : la police tient largement sur la premiere. */
    private static final int PAGES = 4;

    private FontSmoothing() {
    }

    /** Repose le filtrage lisse sur les pages de l'atlas de la police du mod. */
    public static void apply() {
        for (int page = 0; page < PAGES; ++page) {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager()
                    .getTexture(FONT.withSuffix("/" + page));
            if (!(texture instanceof FontTexture)) {
                return;
            }

            texture.setFilter(true, false);
            GlStateManager._bindTexture(texture.getId());
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                    GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                    GL11.GL_LINEAR);
        }
    }
}
