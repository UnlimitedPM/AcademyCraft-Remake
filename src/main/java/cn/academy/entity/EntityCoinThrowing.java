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
 * {@link CoinToss#GRAVITY}, en <b>suivant son lanceur</b> : le X et le Z du dessin sont recolles sur
 * ceux du joueur a chaque image, et seule sa hauteur vit de sa vie — c'est le {@code KeepPosition} de
 * l'original, et c'est ce qui fait qu'on la voit retomber dans la main qui vient de la lancer, meme en
 * courant. Elle ne touche rien : elle traverse ce qu'elle rencontre, plafond compris.
 *
 * <p>Le railgun ne part que si elle est sur sa <b>retombee</b> — voir {@link CoinToss#isReady} : le
 * joueur la jette, attend le sommet, et tire a travers pendant qu'elle redescend. Une piece encore
 * dans sa montee ne sert a rien. Celle qu'on n'a pas tiree <b>revient dans l'inventaire</b>, comme
 * chez l'original : dans la main libre, sinon en s'empilant sur les autres, sinon en tombant au sol —
 * et jamais en creatif.
 *
 * <h2>Elle ne bouge pas : elle est DESSINEE ailleurs</h2>
 *
 * <p>C'est le point delicat, et le joueur l'a vu deux fois. L'entite reste ou elle a ete jetee ; tout
 * son vol est relu au dessin, par {@link #drawPosition}, a partir du lanceur et de l'age. Sans cela
 * deux positions se disputaient la piece — celle, locale, que le client recalculait, et celle que le
 * serveur envoyait par le reseau et que le client etale sur trois ticks — et elle tremblait des que le
 * joueur marchait (« quand je bouge la piece a l'air un peu buguee dans les airs »).
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

    /**
     * La hauteur du lancer, et c'est la SEULE chose qui voyage.
     *
     * <p>Le reste du vol se recalcule des deux cotes a partir de l'age : voir {@link CoinToss}. Le
     * client a donc besoin de cette hauteur-la — et d'elle seule — pour dessiner une piece qui monte
     * et retombe a la bonne place, et sans elle il la verrait partir du sol.
     *
     * <p>Sa valeur par defaut est {@code NaN}, et ce n'est pas un hasard : c'est l'etat « je ne sais
     * pas encore ou elle a ete lancee », celui du client pendant le tick qui separe le paquet de
     * creation de celui de sa donnee. Tant qu'elle y est, la piece ne bouge pas.
     *
     * <p>Un FLOAT et non un double : la 1.20.1 n'a pas de serialiseur de double (voir
     * {@code EntityDataSerializers}), et une hauteur de lancer n'a pas besoin de plus — c'est une
     * coordonnee de monde.
     */
    private static final EntityDataAccessor<Float> DATA_INIT_HT =
            SynchedEntityData.defineId(EntityCoinThrowing.class, EntityDataSerializers.FLOAT);

    /**
     * L'heure du monde ou elle a ete jetee : c'est l'AGE du dessin, et il vient de la, pas de
     * l'entite.
     *
     * <p>L'age d'une copie cliente repart de ZERO chaque fois qu'elle arrive : quand le joueur
     * s'eloigne trop loin, le serveur cesse de la lui envoyer, il l'oublie, et la copie suivante
     * nait avec un age de zero — la piece repartait donc du debut, voire s'evanouissait, des que le
     * joueur marchait vite (« l'animation est toujours buguee voire disparait avant de finir »).
     * L'heure du monde, elle, est la meme partout et ne recule jamais.
     */
    private static final EntityDataAccessor<Long> DATA_LAUNCH_TICK =
            SynchedEntityData.defineId(EntityCoinThrowing.class, EntityDataSerializers.LONG);

    /** Le lanceur, chez le serveur : la reference de l'original, et celle des faux joueurs. */
    private Player direct;

    /**
     * D'ou part la piece : DES PIEDS, comme l'original, et c'est l'affichage qui la remonte.
     *
     * <p>L'original la posait a {@code (posX, posY, posZ)} — les pieds — et son rendu la dessinait un
     * bloc plus haut, devant le visage ({@code translated(-0.63, 1, 0.30)}). C'est ce meme bloc qui
     * fait tout : la piece parait sortir de la main, et, comme elle revient a la hauteur d'ou elle est
     * partie, elle parait y rentrer. Le joueur l'a demande dans ces termes : « au final au visuel on
     * voit vraiment la piece partir de la main et reatterrir dans la main ».
     *
     * <p>La hauteur du VOL reste donc celle des pieds, et c'est celle-la qui decide quand le vol
     * finit. La remonter ici, comme le port l'a fait un moment, deplacait le point de retour — la
     * piece rentrait un demi-bloc trop haut.
     *
     * <p>ET ELLE PART DE LA MAIN, PAS DES PIEDS : le joueur a vu l'entite a la hauteur de ses
     * pieds, et la piece rampait le long de ses jambes avant de monter. Le vol par donc de
     * {@link #HAND_OFFSET} sous les yeux — la hauteur d'une main qui lance.
     */
    private static final double HAND_OFFSET = -0.4;

    /**
     * De combien elle vole a cote du lanceur : de biais et devant, comme l'affichage de l'original.
     *
     * <p>Sans ce decalage elle monterait et retomberait DANS le corps du joueur, et on ne verrait
     * d'elle que ce qui depasse de sa tete.
     */
    private static final double SIDE_OFFSET = 0.35;
    private static final double FORWARD_OFFSET = 0.35;

    /** L'axe autour duquel elle tourne en vol : rendu seulement, donc jamais synchronise. */
    private Vec3 axis = new Vec3(0, 1, 0);

    public EntityCoinThrowing(EntityType<? extends EntityCoinThrowing> type, Level level) {
        super(type, level);
        // L'axe se tire ici et non dans un initialiseur de champ : le hasard de l'entite n'est pose
        // qu'au constructeur, et un initialiseur s'executerait avant lui.
        this.axis = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1,
                random.nextDouble() * 2 - 1);
    }

    /** La piece telle que l'objet la jette : dans la main, vers le haut. */
    public EntityCoinThrowing(Level level, Player thrower) {
        this(ModEntities.COIN.get(), level);
        this.direct = thrower;
        this.entityData.set(DATA_THROWER, thrower.getId());
        double launch = thrower.getEyeY() + HAND_OFFSET;
        this.entityData.set(DATA_INIT_HT, (float) launch);
        this.entityData.set(DATA_LAUNCH_TICK, level.getGameTime());
        Vec3 at = followPoint(thrower);
        setPos(at.x, launch, at.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_THROWER, -1);
        // « On ne sait pas encore d'ou elle a ete lancee » : voir DATA_INIT_HT.
        this.entityData.define(DATA_INIT_HT, Float.NaN);
        this.entityData.define(DATA_LAUNCH_TICK, 0L);
    }

    /** La hauteur du lancer, ou {@code NaN} tant qu'elle n'est pas connue. */
    public double launchHeight() {
        return this.entityData.get(DATA_INIT_HT);
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

    /** Ou en est-elle de son vol, de 0 a 1 : la courbe de {@link CoinToss}, sur son age. */
    public double tossProgress() {
        return CoinToss.progress(tickCount);
    }

    /** Le railgun peut-il partir a travers elle ? Voir {@link CoinToss#READY}. */
    public boolean isReady() {
        return CoinToss.isReady(tickCount);
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

    /**
     * OU ELLE SE DESSINE, a cet age. Le rendu s'en sert, et rien d'autre.
     *
     * <p>L'entite, elle, ne bouge JAMAIS : elle reste ou elle a ete jetee. C'est la seule facon
     * d'eviter le tremblement que le joueur a vu — « quand je bouge la piece a l'air un peu buguee
     * dans les airs » —, qui venait de deux positions qui se disputaient la piece : celle, locale,
     * que le client recalculait chaque tick, et celle que le serveur lui envoyait par le reseau, que
     * le client etale sur trois ticks.
     *
     * <p>Le vol se relit donc ici, entierement : la position du lanceur, son decalage, et la hauteur
     * de l'age. Les deux cotes ont tout ce qu'il faut, et le dessin se fait a chaque image — c'est
     * exactement ce que faisait le client de l'original, qui tenait sa PROPRE copie de la piece.
     */
    public Vec3 drawPosition(float partialTick) {
        double launch = launchHeight();
        Player thrower = thrower();
        if (Double.isNaN(launch) || thrower == null) {
            // Rien de connu : on la laisse ou le paquet de creation l'a posee.
            return position();
        }
        Vec3 at = followPoint(thrower);
        return new Vec3(at.x, launch + CoinToss.height(flightAge(partialTick)), at.z);
    }

    /**
     * L'age du VOL, en ticks : depuis l'heure du monde ou elle a ete jetee.
     *
     * <p>C'est celui du dessin, et il ne depend pas de la copie qui le lit — voir
     * {@link #DATA_LAUNCH_TICK}. Il s'accorde avec {@code tickCount} chez le serveur (a un tick
     * pres), qui reste, lui, l'age de la DECISION : c'est celui que les tests posent.
     */
    public double flightAge(float partialTick) {
        return this.level().getGameTime() - this.entityData.get(DATA_LAUNCH_TICK) + partialTick;
    }

    /**
     * Un tick : le vol, et rien d'autre.
     *
     * <p>Elle ne se DEPLACE pas — voir {@link #drawPosition} — donc il n'y a rien a avancer ici. Ce
     * qui vit, c'est son age : c'est lui qui dit quand elle est assez retombee pour le tir
     * ({@link #isReady}) et quand son vol est fini. Et c'est le serveur, seul, qui la ramene.
     */
    @Override
    public void tick() {
        super.tick();

        // Chez le client, rien du tout : ni fin de vol, ni inventaire. Sa disparition lui arrive par
        // le reseau, et son dessin se fait dans le rendu.
        if (this.level().isClientSide) return;

        if (thrower() == null || CoinToss.finished(tickCount)) {
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
     *
     * <p>ET ELLE NE SUIT PAS LE JOUEUR, elle : l'entite reste la ou elle a ete jetee, c'est le DESSIN
     * qui la fait voler ({@link #drawPosition}). La boite doit donc etre large — un joueur qui sprinte
     * s'eloigne de huit blocs pendant les trente ticks du vol, et l'original suivait son porteur la ou
     * le port, lui, laisse la piece derriere. Trente-deux blocs couvrent le vol entier, meme en volant.
     */
    public static EntityCoinThrowing of(Player player) {
        if (player == null) return null;
        List<EntityCoinThrowing> coins = player.level().getEntitiesOfClass(EntityCoinThrowing.class,
                player.getBoundingBox().inflate(32.0), coin -> coin.isThrownBy(player));
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
