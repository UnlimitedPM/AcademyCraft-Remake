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
    public static final String CLIP_PROGRAM = "skill_clip";
    public static final String EMBLEM_PROGRAM = "cpbar_emblem";

    /** L'uniforme que le programme de l'anneau lit : le remplissage, de zero a un. */
    public static final String RING_PROGRESS_UNIFORM = "SkillProgress";

    /** Ceux du programme d'ecretage : la place de l'icone dans le disque, et son gris. */
    public static final String CLIP_SCALE_UNIFORM = "MaskScale";
    public static final String CLIP_OFFSET_UNIFORM = "MaskOffset";
    public static final String CLIP_GREY_UNIFORM = "Grey";

    @Nullable private static ShaderInstance ring;
    @Nullable private static ShaderInstance mono;
    @Nullable private static ShaderInstance clip;
    @Nullable private static ShaderInstance emblem;

    /** Un type par texture : le construire a chaque image allouerait pour rien. */
    private static final Map<ResourceLocation, RenderType> RING_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> MONO_TYPES = new HashMap<>();
    private static final Map<String, RenderType> CLIP_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> LINE_TYPES = new HashMap<>();
    private static final Map<String, RenderType> EMBLEM_TYPES = new HashMap<>();

    /**
     * La longueur du fondu de chaque bout d'un trait, en unites du document.
     *
     * <p>Mesuree sur les captures du joueur, original contre port : le bout du vrai mod perd son
     * opacite sur six pixels d'ecran, soit un peu moins de deux unites, et il ne s'eteint pas — il se
     * pose a trois dixiemes. Le port dessinait un bout carre et blanc, et c'est ce qu'il a vu : « les
     * bouts des traits blanc n'est pas gris pour nous ».
     */
    private static final double LINE_END_FADE = 2.0d;

    /** L'opacite du bout lui-meme : le trait s'y arrete a trois dixiemes, pas a zero. */
    private static final double LINE_TIP_ALPHA = 0.3d;

    private CGuiShaders() {}

    /** Les textures deja passees au filtre lineaire : on ne le demande qu'une fois chacune. */
    private static final java.util.Set<ResourceLocation> LINEAR = new java.util.HashSet<>();

    /**
     * Demande a la texture elle-meme d'etre lue en <b>lineaire</b>, une fois pour toutes.
     *
     * <p>C'est l'autre route du lissage, et la bonne : celle de l'original, qui chargeait ses images
     * avec {@code preloadMipmapTexture} et les lisait donc filtrees. Le dessin, lui, ne change pas —
     * {@code graphics.blit} respecte le filtre de la texture qu'il lie — et c'est ce qui compte : la
     * premiere tentative du port passait par un type de rendu a nous, et les noeuds de l'arbre ont
     * disparu. Un dessin qui marche vaut mieux qu'un dessin plus savant.
     *
     * <p>Sans mipmaps : ce sont eux qui reouvrent les niveaux de l'image et la vident, et c'est
     * precisement ce qui avait deja fait disparaitre les traits une premiere fois.
     */
    public static void linear(@Nullable ResourceLocation texture) {
        if (texture == null || !LINEAR.add(texture)) return;
        try {
            net.minecraft.client.Minecraft.getInstance().getTextureManager()
                    .getTexture(texture).setFilter(true, false);
        } catch (RuntimeException e) {
            LOGGER.warn("La texture {} reste au plus proche voisin : {}", texture, e.toString());
        }
    }

    /**
     * Cet ecran a-t-il demande le lissage de cette texture ?
     *
     * <p>Le rappel des touches dessine les memes icones et doit les lire au plus proche voisin,
     * comme l'original ; il pose donc son filtre, dessine, puis rend a la texture celui que
     * l'arbre lui avait donne — c'est a cette question qu'il repond.
     */
    public static boolean isLinear(@Nullable ResourceLocation texture) {
        return texture != null && LINEAR.contains(texture);
    }

    /**
     * Un trait de l'arbre, dessine dans le repere courant : l'axe des abscisses suit le trait (de
     * zero a sa longueur), celui des ordonnees le traverse (de zero a son epaisseur).
     *
     * <p>Trois quads et non un seul, parce que l'opacite se donne <b>par sommet</b> : un seul quad ne
     * sait faire qu'une rampe d'un bout a l'autre, et il en faut deux, aux deux extremites. Le corps
     * reste plein, les deux bouts se posent a {@link #LINE_TIP_ALPHA} sur
     * {@link #LINE_END_FADE} unites. La carte interpole le reste, donc le fondu est continu et non
     * par paliers, comme celui du vrai mod.
     *
     * <p>La texture est lue <b>lisse</b>, par l'etat du type et non par un appel a
     * {@link #linear(ResourceLocation)} : c'est ce type qui decrit le dessin du trait, et il doit
     * decrire aussi sa maniere de lire l'image.
     */
    public static void line(GuiGraphics graphics, ResourceLocation texture, double length,
                            double thickness, double scale, int red, int green, int blue,
                            int alpha) {
        if (length <= 0 || thickness <= 0 || alpha <= 0) return;

        RenderType type = LINE_TYPES.computeIfAbsent(texture, key -> RenderType.create(
                "academy_cgui_line", DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getPositionColorTexShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(key, true, false))
                        .setTransparencyState(blending())
                        .setWriteMaskState(noDepthWrite())
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(false)));

        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(type);

        int tip = (int) Math.round(alpha * LINE_TIP_ALPHA);
        // Un trait plus court que deux fondus n'aurait plus de corps : les deux rampes se partagent
        // alors ce qu'il mesure.
        double fade = Math.min(LINE_END_FADE * scale, length / 2.0d);

        segment(buffer, pose, 0.0d, fade, 0.0d, fade / length, thickness, red, green, blue,
                tip, alpha);
        segment(buffer, pose, fade, length - fade, fade / length, (length - fade) / length,
                thickness, red, green, blue, alpha, alpha);
        segment(buffer, pose, length - fade, length, (length - fade) / length, 1.0d, thickness,
                red, green, blue, alpha, tip);

        // Le tampon se vide ici : les dessins du moteur, eux, partent aussitot, et un trait reste en
        // attente se peindrait par-dessus tout ce qui suit — y compris par-dessus son propre disque,
        // qui doit le cacher.
        graphics.flush();
    }

    /**
     * Un morceau de trait : un quad de {@code from} a {@code to} sur la longueur, avec l'opacite de
     * ses deux aretes.
     */
    private static void segment(VertexConsumer buffer, Matrix4f pose, double from, double to,
                                double u0, double u1, double thickness, int red, int green,
                                int blue, int alphaFrom, int alphaTo) {
        buffer.vertex(pose, (float) from, 0f, 0f)
                .color(red, green, blue, alphaFrom).uv((float) u0, 0f).endVertex();
        buffer.vertex(pose, (float) from, (float) thickness, 0f)
                .color(red, green, blue, alphaFrom).uv((float) u0, 1f).endVertex();
        buffer.vertex(pose, (float) to, (float) thickness, 0f)
                .color(red, green, blue, alphaTo).uv((float) u1, 1f).endVertex();
        buffer.vertex(pose, (float) to, 0f, 0f)
                .color(red, green, blue, alphaTo).uv((float) u1, 0f).endVertex();
    }

    /** Charge les deux programmes, chacun de son cote : voir le commentaire de la classe. */
    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        // Un rechargement de ressources repasse par ici : les types gardes tiendraient le programme
        // precedent, il faut donc les refaire.
        ring = null;
        mono = null;
        clip = null;
        emblem = null;
        RING_TYPES.clear();
        MONO_TYPES.clear();
        CLIP_TYPES.clear();
        LINE_TYPES.clear();
        EMBLEM_TYPES.clear();

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

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    program(CLIP_PROGRAM), DefaultVertexFormat.POSITION_COLOR_TEX);
            clip = shader;
            event.registerShader(shader, loaded -> clip = loaded);
        } catch (Exception e) {
            LOGGER.warn("Le programme {} n'a pas pu etre compile : les icones deborderont du disque",
                    CLIP_PROGRAM, e);
        }

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    program(EMBLEM_PROGRAM), DefaultVertexFormat.POSITION_COLOR_TEX);
            emblem = shader;
            event.registerShader(shader, loaded -> emblem = loaded);
        } catch (Exception e) {
            LOGGER.warn("Le programme {} n'a pas pu etre compile : l'embleme du pouvoir "
                    + "s'ecrira en gris au lieu de percer la barre", EMBLEM_PROGRAM, e);
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
     * L'icone d'un noeud, ecretee sur la forme de son disque.
     *
     * <p>C'est ce que l'original obtenait par un tour de profondeur, et ce qui manquait au port : les
     * coins carres de l'icone sortaient du cercle, et le joueur les voyait — « l'image des competences
     * deborde des cercles », « des lignes noires entre la couleur du cercle et les bordures du carre
     * de l'image ». Le disque sert de masque : son alpha multiplie celui de l'icone.
     *
     * @param grey  vrai pour une competence non apprise : l'icone s'eteint, comme le faisait le
     *              programme mono de l'original
     * @return faux quand le programme n'est pas la : l'appelant la dessine alors normalement, ce qui
     *     vaut mieux qu'une icone qui disparait
     */
    public static boolean clipped(GuiGraphics graphics, String iconTexture, String maskTexture,
                                  double iconSize, double discSize, int alpha, boolean grey) {
        ResourceLocation icon = ResourceLocation.tryParse(iconTexture);
        ResourceLocation mask = ResourceLocation.tryParse(maskTexture);
        if (clip == null || icon == null || mask == null || discSize <= 0) return false;

        RenderType type = CLIP_TYPES.computeIfAbsent(icon + "|" + mask, key -> {
            RenderType created = type("academy_skill_clip", () -> clip, icon);
            return created;
        });

        // L'icone fait quatorze unites au milieu du disque qui en fait vingt-trois : ses coordonnees
        // de texture vont donc de 5/23 a 18/23 de celles du disque.
        double scale = iconSize / discSize;
        double offset = (discSize - iconSize) / (2.0d * discSize);
        clip.safeGetUniform(CLIP_SCALE_UNIFORM).set((float) scale, (float) scale);
        clip.safeGetUniform(CLIP_OFFSET_UNIFORM).set((float) offset, (float) offset);
        clip.safeGetUniform(CLIP_GREY_UNIFORM).set(grey ? 1.0f : 0.0f);
        RenderSystem.setShaderTexture(1, mask);

        quad(graphics, type, iconSize / 2, alpha);
        return true;
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
     * Une image grisee, posee dans le repere courant a sa taille exacte.
     *
     * <p>C'est le meme programme que {@link #greyIcon}, mais pour un widget du document : la
     * rangee de conditions de la fiche d'une competence pose ses icones comme n'importe quelle
     * image, et seule la couleur doit changer. Le quad est donc pose a la taille du widget, et non
     * autour de l'origine comme le fait {@code quad}.
     *
     * @return faux quand le programme n'est pas la : l'appelant dessine alors l'image en couleur
     */
    public static boolean greyQuad(GuiGraphics graphics, ResourceLocation texture, double width,
                                   double height, int alpha) {
        if (mono == null || texture == null || width <= 0 || height <= 0) return false;

        RenderType type = MONO_TYPES.computeIfAbsent(texture, key -> type("academy_skill_mono",
                () -> mono, key));

        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(type);

        buffer.vertex(pose, 0f, 0f, 0f).color(255, 255, 255, alpha).uv(0f, 0f).endVertex();
        buffer.vertex(pose, 0f, (float) height, 0f).color(255, 255, 255, alpha).uv(0f, 1f).endVertex();
        buffer.vertex(pose, (float) width, (float) height, 0f).color(255, 255, 255, alpha)
                .uv(1f, 1f).endVertex();
        buffer.vertex(pose, (float) width, 0f, 0f).color(255, 255, 255, alpha).uv(1f, 0f).endVertex();

        graphics.flush();
        return true;
    }

    /**
     * Un morceau du REMPLISSAGE du temoin de CP, perce de l'embleme du pouvoir.
     *
     * <p>Le programme fait ce que faisait le {@code cpbar_cp.frag} de l'original : il retire de
     * l'opacite du remplissage celle de l'icone, dans la boite de 65 unites posee a (857, 43) de
     * l'image de la barre. Le creux laisse donc voir le fond de la barre et le monde derriere, au
     * lieu d'une silhouette grise posee par-dessus.
     *
     * <p>Les coordonnees de texture sont celles de l'IMAGE et non du quad : c'est ce qui permet de
     * dessiner le remplissage par bandes, comme le demande sa coupe en biais, sans deplacer le
     * creux.
     *
     * @return faux quand le programme n'est pas la : l'appelant dessine alors le remplissage plein
     */
    public static boolean emblem(GuiGraphics graphics, ResourceLocation bar, ResourceLocation icon,
                                 double x, double y, double width, double height,
                                 float u0, float v0, float u1, float v1,
                                 int red, int green, int blue, int alpha) {
        if (emblem == null || bar == null || icon == null || width <= 0 || height <= 0) {
            return false;
        }

        RenderType type = EMBLEM_TYPES.computeIfAbsent(bar + "|" + icon,
                key -> type("academy_cpbar_emblem", () -> emblem, bar));

        RenderSystem.setShaderTexture(1, icon);

        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(type);

        buffer.vertex(pose, (float) x, (float) y, 0f).color(red, green, blue, alpha)
                .uv(u0, v0).endVertex();
        buffer.vertex(pose, (float) x, (float) (y + height), 0f).color(red, green, blue, alpha)
                .uv(u0, v1).endVertex();
        buffer.vertex(pose, (float) (x + width), (float) (y + height), 0f)
                .color(red, green, blue, alpha).uv(u1, v1).endVertex();
        buffer.vertex(pose, (float) (x + width), (float) y, 0f).color(red, green, blue, alpha)
                .uv(u1, v0).endVertex();

        graphics.flush();
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
                                GameRenderer::getPositionColorTexShader))
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
