package cn.academy.ability.client;

import cn.academy.ability.client.arc.SurroundArcs;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Le renfort du corps : l'electricite qui grimpe le long du personnage.
 *
 * <p>Portage d'{@code EntityIntensifyEffect}. L'original semait son gresillement sur SEPT
 * hauteurs — 2, 1,8, 1,5, 1, 0,5, 0 et moins 0,1 bloc au-dessus des pieds — en <b>descendant</b> :
 * une hauteur apres 0, 1, 3, 4, 6, 7 et 8 ticks, de la tete vers le sol. Chacune portait trois
 * ou quatre arcs, poses sur un <b>anneau</b> de 0,5 a 0,6 bloc de rayon, et chacun vivait trois
 * ticks. Le tout etait fini en quinze : une onde qui descend, pas une nappe.
 *
 * <p>L'original en faisait une entite cliente qui suivait le joueur. Ici c'est l'inverse : le
 * serveur annonce l'activation ({@code BodyIntensifyPacket}), et ce crochet-ci rejoue les sept
 * hauteurs chez chaque client, en lisant la position du joueur au moment de chacune. Le resultat
 * est le meme — les anneaux suivent le corps — sans rien a suivre dans le monde.
 *
 * <p>Les nombres sont ceux de l'original, et le <b>rayon de l'anneau</b> merite d'etre souligne :
 * son {@code phi} de 0,5 a 0,6 est une distance a l'axe, pas une demi-largeur de boite. C'est ce
 * qui met les arcs juste <b>autour</b> du corps — un joueur fait 0,6 de large, donc 0,3 de
 * rayon — et c'est la seule chose qui distingue cette onde de l'essaim d'une machine, qui lui se
 * tire dans un cube.
 */
@OnlyIn(Dist.CLIENT)
public final class BodyIntensifyEffect {

    /** Les sept hauteurs de l'original, en blocs au-dessus des pieds. */
    private static final double[] HEIGHTS = { 2.0, 1.8, 1.5, 1.0, 0.5, 0.0, -0.1 };

    /** Le tick de chacune : l'onde descend, de la tete vers le sol. */
    private static final int[] DELAYS = { 0, 1, 3, 4, 6, 7, 8 };

    /** L'anneau : de 0,5 a 0,6 bloc du corps, soit juste au-dela de ses 0,3 de rayon. */
    private static final double RING_MIN = 0.5;
    private static final double RING_MAX = 0.6;

    /** Trois ou quatre arcs par hauteur, comme son {@code RandUtils.rangei(3, 4)}. */
    private static final int ARC_MIN = 3;
    private static final int ARC_MAX = 4;

    /** La vie de l'entite de l'original : quinze ticks, soit trois quarts de seconde. */
    private static final int LIFE_TICKS = 15;

    /** Sa {@code BLEND_OUT_TIME} : le voile du HUD s'efface en 200 millisecondes. */
    private static final long VEIL_MILLIS = 200L;

    private static final RandomSource RANDOM = RandomSource.create();

    /** Le porteur, ou moins un quand rien ne se joue. */
    private static int ownerId = -1;

    /** Le tick de l'onde en cours. */
    private static int age;

    /** L'instant ou le voile du HUD aura fini de s'effacer. */
    private static long veilUntil;

    private BodyIntensifyEffect() {
    }

    /** Lance l'onde sur ce porteur. Appele par le paquet, et par personne d'autre. */
    public static void start(int owner) {
        ownerId = owner;
        age = 0;
        veilUntil = Util.getMillis() + VEIL_MILLIS;
    }

    /**
     * Un tick de l'onde : la ou les hauteurs qui tombent sur cet age sont semees.
     *
     * <p>Le porteur est relu a chaque tick, et non garde de cote : c'est ce qui fait suivre les
     * anneaux quand le joueur avance. Un porteur qui disparait — deconnecte, sorti de la portee —
     * arrete tout, sans quoi l'onde resterait ouverte a jamais.
     */
    public static void tick() {
        if (ownerId < 0) return;

        Entity owner = owner();
        if (owner == null) {
            end();
            return;
        }

        for (int i = 0; i < HEIGHTS.length; i++) {
            if (DELAYS[i] == age) {
                sow(owner, HEIGHTS[i]);
            }
        }

        if (++age > LIFE_TICKS) {
            end();
        }
    }

    /** Arrete l'onde en cours. */
    public static void end() {
        ownerId = -1;
        age = 0;
    }

    /**
     * L'opacite du voile bleu du HUD, de un a zero en 200 millisecondes.
     *
     * <p>L'original n'affichait ce voile qu'a l'activation et a la fin d'une charge tenue : le
     * port n'a pas de charge a tenir, donc c'est le seul moment ou il se pose.
     */
    public static float veilAlpha() {
        long left = veilUntil - Util.getMillis();
        if (left <= 0L) return 0f;
        return Math.min(1f, left / (float) VEIL_MILLIS);
    }

    /** Le porteur, s'il est dans le monde du client. */
    private static Entity owner() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : minecraft.level.getEntity(ownerId);
    }

    /** Une hauteur de l'onde : trois ou quatre arcs sur son anneau. */
    private static void sow(Entity owner, double height) {
        int count = ARC_MIN + RANDOM.nextInt(ARC_MAX - ARC_MIN + 1);
        Vec3 base = owner.position();
        List<Vec3> points = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2.0;
            double radius = RING_MIN + RANDOM.nextDouble() * (RING_MAX - RING_MIN);
            points.add(base.add(Math.sin(theta) * radius, height, Math.cos(theta) * radius));
        }

        SurroundArcs.spawnAt(SurroundArcs.THIN, points, ownerId, RANDOM);
    }
}
