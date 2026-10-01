package cn.academy.ability.client.md;

import cn.academy.ability.client.arc.ArcView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Ou un rayon se pose : l'optimisation de vue de l'original, appliquee aux rayons du plasma.
 *
 * <p>C'est la moitie manquante du portage. L'original posait ses rayons aux <b>yeux</b> de son
 * tireur — le {@code y0 + 1.6} de {@code RayBarrage}, le {@code posY + player.eyeHeight} de
 * {@code ElectronBomb} — et laissait son rendu les recoller a sa main :
 * {@code RendererRayBaseGlow} appliquait {@code ViewOptimize.getFixVector} des que
 * {@code ray.needsViewOptimize()}, ce que tous ses rayons demandaient (le drapeau
 * {@code EntityRayBase.viewOptimize} vaut vrai par defaut) sauf les trois qui ne partent pas du
 * tireur : ceux des deux bombes, et la salve — tous nes sur une bille.
 *
 * <p>Le port n'avait pas repris cette etape, et le joueur l'a vu tout de suite : « le rayon
 * nait dans la camera et remplit l'ecran ». Un premier correctif avait decale le depart vers la
 * main droite, a la main, dans la competence. Celui-ci fait la meme chose, mais par le chemin de
 * l'original, et avec ses nombres : le rayon se recolle a la main du tireur <b>exactement</b> la
 * ou se recolle l'eclair de l'electromaster, parce que c'est le meme {@link ArcView} qui s'en
 * charge.
 *
 * <p>Les deux decalages sont donc ceux des eclairs, au chiffre pres : celui de la vue interne
 * pour le rayon de son propre tireur, celui de la main pour tout le reste. La condition est
 * aussi celle de l'original, « thirdPersonView == 0 && clientPlayer == entity.getPlayer() ».
 *
 * <p>Le decalage est applique <b>a la naissance</b>, une fois pour toutes, comme le faisceau du
 * railgun — et non a chaque image comme les eclairs. La difference se voit a une chose : passer
 * en vue externe pendant les deux secondes et demie d'un rayon ne le fait pas glisser de la tete
 * a la main. Ses deux bouts, eux, sont figes de toute facon, chez l'original comme ici.
 *
 * <p>Le calcul lui-meme est pur — voir {@link #place}, qui se relit en test — et c'est la seule
 * raison d'etre de cette classe separee : {@code MdRays} n'a le droit de connaitre que des
 * listes et des nombres.
 */
public final class MdRayView {

    private MdRayView() {}

    /**
     * Pose un rayon annonce par le serveur, sur la main de son tireur s'il en a une.
     *
     * @param ownerId l'identifiant du tireur, ou {@code -1} s'il n'y en a pas
     */
    public static void spawn(MdRayKind kind, Vec3 from, Vec3 to, int ownerId) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean own = minecraft.player != null && minecraft.player.getId() == ownerId;
        boolean ownFirstPerson = own && minecraft.options.getCameraType().isFirstPerson();

        // La verticale de l'ECRAN, comme pour les eclairs : c'est elle qui porte la hauteur du
        // decalage, et le rayon se replace donc correctement meme en regardant le ciel.
        Vector3f up = minecraft.gameRenderer.getMainCamera().getUpVector();
        double[] placed0 = { from.x, from.y, from.z };
        double[] placed1 = { to.x, to.y, to.z };
        double[][] placed = place(kind, placed0, placed1, ownFirstPerson,
                new double[] { up.x, up.y, up.z });

        MdRays.spawn(kind, new Vec3(placed[0][0], placed[0][1], placed[0][2]),
                new Vec3(placed[1][0], placed[1][1], placed[1][2]));
    }

    /**
     * Ou le rayon se dessine : ses deux bouts, et rien d'autre.
     *
     * <p>Un rayon qui n'est pas marque {@code viewOptimize} — ceux qui naissent sur une bille —
     * est rendu tel quel. Les autres passent par {@link ArcView}, qui decale les DEUX bouts du
     * meme vecteur : la direction et la longueur du rayon ne changent donc pas, et ce qu'il
     * touche non plus. Le bout d'arrivee n'est decale que parce que l'original decalait le
     * dessin entier.
     */
    static double[][] place(MdRayKind kind, double[] from, double[] to, boolean ownFirstPerson,
                            double[] above) {
        if (!kind.viewOptimize()) return new double[][] { from, to };
        return ArcView.fix(from, to, above,
                ownFirstPerson ? ArcView.FIRST_PERSON : ArcView.THIRD_PERSON);
    }
}
