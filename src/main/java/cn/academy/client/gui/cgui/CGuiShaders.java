package cn.academy.client.gui.cgui;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Les deux programmes du moteur : l'anneau de progression et le gris des icones eteintes.
 *
 * <h2>Pourquoi deux programmes pour deux dessins</h2>
 *
 * <p>Ni l'un ni l'autre ne s'obtient avec ce que le moteur sait faire. L'anneau se remplit
 * <b>par angle</b> : le masque de l'original porte l'angle en clair — mesure sur l'image, sa composante
 * rouge monte de zero a un sur un tour complet, 52 a zero degre et 203 a cent quatre-vingts — et le
 * programme garde les pixels dont l'angle est sous le remplissage. Un decoupage rectangulaire ne peut
 * donc pas le remplacer : c'est le seul dessin de l'interface du mod qui ne s'obtienne pas avec des
 * rectangles textures.
 *
 * <p>L'icone grisee, elle, demande de lire les trois canaux pour en faire la moyenne. Une teinte grise
 * ne suffirait pas : une teinte <b>multiplie</b>, donc elle assombrit une icone bleue sans l'eteindre.
 * C'est une desaturation qu'il faut, et elle demande le programme.
 *
 * <h2>Un programme qui ne compile pas ne casse rien</h2>
 *
 * <p>Les deux sont charges a l'enregistrement des programmes, et chacun dans son propre essai : un
 * fichier illisible laisse son champ a {@code null} et le dessin s'abstient. Un anneau qui ne se
 * dessine pas vaut mieux qu'un client qui ne demarre pas — et c'est ce qui permet de porter ces deux
 * programmes sans pouvoir les essayer avant de les livrer.
 *
 * <h2>Un type de rendu par texture</h2>
 *
 * <p>L'etat de texture d'un type de rendu se fixe a sa construction, et il est protege : on ne peut pas
 * lui dire « aucune » et lier les deux textures soi-meme. Le port a donc un type <b>par texture</b>,
 * garde dans un cache — c'est deja ce que font les effets du plasma. Le premier sampler vient du type,
 * le second est lie a la main, juste avant le dessin.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class CGuiShaders {

    private static final Logger LOGGER = LoggerFactory.getLogger("academy-cgui");

    /** Le nom du programme de l'anneau, et celui du gris. */
    public static final String RING_PROGRAM = "skill_ring";
    public static final String MONO_PROGRAM = "skill_mono";

    /** L'uniforme que le programme de l'anneau lit : le remplissage, de zero a un. */
    public static final String RING_PROGRESS_UNIFORM = "SkillProgress";

    @Nullable private static ShaderInstance ring;
    @Nullable private static ShaderInstance mono;

    /** Un type par texture : le construire a chaque image allouerait pour rien. */
    private static final Map<ResourceLocation, RenderType> RING_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> MONO_TYPES = new HashMap<>();

    private CGuiShaders() {}

    /** Charge les deux programmes, chacun de son cote : voir le commentaire de la classe. */
    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        // Un rechargement de ressources repasse par ici : les types gardes tiendraient le programme
        // precedent, il faut donc les refaire.
        ring = null;
        mono = null;
        RING_TYPES.clear();
        MONO_TYPES.clear();

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    program(RING_PROGRAM), DefaultVertexFormat.POSITION_COLOR_TEX);
            ring = shader;
            event.registerShader(shader, loaded -> ring = loaded);
        } catch (Exception e) {
            LOGGER.warn("Le programme {} n'a pas pu etre compile : les anneaux ne se dessineront pas",
                    RING_PROGRAM, e);
        }

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    program(MONO_PROGRAM), DefaultVertexFormat.POSITION_COLOR_TEX);
            mono = shader;
            event.registerShader(shader, loaded -> mono = loaded);
        } catch (Exception e) {
            LOGGER.warn("Le programme {} n'a pas pu etre compile : les icones ne seront pas eteintes",
                    MONO_PROGRAM, e);
        }
    }

    private static ResourceLocation program(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, name);
    }

    /**
     * L'anneau d'une competence, au milieu du repere courant.
     *
     * <p>Deux textures sont liees : l'anneau lui-meme, par le type, et le masque, a la main sur l'unite
     * un. Le remplissage part dans l'uniforme du programme avant que le tampon ne soit vide.
     */
    public static void ring(GuiGraphics graphics, String ringTexture, String mask, double size,
                            double fill, int alpha) {
        ResourceLocation circle = ResourceLocation.tryParse(ringTexture);
        ResourceLocation gradient = ResourceLocation.tryParse(mask);
        if (ring == null || circle == null || gradient == null) return;

        RenderType type = RING_TYPES.computeIfAbsent(circle, texture -> type("academy_skill_ring",
                () -> ring, texture));

        ring.safeGetUniform(RING_PROGRESS_UNIFORM).set((float) fill);
        RenderSystem.setShaderTexture(1, gradient);

        quad(graphics, type, size / 2, alpha);
    }

    /**
     * L'icone d'une competence, eteinte.
     *
     * @return faux quand le programme n'est pas la : l'appelant la dessine alors normalement, ce qui
     *     vaut mieux qu'une icone qui disparait
     */
    public static boolean greyIcon(GuiGraphics graphics, String iconTexture, double size, int alpha) {
        ResourceLocation icon = ResourceLocation.tryParse(iconTexture);
        if (mono == null || icon == null) return false;

        RenderType type = MONO_TYPES.computeIfAbsent(icon, texture -> type("academy_skill_mono",
                () -> mono, texture));

        quad(graphics, type, size / 2, alpha);
        return true;
    }

    /**
     * Un type de rendu qui LISSE la texture : mipmaps et filtrage lineaire.
     *
     * <p>Le programme est celui de vanilla â€” seule la maniere de lire la texture change. L'original
     * chargeait ses images par {@code preloadMipmapTexture}, donc mipmappees et filtrees ; le port les
     * passait par {@code graphics.blit}, donc au plus proche voisin. C'est l'escalier que le joueur a
     * vu sur les diagonales : les pixels doux du bord du trait se posaient sur la grille au lieu de
     * se fondre.
     *
     * <p>MIS DE COTE, ET POURQUOI. Le double {@code true} de l'etat de texture demandait des mipmaps,
     * et ce sont {@code TextureUtil.prepareImage} et {@code AbstractTexture.setFilter} qui les
     * fabriquent : ils REOUVRENT tous les niveaux avec des donnees nulles. La texture du trait se
     * vidait donc au premier dessin, et les traits disparaissaient pour de bon. Le joueur l'a vu
     * tout de suite : « je ne vois meme plus les lignes blanches entre les pouvoirs ». Le dessin du
     * trait est revenu a {@code graphics.blit}, qui ne touche pas a la texture. Si un jour on veut
     * vraiment le lissage de l'original, il faudra recharger l'image AVEC ses mipmaps, comme le
     * faisait son {@code preloadMipmapTexture}, et non compter sur l'etat de texture.
     *
     * <p>MIS DE COTE, ET POURQUOI. Le double {@code true} de l'etat de texture demandait des mipmaps,
     * et ce sont {@code TextureUtil.prepareImage} et {@code AbstractTexture.setFilter} qui les
     * fabriquent : ils REOUVRENT tous les niveaux avec des donnees nulles. La texture du trait se
     * vidait donc au premier dessin, et les traits disparaissaient pour de bon. Le joueur l'a vu tout
     * de suite : « je ne vois meme plus les lignes blanches entre les pouvoirs ». Le dessin du trait
     * est revenu a {@code graphics.blit}, qui ne touche pas a la texture. Si un jour on veut vraiment
     * le lissage de l'original, il faudra recharger l'image AVEC ses mipmaps, comme le faisait son
     * {@code preloadMipmapTexture}, et non compter sur l'etat de texture.
     *
     * <p>Le type se garde par texture, comme les deux autres : c'est la texture qui porte le filtre.
     */
    public static RenderType smooth(ResourceLocation texture) {
        return SMOOTH_TYPES.computeIfAbsent(texture, key -> RenderType.create("cgui_smooth",
                DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getPositionTexColorShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(key, false, false))
                        .setTransparencyState(blending())
                        .setWriteMaskState(noDepthWrite())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(false)));
    }

    /** Les types lisses, un par texture : le filtre est pose sur la texture elle-meme. */
    private static final Map<ResourceLocation, RenderType> SMOOTH_TYPES = new HashMap<>();

    /**
     * Un type de rendu qui dessine une texture avec un programme a nous.
     *
     * <p>Pas de tri des faces arriere — une interface n'a qu'une face — le melange des interfaces, et la
     * profondeur ne s'ecrit pas. C'est exactement ce que les effets du plasma demandent, et les trois
     * etats s'ecrivent ici parce que ceux de vanilla sont proteges.
     */
    private static RenderType type(String name, java.util.function.Supplier<ShaderInstance> program,
                                   ResourceLocation texture) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(program))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(blending())
                        .setWriteMaskState(noDepthWrite())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(false));
    }

    /** La couleur s'ecrit, la profondeur non. */
    private static RenderStateShard.WriteMaskStateShard noDepthWrite() {
        return new RenderStateShard.WriteMaskStateShard(true, false);
    }

    /** Le melange des interfaces : la source sur un moins la source. */
    private static RenderStateShard.TransparencyStateShard blending() {
        return new RenderStateShard.TransparencyStateShard("academy_cgui",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                },
                () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                });
    }

    /**
     * Un carre de la taille demandee, au milieu du repere courant, sur toute la texture.
     *
     * <p>La couleur est blanche et c'est l'opacite qui porte le fondu : les deux programmes multiplient
     * ce que la texture contient par ce qu'on leur donne.
     */
    private static void quad(GuiGraphics graphics, RenderType type, double half, int alpha) {
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(type);

        vertex(buffer, pose, -half, -half, 0f, 0f, alpha);
        vertex(buffer, pose, -half, half, 0f, 1f, alpha);
        vertex(buffer, pose, half, half, 1f, 1f, alpha);
        vertex(buffer, pose, half, -half, 1f, 0f, alpha);

        graphics.flush();
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, double x, double y, float u,
                               float v, int alpha) {
        buffer.vertex(pose, (float) x, (float) y, 0f).color(255, 255, 255, alpha).uv(u, v).endVertex();
    }
}
