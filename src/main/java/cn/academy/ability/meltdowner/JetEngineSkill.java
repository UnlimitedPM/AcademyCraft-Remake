package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.JetFlightPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Reacteur, portage de {@code JetEngine} : viser un point a douze blocs, et s'y faire
 * projeter en traversant ce qui s'y trouve.
 *
 * <p>C'est la competence la plus rapide du mod, et la seule dont l'effet <b>commence au
 * relachement</b> : tant que la touche est enfoncee, elle ne fait que montrer ou l'on
 * part — un marqueur pose au sol, dans l'original — et c'est en relachant qu'on part. Le
 * vol dure huit ticks pour rejoindre la cible, puis continue tout droit jusqu'a quinze
 * ticks : la quantite de mouvement est posee une fois, et rien ne l'arrete. L'original
 * laissait donc son porteur <b>depasser</b> sa cible de presque une fois la distance
 * visee — c'est ce qui en fait un deplacement et non une teleportation, et le port suit
 * le meme calcul plutot que de s'arreter proprement sur la cible.
 *
 * <h2>Le quatrieme cas</h2>
 *
 * Pour que cela soit possible, {@link Skill#onRelease} existe : un effet qui rend
 * {@code true} garde son maintien ouvert apres le relachement, et c'est son propre tick
 * qui le terminera. L'original faisait exactement cela — son contexte survivait a son
 * {@code MSG_MARK_END}, et ne mourait qu'a la fin du vol, cote client.
 *
 * <h2>Ce qu'on traverse</h2>
 *
 * A chaque tick du vol, l'original lancait un rayon entre la position precedente et la
 * nouvelle, et frappait le premier <b>vivant</b> rencontre pour 7 a 20 points. Traverser
 * une foule fait donc mal a tout le monde, et traverser une barque ne fait rien.
 * * <h2>Ce qu'on ne traverse plus</h2>
 *
 * Les <b>blocs</b>, eux. L'original posait ses positions sans regarder par ou il passait, et
 * c'est le bug que le joueur a retrouve tel quel : « si je vise un mur ou un plafond et qu'il
 * n'y a pas de bloc derriere, je passe au travers ». La visee s'arrete pourtant bien sur la face
 * — c'est ce que rend {@code getDest} de l'original, et ce que rend
 * {@link TargetingUtil#findImpactPoint} — mais le vol continue sept ticks de plus, tout droit, et
 * rien ne l'empechait donc de finir de l'autre cote d'un mur d'un bloc. Le vol s'arrete maintenant
 * sur la face du premier bloc qu'il rencontre : viser un mur s'acheve sur le mur, et une cible
 * dans l'air garde son elan.
 *
 * <h2>La chute qui suit</h2>
 *
 * Elle est gratuite, comme pour les quatre teleportations, l'acceleration de vecteur et la
 * fermeture des ailes : le vol pose son porteur n'importe ou, donc il se tuait lui-meme en
 * s'envoyant. C'est le meme « ne pas prendre de degats de chute apres la competence » que le
 * joueur a demande ailleurs, avec les deux ticks de l'atterrissage qui vont avec — voir
 * {@link AbilityData#protectFromFall}.
 *
 * <h2>Le bruitage</h2>
 *
 * Le son de l'acceleration de vecteur, en attendant mieux : le joueur l'a demande comme
 * remplacement pour l'instant, donc c'est {@code vecmanip.vec_accel} qui sonne au depart du vol.
 * * <h2>Paiement</h2>
 *
 * L'original payait au relachement (170 a 140 CP, 60 a 50 de surcout). Le port paie a
 * l'appui, comme toute competence tenue — le vol en herite donc meme si le joueur relache
 * aussitot. Le surcout est le prix reel : a son niveau, c'est un quart de la reserve.
 */
public class JetEngineSkill extends Skill {

    /** Ticks de vol pour rejoindre la cible ({@code TIME}). */
    public static final int FLIGHT_TIME = 8;

    /** Au-dela, le vol s'arrete : quinze ticks, cible depassee ({@code LIFETIME}). */
    public static final int LIFETIME = 15;

    /** Portee de la visee : douze blocs, blocs seulement ({@code getDest}). */
    public static final double AIM_RANGE = 12.0;

    public JetEngineSkill() {
        super("jet_engine", 4);
    }

    /**
     * Ou le porteur se trouve apres {@code flightTick} ticks de vol.
     *
     * Portage exact du {@code VecUtils.lerp(start, target, ticks / TIME)} de l'original :
     * le huitieme tick est sur la cible, et le quinzieme est a 1,875 fois la distance —
     * presque un deuxieme bond, sans rien pour le freiner.
     */
    public static Vec3 pathPosition(Vec3 start, Vec3 target, int flightTick) {
        double t = flightTick / (double) FLIGHT_TIME;
        return start.add(target.subtract(start).scale(t));
    }

    /** Vitesse posee a chaque tick : la cible divisee par le temps de vol. */
    public static Vec3 flightVelocity(Vec3 start, Vec3 target) {
        return target.subtract(start).scale(1.0 / FLIGHT_TIME);
    }

    /**
     * La face du premier bloc que le trajet du tick rencontre, ou {@code null} si la voie est
     * libre.
     *
     * <p>C'est ce qui manquait au vol : l'original posait sa position sans rien regarder, donc un
     * mur d'un bloc etait traverse des que la visee portait au-dela — le client, lui, resolvait
     * ensuite la collision en poussant son porteur du cote ou il restait de la place, ce qui le
     * faisait ressortir de l'autre cote. Pure geometrie : le serveur et le client s'en servent
     * tous les deux, avec leur propre monde, et tombent donc d'accord au bloc pres.
     */
    @Nullable
    public static Vec3 blockStop(Level level, Entity traveller, Vec3 previous, Vec3 next) {
        BlockHitResult hit = level.clip(new ClipContext(previous, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, traveller));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    /** Degats de ce qu'on traverse : de 7 a 20 selon l'experience. */
    public float flightDamage(AbilityData data) {
        return lerp(7f, 20f, data.getSkillExp(this));
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Pas de duree maximale : la visee peut durer tant que la touche est tenue, et le vol
     * se termine lui-meme au quinzieme tick.
     */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return 0;
    }

    /**
     * Cout du vol : 170 a 140 CP, comme l'original.
     *
     * La visee, elle, n'est facturee par aucun tick — l'original ne faisait que verifier
     * qu'il aurait de quoi payer, ce que le port fait d'avance en payant a l'appui.
     */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(170f, 140f, data.getSkillExp(this));
    }

    /** Surcout du vol : de 60 a 50, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(60f, 50f, data.getSkillExp(this));
    }

    /** Recharge : de trois secondes a une seconde et demie, comme l'original. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(60f, 30f, data.getSkillExp(this));
    }

    /** Tout se verse au depart du vol : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * Le relachement : le vol commence ici.
     *
     * La cible est relue au moment ou l'on part, comme l'original ({@code getDest} du
     * serveur, au moment du {@code MSG_MARK_END}) : viser puis tourner la tete avant de
     * relacher ne change donc pas la destination. Le repere de temps du maintien retient
     * le tick du depart, pour que le vol sache ou il en est.
     */
    @Override
    public boolean onRelease(Player player, AbilityData data, int heldTicks) {
        // Un effet qui continue tout seul doit avoir de quoi se reperer : le tick de
        // depart (`mark`), le point de depart (`origin`) et la cible (`point`).
        data.setHoldOrigin(this, player.position());
        data.setHoldPoint(this, TargetingUtil.findImpactPoint(player, AIM_RANGE));
        data.setHoldMark(this, heldTicks);

        // La chute qui suit le vol est gratuite, comme apres les quatre teleportations : le vol
        // pose son porteur n'importe ou, et il se tuait donc lui-meme en s'envoyant. Le drapeau
        // couvre la fin du vol ET l'atterrissage, deux ticks poses compris.
        data.protectFromFall();

        // Le bruitage du depart — le son de l'acceleration de vecteur, en attendant mieux.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.VECMANIP_VEC_ACCEL, 0.35f);

        // 0,004 pour le vol, comme l'original. L'experience d'usage d'une competence tenue
        // se verse depuis son effet : le paquet ne voit pas ce qui s'est passe.
        data.addSkillExp(this, 0.004f);

        // Et le depart se dit au client, avec sa trajectoire : son maintien a lui s'arrete au
        // relachement, alors que celui du serveur vole quinze ticks encore. Sans ce mot, le
        // bouclier de diamant et la trainee ne s'ouvriraient jamais — et c'est aussi lui qui
        // permet au client de poser exactement la meme trajectoire, et donc de voler sans
        // attendre que le serveur le tire. Voir JetFlightPacket.
        if (player instanceof ServerPlayer server) {
            AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> server),
                    new JetFlightPacket(player.getId(),
                            data.getHoldOrigin(this), data.getHoldPoint(this)));
        }
        return true;
    }

    /**
     * Un tick de maintien : la visee, puis le vol.
     *
     * Le repere de temps distingue les deux temps : tant qu'il n'y en a pas, la touche est
     * seulement tenue ; une fois pose, le tick du vol se compte a partir de lui, et le vol
     * se termine au-dela de {@link #LIFETIME} — ou sur le premier bloc rencontre.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        int releasedAt = data.getHoldMark(this);
        if (releasedAt < 0) return true;

        int flightTick = heldTicks - releasedAt;
        if (flightTick > LIFETIME) return false;

        return fly(player, data, flightTick);
    }

    /**
     * Un tick de vol : se deplacer, et percuter ce qui se trouve sur le trajet.
     *
     * <p>La position est <b>posee</b> sur la trajectoire, comme le client la pose de son cote, et
     * la quantite de mouvement avec elle. L'original ne faisait que la seconde moitie —
     * {@code setPosition} chez son client, {@code motion} avec — mais son serveur n'avait rien a
     * dire : le client etait maitre de sa position. Ici les deux la posent, donc ils tombent
     * d'accord sans que personne n'ait a corriger personne.
     *
     * <p>Le {@code teleportTo} d'avant est ce qui rendait le vol mou et saccade. Un paquet de
     * position ne s'interpole pas : la copie cliente sautait a chaque tick, et la vitesse posee
     * etait effacee par le teleport — le porteur s'arretait net sur sa cible au lieu de la
     * depasser, et gardait zero elan en sortant.
     *
     * <p>Rend faux quand le vol s'arrete : sur le premier bloc rencontre — voir
     * {@link #blockStop} — ou a la fin de sa vie. Le porteur garde alors son elan, comme a la fin
     * normale du vol.
     */
    private boolean fly(Player player, AbilityData data, int flightTick) {
        Vec3 start = data.getHoldOrigin(this);
        Vec3 target = data.getHoldPoint(this);
        if (start == null || target == null) return false;

        // La chute reste gratuite PENDANT tout le vol, et pas seulement au depart.
        //
        // Le drapeau se leve apres deux ticks passes sans tomber — c'est ce qui laisse un
        // atterrissage a cheval sur deux blocs se poser tranquillement. Or un vol ne tombe jamais :
        // sa chute est remise a zero a chaque tick. Pose une seule fois au relachement, le drapeau
        // se levait donc EN PLEIN VOL des que le porteur etait deja en train de tomber au
        // lancement — sauter d'une falaise, puis viser un mur — et la chute qui suivait l'arret
        // se payait. C'est ce que le joueur a vu : « il arrive que le jeu considere que je me suis
        // arrete sur le mur, et donc que par la suite je prends quand meme les degats de chutes ».
        // Le rappeler a chaque tick garde la protection neuve jusqu'a l'atterrissage, celui qui
        // suit l'arret sur le mur compris.
        data.protectFromFall();

        // Le rayon part de la position du tick precedent, comme le `lastTickPos` de l'original.
        Vec3 previous = new Vec3(player.xo, player.yo, player.zo);
        Vec3 next = pathPosition(start, target, flightTick);

        // Le premier bloc rencontre : on s'arrete sur sa face, sans le traverser. C'est le bug du
        // vrai mod — viser un mur d'un bloc le traversait — et il se voit des qu'il n'y a rien
        // derriere pour repousser le porteur.
        //
        // Le segment juge est celui de la TRAJECTOIRE, et non les `xo/yo/zo` du porteur : ces
        // derniers sont ecrits par `setPos`, donc ils peuvent valoir la position qu'on vient de
        // poser — un segment de longueur nulle, qui ne toucherait jamais rien. La trajectoire, elle,
        // est la meme des deux cotes, ce qui fait tomber serveur et client d'accord au bloc pres.
        Vec3 before = pathPosition(start, target, Math.max(0, flightTick - 1));
        Vec3 stop = blockStop(player.level(), player, before, next);
        if (stop != null) {
            player.setPos(stop.x, stop.y, stop.z);
            player.hurtMarked = true;
            player.fallDistance = 0f;
            return false;
        }

        // L'original faisait descendre son porteur de sa monture au premier tick de vol.
        if (player.isPassenger()) player.stopRiding();

        player.setPos(next.x, next.y, next.z);
        player.setDeltaMovement(flightVelocity(start, target));
        // Le client doit accepter cette vitesse : sans elle il ne la verrait pas, et le porteur
        // repartirait a zero. C'est ce que fait le deplacement magnetique, pour la meme raison.
        player.hurtMarked = true;
        // La chute est remise a zero a chaque tick : on ne se blesse pas en arrivant.
        player.fallDistance = 0f;

        Entity hit = TargetingUtil.findEntityAlong(player, previous, next,
                e -> e instanceof LivingEntity);
        if (hit instanceof LivingEntity living) {
            living.hurt(skillDamage(player),
                    scaled(flightDamage(data)));
            RadiationMarks.mark(living, data);
        }
        return true;
    }
}
