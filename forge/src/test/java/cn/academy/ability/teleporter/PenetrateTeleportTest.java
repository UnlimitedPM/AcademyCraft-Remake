package cn.academy.ability.teleporter;

import cn.academy.ability.client.tp.TeleportAim;
import cn.academy.ability.network.TeleportDistancePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le saut traversant : son trajet, et le reglage de sa distance.
 *
 * <p>Le trajet est une fonction pure — on lui donne un point de depart, un regard, une distance, et
 * un predicat qui dit si un bloc arrete le corps — donc il se deroule ici, sans monde ni joueur.
 * C'est la partie qui compte : c'est elle qui decide de quel cote du mur on ressort, et une erreur
 * d'un pas y poserait le joueur dans la pierre.
 */
class PenetrateTeleportTest {

    /** Un mur de deux blocs, de z=4 a z=5. */
    private static final Predicate<BlockPos> WALL = pos -> pos.getZ() >= 4 && pos.getZ() <= 5;

    /** Un mur qui ne finit jamais : on n'en ressort pas. */
    private static final Predicate<BlockPos> DEEP = pos -> pos.getZ() >= 4;

    /** Rien du tout. */
    private static final Predicate<BlockPos> FREE = pos -> false;

    private static final Vec3 START = new Vec3(0.5, 64, 0.5);
    private static final Vec3 LOOK = new Vec3(0, 0, 1);

    @Test
    @DisplayName("le trajet traverse le mur et ressort de l'autre cote")
    void leTrajetTraverseLeMurEtRessort() {
        // Le regard part des PIEDS, comme l'original, et avance par pas de huit dixiemes de bloc.
        // Trois temps : dans le vide, dans la matiere, et ressorti. La destination est le point de
        // sortie, avance de quelques pas pour ne pas se coller au mur : le mur finit a z=6, donc
        // l'atterrissage se fait plus loin que ca.
        PenetrateTeleportSkill.Destination dest =
                PenetrateTeleportSkill.walk(START, LOOK, 10.0, WALL);

        assertTrue(dest.available(), "on est ressorti : la destination est valable");
        assertEquals(0.5, dest.position().x, 1e-9, "le trajet suit le regard, qui va tout droit");
        assertEquals(64.0, dest.position().y, 1e-9);
        assertTrue(dest.position().z > 6.0,
                "on ressort DE L'AUTRE COTE, pas dans le mur : " + dest.position().z);
    }

    @Test
    @DisplayName("un mur sans sortie ne donne pas de destination")
    void unMurSansSortieNeDonnePasDeDestination() {
        // L'original terminait la, sans rien payer : son fantome etait devenu rouge, et le saut
        // n'avait pas lieu. Le trajet avance jusqu'au bout de la distance demandee, mais il finit
        // dans la matiere — c'est ce que dit `available`.
        PenetrateTeleportSkill.Destination dest =
                PenetrateTeleportSkill.walk(START, LOOK, 10.0, DEEP);

        assertFalse(dest.available(), "on n'est jamais ressorti du mur");
        assertTrue(dest.position().z >= 4.0, "mais le trajet a bien ete jusqu'au mur");
    }

    @Test
    @DisplayName("sans obstacle, on va au bout de la distance demandee")
    void sansObstacleOnVaAuBoutDeLaDistance() {
        PenetrateTeleportSkill.Destination dest =
                PenetrateTeleportSkill.walk(START, LOOK, 10.0, FREE);

        assertTrue(dest.available());
        // Le pas du trajet vaut 0,8 bloc, et l'original avancait AVANT de tester : le dernier pas
        // peut donc depasser la distance demandee d'un pas au plus. C'est son compte a lui, et il
        // vaut mieux le garder : c'est ce qui fait que la portee reelle d'une competence qui
        // annonce trente-cinq blocs n'est pas trente-quatre.
        double travelled = dest.position().z - START.z;
        assertTrue(travelled >= 10.0 && travelled <= 10.0 + PenetrateTeleportSkill.STEP,
                "distance parcourue : " + travelled);
    }

    @Test
    @DisplayName("le trajet suit le regard, y compris vers le haut")
    void leTrajetSuitLeRegard() {
        // Regard vers le haut : le trajet monte, et la destination aussi. C'est ainsi qu'on
        // traverse un plafond.
        Vec3 up = new Vec3(0, 1, 0);
        PenetrateTeleportSkill.Destination dest =
                PenetrateTeleportSkill.walk(START, up, 5.0, FREE);

        assertTrue(dest.available());
        assertTrue(dest.position().y > 64.0 + 4.0, "il est monte : " + dest.position().y);
        assertEquals(0.5, dest.position().z, 1e-9, "sans derive sur les autres axes");
    }

    @Test
    @DisplayName("le saut se tient : il se vise avant de partir")
    void leSautSeTient() {
        // Le port le lancait d'un coup a sa portee maximale, des l'appui : le joueur n'avait pas le
        // temps de voir ou il allait atterrir. L'original se tenait, et c'est ce qui rend sa marque
        // utile — la touche enfoncee, on tourne la tete et on regle.
        var jump = cn.academy.ability.teleporter.TeleporterCategory.PENETRATE_TELEPORT;
        assertTrue(jump.isHeld());
        assertTrue(jump.paysOnEffect(), "c'est le saut qui paie, au bloc parcouru");
        assertTrue(jump.earnsExpOnEffect(), "et lui seul connait la distance");
        assertEquals(0f, jump.getCpCost(), 1e-6, "rien a l'appui");
        assertEquals(0f, jump.getExpGain(new cn.academy.ability.AbilityData()), 1e-6,
                "l'experience se verse a l'atterrissage");

        // Et la distance part de sa portee maximale : sans ce pose, la visee vaudrait zero et le
        // saut se ferait sur place.
        cn.academy.ability.AbilityData data = new cn.academy.ability.AbilityData();
        jump.onStart(null, data);
        assertEquals(10.0, data.getHoldDistance(jump), 1e-6, "la portee maximale au depart");
    }

    @Test
    @DisplayName("la molette regle la distance entre ses deux bornes")
    void laMoletteRegleLaDistance() {
        // Un cran, un bloc — c'est l'original — et rien ne sort des bornes : ni sous le minimum
        // d'un demi-bloc, ni au-dela de la portee de la competence.
        assertEquals(1.0, TeleportAim.NOTCH, 1e-9);
        assertEquals(0.5, TeleportAim.MIN_DISTANCE, 1e-9);

        assertEquals(6.0, TeleportAim.scrolled(5.0, 1.0, 35.0), 1e-9);
        assertEquals(4.0, TeleportAim.scrolled(5.0, -1.0, 35.0), 1e-9);
        assertEquals(35.0, TeleportAim.scrolled(35.0, 1.0, 35.0), 1e-9, "le haut ne depasse pas");
        assertEquals(0.5, TeleportAim.scrolled(0.5, -1.0, 35.0), 1e-9, "et le bas non plus");

        // Elle part de sa portee maximale, comme l'original : viser loin est le defaut, et la
        // molette sert a s'en rapprocher.
        TeleportAim.begin(35.0);
        assertTrue(TeleportAim.active());
        assertEquals(35.0, TeleportAim.distance(), 1e-9);
        assertEquals(34.0, TeleportAim.scroll(-1.0, 35.0), 1e-9, "un cran vers le bas");
        TeleportAim.end();
        assertFalse(TeleportAim.active());
    }

    @Test
    @DisplayName("la distance voyage jusqu'au serveur")
    void laDistanceVoyageJusquAuServeur() {
        // C'est le serveur qui fait le saut, et il n'a pas de molette : la distance lui arrive par
        // ce paquet, un cran a la fois. L'aller-retour se relit ici, comme pour les autres.
        TeleportDistancePacket sent = new TeleportDistancePacket(2, 7, 12.5f);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TeleportDistancePacket.encode(sent, buf);

        TeleportDistancePacket read = TeleportDistancePacket.decode(buf);
        assertEquals(2, read.categoryId());
        assertEquals(7, read.skillId());
        assertEquals(12.5f, read.distance(), 1e-6);
    }
}
