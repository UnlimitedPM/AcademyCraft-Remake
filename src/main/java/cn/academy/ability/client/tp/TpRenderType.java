package cn.academy.ability.client.tp;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Les types de rendu des effets de la teleportation : les etincelles, le sang, et la boite.
 *
 * <p>Le <b>fantome</b> n'est pas ici, et c'est volontaire : il se dessine tout de suite, avec son
 * etat pose a la main, parce qu'un type de rendu <b>range</b> rebat son propre etat de profondeur
 * au moment du vidage — voir {@code TpMarkRenderer.drawMark}, et le rendu de la detection de
 * minerais, qui a paye cette lecon avant lui.
 *
 * <p>Les trois qui restent se voient a travers leur type, et leur profondeur, elle, doit rester
 * <b>fermee</b> : une etincelle est de la fumee, du sang qui traverse les murs se lit comme un
 * defaut, et la boite designe un endroit du monde.
 *
 * <p>L'eclairage des deux premiers est celui des rayons du plasma — le programme de la balise de
 * phare, qui ne connait ni la lumiere du monde ni celle des faces. L'original avait fait de meme
 * avec son {@code ShaderSimple} et son {@code glDisable(GL_LIGHTING)} : ce sont des aplats pales,
 * pas des personnages ombres.
 *
 * <p>Les sommets des etincelles et du sang sont ceux d'une <b>image</b> — position, couleur, image
 * —, parce que ce sont des carres ; ceux de la boite n'ont qu'une couleur a montrer.
 */
public final class TpRenderType {

    /** Un seul type pour les etincelles, qui n'ont qu'une image — et le sang se dessine pareil. */
    private static final Map<ResourceLocation, RenderType> PARTICLES = new HashMap<>();

    /** Et un seul pour la boite, dont les traits n'ont pas d'image du tout. */
    private static RenderType BOX;

    private TpRenderType() {
    }

    /**
     * Le type des <b>etincelles</b> — et du <b>sang</b>, qui se dessine de la meme facon.
     *
     * <p>Un carre d'image pose dans le plan de l'ecran, sans ecriture de profondeur mais avec son
     * test : ces deux-la ne traversent rien. L'original ne leur avait pas donne le meme regime
     * qu'a la marque, et c'est juste : le fantome indique un endroit a travers le mur qu'on
     * s'apprete a franchir, mais une etincelle est de la fumee, et du sang qui passe les murs se
     * lit comme un defaut.
     */
    public static RenderType particle(ResourceLocation texture) {
        return PARTICLES.computeIfAbsent(texture, TpRenderType::createParticle);
    }

    /**
     * Le type de la <b>boite</b> : des rubans de couleur, sans image.
     *
     * <p>Le fantome et les etincelles sont des quads textures ; les traits de la boite, eux, n'ont
     * qu'une couleur a montrer. C'est ce que dessine le programme {@code position_color}, celui des
     * aplats — le meme genre de choix que le {@code ShaderNotex} de l'original, qui dessinait sa
     * boite sans texture et sans eclairage.
     *
     * <p>Le test de profondeur, lui, reste allume : la boite marque un endroit du monde, elle n'a
     * pas a se voir au travers des murs comme le fantome.
     */
    public static RenderType box() {
        if (BOX == null) {
            BOX = createBox();
        }
        return BOX;
    }

    private static RenderType createBox() {
        return RenderType.create("academy_tp_box", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getPositionColorShader))
                        .setTransparencyState(blending())
                        .setWriteMaskState(noDepthWrite())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(true));
    }

    private static RenderType createParticle(ResourceLocation texture) {
        return RenderType.create("academy_tp_particle", DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeBeaconBeamShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(blending())
                        .setWriteMaskState(noDepthWrite())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(true));
    }

    /** La couleur s'ecrit, la profondeur non : le {@code glDepthMask(false)} de l'original. */
    private static RenderStateShard.WriteMaskStateShard noDepthWrite() {
        return new RenderStateShard.WriteMaskStateShard(true, false);
    }

    /** Le melange de l'original, mot pour mot : SRC_ALPHA / ONE_MINUS_SRC_ALPHA. */
    private static RenderStateShard.TransparencyStateShard blending() {
        return new RenderStateShard.TransparencyStateShard("academy_tp_blend",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                },
                () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                });
    }
}
