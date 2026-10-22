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
        implements MenuProvider, cn.academy.StoresItems {

    public static final int SLOT_FAN = 0;

    /**
     * Demi-etendue de la zone balayee par les pales, en blocs.
     *
     * L'original verifiait un carre de 15 sur 15, soit 7 blocs de part et d'autre
     * du centre. Il excluait le bloc central, parce que le rotor y posait une de
     * ses propres parties : c'est ce que fait {@link #computeNoObstacle}.
     */
    private static final int BLADE_RADIUS = 7;

    /**
     * La boite dans laquelle le rendu des pales a le droit de se dessiner.
     *
     * <p>Sans elle, Minecraft n'autorise le rendu que dans la boite d'UN bloc, et les pales
     * disparaissaient des que ce bloc sortait du champ — le joueur : « les helices de l'eolienne
     * partent tres vite si je regarde un peu trop vers le bas ». L'original rendait cette boite
     * infinie ; celle-ci fait la taille des pales, ce qui garde l'elimination quand l'eolienne est
     * vraiment derriere soi. Voir {@link RenderBounds#windgenRotor}.
     */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        double[] b = RenderBounds.windgenRotor(BLADE_RADIUS);
        return new net.minecraft.world.phys.AABB(
                worldPosition.getX() + b[0], worldPosition.getY() + b[1], worldPosition.getZ() + b[2],
                worldPosition.getX() + b[3], worldPosition.getY() + b[4], worldPosition.getZ() + b[5]);
    }

    /**
     * Cadence de revision de la zone balayee, en ticks.
     *
     * <p>Deux ticks, soit un dixieme de seconde : le joueur posait un bloc devant les pales et les
     * voyait tourner encore une seconde (22/10) — « quand je pose un bloc au-dessus de l'eolienne,
     * elle met beaucoup de temps avant de s'arreter ». Le port verifiait toutes les vingt ticks
     * (une seconde), l'original toutes les dix (une demi-seconde) : c'est trop lent des qu'on
     * regarde ce qu'on vient de poser.
     *
     * <p>Le cout n'est pas un probleme : la zone fait 225 blocs, donc 225 lectures d'etat tous les
     * deux ticks — moins qu'une seule image de rendu. Et le resultat reste mis en cache : c'est lui
     * que lisent l'ecran et le client, jamais la zone elle-meme.
     */
    private static final int OBSTACLE_CHECK_INTERVAL = 2;

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

    /**
     * La colonne est-elle assez haute ?
     *
     * <p>C'est la base qui produit, mais c'est le rotor qui doit savoir si ses pales
     * tournent : il remonte donc sa colonne lui-meme, comme le faisait l'original. Le
     * resultat est envoye au client avec le reste de son etat.
     */
    private boolean complete;

    private int obstacleCounter;

    // ------------------------------------------------------------------
    // Etat d'affichage (client seulement, rien n'est sauvegarde)
    // ------------------------------------------------------------------

    /** Angle des pales, en degres. */
    private float fanRotation;

    /**
     * L'ancre de la rotation : l'instant et l'angle d'ou l'on compte.
     *
     * <p>L'angle est une fonction du temps (voir {@link WindgenStructure#angleAt}), pas une
     * accumulation d'image en image : c'est ce qui le rend lisse quand les images n'ont pas toutes
     * la meme duree — en marchant, en tournant la tete — et ce qui permet a l'eolienne de
     * s'arreter ou elle est. L'ancre se repose des que l'etat change, ou si le temps recule.
     */
    private double fanAnchor = -1.0d;
    private float fanAnchorAngle;
    private boolean fanSpinning;

    public WindgenMainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WINDGEN_MAIN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WindgenMainBlockEntity rotor) {
        if (level.isClientSide) return;
        if (++rotor.obstacleCounter < OBSTACLE_CHECK_INTERVAL) return;
        rotor.obstacleCounter = 0;

        boolean now = computeNoObstacle(level, pos, state);
        boolean standing = computeComplete(level, pos, state);
        if (now != rotor.noObstacle || standing != rotor.complete) {
            rotor.noObstacle = now;
            rotor.complete = standing;
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

    /** Vrai si la colonne qui porte ce rotor est assez haute, d'apres la derniere revision. */
    public boolean isComplete() {
        return complete;
    }

    /** Vrai si les pales doivent tourner : helice installee, zone degagee, colonne complete. */
    public boolean isSpinning() {
        return isFanInstalled() && isNoObstacle() && complete;
    }

    /**
     * L'angle des pales, avance d'une image.
     *
     * <p>Appele par le rendu, et rien d'autre : l'original tenait cet angle dans son block
     * entity, cote client, pour qu'une eolienne qui s'arrete s'arrete *ou* elle est au lieu
     * de repartir de la verticale a chaque revision de la structure.
     */
    public float advanceFan(double seconds) {
        boolean spinning = isSpinning();
        if (fanAnchor < 0.0d || spinning != fanSpinning || seconds < fanAnchor) {
            // L'etat vient de changer, ou le temps a recule : on repose l'ancre sur l'angle
            // courant, pour que les pales repartent d'ou elles sont au lieu de sauter.
            fanAnchorAngle = fanRotation;
            fanAnchor = seconds;
            fanSpinning = spinning;
        }
        fanRotation = WindgenStructure.angleAt(seconds, fanAnchor, fanAnchorAngle, spinning);
        return fanRotation;
    }

    /**
     * Verifie la zone balayee par les pales.
     *
     * La zone est un carre vertical perpendiculaire a l'axe du rotor, <b>dans le bloc qui precede
     * le centre</b> — celui ou la nacelle porte le moyeu. C'est la geometrie de l'original, qui
     * verifiait l'offset {@code (i, j, -1)} du multibloc, et il en excluait le point central, parce
     * que le rotor lui-meme y pose une de ses parties.
     *
     * <p>Le port verifiait DEUX blocs devant le centre, et sans exclusion : la zone se trouvait
     * donc un bloc trop loin, et un bloc pose juste devant les pales ne les arretait pas tandis
     * qu'un bloc un cran plus loin les arretait. Le joueur l'a vu le 22/10 : « si on considere
     * qu'il fait du 1x3, les pales sortent du 3, bah la maintenant l'eolienne regarde au 4eme bloc
     * pour savoir si oui ou non les pales peuvent etre la ».
     */
    public static boolean computeNoObstacle(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof WindgenMainBlock)) return false;

        Direction facing = state.getValue(WindgenMainBlock.FACING);
        Direction side = facing.getClockWise();
        BlockPos plane = pos.relative(facing, 1);

        for (int along = -BLADE_RADIUS; along <= BLADE_RADIUS; along++) {
            for (int dy = -BLADE_RADIUS; dy <= BLADE_RADIUS; dy++) {
                // Le moyeu : ce point tombe dans le bloc du rotor lui-meme, qui n'est pas de l'air.
                if (along == 0 && dy == 0) continue;
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

    /**
     * Remonte la colonne a l'envers : les piliers, puis la base.
     *
     * <p>C'est ce que fait {@code TileWindGenMain.isCompleteStructure} de l'original, et
     * c'est la seule chose que le rotor ait besoin de savoir de la base : la hauteur suffit
     * elle ? La production, elle, se calcule en bas.
     *
     * <p>Un troncon non charge vaut un refus : mieux vaut une eolienne qui ne tourne pas
     * qu'une eolienne qui tourne dans un vide dont on ne sait rien.
     */
    public static boolean computeComplete(Level level, BlockPos pos, BlockState state) {
        int pillars = 0;
        BlockPos probe = pos.below();
        while (level.isLoaded(probe)) {
            BlockState below = level.getBlockState(probe);
            if (below.is(ModBlocks.WINDGEN_PILLAR.get())) {
                if (++pillars > WindgenStructure.MAX_PILLARS) return false;
                probe = probe.below();
                continue;
            }
            return WindgenStructure.isComplete(pillars, below.is(ModBlocks.WINDGEN_BASE.get()));
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putBoolean("no_obstacle", noObstacle);
        tag.putBoolean("complete", complete);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        noObstacle = tag.getBoolean("no_obstacle");
        complete = tag.getBoolean("complete");
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
