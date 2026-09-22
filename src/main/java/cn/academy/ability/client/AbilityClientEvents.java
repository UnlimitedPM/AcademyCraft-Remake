package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.ActivateSkillPacket;
import cn.academy.ability.network.ActivateSkillPacket.Phase;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class AbilityClientEvents {

    /**
     * Une touche, la competence qu'elle declenche, et sa charge eventuelle.
     *
     * <p>Regroupee en tableau : huit blocs recopies a la main auraient fait huit
     * occasions d'oublier le relachement d'une competence qui se charge.
     */
    private static final class Binding {

        final KeyMapping key;
        final String category;
        final String skill;

        /** Vrai entre l'appui et le relachement d'une competence qui se charge. */
        boolean charging;

        Binding(KeyMapping key, String category, String skill) {
            this.key = key;
            this.category = category;
            this.skill = skill;
        }
    }

    // Cablage en attendant le systeme de presets et de touches de l'original.
    private static final List<Binding> BINDINGS = List.of(
            new Binding(AbilityKeyBindings.ACTIVATE_SKILL, VecmanipCategory.NAME, "vec_accel"),
            new Binding(AbilityKeyBindings.ACTIVATE_ARC_GEN, ElectromasterCategory.NAME, "arc_gen"),
            new Binding(AbilityKeyBindings.ACTIVATE_RAILGUN, ElectromasterCategory.NAME, "railgun"),
            new Binding(AbilityKeyBindings.ACTIVATE_BODY_INTENSIFY, ElectromasterCategory.NAME, "body_intensify"),
            new Binding(AbilityKeyBindings.ACTIVATE_SHIFT_TP, TeleporterCategory.NAME, "shift_tp"),
            new Binding(AbilityKeyBindings.ACTIVATE_PENETRATE_TP, TeleporterCategory.NAME, "penetrate_teleport"),
            new Binding(AbilityKeyBindings.ACTIVATE_MELTDOWNER, MeltdownerCategory.NAME, "meltdowner"),
            new Binding(AbilityKeyBindings.ACTIVATE_ELECTRON_BOMB, MeltdownerCategory.NAME, "electron_bomb"),
            new Binding(AbilityKeyBindings.ACTIVATE_LIGHT_SHIELD, MeltdownerCategory.NAME, "light_shield"),
            new Binding(AbilityKeyBindings.ACTIVATE_THUNDER_BOLT, ElectromasterCategory.NAME, "thunder_bolt"),
            new Binding(AbilityKeyBindings.ACTIVATE_THUNDER_CLAP, ElectromasterCategory.NAME, "thunder_clap"),
            new Binding(AbilityKeyBindings.ACTIVATE_CHARGING, ElectromasterCategory.NAME, "charging"),
            new Binding(AbilityKeyBindings.ACTIVATE_MAG_MOVEMENT, ElectromasterCategory.NAME, "mag_movement"),
            new Binding(AbilityKeyBindings.ACTIVATE_THREATENING_TELEPORT, TeleporterCategory.NAME,
                    "threatening_teleport"),
            new Binding(AbilityKeyBindings.ACTIVATE_MARK_TELEPORT, TeleporterCategory.NAME, "mark_teleport"),
            new Binding(AbilityKeyBindings.ACTIVATE_FLESH_RIPPING, TeleporterCategory.NAME, "flesh_ripping"),
            new Binding(AbilityKeyBindings.ACTIVATE_MINE_RAY_BASIC, MeltdownerCategory.NAME, "mine_ray_basic"),
            new Binding(AbilityKeyBindings.ACTIVATE_MINE_RAY_EXPERT, MeltdownerCategory.NAME, "mine_ray_expert"),
            new Binding(AbilityKeyBindings.ACTIVATE_MINE_RAY_LUCK, MeltdownerCategory.NAME, "mine_ray_luck"),
            new Binding(AbilityKeyBindings.ACTIVATE_SCATTER_BOMB, MeltdownerCategory.NAME, "scatter_bomb"));

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Binding binding : BINDINGS) {
            tick(binding);
        }
    }

    private static void tick(Binding binding) {
        Category category = CategoryManager.INSTANCE.getCategory(binding.category);
        if (category == null) return;
        Skill skill = category.getSkill(binding.skill);
        if (skill == null) return;

        // Appui : l'original envoyait MSG_KEYDOWN. Une competence qui se charge ouvre
        // son compteur, une competence tenue vit a partir de maintenant, une autre part
        // tout de suite.
        if (binding.key.consumeClick()) {
            binding.charging = skill.isChargeable() || skill.isHeld();
            send(category, skill, Phase.PRESS);
            if (skill.isChargeable()) {
                ClientCharge.begin(skill.getMaxChargeTicks(ClientAbilityData.get()));
            } else if (skill.isHeld()) {
                ClientCharge.beginSustained();
            }
        }

        if (!binding.charging) return;
        if (binding.key.isDown()) {
            ClientCharge.tick();
            return;
        }

        // Relachement : l'original envoyait MSG_KEYUP et le serveur executait la
        // competence avec le temps qu'il avait compte de son cote.
        binding.charging = false;
        ClientCharge.end();
        send(category, skill, Phase.RELEASE);
    }

    private static void send(Category category, Skill skill, Phase phase) {
        AbilityNetwork.CHANNEL.sendToServer(
                new ActivateSkillPacket(category.getCategoryId(), skill.getId(), phase));
    }
}
