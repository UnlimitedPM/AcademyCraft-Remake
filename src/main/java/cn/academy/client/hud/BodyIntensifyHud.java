package cn.academy.client.hud;

import cn.academy.ability.client.BodyIntensifyEffect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * L'electricite du renfort sur l'ecran : le voile bleu, et les arcs qui y scintillent.
 *
 * <p>Portage de {@code CurrentChargingHUD}. Tant que le joueur tient la touche, l'original
 * couvrait l'ecran de son image {@code em_intensify_mask}, entree en matiere en une demi-seconde,
 * et faisait scintiller cinq ou six arcs d'ecran par-dessus. Au relachement, la gerbe remplacait
 * ceux-ci et le tout s'effacait en deux dixiemes de seconde.
 *
 * <p>Ce que le port ne reprend PAS : le noir a 10 pour cent que l'original posait sous son image.
 * Le joueur l'a vu tout de suite — « l'ecran noir n'est pas bon » — et il avait raison : le voile
 * de l'original est bleu, et c'est sa teinte qui doit passer, pas un fond sombre.
 *
 * <p>Sa place est au-dessus de tout le reste : chez l'original c'etait une {@code AuxGui}, donc
 * dessinee apres le HUD.
 *
 * <p>Les positions et les tailles des arcs sont en unites de demi-ecran et en pixels, comme chez
 * lui : voir {@code BodyIntensifyEffect.HudArc}.
 */
@OnlyIn(Dist.CLIENT)
public final class BodyIntensifyHud {

    private static final ResourceLocation MASK = ResourceLocation.fromNamespaceAndPath(
            "academy", "textures/effects/em_intensify_mask.png");

    private BodyIntensifyHud() {
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        float mask = BodyIntensifyEffect.maskAlpha();
        List<BodyIntensifyEffect.HudArc> arcs = BodyIntensifyEffect.hudArcs();
        if (mask <= 0f && arcs.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (mask > 0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, mask);
            // L'original etirait son image sur tout l'ecran : c'est ce qu'il faut, et rien
            // d'autre.
            graphics.blit(MASK, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        }

        float alpha = BodyIntensifyEffect.arcAlpha();
        if (alpha > 0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            for (BodyIntensifyEffect.HudArc arc : arcs) {
                if (!arc.visible()) continue;

                int size = (int) arc.size();
                int x = (int) (arc.x() * screenWidth / 2.0 - size / 2.0);
                int y = (int) (arc.y() * screenHeight / 2.0 - size / 2.0);
                // La zone lue et la taille declaree valent la taille d'affichage : les UV
                // restent donc dans 0..1 quelle que soit la dimension du fichier, et l'image
                // est etiree. C'est ce que faisait l'original, qui dessinait un rectangle la ou
                // sa texture etait liee sans jamais lire ses dimensions.
                graphics.blit(arc.texture(), x, y, size, size, 0f, 0f, size, size, size, size);
            }
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
