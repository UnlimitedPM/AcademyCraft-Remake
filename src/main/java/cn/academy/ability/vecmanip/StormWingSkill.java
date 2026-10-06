package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Les ailes de tempete, portage de {@code StormWing} : la charge ouvre des ailes de vent, et
 * le joueur vole ou son regard le mene.
 *
 * <p>C'est la competence la plus <b>physique</b> de vecmanip, et la seule du port dont une
 * partie vit chez le client. L'original faisait exactement cela : son contexte existait des
 * deux cotes, et c'est le {@code MSG_TICK} client qui posait la vitesse du joueur a chaque
 * tick. Le serveur, lui, ne s'occupait que de la note — le cout, l'experience, et les degats
 * que les ailes font autour d'elles. Le port garde ce partage, parce qu'il est aussi celui
 * de la 1.20.1 : un joueur dont le vol est autorise (voir plus bas) voit ses deplacements
 * acceptes par le serveur.
 *
 * <h2>Deux temps</h2>
 *
 * <p>L'appui ouvre une <b>charge</b> de 70 ticks au depart, 30 au maximum d'experience : elle
 * ne coute rien, et elle fait <b>flotter</b> — la vitesse verticale est poussee de 0,078 par
 * tick tant qu'il n'y a pas de bloc sous les pieds, et posee a 0,1 quand il y en a un. C'est
 * assez pour tenir en l'air sans monter, et c'est ce que l'original faisait pendant que les
 * ailes s'ouvraient.
 *
 * <p>Les ailes ouvertes, chaque touche de deplacement tenue pousse le joueur dans cette
 * direction, <b>par pas de 0,16</b> au lieu d'une vitesse posee d'un coup : le depart est mou
 * et l'arrivee rapide. Le vol est <b>a plat</b> : le regard ne donne que le cap, et l'altitude
 * ne se commande qu'a l'espace (monter) et a l'accroupissement (descendre) — regarder ses pieds
 * en avancant ne fait donc pas piquer. La vitesse va de 1,4 a 3,6 blocs par tick selon
 * l'experience : sous 45 % d'experience les ailes sont lentes (0,7), au-dela elles sont rapides
 * (1,2), et les deux grandissent jusqu'a 3 fois.
 *
 * <p>Le joueur a demande quatre choses a ce vol, et elles tiennent dans la direction : les quatre
 * touches <b>se somment</b> — avant et droite donnent la diagonale, la ou l'original ne gardait
 * que la derniere touche pressee —, l'<b>espace fait monter</b> et l'accroupissement descend, la
 * montee <b>reste verticale</b> au lieu de suivre le regard, et l'avance ne <b>pique plus</b> :
 * « si je regarde vers le bas avec les ailes actives et que j'avance, que ca ne me fasse pas
 * descendre du tout, pour aller vers le bas je veut appuyer sur shift ». La direction entiere est
 * ensuite normalisee, donc une diagonale ou une montee va exactement aussi vite qu'un cap franc.
 *
 * <p>Sans touche tenue, les ailes <b>flottent</b> comme pendant la charge : c'est ainsi qu'on
 * se pose au milieu de l'air, et c'est encore l'original.
 *
 * <h2>Ce que le vol coute, et ce qu'il casse</h2>
 *
 * Chaque tick d'ailes ouvertes verse 0,00005 d'experience et paie 40 a 25 CP et 10 a 2,33 de
 * surcout ; quand l'une des deux reserve manque, le vol s'arrete. Les CP sont ceux de l'original,
 * bruts : sa reserve va de 1800 a 8000 selon le niveau, donc ils s'y lisent sans conversion — le
 * detour par un plafond de 100 et un facteur 28 qui les rendait lisibles a ete retire avec le
 * plafond (voir {@code AbilityData.BASE_MAX_CONTROL_POINT}). Le <b>surcout</b>, lui, a ete
 * <b>abaisse</b> a la demande du joueur : l'original montait de 10 a 7, ce qui remplissait la barre
 * plus vite qu'on ne pouvait en profiter, et il ne monte maintenant qu'a un tiers de cela — voir
 * {@link #OVERLOAD_MAX_EXP}.
 *
 * <p>La charge, elle, est gratuite. Le surcout d'ouverture est le <b>premier tick de vol</b>, paye
 * a l'appui : c'est un ecart assume, l'original ne facturant rien tant que les ailes ne s'etaient
 * pas ouvertes.
 *
 * <p>Sous 15 % d'experience, les ailes sont <b>maladroites</b> : quarante positions sont
 * tirees dans dix blocs autour du joueur, et tout ce qui y a une durete comprise entre 0 et
 * 0,3 casse — herbe, neige, feuilles. Rien au-dela : c'est ce qui fait qu'un debutant ouvre
 * les ailes dans un nuage de debris et qu'un veteran, non.
 *
 * <p>Et a <b>pleine experience</b>, l'ouverture des ailes <b>repousse</b> : tout ce qui se
 * trouve a six blocs est envoye au loin, d'une vitesse tiree entre 0,5 et 1. L'original y
 * tirait aussi une portee de 0,9 a 1,2 — que la normalisation qui suivait annulait, donc le
 * port ne la garde pas.
 *
 * <h2>Le vol libre</h2>
 *
 * <p>L'ouverture du maintien met {@code mayfly} a vrai, et la fin le rend : c'est le
 * {@code allowFlying} de l'original, et c'est ce qui autorise le serveur a accepter le
 * deplacement — sans lui, un joueur en survie verrait sa position corrigee a chaque tick.
 *
 * <p>Non portes : les ailes elles-memes ({@code StormWingEffect}), le son de boucle, les
 * particules qui entourent le joueur, et le son de casse des blocs. La competence de
 * l'original s'ouvrait et se fermait a la meme touche ; le port la tient, comme les autres
 * maintiens — le systeme de presets de l'original reste a porter, et c'est lui qui reglera
 * la question.
 */
public class StormWingSkill extends Skill {

    /** L'ouverture des ailes : 70 ticks au depart, 30 au maximum d'experience. */
    public static final float CHARGE_MIN_EXP = 70f;
    public static final float CHARGE_MAX_EXP = 30f;

    /** La vitesse du vol : un facteur, fois 2 a 3 blocs par tick. */
    public static final double SLOW_FACTOR = 0.7;
    public static final double FAST_FACTOR = 1.2;

    /** Et le facteur rapide a partir de 45 % d'experience, comme l'original. */
    public static final float FAST_EXP = 0.45f;

    /** Le pas d'acceleration d'une composante, le {@code ACCEL} de l'original. */
    public static final double ACCEL = 0.16;

    /** Le flottement : 0,078 par tick, et 0,1 pour se tenir a hauteur. */
    public static final double LIFT = 0.078;
    public static final double HOVER = 0.1;

    /** Un tick de vol : 40 a 25 CP, et 10 a 2,33 de surcout — voir {@link #OVERLOAD_MAX_EXP}. */
    public static final float CP_MIN_EXP = 40f;
    public static final float CP_MAX_EXP = 25f;
    public static final float OVERLOAD_MIN_EXP = 10f;

    /**
     * Le surcout d'un tick de vol a pleine experience : un tiers de ce qu'il valait.
     *
     * <p>L'original montait de 10 a 7, et le joueur a demande une courbe : « le cout des ailes soit
     * moins eleve en terme d'overload, parce que la maintenant, on accumule l'overload tellement
     * vite qu'on n'a pas vraiment le temps d'en profiter ». A 0 % d'experience rien ne change — le
     * vol coute toujours 10 par tick, comme autrefois — et a 100 % il coute trois fois moins
     * qu'avant, soit un tiers de 7. La barre de surcout se remplit donc trois fois plus lentement
     * quand on sait voler, ce qui est precisement ce que l'experience doit acheter.
     *
     * <p>Les CP, eux, ne bougent pas : l'original les tenaient deja, et c'est la surcharge qui
     * empechait de profiter du vol — la reserve se vide plus doucement qu'elle ne se remplit.
     */
    public static final float OVERLOAD_MAX_EXP = 7f / 3f;

    /** 0,00005 d'experience par tick de vol. */
    public static final float EXP_PER_TICK = 0.00005f;

    /** Sous cette experience, les ailes cassent ce qu'elles trouvent. */
    public static final float BEGINNER = 0.15f;

    /** Quarante positions tirees dans dix blocs, sur des blocs de durete au plus 0,3. */
    public static final int BREAK_ATTEMPTS = 40;
    public static final int BREAK_RANGE = 10;
    public static final float BREAK_HARDNESS = 0.3f;

    /** Le souffle de l'ouverture, a pleine experience : six blocs, 0,5 a 1 de vitesse. */
    public static final double BLAST_RANGE = 6.0;
    public static final double BLAST_MIN = 0.5;
    public static final double BLAST_MAX = 1.0;

    /** La recharge : 30 ticks au depart, 10 au maximum. */
    public static final int COOLDOWN_MIN_EXP = 10;
    public static final int COOLDOWN_MAX_EXP = 30;

    /** Le rayon du trace qui cherche le sol sous les pieds, comme l'original. */
    private static final double GROUND_LIFT = 0.5;
    private static final double GROUND_DROP = -0.3;

    public StormWingSkill() {
        super("storm_wing", 3);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    /** Le temps d'ouverture des ailes. */
    public float chargeTime(AbilityData data) {
        return lerp(CHARGE_MIN_EXP, CHARGE_MAX_EXP, data.getSkillExp(this));
    }

    /** Le meme, en ticks entiers : c'est a ce compte que les ailes s'ouvrent. */
    public int chargeTicks(AbilityData data) {
        return (int) chargeTime(data);
    }

    /** Les ailes sont-elles ouvertes a ce tick ? */
    public boolean opened(AbilityData data, int heldTicks) {
        return heldTicks > chargeTicks(data);
    }

    /** Et est-ce le tick qui vient de les ouvrir ? C'est la que le souffle part. */
    public boolean opensThisTick(AbilityData data, int heldTicks) {
        return heldTicks == chargeTicks(data) + 1;
    }

    /**
     * La vitesse du vol.
     *
     * L'original avait deux regimes : sous 45 % d'experience les ailes poussent a 0,7, au-dela
     * a 1,2 — et les deux grandissent de 2 a 3 fois avec l'experience. Le saut de 1,4 a 2,4
     * blocs par tick se sent passer, et c'est celui de l'original.
     */
    public float speed(AbilityData data) {
        float exp = data.getSkillExp(this);
        double factor = exp < FAST_EXP ? SLOW_FACTOR : FAST_FACTOR;
        return (float) (factor * lerp(2f, 3f, exp));
    }

    /** Ce qu'un tick d'ailes ouvertes coute a la reserve. */
    public float consumption(AbilityData data) {
        return lerp(CP_MIN_EXP, CP_MAX_EXP, data.getSkillExp(this));
    }

    /**
     * Le client rejoue ce chiffre pour ses nombres : voir {@link Skill#getTickUpkeep}.
     *
     * <p>La charge elle-meme ne coute rien — l'original ne facturait que les ailes ouvertes —, donc
     * le client ne compte qu'a partir du tick ou elles s'ouvrent, celui que le serveur connait.
     */    @Override
    public float getTickUpkeep(AbilityData data, int ticks) {
        return opened(data, ticks) ? consumption(data) : 0f;
    }

    /** Ce qu'il charge a la surcharge. */
    public float overload(AbilityData data) {
        return lerp(OVERLOAD_MIN_EXP, OVERLOAD_MAX_EXP, data.getSkillExp(this));
    }

    /**
     * Le surcout qu'un tick d'ailes ouvertes ajoute, et que le client rejoue : voir
     * {@link Skill#getTickUpkeepOverload}.
     *
     * <p>C'est la seule competence du port dans ce cas : elle est aussi la seule a ajouter du
     * surcout en vol. Sans ce chiffre, le client ne l'apprenait que par les synchronisations, et la
     * barre montait « d'un certain nombre a chaque fois ».
     */
    @Override
    public float getTickUpkeepOverload(AbilityData data, int ticks) {
        return opened(data, ticks) ? overload(data) : 0f;
    }

    /** Les ailes sont-elles encore maladroites ? */
    public boolean clumsy(AbilityData data) {
        return data.getSkillExp(this) < BEGINNER;
    }

    /** L'ouverture des ailes repousse-t-elle ? A pleine experience seulement. */
    public boolean blows(AbilityData data) {
        return data.getSkillExp(this) >= 1f;
    }

    // ------------------------------------------------------------------
    // Le vol, et ses calculs
    // ------------------------------------------------------------------

    /**
     * Rapproche une composante de la valeur voulue, d'au plus {@code limit}.
     *
     * Portage du {@code move} de l'original : c'est ce qui rend le depart mou et l'arrivee
     * rapide, au lieu d'une vitesse posee d'un coup. Un vecteur nul n'a pas de sens ici, donc
     * un ecart plus petit que le pas est simplement comble.
     */
    public static double step(double from, double to, double limit) {
        double delta = to - from;
        if (Math.abs(delta) <= limit) return to;
        return delta > 0 ? from + limit : from - limit;
    }

    /**
     * Le reperage d'une direction locale dans le monde, par le regard.
     *
     * C'est le {@code worldSpace} de l'original : la direction part dans le repere du joueur,
     * puis subit l'inclinaison <b>puis</b> le lacet, tous deux en sens inverse. L'avant et
     * l'arriere suivent donc le tangage — on vole ou l'on regarde — alors que la gauche et la
     * droite restent horizontales, l'axe des X ne tournant pas sous une inclinaison.
     *
     * <p>Le vol ne s'en sert plus qu'avec un <b>tangage nul</b> — voir {@link #flightDirection},
     * qui explique pourquoi —, mais la fonction reste entiere : c'est celle de l'original, et son
     * inclinaison est ce qui la rend vraie.
     */
    public static Vec3 worldSpace(float yawDegrees, float pitchDegrees, Vec3 local) {
        return local.xRot((float) Math.toRadians(-pitchDegrees))
                .yRot((float) Math.toRadians(-yawDegrees));
    }

    /**
     * La direction locale d'un jeu de touches : +1 par touche tenue, -1 par touche opposee.
     *
     * <p>Dans le repere du joueur, +X est sa <b>gauche</b>, +Y son <b>haut</b> et +Z son
     * <b>avant</b> — c'est le repere que {@link #worldSpace} fait tourner. Les touches se
     * <b>somment</b> : avant et droite donnent la diagonale, la ou l'original ne retenait que la
     * <b>derniere</b> touche pressee, et le joueur a demande la difference — « si on appuie pour
     * aller a droite, ca nous pousse vers la droite, et donc on avance plus en avant en meme temps,
     * meme si on appuie sur les 2 en meme temps ».
     *
     * <p>Et deux touches qui se repondent s'annulent : avant et arriere ensemble ne poussent nulle
     * part, ce qui est le comportement attendu d'un clavier.
     */
    public static Vec3 localWish(boolean forward, boolean back, boolean left, boolean right,
                                 boolean up, boolean down) {
        return new Vec3((left ? 1 : 0) - (right ? 1 : 0),
                (up ? 1 : 0) - (down ? 1 : 0),
                (forward ? 1 : 0) - (back ? 1 : 0));
    }

    /**
     * La direction du vol, dans le monde : <b>a plat</b>, plus la montee et la descente.
     *
     * <p>Avancer suit le lacet du regard et <b>rien d'autre</b> : le tangage n'entre pas dans la
     * direction. Le joueur a d'abord demande le contraire — l'avant suivait alors un regard qui
     * pique —, puis l'a redemande autrement : « si je regarde vers le bas avec les ailes actives et
     * que j'avance, que ca ne me fasse pas descendre du tout, pour aller vers le bas je veut
     * appuyer sur shift ». Regarder ses pieds en vol ne pique donc plus, et l'altitude ne se
     * commande qu'aux deux touches verticales, comme en vol creatif.
     *
     * <p>La <b>montee et la descente</b> sont verticales dans le <b>monde</b> : espace monte,
     * accroupissement descend, et le reste suit le cap. C'est le reperage de {@link #worldSpace} avec
     * un tangage nul, et c'est la seule facon de ne pas se tromper : dans le repere du regard,
     * regarder ses pieds et appuyer sur espace ferait <b>plonger</b>, puisque le haut du regard est
     * alors l'avant.
     */
    public static Vec3 flightDirection(float yawDegrees, Vec3 local) {
        return worldSpace(yawDegrees, 0f, local);
    }

    /**
     * La vitesse verticale du flottement.
     *
     * Le {@code motionY += 0.078} de l'original, qui compense la gravite du client (0,08) sans
     * jamais la depasser : on monte doucement. Un bloc sous les pieds, et la vitesse est
     * <b>posee</b> a 0,1 — c'est ce qui fait tenir le joueur a hauteur quand il rase le sol,
     * au lieu de le faire rebondir.
     */
    public static double hoverVelocity(boolean groundBelow, double motionY) {
        return groundBelow ? HOVER : motionY + LIFT;
    }

    /**
     * La vitesse donnee a ce que l'ouverture des ailes trouve.
     *
     * L'original prenait la direction de la tete de la cible vers le joueur — donc
     * l'eloignement — la multipliait par une portee tiree entre 0,9 et 1,2, puis la
     * <b>normalisait</b> avant de la mettre a 0,5-1 : le tirage s'annulait, et le port ne le
     * garde pas. Ce qui reste est l'eloignement, a la vitesse tiree.
     */
    public static Vec3 blastVelocity(Vec3 playerPos, Vec3 targetEye, double power) {
        Vec3 away = targetEye.subtract(playerPos);
        return away.lengthSqr() == 0 ? Vec3.ZERO : away.normalize().scale(power);
    }

    /** Ce que le bloc oppose aux ailes : toute durete entre 0 et 0,3, comme l'original. */
    public static boolean breaks(float hardness) {
        return hardness >= 0f && hardness <= BREAK_HARDNESS;
    }

    // ------------------------------------------------------------------
    // Le maintien, cote serveur
    // ------------------------------------------------------------------

    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Et les ailes se <b>basculent</b> : un appui les ouvre, un second les referme.
     *
     * <p>C'est l'original mot pour mot — son gestionnaire de touche terminait le contexte ouvert
     * quand les ailes l'etaient deja —, et le port les tenait jusqu'au relachement.
     */
    @Override
    public boolean isToggle() {
        return true;
    }

    /** Aucune duree : les ailes tiennent tant que la reserve suit, ou jusqu'au second appui. */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return 0;
    }

    /** Le prix d'ouverture est le premier tick de vol, et rien d'autre. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(COOLDOWN_MAX_EXP, COOLDOWN_MIN_EXP, data.getSkillExp(this));
    }

    /** Tout est verse par le vol : la charge ne rapporte rien. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /** Le geste se sert des quatre touches de deplacement, comme le scintillement. */
    @Override
    public boolean listensToDirections() {
        return true;
    }

    /**
     * A l'ouverture du maintien : autoriser le vol, et se souvenir de ce qu'il etait.
     *
     * L'original enregistrait {@code prevAllowFlying} pour le rendre a la fin. Le serveur doit
     * savoir que ce joueur peut voler, sinon il corrigerait chaque deplacement envoye par le
     * client ; le client, lui, recoit la capacite avec le reste.
     */
    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHoldFlying(this, player.getAbilities().mayfly);
        player.getAbilities().mayfly = true;
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            server.onUpdateAbilities();
        }
    }

    /**
     * Un tick de maintien, cote serveur : la note du vol.
     *
     * <p>Le deplacement, lui, est pousse par le client ({@link #onClientHoldTick}) : ici on
     * protege de la chute, on casse ce que les ailes maladroites trouvent, on ouvre les ailes
     * au moment dit, et on facture.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        player.fallDistance = 0f;
        // Tant que les ailes volent, la chute est remise a zero a chaque tick — et la protection
        // se repose avec elle. Sans ce deuxieme point, un vol qui frôle le sol la consommerait en
        // route, et c'est la fermeture des ailes qui doit laisser une chute gratuite : la ou on
        // se pose est un choix, et il ne se paie pas. Voir AbilityData.protectFromFall.
        data.protectFromFall();

        if (clumsy(data) && player.level() instanceof ServerLevel level) {
            breakAround(level, player);
        }

        // La charge ne coute rien : l'original ne facturait que les ailes ouvertes.
        if (!opened(data, heldTicks)) return true;

        if (opensThisTick(data, heldTicks)) blowAround(player, data);

        data.addSkillExp(this, EXP_PER_TICK);
        // L'original versait l'experience avant de payer, et terminait le vol quand la
        // reserve ou la surcharge manquait : les deux ressources ou rien, comme toujours.
        return data.perform(getTickUpkeep(data, heldTicks), overload(data));
    }

    /** A la fin du maintien : rendre le vol tel qu'il etait. */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        player.getAbilities().mayfly = data.getHoldFlying(this);
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            server.onUpdateAbilities();
        }
        player.fallDistance = 0f;
        // Et la ou on se posera ne se paie pas : la fermeture des ailes laisse tomber de la
        // hauteur qu'on veut, et c'est le porteur qui la prenait. Meme regle que la teleporteuse,
        // demandee par le joueur. Voir AbilityData.protectFromFall.
        data.protectFromFall();
    }

    // ------------------------------------------------------------------
    // Le vol, cote client
    // ------------------------------------------------------------------

    /**
     * Un tick de vol, chez le joueur.
     *
     * <p>C'est le {@code l_tick} de l'original, et c'est le client qui pousse : la vitesse est
     * posee a chaque tick, dans le repere du <b>cap</b>, par pas de 0,16. Sans touche tenue — ou
     * les ailes encore fermees — on flotte, ce qui permet de se poser en l'air.
     *
     * <p>{@code local} est ce que le joueur demande, dans son propre repere (voir
     * {@link #localWish}). La direction est <b>normalisee</b> avant d'etre mise a la vitesse du
     * vol : une diagonale ou une montee va donc exactement aussi vite qu'un cap franc, et le vol
     * se comporte pareil dans toutes les directions, ce que le joueur a demande.
     */
    @Override
    public void onClientHoldTick(Player player, AbilityData data, int heldTicks, Vec3 local) {
        Vec3 aim = opened(data, heldTicks) ? flightDirection(player.getYRot(), local) : Vec3.ZERO;
        Vec3 motion = player.getDeltaMovement();

        if (aim.lengthSqr() == 0) {
            player.setDeltaMovement(motion.x, hoverVelocity(groundBelow(player), motion.y), motion.z);
        } else {
            Vec3 wanted = aim.normalize().scale(speed(data));
            player.setDeltaMovement(
                    step(motion.x, wanted.x, ACCEL),
                    step(motion.y, wanted.y, ACCEL),
                    step(motion.z, wanted.z, ACCEL));
            // L'original faisait descendre le joueur de sa monture des qu'il poussait.
            if (player.isPassenger()) player.stopRiding();
        }
        player.fallDistance = 0f;
    }

    /** Le sol sous les pieds, par le trace de 0,8 bloc de l'original. */
    private static boolean groundBelow(Player player) {
        Vec3 from = player.position().add(0, GROUND_LIFT, 0);
        Vec3 to = player.position().add(0, GROUND_DROP, 0);
        HitResult hit = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS;
    }

    // ------------------------------------------------------------------
    // Les degats des ailes
    // ------------------------------------------------------------------

    /**
     * Les ailes maladroites cassent ce qu'elles trouvent.
     *
     * Quarante positions tirees dans dix blocs, et rien qu'une durete comprise entre 0 et 0,3 :
     * de l'herbe, de la neige, des feuilles. L'original jouait le son de casse de chaque bloc ;
     * le port n'a pas de son.
     */
    private static void breakAround(ServerLevel level, Player player) {
        if (!cn.academy.Config.destroyBlocks) return;
        for (int attempt = 0; attempt < BREAK_ATTEMPTS; attempt++) {
            BlockPos pos = BlockPos.containing(
                    player.getX() + Mth.nextInt(level.getRandom(), -BREAK_RANGE, BREAK_RANGE),
                    player.getY() + Mth.nextInt(level.getRandom(), -BREAK_RANGE, BREAK_RANGE),
                    player.getZ() + Mth.nextInt(level.getRandom(), -BREAK_RANGE, BREAK_RANGE));
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (!breaks(state.getDestroySpeed(level, pos))) continue;
            level.removeBlock(pos, false);
        }
    }

    /** Et a pleine experience, c'est l'ouverture qui repousse tout ce qui est proche. */
    private void blowAround(Player player, AbilityData data) {
        if (!blows(data)) return;
        Level level = player.level();
        Vec3 center = player.position();
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                new AABB(center, center).inflate(BLAST_RANGE))) {
            if (entity == player) continue;
            double power = BLAST_MIN + level.getRandom().nextDouble() * (BLAST_MAX - BLAST_MIN);
            entity.setDeltaMovement(blastVelocity(center, entity.getEyePosition(), power));
            // Le client doit accepter cette vitesse : sans cela il la corrigerait au tick
            // suivant, et le souffle ne se verrait pas.
            entity.hurtMarked = true;
        }
    }
}
