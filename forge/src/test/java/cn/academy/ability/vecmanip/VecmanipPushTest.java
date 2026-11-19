package cn.academy.ability.vecmanip;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La poussee commune de vecmanip, {@code VecmanipPush}.
 *
 * <p>C'est la fonction que partagent le choc dirige (0,6 de souleve, 0,7 de force) et
 * l'onde de choc dirigee (0,4 et 1,2), et c'est aussi la ou vit la <b>coquille corrigee</b>
 * de l'original : son axe Z recevait la composante verticale, si bien qu'une cible droit
 * devant ne reculait pas du tout. Le test la fixe pour les deux competences a la fois.
 *
 * <p>La meme classe porte les deux autres directions de vecmanip, celles qui ne dependent
 * que d'un point : la <b>bousculade</b> des ondes (0,24 dans l'axe joueur vers cible) et le
 * <b>renvoi</b> de la reflexion, qui retourne une entite vers ce que le regard touche en lui
 * gardant sa vitesse.
 */
class VecmanipPushTest {

    private static Vec3 push(double lift, double force, Vec3 targetEye) {
        return VecmanipPush.push(new Vec3(0, 0, 0), targetEye, lift, force);
    }

    @Test
    void laCiblePartEnArriereEtEnLAir() {
        // Le cas le plus courant : une cible droit devant, a la meme hauteur. Sans la
        // correction, son recul horizontal vaudrait zero et elle monterait seulement.
        Vec3 punch = push(0.6, 0.7, new Vec3(0, 0, 1));
        assertTrue(punch.z > 0, "la cible recule : " + punch);
        assertTrue(punch.y > 0, "et elle monte : " + punch);
        assertEquals(0.600, punch.z, 0.001, "le recul du choc dirige");
        assertEquals(0.360, punch.y, 0.001, "son soulevement");

        // L'onde dirigee pousse plus fort, et plus a plat : sa force plus grande compense
        // son soulevement plus faible, donc c'est la <b>part</b> verticale qui baisse, pas
        // la hauteur.
        Vec3 blast = push(0.4, 1.2, new Vec3(0, 0, 1));
        assertTrue(blast.z > punch.z, "la vague pousse plus loin que le poing : " + blast);
        assertTrue(blast.y / blast.length() < punch.y / punch.length(),
                "et elle pousse plus a plat : " + blast);
        assertEquals(1.115, blast.z, 0.001, "le recul de l'onde dirigee");
        assertEquals(0.446, blast.y, 0.001, "sa hauteur");
    }

    @Test
    void chaqueAxePorteSaPropreComposante() {
        // Cible de cote : elle part de l'autre cote, et l'axe Z ne bouge pas. Avec la
        // coquille de l'original, il aurait recu la composante verticale — nulle ici.
        Vec3 side = push(0.6, 0.7, new Vec3(1, 0, 0));
        assertTrue(side.x > 0, "une cible de cote part de l'autre cote : " + side);
        assertEquals(0.0, side.z, 0.0001, "sans derive sur l'axe Z");

        // Cible au-dessus : elle monte, c'est le sens de l'eloignement.
        Vec3 above = push(0.6, 0.7, new Vec3(0, 3, 0));
        assertTrue(above.y > 0, "une cible au-dessus monte : " + above);

        // Cible en dessous : elle part vers le bas, comme il faut.
        Vec3 below = push(0.6, 0.7, new Vec3(0, -3, 0));
        assertTrue(below.y < 0, "une cible en dessous descend : " + below);
    }

    @Test
    void laForceNeDependPasDeLaVisee() {
        // La poussee garde toujours sa longueur : l'original appliquait sa force a une
        // direction unitaire, quelle que soit la position de la cible.
        for (Vec3 eye : new Vec3[] {
                new Vec3(2, 0, 0), new Vec3(0, 0, -4), new Vec3(3, 1, 3),
                new Vec3(0, -3, 0), new Vec3(0, 5, 0) }) {
            assertEquals(0.7, push(0.6, 0.7, eye).length(), 0.0001, "force du choc pour " + eye);
            assertEquals(1.2, push(0.4, 1.2, eye).length(), 0.0001, "force de la vague pour " + eye);
        }

        // Deux yeux au meme endroit n'ont pas de direction : pas de division par zero.
        assertEquals(Vec3.ZERO, push(0.6, 0.7, new Vec3(0, 0, 0)));
    }

    @Test
    void laBousculadeViseLaCibleSansTenirCompteDeLaHauteur() {
        // 0,24, dans l'axe joueur -> cible, en trois coordonnees comme l'original : une
        // cible plus haute est un peu soulevee au passage.
        Vec3 flat = VecmanipPush.shove(new Vec3(0, 0, 0), new Vec3(0, 0, 2));
        assertEquals(0.0, flat.x, 0.0001);
        assertEquals(0.0, flat.y, 0.0001);
        assertEquals(VecmanipPush.SHOVE, flat.z, 0.0001, "deux blocs devant, poussee pleine");

        Vec3 diagonal = VecmanipPush.shove(new Vec3(0, 0, 0), new Vec3(3, 3, 0));
        assertEquals(VecmanipPush.SHOVE, diagonal.length(), 0.0001, "la longueur ne bouge pas");
        assertTrue(diagonal.y > 0, "et la cible plus haute est soulevee : " + diagonal);

        assertEquals(Vec3.ZERO, VecmanipPush.shove(new Vec3(1, 2, 3), new Vec3(1, 2, 3)));
    }

    @Test
    void leRenvoiGardeLaVitesseEtPrendLaDirectionDuRegard() {
        // Le cas de la reflexion : une fleche qui arrivait sur le joueur (mouvement vers -Z)
        // repart vers le point que le regard touche (loin devant, vers +Z), et garde sa
        // vitesse — 1,0 pour une fleche de l'original.
        Vec3 back = VecmanipPush.redirect(new Vec3(0, 0, 20), new Vec3(0, 0, 3), 1.0);
        assertEquals(1.0, back.length(), 0.0001, "la vitesse de l'entite est conservee");
        assertTrue(back.z > 0, "elle repart vers ce que le regard touche : " + back);
        assertEquals(0.0, back.x, 0.0001, "sans derive laterale quand la visee est droite");

        // Une entite immobile ne part pas : le renvoi ne cree pas d'energie.
        assertEquals(Vec3.ZERO, VecmanipPush.redirect(new Vec3(0, 0, 20), new Vec3(0, 0, 3), 0.0));

        // Et une visee confondue avec l'entite n'a pas de direction : pas de division par zero.
        assertEquals(Vec3.ZERO, VecmanipPush.redirect(new Vec3(1, 2, 3), new Vec3(1, 2, 3), 1.0));

        // La hauteur suit la visee : viser plus bas renvoie plus bas.
        Vec3 down = VecmanipPush.redirect(new Vec3(0, -5, 20), new Vec3(0, 1, 0), 2.0);
        assertEquals(2.0, down.length(), 0.0001, "la longueur reste celle du mouvement");
        assertTrue(down.y < 0, "viser sous soi renvoie vers le bas : " + down);
    }
}
