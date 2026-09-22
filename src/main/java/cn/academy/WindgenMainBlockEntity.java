package cn.academy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Tete de l'eolienne : les pales.
 *
 * Portage de {@code TileWindGenMain}. Elle ne produit rien elle-meme : elle garde
 * l'helice et repond a deux questions que la base lui pose — y a-t-il une helice
 * installee, et la zone balayee par les pales est-elle degagee.
 *
 * <h2>Ou est le block entity</h2>
 *
 * Le rotor occupe trois blocs alignes dans l'axe du vent ({@code part} =
 * {@code front}, {@code center}, {@code back}). Seul le bloc central porte le
 * block entity : les deux autres sont des leurres qui n'existent que pour
 * l'affichage. Sans cela on aurait trois helices et trois zones balayees pour une
 * seule eolienne.
 */
public class WindgenMainBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements MenuProvider {

    public static final int SLOT_FAN = 0;

    /**
     * Demi-etendue de la zone balayee par les pales, en blocs.
     *
     * L'original verifiait un carre de 15 sur 15, soit 7 blocs de part et d'autre
     * du centre. Il excluait le bloc central, parce que le rotor y posait une de
     * ses propres parties ; ici la verification se fait deux blocs devant le
     * rotor, donc plus rien du rotor ne tombe dans la zone et l'exclusion n'a
     * plus lieu d'etre.
     */
    private static final int BLADE_RADIUS = 7;

    /**
     * Cadence de revision de la zone balayee, en ticks.
     *
     * La zone fait 225 blocs : la verifier a chaque tick serait du gaspillage,
     * et la relire depuis l'ecran a chaque image encore plus. Le resultat est
     * donc calcule ici et mis en cache.
     */
    private static final int OBSTACLE_CHECK_INTERVAL = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.isEmpty() || stack.is(ModItems.WINDGEN_FAN.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // L'helice change ce que la base doit produire : il faut la prevenir.
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    /** Dernier resultat connu de la verification de la zone balayee. */
    private boolean noObstacle;

    private int obstacleCounter;

    public WindgenMainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WINDGEN_MAIN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WindgenMainBlockEntity rotor) {
        if (level.isClientSide) return;
        if (++rotor.obstacleCounter < OBSTACLE_CHECK_INTERVAL) return;
        rotor.obstacleCounter = 0;

        boolean now = computeNoObstacle(level, pos, state);
        if (now != rotor.noObstacle) {
            rotor.noObstacle = now;
            rotor.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public boolean isFanInstalled() {
        return inventory.getStackInSlot(SLOT_FAN).is(ModItems.WINDGEN_FAN.get());
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** Vrai si rien ne gene la rotation des pales, d'apres la derniere revision. */
    public boolean isNoObstacle() {
        return noObstacle;
    }

    /**
     * Verifie la zone balayee par les pales.
     *
     * La zone est un carre vertical perpendiculaire a l'axe du rotor, a deux blocs
     * devant celui-ci : les pales balaient un plan, pas un volume. Deux blocs
     * parce que le rotor occupe deja trois blocs dans son axe, dont un devant le
     * centre.
     */
    public static boolean computeNoObstacle(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof WindgenMainBlock)) return false;

        Direction facing = state.getValue(WindgenMainBlock.FACING);
        Direction side = facing.getClockWise();
        BlockPos plane = pos.relative(facing, 2);

        for (int along = -BLADE_RADIUS; along <= BLADE_RADIUS; along++) {
            for (int dy = -BLADE_RADIUS; dy <= BLADE_RADIUS; dy++) {
                BlockPos check = plane.relative(side, along).above(dy);
                // Un chunk non charge est traite comme un obstacle : mieux vaut
                // une eolienne a l'arret qu'une eolienne qui tourne a travers un
                // mur dont on ignore l'existence.
                if (!level.isLoaded(check)) return false;
                if (!level.getBlockState(check).isAir()) return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putBoolean("no_obstacle", noObstacle);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        noObstacle = tag.getBoolean("no_obstacle");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WindgenMainMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.windgen_main");
    }
}
