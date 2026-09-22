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
 * Temporary one-key-per-skill scheme until the preset/key-mapping system is ported.
 * Wiring lives in {@link AbilityClientEvents}.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbilityKeyBindings {

    public static final KeyMapping ACTIVATE_SKILL = new KeyMapping(
            "key.academy.activate_skill", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_R, "key.categories.academy");

    public static final KeyMapping ACTIVATE_ARC_GEN = new KeyMapping(
            "key.academy.activate_arc_gen", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_G, "key.categories.academy");

    public static final KeyMapping ACTIVATE_RAILGUN = new KeyMapping(
            "key.academy.activate_railgun", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_H, "key.categories.academy");

    public static final KeyMapping ACTIVATE_BODY_INTENSIFY = new KeyMapping(
            "key.academy.activate_body_intensify", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_J, "key.categories.academy");

    public static final KeyMapping ACTIVATE_SHIFT_TP = new KeyMapping(
            "key.academy.activate_shift_tp", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, "key.categories.academy");

    public static final KeyMapping ACTIVATE_PENETRATE_TP = new KeyMapping(
            "key.academy.activate_penetrate_tp", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_L, "key.categories.academy");

    public static final KeyMapping ACTIVATE_MELTDOWNER = new KeyMapping(
            "key.academy.activate_meltdowner", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_Y, "key.categories.academy");

    public static final KeyMapping ACTIVATE_ELECTRON_BOMB = new KeyMapping(
            "key.academy.activate_electron_bomb", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_U, "key.categories.academy");

    /** Le bouclier se tient : c'est la touche qui reste enfoncee. */
    public static final KeyMapping ACTIVATE_LIGHT_SHIELD = new KeyMapping(
            "key.academy.activate_light_shield", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_I, "key.categories.academy");

    public static final KeyMapping ACTIVATE_THUNDER_BOLT = new KeyMapping(
            "key.academy.activate_thunder_bolt", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_N, "key.categories.academy");

    public static final KeyMapping ACTIVATE_THUNDER_CLAP = new KeyMapping(
            "key.academy.activate_thunder_clap", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_B, "key.categories.academy");

    /** Brancher sa reserve sur une machine : la touche reste enfoncee. */
    public static final KeyMapping ACTIVATE_CHARGING = new KeyMapping(
            "key.academy.activate_charging", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_C, "key.categories.academy");

    /** S'accrocher a un metal et se faire tirer dessus : la touche reste enfoncee. */
    public static final KeyMapping ACTIVATE_MAG_MOVEMENT = new KeyMapping(
            "key.academy.activate_mag_movement", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_V, "key.categories.academy");

    /** Lancer l'objet tenu : la touche reste enfoncee, l'objet part au relachement. */
    public static final KeyMapping ACTIVATE_THREATENING_TELEPORT = new KeyMapping(
            "key.academy.activate_threatening_teleport", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_X, "key.categories.academy");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_SKILL);
        event.register(ACTIVATE_ARC_GEN);
        event.register(ACTIVATE_RAILGUN);
        event.register(ACTIVATE_BODY_INTENSIFY);
        event.register(ACTIVATE_SHIFT_TP);
        event.register(ACTIVATE_PENETRATE_TP);
        event.register(ACTIVATE_MELTDOWNER);
        event.register(ACTIVATE_ELECTRON_BOMB);
        event.register(ACTIVATE_LIGHT_SHIELD);
        event.register(ACTIVATE_THUNDER_BOLT);
        event.register(ACTIVATE_THUNDER_CLAP);
        event.register(ACTIVATE_CHARGING);
        event.register(ACTIVATE_MAG_MOVEMENT);
        event.register(ACTIVATE_THREATENING_TELEPORT);
    }
}
