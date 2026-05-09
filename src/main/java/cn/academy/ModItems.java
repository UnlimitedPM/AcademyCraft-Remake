package cn.academy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AcademyCraft.MOD_ID);

    // --- CRISTAUX ET MÉTAUX ---
    public static final RegistryObject<Item> CRYSTAL_LOW = ITEMS.register("crystal_low", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CRYSTAL_NORMAL = ITEMS.register("crystal_normal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CRYSTAL_PURE = ITEMS.register("crystal_pure", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RESO_CRYSTAL = ITEMS.register("reso_crystal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONSTRAINT_INGOT = ITEMS.register("constraint_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONSTRAINT_PLATE = ITEMS.register("constraint_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IMAG_SILICON_INGOT = ITEMS.register("imag_silicon_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WAFER = ITEMS.register("wafer", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IMAG_SILICON_PIECE = ITEMS.register("imag_silicon_piece", () -> new Item(new Item.Properties()));

    // --- COMPOSANTS ---
    public static final RegistryObject<Item> BRAIN_COMPONENT = ITEMS.register("brain_component", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INFO_COMPONENT = ITEMS.register("info_component", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RESONANCE_COMPONENT = ITEMS.register("resonance_component", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ENERGY_CONVERT_COMPONENT = ITEMS.register("energy_convert_component", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REINFORCED_IRON_PLATE = ITEMS.register("reinforced_iron_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CALC_CHIP = ITEMS.register("calc_chip", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DATA_CHIP = ITEMS.register("data_chip", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAGNETIC_COIL = ITEMS.register("magnetic_coil", () -> new Item(new Item.Properties()));


    // --- UNITÉS ET CORES ---[cite: 1]
    public static final RegistryObject<Item> MATTER_UNIT = ITEMS.register("matter_unit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MATTER_UNIT_PHASE = ITEMS.register("matter_unit_phase_liquid", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAT_CORE_0 = ITEMS.register("mat_core_0", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAT_CORE_1 = ITEMS.register("mat_core_1", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAT_CORE_2 = ITEMS.register("mat_core_2", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ENERGY_UNIT = ITEMS.register("energy_unit",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WINDGEN_FAN = ITEMS.register("windgen_fan", () -> new Item(new Item.Properties()));

    // --- OUTILS ET DIVERS ---[cite: 1]
    public static final RegistryObject<Item> COIN = ITEMS.register("coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILBARN = ITEMS.register("silbarn", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NEEDLE = ITEMS.register("needle", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAG_HOOK = ITEMS.register("mag_hook", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TERMINAL_INSTALLER = ITEMS.register("terminal_installer", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEVELOPER_PORTABLE = ITEMS.register("developer_portable", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TUTORIAL = ITEMS.register("tutorial", () -> new Item(new Item.Properties()));

    // --- MÉDIAS (MUSIQUES) --- [cite: 13]
    public static final RegistryObject<Item> MEDIA_RAILGUN = ITEMS.register("media_only_my_railgun",
            () -> new TooltipItem("ac.media.only_my_railgun.desc"));
    public static final RegistryObject<Item> MEDIA_JUDGELIGHT = ITEMS.register("media_level5_judgelight",
            () -> new TooltipItem("ac.media.level5_judgelight.desc"));
    public static final RegistryObject<Item> MEDIA_SISTERS_NOISE = ITEMS.register("media_sisters_noise",
            () -> new TooltipItem("ac.media.sisters_noise.desc"));

    // --- FACTEURS D'INDUCTION (Nom :  | Descriptions : ) ---
    public static final RegistryObject<Item> FACTOR_ELECTRO = ITEMS.register("factor_electromaster",
            () -> new TooltipItem("ac.ability.electromaster.name"));
    public static final RegistryObject<Item> FACTOR_MELT = ITEMS.register("factor_meltdowner",
            () -> new TooltipItem("ac.ability.meltdowner.name"));
    public static final RegistryObject<Item> FACTOR_TELE = ITEMS.register("factor_teleporter",
            () -> new TooltipItem("ac.ability.teleporter.name"));
    public static final RegistryObject<Item> FACTOR_VEC = ITEMS.register("factor_vecmanip",
            () -> new TooltipItem("ac.ability.vecmanip.name"));

    // --- LIQUIDE ET LOGO ---
    public static final RegistryObject<Item> PHASE_LIQUID_MAT = ITEMS.register("phase_liquid_mat",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LOGO = ITEMS.register("logo",
            () -> new Item(new Item.Properties()));

    // --- APPLICATIONS () ---
    public static final RegistryObject<Item> APP_SKILL_TREE = ITEMS.register("app_skill_tree", () -> new TooltipItem("ac.app.skill_tree.name"));
    public static final RegistryObject<Item> APP_MEDIA_PLAYER = ITEMS.register("app_media_player", () -> new TooltipItem("ac.app.media_player.name"));
    public static final RegistryObject<Item> APP_FREQ_TRANSMITTER = ITEMS.register("app_freq_transmitter", () -> new TooltipItem("ac.app.freq_transmitter.name"));
    public static final RegistryObject<Item> APP_SETTINGS = ITEMS.register("app_settings", () -> new TooltipItem("ac.app.settings.name"));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    // Classe générique pour les items avec un nom simple et une description au survol
    public static class TooltipItem extends Item {
        private final String tooltipKey;
        public TooltipItem(String tooltipKey) {
            super(new Item.Properties());
            this.tooltipKey = tooltipKey;
        }
        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        }
    }
}