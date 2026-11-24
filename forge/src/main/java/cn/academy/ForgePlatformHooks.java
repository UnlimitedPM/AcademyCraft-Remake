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
        PlatformHooks.installHeldEnd(new PlatformHooks.HeldEnd() {
            @Override
            public void end(net.minecraft.server.level.ServerPlayer player,
                            cn.academy.ability.AbilityData data, cn.academy.ability.Skill skill) {
                cn.academy.ability.AbilityTicker.endHeld(player, data, skill);
            }

            @Override
            public void endAll(net.minecraft.server.level.ServerPlayer player,
                               cn.academy.ability.AbilityData data) {
                cn.academy.ability.AbilityTicker.endAllOnDeactivate(player, data);
            }

            @Override
            public void endOutsidePreset(net.minecraft.server.level.ServerPlayer player) {
                cn.academy.ability.AbilityTicker.endHoldsOutsideCurrentPreset(player);
            }
        });
        // Le client tient ses touches : c'est lui qui ferme le bon maintien.
        // Le garde de dist est obligatoire : la reference de methode charge la classe, et
        // AbilityClientEvents touche LocalPlayer -> plantage sur serveur dedie.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            PlatformHooks.installHoldOverListener(
                    cn.academy.ability.client.AbilityClientEvents::onHoldOver);
        }
        PlatformHooks.installBlockBreakGuard((player, level, pos, state) ->
                !net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new net.minecraftforge.event.level.BlockEvent.BreakEvent(
                                level, pos, state, player)));
        PlatformHooks.installPresetQuery((player, skillName) -> {
            cn.academy.ability.preset.PresetData presets =
                    cn.academy.ability.preset.PresetTracker.of(player);
            return presets != null && presets.getCurrent().contains(skillName);
        });
        PlatformHooks.installPresetAccess(player -> player
                .getCapability(cn.academy.ability.preset.PresetCapability.PRESET_DATA)
                .map(cn.academy.ability.preset.PresetCapability.Holder::get)
                .orElse(null));
        PlatformHooks.installTerminalAccess(player -> player
                .getCapability(cn.academy.terminal.TerminalCapability.TERMINAL_DATA)
                .resolve().orElse(null));
        PlatformHooks.installTutorialAccess(player -> player
                .getCapability(cn.academy.terminal.tutorial.TutorialCapability.TUTORIAL_DATA)
                .resolve().orElse(null));
        PlatformHooks.installMediaAccess(player -> player
                .getCapability(cn.academy.misc.media.MediaCapability.MEDIA_DATA)
                .map(cn.academy.misc.media.MediaCapability.Holder::get)
                .orElse(null));
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            PlatformHooks.installTerminalInstallBanner(
                    cn.academy.terminal.client.TerminalInstallOverlay::start);
            // Les shaders ne voient pas les block entities d'eux-memes : c'est le mod de
            // compatibilite Oculus qui a besoin qu'on lui nomme les blocs a ombrer.
            PlatformHooks.installShadowCasterTracker(
                    cn.academy.compat.oculus.OculusShadowCompat::track);
        }
        PlatformHooks.installHandEffectAnnouncer(player ->
                cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                                .with(() -> player),
                        new cn.academy.ability.network.RailgunHandPacket(player.getId())));
        PlatformHooks.installMobConversion(new PlatformHooks.MobConversion() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean canConvert(net.minecraft.world.entity.Mob source,
                                      net.minecraft.world.entity.EntityType<?> result) {
                return net.minecraftforge.event.ForgeEventFactory.canLivingConvert(source,
                        (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>) result,
                        ignored -> {
                        });
            }

            @Override
            public void onConverted(net.minecraft.world.entity.Mob source,
                                    net.minecraft.world.entity.Mob result) {
                net.minecraftforge.event.ForgeEventFactory.onLivingConvert(source, result);
            }
        });
        // Le moteur des rayons est un dessin d'effet : il n'existe que chez le client.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            PlatformHooks.installRayBeamSpawner((kind, from, to, ownerId) ->
                    cn.academy.ability.client.md.MdRayView.spawn(
                            cn.academy.ability.client.md.MdRayKind.byName(kind), from, to, ownerId));
        }
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
