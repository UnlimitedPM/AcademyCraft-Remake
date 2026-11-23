package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;

/**
 * La theorie du repli dimensionnel, passive du teleporteur.
 *
 * <p>Elle ne protege de <b>rien</b> : c'est un bonus de degats. L'original la laissait vide —
 * c'est {@code TPSkillHelper} qui la remplissait — et s'en servait comme d'une reserve
 * d'experience : son experience decide de la probabilite et de la force des coups critiques du
 * teleporteur, et chaque critique la fait grandir. Voir {@link TeleportCrits}.
 *
 * <p>Elle a porte un temps une immunite aux degats de chute, et c'est une invention : les deux
 * seules traces de {@code fallDistance} de l'original sont dans le scintillement et le saut
 * traversant, qui l'effacent <b>au moment de la teleportation</b>. C'est d'ailleurs ce qu'on
 * veut : apres un saut, aucune chute a payer ; sans saut, une chute se paie.
 */
public class DimFoldingTheoremSkill extends Skill {

    public DimFoldingTheoremSkill() {
        super("dim_folding_theorem", 1);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    /**
     * Elle ne se range pas sur une touche : c'est le bonus des autres teleportations, pas un
     * pouvoir a part entiere. L'original le disait par {@code canControl = false}, et le joueur
     * l'a rappele — la proposer dans l'editeur de prereglaGes laisserait croire qu'elle se lance.
     */
    @Override
    public boolean canControl() {
        return false;
    }

    /**
     * Experience versee a chaque teleportation, 0,005 comme dans l'original.
     *
     * L'original comptait les teleportations depuis un utilitaire partage
     * ({@code TPSkillHelper.incrTPCount}) appele par les deux competences de
     * teleportation, et faisait grandir le gain avec la suite de sauts. Le port reprend
     * le gain de base et laisse les deux competences appeler directement — la suite de
     * sauts, elle, se reglera avec le reste de la progression.
     */
    public void onTeleported(AbilityData data) {
        data.addSkillExp(this, 0.005f);
    }
}
