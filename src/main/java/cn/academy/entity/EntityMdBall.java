package cn.academy.entity;

import cn.academy.ModEntities;
import cn.academy.ability.AbilityCapability;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.meltdowner.MdBallVisuals;
import cn.academy.ability.meltdowner.RadiationMarks;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.MdRayPacket;
import cn.academy.ability.client.md.MdRayKind;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * La bille de plasma, portage d'{@code EntityMdBall} : le projectile du meltdowner.
 *
 * <h2>Ce qu'elle est vraiment</h2>
 *
 * <p>Elle ne vole pas. L'original la placait a chaque tick a {@code porteur + ecart}, l'ecart
 * etant tire une fois pour toutes au lancement — voir {@link MdBallVisuals}. Elle flotte donc
 * devant et autour de celui qui l'a lachee, comme un morceau d'orage tenu en laisse, et c'est
 * <b>d'elle</b> que part le trait : a deux ticks de sa fin, elle tire un rayon vers ce que son
 * porteur regarde.
 *
 * <p>C'est ce qui donne a la bombe a electrons son rythme : une seconde de charge (vingt ticks,
 * ou cinq seulement si le joueur a depasse 80 % d'experience), puis le trait. Le port faisait
 * autrement — une explosion instantanee d'un rayon de trois blocs au point vise — et c'etait
 * une invention : l'original ne frappait que ce que son rayon touchait, une seule cible.
 *
 * <h2>Ce qui voyage</h2>
 *
 * <p>Le porteur, l'ecart et la duree de vie, et rien d'autre : ce sont les trois choses que le
 * client ne peut pas deviner. Son {@code texID} et son scintillement, eux, vivent chez le client
 * seul, comme dans l'original — c'est ce qui fait qu'une bille ne ressemble jamais tout a fait
 * a une autre.
 */
public class EntityMdBall extends Entity {

    /** Le porteur, par son numero d'entite : c'est ce que l'original synchronisait. */
    private static final EntityDataAccessor<Integer> DATA_SPAWNER =
            SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.INT);

    /** L'ecart au porteur, en blocs, tel qu'il a ete tire au lancement. */
    private static final EntityDataAccessor<Float> DATA_SUB_X =
            SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SUB_Y =
            SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SUB_Z =
            SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.FLOAT);

    /** Sa duree de vie, en ticks : le client en a besoin pour sa courbe d'opacite. */
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.INT);

    /** Ticks vecus. */
    private int age;

    /** Le tir a-t-il deja eu lieu ? */
    private boolean fired;

    /**
     * Le porteur, tenu de cote chez le serveur.
     *
     * <p>C'est la reference de l'original, qui la gardait dans un champ et ne se servait de son
     * numero d'entite que pour le client. Ce n'est pas un detail : un joueur factice — celui des
     * tests de jeu — n'est pas dans la table des entites du niveau, et une bille qui ne le
     * retrouverait que par son numero disparaitrait aussitot posee.
     */
    private Player direct;

    /** Degats du trait, chez le serveur seulement : le client n'a rien a en faire. */
    private float damage;

    // --- L'ETAT D'IMAGE, CHEZ LE CLIENT SEUL ---

    /** L'image du coeur, tiree au hasard comme l'original. */
    private int texture;

    /** Le scintillement d'opacite : une marche au hasard sur [0, 1]. */
    private double wiggle = 0.8;

    /** Son acceleration courante, et l'instant de la derniere image. */
    private double wiggleAccel;
    private long wiggleFrame;

    /** L'instant de naissance, en millisecondes : les courbes de l'original sont en secondes. */
    private long birthMs;

    public EntityMdBall(EntityType<? extends EntityMdBall> type, Level level) {
        super(type, level);
        this.noCulling = true;
        // L'instant de naissance : c'est lui qui donne l'age de la bille au rendu, et les
        // courbes de l'original se lisent en secondes.
        this.birthMs = Util.getMillis();
    }

    /**
     * La bille telle qu'une competence la lache.
     *
     * @param spawner   celui qui la lache, et autour duquel elle se tiendra
     * @param lifeTicks sa duree de vie : vingt ticks, ou cinq pour une bille amelioree
     * @param damage    les degats du trait qu'elle tirera
     */
    public EntityMdBall(Level level, Player spawner, int lifeTicks, float damage) {
        this(ModEntities.MD_BALL.get(), level);
        this.direct = spawner;
        this.damage = damage;

        Vec3 sub = MdBallVisuals.subOffset(spawner.getYRot(), spawner.getRandom());
        setSub(sub);
        entityData.set(DATA_SPAWNER, spawner.getId());
        entityData.set(DATA_LIFE, lifeTicks);
        setPos(spawner.getX() + sub.x, spawner.getY() + sub.y, spawner.getZ() + sub.z);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_SPAWNER, 0);
        entityData.define(DATA_SUB_X, 0f);
        entityData.define(DATA_SUB_Y, 0f);
        entityData.define(DATA_SUB_Z, 0f);
        entityData.define(DATA_LIFE, MdBallVisuals.LIFE_TICKS);
    }

    /**
     * La bille ne survit pas a un rechargement.
     *
     * <p>L'original tuait la sienne des qu'on la relisait — son {@code readEntityFromNBT}
     * appelait {@code setDead} — et n'ecrivait rien du tout. C'est coherent : une bille vit une
     * seconde et n'a de sens qu'accrochee a son porteur, qui n'est jamais le meme apres un
     * rechargement.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** L'ecart au porteur, en blocs. Nul tant que la synchronisation n'est pas arrivee. */
    public Vec3 sub() {
        return new Vec3(entityData.get(DATA_SUB_X), entityData.get(DATA_SUB_Y),
                entityData.get(DATA_SUB_Z));
    }

    private void setSub(Vec3 sub) {
        entityData.set(DATA_SUB_X, (float) sub.x);
        entityData.set(DATA_SUB_Y, (float) sub.y);
        entityData.set(DATA_SUB_Z, (float) sub.z);
    }

    /** Sa duree de vie, en ticks. */
    public int lifeTicks() {
        return entityData.get(DATA_LIFE);
    }

    /** Le porteur, s'il est encore la — un joueur deconnecte laisse sa bille derriere lui. */
    public Player spawner() {
        if (direct != null) return direct.isRemoved() ? null : direct;
        Entity entity = level().getEntity(entityData.get(DATA_SPAWNER));
        return entity instanceof Player player ? player : null;
    }

    @Override
    public void tick() {
        super.tick();
        ++age;

        Player spawner = spawner();
        if (spawner == null) {
            // Sans porteur, la bille n'a plus de place : elle disparait, comme l'original
            // dont l'entite portait une reference morte.
            if (!level().isClientSide) discard();
            return;
        }

        Vec3 sub = sub();
        setPos(spawner.getX() + sub.x, spawner.getY() + sub.y, spawner.getZ() + sub.z);

        if (level().isClientSide) return;

        if (!fired && age >= lifeTicks() - MdBallVisuals.SHOT_DELAY) {
            fired = true;
            fire(spawner);
        }
        if (age >= lifeTicks()) discard();
    }

    /**
     * Le tir : un rayon vers ce que le porteur regarde, et rien de plus.
     *
     * <p>Le rayon part de la bille <b>remontee a la hauteur des yeux</b> — c'est le
     * {@code target.posY + player.eyeHeight} de l'original — et va jusqu'au point que le regard
     * touche, a quinze blocs. La bille elle-meme est retiree du trajet, sans quoi elle
     * s'arreterait sur ses propres semblables : l'original retirait aussi le porteur et ses
     * billes du selecteur.
     */
    private void fire(Player spawner) {
        Vec3 from = new Vec3(getX(), getY() + spawner.getEyeHeight(), getZ());
        Vec3 to = TargetingUtil.findImpactPoint(spawner,
                cn.academy.ability.meltdowner.ElectronBombSkill.RANGE);

        // Le moindre mur entre la bille et ce point arrete le tir : c'est ce que faisait le
        // {@code Raytrace.perform} de l'original, dont le rayon traversait le vide et rien
        // d'autre. La bille etant jusqu'a 1,3 bloc de cote, ce n'est pas un cas d'ecole.
        BlockHitResult wall = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, spawner));
        if (wall.getType() == HitResult.Type.BLOCK) to = wall.getLocation();

        Entity target = TargetingUtil.findEntityAlong(spawner, from, to,
                e -> !(e instanceof EntityMdBall));

        if (target != null) {
            target.hurt(spawner.damageSources().indirectMagic(spawner, spawner), damage);
            RadiationMarks.mark(target, spawner.getCapability(AbilityCapability.ABILITY_DATA)
                    .orElse(null));
        }

        AbilityNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                new MdRayPacket(MdRayKind.SMALL.name(), from, to));

        // Et le rayon s'annonce, comme le faisait l'entite de rayon de l'original en naissant.
        //
        // C'est le SERVEUR qui le joue, et c'est le seul chemin qui marche : le port avait
        // essaye un evenement de son fabrique a la main chez le client, et il ne s'entendait
        // pas du tout — le client cherche ses sons dans son propre fichier, et un evenement qui
        // n'est pas passe par le registre ne s'y resout pas. Le joueur l'a signale aussitot :
        // « quand on lance un laser, il n'y a aucun son ». Les autres sons du port passent tous
        // par le registre, et s'entendent.
        net.minecraft.sounds.SoundEvent sound =
                cn.academy.sound.SoundLookup.event(MdRayKind.SMALL.sound());
        if (sound != null) {
            level().playSound(null, from.x, from.y, from.z, sound,
                    net.minecraft.sounds.SoundSource.AMBIENT, MdRayKind.SMALL.soundVolume(), 1f);
        }
    }

    // --- LE RENDU, CHEZ LE CLIENT ---

    /** L'age de la bille, en secondes : c'est l'unite des courbes de l'original. */
    public double ageSeconds() {
        return (Util.getMillis() - birthMs) / 1000.0;
    }

    /** Le scintillement d'opacite, sur [0, 1]. */
    public double wiggle() {
        return wiggle;
    }

    /** L'image du coeur a dessiner. */
    public int texture() {
        return texture;
    }

    /**
     * Un pas de rendu : le scintillement et l'image du coeur.
     *
     * <p>Appele par le rendu, et non par le tick, parce que l'original le faisait par
     * <b>image</b> : une bille scintille d'autant plus vite que l'ecran est rapide, et c'est ce
     * qui lui donne son cote electrique. Les trois nombres — une chance sur huit de changer
     * d'acceleration, et une sur huit de changer d'image — sont ceux de son
     * {@code updateRenderTick}.
     */
    public void advanceRender(RandomSource random) {
        long now = Util.getMillis();

        if (wiggleFrame != 0) {
            double dt = (now - wiggleFrame) / 1000.0;
            if (random.nextInt(8) < MdBallVisuals.WIGGLE_CHANCE_IN_EIGHT) {
                wiggleAccel = MdBallVisuals.WIGGLE_MAX_ACCEL * (random.nextDouble() * 2 - 1);
            }
            wiggle = Math.min(1, Math.max(0, wiggle + wiggleAccel * dt));
        }
        wiggleFrame = now;

        if (random.nextInt(8) < MdBallVisuals.TEXTURE_CHANCE_IN_EIGHT) {
            texture = random.nextInt(MdBallVisuals.CORE_TEXTURES);
        }
    }
}
