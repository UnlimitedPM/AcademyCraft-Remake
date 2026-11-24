package cn.academy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;


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
    // Elle surcharge le developeur pour reecrire la realite personnelle : l'original le
    // disait sous son nom, et le port le dit pareil (voir TooltipItem).
    public static final RegistryObject<Item> MAGNETIC_COIL = ITEMS.register("magnetic_coil",
            () -> new TooltipItem("item.academy.magnetic_coil.desc"));


    // Machines
    public static final RegistryObject<Item> IMAG_FUSOR = ITEMS.register("imag_fusor",
            () -> new BlockItem(ModBlocks.IMAG_FUSOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> METAL_FORMER = ITEMS.register("metal_former",
            () -> new BlockItem(ModBlocks.METAL_FORMER.get(), new Item.Properties()));
    public static final RegistryObject<Item> PHASE_GENERATOR = ITEMS.register("phase_gen",
            () -> new BlockItem(ModBlocks.PHASE_GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> MATRIX = ITEMS.register("matrix",
            () -> new BlockItem(ModBlocks.MATRIX.get(), new Item.Properties()));
    public static final RegistryObject<Item> NODE_BASIC = ITEMS.register("node_basic",
            () -> new BlockItem(ModBlocks.NODE_BASIC.get(), new Item.Properties()));
    public static final RegistryObject<Item> NODE_STANDARD = ITEMS.register("node_standard",
            () -> new BlockItem(ModBlocks.NODE_STANDARD.get(), new Item.Properties()));
    public static final RegistryObject<Item> NODE_ADVANCED = ITEMS.register("node_advanced",
            () -> new BlockItem(ModBlocks.NODE_ADVANCED.get(), new Item.Properties()));

    // --- UNITÉS ET CORES ---
    // UNE SEULE FOIS MATTER_UNIT ICI
    // 1. L'unité vide bridée à 16
    public static final RegistryObject<Item> MATTER_UNIT = ITEMS.register("matter_unit",
            () -> new cn.academy.EmptyUnitItem(new Item.Properties().stacksTo(16)));

    // 2. L'unité de phase bridée à 16
    public static final RegistryObject<Item> MATTER_UNIT_PHASE = ITEMS.register("matter_unit_phase_liquid",
            () -> new cn.academy.PhaseUnitItem(ModBlocks.PHASE_LIQUID_BLOCK.get(),
                    new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MAT_CORE_0 = ITEMS.register("mat_core_0", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAT_CORE_1 = ITEMS.register("mat_core_1", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAT_CORE_2 = ITEMS.register("mat_core_2", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ENERGY_UNIT = ITEMS.register("energy_unit", EnergyUnitItem::new);
    public static final RegistryObject<Item> WINDGEN_FAN = ITEMS.register("windgen_fan",
            // L'original : setMaxStackSize(1) + setMaxDamage(100). Rien ne l'use
            // en jeu, mais il n'est pas empilable et c'est ce qui compte pour
            // l'emplacement du rotor. La durabilite est conservee pour ne pas
            // s'ecarter de l'original plus que necessaire.
            () -> new Item(new Item.Properties().stacksTo(1).durability(100)));

    public static final RegistryObject<Item> WINDGEN_BASE = ITEMS.register("windgen_base",
            () -> new BlockItem(ModBlocks.WINDGEN_BASE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WINDGEN_MAIN = ITEMS.register("windgen_main",
            () -> new BlockItem(ModBlocks.WINDGEN_MAIN.get(), new Item.Properties()));

    // --- OUTILS ET DIVERS ---[cite: 1]
    public static final RegistryObject<Item> COIN = ITEMS.register("coin", ForgeCoinItem::new);
    public static final RegistryObject<Item> SILBARN = ITEMS.register("silbarn", () -> new cn.academy.SilbarnItem());
    public static final RegistryObject<Item> NEEDLE = ITEMS.register("needle", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAG_HOOK = ITEMS.register("mag_hook", cn.academy.MagHookItem::new);
    public static final RegistryObject<Item> TERMINAL_INSTALLER = ITEMS.register("terminal_installer",
            () -> new cn.academy.TerminalInstallerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DEVELOPER_PORTABLE = ITEMS.register("developer_portable",
            ForgeDeveloperPortableItem::new);
    public static final RegistryObject<Item> TUTORIAL = ITEMS.register("tutorial", cn.academy.TutorialItem::new);
    public static final RegistryObject<Item> DEV_NORMAL_ITEM = ITEMS.register("dev_normal",
            () -> new BlockItem(ModBlocks.DEV_NORMAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEV_ADVANCED_ITEM = ITEMS.register("dev_advanced",
            () -> new BlockItem(ModBlocks.DEV_ADVANCED.get(), new Item.Properties()));
    public static final RegistryObject<Item> CAT_ENGINE = ITEMS.register("cat_engine",
            () -> new BlockItem(ModBlocks.CAT_ENGINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ABILITY_INTERFERER = ITEMS.register("ability_interferer",
            () -> new BlockItem(ModBlocks.ABILITY_INTERFERER.get(), new Item.Properties()));

    // --- MÉDIAS (MUSIQUES) ---
    // Chacun donne son morceau au joueur, comme l'objet MediaItem de l'original : il faut
    // avoir installe l'application, et le morceau ne s'obtient qu'une fois.
    public static final RegistryObject<Item> MEDIA_RAILGUN = ITEMS.register("media_only_my_railgun",
            () -> new MediaItem("only_my_railgun"));
    public static final RegistryObject<Item> MEDIA_JUDGELIGHT = ITEMS.register("media_level5_judgelight",
            () -> new MediaItem("level5_judgelight"));
    public static final RegistryObject<Item> MEDIA_SISTERS_NOISE = ITEMS.register("media_sisters_noise",
            () -> new MediaItem("sisters_noise"));

    // --- FACTEURS D'INDUCTION (Nom :  | Descriptions : ) ---
    // L'original n'en avait qu'un, dont la categorie vivait dans les metadonnees. La
    // 1.20.1 n'en a plus : d'ou quatre objets, dont la classe retient la categorie.
    public static final RegistryObject<Item> FACTOR_ELECTRO = ITEMS.register("factor_electromaster",
            () -> new FactorItem("electromaster"));
    public static final RegistryObject<Item> FACTOR_MELT = ITEMS.register("factor_meltdowner",
            () -> new FactorItem("meltdowner"));
    public static final RegistryObject<Item> FACTOR_TELE = ITEMS.register("factor_teleporter",
            () -> new FactorItem("teleporter"));
    public static final RegistryObject<Item> FACTOR_VEC = ITEMS.register("factor_vecmanip",
            () -> new FactorItem("vecmanip"));

    // --- LIQUIDE ET LOGO ---
    // On utilise BlockItem pour faire le lien entre l'objet dans la main et le bloc au sol
    public static final RegistryObject<Item> IMAG_PHASE = ITEMS.register("imag_phase",
            () -> new BlockItem(ModBlocks.PHASE_LIQUID_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> LOGO = ITEMS.register("logo",
            () -> new Item(new Item.Properties()));

    // --- APPLICATIONS () ---
    public static final RegistryObject<Item> APP_SKILL_TREE = ITEMS.register("app_skill_tree",
            () -> new AppInstallerItem("skill_tree"));
    public static final RegistryObject<Item> APP_MEDIA_PLAYER = ITEMS.register("app_media_player",
            () -> new AppInstallerItem("media_player"));
    public static final RegistryObject<Item> APP_FREQ_TRANSMITTER = ITEMS.register("app_freq_transmitter",
            () -> new AppInstallerItem("freq_transmitter"));
    public static final RegistryObject<Item> APP_SETTINGS = ITEMS.register("app_settings",
            () -> new AppInstallerItem("settings"));

    /**
     * La bille de silicium : l'objet qu'on lance, portage de {@code ItemSilbarn}.
     *
     * Un objet qui ne sert a rien tout seul, et qui n'existe que pour la salve de rayons :
     * on le jette ou l'on veut que la salve explose. Il se consomme au lancer, comme
     * l'original, sauf en creatif.
     *
     * Le son est celui d'un oeuf lance. L'original le jouait en excluant le lanceur
     * ({@code world.playSound(player, ...)}) : le port le fait entendre a tout le monde,
     * celui qui lance compris — une main qui ne s'entend pas lancer est une main qui fait
     * douter du clic.
     */
    public static class SilbarnItem extends Item {
        public SilbarnItem() {
            super(new Item.Properties());
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.EGG_THROW, net.minecraft.sounds.SoundSource.PLAYERS,
                    0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

            if (!level.isClientSide) {
                level.addFreshEntity(new cn.academy.entity.EntitySilbarn(level, player));
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }
    }


    // --- CLASSE : L'UNITÉ DE PHASE ---
    // Elle vit maintenant dans common/ (cn.academy.PhaseUnitItem) : les deux chargeurs la posent,
    // seul le bloc de liquide change de provenance.


    /**
     * Ancien point d'entree de l'unite d'energie, garde pour les appelants
     * existants.
     *
     * <p>La regle vit maintenant dans {@link cn.academy.energy.EnergyItems}, une
     * seule fois pour l'unite ET pour le developpeur portable. Ce bloc ne fait
     * que deleguer, avec les types {@code float} que le port exposait avant.
     */
    public static final class EnergyUnit {
        private EnergyUnit() {}

        public static final float MAX_ENERGY = (float) EnergyUnitItem.MAX_ENERGY;
        public static final float BANDWIDTH = (float) EnergyUnitItem.BANDWIDTH;

        public static float getEnergy(ItemStack stack) {
            return (float) cn.academy.energy.EnergyItems.getEnergy(stack);
        }

        /** Ajoute de l'energie sans depasser {@link #MAX_ENERGY}. Retourne le nouveau niveau. */
        public static float charge(ItemStack stack, float amount) {
            return (float) cn.academy.energy.EnergyItems.charge(stack, amount);
        }

        /** Retire de l'energie. Retourne la quantite reellement retiree. */
        public static float discharge(ItemStack stack, float amount) {
            return (float) cn.academy.energy.EnergyItems.discharge(stack, amount);
        }

        public static boolean isFull(ItemStack stack) {
            return cn.academy.energy.EnergyItems.isFull(stack);
        }
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}