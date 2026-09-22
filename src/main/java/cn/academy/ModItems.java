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
    public static final RegistryObject<Item> MAGNETIC_COIL = ITEMS.register("magnetic_coil", () -> new Item(new Item.Properties()));


    // Machines
    public static final RegistryObject<Item> IMAG_FUSOR = ITEMS.register("imag_fusor",
            () -> new BlockItem(ModBlocks.IMAG_FUSOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> METAL_FORMER = ITEMS.register("metal_former",
            () -> new BlockItem(ModBlocks.METAL_FORMER.get(), new Item.Properties()));
    public static final RegistryObject<Item> PHASE_GENERATOR = ITEMS.register("phase_generator",
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
            () -> new EmptyUnitItem(new Item.Properties().stacksTo(16)));

    // 2. L'unité de phase bridée à 16
    public static final RegistryObject<Item> MATTER_UNIT_PHASE = ITEMS.register("matter_unit_phase_liquid",
            () -> new PhaseUnitItem(ModBlocks.PHASE_LIQUID_BLOCK.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MAT_CORE_0 = ITEMS.register("mat_core_0", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAT_CORE_1 = ITEMS.register("mat_core_1", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAT_CORE_2 = ITEMS.register("mat_core_2", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ENERGY_UNIT = ITEMS.register("energy_unit",
            () -> new Item(new Item.Properties().stacksTo(1)));
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
    public static final RegistryObject<Item> COIN = ITEMS.register("coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILBARN = ITEMS.register("silbarn", () -> new SilbarnItem());
    public static final RegistryObject<Item> NEEDLE = ITEMS.register("needle", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAG_HOOK = ITEMS.register("mag_hook", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TERMINAL_INSTALLER = ITEMS.register("terminal_installer",
            () -> new TerminalInstallerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DEVELOPER_PORTABLE = ITEMS.register("developer_portable", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TUTORIAL = ITEMS.register("tutorial", () -> new TutorialItem());
    public static final RegistryObject<Item> DEV_NORMAL_ITEM = ITEMS.register("dev_normal",
            () -> new BlockItem(ModBlocks.DEV_NORMAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEV_ADVANCED_ITEM = ITEMS.register("developer_advanced",
            () -> new BlockItem(ModBlocks.DEV_ADVANCED.get(), new Item.Properties()));
    public static final RegistryObject<Item> CAT_ENGINE = ITEMS.register("cat_engine",
            () -> new BlockItem(ModBlocks.CAT_ENGINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ABILITY_INTERFERER = ITEMS.register("ability_interferer",
            () -> new BlockItem(ModBlocks.ABILITY_INTERFERER.get(), new Item.Properties()));

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
    // On utilise BlockItem pour faire le lien entre l'objet dans la main et le bloc au sol
    public static final RegistryObject<Item> IMAG_PHASE = ITEMS.register("imag_phase",
            () -> new BlockItem(ModBlocks.PHASE_LIQUID_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> LOGO = ITEMS.register("logo",
            () -> new Item(new Item.Properties()));

    // --- APPLICATIONS () ---
    public static final RegistryObject<Item> APP_SKILL_TREE = ITEMS.register("app_skill_tree",
            () -> new AppInstallerItem("skill_tree"));
    public static final RegistryObject<Item> APP_MEDIA_PLAYER = ITEMS.register("app_media_player", () -> new TooltipItem("ac.app.media_player.name"));
    public static final RegistryObject<Item> APP_FREQ_TRANSMITTER = ITEMS.register("app_freq_transmitter", () -> new TooltipItem("ac.app.freq_transmitter.name"));
    public static final RegistryObject<Item> APP_SETTINGS = ITEMS.register("app_settings", () -> new TooltipItem("ac.app.settings.name"));

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

    // --- CLASSE : L'UNITÉ VIDE (Capture corrigée) ---
    public static class EmptyUnitItem extends Item {
        public EmptyUnitItem(Properties props) { super(props); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack itemstack = player.getItemInHand(hand);

            // On cherche le liquide sous le regard du joueur
            net.minecraft.world.phys.BlockHitResult hitResult = getPlayerPOVHitResult(world, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);

            if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos pos = hitResult.getBlockPos();

                // Si le fluide visé est notre liquide de phase
                if (world.getFluidState(pos).getFluidType() == ModFluids.PHASE_LIQUID_TYPE.get()) {
                    if (!world.isClientSide) {
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        itemstack.shrink(1);

                        ItemStack filled = new ItemStack(ModItems.MATTER_UNIT_PHASE.get());
                        if (!player.getInventory().add(filled)) {
                            player.drop(filled, false);
                        }
                    }
                    return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
                }
            }
            return InteractionResultHolder.pass(itemstack);
        }
    }

    // --- CLASSE : L'UNITÉ DE PHASE (Pose + Recyclage) ---
    public static class PhaseUnitItem extends BlockItem {
        public PhaseUnitItem(net.minecraft.world.level.block.Block block, Properties props) {
            super(block, props);
        }

        // Force l'item à utiliser "item.academy.matter_unit_phase_liquid"
        @Override
        public String getDescriptionId() {
            return "item.academy.matter_unit_phase_liquid";
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            // On enregistre l'item actuel pour savoir s'il faut le remplacer
            ItemStack itemstack = context.getItemInHand();
            InteractionResult result = super.useOn(context);

            // Si la pose a réussi
            if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
                Player player = context.getPlayer();
                if (player != null && !player.getAbilities().instabuild) {
                    // On donne l'unité vide
                    ItemStack emptyUnit = new ItemStack(ModItems.MATTER_UNIT.get());
                    if (!player.getInventory().add(emptyUnit)) {
                        player.drop(emptyUnit, false);
                    }
                }
            }
            return result;
        }
    }

    public static final RegistryObject<Item> DEBUG_CHARGER = ITEMS.register("debug_charger",
            () -> new Item(new Item.Properties()) {
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack energyUnit = player.getOffhandItem(); // Energy Unit en main gauche
                    if (energyUnit.getItem() == ModItems.ENERGY_UNIT.get()) {
                        EnergyUnit.charge(energyUnit, EnergyUnit.MAX_ENERGY * 0.5f);
                        return InteractionResultHolder.success(player.getItemInHand(hand));
                    }
                    return InteractionResultHolder.pass(player.getItemInHand(hand));
                }
            });

    /**
     * Etat de l'unite d'energie, porte par le NBT {@code ac_energy}.
     *
     * L'original (1.12.2) utilisait {@code ItemEnergyBase(10000, 20)} avec des
     * degats d'objet pour l'icone. En 1.20.1 on stocke l'energie en NBT et on
     * expose l'etat via la propriete d'item {@code academy:energy}
     * (voir {@link ModItemProperties}).
     */
    public static final class EnergyUnit {
        private EnergyUnit() {}

        public static final float MAX_ENERGY = 10000f;
        public static final float BANDWIDTH = 20f;
        private static final String KEY = "ac_energy";

        public static float getEnergy(ItemStack stack) {
            return stack.getTag() != null ? stack.getTag().getFloat(KEY) : 0f;
        }

        /** Ajoute de l'energie sans depasser {@link #MAX_ENERGY}. Retourne le nouveau niveau. */
        public static float charge(ItemStack stack, float amount) {
            float updated = Math.min(getEnergy(stack) + amount, MAX_ENERGY);
            stack.getOrCreateTag().putFloat(KEY, updated);
            return updated;
        }

        /** Retire de l'energie. Retourne la quantite reellement retiree. */
        public static float discharge(ItemStack stack, float amount) {
            float available = getEnergy(stack);
            float taken = Math.min(available, amount);
            stack.getOrCreateTag().putFloat(KEY, available - taken);
            return taken;
        }

        public static boolean isFull(ItemStack stack) {
            return getEnergy(stack) >= MAX_ENERGY;
        }
    }

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

    /**
     * L'objet qui installe le terminal de donnees.
     *
     * Portage de {@code ItemTerminalInstaller}. Comme dans l'original, il est
     * consomme sauf en creatif, refuse de s'appliquer deux fois, et confirme par un
     * message dans le chat.
     *
     * L'original ajoutait aussi le nom de la touche a ce message, en la lisant
     * depuis le gestionnaire de touches. Le serveur ne peut pas connaitre la touche
     * du client : le message dit donc « Alt par defaut » plutot que d'inventer une
     * touche que le joueur a peut-etre changee.
     */
    public static class TerminalInstallerItem extends Item {
        public TerminalInstallerItem(Properties props) { super(props); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);

            var data = player.getCapability(cn.academy.terminal.TerminalCapability.TERMINAL_DATA).orElse(null);
            if (data == null) return InteractionResultHolder.pass(stack);

            if (data.isTerminalInstalled()) {
                player.displayClientMessage(Component.translatable("ac.terminal.alrdy_installed"), false);
                return InteractionResultHolder.success(stack);
            }

            if (!player.getAbilities().instabuild) stack.shrink(1);
            data.install();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                cn.academy.terminal.TerminalEvents.sync(serverPlayer);
            }
            player.displayClientMessage(Component.translatable("ac.terminal.key_hint"), false);
            return InteractionResultHolder.success(stack);
        }
    }

    /**
     * L'objet MisakaCloud : les tutoriels du mod, ouverts directement.
     *
     * Portage de {@code ItemTutorial}. L'original l'attachait a l'application du meme
     * nom, mais l'ouvrait sans passer par le terminal — et c'est ce que fait le port :
     * l'application est installee d'office, donc l'objet ne sert qu'a l'ouvrir. L'ecran
     * est atteint par {@code App.requestOpen}, qui ne nomme aucun type client.
     */
    public static class TutorialItem extends Item {

        public TutorialItem() {
            super(new Item.Properties());
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                cn.academy.terminal.App.requestOpen(cn.academy.terminal.app.AppTutorial.INSTANCE);
            }
            return InteractionResultHolder.success(stack);
        }
    }

    /**
     * L'objet qui installe une application du terminal.
     *
     * Portage de {@code ItemApp}. Le nom de l'application est connu des la
     * construction de l'objet, mais l'application elle-meme ne l'est pas : les
     * registres ne sont pas remplis au chargement des classes. La recherche se fait
     * donc a l'usage, et une application absente du registre — parce que son
     * contenu n'est pas encore porte — se contente de le dire au lieu de planter.
     */
    public static class AppInstallerItem extends Item {
        private final String appName;

        public AppInstallerItem(String appName) {
            super(new Item.Properties());
            this.appName = appName;
        }

        private cn.academy.terminal.App app() {
            return cn.academy.terminal.AppRegistry.INSTANCE.getByName(appName);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);

            var app = app();
            if (app == null) {
                player.displayClientMessage(Component.translatable("ac.terminal.app_missing",
                        Component.translatable("ac.app." + appName + ".name")), false);
                return InteractionResultHolder.success(stack);
            }

            var data = player.getCapability(cn.academy.terminal.TerminalCapability.TERMINAL_DATA).orElse(null);
            if (data == null) return InteractionResultHolder.pass(stack);

            if (!data.isTerminalInstalled()) {
                player.displayClientMessage(Component.translatable("ac.terminal.notinstalled"), false);
                return InteractionResultHolder.success(stack);
            }
            if (data.isInstalled(app)) {
                player.displayClientMessage(Component.translatable("ac.terminal.app_alrdy_installed",
                        app.getDisplayName()), false);
                return InteractionResultHolder.success(stack);
            }

            if (!player.getAbilities().instabuild) stack.shrink(1);
            data.installApp(app);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                cn.academy.terminal.TerminalEvents.sync(serverPlayer);
            }
            player.displayClientMessage(Component.translatable("ac.terminal.app_installed",
                    app.getDisplayName()), false);
            return InteractionResultHolder.success(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("ac.app." + appName + ".name").withStyle(ChatFormatting.GRAY));
        }
    }
}