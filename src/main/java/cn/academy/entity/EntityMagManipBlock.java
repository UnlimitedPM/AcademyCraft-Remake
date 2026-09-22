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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 * L'original cherchait le premier emplacement libre en partant du bloc touche : lui-meme s'il
 * est remplacable, sinon le voisin de la face touchee, sinon l'un des huit coins. Le port
 * suit les memes trois etapes ; la ou l'original finissait par {@code EntityBlock Lost} quand
 * rien ne convenait — le bloc disparaissait en silence —, le port le <b>laisse tomber en
 * objet</b>. Perdre un bloc de fer sans rien dire serait un prix un peu cher pour une
 * competence de niveau 2.
 *
 * <p>Non porte : la sonde de particules ({@code EntitySurroundArc}) que l'original accrochait
 * au bloc pour l'entourer d'arcs electriques.
 */
public class EntityMagManipBlock extends Projectile {

    /** Les degats d'un bloc lance : la constante de l'original. */
    public static final float HIT_DAMAGE = 10f;

    /** La gravite d'un bloc lache, par tick : le {@code motionY -= 0.04} de l'original. */
    public static final double GRAVITY = 0.04;

    /** La quantite d'emplacements essayes apres le bloc touche : les huit coins. */
    private static final int[][] CORNERS = {
            { 1, 1, 1 }, { 1, 1, -1 }, { 1, -1, 1 }, { 1, -1, -1 },
            { -1, 1, 1 }, { -1, 1, -1 }, { -1, -1, 1 }, { -1, -1, -1 },
    };

    private static final EntityDataAccessor<BlockState> DATA_BLOCK =
            SynchedEntityData.defineId(EntityMagManipBlock.class, EntityDataSerializers.BLOCK_STATE);

    /** Le point que le bloc suit, ou {@code null} quand il est lache. */
    private Vec3 carryTo;

    /** Vrai quand un bloc lache doit se poser au lieu de traverser le monde. */
    private boolean placeOnCollide = true;
    /** Le bloc s'est-il deja pose ? */
    private boolean placed;

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

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (carryTo != null) {
            setDeltaMovement(MagManipVisuals.carryVelocity(position(), carryTo));
            move(MoverType.SELF, getDeltaMovement());
            return;
        }

        setDeltaMovement(getDeltaMovement().add(0, -GRAVITY, 0));

        // Le rayon se lance AVANT le deplacement, sur le trajet voulu entier. Le faire apres,
        // entre la position d'arrivee et elle-meme, ne trouverait rien : `move` a deja resolu
        // la collision, donc le bloc s'arrete colle au mur, son rayon est de longueur nulle, et
        // il reste la, en l'air, sans jamais se poser. C'est le GameTest qui l'a vu.
        Vec3 from = position();
        Vec3 motion = getDeltaMovement();
        if (hitSomething(from, from.add(motion))) return;

        move(MoverType.SELF, motion);
        hurtAlong();
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
     * Un bloc touche : le bloc pose se pose, ou s'arrete net.
     *
     * <p>Le rayon rend a la fois le bloc touche et sa face — c'est l'evenement de collision de
     * l'original — et c'est de la que partent les trois recherches d'emplacement. Il se lance
     * sur le trajet <b>voulu</b>, avant que {@code move} n'ait resolu la collision : voir
     * {@link #tick()}.
     */
    private boolean hitSomething(Vec3 from, Vec3 to) {
        HitResult hit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        place((BlockHitResult) hit);
        return true;
    }

    /** Pose le bloc, ou le laisse tomber en objet s'il n'y a pas de place. */
    private void place(BlockHitResult hit) {
        if (placed) return;
        placed = true;

        BlockPos hitPos = hit.getBlockPos();
        BlockPos target = spot(hitPos, hit);
        if (target != null) {
            level().setBlock(target, getBlockState(), 3);
        } else {
            Containers.dropItemStack(level(), getX(), getY(), getZ(),
                    new ItemStack(getBlockState().getBlock()));
        }
        discard();
    }

    /**
     * Le premier emplacement qui accepte le bloc.
     *
     * <p>Le bloc touche s'il est remplacable — de l'herbe, de l'eau — puis le voisin de la
     * face touchee, puis les huit coins. C'est la recherche de l'original, y compris son
     * dernier filet : il ne regardait que les coins, jamais les voisins d'arete.
     */
    private BlockPos spot(BlockPos hitPos, BlockHitResult hit) {
        if (free(hitPos)) return hitPos;

        BlockPos face = hitPos.relative(hit.getDirection());
        if (free(face)) return face;

        for (int[] corner : CORNERS) {
            BlockPos pos = hitPos.offset(corner[0], corner[1], corner[2]);
            if (free(pos)) return pos;
        }
        return null;
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
