package cn.academy.ability.client.tp;

import cn.academy.ability.teleporter.TeleporterCategory;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La marque de teleportation : son animation, ses etincelles, et quand elle s'allume.
 *
 * <p>Ce sont les trois choses que l'original tenait dans {@code EntityTPMarking} et dans le
 * {@code TPParticleFactory} de ses particules. Le fantome lui-meme — un modele de joueur — ne se
 * relit pas en test ; ce qui se relit, c'est son <b>animation</b> (sept images, une toutes les deux
 * ticks et demie) et la vie de ses etincelles.
 */
class TpMarkTest {

    @Test
    @DisplayName("la marque defile ses sept images, un tour en dix-sept ticks et demi")
    void laMarqueDefileSesSeptImages() {
        // L'original : `(int) ((ticksExisted / 2.5) % tex.length)`. Chaque image tient donc deux
        // ticks et demi — la premiere de 0 a 2, la deuxieme de 3 a 5 — et le tour recommence a 18.
        assertEquals(7, TpMarkRenderer.FRAMES);
        assertEquals(2.5, TpMarkRenderer.FRAME_TICKS, 1e-9);

        assertEquals(0, TpMarkRenderer.frame(0));
        assertEquals(0, TpMarkRenderer.frame(2), "l'image tient deux ticks et demi, pas deux");
        assertEquals(1, TpMarkRenderer.frame(3));
        assertEquals(2, TpMarkRenderer.frame(5));
        assertEquals(3, TpMarkRenderer.frame(8));
        assertEquals(6, TpMarkRenderer.frame(15), "la derniere image");
        assertEquals(6, TpMarkRenderer.frame(17));
        assertEquals(0, TpMarkRenderer.frame(18), "et le tour recommence");
        assertEquals(1, TpMarkRenderer.frame(21));
    }

    @Test
    @DisplayName("les etincelles de la teleportation sont celles de l'original")
    void lesEtincellesSontCellesDeLoriginal() {
        // `TPParticleFactory` : deux dixiemes de bloc, une opacite de 153 a 204, et une vie de
        // `fadeAfter(20, 20)` — vingt ticks pleins, puis vingt d'effacement.
        assertEquals(20, TpParticles.LIFE_TICKS);
        assertEquals(20, TpParticles.FADE_TICKS);
        assertEquals(5, TpParticles.FADE_IN_TICKS);
        assertEquals(153, TpParticles.ALPHA_MIN);
        assertEquals(204, TpParticles.ALPHA_MAX);
        assertEquals(0.1, TpParticles.SIZE_MIN, 1e-9);
        assertEquals(0.2, TpParticles.SIZE_MAX, 1e-9);
        assertEquals("textures/effects/tp_particle.png", TpParticles.TEXTURE.getPath());

        // Leur vie, en trois temps : elles montent en cinq ticks, tiennent vingt, s'effacent en
        // vingt autres — la plus longue particule du port, deux secondes. L'opacite de depart est
        // tiree entre 153 et 204 sur 255, donc les courbes se lisent par rapport a elle.
        long now = 1_000L;
        TpParticles.clear();
        TpParticles.spawn(Vec3.ZERO, Vec3.ZERO, now, new Random(1));
        TpParticles.Spark spark = TpParticles.live().get(0);

        float full = spark.alpha(now + 1_000);
        assertTrue(full >= 153 / 255f && full <= 204 / 255f,
                "son opacite est tiree entre 153 et 204 sur 255");

        assertEquals(0f, spark.alpha(now), 1e-6, "elle nait transparente");
        assertEquals(full / 5f, spark.alpha(now + 50), 1e-4, "un cinquieme au premier tick");
        assertEquals(full, spark.alpha(now + 250), 1e-6, "pleine a cinq ticks");
        assertEquals(full, spark.alpha(now + 1_000), 1e-6, "et elle tient ses vingt ticks");
        assertEquals(full / 2f, spark.alpha(now + 1_500), 1e-4, "a moitie a trente ticks");
        assertEquals(0f, spark.alpha(now + 2_000), 1e-6, "eteinte a quarante");

        assertTrue(spark.dead(now + 2_000));
        TpParticles.tick(now + 1_950);
        assertEquals(1, TpParticles.live().size(), "elle vit jusqu'au bout");
        TpParticles.tick(now + 2_000);
        assertTrue(TpParticles.live().isEmpty(), "et s'en va apres");
    }

    @Test
    @DisplayName("la marque semme ses etincelles deux ticks sur cinq")
    void laMarqueSemmeSesEtincelles() {
        // L'original : `rand.nextDouble() < 0.4`, une boite d'un bloc de large, et une hauteur de
        // `ranged(0,2, 1,6) - 1,6` — de moins un bloc quatre a zero, sous les pieds de la marque.
        assertEquals(0.4, TeleportMark.SPARK_CHANCE, 1e-9);
        assertEquals(1.0, TeleportMark.SPARK_RADIUS, 1e-9);
        assertEquals(-1.4, TeleportMark.SPARK_LOW, 1e-9);
        assertEquals(0.0, TeleportMark.SPARK_HIGH, 1e-9);
        assertEquals(0.03, TeleportMark.SPARK_SPEED, 1e-9);
        assertEquals(0.05, TeleportMark.SPARK_RISE, 1e-9);
    }

    @Test
    @DisplayName("le scintillement ne montre rien tant qu'aucune direction n'est visee")
    void leScintillementNeMontreRienSansDirection() {
        // Chez l'original, c'etait la touche de direction qui allumait l'anneau, et son
        // relachement qui faisait partir le saut : sans touche, il n'y a pas de destination, donc
        // pas de fantome. Le joueur, lui, n'est jamais touche par ce cas — c'est ce que verifie ce
        // test en passant un joueur nul.
        assertNull(TeleportMark.seat(null, TeleporterCategory.FLASHING, 0, 0),
                "la touche de direction n'est pas enfoncee");

        // Et une competence qui n'a pas de marque n'en allume aucune, quelle que soit la visee :
        // c'est ce qui eteint le fantome quand le joueur change de touche sans relacher.
        //
        // Le saut court n'est plus de celles-la : il se tient, comme dans l'original, et montre
        // donc son point d'arrivee pendant tout le maintien — mais sa destination se calcule avec
        // le monde et le joueur, donc ce cas-la ne se relit pas ici, sans un vrai joueur.
        // Le saut traversant non plus, tant qu'aucune visee n'est ouverte : c'est son etat qui
        // decide, et non la touche. Voir TeleportAim.
        assertNull(TeleportMark.seat(null, TeleporterCategory.PENETRATE_TELEPORT, 0, 3));
        assertNull(TeleportMark.seat(null, TeleporterCategory.LOCATION_TELEPORT, 0, 3));
    }

    @Test
    @DisplayName("la marque a deux formes, et deux seulement : le fantome et la boite")
    void laMarqueADeuxFormes() {
        // Le fantome : le modele du joueur lui donne sa silhouette, donc la marque n'a aucune
        // dimension propre. C'est la forme des quatre competences qui teleportent le corps.
        assertFalse(TeleportMark.Shape.GHOST.isBox(),
                "un fantome n'est pas une boite, sinon le rendu dessinerait un cube autour de lui");

        // La boite : le marqueur de l'original, pour les deux competences qui visent autre chose.
        // Elle est cubique chez lui — `marker.width = marker.height` — donc une seule taille.
        TeleportMark.Shape demi = TeleportMark.Shape.box(0.5);
        assertTrue(demi.isBox(), "un demi-bloc est une boite");
        assertEquals(0.5, demi.width(), 1e-9);
        assertEquals(0.5, demi.height(), 1e-9);

        TeleportMark.Shape bloc = TeleportMark.Shape.box(1.0);
        assertEquals(1.0, bloc.width(), 1e-9);
        assertEquals(1.0, bloc.height(), 1e-9);

        // Et une boite de dimension nulle ne serait pas dessinable : c'est le fantome.
        assertFalse(new TeleportMark.Shape(0.0, 0.0).isBox());
        assertFalse(new TeleportMark.Shape(0.5, 0.0).isBox(), "une boite plate n'en est pas une");
    }

    @Test
    @DisplayName("les deux teintes de l'original sont distinguees")
    void lesDeuxTeintesDeLOriginal() {
        // Le blanc des marques sans rien a signaler, et le rouge de celles qui signalent une cible
        // ou un obstacle — celles-la portent un fantome.
        assertEquals(0xFFFFFFFF, TeleportMark.COLOR_NORMAL);
        assertEquals(0xFFFF3333, TeleportMark.COLOR_THREATENING);

        // Les boites ont les leurs, prises a l'original — et dans son ordre de canaux, qui est du
        // RGBA : le rouge du lancer d'objet vient de 0xba, son vert de 0xb2, son bleu de 0x23. Le
        // quatrieme octet, son alpha, n'est pas repris : son rendu ne le lisait pas, donc la boite
        // sortait pleine, et c'est ce que l'on veut ici aussi.
        assertEquals(0xFFBABABA, TeleportMark.COLOR_VOID, "le gris du lancer d'objet");
        assertEquals(0xFFBAB223, TeleportMark.COLOR_HIT_ORANGE, "son ORANGE quand il vise");
        assertEquals(0xFF4A4A4A, TeleportMark.COLOR_FLESH_IDLE, "le gris eteint de la chair");
        assertEquals(0xFFB91919, TeleportMark.COLOR_FLESH_HIT, "le rouge de la chair");

        // Et les quatre sont opaques : le quatrieme octet est plein.
        for (int color : new int[] { TeleportMark.COLOR_VOID, TeleportMark.COLOR_HIT_ORANGE,
                TeleportMark.COLOR_FLESH_IDLE, TeleportMark.COLOR_FLESH_HIT }) {
            assertEquals(0xFF, color >>> 24, "une boite se voit pleine");
        }

        // Ce qui separe les deux, c'est le canal vert : presque aussi fort que le rouge pour
        // l'orange (0xb2 contre 0xba), et six fois plus faible pour le rouge de la chair (0x19
        // contre 0xb9). Une inversion de canaux, elle, fait tomber cet ecart.
        assertNotEquals(TeleportMark.COLOR_HIT_ORANGE, TeleportMark.COLOR_FLESH_HIT);
        assertTrue(((TeleportMark.COLOR_HIT_ORANGE >> 8) & 0xFF)
                > 0.9 * ((TeleportMark.COLOR_HIT_ORANGE >> 16) & 0xFF),
                "l'orange a le vert presque au niveau du rouge");
        assertTrue(2 * ((TeleportMark.COLOR_FLESH_HIT >> 8) & 0xFF)
                < ((TeleportMark.COLOR_FLESH_HIT >> 16) & 0xFF),
                "le rouge de la chair a le vert tres bas");
    }

    @Test
    @DisplayName("les boites ont les tailles de l'original")
    void lesTaillesDesBoites() {
        // Un demi-bloc pour le lancer d'objet dans le vide, un bloc entier pour la chair qui ne
        // trouve personne.
        assertEquals(0.5, TeleportMark.VOID_BOX, 1e-9);
        assertEquals(1.0, TeleportMark.FLESH_BOX, 1e-9);

        // Et une creature visee donne une boite batie sur SES dimensions : sa largeur et sa
        // hauteur, pas un cube — et sans grossissement, celle du lancer d'objet venant deja de
        // couvrir la bete en entier.
        TeleportMark.Shape bete = TeleportMark.Shape.box(0.6, 1.95);
        assertTrue(bete.isBox());
        assertEquals(0.6, bete.width(), 1e-9);
        assertEquals(1.95, bete.height(), 1e-9);
    }

    @Test
    @DisplayName("l'epaisseur d'un trait suit la distance : c'est une largeur d'ecran")
    void lEpaisseurSuitLaDistance() {
        // Un trait de l'original etait une ligne de GL, large de trois pixels quelle que soit la
        // distance — le monde grandit et rapetisse autour d'elle. Rendue en blocs, cette constance
        // veut dire une epaisseur PROPORTIONNELLE a la distance, et c'est cet invariant qui tient
        // l'aspect du trait : l'epaisseur divisee par la distance ne bouge pas.
        for (double distance = 2.0; distance <= 32.0; distance *= 2.0) {
            assertEquals(TpMarkRenderer.STROKE / TpMarkRenderer.STROKE_REFERENCE,
                    TpMarkRenderer.strokeAt(distance) / distance, 1e-9,
                    "a " + distance + " blocs, la largeur d'ecran ne change pas");
        }

        // Donc elle grandit avec la distance, et pas l'inverse : c'est le trait de pres qui est
        // fin, et celui de loin qui est gros — en blocs, parce qu'a l'ecran les deux font pareil.
        assertTrue(TpMarkRenderer.strokeAt(2.0) < TpMarkRenderer.strokeAt(8.0));
        assertTrue(TpMarkRenderer.strokeAt(8.0) < TpMarkRenderer.strokeAt(16.0));

        // La reference est la distance ou l'epaisseur vaut STROKE, donc celle ou elle fait les trois
        // pixels de l'original.
        assertEquals(TpMarkRenderer.STROKE,
                TpMarkRenderer.strokeAt(TpMarkRenderer.STROKE_REFERENCE), 1e-9);

        // Et il y a un plancher : une marque qui se tient dans la camera ne donne pas un trait de
        // largeur nulle, qui disparaitrait.
        assertEquals(TpMarkRenderer.strokeAt(1.0), TpMarkRenderer.strokeAt(0.0), 1e-9);
    }

    @Test
    @DisplayName("la marque glisse quand elle se deplace, et se pose quand elle saute")
    void laMarqueGlisseOuSePose() {
        Vec3 from = new Vec3(10, 64, -5);

        // Sans point precedent, il n'y a rien a interpoler : la marque se pose. C'est le cas d'une
        // marque qui nait, et sans lui elle glisserait depuis la position de la precedente.
        Vec3 birth = new Vec3(30, 70, 20);
        assertEquals(birth, TeleportMark.follow(null, birth));

        // Un deplacement d'un tick — une creature qui marche, un regard qui balaie — se glisse.
        Vec3 walking = from.add(0.3, 0, 0.4);
        assertEquals(from, TeleportMark.follow(from, walking));

        // Et un saut ne se glisse pas : on se pose dessus. Autrement le fantome traverserait tout
        // ce qu'il y a entre les deux points, ce qui se voit comme un bond sans raison.
        Vec3 far = from.add(20, 0, 0);
        assertEquals(far, TeleportMark.follow(from, far));
        assertEquals(from.add(0, 0, 20), TeleportMark.follow(from, from.add(0, 0, 20)));

        // La limite elle-meme, des deux cotes : trois blocs glissent, trois et un chouia se pose.
        Vec3 edge = from.add(TeleportMark.SMOOTH_DISTANCE, 0, 0);
        assertEquals(from, TeleportMark.follow(from, edge));
        assertEquals(edge.add(0.01, 0, 0), TeleportMark.follow(from, edge.add(0.01, 0, 0)));
    }

    @Test
    @DisplayName("une marque finie ne se dessine nulle part, a aucun instant de l'image")
    void uneMarqueFinieNeSeDessinePas() {
        // Le rendu demande la position a l'instant de l'image, et pas a celui du tick, pour suivre
        // une creature qui bouge : c'est cette seconde porte qu'il faut fermer aussi, sinon un
        // fantome resterait plante la ou la competence s'est arretee.
        TeleportMark.end();
        assertNull(TeleportMark.position());
        assertNull(TeleportMark.interpolated(0.0));
        assertNull(TeleportMark.interpolated(0.5));
        assertNull(TeleportMark.interpolated(1.0));
    }

    @Test
    @DisplayName("le fantome s'efface quand la competence s'arrete")
    void leFantomeSEffaceQuandLaCompetenceSArrete() {
        // Rien a dessiner au depart : le rendu ne fait rien tant que la marque n'existe pas.
        TeleportMark.end();
        assertNull(TeleportMark.position());
        assertEquals(0, TeleportMark.ageTicks(), "son age repart de zero a chaque marque");

        // Et les etincelles deja semees s'en vont avec le monde, pas avec la competence : la
        // derniere image d'un saut doit pouvoir finir de se dissiper.
        TpParticles.spawn(Vec3.ZERO, Vec3.ZERO, 0L, new Random(2));
        assertFalse(TpParticles.live().isEmpty());
        TpParticles.clear();
        assertTrue(TpParticles.live().isEmpty());
    }
}
