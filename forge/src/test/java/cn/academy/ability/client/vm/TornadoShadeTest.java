package cn.academy.ability.client.vm;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'ombre des anneaux des tornades, et des ailes de tempete.
 *
 * <p>L'original posait une normale par quad — {@code glNormal3d(x0, y0, 0)} — et <b>laissait
 * faire la lumiere du jeu</b> : la lumiere standard de GL, celle des objets tenus en main, avec
 * ses deux lampes opposees et son ambiance. C'est elle que le port refait maintenant, apres deux
 * tentatives — une lampe posee sur l'oeil, puis le soleil — dont le joueur a dit qu'elles ne
 * ressemblaient pas au vrai.
 *
 * <p>Les normales de ces verifications sont deja dans le repere de l'oeil : c'est la que la
 * matrice du rendu les amene, et c'est la que la lumiere se lit.
 */
class TornadoShadeTest {

    /** L'oeil regarde vers les Z negatifs : une face qui regarde l'oeil a donc +Z pour normale. */
    private static final Vector3f VERS_LOEIL = new Vector3f(0, 0, 1);

    /** Et celle qui lui tourne le dos, -Z — c'est la direction de la premiere lampe. */
    private static final Vector3f DOS_A_LOEIL = new Vector3f(0, 0, -1);

    /** Le cosinus de la premiere lampe pour une normale donnee, tel que le rendu le calcule. */
    private static double cosinus(Vector3f normal) {
        double light = Math.sqrt(TornadoRenderer.LIGHT_X * TornadoRenderer.LIGHT_X
                + TornadoRenderer.LIGHT_Y * TornadoRenderer.LIGHT_Y
                + TornadoRenderer.LIGHT_Z * TornadoRenderer.LIGHT_Z);
        return (normal.x * TornadoRenderer.LIGHT_X + normal.y * TornadoRenderer.LIGHT_Y
                + normal.z * TornadoRenderer.LIGHT_Z) / (normal.length() * light);
    }

    @Test
    void uneFaceQuiRegardeUneLampeEstAuPlusClair() {
        // La premiere lampe est devant l'oeil, en haut a droite : une face qui lui tourne le dos
        // (donc qui regarde l'oeil) la prend de plein fouet. L'ambiance de 0,4 plus les 0,8 de la
        // lampe depassent 1 : la lumiere sature.
        assertEquals(1f, TornadoRenderer.shade(VERS_LOEIL), 1e-6f,
                "la face qui regarde la lampe est au plus clair");
        assertEquals(1f, TornadoRenderer.shade(DOS_A_LOEIL), 1e-6f,
                "et celle qui regarde l'autre lampe aussi");
    }

    @Test
    void lesDeuxLampesSontOpposeesDoncLesDeuxCotesSontAussiClairs() {
        // C'est la difference avec les deux essais precedents, et c'est ce que le joueur voyait
        // dans le vrai mod : la lumiere du jeu a DEUX lampes opposees, donc le cote qui regarde
        // l'oeil n'est pas plus sombre que celui qui lui tourne le dos. Une lampe unique — celle de
        // l'oeil — assombrissait tout un cote, et c'est ce qui rendait les ailes bizarres.
        float vers = TornadoRenderer.shade(VERS_LOEIL);
        float dos = TornadoRenderer.shade(DOS_A_LOEIL);

        assertEquals(vers, dos, 1e-6f, "les deux faces opposees prennent la meme lumiere");
        // La lampe est posee en (0,2 ; 0,2 ; -1) : normalisee, son axe vaut 1 / racine(1,08).
        assertEquals(0.9622504486493761, cosinus(DOS_A_LOEIL), 1e-9,
                "l'une est sous la premiere lampe");
        assertEquals(-0.9622504486493761, cosinus(VERS_LOEIL), 1e-9,
                "et l'autre sous la seconde");
    }

    @Test
    void leCreuxDOmbreEstLAmbianteDuJeu() {
        // Une normale perpendiculaire a la lampe ne prend rien du tout : il ne lui reste que
        // l'ambiance. Cette normale la est perpendiculaire, parce que les deux composantes
        // laterales de la lampe sont egales — (1, -1, 0) annule (0,2, 0,2, -1).
        Vector3f perpendiculaire = new Vector3f(1f, -1f, 0f);

        assertEquals(0.0, cosinus(perpendiculaire), 1e-9, "elle ne prend rien des lampes");
        assertEquals(TornadoRenderer.AMBIENT, TornadoRenderer.shade(perpendiculaire), 1e-6f,
                "et il ne lui reste que l'ambiance du jeu");
    }

    @Test
    void lOmbreGlisseAutourDuRuban() {
        // Les normales d'un ruban tournent autour de son axe : celles qui sont couchées dans le
        // plan de l'oeil prennent l'ombre, celles qui regardent une lampe sont au plus clair. C'est
        // ce glissement qui fait vivre le ruban, et il est le meme des deux cotes.
        float couche = TornadoRenderer.shade(new Vector3f(1, 0, 0));

        assertEquals(0.4f + 0.8f * (float) Math.abs(cosinus(new Vector3f(1, 0, 0))), couche, 1e-6f,
                "une face couchee prend l'ambiance plus ce que la lampe lui donne : " + couche);
        assertEquals(true, couche < TornadoRenderer.shade(VERS_LOEIL),
                "et elle est plus sombre que celles qui regardent une lampe");
        assertEquals(TornadoRenderer.shade(new Vector3f(1, 0, 0)),
                TornadoRenderer.shade(new Vector3f(-1, 0, 0)), 1e-6f,
                "les deux cotes du ruban prennent la meme lumiere");
    }

    @Test
    void laNormaleDuTamponEstTourneeAvecLeQuad() {
        // L'original posait (x0, y0, 0) dans le repere du ruban ; le port la tourne avec lui, et
        // c'est ce qui met la lumiere du bon cote. Un quart de tour autour de Y envoie le rayon
        // vers les Z negatifs.
        Matrix4f quart = new Matrix4f().rotateY((float) Math.toRadians(90));

        Vector3f tournee = TornadoRenderer.normal(quart, 1, 0);
        assertEquals(0f, tournee.x, 1e-6f, "le rayon part vers -Z");
        assertEquals(-1f, tournee.z, 1e-6f, "de la meme longueur");
        assertEquals(0f, tournee.y, 1e-6f, "et sans hauteur");
    }
}
