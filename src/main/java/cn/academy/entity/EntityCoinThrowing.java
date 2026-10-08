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
 * <p>Lancee d'une main, elle monte a {@link CoinToss#INIT_VEL} de vitesse — <b>plus l'elan du
 * joueur</b>, car l'original faisait {@code motionY = player.motionY} avant son {@code += INITVEL} :
 * une piece jetee en plein saut monte donc bien plus haut, et retombe bien plus tard, que celle jetee
 * a l'arret. Elle retombe sous {@link CoinToss#GRAVITY}, en <b>suivant son lanceur</b> : son X et son
 * Z sont recolles sur ceux du joueur a chaque tick, et seule sa hauteur vit de sa vie — c'est le
 * {@code KeepPosition} de l'original, et c'est ce qui fait qu'on la voit retomber dans la main qui
 * vient de la lancer, meme en courant. Elle ne touche rien : elle traverse ce qu'elle rencontre,
 * plafond compris.
 *
 * <p>ET ELLE S'ARRETE AVEC CETTE MAIN, pas a son point de depart : des qu'elle redescend au niveau de
 * la main de son lanceur <b>a cet instant</b>. Un joueur qui monte la rattrape donc plus tot, et un
 * joueur qui tombe la laisse descendre avec lui — c'est le {@code posY < player.posY} de l'original,
 * et ce que le joueur a demande : « l'entite retombe juste a son point d'origine alors que
 * normalement, si je monte en meme temps, l'entite est censee s'arreter avec ».
 *
 * <p>Le railgun ne part que si elle est sur sa <b>retombee</b> — voir {@link CoinToss#isReady} : le
 * joueur la jette, attend le sommet, et tire a travers pendant qu'elle redescend. Une piece encore
 * dans sa montee ne sert a rien. Celle qu'on n'a pas tiree <b>revient dans l'inventaire</b>, comme
 * chez l'original : dans la main libre, sinon en s'empilant sur les autres, sinon en tombant au sol —
 * et jamais en creatif.
 *
 * <h2>Elle BOUGE : c'est l'ENTITE qui suit son lanceur, pas la piece qui s'invente un vol</h2>
 *
 * <p>Le port a commence par la figer la ou elle naissait, et par relire tout son vol au DESSIN — une
 * position fabriquee par le client, a cote de celle que le serveur envoyait, et le joueur l'a vue
 * trembler des qu'il marchait (« quand je bouge la piece a l'air un peu buguee dans les airs »).
 * L'original ne fait pas cela : son {@code KeepPosition} recolle l'entite sur son lanceur, sa
 * {@code Rigidbody} la fait monter, et le rendu ne fait que TOURNER une piece sur place. C'est ce que
 * fait le port, et c'est ce qui rend l'animation fluide : une entite qui bouge est interpolee par le
 * client entre deux positions recues, a chaque image, sans que personne n'ait a recalculer son vol.
 *
 * <p>Le rendu ne dessine donc plus rien a cote d'elle : il la dessine la ou est l'entite. Les
 * decalages de l'original — de biais, devant, a hauteur de main — sont dans sa POSITION, la ou tout le
 * monde les voit : le serveur, les autres joueurs, et le client qui l'interpole.
 *
 * <h2>Une seule piece par joueur</h2>
 *
 * <p>C'est l'original : {@code ItemCoin} refusait un second lancer tant que la premiere vivait. Le
 * port le lit dans le monde ({@link #of}) plutot que dans une table, donc rien ne survit a un
 * changement de monde.
 *
 * <h2>Ce qui voyage</h2>
 *
 * <p>Sa POSITION, d'abord : c'est du mouvement d'entite ordinaire, donc le reseau s'en occupe tout
 * seul, et c'est ce qui la rend fluide. Le lanceur ensuite ({@code DATA_THROWER}) : le client s'en
 * sert pour repondre a la seule question qu'il lui pose — ce joueur a-t-il deja une piece en l'air ?
 * Le porteur est tenu de cote chez le serveur comme celui de la bille de plasma — un joueur factice,
 * celui des tests, n'est pas dans la table des entites du niveau, et une piece qui ne le retrouverait
 * que par son numero ne le suivrait pas.
 */
public class EntityCoinThrowing extends Entity {

    /**
     * Le lanceur, par son numero d'entite.
     *
     * <p>C'est tout ce que le client a besoin de savoir d'elle : il s'en sert pour la reconnaitre
     * ({@link #isThrownBy}) et refuser un second lancer. Le vol, lui, ne se relit plus nulle part — il
     * lui arrive par le mouvement de l'entite.
     */
    private static final EntityDataAccessor<Integer> DATA_THROWER =
            SynchedEntityData.defineId(EntityCoinThrowing.class, EntityDataSerializers.INT);

    /**
     * La hauteur d'ou la piece est partie : la main qui vient de la lancer, et l'ELAN qu'elle a recu.
     *
     * <p>Ces deux nombres, plus l'heure du lancer, sont tout ce que le vol demande — et c'est aussi
     * tout ce que le CLIENT doit savoir pour le refaire lui-meme. Ils voyagent donc, et le client n'a
     * rien d'autre a recevoir : voir {@link #tick}, ou les deux cotes recalculent la meme position.
     *
     * <p>Un FLOAT et non un double : la 1.20.1 n'a pas de serialiseur de double (voir
     * {@code EntityDataSerializers}), et une hauteur comme une vitesse de lancer n'ont pas besoin de
     * plus.
     */
    private static final EntityDataAccessor<Float> DATA_INIT_HT =
            SynchedEntityData.defineId(EntityCoinThrowing.class, EntityDataSerializers.FLOAT);

    /** Et l'elan du lancer : {@link CoinToss#INIT_VEL} plus la vitesse verticale du joueur. */
    private static final EntityDataAccessor<Float> DATA_LAUNCH_VEL =
            SynchedEntityData.defineId(EntityCoinThrowing.class, EntityDataSerializers.FLOAT);

    /**
     * L'heure du MONDE ou elle a ete jetee : c'est l'AGE du vol, et il est le meme partout.
     *
     * <p>Et non l'age de la copie qui le lit : celui-la repart de ZERO chaque fois que le serveur
     * renvoie la piece a un joueur qui s'etait eloigne, donc un client qui arrive en retard rejouerait
     * tout le vol depuis le debut. L'heure du monde, elle, ne recule jamais.
     */
    private static final EntityDataAccessor<Long> DATA_LAUNCH_TICK =
            SynchedEntityData.defineId(EntityCoinThrowing.class, EntityDataSerializers.LONG);

    /** Le lanceur, chez le serveur : la reference de l'original, et celle des faux joueurs. */
    private Player direct;

    /**
     * D'ou part la piece : de la MAIN, et non des pieds.
     *
     * <p>L'original la posait a la hauteur des pieds ({@code player.posY}) et son rendu la remontait
     * d'un bloc, devant le visage ({@code translated(-0.63, 1, 0.30)}). Le port, lui, pose des le
     * depart la position qui compte : le joueur voyait l'entite a ses pieds et la piece ramper le
     * long de ses jambes avant de monter. Elle part donc de {@link #HAND_OFFSET} sous les yeux — la
     * hauteur d'une main qui lance — et, comme elle y revient, elle parait y rentrer : « au final au
     * visuel on voit vraiment la piece partir de la main et reatterrir dans la main ».
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
        //
        // ET C'EST UNE CULBUTE, pas n'importe quel axe : la piece est un disque pose a plat, donc un
        // axe proche de la verticale la ferait tourner SUR ELLE-MEME — le disque ne bougerait pas et
        // l'image tournerait sur place, ce qui ne se lit pas comme une piece qu'on lance. L'original
        // tirait son axe au hasard dans les trois directions ; celui-ci reste dans le plan de la
        // piece, avec un peu de travers, donc elle bascule toujours.
        double tilt = random.nextDouble() * Math.PI * 2.0;
        this.axis = new Vec3(Math.cos(tilt), (random.nextDouble() - 0.5) * 0.4, Math.sin(tilt));
    }

    /** La piece telle que l'objet la jette : dans la main, vers le haut. */
    public EntityCoinThrowing(Level level, Player thrower) {
        this(ModEntities.COIN.get(), level);
        this.direct = thrower;
        this.entityData.set(DATA_THROWER, thrower.getId());
        // Ce que le vol demande : d'ou elle part, avec quel elan, et a quelle heure du monde. L'elan
        // est l'original au mot pres (motionY = player.motionY, puis += INITVEL) : une piece jetee en
        // plein saut monte donc bien plus haut que celle jetee a l'arret.
        this.entityData.set(DATA_INIT_HT, (float) handHeight(thrower));
        this.entityData.set(DATA_LAUNCH_VEL,
                (float) (CoinToss.INIT_VEL + thrower.getDeltaMovement().y));
        this.entityData.set(DATA_LAUNCH_TICK, level.getGameTime());
        Vec3 at = followPoint(thrower, 1f);
        setPos(at.x, launchHeight(), at.z);
    }

    /** Ou est la main de son lanceur : la hauteur des yeux moins {@link #HAND_OFFSET}. */
    private static double handHeight(Player thrower) {
        return thrower.getEyeY() + HAND_OFFSET;
    }

    /** La hauteur d'ou elle est partie : la main qui venait de la lancer. */
    private double launchHeight() {
        return this.entityData.get(DATA_INIT_HT);
    }

    /** L'elan qu'elle a recu : {@link CoinToss#INIT_VEL} plus la vitesse verticale du joueur. */
    private double launchSpeed() {
        return this.entityData.get(DATA_LAUNCH_VEL);
    }

    /**
     * L'age du vol, en ticks : depuis l'heure du MONDE ou elle a ete jetee — voir
     * {@link #DATA_LAUNCH_TICK}.
     */
    private double flightAge() {
        return this.level().getGameTime() - this.entityData.get(DATA_LAUNCH_TICK);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_THROWER, -1);
        this.entityData.define(DATA_INIT_HT, 0f);
        this.entityData.define(DATA_LAUNCH_VEL, (float) CoinToss.INIT_VEL);
        this.entityData.define(DATA_LAUNCH_TICK, 0L);
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
        return CoinToss.progress(tickCount, launchSpeed());
    }

    /** Le railgun peut-il partir a travers elle ? Voir {@link CoinToss#READY}. */
    public boolean isReady() {
        return CoinToss.isReady(tickCount, launchSpeed());
    }

    /** Son axe de rotation. Jamais nul : un axe de longueur nulle ne tourne rien. */
    public Vec3 spinAxis() {
        return axis.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : axis;
    }

    /**
     * Le point du monde ou la piece suit son lanceur : de biais et devant lui.
     *
     * <p>Seul le LACET compte, comme chez l'original ({@code rotationYaw}) : la piece reste devant et
     * de biais quoi que le joueur regarde, et lever les yeux ne la fait pas monter plus haut. La
     * hauteur, elle, n'est pas ici — c'est celle du vol, posee a chaque tick par {@link #tick}.
     *
     * <p>Le temps partiel sert au RENDU : le serveur pose la position au tick (1), et le client relit
     * le suivi a chaque image pour que la piece ne traine pas derriere un joueur qui court.
     */
    public static Vec3 followPoint(Player thrower, float partialTick) {
        Vec3 look = thrower.getViewVector(partialTick);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        // Un regard droit vers le haut ou le bas n'a plus de direction : on prend le sud, le lacet
        // zero de Minecraft.
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0, flat.x);
        return thrower.getPosition(partialTick)
                .add(flat.scale(FORWARD_OFFSET)).add(side.scale(SIDE_OFFSET));
    }

    /**
     * Un tick : LE VOL, et il est calcule DES DEUX COTES, sur la meme horloge.
     *
     * <p>Elle suit son lanceur en X et Z — le {@code KeepPosition} de l'original — et sa hauteur est
     * celle de son age — sa {@code Rigidbody} : {@link CoinToss#height} de l'age du vol, pour l'elan
     * du lancer.
     *
     * <p>ET LE CLIENT LE FAIT AUSSI, avec les memes nombres : l'heure du monde, la hauteur de depart
     * et l'elan, qui voyagent ({@link #DATA_LAUNCH_TICK}). C'est ce qui rend l'animation fluide : les
     * deux cotes posent la piece exactement au meme endroit, donc les positions du reseau ne
     * contredisent plus celles que le client calcule, et le rendu n'a plus qu'a interpoler deux
     * positions justes — c'est ce qui manquait, et c'est ce que le joueur demandait en voulant « une
     * animation plus fluide ».
     *
     * <p>Elle S'ARRETE AVEC LA MAIN : des qu'elle redescend au niveau de la main de son lanceur
     * <b>maintenant</b> — voir {@link CoinToss#hasLanded} — un joueur qui monte la rattrape donc plus
     * tot. C'est le filet de {@link CoinToss#MAX_LIFE} qui arrete celle qu'il ne rattrape pas. Les
     * DECISIONS, elles, restent au serveur, et se lisent sur {@code tickCount} : c'est l'age que les
     * tests posent pour avancer un vol d'un coup.
     */
    @Override
    public void tick() {
        super.tick();

        Player thrower = thrower();
        if (thrower == null) {
            // Une piece orpheline : chez le client elle reste ou elle est, chez le serveur elle rend
            // la piece et disparait.
            if (!this.level().isClientSide) settle();
            return;
        }

        Vec3 at = followPoint(thrower, 1f);
        setPos(at.x, launchHeight() + CoinToss.height(flightAge(), launchSpeed()), at.z);

        if (this.level().isClientSide) return;

        double handDrop = handHeight(thrower) - launchHeight();
        if (CoinToss.hasLanded(tickCount, launchSpeed(), handDrop)
                || tickCount > CoinToss.MAX_LIFE) {
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
     * <p>La boite est large : elle ne coute qu'une recherche, et elle pardonne les cas ou la piece
     * n'est pas exactement la ou on l'attend — un vol ne dure que trente ticks, et une piece perdue
     * vaudrait une piece perdue pour de bon.
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
