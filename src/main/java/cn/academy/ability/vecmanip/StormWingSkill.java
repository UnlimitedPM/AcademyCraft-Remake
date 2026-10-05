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
 * et l'arrivee rapide. La direction est prise dans le repere du regard — avant et arriere
 * suivent le tangage, gauche et droite restent horizontaux, comme les quatre clefs de
 * l'original — et la vitesse va de 1,4 a 3,6 blocs par tick selon l'experience : sous 45 %
 * d'experience les ailes sont lentes (0,7), au-dela elles sont rapides (1,2), et les deux
 * grandissent jusqu'a 3 fois.
 *
 * <p>Sans touche tenue, les ailes <b>flottent</b> comme pendant la charge : c'est ainsi qu'on
 * se pose au milieu de l'air, et c'est encore l'original.
 *
 * <h2>Ce que le vol coute, et ce qu'il casse</h2>
 *
 * Chaque tick d'ailes ouvertes verse 0,00005 d'experience et paie 40 a 25 CP et 10 a 7 de
 * surcout ; quand l'une des deux reserve manque, le vol s'arrete. Ce sont les nombres de
 * l'original, bruts : sa reserve va de 1800 a 8000 selon le niveau, et son surcout de 100 a 500,
 * donc ils s'y lisent sans conversion — le detour par un plafond de 100 et un facteur 28 qui les
 * rendait lisibles a ete retire avec le plafond (voir {@code AbilityData.BASE_MAX_CONTROL_POINT}).
 * La charge, elle,
 * est gratuite. Le surcout d'ouverture est le <b>premier tick de vol</b>, paye a l'appui :
 * c'est un ecart assume, l'original ne facturant rien tant que les ailes ne s'etaient pas
 * ouvertes.
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

    /** Un tick de vol : 40 a 25 CP, et 10 a 7 de surcout, comme l'original. */
    public static final float CP_MIN_EXP = 40f;
    public static final float CP_MAX_EXP = 25f;
    public static final float OVERLOAD_MIN_EXP = 10f;
    public static final float OVERLOAD_MAX_EXP = 7f;

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

    /** Ce qu'il charge a la surcharge. */
    public float overload(AbilityData data) {
        return lerp(OVERLOAD_MIN_EXP, OVERLOAD_MAX_EXP, data.getSkillExp(this));
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
     */
    public static Vec3 worldSpace(float yawDegrees, float pitchDegrees, Vec3 local) {
        return local.xRot((float) Math.toRadians(-pitchDegrees))
                .yRot((float) Math.toRadians(-yawDegrees));
    }

    /** La direction locale d'une touche de deplacement, comme les quatre clefs de l'original. */
    public static Vec3 localDirection(int direction) {
        return switch (direction) {
            case 1 -> new Vec3(1, 0, 0);
            case 2 -> new Vec3(-1, 0, 0);
            case 3 -> new Vec3(0, 0, 1);
            case 4 -> new Vec3(0, 0, -1);
            default -> Vec3.ZERO;
        };
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

        if (clumsy(data) && player.level() instanceof ServerLevel level) {
            breakAround(level, player);
        }

        // La charge ne coute rien : l'original ne facturait que les ailes ouvertes.
        if (!opened(data, heldTicks)) return true;

        if (opensThisTick(data, heldTicks)) blowAround(player, data);

        data.addSkillExp(this, EXP_PER_TICK);
        // L'original versait l'experience avant de payer, et terminait le vol quand la
        // reserve ou la surcharge manquait : les deux ressources ou rien, comme toujours.
        return data.perform(consumption(data), overload(data));
    }

    /** A la fin du maintien : rendre le vol tel qu'il etait. */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        player.getAbilities().mayfly = data.getHoldFlying(this);
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            server.onUpdateAbilities();
        }
        player.fallDistance = 0f;
    }

    // ------------------------------------------------------------------
    // Le vol, cote client
    // ------------------------------------------------------------------

    /**
     * Un tick de vol, chez le joueur.
     *
     * <p>C'est le {@code l_tick} de l'original, et c'est le client qui pousse : la vitesse est
     * posee a chaque tick, dans le repere du regard, par pas de 0,16. Sans touche tenue — ou
     * les ailes encore fermees — on flotte, ce qui permet de se poser en l'air.
     */
    @Override
    public void onClientHoldTick(Player player, AbilityData data, int heldTicks, int direction) {
        Vec3 aim = opened(data, heldTicks)
                ? worldSpace(player.getYRot(), player.getXRot(), localDirection(direction))
                : Vec3.ZERO;
        Vec3 motion = player.getDeltaMovement();

        if (aim.lengthSqr() == 0) {
            player.setDeltaMovement(motion.x, hoverVelocity(groundBelow(player), motion.y), motion.z);
        } else {
            Vec3 wanted = aim.scale(speed(data));
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
