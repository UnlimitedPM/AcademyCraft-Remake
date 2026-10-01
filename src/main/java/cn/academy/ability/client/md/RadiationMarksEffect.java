package cn.academy.ability.client.md;

import cn.academy.ability.meltdowner.RadiationMarks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * La fumee du plasma autour des cibles marquees, portage du {@code onUpdateClient} de
 * {@code MDDamageHelper}.
 *
 * <h2>Ce que fait le passif de radiation, a l'ecran</h2>
 *
 * <p>Rien qu'une chose, et c'est celle-la : une cible touchee par un tir du meltdowner quand le
 * joueur a appris le passif <b>fume du plasma</b> pendant soixante ticks. C'est la seule
 * animation du passif, et elle compte : c'est elle qui dit au joueur, sans le moindre mot, que
 * la cible est marquee — donc que ses coups suivants porteront plus fort. Sans elle, le passif
 * ne se voyait pas du tout.
 *
 * <h2>Ses nombres</h2>
 *
 * <p>Une cible large de {@code w} et haute de {@code h} recoit, <b>par tick</b> :
 *
 * <ul>
 *   <li>de zéro a deux etincelles — le {@code RandUtils.rangei(0, 3)} de l'original, dont la
 *       borne haute est exclue ; un tick sur trois ne montre donc rien ;</li>
 *   <li>posees sur un <b>anneau</b> de 0,6 a 0,7 fois sa largeur — donc collees a son corps, et
 *       non autour d'un rayon fixe, si bien qu'un zombie et un golem ne fument pas de la meme
 *       taille ;</li>
 *   <li>a une hauteur tiree entre ses pieds et son sommet ;</li>
 *   <li>et avec une derive minuscule — deux centimetres par tick, dans n'importe quel sens.</li>
 * </ul>
 *
 * <p>Ce sont les memes etincelles que le rayon de la bille : la particule unique du meltdowner,
 * qui vit 45 a 74 ticks. La fumee reste donc un moment apres que la marque s'est eteinte, ce qui
 * est exactement ce que faisait l'original.
 *
 * <p>Elle se joue chez <b>chaque</b> joueur qui voit la cible, et pas seulement chez le tireur :
 * c'est ce que le paquet de la marque annonce. Voir {@code RadiationMarkPacket}.
 */
@OnlyIn(Dist.CLIENT)
public final class RadiationMarksEffect {

    /** Le nombre d'etincelles d'un tick : de 0 a 2, borne haute exclue. */
    public static final int SPARKS_BOUND = 3;

    /** L'anneau du corps : de 0,6 a 0,7 fois la largeur de la cible. */
    public static final double RING_MIN = 0.6;
    public static final double RING_MAX = 0.7;

    /** Et leur derive : deux centimetres par tick, sur chaque axe, au hasard. */
    public static final double SPEED = 0.02;

    private static final Random RANDOM = new Random();

    private RadiationMarksEffect() {
    }

    /** Un tick du client : chaque cible marquee fume. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (RadiationMarks.isMarked(entity)) {
                sow(entity.position(), entity.getBbWidth(), entity.getBbHeight());
            }
        }
    }

    /**
     * La marque annoncee par le serveur : le client la pose a son tour.
     *
     * <p>La marque ne vit que chez le serveur — c'est lui qui l'applique aux degats — mais ses
     * <b>cles</b> sont celles de l'original, et le client les relit comme lui : c'est ce qui fait
     * que {@code RadiationMarks.isMarked} et son decompte marchent des deux cotes, sans une
     * seconde donnee a tenir.
     */
    public static void apply(int entityId, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        Entity entity = minecraft.level.getEntity(entityId);
        if (entity != null) RadiationMarks.setTicks(entity, ticks);
    }

    /** Seme les etincelles d'un tick sur une cible. */
    private static void sow(Vec3 centre, double width, double height) {
        int count = sparksPerTick(RANDOM);
        for (int i = 0; i < count; i++) {
            MdSparks.spawn(ring(centre, width, height, RANDOM), velocity(RANDOM));
        }
    }

    /** Combien d'etincelles ce tick : 0, 1 ou 2. */
    public static int sparksPerTick(Random random) {
        return random.nextInt(SPARKS_BOUND);
    }

    /**
     * Le point ou nait une etincelle : sur l'anneau du corps, a une hauteur quelconque.
     *
     * <p>L'angle se prend d'un sinus et d'un cosinus, comme dans l'original : c'est ce qui pose
     * les etincelles <b>sur</b> un cercle plutot que dans un carre.
     */
    public static Vec3 ring(Vec3 centre, double width, double height, Random random) {
        double radius = (RING_MIN + random.nextDouble() * (RING_MAX - RING_MIN)) * width;
        double theta = random.nextDouble() * Math.PI * 2;
        return centre.add(Math.sin(theta) * radius, random.nextDouble() * height,
                Math.cos(theta) * radius);
    }

    /** Sa vitesse de depart : deux centimetres par tick, au plus, sur chaque axe. */
    public static Vec3 velocity(Random random) {
        return new Vec3((random.nextDouble() * 2 - 1) * SPEED,
                (random.nextDouble() * 2 - 1) * SPEED,
                (random.nextDouble() * 2 - 1) * SPEED);
    }
}
