package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.ActivateSkillPacket;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class AbilityClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Pilot wiring: hardcode one key per skill until the preset/key-mapping system is ported.
        if (AbilityKeyBindings.ACTIVATE_SKILL.consumeClick()) {
            send(VecmanipCategory.NAME, "vec_accel");
        }
        if (AbilityKeyBindings.ACTIVATE_ARC_GEN.consumeClick()) {
            send(ElectromasterCategory.NAME, "arc_gen");
        }
        if (AbilityKeyBindings.ACTIVATE_RAILGUN.consumeClick()) {
            send(ElectromasterCategory.NAME, "railgun");
        }
        if (AbilityKeyBindings.ACTIVATE_BODY_INTENSIFY.consumeClick()) {
            send(ElectromasterCategory.NAME, "body_intensify");
        }
        if (AbilityKeyBindings.ACTIVATE_SHIFT_TP.consumeClick()) {
            send(TeleporterCategory.NAME, "shift_tp");
        }
        if (AbilityKeyBindings.ACTIVATE_PENETRATE_TP.consumeClick()) {
            send(TeleporterCategory.NAME, "penetrate_teleport");
        }
    }

    private static void send(String categoryName, String skillName) {
        Category category = CategoryManager.INSTANCE.getCategory(categoryName);
        if (category == null) return;
        Skill skill = category.getSkill(skillName);
        if (skill == null) return;
        AbilityNetwork.CHANNEL.sendToServer(new ActivateSkillPacket(category.getCategoryId(), skill.getId()));
    }
}
