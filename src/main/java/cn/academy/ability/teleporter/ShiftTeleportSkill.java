package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Le depose au loin, portage de {@code ShiftTeleport} : on tient un <b>bloc</b>, on le pose la ou
 * l'on regarde — jusqu'a trente-cinq blocs — et <b>tout ce qui se trouve sur la ligne</b> prend le
 * coup au passage.
 *
 * <h2>Ce n'est pas une teleportation de son joueur</h2>
 *
 * <p>Le port en avait fait un saut court : le joueur se deplacait jusqu'a l'endroit vise, avec un
 * fantome pour le montrer. L'original ne deplace personne — son {@code s_execute} pose un bloc et
 * frappe, et rien d'autre. Son nom dit le geste : on <b>deplace</b> le bloc, pas le bonhomme. Le
 * joueur a vu la difference : « le shift teleport n'a aucun rapport avec le vrai pouvoir ».
 *
 * <h2>Il faut un bloc en main</h2>
 *
 * <p>C'est ce bloc qui part, et c'est la seule condition d'ouverture : {@link #canStart} refuse la
 * competence quand la main est vide, comme l'{@code isHandValid} de l'original. Le geste en coute
 * <b>un</b> — l'objet pose est retire de la pile — sauf en creatif, ou le monde est un brouillon.
 *
 * <p>Et quand l'endroit refuse le bloc, l'objet <b>tombe</b> au point touche, comme chez
 * l'original : le geste ne se perd pas, il change de main. Il tombe aussi quand le geste
 * <b>touche quelqu'un</b> — la seule chose que le port ajoute a l'original, qui plantait son bloc
 * jusque dans la creature qu'il venait de frapper. Voir {@link #onRelease}.
 *
 * <h2>Et la ligne</h2>
 *
 * <p>{@link #line} prend toutes les creatures dont la boite croise le segment qui va des pieds du
 * joueur a la case visee — donc celles d'un peu partout, y compris derriere un mur : c'est le
 * cote « epieu » du geste, et c'est {@link TeleportCrits} qui frappe, comme le lancer d'objet.
 *
 * <h2>Et sa trainee</h2>
 *
 * <p>C'est la seule chose que le geste donne a voir, puisqu'il ne deplace personne : l'original
 * semait des etincelles de teleportation le long du trajet, de ses pieds jusqu'a la case visee.
 * Le port l'avait oubliee ; elle part maintenant de {@link #onRelease}, par un
 * {@code ShiftTeleportPacket} qui ne porte que les deux bouts — c'est le client qui seme, comme
 * pour les rayons du meltdowner. Voir {@code ShiftTrail}.
 */
public class ShiftTeleportSkill extends Skill {

    /** Le prix du geste : 260 a 320 CP, comme l'original, et il monte avec l'experience. */
    private static final float CP_COST_MIN_EXP = 260f;
    private static final float CP_COST_MAX_EXP = 320f;

    /** Ce qu'une creature traversee encaisse : de 15 a 35, comme l'original. */
    private static final float DAMAGE_MIN = 15f;
    private static final float DAMAGE_MAX = 35f;

    /** L'experience par creature traverse, plus la part de base : les deux de l'original. */
    public static final float EXP_PER_TARGET = 0.002f;

    public ShiftTeleportSkill() {
        super("shift_tp", 4);
    }

    /** La portee du geste : de 25 a 35 blocs selon l'experience, comme l'original. */
    public double maxRange(AbilityData data) {
        return lerp(25f, 35f, data.getSkillExp(this));
    }

    /** Ce qu'une creature traversee encaisse : de 15 a 35 selon l'experience. */
    public float damage(AbilityData data) {
        return lerp(DAMAGE_MIN, DAMAGE_MAX, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 100 a 60 ticks, soit 5 a 3 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(100f, 60f, data.getSkillExp(this));
    }

    /**
     * L'experience vient du geste, qui seul connait le nombre de creatures traversees.
     *
     * <p>L'original versait {@code (1 + n) * 0,002} : la part de base, plus une par creature. Le
     * port versait la seule part de base, faute de compter — voir {@link #onRelease}.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Surcout repris de l'original : de 40 a 30 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(40f, 30f, data.getSkillExp(this));
    }

    /**
     * La competence se <b>tient</b>, comme dans l'original : on vise tant que la touche est
     * enfoncee, et le geste part au relachement.
     *
     * <p>C'est aussi pourquoi la marque a le temps de se montrer : chez l'original, le contexte
     * vivait pendant tout le maintien et y posait sa boite sur la case visee.
     */
    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Tenir ne coute rien : c'est le geste qui se paie, au relachement.
     *
     * <p>L'original consommait dans son message d'execution, donc au relachement lui aussi. Le prix
     * du geste reste {@link #getCpCost(AbilityData)} et {@link #getOverloadCost(AbilityData)}.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /**
     * Il faut un <b>bloc en main</b> : c'est lui qui part.
     *
     * <p>Le refus se lit a l'appui, et la competence ne s'ouvre pas — c'est l'{@code isHandValid}
     * de l'original, qu'il testait a l'ouverture <b>et</b> au relachement. Le port n'a besoin que
     * du premier : une pile qui se vide en tenant la touche ne vide pas la competence, elle la
     * laisse simplement partir sans rien poser — voir {@link #onRelease}.
     */
    @Override
    public boolean canStart(Player player, AbilityData data) {
        return isHandValid(player);
    }

    /**
     * Et c'est tout ce qu'elle demande : un bloc, et rien d'autre.
     *
     * <p>Dite <b>a part</b> de {@link #canStart} pour que le client puisse la lire tout seul, sans
     * risque : c'est ce qui l'empeche d'ouvrir un maintien que le serveur refusera, et d'en laisser
     * voir le debut pour rien. Voir {@code Skill#isHandValid}.
     */
    @Override
    public boolean isHandValid(Player player) {
        return blockOf(player) != null;
    }

    /** Le bloc que la main tient, ou {@code null} si ce n'est pas un bloc. */
    @Nullable
    public static BlockItem blockOf(Player player) {
        return player.getMainHandItem().getItem() instanceof BlockItem item ? item : null;
    }

    /**
     * Le relachement : le bloc part, et la ligne encaisse.
     *
     * <p>Refuse, il ne se passe rien du tout — rien n'est pose, rien n'est depense : c'est le
     * {@code consume()} de l'original, qui gardait tout ou ne payait rien.
     *
     * <p>La pose passe par {@link BlockItem#place}, qui fait tout le travail de l'original : l'etat
     * de la face visee, le controle de place, le bruit du bloc, et le retrait de l'objet de la
     * pile — sauf en creatif. Quand l'endroit refuse, le meme objet tombe au point touche.
     *
     * <p>Et quand le geste <b>touche quelqu'un</b>, le bloc ne se plante pas davantage : il
     * <b>continue sa route</b> et tombe derriere la creature. L'original, lui, le posait — sa
     * verification ne regardait que le terrain, jamais les creatures, et il enfoncait donc son bloc
     * dans celle qu'il venait de frapper. Le port, lui, ne l'y laisse plus s'arreter : c'est un coup
     * d'epieu, pas un pieu — « quand je vise la cible directement, le bloc a l'air de bloquer sur la
     * cible plutot que de passer derriere elle ».
     *
     * <p>Le controle de l'original qui verifiait qu'on avait le droit de <b>casser</b> le bloc vise
     * n'a pas d'equivalent dans le port : il n'y a pas de reglage de ce genre a lire.
     */
    @Override
    public boolean onRelease(Player player, AbilityData data, int heldTicks) {
        ItemStack stack = player.getMainHandItem();
        BlockItem item = blockOf(player);
        if (item == null) return false;
        if (!data.perform(getCpCost(data), getOverloadCost(data))) return false;

        Target target = target(player, data);
        // Les creatures que le geste croise, dans l'ordre ou elles se presentent : c'est un coup
        // d'epieu qui part des pieds et va jusqu'a la case visee. La question se pose AVANT la pose,
        // parce que c'est elle qui la decide.
        List<LivingEntity> hit = line(player.level(), player, target.cell());
        // La visee peut avoir trouve une bete que la LIGNE rate — viser les pieds, une bete collee
        // au sol : le geste la frappe quand meme, et le bloc part derriere elle.
        if (target.victim() instanceof LivingEntity visee && !hit.contains(visee)) {
            hit.add(visee);
        }

        // Le geste qui touche quelqu'un ne se plante pas dans le sol : le bloc, lui, <b>continue sa
        // route</b> et tombe derriere la creature — c'est un coup d'epieu, pas un pieu. Seuls les
        // degats s'arretent sur elle.
        //
        // ET LA BETE VISEE COMPTE COMME TOUCHEE, meme quand la LIGNE la rate : c'est elle qui
        // decidait de la pose, et c'est par la qu'un bloc se plantait sur elle — la ligne manquait
        // la creature, la pose s'ouvrait, et la case visee etait la sienne. Voir #past.
        boolean touches = !hit.isEmpty() || target.victim() != null;
        boolean planted = !touches && place(player, item, stack, target);
        if (!planted) {
            drop(player, stack, touches ? past(player, data, target, hit) : target);
        }

        for (LivingEntity living : hit) {
            // `scaled` : le multiplicateur global de degats de la config passe par la, comme
            // pour toutes les competences actives. Les deux autres coups de la categorie
            // l'appliquaient deja ; celui-ci l'oubliait, donc `general.damageScale` ne mordait
            // pas sur le geste d'epee.
            TeleportCrits.strike(player, data, living, scaled(damage(data)));
        }

        // La trainee du geste, du corps du lanceur jusqu'a la case visee : c'est la seule chose
        // qu'un geste qui ne deplace personne donne a voir. Elle part d'ICI, apres le paiement —
        // l'original l'envoyait avant de payer, donc un geste refuse par la reserve laissait
        // quand meme sa trace. Voir ShiftTeleportPacket et ShiftTrail.
        cn.academy.ability.network.ShiftTeleportPacket.send(player, player.position(),
                target.cell());

        // Le son part toujours, comme chez l'original : il annonce le geste, pas ses victimes.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP_SHIFT, 0.5f);
        // 0,002 par creature traversee, plus la part de base : l'original, au chiffre pres.
        data.addSkillExp(this, (1 + hit.size()) * EXP_PER_TARGET);

        // Le maintien se ferme ici : la recharge se pose ensuite par la fin ordinaire.
        return false;
    }

    /**
     * Pose le bloc sur la case visee, et dit si l'endroit l'a accepte.
     *
     * <p>{@link BlockItem#place} fait tout le travail de l'original : l'etat de la face visee, le
     * controle de place, le bruit du bloc, et le retrait de l'objet de la pile — sauf en creatif,
     * ou il ne coute rien.
     */
    private static boolean place(Player player, BlockItem item, ItemStack stack, Target target) {
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(target.point(), target.face(), target.block(), false));
        return item.place(context).consumesAction();
    }

    /** Fait tomber l'objet au point touche, ou il en coute un — sauf en creatif. */
    private static void drop(Player player, ItemStack stack, Target target) {
        ItemStack thrown = stack.copy();
        thrown.setCount(1);
        player.level().addFreshEntity(new ItemEntity(player.level(), target.point().x,
                target.point().y, target.point().z, thrown));
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    /**
     * Ou le bloc tomberait : la case visee, et le point touche.
     *
     * <p>Portage de {@code getTraceDest} et de {@code getTracePosition}, qui etaient la meme chose
     * dite deux fois — l'un en coordonnees de bloc, l'autre en point. La case visee est celle qui
     * <b>suit la face touchee</b> : c'est la que le bloc se poserait, et c'est ce que la marque
     * montre. Quand le regard ne butte sur rien, elle se prend au bout de la portee, dans l'air.
     *
     * <p>Elle est toujours <b>libre</b> : le rayon s'arrete au premier bloc, donc la case qui suit
     * la face touchee est forcement vide — sinon le rayon l'aurait touchee avant lui.
     */
    /** Le pas dont la continuation repart apres la derniere boite traversee, en blocs. */
    private static final double EXIT_STEP = 0.05;

    /**
     * Ou le bloc atterrit quand le geste a touche quelqu'un : <b>plus loin</b>, dans l'axe du geste.
     *
     * <p>Le joueur, trois fois : « le bloc s'arrete sur l'entite, il ne passe pas derriere meme si il
     * y a de la place », « meme si il n'y a aucun mur derriere elle, le bloc s'arrete quand meme sur
     * l'entite plutot que de continuer son chemin ». Le port avait tente de repartir du <b>point
     * touche</b> en sautant les blocs trop proches : en visant un corps, ce point est sur le sol
     * colle aux pieds de la bete, et le saut suivant repartait <b>dans</b> ce sol — chaque essai
     * retombait donc a un bloc ou deux d'elle, c'est-a-dire sur elle.
     *
     * <p>La continuation prend maintenant le regard <b>tel qu'il est</b> et repart la ou il SORT des
     * boites traversees — la bete visee, et celles que la ligne croise. C'est la meme droite, une
     * fois ces boites effacees : le bloc suit donc le chemin qu'il aurait pris sans personne, et
     * c'est exactement ce que le joueur demande. Voir {@link #exitAlong}, fonction pure.
     *
     * <p>Quand il ne reste aucun terrain devant — le regard part vers le ciel —, la destination est
     * le bout de la portee, dans l'air, et le bloc tombe la.
     */
    public Target past(Player player, AbilityData data, Target struck,
                       List<LivingEntity> crossed) {
        Vec3 eye = player.getEyePosition(1f);
        Vec3 look = player.getViewVector(1f);
        Vec3 end = eye.add(look.scale(maxRange(data) + 2.0));

        double skip = 0.0;
        if (struck.victim() instanceof LivingEntity victim) {
            skip = Math.max(skip, exitAlong(victim.getBoundingBox(), eye, look));
        }
        for (LivingEntity living : crossed) {
            skip = Math.max(skip, exitAlong(living.getBoundingBox(), eye, look));
        }

        Vec3 start = skip > 0.0 ? eye.add(look.scale(skip + EXIT_STEP)) : eye;
        BlockHitResult hit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? fromMiss(end) : fromHit(hit);
    }

    /**
     * A quelle distance le long du regard on <b>sort</b> de cette boite, ou {@code -1} si le regard
     * ne la traverse pas.
     *
     * <p>Fonction pure, donc verifiable : c'est le decoupage d'un rayon par les six plans de la
     * boite, sans monde ni entite. Un rayon qui part de l'interieur rend la sortie — c'est le cas de
     * la creature collee au tireur —, et un rayon qui ne la croise pas rend moins un.
     */
    public static double exitAlong(AABB box, Vec3 from, Vec3 dir) {
        double enter = 0.0;
        double leave = Double.MAX_VALUE;
        double[] origin = {from.x, from.y, from.z};
        double[] way = {dir.x, dir.y, dir.z};
        double[] low = {box.minX, box.minY, box.minZ};
        double[] high = {box.maxX, box.maxY, box.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(way[axis]) < 1.0e-9) {
                // Le rayon ne bouge pas sur cet axe : il est dedans ou il est a cote, pour toujours.
                if (origin[axis] < low[axis] || origin[axis] > high[axis]) return -1.0;
                continue;
            }
            double one = (low[axis] - origin[axis]) / way[axis];
            double two = (high[axis] - origin[axis]) / way[axis];
            enter = Math.max(enter, Math.min(one, two));
            leave = Math.min(leave, Math.max(one, two));
        }
        return leave >= enter ? leave : -1.0;
    }

    public Target target(Player player, AbilityData data) {
        Vec3 eye = player.getEyePosition(1f);
        Vec3 look = player.getViewVector(1f);
        Vec3 end = eye.add(look.scale(maxRange(data)));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        // ET LES CREATURES, comme le {@code traceLiving} de l'original, qui tracait les deux d'un
        // seul rayon. Le port ne tracait que les blocs : viser une bete SANS MUR DERRIERE laissait
        // la case visee au bout de la portee, et le coup partait sous elle — le joueur l'a vu le
        // 22/10 avec un shulker : « si il n'y a pas de bloc derriere lui, alors l'attaque ne le
        // frappe pas ». La ligne part des PIEDS et va au centre de la case visee ; sur trente-cinq
        // blocs, la hauteur d'yeux qui la separe du regard suffit a passer dessous.
        var struck = cn.academy.ability.TargetingUtil.findEntityHit(player, eye, end,
                e -> e instanceof LivingEntity);
        if (struck != null && (hit.getType() == HitResult.Type.MISS
                || struck.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye))) {
            return fromEntity(struck.getEntity(), struck.getLocation());
        }
        // ET LA CREATURE QUI SE TIENT AU POINT TOUCHE. Viser les PIEDS d'un joueur fait buter le
        // rayon sur le sol juste devant lui : le regard ne traverse donc jamais sa boite, et le
        // geste plantait son bloc dans l'herbe au lieu de le transpercer — « la seule solution,
        // c'est de le viser un peu vers le ciel ». Le point touche, lui, est a ses pieds : la
        // creature qui l'occupe est la cible.
        if (hit.getType() != HitResult.Type.MISS) {
            LivingEntity standing = standingAt(player, hit.getLocation());
            if (standing != null) {
                return fromEntity(standing, hit.getLocation());
            }
            return fromHit(hit);
        }
        return fromMiss(end);
    }

    /**
     * La creature dont la boite contient ce point, ou qui le touche de pres.
     *
     * <p>La tolerance valait trois dixiemes, au motif que le point touche est sur le sol, donc a la
     * limite basse de la boite. C'etait compter sans les creatures qui ne se tiennent pas
     * <b>pile</b> sur la case : une vache au bord d'une dalle, un monstre qui vient de sauter, et le
     * point touche tombe a un demi-bloc de ses pieds — la visee ne trouvait plus personne. Elle vaut
     * maintenant huit dixiemes, la largeur d'un corps pres. Le joueur : « parfois, si le monstre
     * n'est pas parfaitement sur le bloc, il n'est tout simplement pas encadre en rouge ».
     */
    private static LivingEntity standingAt(Player player, Vec3 point) {
        AABB box = new AABB(point, point).inflate(STANDING_GRACE);
        LivingEntity best = null;
        double closest = Double.MAX_VALUE;
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && !e.isSpectator() && e.isAlive())) {
            double distance = living.getBoundingBox().distanceToSqr(point);
            if (distance < closest) {
                closest = distance;
                best = living;
            }
        }
        return closest <= STANDING_GRACE * STANDING_GRACE ? best : null;
    }

    /** La tolerance du point touche a la boite de la creature qui l'occupe, en blocs. */
    private static final double STANDING_GRACE = 0.8;

    /**
     * La case visee quand le regard bute sur une <b>creature</b> : la sienne.
     *
     * <p>C'est la {@code getTraceDest} de l'original, dont le rayon rendait aussi les betes : la
     * case visee etait alors celle ou la bete se tient, donc la ligne du coup s'arretait <b>sur
     * elle</b> au lieu de la depasser. Le point, lui, est le point touche sur sa boite — c'est la
     * que le bloc tombera, puisqu'un geste qui touche ne se plante pas.
     *
     * <p>Fonction pure, donc verifiable : la case vient de la position de la creature, rien d'autre.
     */
    public static Target fromEntity(Entity entity, Vec3 struck) {
        BlockPos cell = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        return new Target(cell, struck, Direction.DOWN, cell, entity);
    }

    /**
     * La case visee par une face touchee : celle qui la suit, et le point touche qui la suit aussi.
     *
     * <p>Fonction pure, donc verifiable : c'est de la geometrie, et rien d'autre.
     */
    public static Target fromHit(BlockHitResult hit) {
        Direction face = hit.getDirection();
        BlockPos block = hit.getBlockPos();
        Vec3 point = hit.getLocation().add(face.getStepX(), face.getStepY(), face.getStepZ());
        return new Target(block.relative(face), point, face, block, null);
    }

    /**
     * Et celle d'un regard qui ne butte sur rien : elle se prend au bout de la portee.
     *
     * <p>Fonction pure, donc verifiable. La face est <b>celle du dessous</b>, comme chez l'original :
     * un bloc qui se pose dans le vide s'y accroche par en dessous, et le rayon est parti vers le
     * bas du monde.
     */
    public static Target fromMiss(Vec3 end) {
        BlockPos cell = BlockPos.containing(end);
        return new Target(cell, end, Direction.DOWN, cell, null);
    }

    /**
     * La marge autour d'une boite, en blocs : une creature qui <b>rase</b> la ligne est traversee.
     *
     * <p>Sans elle, il fallait tomber pile dessus. Le joueur : « parfois, si le monstre n'est pas
     * parfaitement sur le bloc, il n'est tout simplement pas encadre en rouge, et ne prend donc
     * aucun degats ». Un tiers de bloc, c'est l'ecart d'un corps qui se tient au bord de sa case.
     */
    public static final double LINE_GRACE = 0.35;

    /**
     * Les creatures que le geste traverse : celles dont la boite croise le segment qui va des pieds
     * du joueur jusqu'au point vise.
     *
     * <p>C'est le {@code getTargetsInLine} de l'original : un volume pour demander au monde ses
     * creatures — celui du segment —, puis un croisement de boite pour chacune, qui ecarte celles
     * qui ne sont que <b>a cote</b> de la ligne. L'auteur du geste en est retire.
     *
     * <p>Le segment part des pieds, comme chez l'original, et non des yeux : c'est le corps entier
     * qui passe, et c'est lui qui frappe. Il va au <b>centre de la case visee</b>.
     */
    public static List<LivingEntity> line(Level level, Player player, BlockPos cell) {
        return line(level, player, Vec3.atCenterOf(cell));
    }

    /**
     * La meme ligne, mais jusqu'au point que la visee a trouve.
     *
     * <p>Le centre de la case n'est pas toujours <b>dans</b> la creature : ses pieds peuvent tomber
     * a un cheveu sous la limite de la case, et la case visee est alors celle du sol, donc son
     * centre passe sous la bete — la ligne l'effleurait sans la toucher, et le geste la traversait
     * sans la voir. Le centre de la boite, lui, est toujours dedans. Voir {@link #aimOf}.
     */
    public static List<LivingEntity> line(Level level, Player player, Target target) {
        return line(level, player, aimOf(target));
    }

    /** Le point que la ligne du geste vise : le centre de la bete, ou celui de la case. */
    public static Vec3 aimOf(Target target) {
        return target.victim() != null ? target.victim().getBoundingBox().getCenter()
                : Vec3.atCenterOf(target.cell());
    }

    private static List<LivingEntity> line(Level level, Player player, Vec3 to) {
        Vec3 from = player.position();
        AABB area = new AABB(
                Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z),
                Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z))
                .inflate(LINE_GRACE);

        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && !e.isSpectator())) {
            if (crosses(living.getBoundingBox().inflate(LINE_GRACE), from, to)) hit.add(living);
        }
        return hit;
    }

    /**
     * Vrai si le segment entre dans cette boite.
     *
     * <p>Fonction pure, donc verifiable : c'est le {@code VecUtils.checkLineBox} de l'original, mot
     * pour mot — un croisement de segment et de boite, sans monde ni entite.
     *
     * <p>Le cas de la boite qui <b>contient</b> le depart est traite a part, et ce n'est pas une
     * precaution : le croisement de Minecraft est un rayon qui vient de l'exterieur, et il rend
     * « rien » quand la source est <b>dans</b> la boite. Une creature collee a son auteur — elle le
     * touche — ne serait alors pas frappee, alors que l'original la comptait.
     */
    public static boolean crosses(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) || box.clip(from, to).isPresent();
    }

    /**
     * L'endroit vise, en deux pieces : la <b>case</b> du bloc, et le <b>point touche</b>.
     *
     * <p>Les deux servent, et a des choses differentes : la case est ce que la marque dessine et ce
     * que la ligne vise, le point est la ou le bloc se pose — ou tombe.
     */
    /**
     * La case visee, son point, sa face — et la creature visee, s'il y en avait une.
     *
     * <p>{@code victim} est la pour une raison : la ligne du geste peut rater une bete que l'oeil a
     * pourtant visee (viser les pieds, une bete collee au sol). Le geste la frappe quand meme, et
     * le bloc, lui, part derriere — sans cette information, il se posait sur elle.
     */
    public record Target(BlockPos cell, Vec3 point, Direction face, BlockPos block,
                         Entity victim) {
    }
}
