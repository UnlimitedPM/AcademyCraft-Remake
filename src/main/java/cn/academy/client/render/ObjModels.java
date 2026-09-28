package cn.academy.client.render;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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

    private ObjModels() {}

    /** Le fichier OBJ d'un modele, tel que les modeles de bloc le nomment. */
    public static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "models/" + name + ".obj");
    }

    /**
     * La texture d'un modele.
     *
     * <p>Dans {@code textures/block}, comme les modeles de bloc du port l'ecrivent
     * ({@code "textures": {"base": "academy:block/windgen_main_model"}}). Le dossier
     * {@code textures/models} en contient une seconde copie, plus ancienne et differente :
     * s'y tromper fait disparaitre des faces entieres, l'alpha de l'autre image ne
     * correspondant plus aux coordonnees du fichier OBJ.
     */
    public static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, "textures/block/" + name + ".png");
    }

    /** Le type de rendu des modeles : decoupe, et sans cull — les OBJ du mod sont fins. */
    public static RenderType type(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
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
