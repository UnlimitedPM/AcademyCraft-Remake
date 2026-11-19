package cn.academy.ability.client.vm;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le corps de plasma du canon.
 *
 * <p>Ce qui se relit sans Minecraft : les deux familles de boules et leurs bornes, le cercle que
 * chacune decrit, les trois temps de son opacite et les deux bouts de sa couleur. Une boule qui
 * tourne carre, qui nait a la mauvaise taille ou qui prend le rose au coeur ne se verraient qu'a
 * l'ecran — et c'est justement ce qu'aucune des quatre portes ne regarde.
 */
class PlasmaBodyVisualsTest {

    @Test
    void lEssaimPorteLesDeuxFamillesDeLOriginal() {
        List<PlasmaBodyVisuals.Ball> balls =
                PlasmaBodyVisuals.roll(RandomSource.create(20261006L));

        // Trois grosses, puis trois petites : c'est la description du joueur, et non plus celle de
        // l'original, qui en tirait quatre et quatre ou cinq. « On voyait trois grosses boules au
        // centre [...] et seulement trois petites boules autour », puis « visuellement on devrait
        // avoir moins de boules ».
        assertEquals(PlasmaBodyVisuals.BIG_COUNT + PlasmaBodyVisuals.SMALL_COUNT_MIN, balls.size(),
                "six boules, trois et trois");

        for (int i = 0; i < PlasmaBodyVisuals.BIG_COUNT; i++) {
            var ball = balls.get(i);
            assertTrue(ball.size() >= PlasmaBodyVisuals.BIG_SIZE_MIN
                            && ball.size() <= PlasmaBodyVisuals.BIG_SIZE_MAX,
                    "une grosse mesure de 1,3 a 1,9 : " + ball.size());
            assertTrue(within(ball.cx()) && within(ball.cy()) && within(ball.cz()),
                    "et son centre tient dans le cube du corps");
            // Et elle ne bouge presque pas : c'est la demande du joueur, « elles ne doivent pas
            // bouger ». Il leur reste huit fois moins de balancement qu'aux petites de l'original.
            assertTrue(ball.horizontal().amplitude() <= PlasmaBodyVisuals.AMPLITUDE_MAX
                            * PlasmaBodyVisuals.BIG_MOTION + 1e-9
                            && ball.vertical().amplitude() >= PlasmaBodyVisuals.AMPLITUDE_MIN
                            * PlasmaBodyVisuals.BIG_MOTION - 1e-9,
                    "et elle reste presque immobile : " + ball.horizontal().amplitude());
            assertTrue(ball.horizontal().amplitude() < 0.3,
                    "moins d'un tiers de bloc de derive : " + ball.horizontal().amplitude());
        }
        for (int i = PlasmaBodyVisuals.BIG_COUNT; i < balls.size(); i++) {
            var ball = balls.get(i);
            assertTrue(ball.size() >= PlasmaBodyVisuals.SMALL_SIZE_MIN
                            && ball.size() <= PlasmaBodyVisuals.SMALL_SIZE_MAX,
                    "une petite mesure de 0,1 a 0,3 : " + ball.size());
            assertTrue(Math.abs(ball.cx()) <= PlasmaBodyVisuals.SMALL_OFFSET
                            && Math.abs(ball.cy()) <= PlasmaBodyVisuals.SMALL_OFFSET
                            && Math.abs(ball.cz()) <= PlasmaBodyVisuals.SMALL_OFFSET,
                    "et son centre tient dans le grand cube");
            // Le balancement des petites vaut deux fois et demie le leur : c'est ce qui les fait
            // tourner LOIN du corps malgre leur taille minuscule. Elles, elles bougent pour de bon.
            assertTrue(ball.horizontal().amplitude() >= PlasmaBodyVisuals.AMPLITUDE_MIN * 2.5
                            && ball.horizontal().amplitude() <= PlasmaBodyVisuals.AMPLITUDE_MAX * 2.5,
                    "elles balancent de 3,5 a 5 blocs : " + ball.horizontal().amplitude());
        }
    }

    @Test
    void chaqueBouleDecritUnCercleEtNeDepassePasSonAutreAmplitude() {
        for (var ball : PlasmaBodyVisuals.roll(RandomSource.create(7L))) {
            double horizontal = ball.horizontal().amplitude();
            double vertical = ball.vertical().amplitude();

            for (double seconds = 0; seconds < 6; seconds += 0.13) {
                Vec3 at = PlasmaBodyVisuals.offset(ball, seconds);
                double radius = Math.hypot(at.x - ball.cx(), at.z - ball.cz());
                assertEquals(horizontal, radius, 1e-9,
                        "x et z tournent sur un meme cercle : c'est la meme phase");
                assertTrue(Math.abs(at.y - ball.cy()) <= vertical + 1e-9,
                        "et la hauteur tient dans sa propre amplitude");
            }
        }
    }

    @Test
    void lOpaciteMonteEnTroisDixiemesParSecondeEtRetombeEnUne() {
        // En secondes, comme l'original : rien a la naissance, puis trois dixiemes par seconde.
        assertEquals(0f, PlasmaBodyVisuals.alpha(0), 1e-6f, "transparent a la naissance");
        assertEquals(0.3f, PlasmaBodyVisuals.alpha(1), 1e-6f, "un dixieme de plus par dixieme");
        assertEquals(0.9f, PlasmaBodyVisuals.alpha(3), 1e-6f, "et encore a trois secondes");
        assertEquals(1f, PlasmaBodyVisuals.alpha(4), 1e-6f, "pleine a quatre");
        assertEquals(1f, PlasmaBodyVisuals.alpha(90), 1e-6f, "et elle ne monte plus");

        // La chute, elle, prend une seconde, d'ou qu'elle parte.
        assertEquals(1f, PlasmaBodyVisuals.fading(1f, 0), 1e-6f, "elle part de sa valeur");
        assertEquals(0.5f, PlasmaBodyVisuals.fading(1f, 0.5), 1e-6f, "a moitie en une demi-seconde");
        assertEquals(0f, PlasmaBodyVisuals.fading(1f, 1), 1e-6f, "et il n'en reste rien");
        assertEquals(0f, PlasmaBodyVisuals.fading(0.4f, 3), 1e-6f, "sans jamais passer sous zero");
    }

    @Test
    void uneBouleGrossitEnNaitreEtRetrecitEnMourir() {
        // La densite du nuanceur valait alpha * taille / distance carre : une boule se voit donc
        // jusqu'a racine de alpha. Rien a la naissance, pleine taille une fois le corps noue — et
        // c'est ce qui manquait : les boules apparaissaient a taille pleine, ce qui se lisait comme
        // un allumage et non comme de la matiere qui se noue.
        assertEquals(0.0, PlasmaBodyVisuals.growth(0f), 1e-9, "rien tant qu'il n'y a rien");
        assertEquals(0.5, PlasmaBodyVisuals.growth(0.25f), 1e-9, "un quart d'opacite, moitie de rayon");
        assertEquals(1.0, PlasmaBodyVisuals.growth(1f), 1e-9, "et pleine une fois nouee");
        assertEquals(1.0, PlasmaBodyVisuals.growth(4f), 1e-9, "jamais plus que sa taille");
        assertEquals(0.0, PlasmaBodyVisuals.growth(-2f), 1e-9, "et jamais negative");
    }

    @Test
    void leHaloEstAssezLargePourQueLesBoulesSeFondent() {
        // Le rayon suit la RACINE de la taille, pas la taille : un facteur direct rendrait les
        // petites boules invisibles (deux dixiemes de bloc) et les grosses enormes.
        double petite = PlasmaBodyVisuals.haloRadius(PlasmaBodyVisuals.SMALL_SIZE_MIN, 1f);
        double grosse = PlasmaBodyVisuals.haloRadius(PlasmaBodyVisuals.BIG_SIZE_MAX, 1f);

        assertEquals(Math.sqrt(PlasmaBodyVisuals.BIG_SIZE_MAX / PlasmaBodyVisuals.VISIBILITY),
                grosse, 1e-9, "une grosse se voit a plus de trois blocs, une fois nouee");
        assertTrue(grosse > 3.0, "et c'est bien plus de trois : " + grosse);
        assertTrue(grosse / petite > 3.0, "quand la petite en fait moins d'un : " + petite);

        // A la naissance il n'y a rien : le rayon suit la croissance du corps.
        assertEquals(0.0, PlasmaBodyVisuals.haloRadius(1.0, 0f), 1e-9, "rien a la naissance");

        // ET C'EST LE POINT : a mi-charge, deux grosses boules ecartees de trois blocs et demi se
        // recouvrent encore. Avec un halo de moitie — ce que le port avait — elles laissaient un
        // trou entre elles, et le joueur y a lu « quinze petites boules » au lieu d'un corps.
        double mid = PlasmaBodyVisuals.haloRadius(1.2, 0.45f);
        assertTrue(2 * mid > 3.5, "elles se fondent a mi-charge aussi : " + (2 * mid));
    }

    @Test
    void laCouvertureNeLaissePasLeCorpsTerne() {
        // Le facteur du nuanceur, mot pour mot : alpha * (0,5 + 0,5 alpha). Le port multipliait par
        // la seule opacite, et le joueur a vu le resultat — « tellement transparent que c'est a
        // peine si j'arrive a le voir ».
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA * 0.5f, PlasmaBodyVisuals.coverage(0f), 1e-6f,
                "a la naissance la moitie du facteur — mais le rayon, lui, est nul");
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA * 0.75f, PlasmaBodyVisuals.coverage(0.5f), 1e-6f,
                "a mi-charge, deja les trois quarts");
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA, PlasmaBodyVisuals.coverage(1f), 1e-6f,
                "et le plein une fois le corps noue");

        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA, PlasmaBodyVisuals.coverage(4f), 1e-6f, "bornee");
        assertEquals(PlasmaBodyVisuals.DRAW_ALPHA * 0.5f, PlasmaBodyVisuals.coverage(-1f), 1e-6f,
                "et jamais sous la moitie");

        // Et le plein est haut, parce que le fond le plus dur est le CIEL : le joueur y trouvait les
        // boules trop transparentes, « dans le ciel on a du mal a les voir ». Un rose a six
        // dixiemes sur un ciel clair se lave.
        assertTrue(PlasmaBodyVisuals.coverage(1f) >= 0.8f,
                "une boule nouee couvre au moins huit dixiemes : " + PlasmaBodyVisuals.coverage(1f));
    }

    @Test
    void chaqueBouleEstRoseAvecUnCoeurBleu() {
        // Les deux bouts du nuanceur, tels quels : rose (0,98 / 0,51 / 0,92) et bleu (0,43 / 0,74 / 1).
        float[] rose = PlasmaBodyVisuals.ballColor();
        assertEquals(0.98f, rose[0], 1e-6f, "une boule prend le rose, en rouge");
        assertEquals(0.51f, rose[1], 1e-6f, "en vert");
        assertEquals(0.92f, rose[2], 1e-6f, "en bleu");

        float[] bleu = PlasmaBodyVisuals.coreColor();
        assertEquals(0.43f, bleu[0], 1e-6f, "et son coeur prend le bleu, en rouge");
        assertEquals(0.74f, bleu[1], 1e-6f, "en vert");
        assertEquals(1.0f, bleu[2], 1e-6f, "en bleu");

        // Le coeur tient dans le halo, et DANS LE MEME RAPPORT pour toutes les boules : c'est le
        // rapport des deux seuils de densite, donc il ne depend ni de la taille ni de l'instant. Le
        // joueur l'a reclame en une phrase : « leur centre est rose, donc pas comme le vrai ».
        double expected = Math.sqrt(PlasmaBodyVisuals.VISIBILITY / PlasmaBodyVisuals.CORE_DENSITY);
        for (double size : new double[] { 0.1, 0.3, 1.0, 1.5 }) {
            double halo = PlasmaBodyVisuals.haloRadius(size, 0.7f);
            double core = PlasmaBodyVisuals.coreRadius(size, 0.7f);
            assertTrue(core < halo, "le coeur tient dans le halo : " + size);
            assertEquals(expected, core / halo, 1e-9, "et dans le meme rapport : " + size);
        }
    }

    @Test
    void leCarreCompteLeBordMourantDeLImage() {
        // L'image s'eteint avant son bord — son profil est un (1 - r) au carre, qui passe sous un
        // dixieme d'opacite a 68 % du carre. Sans ce rattrapage, tout ce que le port dessine est
        // d'un bon tiers plus petit que ce qu'il croit dessiner.
        assertEquals(1.0 / PlasmaBodyVisuals.SPRITE_REACH, PlasmaBodyVisuals.quadRadius(1.0), 1e-9);
        assertTrue(PlasmaBodyVisuals.quadRadius(1.0) > 1.4, "le carre depasse le rayon vu");

        // Et le corps a son propre coeur bleu, la ou les densites s'additionnent.
        assertEquals(PlasmaBodyVisuals.BODY_CORE_RADIUS, PlasmaBodyVisuals.bodyCoreRadius(1f), 1e-9,
                "sa taille pleine une fois le corps noue");
        assertEquals(0.0, PlasmaBodyVisuals.bodyCoreRadius(0f), 1e-9, "et rien a la naissance");
        assertTrue(PlasmaBodyVisuals.bodyCoreRadius(0.8f) > PlasmaBodyVisuals.bodyCoreRadius(0.2f),
                "il grandit a mesure que la matiere se noue");
    }

    /**
     * L'essaim se resserre a l'echelle de la colonne, et ses rayons avec lui.
     *
     * <p>Dans une grotte, la colonne du canon se raccourcit (voir {@code TornadoVisuals.cannonScale})
     * et les boules doivent suivre, sans quoi une masse large comme une maison pend sous une colonne
     * de trois blocs. Le joueur : « il faudrait aussi un peu reduire la taille des boules pour que
     * ce soit dans le meme ordre que la grande tornade ».
     */
    @Test
    void lesBoulesSeResserrantAvecLaColonne() {
        List<PlasmaBodyVisuals.Ball> essaim = PlasmaBodyVisuals.roll(RandomSource.create(4));
        PlasmaBodyVisuals.Ball grosse = essaim.get(0);

        // A l'echelle un, rien ne bouge : c'est le corps de l'original, sous le ciel.
        PlasmaBodyVisuals.Ball entiere = PlasmaBodyVisuals.scaled(grosse, 1.0);
        assertEquals(grosse.cx(), entiere.cx(), 1e-9, "a ciel ouvert, aucun centre ne bouge");
        assertEquals(grosse.horizontal().amplitude(), entiere.horizontal().amplitude(), 1e-9,
                "ni aucun balancement");

        // Au quart, les ecarts tombent au quart — et la matiere, elle, ne change pas de nombre.
        PlasmaBodyVisuals.Ball quart = PlasmaBodyVisuals.scaled(grosse, 0.25);
        assertEquals(grosse.cx() * 0.25, quart.cx(), 1e-9, "le centre suit l'echelle");
        assertEquals(grosse.cy() * 0.25, quart.cy(), 1e-9, "sur les trois axes");
        assertEquals(grosse.cz() * 0.25, quart.cz(), 1e-9);
        assertEquals(grosse.horizontal().amplitude() * 0.25, quart.horizontal().amplitude(), 1e-9,
                "et le balancement aussi");
        assertEquals(grosse.size(), quart.size(), 1e-9, "mais la boule reste la meme matiere");

        // Le rayon dessine, lui, suit l'echelle du dessin — et pas la racine de la taille : sinon
        // une boule trois fois plus petite se verrait encore a moitie.
        assertEquals(PlasmaBodyVisuals.quadRadius(2.0) * 0.25,
                PlasmaBodyVisuals.quadRadius(2.0, 0.25), 1e-9, "le carre suit la meme echelle");
        assertTrue(PlasmaBodyVisuals.offset(quart, 3.0).length()
                        < PlasmaBodyVisuals.offset(grosse, 3.0).length(),
                "et la boule tourne donc plus pres du centre");

        // L'essaim entier, enfin, et dans l'ordre.
        List<PlasmaBodyVisuals.Ball> serre = PlasmaBodyVisuals.scaled(essaim, 0.25);
        assertEquals(essaim.size(), serre.size(), "personne ne se perd en chemin");
        for (int i = 0; i < essaim.size(); i++) {
            assertEquals(essaim.get(i).cx() * 0.25, serre.get(i).cx(), 1e-9, "boule " + i);
        }
    }

    /** Vrai si un centre tient dans le cube de plus ou moins 1,5 bloc des grosses. */
    private static boolean within(double value) {
        return Math.abs(value) <= PlasmaBodyVisuals.BIG_OFFSET;
    }
}
