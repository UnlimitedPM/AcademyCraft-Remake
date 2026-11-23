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

    /**
     * Pose un rayon du moteur des rayons chez le client.
     *
     * <p>Le moteur lui-meme ({@code MdRayView}, ses mine ray, ses rubans) reste cote Forge : c'est
     * un dessin d'effet, branche sur les evenements de rendu du chargeur. Ce que le code commun a
     * besoin de faire, c'est de DEMANDER le rayon — le railgun, par exemple, se dessine avec ce
     * moteur et non avec un dessin a lui.
     */
    @FunctionalInterface
    public interface RayBeamSpawner {
        void spawn(String kind, net.minecraft.world.phys.Vec3 from,
                   net.minecraft.world.phys.Vec3 to, int ownerId);
    }

    /**
     * Les deux crochets de conversion d'une creature vivante.
     *
     * <p>Forge les poste autour de {@code Villager.thunderHit} : un mod peut refuser la
     * transformation, et il recoit celle qui a eu lieu. Le code commun ne connait que ces deux
     * questions ; sans installation, tout est permis et personne n'est prevenu.
     */
    public interface MobConversion {
        /** La conversion est-elle permise ? */
        boolean canConvert(net.minecraft.world.entity.Mob source,
                           net.minecraft.world.entity.EntityType<?> result);

        /** La conversion vient d'avoir lieu. */
        void onConverted(net.minecraft.world.entity.Mob source, net.minecraft.world.entity.Mob result);
    }

    /**
     * Le refus d'une protection de terrain, avant qu'un bloc ne parte.
     *
     * <p>Forge poste un {@code BlockEvent.BreakEvent} pour chaque bloc casse par un joueur, et
     * c'est par lui que les plugins de protection disent non. Sans ce crochet, une competence qui
     * creuse en area passerait a travers. Sans installation, tout est permis.
     */
    @FunctionalInterface
    public interface BlockBreakGuard {
        boolean allow(net.minecraft.world.entity.player.Player player,
                      net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                      net.minecraft.world.level.block.state.BlockState state);
    }

    /**
     * Termine un maintien, par la fin ordinaire.
     *
     * <p>C'est la sortie qui rend ce que le maintien avait emprunte, pose sa recharge et previent
     * le client ({@code HoldOverPacket}). Elle vit dans l'evenement Forge, qui la partage avec le
     * relachement de touche : on la demande donc au chargeur.
     */
    public interface HeldEnd {
        void end(net.minecraft.server.level.ServerPlayer player,
                 cn.academy.ability.AbilityData data, cn.academy.ability.Skill skill);
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

    private static RayBeamSpawner rayBeamSpawner = (kind, from, to, ownerId) -> {
    };

    private static MobConversion mobConversion = new MobConversion() {
        @Override
        public boolean canConvert(net.minecraft.world.entity.Mob source,
                                  net.minecraft.world.entity.EntityType<?> result) {
            return true;
        }

        @Override
        public void onConverted(net.minecraft.world.entity.Mob source,
                                net.minecraft.world.entity.Mob result) {
        }
    };

    private static HeldEnd heldEnd = (player, data, skill) -> {
    };

    private static BlockBreakGuard blockBreakGuard = (player, level, pos, state) -> true;

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

    public static void installRayBeamSpawner(RayBeamSpawner spawner) {
        rayBeamSpawner = spawner;
    }

    public static void installMobConversion(MobConversion conversion) {
        mobConversion = conversion;
    }

    public static void installHeldEnd(HeldEnd end) {
        heldEnd = end;
    }

    public static void installBlockBreakGuard(BlockBreakGuard guard) {
        blockBreakGuard = guard;
    }

    /** Une protection de terrain permet-elle cette casse ? */
    public static boolean allowsBlockBreak(net.minecraft.world.entity.player.Player player,
                                           net.minecraft.world.level.Level level,
                                           net.minecraft.core.BlockPos pos,
                                           net.minecraft.world.level.block.state.BlockState state) {
        return blockBreakGuard.allow(player, level, pos, state);
    }

    /** Termine un maintien par la fin ordinaire. */
    public static void endHeld(net.minecraft.server.level.ServerPlayer player,
                               cn.academy.ability.AbilityData data, cn.academy.ability.Skill skill) {
        heldEnd.end(player, data, skill);
    }

    /** La conversion d'une creature est-elle permise ? */
    public static boolean canConvertMob(net.minecraft.world.entity.Mob source,
                                        net.minecraft.world.entity.EntityType<?> result) {
        return mobConversion.canConvert(source, result);
    }

    /** La conversion vient d'avoir lieu. */
    public static void mobConverted(net.minecraft.world.entity.Mob source,
                                    net.minecraft.world.entity.Mob result) {
        mobConversion.onConverted(source, result);
    }

    /** Demande un rayon du moteur des rayons (cote client). */
    public static void spawnRayBeam(String kind, net.minecraft.world.phys.Vec3 from,
                                    net.minecraft.world.phys.Vec3 to, int ownerId) {
        rayBeamSpawner.spawn(kind, from, to, ownerId);
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
