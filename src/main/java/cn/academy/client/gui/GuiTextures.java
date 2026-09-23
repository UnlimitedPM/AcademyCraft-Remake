package cn.academy.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Le filtrage des textures d'interface, comme l'original le faisait.
 *
 * <p>L'original chargeait chacune de ses textures avec un filtrage <b>lineaire</b>
 * ({@code RenderUtils.loadTexture} force GL_LINEAR), alors que Minecraft garde ses textures
 * d'interface au plus proche voisin. Tant qu'une texture est dessinee a sa taille, les deux
 * se valent ; des qu'on la reduit, non. Or cet ecran reduit tout : le panneau de moitie, le
 * rappel des touches a 0,46, les notifications au quart. Au plus proche voisin, un liseré
 * d'un pixel disparait ou double — le bas de la barre de CP, la bordure bleue d'une touche —
 * et en lineaire il s'adoucit, comme chez lui.
 *
 * <p>Le reglage est pose sur la texture elle-meme, donc une seule fois. Un rechargement de
 * ressources recree les textures, d'ou le souvenir de l'identifiant regle : si l'identifiant
 * a change, on repose le filtrage.
 */
@OnlyIn(Dist.CLIENT)
public final class GuiTextures {

    /** L'identifiant de texture deja regle, par texture. */
    private static final Map<ResourceLocation, Integer> DONE = new ConcurrentHashMap<>();

    private GuiTextures() {
    }

    /** Donne a une texture le filtrage lineaire de l'original. */
    public static void linear(ResourceLocation texture) {
        int id = Minecraft.getInstance().getTextureManager().getTexture(texture).getId();
        Integer done = DONE.get(texture);
        if (done != null && done == id) {
            return;
        }
        DONE.put(texture, id);

        GlStateManager._bindTexture(id);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_LINEAR);
    }
}
