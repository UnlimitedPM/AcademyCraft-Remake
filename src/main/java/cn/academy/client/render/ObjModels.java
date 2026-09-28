package cn.academy.client.render;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
     * <p>Le reste reprend ce que fait {@code entityCutoutNoCull} : decoupe, lumiere du bloc
     * et superposition. Avec une difference, le CULL, garde ici (l'original le gardait
     * aussi, et un modele de bloc dessine par Forge aussi) : les modeles du mod sont
     * doubles paroi par paroi — une face vers l'exterieur, et la meme exactement au meme
     * plan vers l'interieur — et sans cull les deux se disputent la profondeur, ce qui
     * scintille des qu'on bouge la camera (mesure : 89 paires coplanaires dans
     * {@code matrix.obj}, 64 a normales opposees).
     */
    public static RenderType type(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, ObjModels::createType);
    }

    private static RenderType createType(ResourceLocation texture) {
        return RenderType.create("academy_obj",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeEntityCutoutNoCullShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(new RenderStateShard.TransparencyStateShard("academy_obj",
                                () -> RenderSystem.disableBlend(), () -> { }))
                        .setCullState(new RenderStateShard.CullStateShard(true))
                        .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                        .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                        .createCompositeState(true));
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
     */
    public static void draw(List<ObjMesh.Face> faces, PoseStack pose, VertexConsumer out,
                            int light, int overlay) {
        Matrix4f matrix = pose.last().pose();
        for (ObjMesh.Face face : faces) {
            for (ObjMesh.Vertex vertex : face.vertices()) {
                out.vertex(matrix, vertex.x(), vertex.y(), vertex.z())
                        .color(1f, 1f, 1f, 1f)
                        .uv(vertex.u(), 1f - vertex.v())
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(vertex.nx(), vertex.ny(), vertex.nz())
                        .endVertex();
            }
        }
    }
}
