package cn.academy.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import cn.academy.platform.network.Network;

import java.util.function.Consumer;

/**
 * Petits crochets de plateforme, installes par chaque module de chargeur.
 *
 * <p>Le code de {@code common/} ne doit jamais importer une API de chargeur. Quand une regle
 * commune a besoin d'une information que seul le chargeur connait, elle passe par ici. Le defaut
 * est neutre : sans installation, aucun objet exterieur n'est reconnu.
 */
public final class PlatformHooks {

    /** Dit si une pile porte un stockage d'energie fourni par le chargeur (Forge Energy, etc.). */
    @FunctionalInterface
    public interface ExternalEnergyProbe {
        boolean isExternalEnergyItem(ItemStack stack);
    }

    /** Ouvre un menu avec les donnees d'ouverture propres au menu (position du bloc, etc.). */
    @FunctionalInterface
    public interface MenuOpener {
        void open(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> writer);
    }

    /** Echanges d'energie avec les objets et les blocs fournis par le chargeur. */
    public interface ExternalEnergyIO {
        int chargeItem(ItemStack stack, int amount, boolean simulate);

        int extractItem(ItemStack stack, int amount, boolean simulate);

        int pushToNeighbor(Level level, BlockPos pos, Direction side, int amount, boolean simulate);
    }

    /** Raccordement d'une machine a un noeud sans fil (le noeud lui-meme est cote chargeur). */
    public interface NodeLinker {
        boolean ensureLinked(ServerLevel level, BlockPos pos);

        default void detach(ServerLevel level, BlockPos pos) {
        }
    }

    /** Ce qu'un ecran de Matrix demande au serveur. */
    public enum MatrixConfigAction {
        INIT, SSID, PASSWORD
    }

    /** Envoi d'une demande de configuration du Matrix au serveur. */
    @FunctionalInterface
    public interface MatrixConfigSender {
        void send(BlockPos pos, MatrixConfigAction action, String ssid, String password);
    }

    /** Envoi d'un nouveau nom ou mot de passe de noeud au serveur. */
    @FunctionalInterface
    public interface NodeConfigSender {
        void send(BlockPos pos, boolean password, String value);
    }

    /** Ce que l'onglet sans fil demande au serveur. */
    public enum WirelessAction {
        FIND, CONNECT, DISCONNECT
    }

    /** Envoi d'une demande de l'onglet sans fil (recherche, raccordement, detachement). */
    @FunctionalInterface
    public interface WirelessRequestSender {
        void send(WirelessAction action, BlockPos node, String password);
    }

    /** Les eclats d'une bille de silicium qui vient de se poser (rendu client). */
    @FunctionalInterface
    public interface SilbarnFragments {
        void spawn(net.minecraft.world.phys.Vec3 position);
    }

    /**
     * Donnees d'aptitude portees par un joueur.
     *
     * <p>Le stockage appartient au chargeur (capacite attachee au joueur, cote Forge). Le code
     * commun ne demande jamais ou les donnees vivent, il demande seulement celles du joueur.
     * {@code get} rend {@code null} quand le joueur n'en porte pas : c'est le cas d'un joueur
     * qui n'a jamais touche au systeme, et cela doit rester sans danger.
     */
    public interface AbilityDataAccess {
        cn.academy.ability.AbilityData get(net.minecraft.world.entity.player.Player player);

        void set(net.minecraft.world.entity.player.Player player, cn.academy.ability.AbilityData data);
    }

    /**
     * Ce qui doit suivre un changement de categorie.
     *
     * <p>Cote Forge, c'est le prereglage : les competences de la categorie quittee ne sont plus
     * apprises, donc les maintiens qu'elles portaient s'eteignent. Le code commun ne sait pas
     * ou vit le prereglage, il annonce seulement le changement.
     */
    @FunctionalInterface
    public interface CategoryChangeHook {
        void afterCategoryChange(net.minecraft.world.entity.player.Player player);
    }

    /** Les succes du mod, que seule la logique des aptitudes declenche. */
    public enum Advancement {
        OVERLOAD, LEARNING_SKILL, EXP_FULL, CONVERT_CATEGORY
    }

    /** Attribution des succes (les criteres sont propres au chargeur). */
    public interface AdvancementHooks {
        void award(net.minecraft.world.entity.player.Player player, Advancement advancement);

        void onCategoryLevel(net.minecraft.world.entity.player.Player player, int level);
    }

    private static NodeLinker nodeLinker = new NodeLinker() {
        @Override
        public boolean ensureLinked(ServerLevel level, BlockPos pos) {
            return false;
        }
    };

    private static MatrixConfigSender matrixConfigSender = (pos, action, ssid, password) ->
            Network.sendToServer(new cn.academy.energy.network.MatrixConfigPacket(pos,
                    cn.academy.energy.network.MatrixConfigPacket.Action.valueOf(action.name()),
                    ssid, password));

    private static NodeConfigSender nodeConfigSender = (pos, password, value) ->
            Network.sendToServer(new cn.academy.energy.network.NodeConfigPacket(pos, password, value));

    private static WirelessRequestSender wirelessRequestSender = (action, node, password) -> {        cn.academy.energy.network.WirelessRequestPacket packet = switch (action) {
            case FIND -> cn.academy.energy.network.WirelessRequestPacket.find();
            case DISCONNECT -> cn.academy.energy.network.WirelessRequestPacket.disconnect();
            case CONNECT -> cn.academy.energy.network.WirelessRequestPacket.connect(node, password);
        };
        Network.sendToServer(packet);
    };

    private static SilbarnFragments silbarnFragments = position -> {
    };

    private static AdvancementHooks advancementHooks = new AdvancementHooks() {
        @Override
        public void award(net.minecraft.world.entity.player.Player player, Advancement advancement) {
        }

        @Override
        public void onCategoryLevel(net.minecraft.world.entity.player.Player player, int level) {
        }
    };

    private static AbilityDataAccess abilityDataAccess = new AbilityDataAccess() {
        @Override
        public cn.academy.ability.AbilityData get(net.minecraft.world.entity.player.Player player) {
            return null;
        }

        @Override
        public void set(net.minecraft.world.entity.player.Player player, cn.academy.ability.AbilityData data) {
        }
    };

    private static ExternalEnergyProbe externalEnergyProbe = stack -> false;
    private static MenuOpener menuOpener = (player, provider, writer) -> player.openMenu(provider);

    private static CategoryChangeHook categoryChangeHook = player -> {
    };

    private static ExternalEnergyIO externalEnergyIO = new ExternalEnergyIO() {
        @Override
        public int chargeItem(ItemStack stack, int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int extractItem(ItemStack stack, int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int pushToNeighbor(Level level, BlockPos pos, Direction side, int amount, boolean simulate) {
            return 0;
        }
    };

    private PlatformHooks() {
    }

    public static void installExternalEnergyProbe(ExternalEnergyProbe probe) {
        externalEnergyProbe = probe;
    }

    public static void installMenuOpener(MenuOpener opener) {
        menuOpener = opener;
    }

    public static void installExternalEnergyIO(ExternalEnergyIO io) {
        externalEnergyIO = io;
    }

    public static void installNodeLinker(NodeLinker linker) {
        nodeLinker = linker;
    }

    public static void installMatrixConfigSender(MatrixConfigSender sender) {
        matrixConfigSender = sender;
    }

    public static void installNodeConfigSender(NodeConfigSender sender) {
        nodeConfigSender = sender;
    }

    public static void installWirelessRequestSender(WirelessRequestSender sender) {
        wirelessRequestSender = sender;
    }

    public static void installSilbarnFragments(SilbarnFragments fragments) {
        silbarnFragments = fragments;
    }

    public static void installAdvancementHooks(AdvancementHooks hooks) {
        advancementHooks = hooks;
    }

    public static void installAbilityData(AbilityDataAccess access) {
        abilityDataAccess = access;
    }

    public static void installCategoryChangeHook(CategoryChangeHook hook) {
        categoryChangeHook = hook;
    }

    /** A appeler une fois la categorie du joueur changee. */
    public static void categoryChanged(net.minecraft.world.entity.player.Player player) {
        categoryChangeHook.afterCategoryChange(player);
    }

    /** Donnees d'aptitude du joueur, ou {@code null} s'il n'en porte pas. */
    public static cn.academy.ability.AbilityData abilityData(net.minecraft.world.entity.player.Player player) {
        return player == null ? null : abilityDataAccess.get(player);
    }

    public static void setAbilityData(net.minecraft.world.entity.player.Player player,
                                      cn.academy.ability.AbilityData data) {
        abilityDataAccess.set(player, data);
    }

    public static void awardAdvancement(net.minecraft.world.entity.player.Player player, Advancement advancement) {
        advancementHooks.award(player, advancement);
    }

    public static void categoryLevelReached(net.minecraft.world.entity.player.Player player, int level) {
        advancementHooks.onCategoryLevel(player, level);
    }

    public static boolean hasExternalEnergy(ItemStack stack) {
        return !stack.isEmpty() && externalEnergyProbe.isExternalEnergyItem(stack);
    }

    public static int chargeExternalItem(ItemStack stack, int amount, boolean simulate) {
        return externalEnergyIO.chargeItem(stack, amount, simulate);
    }

    public static int extractExternalItem(ItemStack stack, int amount, boolean simulate) {
        return externalEnergyIO.extractItem(stack, amount, simulate);
    }

    public static int pushEnergyToNeighbor(Level level, BlockPos pos, Direction side, int amount, boolean simulate) {
        return externalEnergyIO.pushToNeighbor(level, pos, side, amount, simulate);
    }

    public static boolean ensureNodeLinked(ServerLevel level, BlockPos pos) {
        return nodeLinker.ensureLinked(level, pos);
    }

    public static void detachNode(ServerLevel level, BlockPos pos) {
        nodeLinker.detach(level, pos);
    }

    public static void sendMatrixConfig(BlockPos pos, MatrixConfigAction action, String ssid, String password) {
        matrixConfigSender.send(pos, action, ssid, password);
    }

    public static void sendNodeConfig(BlockPos pos, boolean password, String value) {
        nodeConfigSender.send(pos, password, value);
    }

    public static void sendWirelessRequest(WirelessAction action, BlockPos node, String password) {
        wirelessRequestSender.send(action, node, password);
    }

    public static void spawnSilbarnFragments(net.minecraft.world.phys.Vec3 position) {
        silbarnFragments.spawn(position);
    }

    public static void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> writer) {
        menuOpener.open(player, provider, writer);
    }
}
