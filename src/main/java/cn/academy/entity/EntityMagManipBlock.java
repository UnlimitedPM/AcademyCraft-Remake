package cn.academy.entity;

import cn.academy.ModEntities;
import cn.academy.ability.electromaster.MagManipVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Un bloc porte par la manipulation magnetique, portage de {@code MagManipEntityBlock} et de
 * son {@code EntityBlock}.
 *
 * <p>C'est une entite a deux vies. <b>Portee</b>, elle suit un point que la competence lui
 * donne a chaque tick — deux blocs devant les yeux de son porteur — et ne tombe pas : c'est
 * un bloc arrache au sol qui flotte devant la main. <b>Lachee</b>, elle retombe ou part, et
 * se repose des qu'elle touche quelque chose.
 *
 * <h2>Ce qu'elle fait en chemin</h2>
 *
 * Une fois lachee, elle frappe : dix points de degats au premier corps qu'elle traverse, et
 * c'est ce que l'original faisait aussi — {@code damage = 10}, pose a la construction, alors
 * que la competence calculait une courbe de 8 a 15 qu'elle ne passait jamais. Le port reprend
 * la constante, et la courbe morte reste ou elle est.
 *
 * <h2>Ou elle se pose</h2>
 *
 * <p>Des que sa <b>boite</b> touche quelque chose, et pas des que son axe passe au-dessus :
 * c'est le moteur qui resout la collision de la boite entiere, comme la {@code Rigidbody}
 * « accurate » de l'original, et le bloc se pose la ou elle s'est arretee. Un rayon unique, parti
 * du centre, laissait le bloc GLISSER sur les pentes et les aretes — il ne voyait le sol qu'une
 * fois son axe au-dessus, et filait dessus au lieu de s'y poser. Le client, lui, arrete sa copie
 * au meme endroit : il ne pose rien (c'est le serveur qui pose et qui enleve l'entite), mais il
 * ne continue pas a glisser en attendant le paquet.
 *
 * <p>L'original cherchait le premier emplacement libre a partir du bloc touche : lui-meme s'il
 * est remplacable, puis le voisin de la face touchee, puis l'un des huit coins. Le port part de
 * la boite qui a touche — son propre bloc, ses six voisins, puis ses huit coins —, et la ou
 * l'original finissait par {@code EntityBlock Lost} quand rien ne convenait — le bloc
 * disparaissait en silence —, le port le <b>laisse tomber en objet</b>. Perdre un bloc de fer
 * sans rien dire serait un prix un peu cher pour une competence de niveau 2.
 *
 * <p>Non porte : la sonde de particules ({@code EntitySurroundArc}) que l'original accrochait
 * au bloc pour l'entourer d'arcs electriques.
 */
public class EntityMagManipBlock extends Projectile {

    /** Les degats d'un bloc lance : la constante de l'original. */
    public static final float HIT_DAMAGE = 10f;

    /**
     * La gravite d'un bloc lache, par tick : le {@code motionY -= 0.04} de l'original, fois
     * l'avance double de son bloc — voir {@code MagManipVisuals.STEPS}.
     */
    public static final double GRAVITY = MagManipVisuals.flightGravity();

    /**
     * Les emplacements essayes quand la boite a touche : le sien, ses six voisins, ses coins.
     *
     * <p>C'est l'ordre de l'original a une chose pres : il partait du bloc TOUCHE, puis du voisin
     * de la face, puis des huit coins — et il ne regardait jamais les voisins d'arete. Le port
     * part de la boite elle-meme, parce que c'est elle qui detecte le contact maintenant : son
     * propre bloc d'abord (l'air juste au-dessus du sol quand elle s'est posee, l'air juste
     * devant le mur quand elle s'y est arretee), puis ses six voisins, puis ses huit coins.
     */
    private static final int[][] SPOTS = {
            { 0, 0, 0 },
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 },
            { 1, 1, 1 }, { 1, 1, -1 }, { 1, -1, 1 }, { 1, -1, -1 },
            { -1, 1, 1 }, { -1, 1, -1 }, { -1, -1, 1 }, { -1, -1, -1 },
    };

    private static final EntityDataAccessor<BlockState> DATA_BLOCK =
            SynchedEntityData.defineId(EntityMagManipBlock.class, EntityDataSerializers.BLOCK_STATE);

    /** Le point que le bloc suit, ou {@code null} quand il est lache. */
    private Vec3 carryTo;

    /** Le bloc s'est-il deja pose ? */
    private boolean placed;

    /**
     * Cote client : ce bloc est celui du JOUEUR LOCAL, donc c'est son client qui le fait vivre.
     *
     * <p>Rien a voir avec le {@code carryTo} du serveur, qui n'est pas synchronise : c'est
     * {@code MagManipEffect} qui le pose, une fois, en voyant un bloc dont le proprietaire est le
     * joueur. Les blocs des autres joueurs n'y ont pas droit : ils suivent les positions du
     * serveur, comme toute entite.
     */
    private boolean clientOwned;

    /**
     * Et le portage client s'occupe de lui, pour ce tick.
     *
     * <p>Pose par {@code MagManipEffect} tant que la touche est tenue, et efface a chaque tick
     * client avant : un bloc qui n'est plus porte reprend donc son vol tout seul.
     */
    private boolean clientCarried;

    /**
     * La pose LOCALE du portage, et celle d'avant.
     *
     * <p>Elles n'appartiennent qu'au client, et c'est tout le sujet : le portage est mene des DEUX
     * cotes, chacun suivant le regard qu'il connait, et le serveur envoie sa position tous les deux
     * ticks. Sans ces deux points, chaque paquet RAMENAIT la copie cliente jusqu'a un demi-bloc en
     * arriere — les « ralentissements » que le joueur voit quand il tourne la tete ou s'eloigne.
     * Le rendu part de ces deux points-la, jamais de ceux du reseau.
     *
     * <p>Voir {@link #poseCarried} et {@link #displayPosition}.
     */
    private Vec3 carriedAt;
    private Vec3 carriedBefore;

    public EntityMagManipBlock(EntityType<? extends EntityMagManipBlock> type, Level level) {
        super(type, level);
    }

    /** Le bloc tel que la competence l'arrache : au centre du bloc vise, ou devant la main. */
    public EntityMagManipBlock(Level level, Player holder, BlockState state, Vec3 position) {
        this(ModEntities.MAG_MANIP_BLOCK.get(), level);
        setOwner(holder);
        setBlockState(state);
        setPos(position.x, position.y, position.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK, state);
    }

    /** La competence lui donne le point a suivre, a chaque tick. */
    public void carryTo(Vec3 target) {
        this.carryTo = target;
    }

    /**
     * Lache le bloc.
     *
     * <p>Il se posera au premier contact : c'est le {@code setPlaceFromServer(true)} de
     * l'original, pose au relachement comme a la fin du maintien. Tant qu'il est porte, il ne
     * se pose pas — un bloc qui frotte un mur pendant qu'on le tient ne doit pas s'y coller.
     */
    public void release() {
        this.carryTo = null;
    }

    /** Marque le bloc comme celui du joueur local. Voir {@link #clientOwned}. */
    public void markOwnedByLocalPlayer() {
        this.clientOwned = true;
    }

    /**
     * Marque le bloc comme porte, pour ce tick client. Voir {@link #clientCarried}.
     *
     * <p>Appele par {@code MagManipEffect} tant que la touche est tenue, et efface a chaque tick
     * client avant : un bloc qui n'est plus porte reprend donc son vol tout seul.
     */
    public void markCarried() {
        this.clientCarried = true;
    }

    /** Le portage client ne veut plus de ce bloc : il vole de ses propres ailes. */
    public void unmarkCarried() {
        this.clientCarried = false;
        this.carriedAt = null;
        this.carriedBefore = null;
    }

    /**
     * Pose la copie locale du bloc porte.
     *
     * <p>Deux points, et pas un : le point d'arrivee et celui d'OU l'on vient. Le rendu interpole
     * entre les anciennes positions de l'entite et les nouvelles, et celles du reseau ne sont pas
     * les notres — un paquet recu juste avant l'image partirait donc d'un point ou le bloc n'a
     * jamais ete, et ferait un bond au lieu de glisser.
     */
    public void poseCarried(Vec3 at, Vec3 before) {
        this.carriedAt = at;
        this.carriedBefore = before;
        this.xo = before.x;
        this.yo = before.y;
        this.zo = before.z;
        setPos(at.x, at.y, at.z);
    }

    /**
     * Ou le bloc se dessine : sa pose locale s'il est porte par ce client, sa position sinon.
     *
     * <p>Un bloc porte par un AUTRE joueur n'est pas concerne : il n'a pas de pose locale ici, et
     * suit les positions du serveur comme n'importe quelle entite.
     */
    public Vec3 displayPosition() {
        return carriedAt != null ? carriedAt : position();
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            // Le client avance la copie de SON joueur comme le serveur, mais sans RIEN poser :
            // c'est le serveur qui decide ou le bloc s'arrete, et il le fait savoir en enlevant
            // l'entite.
            //
            // C'est ce que faisait l'original, et ce qui manquait ici : son c_perform donnait la
            // vitesse a la copie cliente, dont le tick la faisait voler. Sans cela le bloc
            // n'avançait que la ou le serveur le mettait, et le serveur n'envoie sa position que
            // tous les deux ticks : le lancer paraissait mou et lent, ce que le joueur a vu.
            if (!clientOwned || clientCarried) return;
            if (getDeltaMovement().lengthSqr() < 1.0E-6) return;
            setDeltaMovement(getDeltaMovement().add(0, -GRAVITY, 0));
            // S'IL TOUCHE, IL S'ARRETE ICI. Le client ne pose pas de bloc — c'est le serveur qui
            // le fait, et qui enleve l'entite —, mais il ne doit pas non plus continuer a glisser
            // en attendant le paquet : a deux blocs par tick, ces deux ticks d'attente font
            // quatre blocs de trop, et c'est ce que le joueur a vu — « il a glisse et il est
            // parti plus loin au lieu de s'arreter sur le sol ».
            if (touched()) {
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        if (carryTo != null) {
            setDeltaMovement(MagManipVisuals.carryVelocity(position(), carryTo));
            move(MoverType.SELF, getDeltaMovement());
            return;
        }

        setDeltaMovement(getDeltaMovement().add(0, -GRAVITY, 0));

        // La boite avance, et le MOTEUR dit si elle a touche quelque chose.
        //
        // C'etait un rayon, parti du centre de la boite, et c'etait le defaut : un rayon ne voit
        // ni les pentes, ni les aretes, ni les blocs qui ne croisent que les coins. Le bloc
        // touchait le sol du cote et continuait a glisser dessus au lieu de s'y poser. L'original
        // ne faisait pas ca : sa Rigidbody « accurate » regardait la boite ENTIERE.
        //
        // Et la pose n'attend pas la fin du tick : des que la boite touche, le bloc est pose la
        // ou elle s'est arretee.
        if (touched()) {
            placeAround();
            return;
        }
        hurtAlong();
    }

    /**
     * Avance la boite de son mouvement, et dit si elle a touche quelque chose.
     *
     * <p>Les deux drapeaux du moteur disent exactement cela : quelque chose sur le cote
     * ({@code horizontalCollision}) ou dessous, dessus ({@code verticalCollision}). Le moteur
     * resout aussi la collision — la boite s'arrete collee a ce qu'elle a touche, et ne peut pas
     * traverser un mur d'un bloc, meme avancee de deux blocs par tick.
     */
    private boolean touched() {
        move(MoverType.SELF, getDeltaMovement());
        return horizontalCollision || verticalCollision;
    }

    /** Un corps sur le trajet, et c'est dix points. */
    private void hurtAlong() {
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(0.2))) {
            if (entity == this || entity == getOwner() || !(entity instanceof LivingEntity living)) {
                continue;
            }
            Entity owner = getOwner();
            var source = owner instanceof LivingEntity livingOwner
                    ? damageSources().indirectMagic(this, livingOwner)
                    : damageSources().indirectMagic(this, this);
            living.hurt(source, HIT_DAMAGE * (float) cn.academy.Config.damageScale);
        }
    }

    /**
     * Un bloc touche : le bloc pose se pose.
     *
     * <p>Le rayon qui vivait ici rendait a la fois le bloc touche et sa face — l'evenement de
     * collision de l'original —, et les trois recherches d'emplacement partaient de la. Il ne
     * touchait rien des qu'il passait a cote : voir {@link #touched()}, qui regarde la boite
     * entiere, et {@link #placeAround()}, qui part de la boite elle-meme.
     */
    private void placeAround() {
        if (placed) return;
        placed = true;

        BlockPos around = blockPosition();
        for (int[] offset : SPOTS) {
            BlockPos pos = around.offset(offset[0], offset[1], offset[2]);
            if (free(pos)) {
                level().setBlock(pos, getBlockState(), 3);
                discard();
                return;
            }
        }
        Containers.dropItemStack(level(), getX(), getY(), getZ(),
                new ItemStack(getBlockState().getBlock()));
        discard();
    }

    private boolean free(BlockPos pos) {
        return level().getBlockState(pos).canBeReplaced();
    }

    /**
     * Rien ne survit a un rechargement.
     *
     * <p>L'original lisait son bloc dans une donnee synchronisee et le tuait au chargement,
     * comme la bille de silicium : le port fait pareil.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (getBlockState() != null) {
            tag.putString("block", BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString());
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
