package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Single generic "activate skill" key for now, pilot-bound to Vecmanip/vec_accel in
 * {@link AbilityClientEvents}. Will be replaced by a per-preset key system later.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbilityKeyBindings {

    public static final KeyMapping ACTIVATE_SKILL = new KeyMapping(
            "key.academy.activate_skill", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_R, "key.categories.academy");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_SKILL);
    }
}
