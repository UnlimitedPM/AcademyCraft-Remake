package cn.academy.ability.develop;

import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;

/**
 * Apprendre une competence precise.
 *
 * Portage de {@code DevelopActionSkill}. C'est le deuxieme apprentissage du
 * developpeur : le premier fait monter une categorie d'un niveau, celui-ci apprend
 * une des competences de ce niveau.
 *
 * Le cout suit le niveau de la competence ({@code 3 + niveau²/2} stimulations,
 * voir {@link Skill#getLearningStims()}), donc apprendre les competences hautes est
 * nettement plus long que les premieres.
 *
 * La validation rejoue {@code LearningHelper} : le joueur peut lancer un
 * apprentissage puis changer d'avis, ou perdre une condition en route, et il
 * echoue alors apres avoir paye. C'est le comportement de l'original, qui validait
 * a la fin et non au debut.
 */
public class DevelopActionSkill implements DevelopAction {

    private final Skill skill;

    public DevelopActionSkill(Skill skill) {
        this.skill = skill;
    }

    public Skill getSkill() {
        return skill;
    }

    @Override
    public int getCategoryId() {
        return skill.getCategory().getCategoryId();
    }

    @Override
    public int getSkillId() {
        return skill.getId();
    }

    @Override
    public int getStimulations(Player player) {
        return skill.getLearningStims();
    }

    @Override
    public boolean validate(Player player, DeveloperType developer) {
        Optional<AbilityData> data = player.getCapability(AbilityCapability.ABILITY_DATA).resolve();
        if (data.isEmpty()) return false;
        if (data.get().isSkillLearned(skill)) return false;
        return LearningHelper.canLearn(data.get(), skill, developer);
    }

    @Override
    public void onLearned(Player player) {
        player.getCapability(AbilityCapability.ABILITY_DATA)
                .ifPresent(data -> data.learnSkill(skill));
    }
}
