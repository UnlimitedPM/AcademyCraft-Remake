package cn.academy.ability.client;

import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.md.MdSparks;
import cn.academy.ability.meltdowner.JetEngineSkill;
import cn.academy.ability.meltdowner.JetEngineVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * Les deux effets du reacteur, cote client : la marque au sol pendant la visee, et le bouclier de
 * diamant avec sa trainee pendant le vol.
 *
 * <h2>La marque</h2>
 *
 * <p>L'original posait une entite cliente — {@code EntityRippleMark} — au point que le regard
 * touche, la relisait a chaque tick, et la tuait au relachement. Trois ondes s'y relayaient, et
 * c'est ce qui disait au joueur <b>ou</b> il allait partir : la competence ne se declenche qu'au
 * relachement, donc sans cette marque il n'aurait rien vu du tout. Le port la relit du meme point
 * — une visee de douze blocs, sur les blocs seuls — et la garde tant que le maintien est ouvert.
 *
 * <h2>Le vol</h2>
 *
 * <p>Le depart ne se voit pas chez le client : son maintien a lui s'arrete au relachement, alors
 * que celui du serveur continue quinze ticks encore. C'est le paquet {@code JetFlightPacket} qui
 * le lui dit, avec les deux bouts de la trajectoire, et il ouvre alors ce que l'original ouvrait
 * dans son {@code MSG_TRIGGER} : le bouclier de diamant autour du porteur, et dix etincelles de
 * plasma par tick.
 *
 * <p>Le deplacement, lui, se fait <b>ici</b> : le client pose sa position sur la trajectoire a
 * chaque tick, exactement comme le {@code setPosition} de l'original, et le serveur pose la meme.
 * Les deux cotes doivent tomber d'accord au bloc pres — c'est pourquoi la trajectoire voyage dans
 * le paquet plutot que d'etre recalculee de chaque cote.
 *
 * <p>Le serveur, de son cote, teleportait son porteur vingt fois par seconde. Un paquet de
 * position ne s'interpole pas : la copie cliente sautait donc a chaque tick, et la vitesse posee
 * mourait dans le teleport — le vol paraissait lent, et s'arretait net sur sa cible au lieu de la
 * depasser. C'est le meme piege que le deplacement magnetique, et il se resout de la meme facon,
 * en posant la position et la quantite de mouvement des deux cotes.
 */
@OnlyIn(Dist.CLIENT)
public final class JetEngineEffect {

    /** Le point vise, relu a chaque tick — la ou les ondes se posent. Nul, rien a dessiner. */
    private static Vec3 markAt;

    /** L'age de la marque, en ticks client depuis sa naissance. */
    private static int markTicks;

    /** Le premier tick du vol, ou -1 : c'est le paquet du serveur qui l'ouvre. */
    private static int flightTick = -1;

    /**
     * Les deux bouts de la trajectoire, tels que le serveur les a poses.
     *
     * <p>C'est le serveur qui les decide — son relachement les epingle — et le client s'en sert
     * tels quels : les deux doivent poser <b>exactement</b> la meme position a chaque tick.
     */
    private static Vec3 flightStart;
    private static Vec3 flightTarget;

    private static final Random RANDOM = new Random();

    private JetEngineEffect() {}

    /** A appeler a chaque tick client : voir {@code AbilityClientEvents.onClientTick}. */
    public static void tick() {
        tickMark();
        tickFlight();
    }

    /** La marque suit le point vise tant que le maintien est ouvert, et disparait avec lui. */
    private static void tickMark() {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null
                || !JetEngineVisuals.showsMark(JetEngineVisuals.SKILL,
                        ClientCharge.isSustained(JetEngineVisuals.SKILL))) {
            markAt = null;
            markTicks = 0;
            return;
        }

        // Un maintien qui vient de s'ouvrir : la marque nait ici, et son age part de la.
        if (markAt == null) markTicks = 0;
        else markTicks++;

        markAt = TargetingUtil.findImpactPoint(player, JetEngineSkill.AIM_RANGE);
    }

    /** Le vol, annonce par le serveur : le bouclier, et la trainee de plasma. */
    private static void tickFlight() {
        if (flightTick < 0) return;

        flightTick++;
        // Le vol se termine tout seul au bout de sa vie, comme celui de l'original : le
        // serveur s'est deja arrete de deplacer son porteur, et son propre paquet de fin
        // arrivera de toute facon.
        if (flightTick > JetEngineSkill.LIFETIME) {
            flightTick = -1;
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || flightStart == null || flightTarget == null) return;

        // La position est POSEE, jamais parcourue : c'est le `setPosition(lerp(...))` de
        // l'original, et c'est ce qui donne au vol toute sa vitesse. Laisse a sa propre
        // physique, un porteur au sol perdrait la moitie de son elan a la friction, et le
        // reacteur ne ressemblerait plus a ce qu'il est.
        Vec3 at = JetEngineSkill.pathPosition(flightStart, flightTarget, flightTick);
        player.setPos(at.x, at.y, at.z);
        // La quantite de mouvement est reposee a chaque tick, et rien ne l'efface a la fin :
        // c'est elle qui fait depasser la cible, et qui porte encore apres le quinzieme tick.
        player.setDeltaMovement(JetEngineSkill.flightVelocity(flightStart, flightTarget));
        player.fallDistance = 0f;

        Vec3 pos = player.position();
        for (int i = 0; i < JetEngineVisuals.TRAIL_PER_TICK; i++) {
            MdSparks.spawn(spread(pos, RANDOM), drift(RANDOM));
        }
    }

    /**
     * Le depart du vol, annonce par le serveur.
     *
     * <p>Seul le porteur le voit : c'est a lui que le paquet est envoye, comme l'original
     * n'ouvrait son bouclier que dans son propre client.
     */
    public static void startFlight(int playerId, Vec3 start, Vec3 target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getId() != playerId) return;

        flightStart = start;
        flightTarget = target;
        flightTick = 0;
        player.stopRiding();
        // Le vol commence : la marque de visee a fini son office.
        markAt = null;
        markTicks = 0;
    }

    /**
     * L'age de la marque, en secondes, ou -1 s'il n'y en a pas.
     *
     * <p>Le tick partiel compte : sans lui, les ondes avanceraient par saccades, et c'est
     * justement leur glissement continu qui fait la vague.
     */
    public static double markAgeSeconds(double partialTick) {
        return markAt == null ? -1 : (markTicks + partialTick) / 20.0;
    }

    /** Ou les ondes se posent, ou {@code null} s'il n'y a rien a dessiner. */
    public static Vec3 markAt() {
        return markAt;
    }

    /** Le tick de vol en cours, ou -1. Le rendu s'en sert pour son bouclier. */
    public static int flightTick() {
        return flightTick;
    }

    /** Une position de trainee : autour du porteur, dans un cube de 0,3 bloc de cote. */
    public static Vec3 spread(Vec3 centre, Random random) {
        return new Vec3(centre.x + jitter(random), centre.y + jitter(random),
                centre.z + jitter(random));
    }

    /** Sa derive : deux centimetres par tick au plus, sur chaque axe. */
    public static Vec3 drift(Random random) {
        return new Vec3(reach(random, JetEngineVisuals.TRAIL_DRIFT),
                reach(random, JetEngineVisuals.TRAIL_DRIFT),
                reach(random, JetEngineVisuals.TRAIL_DRIFT));
    }

    private static double jitter(Random random) {
        return reach(random, JetEngineVisuals.TRAIL_SPREAD);
    }

    /** Un tirage dans [-amount, amount]. */
    private static double reach(Random random, double amount) {
        return (random.nextDouble() * 2 - 1) * amount;
    }
}
