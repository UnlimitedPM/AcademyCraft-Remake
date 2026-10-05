package cn.academy.ability.client.vm;

import cn.academy.ability.Skill;
import cn.academy.ability.vecmanip.VecAccelSkill;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * La parabole de visee de l'acceleration de vecteur, portage de {@code ParabolaEffect}.
 *
 * <p>La seule competence de vecmanip qui se <b>vise</b> : on charge, on regarde, et une ligne de
 * lueur montre ou l'on va partir. Elle part de la main droite du porteur — un peu en dessous des
 * yeux —, suit le regard <b>baisse de dix degres</b> (c'est l'inclinaison du lancement), et
 * retombe sous son propre poids. La montee de la charge la fait partir de plus loin.
 *
 * <h2>Le lancer est simule, pas calcule</h2>
 *
 * <p>Cent pas de deux centiemes de seconde, une trainee de 0,98 par pas — l'air freine — et une
 * pesanteur de 1,9 par seconde carree. C'est exactement ce que fait le serveur au tir
 * ({@code setDeltaMovement}), a ceci pres que le serveur n'applique le frein et la pesanteur que
 * par le deplacement de vanilla, qui vaut la meme chose. La parabole ne peut donc pas mentir sur
 * la direction, seulement sur l'endroit exact ou l'on s'arretera — un mur, un plafond, un arbre.
 *
 * <h2>Et sa couleur</h2>
 *
 * <p>L'original la passait au <b>rouge</b> quand le saut n'aurait pas eu lieu : au sol sous la
 * moitie de l'experience. Le port ne refuse jamais ce saut — il est plus permissif que l'original,
 * et c'est un choix assume — donc sa parabole reste blanche : un avertissement qui ne correspond a
 * rien serait pire que pas d'avertissement du tout.
 */
public final class VecAccelPreview {

    /** La simulation : cent pas de deux centiemes de seconde. */
    public static final int STEPS = 100;
    public static final double STEP_SECONDS = 0.02;

    /** L'air freine de deux centiemes par pas, et la pesanteur vaut 1,9 par seconde carree. */
    public static final double DRAG = 0.98;
    public static final double GRAVITY = 1.9;

    /**
     * Le decalage lateral du depart, vers la droite du porteur.
     *
     * <p>L'original en donnait huit centimetres — son propre {@code -0.08}, applique au regard
     * tourne d'un quart de tour. Le joueur trouve la parabole du port un peu trop a droite de son
     * ecran par rapport au vrai mod : le decalage est donc reduit de moitie, et c'est le seul
     * nombre que cette correction touche.
     */
    public static final double HAND_SIDE = -0.04;

    /** Le reste du depart : 1,56 de haut — la main, pas les yeux — et douze centimetres en arriere. */
    public static final double HAND_HEIGHT = 1.56;
    public static final double HAND_BACK = -0.12;

    /** L'inclinaison du lancer : dix degres sous le regard. */
    public static final float LAUNCH_PITCH = -10;

    /** Le ruban : deux centimetres de haut, et une opacite qui decroit de trois centiemes par pas. */
    public static final double RIBBON_HALF = 0.02;
    public static final double ALPHA_START = 0.7;
    public static final double ALPHA_STEP = 0.03;

    /** La ou part la parabole : la main du porteur, dans son repere. */
    public static Vec3 handFrom(Vec3 feet, Vec3 look) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        // Un regard tout droit vers le ciel n'a pas de droite : on retombe alors sur le nord, comme
        // l'original qui normalisait un vecteur nul.
        Vec3 side = flat.lengthSqr() < 1e-9 ? new Vec3(0, 0, 1) : new Vec3(flat.z, 0, -flat.x).normalize();
        return feet.add(side.scale(HAND_SIDE)).add(0, HAND_HEIGHT, 0).add(look.scale(HAND_BACK));
    }

    /** La vitesse de lancement : le regard baisse de dix degres, a la vitesse de la charge. */
    public static Vec3 initialSpeed(float pitch, float yaw, double speed) {
        return Vec3.directionFromRotation(pitch + LAUNCH_PITCH, yaw).scale(speed);
    }

    /** La trajectoire, pas a pas : c'est la simulation de l'original, dans le meme ordre. */
    public static List<Vec3> points(Vec3 hand, Vec3 initialSpeed) {
        List<Vec3> out = new ArrayList<>(STEPS);
        Vec3 position = hand;
        Vec3 speed = initialSpeed;

        for (int i = 0; i < STEPS; i++) {
            out.add(position);
            speed = speed.scale(DRAG);
            position = position.add(speed.scale(STEP_SECONDS));
            speed = new Vec3(speed.x, speed.y - STEP_SECONDS * GRAVITY, speed.z);
        }
        return List.copyOf(out);
    }

    /** L'opacite du ruban a un pas donne : il s'efface vers sa fin. */
    public static double alpha(int index) {
        return Math.max(0, ALPHA_START - index * ALPHA_STEP);
    }

    /** La parabole vivante : son porteur, et la vitesse de sa charge. */
    public static final class Live {

        private final Player player;
        private double speed;

        private Live(Player player, double speed) {
            this.player = player;
            this.speed = speed;
        }

        public Player player() {
            return player;
        }

        public double speed() {
            return speed;
        }
    }

    private static Live live;

    private VecAccelPreview() {
    }

    /** Un tick de charge : la parabole se pose au premier, et grandit avec la charge. */
    public static void tickHeld(Player player, Skill skill, int chargeTicks) {
        if (skill != VecmanipCategory.VEC_ACCEL) return;
        if (live == null) {
            live = new Live(player, VecAccelSkill.speedAt(chargeTicks));
        } else {
            live.speed = VecAccelSkill.speedAt(chargeTicks);
        }
    }

    /** Le relachement : la parabole s'en va, que le saut ait lieu ou non. */
    public static void end(Skill skill) {
        if (skill == VecmanipCategory.VEC_ACCEL) live = null;
    }

    /** Ce qu'il y a a dessiner, ou rien. */
    public static Live live() {
        return live;
    }

    /** Et tout oublier : la deconnexion d'un monde n'est pas une fin de competence. */
    public static void clear() {
        live = null;
    }
}
