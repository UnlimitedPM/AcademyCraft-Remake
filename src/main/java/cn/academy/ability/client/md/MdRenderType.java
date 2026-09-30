package cn.academy.ability.client.md;

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
 * Le type de rendu des effets du plasma : une image transparente, sans eclairage.
 *
 * <p>C'est celui des eclairs de l'electromaster, mot pour mot — voir {@code ArcRenderer} — et
 * il faut les trois memes choses : pas de tri des faces arriere (une lueur et un ruban n'ont
 * qu'une face), un melange normal et non additif (ajouter fait briller les bords presque
 * transparents, donc epaissit le trait), et surtout <b>aucune lumiere</b> : le programme de la
 * balise de phare n'en connait pas, et c'est ce qu'il faut a une etincelle qui emet la sienne.
 *
 * <p>Les images du meltdowner sont toutes dans le meme cas, donc un seul cache suffit.
 */
public final class MdRenderType {

    /** Un type par image : le construire a chaque image allouerait pour rien. */
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    /** Et celui des tubes, construit a la premiere image : il n'y a rien a y mettre. */
    private static RenderType flat;

    private MdRenderType() {
    }

    /** Le type qui dessine cette image. */
    public static RenderType of(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, MdRenderType::create);
    }

    /**
     * Le type des tubes : une couleur pleine, sans image.
     *
     * <p>C'est le shader « sans texture » de l'original, qui dessinait ses cylindres avec une
     * couleur et rien d'autre. Le port avait trouve une astuce pour s'en passer sur le railgun
     * — il donnait a ses cylindres la ligne blanche du milieu de {@code railgun.png}, qui ne
     * change donc pas leur couleur — mais aucune image du plasma n'a de ligne blanche : mieux
     * vaut le programme de couleur, qui fait exactement ce qu'on lui demande.
     */
    public static RenderType flat() {
        if (flat == null) {
            flat = RenderType.create("academy_md_flat", DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS, 256, false, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(
                                    GameRenderer::getPositionColorShader))
                            .setTransparencyState(blending())
                            .setCullState(new RenderStateShard.CullStateShard(false))
                            .createCompositeState(true));
        }
        return flat;
    }

    private static RenderType create(ResourceLocation texture) {
        return RenderType.create("academy_md", DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeBeaconBeamShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(blending())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(true));
    }

    /** Le melange de l'original, mot pour mot : SRC_ALPHA / ONE_MINUS_SRC_ALPHA. */
    private static RenderStateShard.TransparencyStateShard blending() {
        return new RenderStateShard.TransparencyStateShard("academy_md",
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
