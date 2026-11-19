package cn.academy.ability.client.vm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Le geste du poing de vecmanip.
 *
 * <p>Ce qui se relit sans Minecraft : les deux courbes, leurs instants remarquables et le passage
 * des ticks aux secondes de l'original. Un geste qui part du mauvais cote ou qui ne revient pas
 * exactement a sa place ne se verraient qu'a l'ecran, et c'est justement ce qu'aucune des quatre
 * portes ne regarde.
 */
class HandAnimTest {

    @Test
    void laChargeArmeLePoingSansLeDecaler() {
        HandAnim anim = HandAnim.PREPARE;

        assertPose(0, 0, 0, 0, 0, 0, anim.pose(0), "le poing part de sa place ordinaire");

        // Le plein de la charge : la main a recule, monte et s'est couchee vers l'arriere, et les
        // deux derniers angles n'ont pas bouge — l'original ne s'en servait pas.
        assertPose(-0.02, 0.4, -0.05, -20, 0, 0, anim.pose(1), "au bout de son plein");
    }

    @Test
    void uneChargeQuiDureContinueDeMonterPuisSEntete() {
        // L'original plafonnait le temps a 2,0, et sa derniere courbe est prolongee en ligne
        // droite : la main finit donc a 0,8 et n'en bouge plus, tant que la touche se tient.
        assertEquals(0.8, HandAnim.PREPARE.pose(HandAnim.PREPARE_HOLD).y(), 1e-9,
                "la garde se tient a huit dixiemes");
        // Et le temps ne monte plus : au plafond, et bien apres, c'est la meme pose.
        assertEquals(HandAnim.PREPARE.pose(HandAnim.PREPARE_HOLD),
                HandAnim.PREPARE.pose(HandAnim.prepareTime(600)),
                "le poing ne bouge plus une fois la garde prise");
    }

    @Test
    void leCoupAbatLePoingPuisLeRepose() {
        HandAnim anim = HandAnim.PUNCH;

        // Le coup commence HAUT, poignet couche : c'est ce depart qui lui donne son abattage.
        assertPose(-0.04, 0.8, 0, -40, 0, 0, anim.pose(0), "le poing part de dessus");

        // Les autres instants remarquables de ses courbes, un par point : au tiers du geste la
        // main pousse vers l'ecran et le poignet s'ouvre d'un dixieme de tour en Y ; au milieu,
        // la main est encore haute et le poignet se creuse au maximum.
        assertEquals(-0.4, anim.pose(0.3).z(), 1e-9, "au tiers, la poussee vers l'ecran");
        assertEquals(10, anim.pose(0.3).ry(), 1e-9, "au tiers, le poignet qui s'ouvre");
        assertEquals(0.75, anim.pose(0.5).y(), 1e-9, "a mi-geste, la main encore haute");
        assertEquals(-45, anim.pose(0.5).rx(), 1e-9, "a mi-geste, le poignet creuse");

        // Et il revient EXACTEMENT a sa place : c'est ce qui permet a Minecraft de reprendre la
        // main sans que le joueur voie de saut au dernier tick.
        assertPose(0, 0, 0, 0, 0, 0, anim.pose(1), "le poing est rentre");
    }

    @Test
    void lesDeuxTempsSeLisentEnTicks() {
        // La charge atteint son plein en trois ticks (0,15 s), et son temps est plafonne a 2.
        assertEquals(0.0, HandAnim.prepareTime(0), 1e-9, "rien au premier tick");
        assertEquals(1.0, HandAnim.prepareTime(3), 1e-9, "pleine au troisieme");
        assertEquals(2.0, HandAnim.prepareTime(6), 1e-9, "et au double ensuite");
        assertEquals(2.0, HandAnim.prepareTime(600), 1e-9, "meme longtemps apres");

        // Le coup se parcourt en six ticks (0,3 s), et vaut un quand il a fini.
        assertEquals(0.0, HandAnim.punchTime(0), 1e-9, "rien au premier tick");
        assertEquals(0.5, HandAnim.punchTime(3), 1e-9, "a mi-geste");
        assertEquals(1.0, HandAnim.punchTime(6), 1e-9, "et rentre au sixieme");
        assertEquals(HandAnim.PUNCH_TICKS, 6, "six ticks, c'est bien trois dixiemes de seconde");
    }

    /**
     * La pose attendue, composant par composant.
     *
     * <p>Chaque nombre est compare avec une tolerance : la courbe cubique repasse par ses points,
     * mais a un chouia pres, et une egalite stricte de la pose entiere tomberait sur ces poussieres.
     */
    private static void assertPose(double x, double y, double z,
                                   double rx, double ry, double rz,
                                   HandAnim.Pose actual, String why) {
        assertEquals(x, actual.x(), 1e-9, why + " (x)");
        assertEquals(y, actual.y(), 1e-9, why + " (y)");
        assertEquals(z, actual.z(), 1e-9, why + " (z)");
        assertEquals(rx, actual.rx(), 1e-9, why + " (rx)");
        assertEquals(ry, actual.ry(), 1e-9, why + " (ry)");
        assertEquals(rz, actual.rz(), 1e-9, why + " (rz)");
    }
}
