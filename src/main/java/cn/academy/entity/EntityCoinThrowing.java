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

    /** La piece telle que l'objet la jette : dans la main de son lanceur, vers le haut. */
    public EntityCoinThrowing(Level level, Player thrower) {
        this(ModEntities.COIN.get(), level);
        this.direct = thrower;
        this.entityData.set(DATA_THROWER, thrower.getId());
        this.initHt = thrower.getY();
        this.maxHt = this.initHt;
        this.fallSpeed = CoinToss.INIT_VEL;
        setPos(thrower.getX(), thrower.getY(), thrower.getZ());
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

    @Override
    public void tick() {
        super.tick();

        Player thrower = thrower();
        if (thrower == null || thrower.level() != this.level()) {
            // Plus personne a suivre : on rend la piece et on s'en va.
            settle();
            return;
        }

        // Elle ne tombe pas : elle est PORTEE par son lanceur, et seule sa hauteur vit de sa vie.
        // C'est le `KeepPosition` de l'original, et c'est ce qui la fait retomber dans la main meme
        // quand le joueur court ou vole.
        double x = thrower.getX();
        double z = thrower.getZ();

        fallSpeed -= CoinToss.GRAVITY;
        double y = getY() + fallSpeed;
        maxHt = Math.max(maxHt, y);

        // Elle passe a travers tout : l'original ne la deplacait que par sa vitesse, sans jamais
        // demander au monde si la place etait libre.
        setPos(x, y, z);

        // Elle a fini de retomber — ou elle traine depuis trop longtemps. Les deux la ramenent au
        // lanceur : c'est le `finishThrowing` de l'original.
        if (tickCount > CoinToss.MAX_LIFE || (y < thrower.getY() && fallSpeed < 0)) {
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
