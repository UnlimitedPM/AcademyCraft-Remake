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
        // L'original etirait son image sur tout l'ecran : elle n'est qu'un degrade, sa taille
        // ne compte donc pas — les deux dernieres valeurs la mettent a l'echelle de l'ecran.
        graphics.blit(MASK, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
