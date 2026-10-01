package cn.academy.ability.client.tp;

import cn.academy.ability.teleporter.TeleporterCategory;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertNull(TeleportMark.seat(null, TeleporterCategory.SHIFT_TELEPORT, 0, 0));
        assertNull(TeleportMark.seat(null, TeleporterCategory.SHIFT_TELEPORT, 0, 3));
        // Le saut traversant non plus, tant qu'aucune visee n'est ouverte : c'est son etat qui
        // decide, et non la touche. Voir TeleportAim.
        assertNull(TeleportMark.seat(null, TeleporterCategory.PENETRATE_TELEPORT, 0, 3));
        assertNull(TeleportMark.seat(null, TeleporterCategory.LOCATION_TELEPORT, 0, 3));
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
