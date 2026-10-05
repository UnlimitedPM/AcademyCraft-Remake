package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.ClientCharge;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Le dessin des ondulations du champ de vecmanip : des ronds de lueur par-dessus la vue.
 *
 * <p>L'original le posait a l'etape du <b>reticule</b>, donc sous le HUD : des ronds lumineux
 * apparaissent dans le champ de vision, et rien de plus. Le port les enregistre au-dessus du
 * reticule, ce qui revient au meme — l'ecran est le leur, ils ne genent aucun autre element.
 *
 * <p>Deux pieges de HUD connus s'appliquent ici, et le code s'en garde :
 *
 * <ul>
 *   <li>le <b>melange</b> se rallume avant de dessiner : tout ce que le HUD a deja pose — un texte,
 *       un aplat — a pu l'eteindre, et une lueur sans melange serait un disque plein ;</li>
 *   <li>et la <b>couleur du nuanceur</b> se remet a blanc apres : elle porte l'opacite de chaque
 *       rond, et la laisser en place teindrait tout ce que le jeu dessinerait ensuite.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class RippleOverlay implements IGuiOverlay {

    /** L'image de l'original, telle quelle : le meme disque de lueur que les ondes de choc. */
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/glow_circle.png");

    /** Sa taille, en pixels : l'image est carree, et dessinee en entier. */
    private static final int TEXTURE = 256;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth,
                       int screenHeight) {
        WaveRipples.Settings settings = WaveRipples.forSkill(ClientCharge.getSkill());
        if (settings == null) {
            WaveRipples.clear();
            return;
        }

        WaveRipples.frame(WaveRipples.now(), screenWidth, screenHeight, settings);
        if (WaveRipples.live().isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (WaveRipples.Ripple ripple : WaveRipples.live()) {
            float alpha = (float) (settings.alpha() * ripple.alpha());
            if (alpha <= 0f) continue;

            double size = ripple.drawSize();
            int side = (int) Math.round(size);
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            graphics.blit(GLOW,
                    (int) Math.round(ripple.x() - size / 2),
                    (int) Math.round(ripple.y() - size / 2),
                    side, side, 0f, 0f, TEXTURE, TEXTURE, TEXTURE, TEXTURE);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
