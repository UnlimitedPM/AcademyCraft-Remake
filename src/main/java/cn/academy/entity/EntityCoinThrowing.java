package cn.academy.entity;

import cn.academy.ModEntities;
import cn.academy.ModItems;
import cn.academy.ability.electromaster.CoinToss;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * La piece lancee, portage d'{@code EntityCoinThrowing} : ce qu'on jette pour que le railgun
 * puisse partir.
 *
 * <h2>Ce qu'elle fait</h2>
 *
 * <p>Lancee d'une main, elle monte a {@link CoinToss#INIT_VEL} de vitesse et retombe sous
 * {@link CoinToss#GRAVITY}, en <b>suivant son lanceur</b> : l'original la laissait a la hauteur ou
 * elle etait et recollait son X et son Z sur ceux du joueur a chaque tick, et c'est ce qui fait
 * qu'on la voit retomber dans la main qui vient de la lancer, meme en courant. Elle ne touche rien :
 * elle traverse ce qu'elle rencontre, plafond compris.
 *
 * <p>Le railgun ne part que si elle est sur sa <b>retombee</b> — voir {@link CoinToss#isReady} : le
 * joueur la jette, attend le sommet, et tire a travers pendant qu'elle redescend. Une piece encore
 * dans sa montee ne sert a rien. Celle qu'on n'a pas tiree <b>revient dans l'inventaire</b>, comme
 * chez l'original : dans la main libre, sinon en s'empilant sur les autres, sinon en tombant au
 * sol — et jamais en creatif.
 *
 * <h2>Une seule piece par joueur</h2>
 *
 * <p>C'est l'original : {@code ItemCoin} refusait un second lancer tant que la premiere vivait. Le
 * port le lit dans le monde ({@link #of}) plutot que dans une table, donc rien ne survit a un
 * changement de monde.
 *
 * <h2>Ce qui voyage</h2>
 *
 * <p>Le lanceur seul ({@code DATA_THROWER}), et rien d'autre : sa hauteur, sa vitesse et son axe de
 * rotation se recalculent des deux cotes avec les memes nombres. Le porteur est tenu de cote chez
 * le serveur comme celui de la bille de plasma — un joueur factice, celui des tests, n'est pas dans
 * la table des entites du niveau, et une piece qui ne le retrouverait que par son numero ne le
 * suivrait pas.
 */
public class EntityCoinThrowing extends Entity {

    /** Le lanceur, par son numero d'entite : c'est ce que le client a besoin de savoir. */
    private static final EntityDataAccessor<Integer> DATA_THROWER =
            SynchedEntityData.defineId(EntityCoinThrowing.class, EntityDataSerializers.INT);

    /** Le lanceur, chez le serveur : la reference de l'original, et celle des faux joueurs. */
    private Player direct;

    /** La hauteur du lancer, et la plus haute atteinte. */
    private double initHt;
    private double maxHt;

    /**
     * D'ou part la piece : de la main, pas des pieds.
     *
     * <p>L'original la posait aux PIEDS ({@code setPosition(posX, posY, posZ)}) et ne la remontait
     * qu'a l'affichage, un bloc plus haut, devant le visage. Le port la fait partir de la ou elle se
     * voit — sous les yeux — parce que c'est de la que le vol doit se lire, et parce que c'est la
     * hauteur ou elle doit revenir : une piece lancee des pieds retombe dans les pieds.
     */
    private static final double EYE_OFFSET = -0.4;

    /**
     * Et de combien elle vole a cote du lanceur : de biais et devant, comme l'affichage de
     * l'original ({@code translated(-0.63, 1, 0.30)}).
     *
     * <p>Sans ce decalage elle monterait et retomberait DANS le corps du joueur, et on ne verrait
     * d'elle que ce qui depasse de sa tete.
     */
    private static final double SIDE_OFFSET = 0.35;
    private static final double FORWARD_OFFSET = 0.35;

    /** Sa vitesse verticale, menee a la main comme le {@code Rigidbody} de l'original. */
    private double fallSpeed;

    /** L'axe autour duquel elle tourne en vol : rendu seulement, donc jamais synchronise. */
    private Vec3 axis = new Vec3(0, 1, 0);

    public EntityCoinThrowing(EntityType<? extends EntityCoinThrowing> type, Level level) {
        super(type, level);
        // L'axe se tire ici et non dans un initialiseur de champ : le hasard de l'entite n'est pose
        // qu'au constructeur, et un initialiseur s'executerait avant lui.
        this.axis = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1,
                random.nextDouble() * 2 - 1);
    }

    /** La piece telle que l'objet la jette : au-dessus de la main, vers le haut. */
    public EntityCoinThrowing(Level level, Player thrower) {
        this(ModEntities.COIN.get(), level);
        this.direct = thrower;
        this.entityData.set(DATA_THROWER, thrower.getId());
        this.initHt = thrower.getEyeY() + EYE_OFFSET;
        this.maxHt = this.initHt;
        this.fallSpeed = CoinToss.INIT_VEL;
        Vec3 at = followPoint(thrower);
        setPos(at.x, this.initHt, at.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_THROWER, -1);
    }

    /** Qui l'a lancee, ou nul si on ne le trouve pas — piece orpheline, joueur deconnecte. */
    public Player thrower() {
        if (direct != null && !direct.isRemoved()) return direct;
        return this.level().getEntity(this.entityData.get(DATA_THROWER)) instanceof Player player
                ? player : null;
    }

    /** Cette piece est-elle celle de ce joueur ? */
    public boolean isThrownBy(Player player) {
        return thrower() == player;
    }

    /** Ou en est-elle de son vol, de 0 a 1 : la courbe de {@link CoinToss}. */
    public double tossProgress() {
        return CoinToss.progress(fallSpeed, maxHt, getY(), initHt);
    }

    /** Le railgun peut-il partir a travers elle ? Voir {@link CoinToss#READY}. */
    public boolean isReady() {
        return CoinToss.isReady(tossProgress());
    }

    /** Son axe de rotation. Jamais nul : un axe de longueur nulle ne tourne rien. */
    public Vec3 spinAxis() {
        return axis.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : axis;
    }

    /**
     * Le point du monde ou la piece suit son lanceur : de biais et devant lui, a sa hauteur.
     *
     * <p>PUR, et recalcule des deux cotes : le client n'a pas besoin qu'on le lui dise, le lanceur et
     * son regard suffisent.
     */
    private static Vec3 followPoint(Player thrower) {
        Vec3 look = thrower.getViewVector(1f);
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        // Un regard droit vers le haut ou le bas ne donne aucun cote : celui de l'original etait
        // tire au hasard, ici on prend l'est, faute de mieux.
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        return thrower.position().add(look.scale(FORWARD_OFFSET)).add(side.scale(SIDE_OFFSET));
    }

    @Override
    public void tick() {
        super.tick();

        Player thrower = thrower();
        if (thrower == null) {
            // Plus personne a suivre. Chez le serveur la piece revient a son proprietaire ; chez le
            // client on ne fait rien du tout : c'est le serveur qui decide quand elle finit, et une
            // piece qui se retire toute seule chez le client disparaitrait aussitot nee.
            if (!this.level().isClientSide) settle();
            return;
        }

        if (this.level().isClientSide) {
            // Le client ne fait que SUIVRE son lanceur : sa hauteur, elle, lui vient du serveur.
            // S'il la calculait lui-meme il repartirait de zero — sa copie est nee du paquet, elle
            // ne connait ni la hauteur du lancer ni sa vitesse — et la piece tomberait dans la
            // seconde qui suit son apparition.
            Vec3 at = followPoint(thrower);
            setPos(at.x, getY(), at.z);
            return;
        }

        // Le serveur, seul, mene le vol : elle monte, elle retombe, et c'est LUI qui la ramene.
        this.fallSpeed -= CoinToss.GRAVITY;
        double y = getY() + this.fallSpeed;
        this.maxHt = Math.max(this.maxHt, y);
        Vec3 at = followPoint(thrower);
        setPos(at.x, y, at.z);

        // Elle est revenue a sa hauteur de depart — ou elle traine depuis trop longtemps. Les deux
        // la ramenent au lanceur : c'est le `finishThrowing` de l'original.
        if (tickCount > CoinToss.MAX_LIFE || (y <= this.initHt && this.fallSpeed < 0)) {
            settle();
        }
    }

    /**
     * La piece a fini son vol : elle revient a son lanceur, et disparait.
     *
     * <p>Le retour n'est fait que chez le SERVEUR — c'est lui qui tient les inventaires — et jamais
     * en creatif, comme l'original. Un client qui arrive ici par ses propres moyens se contente
     * donc de la retirer, et la vraie piece, celle du serveur, rentre dans la main de son joueur.
     */
    public void settle() {
        if (!this.level().isClientSide) {
            Player thrower = thrower();
            if (thrower != null) returnCoin(thrower);
        }
        discard();
    }

    /**
     * Le railgun a tire a travers elle : elle est consommee, et ne revient pas.
     *
     * <p>C'est ce que fait {@code MSG_COIN_PERFORM} de l'original : la piece meurt <b>avant</b> que
     * le tir parte, et le tir part de toute facon — c'est elle qui lui sert de depart.
     */
    public void consume() {
        discard();
    }

    /** La piece qui revient : dans la main libre, sinon empilee, sinon par terre. */
    private void returnCoin(Player thrower) {
        if (thrower.getAbilities().instabuild) return;

        ItemStack coin = new ItemStack(ModItems.COIN.get());
        ItemStack main = thrower.getMainHandItem();
        if (main.isEmpty()) {
            thrower.setItemInHand(InteractionHand.MAIN_HAND, coin);
            return;
        }
        if (main.is(coin.getItem()) && main.getCount() < main.getMaxStackSize()) {
            main.grow(1);
            return;
        }
        if (!thrower.getInventory().add(coin)) {
            thrower.drop(coin, false);
        }
    }

    /**
     * La piece de ce joueur, s'il en a une en vol.
     *
     * <p>Le port la cherche dans le monde plutot que dans une table : c'est le
     * {@code ItemCoin.getPlayerCoin} de l'original, sans ce qui survivrait a un changement de monde.
     * La boite est large en hauteur — la piece monte a sept blocs au-dessus de la tete avant de
     * redescendre.
     */
    public static EntityCoinThrowing of(Player player) {
        if (player == null) return null;
        List<EntityCoinThrowing> coins = player.level().getEntitiesOfClass(EntityCoinThrowing.class,
                player.getBoundingBox().inflate(8.0), coin -> coin.isThrownBy(player));
        return coins.isEmpty() ? null : coins.get(0);
    }

    /** La piece de ce joueur, si elle est assez retombee pour que le railgun parte a travers. */
    public static EntityCoinThrowing ready(Player player) {
        EntityCoinThrowing coin = of(player);
        return coin != null && coin.isReady() ? coin : null;
    }

    // --- SAUVEGARDE ---

    /**
     * Une piece ne survit pas a un rechargement : elle rend la piece et disparait.
     *
     * <p>C'est l'original au mot pres — son {@code readFromNBT} faisait tomber un objet au sol et
     * mourait. Une piece dont on ne connait plus ni la hauteur de lancer ni le lanceur ne peut pas
     * reprendre son vol.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (!this.level().isClientSide) {
            spawnAtLocation(new ItemStack(ModItems.COIN.get()));
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        // Rien : ce qui reste d'une piece, c'est la piece elle-meme.
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // --- CE QU'ELLE EST POUR LE MONDE ---

    /** On ne la repousse pas, on ne la frappe pas : c'est une piece, pas une bete. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
