package cn.academy.client.hud;

import cn.academy.ability.client.BodyIntensifyEffect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le voile du renfort : l'ecran bleui une fraction de seconde quand il prend.
 *
 * <p>Portage de la partie « masque » de {@code CurrentChargingHUD}. L'original posait, sous ses
 * arcs d'ecran, un noir a 10 % puis son image {@code em_intensify_mask} etiree sur tout l'ecran,
 * le tout a une opacite qui tombait de un a zero en 200 millisecondes. C'est ce court flash qui
 * accompagne le corps qui s'electrise, et c'est tout ce qui reste a montrer du HUD d'origine :
 * le port n'a pas de charge a tenir, donc ni barre de charge, ni arcs d'ecran pendant un
 * maintien — seulement l'eclair de l'activation.
 *
 * <p>Il se dessine par-dessus tout le reste, comme l'{@code AuxGui} de l'original.
 */
@OnlyIn(Dist.CLIENT)
public final class BodyIntensifyHud {

    private static final ResourceLocation MASK = ResourceLocation.fromNamespaceAndPath(
            "academy", "textures/effects/em_intensify_mask.png");

    /** Le noir a 10 % de l'original, pose sous son image. */
    private static final float DIM = 0.1f;

    private BodyIntensifyHud() {
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        float alpha = BodyIntensifyEffect.veilAlpha();
        if (alpha <= 0f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int dim = ((int) (DIM * alpha * 255f)) << 24;
        graphics.fill(0, 0, screenWidth, screenHeight, dim);

        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        // L'original etirait son image sur tout l'ecran : c'est ce qu'il faut, et rien d'autre.
        graphics.blit(MASK, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
