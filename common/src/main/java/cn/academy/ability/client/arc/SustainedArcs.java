package cn.academy.ability.client.arc;

import net.minecraft.world.phys.Vec3;

/**
 * Un eclair qui dure tant qu'une touche est tenue : un seul a la fois, remplace quand il s'eteint.
 *
 * <p>Portage du contexte client de l'original pour ses competences tenues : il n'en dessinait
 * qu'<b>un</b>, dont la forme changeait au fil des images. Le port avait d'abord fait l'inverse
 * — un arc par tick, vivant trois ticks — et le joueur y a vu trois eclairs superposes.
 *
 * <p>Ce sont donc les deux competences tenues de l'electromaster qui partagent ce rythme : la
 * charge branche une machine, la manipulation magnetique tire le joueur. Dix ticks, comme la
 * genese d'arc : assez long pour que le trait se lise, assez court pour qu'un nouvel eclair
 * reprenne la place aussitot.
 */
public final class SustainedArcs {

    /** La vie de l'arc, en ticks : celle du motif de la genese d'arc, deja validee. */
    public static final int LIFE_TICKS = 10;

    private SustainedArcs() {
    }

    /**
     * Pose l'arc, ou le deplace s'il est deja la.
     *
     * <p>A appeler a chaque tick du maintien : le premier appel le pose, les suivants le font
     * suivre ce que le joueur regarde. Il n'y en a donc jamais deux, et il n'a jamais de retard
     * sur le regard — c'est le defaut que le joueur avait vu, un eclair qui mettait une
     * demi-seconde a se retourner.
     *
     * <p>Le motif doit etre celui de la competence : l'original donnait a chacune le sien, et
     * c'est toute la difference entre un trait de charge et un trait de traction.
     */
    public static void spawn(ArcPattern pattern, Vec3 from, Vec3 to, int ownerId) {
        ArcRenderer.sustain(pattern.name(), from, to, LIFE_TICKS, ownerId);
    }
}
