package cn.academy.entity;

import cn.academy.ModEntities;
import cn.academy.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Le crochet magnetique, portage d'{@code EntityMagHook} : l'objet qu'un electromaster lance, qui
 * vole a deux blocs par tick et se plante dans la premiere paroi qu'il touche.
 *
 * <p>C'est le soutien du deplacement magnetique, et le nom de l'original le dit — « Elec Move
 * Support Hook ». Le crochet lui-meme ne tire personne : ce qu'il fait, c'est <b>exister</b>,
 * quelque part devant le joueur, et comme l'electromaster attire les entites metalliques, le
 * deplacement magnetique s'y accroche. On se balance donc d'un crochet lance a vingt blocs, au lieu
 * de ne pouvoir viser que ce qu'on toucherait du regard.
 *
 * <h2>Ce qu'il fait de son vol</h2>
 *
 * <p>Un rayon unique, du centre vers la ou le mouvement le mene — c'est le {@code Rigidbody} de
 * l'original, qui regardait d'un seul trait — puis il avance de tout son pas, et la gravite le
 * tire. C'est ce qui le fait se planter dans la premiere paroi sans jamais la traverser, meme a
 * deux blocs par tick. Rien ne l'arrete au passage : il touche ce qu'il rencontre.
 *
 * <h2>Plante</h2>
 *
 * <p>Sa vitesse tombe a zero, il se recolle a sa face a chaque tick (le {@code preRender} de
 * l'original, des deux cotes, donc la position est la meme partout), il prend sa taille d'un bloc,
 * et son modele s'ouvre.
 *
 * <p>Il vit ensuite tant que son bloc tient : on le retire, et le crochet retombe en objet par
 * terre. Et un joueur qui le <b>frappe</b> le recupere de la meme facon — c'est ainsi que
 * l'original rendait l'objet qu'on venait de lancer, et sans cela chaque lancer en couterait un.
 *
 * <h2>Ce qui se sauvegarde</h2>
 *
 * <p>L'original ecrivait ses cinq nombres et les relisait : un crochet plante survit donc a un
 * rechargement, et se retrouve la ou il etait. C'est la difference avec la bille de silicium, qui
 * disparait au chargement.
 */
public class EntityMagHook extends Projectile {

    private static final EntityDataAccessor<Boolean> DATA_HIT =
            SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SIDE =
            SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_X =
            SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_Y =
            SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_Z =
            SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.INT);

    /**
     * Accroche ou non, en clair.
     *
     * <p>Deux fois la meme chose que {@code DATA_HIT}, et il faut les deux : la donnee synchronisee
     * est le seul canal par lequel le client apprend l'accroche, mais elle n'existe pas encore quand
     * le jeu demande la boite de collision du tout neuf objet — {@code defineSynchedData} passe
     * apres, et la lire la ferait tomber sur un accesseur non declare. La boite lit donc ce
     * booleen, que le tick et la synchronisation tiennent a jour.
     */
    private boolean stuck;

    public EntityMagHook(EntityType<? extends EntityMagHook> type, Level level) {
        super(type, level);
    }

    /**
     * Le lancer : au niveau des yeux, dans l'axe du regard, a {@link MagHookVisuals#SPEED} blocs
     * par tick.
     *
     * <p>L'original posait sa vitesse a la main et orientait l'objet sur la tete de son lanceur ;
     * le port passe par {@code shoot}, qui fait les deux — et c'est le meme resultat, le regard
     * etant justement la direction de la vitesse.
     */
    public EntityMagHook(Level level, Player thrower) {
        this(ModEntities.MAG_HOOK.get(), level);
        setOwner(thrower);
        setPos(thrower.getX(), thrower.getEyeY(), thrower.getZ());
        Vec3 look = thrower.getLookAngle();
        shoot(look.x, look.y, look.z, (float) MagHookVisuals.SPEED, 0f);
        setYRot(thrower.getYRot());
        setXRot(thrower.getXRot());
        // Les deux angles de l'image precedente, sinon le premier rendu interpolerait depuis la
        // visee que `shoot` a calculee et le crochet partirait de travers pendant une image.
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HIT, false);
        this.entityData.define(DATA_SIDE, Direction.DOWN.ordinal());
        this.entityData.define(DATA_X, 0);
        this.entityData.define(DATA_Y, 0);
        this.entityData.define(DATA_Z, 0);
    }

    /** Le crochet est-il plante ? C'est ce que voit le rendu, et ce que vise la traction. */
    public boolean isHit() {
        return this.entityData.get(DATA_HIT);
    }

    /** La face qui le porte. Sans objet tant qu'il vole. */
    public Direction hitSide() {
        Direction[] all = Direction.values();
        return all[Mth.clamp(this.entityData.get(DATA_SIDE), 0, all.length - 1)];
    }

    /** Le bloc auquel il tient. Sans objet tant qu'il vole. */
    public BlockPos hookBlock() {
        return new BlockPos(this.entityData.get(DATA_X), this.entityData.get(DATA_Y),
                this.entityData.get(DATA_Z));
    }

    /**
     * Le point exact ou il se tient, une fois plante.
     *
     * <p>Le rendu s'en sert pour ne PAS interpoler : l'original repeignait sa position sur la face
     * d'un coup, et c'est ce claquement contre la paroi qui se voit. Une interpolation d'une image
     * ferait glisser le crochet depuis le point d'impact, ce qui n'est pas la meme chose.
     */
    public Vec3 snapPosition() {
        return MagHookVisuals.snapTo(hookBlock(), hitSide());
    }

    @Override
    public void tick() {
        super.tick();

        // Le client apprend l'accroche par la donnee synchronisee, un tick apres le serveur : il
        // passe donc par le meme etat, sans refaire le travail de celui qui decide.
        if (!this.stuck && isHit()) {
            this.stuck = true;
            setDeltaMovement(Vec3.ZERO);
        }

        if (this.stuck) {
            planted();
        } else {
            fly();
        }
    }

    /** En vol : un rayon, un pas, et la gravite. */
    private void fly() {
        Vec3 motion = getDeltaMovement();
        Vec3 from = position();
        Vec3 to = from.add(motion);

        // Les blocs : un rayon UNIQUE du centre vers la ou le mouvement le mene, comme le
        // Rigidbody de l'original. Solides seulement — une touffe d'herbe n'arrete pas un crochet.
        BlockHitResult blockHit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        // Et les creatures, qui comptent quand elles sont plus pres que la paroi. Deux sortes
        // d'entites sont traversees : le lanceur, que l'original excluait, et un crochet ENCORE EN
        // VOL — deux crochets ne se rencontrent pas en l'air, alors qu'un crochet plante, si.
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this, from, to,
                getBoundingBox().expandTowards(motion).inflate(1.0),
                e -> e != getOwner() && e.isPickable() && !e.isSpectator()
                        && !(e instanceof EntityMagHook other && !other.isHit()),
                from.distanceToSqr(to));

        if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || from.distanceToSqr(entityHit.getLocation())
                        <= from.distanceToSqr(blockHit.getLocation()))) {
            strike(entityHit.getEntity());
            return;
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            stick(blockHit.getBlockPos(), blockHit.getDirection());
            return;
        }

        // Rien touche : il avance, la gravite le tirant vers le sol.
        Vec3 step = motion.add(0, -MagHookVisuals.GRAVITY, 0);
        setDeltaMovement(step);
        setPos(from.add(step));
    }

    /** Plante : il se recolle a sa face, et il tombe si on lui retire son bloc. */
    private void planted() {
        setPos(snapPosition());

        if (!level().isClientSide && level().getBlockState(hookBlock()).isAir()) {
            dropAsItem();
        }
    }

    /** L'accroche : les cinq nombres de l'original, la vitesse a zero, et le bruit. */
    private void stick(BlockPos block, Direction side) {
        this.entityData.set(DATA_HIT, true);
        this.entityData.set(DATA_SIDE, side.ordinal());
        this.entityData.set(DATA_X, block.getX());
        this.entityData.set(DATA_Y, block.getY());
        this.entityData.set(DATA_Z, block.getZ());
        this.stuck = true;
        setDeltaMovement(Vec3.ZERO);
        setPos(MagHookVisuals.snapTo(block, side));
        setBoundingBox(makeBoundingBox());

        // Le bruit de l'accroche, et il appartient au SERVEUR : le client joue deja le sien quand
        // l'accroche lui parvient, un son pose des deux cotes s'entendrait deux fois. Volume et
        // hauteur sont ceux de l'original.
        //
        // Son fichier, lui, n'a jamais existe : l'original demandait `maghook_land`, absent de son
        // propre `sounds.json` — la meme famille de defaut que le creeper qu'il ne chargeait pas.
        // Le port prend donc un son de vanilla pour ce geste, un noeud qu'on serre.
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
    }

    /** Il touche quelque chose qui vit (ou un crochet deja plante) : il frappe, puis il tombe. */
    private void strike(Entity target) {
        if (level().isClientSide) {
            return;
        }
        if (!(target instanceof EntityMagHook)) {
            target.hurt(playerDamage(), MagHookVisuals.HIT_DAMAGE);
        }
        dropAsItem();
    }

    /** Les degats d'un crochet : ceux du joueur qui l'a lance, comme {@code causePlayerDamage}. */
    private DamageSource playerDamage() {
        Entity owner = getOwner();
        return owner instanceof Player player ? player.damageSources().playerAttack(player)
                : damageSources().generic();
    }

    /** Il redevient l'objet qu'on avait lance. */
    private void dropAsItem() {
        level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(),
                new ItemStack(ModItems.MAG_HOOK.get())));
        discard();
    }

    /**
     * Un coup de main sur un crochet plante le recupere.
     *
     * <p>C'est l'{@code attackEntityFrom} de l'original, et il a une raison d'etre : sans lui, un
     * crochet lance serait un crochet perdu, et il faudrait trois lingots par accroche. Le crochet
     * absorbe toujours le coup (l'original rendait vrai sans condition) : on ne peut pas casser ce
     * qu'on veut justement ramasser.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.stuck && !level().isClientSide && source.getEntity() instanceof Player) {
            dropAsItem();
        }
        return true;
    }

    /**
     * Une boite centree sur le crochet, comme celle de la bille de silicium.
     *
     * <p>Par defaut la boite d'une entite part de ses pieds, alors que ce modele se dessine centre
     * sur sa position : un crochet d'un demi-bloc portait donc sa boite un quart de bloc trop haut.
     * Et sa taille change en se plantant — {@link MagHookVisuals#FLY_SIZE} en vol,
     * {@link MagHookVisuals#HIT_SIZE} contre une paroi — ce que l'original obtenait par
     * {@code setSize} et le port par cette methode.
     */
    @Override
    protected AABB makeBoundingBox() {
        double half = (this.stuck ? MagHookVisuals.HIT_SIZE : MagHookVisuals.FLY_SIZE) / 2.0;
        Vec3 at = position();
        return new AABB(at.x - half, at.y - half, at.z - half,
                at.x + half, at.y + half, at.z + half);
    }

    /**
     * Un crochet en vol ne se vise pas ; un crochet plante, si.
     *
     * <p>C'est le {@code canBeCollidedWith} de l'original, et c'est la porte par laquelle le
     * deplacement magnetique le trouve : {@code MagMovementSkill.findTarget} ne retient que les
     * entites {@code isPickable}. Un crochet qui vole n'est donc jamais une ancre, ce qui serait
     * d'ailleurs injouable.
     */
    @Override
    public boolean isPickable() {
        return this.stuck;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_HIT, tag.getBoolean("isHit"));
        this.entityData.set(DATA_SIDE, tag.getInt("hitSide"));
        this.entityData.set(DATA_X, tag.getInt("hookX"));
        this.entityData.set(DATA_Y, tag.getInt("hookY"));
        this.entityData.set(DATA_Z, tag.getInt("hookZ"));

        if (isHit()) {
            // Releve plante : il reprend sa place contre la paroi, comme l'original le faisait dans
            // son `readEntityFromNBT`. C'est ce qui permet de retrouver son crochet apres un
            // rechargement, et de s'y accrocher encore.
            this.stuck = true;
            setDeltaMovement(Vec3.ZERO);
            setPos(snapPosition());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Les cinq noms de l'original, gardes tels quels.
        tag.putBoolean("isHit", isHit());
        tag.putInt("hitSide", this.entityData.get(DATA_SIDE));
        tag.putInt("hookX", this.entityData.get(DATA_X));
        tag.putInt("hookY", this.entityData.get(DATA_Y));
        tag.putInt("hookZ", this.entityData.get(DATA_Z));
    }

    /**
     * La naissance chez le client, et un piege de vanilla qui se corrige ici.
     *
     * <p>{@code Entity.recreateFromPacket} pose la position de l'entite neuve par
     * {@code moveTo(x, y, z)} — et cette surcharge-la remet les angles DU MOMENT a
     * {@code setOldPosAndRot()}, c'est-a-dire zero et zero, puisqu'elle ne recoit ceux du paquet
     * que juste <b>apres</b>. Un crochet neuf arrive donc chez son client avec un {@code yRotO} et
     * un {@code xRotO} a zero, et le rendu, qui interpole ses angles, le fait <b>tournoyer depuis le
     * sud pendant sa premiere image</b>. Le joueur l'a vu tout de suite : « le modele en lui meme
     * n'est pas le probleme [...] c'est juste qu'il tourne mal » — et d'autant plus dur a lire
     * qu'il file a deux blocs par tick.
     *
     * <p>Un {@code setOldPosAndRot()} de plus, une fois les vrais angles en place, et il nait
     * tourne comme il faut.
     */
    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setOldPosAndRot();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
