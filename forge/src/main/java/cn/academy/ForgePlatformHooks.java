package cn.academy;

import cn.academy.platform.PlatformHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

/**
 * Installation des crochets Forge/NeoForge pour le code de common/.
 *
 * <p>NeoForge 1.20.1 garde les packages net.minecraftforge : cette classe compile et fonctionne
 * sur les deux chargeurs.
 */
public final class ForgePlatformHooks {

    private ForgePlatformHooks() {
    }

    public static void install() {
        PlatformHooks.installExternalEnergyProbe(stack -> !stack.isEmpty()
                && stack.getCapability(ForgeCapabilities.ENERGY).isPresent());
        PlatformHooks.installMenuOpener((player, provider, writer) ->
                NetworkHooks.openScreen(player, provider, writer));
        PlatformHooks.installNodeLinker(new PlatformHooks.NodeLinker() {
            @Override
            public boolean ensureLinked(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
                return cn.academy.energy.NodeFinder.ensureLinked(level, pos);
            }

            @Override
            public void detach(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
                cn.academy.energy.NodeFinder.detach(level, pos);
            }
        });
        cn.academy.platform.network.Network.install(
                new cn.academy.platform.network.ForgeNetworkTransport());
        cn.academy.platform.network.AcademyPackets.register();
        PlatformHooks.installAbilityData(new PlatformHooks.AbilityDataAccess() {
            @Override
            public cn.academy.ability.AbilityData get(net.minecraft.world.entity.player.Player player) {
                return player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                        .resolve().orElse(null);
            }

            @Override
            public void set(net.minecraft.world.entity.player.Player player,
                            cn.academy.ability.AbilityData data) {
                player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA).ifPresent(current -> {
                    // La capacite tient une instance fixe : on la remplit au lieu de la remplacer,
                    // sinon tous les LazyOptional deja distribues pointeraient sur du vide.
                    if (current != data) current.copyFrom(data);
                });
            }
        });
        PlatformHooks.installCategoryChangeHook(
                cn.academy.ability.preset.PresetTracker::clearOnCategoryChange);
        PlatformHooks.installAdvancementHooks(new PlatformHooks.AdvancementHooks() {
            @Override
            public void award(net.minecraft.world.entity.player.Player player,
                              PlatformHooks.Advancement advancement) {
                if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return;
                String name = switch (advancement) {
                    case OVERLOAD -> cn.academy.advancements.AcademyAdvancements.AC_OVERLOAD;
                    case LEARNING_SKILL -> cn.academy.advancements.AcademyAdvancements.AC_LEARNING_SKILL;
                    case EXP_FULL -> cn.academy.advancements.AcademyAdvancements.AC_EXP_FULL;
                    case CONVERT_CATEGORY -> cn.academy.advancements.AcademyAdvancements.CONVERT_CATEGORY;
                };
                cn.academy.advancements.AcademyAdvancements.award(server, name);
            }

            @Override
            public void onCategoryLevel(net.minecraft.world.entity.player.Player player, int level) {
                if (player instanceof net.minecraft.server.level.ServerPlayer server) {
                    cn.academy.advancements.AcademyAdvancements.onCategoryLevel(server, level);
                }
            }
        });
        PlatformHooks.installExternalEnergyIO(new PlatformHooks.ExternalEnergyIO() {
            @Override
            public int chargeItem(ItemStack stack, int amount, boolean simulate) {
                return stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> storage.receiveEnergy(amount, simulate))
                        .orElse(0);
            }

            @Override
            public int extractItem(ItemStack stack, int amount, boolean simulate) {
                return stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> storage.extractEnergy(amount, simulate))
                        .orElse(0);
            }

            @Override
            public int pushToNeighbor(Level level, BlockPos pos, Direction side, int amount, boolean simulate) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
                if (neighbor == null) return 0;
                return neighbor.getCapability(ForgeCapabilities.ENERGY, side.getOpposite())
                        .map(storage -> storage.receiveEnergy(amount, simulate))
                        .orElse(0);
            }
        });
    }
}
