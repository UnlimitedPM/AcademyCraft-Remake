package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Passive skill, port of original DimFoldingTheorem: negates fall damage while learned. */
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

    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        if (event.getSource().is(DamageTypes.FALL)) {
            return 0f;
        }
        return event.getAmount();
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
