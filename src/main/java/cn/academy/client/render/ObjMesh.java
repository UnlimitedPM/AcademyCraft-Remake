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

    /** Tolerance sur le plan commun, en blocs : 1e-5 sur un bloc, personne ne le voit. */
    private static final double PLANE_EPSILON = 1.0e-5d;

    /** Tolerance sur l'aire commune, en blocs carres : en dessous, ce n'est qu'un bord. */
    private static final double OVERLAP_EPSILON = 1.0e-5d;

    /**
     * Le meme modele, sans les faces recouvertes par une AUTRE face du meme plan.
     *
     * <p>C'est le nettoyage qui manquait. Les fichiers du mod viennent d'un export qui
     * double chaque paroi : une face vers l'exterieur, et la meme exactement au meme plan
     * vers l'interieur (mesure : 42 faces recouvertes dans {@code matrix.obj}, 18 dans
     * {@code windgen_base}, 8 dans {@code windgen_pillar}, 2 dans {@code windgen_main},
     * aucune dans {@code windgen_fan}). Les deux sont dessinees, donc elles se disputent la
     * profondeur, et la gagnante change avec l'angle de la camera : c'est le scintillement.
     *
     * <p>On garde la DERNIERE de chaque groupe — c'est celle qui gagne deja aujourd'hui a
     * profondeur egale, donc l'aspect ne change pas d'un pixel — et le combat disparait. Les
     * faces qui ne font que se toucher par une arete (les deux triangles d'un meme panneau,
     * ou deux morceaux voisins) ne se recouvrent pas et restent toutes : c'est le
     * recouvrement REEL qui decide, pas le plan partage.
     */
    public ObjMesh withoutCoveredFaces() {
        List<Face> all = new ArrayList<>();
        for (List<Face> faces : groups.values()) all.addAll(faces);
        boolean[] covered = new boolean[all.size()];

        for (int i = 0; i < all.size(); i++) {
            Face face = all.get(i);
            float[] normal = faceNormal(face.vertices());
            for (int j = i + 1; j < all.size() && !covered[i]; j++) {
                if (isCoveredBy(face, normal, all.get(j))) covered[i] = true;
            }
        }

        ObjMesh out = new ObjMesh();
        int index = 0;
        for (Map.Entry<String, List<Face>> entry : groups.entrySet()) {
            List<Face> kept = new ArrayList<>(entry.getValue().size());
            for (Face face : entry.getValue()) {
                if (!covered[index]) kept.add(face);
                index++;
            }
            if (!kept.isEmpty()) out.groups.put(entry.getKey(), kept);
        }
        return out;
    }

    /** La face {@code other} est-elle dans le meme plan que {@code face}, et la recouvre-t-elle ? */
    private static boolean isCoveredBy(Face face, float[] normal, Face other) {
        float[] otherNormal = faceNormal(other.vertices());

        double dot = normal[0] * otherNormal[0] + normal[1] * otherNormal[1] + normal[2] * otherNormal[2];
        if (Math.abs(Math.abs(dot) - 1.0d) > 1.0e-6d) return false;

        double mine = planeOffset(normal, face.vertices().get(0));
        double theirs = planeOffset(otherNormal, other.vertices().get(0));
        if (Math.abs(Math.abs(mine) - Math.abs(theirs)) > PLANE_EPSILON) return false;

        return overlapArea(face, other, normal) > OVERLAP_EPSILON;
    }

    private static double planeOffset(float[] normal, Vertex point) {
        return normal[0] * point.x() + normal[1] * point.y() + normal[2] * point.z();
    }

    /**
     * L'aire commune aux deux faces, dans leur plan.
     *
     * <p>On decoupe le premier triangle par les trois aretes du second (Sutherland-Hodgman),
     * puis on mesure le polygone qui reste. Deux faces qui ne font que se toucher donnent
     * zero.
     */
    private static double overlapArea(Face face, Face other, float[] normal) {
        double[] axisU = new double[3];
        double[] axisV = new double[3];
        planeAxes(normal, axisU, axisV);

        List<double[]> mine = project(face.vertices(), axisU, axisV);
        List<double[]> theirs = project(other.vertices(), axisU, axisV);
        double orientation = side(theirs.get(0), theirs.get(1), theirs.get(2));
        if (Math.abs(orientation) < 1.0e-12d) return 0.0d;

        List<double[]> polygon = mine;
        for (int i = 0; i < 3; i++) {
            double[] from = theirs.get(i);
            double[] to = theirs.get((i + 1) % 3);
            List<double[]> kept = new ArrayList<>(polygon.size() + 3);
            for (int j = 0; j < polygon.size(); j++) {
                double[] point = polygon.get(j);
                double[] next = polygon.get((j + 1) % polygon.size());
                boolean inPoint = inside(from, to, point, orientation);
                boolean inNext = inside(from, to, next, orientation);
                if (inPoint) kept.add(point);
                if (inPoint != inNext) {
                    double a = side(from, to, point);
                    double b = side(from, to, next);
                    double t = a / (a - b);
                    kept.add(new double[] { point[0] + (next[0] - point[0]) * t,
                            point[1] + (next[1] - point[1]) * t });
                }
            }
            polygon = kept;
            if (polygon.size() < 3) return 0.0d;
        }

        double total = 0.0d;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            total += Math.abs((polygon.get(i)[0] - polygon.get(0)[0]) * (polygon.get(i + 1)[1] - polygon.get(0)[1])
                    - (polygon.get(i + 1)[0] - polygon.get(0)[0]) * (polygon.get(i)[1] - polygon.get(0)[1])) / 2.0d;
        }
        return total;
    }

    /** Une base orthonormee du plan, pour y ramener les faces en deux dimensions. */
    private static void planeAxes(float[] normal, double[] axisU, double[] axisV) {
        double[] helper = Math.abs(normal[0]) < 0.9f ? new double[] { 1, 0, 0 } : new double[] { 0, 1, 0 };
        double u0 = normal[1] * helper[2] - normal[2] * helper[1];
        double u1 = normal[2] * helper[0] - normal[0] * helper[2];
        double u2 = normal[0] * helper[1] - normal[1] * helper[0];
        double length = Math.sqrt(u0 * u0 + u1 * u1 + u2 * u2);
        axisU[0] = u0 / length;
        axisU[1] = u1 / length;
        axisU[2] = u2 / length;
        axisV[0] = normal[1] * axisU[2] - normal[2] * axisU[1];
        axisV[1] = normal[2] * axisU[0] - normal[0] * axisU[2];
        axisV[2] = normal[0] * axisU[1] - normal[1] * axisU[0];
    }

    private static List<double[]> project(List<Vertex> vertices, double[] axisU, double[] axisV) {
        List<double[]> out = new ArrayList<>(vertices.size());
        for (Vertex vertex : vertices) {
            out.add(new double[] {
                    vertex.x() * axisU[0] + vertex.y() * axisU[1] + vertex.z() * axisU[2],
                    vertex.x() * axisV[0] + vertex.y() * axisV[1] + vertex.z() * axisV[2] });
        }
        return out;
    }

    private static double side(double[] a, double[] b, double[] point) {
        return (a[0] - point[0]) * (b[1] - point[1]) - (b[0] - point[0]) * (a[1] - point[1]);
    }

    private static boolean inside(double[] a, double[] b, double[] point, double orientation) {
        double value = side(a, b, point);
        return orientation > 0 ? value >= -1.0e-9d : value <= 1.0e-9d;
    }
}
