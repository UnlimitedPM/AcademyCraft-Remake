package cn.academy.util;

/**
 * Le bruit de Perlin, en Java, tel quel.
 *
 * <p>C'est la reference de Ken Perlin, recopiee de l'original du mod (qui la portait deja telle
 * quelle) : les tornades de vecmanip s'en servent pour gondoler leurs anneaux, et sa
 * <b>determinisme</b> compte — deux clients qui tirent les memes anneaux doivent voir la meme
 * forme.
 *
 * <p>Elle est ici parce que le reste du port n'a aucun bruit, et parce qu'elle est <b>pure</b> :
 * elle ne connait ni Minecraft ni le hasard, donc elle se verifie en JUnit.
 */
public final class ImprovedNoise {

    private ImprovedNoise() {
    }

    /** Le bruit en trois dimensions : entre -1 et 1, et periodique de 256. */
    public static double noise(double x, double y, double z) {
        int xx = (int) Math.floor(x) & 255,
                yy = (int) Math.floor(y) & 255,
                zz = (int) Math.floor(z) & 255;
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        double u = fade(x), v = fade(y), w = fade(z);
        int a = P[xx] + yy, aa = P[a] + zz, ab = P[a + 1] + zz,
                b = P[xx + 1] + yy, ba = P[b] + zz, bb = P[b + 1] + zz;

        return lerp(w, lerp(v, lerp(u, grad(P[aa], x, y, z),
                        grad(P[ba], x - 1, y, z)),
                        lerp(u, grad(P[ab], x, y - 1, z),
                                grad(P[bb], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(P[aa + 1], x, y, z - 1),
                        grad(P[ba + 1], x - 1, y, z - 1)),
                        lerp(u, grad(P[ab + 1], x, y - 1, z - 1),
                                grad(P[bb + 1], x - 1, y - 1, z - 1))));
    }

    /** Le bruit a deux dimensions, qui est le meme dans le plan z = 0. */
    public static double noise(double x, double y) {
        return noise(x, y, 0);
    }

    /** Et a une dimension. */
    public static double noise(double x) {
        return noise(x, 0, 0);
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y,
                v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static final int[] P = new int[512];
    private static final int[] PERMUTATION = {
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30,
            69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94,
            252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136,
            171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229,
            122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63,
            161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188,
            159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38,
            147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42,
            223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172,
            9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246,
            97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249,
            14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45,
            127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66,
            215, 61, 156, 180
    };

    static {
        for (int i = 0; i < 256; i++) {
            P[256 + i] = P[i] = PERMUTATION[i];
        }
    }
}
