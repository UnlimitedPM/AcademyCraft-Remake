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
 * Le type de rendu de la marque de teleportation : un fantome qui se voit <b>a travers les murs</b>.
 *
 * <p>L'original le disait en trois lignes de GL : il eteignait le test de profondeur
 * ({@code glDisable(GL_DEPTH_TEST)}), le tri des faces arriere et l'eclairage, et il activait le
 * melange. C'est bien un choix et non un oubli : la marque indique ou l'on va <b>atterrir</b>, et
 * elle serait inutile si le mur qu'on s'apprete a traverser la cachait. Le joueur voit donc son
 * propre fantome au travers de la pierre, et c'est ce qui rend la competence lisible.
 *
 * <p>L'eclairage est celui des rayons du plasma — le programme de la balise de phare, qui ne
 * connait ni la lumiere du monde ni celle des faces. L'original avait fait de meme avec son
 * {@code ShaderSimple} et son {@code glDisable(GL_LIGHTING)} : la marque est un aplat pale, pas
 * un personnage ombre.
 *
 * <p>Les sommets du fantome, eux, sont ceux d'une <b>entite</b> : un modele ecrit la position, la
 * couleur, l'image, la superposition, la lumiere et la normale. Le programme de la balise n'en lit
 * que les trois premiers, ce qui est exactement ce qu'on lui demande. Les etincelles, elles, sont
 * des carres — donc des sommets d'image ordinaires.
 */
public final class TpRenderType {

    /** Un type par image : la marque en a sept, et les construire a chaque image allouerait. */
    private static final Map<ResourceLocation, RenderType> MARKS = new HashMap<>();

    /** Et un seul pour les etincelles, qui n'ont qu'une image. */
    private static final Map<ResourceLocation, RenderType> PARTICLES = new HashMap<>();

    /** Et un seul pour la boite, dont les traits n'ont pas d'image du tout. */
    private static RenderType BOX;

    private TpRenderType() {
    }

    /**
     * Le type du <b>fantome</b> : il se voit au travers des murs.
     *
     * <p>Les sommets sont ceux d'une <b>entite</b> — position, couleur, image, superposition,
     * lumiere et normale — parce que c'est ce qu'ecrit un modele. Le programme de la balise n'en
     * lit que les trois premiers, ce qui est exactement ce qu'on lui demande.
     */
    public static RenderType mark(ResourceLocation texture) {
        return MARKS.computeIfAbsent(texture, TpRenderType::createMark);
    }

    /**
     * Le type des <b>etincelles</b> : celles-la ne traversent rien.
     *
     * <p>L'original ne leur avait pas donne le meme regime que la marque, et c'est juste : le
     * fantome indique un endroit a travers le mur qu'on s'apprete a franchir, mais ses etincelles
     * sont de la fumee, et de la fumee qui passe les murs se lit comme un defaut.
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

    private static RenderType createMark(ResourceLocation texture) {
        return RenderType.create("academy_tp_mark", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeBeaconBeamShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(blending())
                        .setWriteMaskState(noDepthWrite())
                        .setDepthTestState(noDepthTest())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .setLightmapState(new RenderStateShard.LightmapStateShard(false))
                        .setOverlayState(new RenderStateShard.OverlayStateShard(false))
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

    /**
     * Le test de profondeur, eteint : le {@code glDisable(GL_DEPTH_TEST)} de l'original.
     *
     * <p>{@code 519} est le numero GL de {@code GL_ALWAYS} — le test passe toujours, ce qui revient
     * a ne pas le faire, mais laisse la profondeur telle qu'elle est : le fantome ne se coupe pas
     * lui-meme, et le monde non plus.
     */
    private static RenderStateShard.DepthTestStateShard noDepthTest() {
        return new RenderStateShard.DepthTestStateShard("academy_tp_always", 519);
    }

    /** Le melange de l'original, mot pour mot : SRC_ALPHA / ONE_MINUS_SRC_ALPHA. */
    private static RenderStateShard.TransparencyStateShard blending() {
        return new RenderStateShard.TransparencyStateShard("academy_tp_mark",
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
