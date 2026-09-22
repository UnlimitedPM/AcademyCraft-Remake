package cn.academy.entity;

import cn.academy.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * La bille de silicium, portage de {@code EntitySilbarn} : un objet qu'on lance, qui se
 * pose ou il touche, et qui sert de point d'ancrage a la salve de rayons.
 *
 * <h2>Elle ne vole pas longtemps</h2>
 *
 * L'original la lancait a la vitesse du regard — un bloc par tick, soit vingt blocs par
 * seconde — et lui appliquait aussitot une trainee de 0,8 par tick. En cinq ticks elle a
 * donc perdu les deux tiers de sa vitesse et parcouru cinq blocs : c'est un lancer de
 * main, pas un projectile. Sa gravite ne s'allume qu'apres cinquante ticks (deux secondes
 * et demie) : elle <b>flotte</b> un moment la ou elle a ete lancee, puis tombe.
 *
 * <h2>Ce que « posee » veut dire</h2>
 *
 * Des qu'elle touche un bloc, elle est <b>posee</b> ({@code hit} de l'original), ce qui a
 * trois consequences :
 *
 * <ul>
 *   <li>elle disparait dix ticks plus tard — on a le temps de la voir, pas celui d'en
 *       lancer une autre au meme endroit ;</li>
 *   <li>elle n'est plus visable : la salve ne s'amorce que sur une bille
 *       <b>posee</b>, et la chercher a nouveau apres coup ne donnerait rien ;</li>
 *   <li>elle ne se sauvegarde pas. L'original tuait la sienne au chargement, et le port
 *       fait pareil : ni une bille posee ni une bille en vol ne survit a un
 *       rechargement.</li>
 * </ul>
 *
 * <h2>Ce qui compte pour la suite</h2>
 *
 * {@link #isHit()} voyage jusqu'au client (donnee synchronisee) : le rendu cache la bille
 * posee, comme l'original, et la salve lit le meme drapeau cote serveur.
 */
public class EntitySilbarn extends Projectile {

    /** Trainee par tick, reprise du {@code Rigidbody} de l'original. */
    public static final double DRAG = 0.8;

    /** Gravite, reprise de l'original : 0,12 par tick. */
    public static final double GRAVITY = 0.12;

    /** Ticks avant que la gravite ne s'applique : deux secondes et demie de flottement. */
    public static final int GRAVITY_DELAY = 50;

    /** Ticks entre le contact et la disparition. */
    public static final int HIT_LIFETIME = 10;

    private static final EntityDataAccessor<Boolean> DATA_HIT =
            SynchedEntityData.defineId(EntitySilbarn.class, EntityDataSerializers.BOOLEAN);

    /** Ticks vecus, pour savoir quand la gravite s'allume. */
    private int life;

    /** Ticks depuis le contact, ou -1 tant qu'elle n'a rien touche. */
    private int sinceHit = -1;

    public EntitySilbarn(EntityType<? extends EntitySilbarn> type, Level level) {
        super(type, level);
    }

    /** La bille telle que l'objet la lance : au niveau des yeux, dans l'axe du regard. */
    public EntitySilbarn(Level level, Player thrower) {
        this(ModEntities.SILBARN.get(), level);
        setOwner(thrower);
        setPos(thrower.getX(), thrower.getEyeY(), thrower.getZ());
        Vec3 look = thrower.getLookAngle();
        // Un bloc par tick, comme `motionX = look.x` de l'original — donc une vitesse
        // que la trainee fait tomber en cinq ticks.
        shoot(look.x, look.y, look.z, 1f, 0f);
        // L'original orientait sa bille sur le lacet de la tete.
        setYRot(thrower.getYRot());
        setXRot(thrower.getXRot());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HIT, false);
    }

    /** La bille est-elle posee ? Vrai des qu'elle a touche un bloc. */
    public boolean isHit() {
        return this.entityData.get(DATA_HIT);
    }

    /**
     * Pose la bille : elle est desormais au contact.
     *
     * Deux appelants, comme dans l'original : le contact avec un bloc, et la salve de
     * rayons, qui fait exploser la bille qu'elle a trouvee en la postant au meme etat. Dans
     * les deux cas la bille cesse d'etre visable et disparait dix ticks plus tard.
     */
    public void markHit() {
        if (isHit()) return;
        this.entityData.set(DATA_HIT, true);
        this.sinceHit = 0;
    }

    @Override
    public void tick() {
        super.tick();
        ++this.life;

        if (this.sinceHit >= 0 && ++this.sinceHit >= HIT_LIFETIME) {
            // Dix ticks apres le contact : le temps de la voir posee, pas celui d'en
            // lancer une autre au meme endroit.
            discard();
            return;
        }

        // Elle passe a travers tout ce qui vit (`entitySel = nothing` de l'original) :
        // seuls les blocs l'arretent.
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision || this.verticalCollision) {
            markHit();
        }

        Vec3 motion = this.getDeltaMovement().scale(DRAG);
        if (this.life >= GRAVITY_DELAY) {
            motion = motion.add(0, -GRAVITY, 0);
        }
        this.setDeltaMovement(motion);
    }

    /**
     * Une bille posee ne se vise plus.
     *
     * C'est ce que lit la salve : elle cherche une bille <b>en vol</b> devant elle, et une
     * bille deja posee n'est plus une cible.
     */
    @Override
    public boolean isPickable() {
        return !isHit();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // L'original faisait disparaitre la sienne au chargement : une bille ne survit pas
        // a un rechargement.
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Rien : voir readAdditionalSaveData. C'est ce que faisait l'original.
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
