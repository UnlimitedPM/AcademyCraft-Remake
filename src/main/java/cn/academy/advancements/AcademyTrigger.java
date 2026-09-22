package cn.academy.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Le declencheur des succes du mod : un seul type, quinze instances.
 *
 * <p>Portage de {@code ACTrigger}. L'original n'avait pas d'avantage de mecanisme : chaque
 * succes du mod se declenchait sur un <b>evenement maison</b> — apprendre une competence,
 * changer de categorie, fabriquer tel objet — et se contentait de le signaler. Une seule
 * classe suffit donc, et son identifiant vient de son enregistrement.
 *
 * <p>En 1.20.1 un declencheur n'est pas un objet enregistre dans un registre : il vit dans
 * une simple table de {@code CriteriaTriggers}, rangee par identifiant. Il faut donc une
 * instance <b>par nom</b>, chacune portant le sien — c'est ce que {@code PlayerTrigger}
 * fait aussi, pour la meme raison. La condition est vide : un succes de ce mod se declenche
 * parce que l'action a eu lieu, pas parce qu'un predicat sur le joueur est vrai.
 */
public class AcademyTrigger extends SimpleCriterionTrigger<AcademyTrigger.Instance> {

    private final ResourceLocation id;

    public AcademyTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(id, player);
    }

    /** Declenche le succes pour ce joueur, sans condition. */
    public void trigger(ServerPlayer player) {
        trigger(player, instance -> true);
    }

    /** Ce que le fichier de succes a le droit de dire : rien, ou un predicat de joueur. */
    public static class Instance extends AbstractCriterionTriggerInstance {

        public Instance(ResourceLocation id, ContextAwarePredicate player) {
            super(id, player);
        }
    }
}
