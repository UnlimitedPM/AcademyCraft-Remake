package cn.academy.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
    @FunctionalInterface
    public interface NodeLinker {
        boolean ensureLinked(ServerLevel level, BlockPos pos);
    }

    private static ExternalEnergyProbe externalEnergyProbe = stack -> false;

    private static MenuOpener menuOpener = (player, provider, writer) -> player.openMenu(provider);

    private static NodeLinker nodeLinker = (level, pos) -> false;

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

    public static void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> writer) {
        menuOpener.open(player, provider, writer);
    }
}
