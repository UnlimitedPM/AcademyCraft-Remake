package cn.academy.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le halo de l'original : huit images posees autour d'une boite.
 *
 * <p>C'est {@code ACRenderingHelper.drawGlow} de la 1.12.2, et il sert a DEUX endroits du HUD :
 * autour de l'icone du rappel des touches, et autour du numero du prereglage en service. Le
 * port l'avait d'abord ecrit dans {@code KeyHintHud}, ou il ne pouvait pas resservir ; il vit
 * ici, et c'est la seule definition.
 *
 * <p>Les huit images ({@code glow_left|right|up|down|ru|rd|lu|ld}) sont des <b>degrades</b> :
 * mesure sur {@code glow_left.png}, 32 pixels de large dont l'opacite va de 22 a 195 d'un bord a
 * l'autre. Resserre dans la taille du halo — cinq unites, un pixel a l'ecran — un degrade ne se
 * lit que s'il est echantillonne <b>au plus proche voisin</b> : en lisse, il ne donne qu'une
 * tache, et le joueur l'a vu tout de suite (« le halo n'est pas bon, comme s'il etait flou »).
 *
 * <p>La teinte passe par la couleur du shader, que {@code blit} laisse derriere lui : chaque
 * morceau la pose donc lui-meme, et c'est l'opacite demandee qui est ecrite.
 */
@OnlyIn(Dist.CLIENT)
public final class GlowSprites {

    private GlowSprites() {
    }

    /**
     * Pose le halo autour d'une boite.
     *
     * @param x      coin gauche de la boite, en unites de la pose en cours
     * @param y      coin haut
     * @param width  largeur de la boite
     * @param height hauteur de la boite
     * @param size   epaisseur du halo : c'est le {@code 5} de l'original
     * @param rgb    la couleur, sans opacite
     * @param alpha  l'opacite du halo, de 0 a 1
     */
    public static void draw(GuiGraphics graphics, double x, double y, double width, double height,
                            double size, int rgb, float alpha) {
        if (alpha <= 0.0f) return;

        int s = Math.max(1, (int) Math.round(size));
        int left = (int) Math.round(x);
        int top = (int) Math.round(y);
        int w = Math.max(1, (int) Math.round(width));
        int h = Math.max(1, (int) Math.round(height));

        // Les quatre cotes, puis les quatre coins : huit morceaux, comme l'original.
        quad(graphics, "left", left - s, top, s, h, rgb, alpha);
        quad(graphics, "right", left + w, top, s, h, rgb, alpha);
        quad(graphics, "up", left, top - s, w, s, rgb, alpha);
        quad(graphics, "down", left, top + h, w, s, rgb, alpha);
        quad(graphics, "ru", left + w, top - s, s, s, rgb, alpha);
        quad(graphics, "rd", left + w, top + h, s, s, rgb, alpha);
        quad(graphics, "lu", left - s, top - s, s, s, rgb, alpha);
        quad(graphics, "ld", left - s, top + h, s, s, rgb, alpha);
    }

    /** Un morceau du halo : l'image entiere, etiree a la forme voulue et teintee. */
    private static void quad(GuiGraphics graphics, String part, int x, int y, int width, int height,
                             int rgb, float alpha) {
        if (width <= 0 || height <= 0) return;
        ResourceLocation texture = texture(part);
        GuiTextures.nearest(texture);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f, alpha);
        graphics.blit(texture, x, y, width, height, 0, 0, 1, 1, 1, 1);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static ResourceLocation texture(String part) {
        return ResourceLocation.fromNamespaceAndPath("academy", "textures/guis/glow_" + part + ".png");
    }
}
