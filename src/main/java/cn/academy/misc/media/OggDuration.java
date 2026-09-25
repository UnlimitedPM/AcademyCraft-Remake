package cn.academy.misc.media;

/**
 * La duree d'un fichier Ogg Vorbis, lue dans ses en-tetes.
 *
 * <p>L'original decodait le morceau pour en connaitre la longueur ({@code VorbisFile.time_total},
 * via JOrbis). Le port n'embarque pas de decodeur : il lit ce que le format ecrit lui-meme.
 *
 * <p>Un Ogg est une suite de <b>pages</b>, chacune commencant par {@code OggS} et portant un
 * <b>granule</b> : le nombre d'echantillons produits depuis le debut du flux. La derniere page
 * donne donc la duree, une fois divisee par le taux d'echantillonnage — que donne le paquet
 * d'identification de Vorbis, dans la premiere page.
 *
 * <p>Aucun type Minecraft ici, et pas de fichier non plus : un tableau d'octets, donc relisible
 * en JUnit sur les morceaux livres.
 */
public final class OggDuration {

    /** Le debut d'une page Ogg. */
    private static final byte[] CAPTURE = {0x4F, 0x67, 0x67, 0x53};

    /** Le debut du paquet d'identification de Vorbis : 0x01 puis "vorbis". */
    private static final byte[] VORBIS_ID = {0x01, 0x76, 0x6F, 0x72, 0x62, 0x69, 0x73};

    private OggDuration() {}

    /**
     * La duree du morceau, en secondes.
     *
     * <p>Rend <b>0</b> quand le fichier ne s'y prete pas — ce n'est pas un Ogg, l'identification
     * manque, le taux est nul, ou aucune page ne porte de granule. Un morceau sans duree
     * s'affiche sans barre plutot que de faire tomber le HUD.
     */
    public static float seconds(byte[] data) {
        if (data == null) return 0f;
        int rate = sampleRate(data);
        long granule = lastGranule(data);
        if (rate <= 0 || granule <= 0) return 0f;
        return granule / (float) rate;
    }

    /** Le taux d'echantillonnage, ou 0 s'il est introuvable. */
    public static int sampleRate(byte[] data) {
        int start = indexOf(data, VORBIS_ID, 0);
        if (start < 0) return 0;
        // 0x01 "vorbis", puis version (4 octets), canaux (1), taux (4).
        int offset = start + VORBIS_ID.length + 4 + 1;
        if (offset + 4 > data.length) return 0;
        return readInt(data, offset);
    }

    /**
     * Le granule de la derniere page, ou 0.
     *
     * <p>On remonte depuis la fin : la derniere page d'un fichier ecrit normalement porte le
     * dernier echantillon, mais un flux coupe peut finir sur une page sans granule (le marqueur
     * valant alors -1), et c'est la precedente qui a raison.
     */
    public static long lastGranule(byte[] data) {
        int page = data.length;
        while (true) {
            page = lastIndexOf(data, CAPTURE, page - CAPTURE.length);
            if (page < 0) return 0L;
            // Le granule est a l'offset 6 de l'en-tete de page, sur 8 octets.
            if (page + 14 <= data.length) {
                long granule = readLong(data, page + 6);
                if (granule > 0) return granule;
            }
        }
    }

    private static int indexOf(byte[] data, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i + needle.length <= data.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] data, byte[] needle, int from) {
        for (int i = Math.min(from, data.length - needle.length); i >= 0; i--) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    /** Un entier 32 bits little-endian, comme tout ce que l'Ogg ecrit. */
    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private static long readLong(byte[] data, int offset) {
        long value = 0L;
        for (int i = 7; i >= 0; i--) {
            value = (value << 8) | (data[offset + i] & 0xFFL);
        }
        return value;
    }
}
