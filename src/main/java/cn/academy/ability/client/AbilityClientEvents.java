package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.ActivateSkillPacket;
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
        if (!AbilityKeyBindings.ACTIVATE_SKILL.consumeClick()) return;

        // Pilot wiring: hardcode Vecmanip/vec_accel until the preset/key-mapping system is ported.
        Category category = CategoryManager.INSTANCE.getCategory(VecmanipCategory.NAME);
        if (category == null) return;
        Skill skill = category.getSkill("vec_accel");
        if (skill == null) return;
        AbilityNetwork.CHANNEL.sendToServer(new ActivateSkillPacket(category.getCategoryId(), skill.getId()));
    }
}
