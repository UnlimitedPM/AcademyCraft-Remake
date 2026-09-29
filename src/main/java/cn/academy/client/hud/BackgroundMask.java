package cn.academy.client.hud;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.client.ClientAbilityData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Le voile d'ecran de l'original, plein cadre, pose <b>derriere</b> le HUD.
 *
 * <p>Portage de {@code BackgroundMask}. L'original enveloppait tout l'ecran de la couleur de la
 * categorie du joueur des que son aptitude etait allumee, et le passait au rouge pendant une
 * surcharge — le tout en fondu, une unite par seconde (voir {@link MaskVisuals}).
 *
 * <p>Il est enregistre <b>avant</b> la barre d'objets, donc derriere le HUD vanilla comme
 * derriere celui de l'Academy : c'est ce que faisait l'original, et c'est ce qui donne au
 * temoin de CP la densite qu'il avait chez lui. Sans lui, sa plaque et sa bande de surcharge
 * lisaient trop pale.
 */
@OnlyIn(Dist.CLIENT)
public class BackgroundMask implements IGuiOverlay {

    private static final ResourceLocation MASK =
            ResourceLocation.fromNamespaceAndPath("academy", "textures/effects/screen_mask.png");

    /** La taille de l'image du voile : 512 sur 288, comme dans l'original. */
    private static final int TEX_W = 512;
    private static final int TEX_H = 288;

    /**
     * La part de l'ecran que l'image couvre en largeur.
     *
     * <p>L'original l'etirait sur tout l'ecran, mais son degrade ne vit que sur les 19 %
     * exterieurs de l'image : la teinte se voyait donc a peine, et le joueur l'a trouvee trop
     * etroite. En resserrant l'image sur les trois quarts de l'ecran, ce degrade occupe le quart
     * exterieur, et les bords de l'ecran tombent sur le bord de l'image — sa partie la plus
     * dense. En hauteur, rien ne bouge : cette image n'a pas de degrade vertical.
     */
    private static final float SPREAD = 0.75f;

    /** Ce que le voile montre en ce moment, ARGB : l'animation a besoin de la valeur precedente. */
    private static int current;
    private static long lastFrame;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        AbilityData data = ClientAbilityData.get();
        Category category = data.getHighestCategory();

        // Une fois par image, avec le temps ecoule depuis la precedente — et jamais plus de
        // 100 ms, pour qu'une pause du jeu ne fasse pas bondir le voile au retour.
        long now = Util.getMillis();
        float seconds = lastFrame == 0L ? 0.0f : Math.min(now - lastFrame, 100L) / 1000.0f;
        lastFrame = now;

        // Une aptitude allumee a toujours une categorie (`isActivated` l'exige), donc la couleur
        // n'est lue que dans ce cas-la : hors de la, la visee n'est qu'une opacite qui tombe.
        int target = MaskVisuals.target(current, data.isOverloaded(), data.isActivated(),
                category == null ? 0 : category.getColorStyle());
        current = MaskVisuals.smooth(current, target, MaskVisuals.step(seconds));

        float alpha = CpBarVisuals.alphaOf(current);
        if (alpha <= 0.0f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((current >> 16) & 0xFF) / 255.0f,
                ((current >> 8) & 0xFF) / 255.0f, (current & 0xFF) / 255.0f, alpha);
        // L'image est posee sur tout l'ecran, mais ses coordonnees couvrent un peu plus que ses
        // 512 pixels : elle est donc resserree au centre, et le bord de l'ecran echantillonne son
        // bord a elle — la partie la plus dense de son degrade, qui se repete faute d'au-dela.
        float span = TEX_W / SPREAD;
        graphics.blit(MASK, 0, 0, screenWidth, screenHeight,
                (TEX_W - span) / 2.0f, 0.0f, Math.round(span), TEX_H, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
