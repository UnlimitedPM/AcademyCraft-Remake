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
 * <h2>Et une quatrieme, que le joueur a fait ajouter</h2>
 *
 * <p>Les effets du plasma <b>n'ecrivent pas la profondeur</b>. C'est encore l'original qui le
 * dit — son entite de bille posait {@code glDepthMask(false)} avant de dessiner ses deux
 * carres — et ce n'est pas une precaution de style. La bille, par exemple, est faite de deux
 * images <b>coplanaires</b>, posees exactement au meme endroit : le halo de 0,7 bloc et le
 * coeur de 0,5. Si elles ecrivent la profondeur, elles se la disputent au micron pres, et
 * selon l'arrondi du fragment c'est l'une ou l'autre qui passe : des morceaux du coeur
 * disparaissent, et l'image parait coupee en deux. Le joueur l'a decrit mot pour mot :
 * « pendant que la bille se forme, parfois on a l'impression que l'image se fait couper en 2
 * [...] peut-etre parce que 2 images s'affichent en meme temps et que quand il y en a une qui
 * passe devant l'autre ca fait ca ». C'etait exactement ca.
 *
 * <p>Le <b>test</b> de profondeur, lui, reste : un rayon ne traverse toujours pas une montagne,
 * et une bille reste cachee derriere un mur.
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
                            .setWriteMaskState(noDepthWrite())
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
                        .setWriteMaskState(noDepthWrite())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(true));
    }

    /**
     * La couleur s'ecrit, la profondeur non — le {@code glDepthMask(false)} de l'original.
     *
     * <p>Voir le commentaire de la classe : c'est ce qui empeche le halo et le coeur de la
     * bille, coplanaires, de se couper l'un l'autre.
     */
    private static RenderStateShard.WriteMaskStateShard noDepthWrite() {
        return new RenderStateShard.WriteMaskStateShard(true, false);
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
