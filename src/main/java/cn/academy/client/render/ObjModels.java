package cn.academy.client.render;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Les modeles OBJ du mod, lus depuis les ressources et dessines a la demande.
 *
 * <p>Un modele de bloc ne peut pas bouger : le blockstate le fige la ou il le pose. Pour
 * faire tourner des pales ou des plaques, il faut donc relire le fichier OBJ et le
 * redessiner — c'est ce que fait le rendu des machines animees, comme le faisait
 * l'original avec ses propres modeles.
 */
public final class ObjModels {

    /** Chaque fichier n'est lu qu'une fois : le rendu passe ici a chaque image. */
    private static final Map<ResourceLocation, ObjMesh> CACHE = new HashMap<>();

    /** Un type de rendu par texture : le construire a chaque image allouerait pour rien. */
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    /** Idem, pour les faces qui passent derriere (un recul de profondeur en plus). */
    private static final Map<ResourceLocation, RenderType> BEHIND = new HashMap<>();

    private ObjModels() {}

    /** Le fichier OBJ d'un modele, tel que les modeles de bloc le nomment. */
    public static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "models/" + name + ".obj");
    }

    /**
     * La texture d'un modele.
     *
     * <p>Dans {@code textures/block}, comme TOUS les autres modeles de blocs du port
     * ({@code academy:block/<nom>_model}), et pas dans {@code textures/models} : un modele
     * de bloc OBJ passe par l'atlas des blocs, et une texture referencee depuis
     * {@code textures/models} n'y est pas cousue. Le client le dit alors lui-meme —
     * « Missing textures in model academy:windgen_pillar# : ...academy:models/... » — et le
     * bloc se dessine sans texture. Le dossier {@code textures/models} garde des copies
     * pour les rendus qui lisent un fichier directement, jamais pour un modele de bloc.
     */
    public static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/block/" + name + ".png");
    }

    /**
     * Le type de rendu des modeles.
     *
     * <p>Il est construit a la main pour une seule raison : les faces du lecteur OBJ sont
     * des TRIANGLES, alors que tous les types d'entite de Minecraft declarent
     * {@code VertexFormat.Mode.QUADS} — quatre sommets par face. Le lecteur en ecrit trois,
     * donc la carte graphique recollait la fin d'un triangle avec le debut du suivant : des
     * faces etirees entre deux morceaux du modele, qui semblent dessinees a l'envers, et
     * d'autres qui disparaissent. Vecu sur les pales de l'eolienne.
     *
     * <p>Le reste reprend ce que fait {@code entityCutoutNoCull} : decoupe, SANS cull,
     * lumiere du bloc et superposition. Le cull est justement laisse de cote : les modeles
     * du mod sont doubles paroi par paroi, mais ils sont aussi ouverts par endroits (73
     * aretes libres dans {@code matrix.obj}), donc cacher le cote qui regarde ailleurs y
     * ferait des trous — on voyait l'herbe au travers de la base. Le scintillement, lui, se
     * regle a la source : {@link ObjMesh#withoutCoveredFaces()} retire les faces
     * recouvertes, et il n'y a plus deux surfaces pour se disputer la profondeur.
     */
    public static RenderType type(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, tex -> createType(tex, false));
    }

    /**
     * Le type de rendu des faces qui passent DERRIERE : le meme, avec un recul de profondeur.
     *
     * <p>C'est le « polygon offset » de vanilla, celui qui sert a plaquer un decor sur une
     * face. Les modeles du mod doublent chaque paroi, et les deux copies se recouvrent en
     * partie (voir {@link ObjMesh#behind}) : au meme plan, la profondeur ne les departage pas,
     * et son infime imprecision change avec l'angle de la camera — c'est le scintillement du
     * matrix. Avec ce recul, la face qui recouvre l'emporte TOUJOURS, et la face recouverte
     * reste visible partout ou elle ne l'est pas.
     */
    public static RenderType behind(ResourceLocation texture) {
        return BEHIND.computeIfAbsent(texture, tex -> createType(tex, true));
    }

    private static RenderType createType(ResourceLocation texture, boolean behind) {
        RenderType.CompositeState.CompositeStateBuilder states = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(
                        GameRenderer::getRendertypeEntityCutoutNoCullShader))
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(new RenderStateShard.TransparencyStateShard("academy_obj",
                        () -> RenderSystem.disableBlend(), () -> { }))
                .setCullState(new RenderStateShard.CullStateShard(false))
                .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                .setOverlayState(new RenderStateShard.OverlayStateShard(true));
        if (behind) {
            states = states.setLayeringState(new RenderStateShard.LayeringStateShard("academy_obj_behind",
                    () -> { RenderSystem.polygonOffset(1.0f, 1.0f); RenderSystem.enablePolygonOffset(); },
                    () -> { RenderSystem.polygonOffset(0.0f, 0.0f); RenderSystem.disablePolygonOffset(); }));
        }

        return RenderType.create(behind ? "academy_obj_behind" : "academy_obj",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES,
                256,
                false,
                true,
                states.createCompositeState(true));
    }

    /**
     * Dessine un morceau de modele : les faces recouvertes d'abord, en recul, puis les autres.
     *
     * <p>L'ordre compte : le recul ne departage que ce qui est deja dessine.
     */
    public static void draw(ObjMesh mesh, String group, ResourceLocation texture, PoseStack pose,
                            MultiBufferSource buffers, int light, int overlay) {
        List<ObjMesh.Face> behind = mesh.behind(group);
        if (!behind.isEmpty()) {
            draw(behind, pose, buffers.getBuffer(behind(texture)), light, overlay);
        }
        draw(mesh.front(group), pose, buffers.getBuffer(type(texture)), light, overlay);
    }

    /** Le modele, lu la premiere fois qu'on le demande. */
    public static ObjMesh get(ResourceLocation model) {
        ObjMesh known = CACHE.get(model);
        if (known != null) return known;

        ObjMesh mesh = ObjMesh.empty();
        try (var in = Minecraft.getInstance().getResourceManager().open(model)) {
            mesh = ObjMesh.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Un modele absent ne fait pas tomber le rendu d'un bloc : la machine se
            // dessine simplement sans cette piece, ce qui se voit tout de suite.
            mesh = ObjMesh.empty();
        }
        CACHE.put(model, mesh);
        return mesh;
    }

    /**
     * Dessine des faces, toutes avec la meme texture.
     *
     * <p>La coordonnee v est retournee : les OBJ mesurent leurs textures depuis le bas,
     * les textures de Minecraft depuis le haut.
     *
     * <p>Et la normale passe par la <b>matrice de normales de la pose</b>, comme celle d'une
     * entite de vanilla. Le shader des entites eclaircit et assombrit ses sommets selon cette
     * normale : laissee dans le repere du modele, elle y est tournee par la camera, et
     * l'eclairage d'une piece se met a suivre les rotations du modele au lieu de la lumiere
     * du monde. Cela ne se voyait sur rien — tous les modeles du port sont fixes — sauf sur
     * les pales de l'eolienne, les seules qui tournent : leur lumiere changeait a chaque tour.
     */
    public static void draw(List<ObjMesh.Face> faces, PoseStack pose, VertexConsumer out,
                            int light, int overlay) {
        Matrix4f matrix = pose.last().pose();
        PoseStack.Pose frame = pose.last();
        for (ObjMesh.Face face : faces) {
            for (ObjMesh.Vertex vertex : face.vertices()) {
                out.vertex(matrix, vertex.x(), vertex.y(), vertex.z())
                        .color(1f, 1f, 1f, 1f)
                        .uv(vertex.u(), 1f - vertex.v())
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(frame.normal(), vertex.nx(), vertex.ny(), vertex.nz())
                        .endVertex();
            }
        }
    }
}
