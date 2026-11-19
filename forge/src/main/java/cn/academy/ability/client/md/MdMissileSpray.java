package cn.academy.ability.client.md;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * L'anneau de plasma du missile electronique, portage du {@code c_updateEffect} d'{@code EMContext}.
 *
 * <h2>Ce que le missile montre de lui-meme</h2>
 *
 * <p>Ses billes se voient ({@code EntityMdBall}, dessinees par leur propre rendu), mais c'est
 * <b>cet</b> anneau qui dit au joueur que la competence est ouverte : tant que le maintien dure,
 * le plasma monte autour de lui, du haut du torse aux hanches. C'est la seule animation de
 * contact du missile, et elle se joue <b>chez le lanceur seul</b> — l'original l'envoyait a son
 * propre client, pas a ceux qui le regardaient.
 *
 * <h2>Ses nombres</h2>
 *
 * <p>Un tick du maintien pose de <b>une a deux</b> etincelles — le {@code RandUtils.rangei(1, 3)}
 * de l'original, borne haute exclue — sur un anneau de 0,5 a 1 bloc autour du corps, a une
 * hauteur prise entre 1,2 bloc sous la poitrine et la poitrine. C'est le {@code getHeightFix} de
 * l'original, cette hauteur d'yeux de 1,6 bloc qu'on retrouve partout dans le meltdowner.
 *
 * <p>Elles montent : leur derive va de un a cinq centimetres vers le haut, et de deux centimetres
 * au plus sur les cotes. C'est ce qui distingue l'anneau du missile de la fumee de la radiation,
 * qui derive dans n'importe quel sens — ici le plasma s'eleve, et la silhouette du lanceur se
 * couvre de bas en haut.
 */
@OnlyIn(Dist.CLIENT)
public final class MdMissileSpray {

    /** Le nombre d'etincelles d'un tick : de 1 a 2, borne haute exclue. */
    public static final int COUNT_BOUND = 3;

    /** L'anneau autour du corps : de 0,5 a 1 bloc. */
    public static final double RING_MIN = 0.5;
    public static final double RING_MAX = 1.0;

    /** La hauteur du centre : le {@code getHeightFix} de l'original, 1,6 bloc au-dessus des pieds. */
    public static final double HEIGHT_FIX = 1.6;

    /** Puis on redescend : de 1,2 bloc plus bas jusqu'au centre, soit de la poitrine aux hanches. */
    public static final double DROP_MIN = -1.2;
    public static final double DROP_MAX = 0.0;

    /** Et la derive : deux centimetres de cote au plus, et une montee de un a cinq. */
    public static final double DRIFT_XZ = 0.02;
    public static final double RISE_MIN = 0.01;
    public static final double RISE_MAX = 0.05;

    private static final Random RANDOM = new Random();

    private MdMissileSpray() {
    }

    /**
     * L'anneau d'un tick, annonce par le serveur.
     *
     * <p>Le paquet ne porte qu'un numero d'entite : tout le reste est l'affaire du client, qui
     * relit la position du porteur et tire ses propres nombres. C'est exactement ce que
     * l'original envoyait — un message sans contenu, juste « un tick de plus ».
     */
    public static void apply(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        Entity caster = minecraft.level.getEntity(entityId);
        if (caster != null) sow(caster.position(), RANDOM);
    }

    /** Seme les etincelles d'un tick autour d'un porteur. */
    public static void sow(Vec3 feet, Random random) {
        int count = perTick(random);
        for (int i = 0; i < count; i++) {
            MdSparks.spawn(ring(feet, random), drift(random));
        }
    }

    /** Combien d'etincelles ce tick : une ou deux. */
    public static int perTick(Random random) {
        return 1 + random.nextInt(COUNT_BOUND - 1);
    }

    /**
     * Le point ou nait une etincelle : sur l'anneau du corps, a une hauteur tiree.
     *
     * <p>L'angle se prend d'un sinus et d'un cosinus, comme dans l'original : c'est ce qui pose
     * les etincelles <b>sur</b> un cercle plutot que dans un carre.
     */
    public static Vec3 ring(Vec3 feet, Random random) {
        double radius = RING_MIN + random.nextDouble() * (RING_MAX - RING_MIN);
        double theta = random.nextDouble() * Math.PI * 2;
        double height = HEIGHT_FIX + DROP_MIN + random.nextDouble() * (DROP_MAX - DROP_MIN);
        return new Vec3(feet.x + Math.sin(theta) * radius, feet.y + height,
                feet.z + Math.cos(theta) * radius);
    }

    /** Sa vitesse de depart : elle monte, et derive a peine de cote. */
    public static Vec3 drift(Random random) {
        return new Vec3((random.nextDouble() * 2 - 1) * DRIFT_XZ,
                RISE_MIN + random.nextDouble() * (RISE_MAX - RISE_MIN),
                (random.nextDouble() * 2 - 1) * DRIFT_XZ);
    }
}
