package cn.academy.advancements;

import cn.academy.AcademyCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Ce qui declenche un succes sans que personne ne l'ait demande.
 *
 * <p>Deux familles seulement : <b>fabriquer</b> un objet, et en <b>ramasser</b> un. Le
 * reste des succes se declenche la ou l'action a lieu — un niveau qui monte, une categorie
 * qui change, une competence qui s'apprend — parce que ces endroits-la savent deja ce qui
 * vient de se passer.
 *
 * <p>L'original avait un annuaire exactement comme celui-ci : une table objet -> succes,
 * remplie a l'initialisation, et deux ecouteurs qui la consultaient. Le port garde la table
 * dans le code plutot que dans un annuaire modifiable : il n'y a pas de mod tiers pour
 * l'enrichir.
 *
 * <p>Les tables sont rangees par <b>nom d'objet</b> et non par objet : elles se relisent
 * dans un test unitaire, ou aucun registre n'existe. C'est l'appelant qui traduit sa pile
 * en nom.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID)
public class AcademyAdvancementDispatcher {

    /** Fabriquer ces objets donne le succes en face. */
    public static final Map<String, String> CRAFTED = Map.of(
            "phase_generator", AcademyAdvancements.PHASE_GENERATOR,
            "node_basic", AcademyAdvancements.AC_NODE,
            "matrix", AcademyAdvancements.AC_MATRIX,
            "developer_portable", AcademyAdvancements.AC_DEVELOPER);

    /** Ramasser ces objets donne le succes en face. */
    public static final Map<String, String> PICKED_UP = Map.of(
            // Le facteur d'induction : les quatre objets du port en tiennent lieu,
            // l'original n'en avait qu'un dont la categorie vivait dans les metadonnees.
            "factor_electromaster", AcademyAdvancements.GETTING_FACTOR,
            "factor_meltdowner", AcademyAdvancements.GETTING_FACTOR,
            "factor_teleporter", AcademyAdvancements.GETTING_FACTOR,
            "factor_vecmanip", AcademyAdvancements.GETTING_FACTOR,
            // « Unite de matiere remplie de phase » : c'est ce que l'original appelait
            // moissonner, et ce que le port produit quand l'unite fond.
            "matter_unit_phase_liquid", AcademyAdvancements.GETTING_PHASE);

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AcademyAdvancements.award(player, nameFor(CRAFTED, event.getCrafting()));
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AcademyAdvancements.award(player, nameFor(PICKED_UP, event.getItem().getItem()));
    }

    /** Le succes qu'appelle cette pile, ou {@code null} si elle n'en appelle aucun. */
    public static String nameFor(Map<String, String> table, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return null;
        return table.get(key.getPath());
    }
}
