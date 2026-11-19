package cn.academy.ability.teleporter;

import cn.academy.ability.Skill;

/**
 * Space Fluctuation : le passif de niveau 4 du teleporteur.
 *
 * <p>Portage de {@code SpaceFluctuation}, qui n'etait qu'une coquille dans l'original — son
 * effet vivait dans {@code TPSkillHelper}. C'est le second bonus des coups critiques : il
 * augmente la chance de tous les paliers et ouvre a lui seul les deux plus violents (1,6x et
 * 2,6x). Le joueur l'a vu manquer, et il manquait : sans lui, la moitie de l'arbre de la
 * teleportation ne servait a rien.
 *
 * <p>Il monte en gagnant de l'experience a chaque critique (voir {@link TeleportCrits}) : c'est
 * la seule facon de le faire progresser, puisqu'il ne s'active jamais.
 */
public class SpaceFluctuationSkill extends Skill {

    public SpaceFluctuationSkill() {
        super("space_fluct", 4);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public boolean canControl() {
        return false;
    }
}
