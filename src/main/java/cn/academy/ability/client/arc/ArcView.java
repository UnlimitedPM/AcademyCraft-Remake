package cn.academy.ability.client.arc;

/**
 * L'optimisation de vue de l'original : l'eclair du tireur part de sa camera.
 *
 * <p>Portage de ce que LambdaLib2 appelait {@code ViewOptimize.fix}, et qui n'est pas un
 * detail d'implementation : c'est ce qui explique tout le reste. L'original posait son arc
 * aux YEUX du joueur et le long de son regard. Chez les autres joueurs, cela se lit tres
 * bien — l'eclair sort de leur tete. Mais chez le tireur, la camera de troisieme personne est
 * quatre blocs DERRIERE ces yeux, sur le meme axe : un arc pose aux yeux y apparait donc deja
 * commence par le bas de l'ecran, et il ne passe pas par le personnage. C'est exactement ce
 * que le joueur a reconnu en visant le ciel : l'attaque ne part pas de son personnage du tout.
 *
 * <p>Recoller le depart sur la camera ne change ni la direction ni la longueur : le glissement
 * se fait le long de l'axe de l'arc, puisque la camera est sur cet axe. L'eclair est donc le
 * meme trait, simplement prolonge jusqu'a l'oeil — et il suit la camera si elle bouge, d'un
 * bout a l'autre.
 *
 * <p>En vue interne, la camera est aux yeux : le glissement est alors nul, et l'eclair garde
 * exactement son depart. C'est la meme chose que de ne rien faire, et c'est ce que verifie le
 * test.
 *
 * <p>Aucun type de Minecraft ici : les points sont des coordonnees, et le glissement se relit
 * en test.
 */
public final class ArcView {

    private ArcView() {}

    /**
     * L'arc, recolle a la camera : les deux bouts glissent du meme vecteur, celui qui va du
     * depart a la camera. Le depart devient la camera, l'arrivee recule d'autant.
     */
    public static double[][] fix(double[] from, double[] to, double[] camera) {
        double dx = camera[0] - from[0];
        double dy = camera[1] - from[1];
        double dz = camera[2] - from[2];

        return new double[][] {
                { camera[0], camera[1], camera[2] },
                { to[0] + dx, to[1] + dy, to[2] + dz } };
    }
}
