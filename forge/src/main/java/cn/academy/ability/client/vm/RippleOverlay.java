package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.ClientCharge;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Matrix4f;

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
 *
 * <p>Et les carres se posent <b>tout de suite</b>, la ou un {@code blit} passe par le tampon du
 * HUD. Ce n'est pas un gout : un blit arrondit la position <b>et</b> la taille, et leurs deux
 * arrondis ne tombent pas ensemble — le centre du rond sautait d'un pixel en grandissant, et
 * l'ondulation vibrait. Le joueur l'a vu : « le centre du rond n'est jamais au meme endroit, ce
 * qui fait que le cercle vibre et ce n'est pas normal ». Le carre se pose donc a sa place exacte,
 * et seule sa taille bouge. Le tampon du HUD se vide avant, sinon nos carres passeraient sous ce
 * qui a ete pose avant eux.
 */
@OnlyIn(Dist.CLIENT)
public class RippleOverlay implements IGuiOverlay {

    /** L'image de l'original, telle quelle : le meme disque de lueur que les ondes de choc. */
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/glow_circle.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth,
                       int screenHeight) {
        // L'ondulation cherche SA competence dans les charges ouvertes, de la plus recente a la
        // plus ancienne : la veille qui la produit n'est pas forcement la derniere ouverte, puisque
        // le joueur peut tenir une veille et charger autre chose en meme temps. Voir ClientCharge.
        WaveRipples.Settings settings = null;
        for (String open : ClientCharge.openSkills()) {
            settings = WaveRipples.forSkill(open);
            if (settings != null) break;
        }
        if (settings == null) {
            WaveRipples.clear();
            return;
        }

        WaveRipples.frame(WaveRipples.now(), screenWidth, screenHeight, settings);
        if (WaveRipples.live().isEmpty()) return;

        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GLOW);

        Matrix4f pose = graphics.pose().last().pose();
        for (WaveRipples.Ripple ripple : WaveRipples.live()) {
            float alpha = (float) (settings.alpha() * ripple.alpha());
            if (alpha <= 0f) continue;

            double size = WaveRipples.drawnSize(ripple);
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            quad(pose, WaveRipples.cornerX(ripple, screenWidth),
                    WaveRipples.cornerY(ripple, screenHeight), size);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /**
     * Un rond : le carre de l'image, pose a sa place exacte.
     *
     * <p>Meme nuanceur et meme format que le {@code blit} du HUD — c'est un carre de texture, et
     * rien d'autre —, a ceci pres que rien n'est arrondi.
     */
    private static void quad(Matrix4f pose, double x0, double y0, double size) {
        double x1 = x0 + size;
        double y1 = y0 + size;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(pose, (float) x0, (float) y0, 0f).uv(0f, 0f).endVertex();
        buffer.vertex(pose, (float) x0, (float) y1, 0f).uv(0f, 1f).endVertex();
        buffer.vertex(pose, (float) x1, (float) y1, 0f).uv(1f, 1f).endVertex();
        buffer.vertex(pose, (float) x1, (float) y0, 0f).uv(1f, 0f).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }
}
