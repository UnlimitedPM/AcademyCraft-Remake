package cn.academy.client.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un modele OBJ lu en memoire.
 *
 * <p>Le port avait besoin de dessiner des morceaux de modele — les pales de l'eolienne,
 * les plaques du matrix — a des endroits qui bougent. Un modele de bloc ne sait pas faire
 * ca : il est fige la ou le blockstate le pose, et rien ne peut le faire tourner. Les
 * fichiers OBJ du mod sont donc lus ici, et le rendu les redessine image par image, comme
 * le faisait l'original.
 *
 * <p>Ce que le lecteur comprend : les sommets, les coordonnees de texture, les normales,
 * les groupes ({@code g}), et les faces — triangles ou quads, indices positifs ou negatifs
 * comme le veut le format. Ce qu'il ignore : les materiaux et le lissage, qui ne servent
 * pas ici puisque chaque morceau n'a qu'une texture.
 *
 * <p>Aucune classe de Minecraft : ce lecteur se relit en JUnit, ou l'on verifie qu'un quad
 * devient bien deux triangles et qu'une normale manquante se calcule.
 */
public final class ObjMesh {

    /** Un sommet, avec tout ce qu'il faut pour le dessiner. */
    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}

    /** Une face, deja triangulaire. */
    public record Face(List<Vertex> vertices) {}

    private final Map<String, List<Face>> groups = new LinkedHashMap<>();

    private ObjMesh() {}

    /** Un modele vide, pour un fichier absent. */
    public static ObjMesh empty() {
        return new ObjMesh();
    }

    /** Les faces d'un groupe, ou une liste vide si le groupe n'existe pas. */
    public List<Face> group(String name) {
        return groups.getOrDefault(name, List.of());
    }

    /** Le nom du groupe par defaut : les fichiers exportes sans {@code g} en ont un. */
    public List<Face> all() {
        List<Face> out = new ArrayList<>();
        for (List<Face> faces : groups.values()) out.addAll(faces);
        return out;
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    public static ObjMesh parse(String text) {
        ObjMesh mesh = new ObjMesh();
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        String group = "";

        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "v" -> positions.add(numbers(parts, 3));
                case "vt" -> uvs.add(numbers(parts, 2));
                case "vn" -> normals.add(numbers(parts, 3));
                case "g", "o" -> group = parts.length > 1 ? parts[1] : "";
                case "f" -> mesh.addFace(parts, positions, uvs, normals, group);
                default -> { /* mtllib, usemtl, s : rien a faire */ }
            }
        }
        return mesh;
    }

    private void addFace(String[] parts, List<float[]> positions, List<float[]> uvs,
                         List<float[]> normals, String group) {
        List<Vertex> vertices = new ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) {
            Vertex vertex = vertex(parts[i], positions, uvs, normals);
            if (vertex != null) vertices.add(vertex);
        }
        if (vertices.size() < 3) return;

        // Les sommets sans normale prennent celle de la face : un modele exporte sans
        // normales se dessinerait tout noir.
        float[] normal = faceNormal(vertices);
        List<Vertex> filled = new ArrayList<>(vertices.size());
        for (Vertex vertex : vertices) {
            filled.add(withNormal(vertex, normal[0], normal[1], normal[2]));
        }

        // Un quad n'est pas dessinable tel quel : deux triangles, dans l'ordre du fichier.
        List<Face> faces = groups.computeIfAbsent(group, name -> new ArrayList<>());
        for (int i = 1; i + 1 < filled.size(); i++) {
            faces.add(new Face(List.of(filled.get(0), filled.get(i), filled.get(i + 1))));
        }
    }

    /** La normale d'une face, prise sur ses trois premiers sommets. */
    private static float[] faceNormal(List<Vertex> vertices) {
        Vertex a = vertices.get(0);
        Vertex b = vertices.get(1);
        Vertex c = vertices.get(2);
        float ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
        float vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        return length < 1e-6f ? new float[] { 0f, 1f, 0f }
                : new float[] { nx / length, ny / length, nz / length };
    }

    /** Un sommet d'une face : {@code v}, {@code v/vt}, {@code v//vn} ou {@code v/vt/vn}. */
    private static Vertex vertex(String token, List<float[]> positions, List<float[]> uvs,
                                 List<float[]> normals) {
        String[] parts = token.split("/", -1);
        float[] position = at(positions, parts[0]);
        if (position == null) return null;

        float[] uv = parts.length > 1 ? at(uvs, parts[1]) : null;
        float[] normal = parts.length > 2 ? at(normals, parts[2]) : null;
        return new Vertex(position[0], position[1], position[2],
                uv == null ? 0f : uv[0], uv == null ? 0f : uv[1],
                normal == null ? 0f : normal[0], normal == null ? 0f : normal[1],
                normal == null ? 0f : normal[2]);
    }

    /**
     * Le sommet vise par un indice du fichier, ou {@code null} s'il est vide ou hors du
     * fichier. Les indices sont numerotes a partir de un, et un indice negatif compte
     * depuis la fin — c'est le format, et les exports de MilkShape s'en servent.
     */
    private static float[] at(List<float[]> list, String token) {
        if (token == null || token.isEmpty()) return null;
        int index;
        try {
            index = Integer.parseInt(token.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (index < 0) index = list.size() + index;
        else index = index - 1;
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    private static float[] numbers(String[] parts, int count) {
        float[] out = new float[count];
        for (int i = 0; i < count; i++) {
            out[i] = parts.length > i + 1 ? number(parts[i + 1]) : 0f;
        }
        return out;
    }

    private static float number(String token) {
        try {
            return Float.parseFloat(token);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /**
     * Complete une normale manquante.
     *
     * <p>Les fichiers du mod en ont tous, mais un modele exporte sans normales se
     * dessinerait tout noir : mieux vaut une normale plate deduite de la face.
     */
    private static Vertex withNormal(Vertex vertex, float nx, float ny, float nz) {
        return vertex.nx() == 0f && vertex.ny() == 0f && vertex.nz() == 0f
                ? new Vertex(vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v(), nx, ny, nz)
                : vertex;
    }
}
