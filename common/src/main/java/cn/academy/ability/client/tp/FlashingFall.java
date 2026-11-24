package cn.academy.ability.client.tp;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * La retombee du scintillement : la descente est LENTE, et des etincelles tournent autour du
 * joueur.
 *
 * <p>Demande du joueur, 22/10 : « le vrai flashing nous teleporte aussi avec 1 bloc de hauteur,
 * mais normalement on doit avoir un effet qui nous fait retomber lentement pendant un certain
 * temps avec des particules autour du joueur ». C'est le portage du {@code GravityCancellor} de
 * l'original : quarante ticks pendant lesquels la vitesse verticale remonte de 0,072 a chaque
 * tick. La gravite d'un joueur valant 0,08, il reste un centieme par tick — on descend au lieu
 * de tomber.
 *
 * <p>CHEZ LE CLIENT, et pas au serveur. C'est la lecon que le port a payee deux fois (voir la
 * memoire du depot) : le client est maitre de son propre mouvement, et la vitesse que le serveur
 * lui pose est reprise au tick suivant. L'original, lui, enregistrait son cancellor depuis
 * {@code clientPerform} — donc chez le client aussi, et c'est bien ce que l'on refait ici.
 *
 * <p>Les etincelles sont celles de {@link TpParticles}, l'image de la teleportation : une gerbe
 * au depart, puis quelques grains par tick tant que le joueur descend. Elles s'arretent des qu'il
 * touche le sol, et le compte a rebours s'ecoule quand meme.
 */
public final class FlashingFall {

    /** La duree de la retombee, en ticks : celle du {@code GravityCancellor} de l'original. */
    public static final int TICKS = 40;

    /**
     * Ce que le {@code GravityCancellor} rendait a la vitesse verticale, chaque tick.
     *
     * <p>C'est le chiffre de l'original, tel quel — et c'est bien 0,072 devant la gravite de 0,08 :
     * l'effet visible n'est pas de flotter mais de tomber LENTEMENT.
     */
    public static final double LIFT = 0.072;

    /** Les etincelles du saut lui-meme, et celles de chaque tick de descente. */
    public static final int BURST = 14;
    public static final int PER_TICK = 2;

    private static final Random RANDOM = new Random();

    /** Les ticks de descente qui restent ; zero quand il n'y a rien a faire. */
    private static int ticks;

    private FlashingFall() {
    }

    /**
     * Un saut vient de partir : la retombee est armee, et une gerbe part du corps.
     *
     * <p>Armer de nouveau repart de quarante ticks, il ne s'additionne pas : enchainer deux sauts
     * ne fait pas descendre deux fois plus lentement.
     */
    public static void begin(Player player) {
        ticks = TICKS;
        if (player == null) return;
        for (int i = 0; i < BURST; i++) {
            spawnAround(player);
        }
    }

    /**
     * Un tick : la vitesse verticale remonte tant qu'il reste de la retombee, et quelques
     * etincelles tant que le joueur est en l'air.
     *
     * <p>La remontee s'arrete en vol libre dirige (l'original testait
     * {@code capabilities.isFlying}) et une fois au sol — sans quoi elle retiendrait le joueur
     * au lieu de le poser.
     */
    public static void tick(Player player) {
        if (ticks <= 0) return;
        if (player == null) {
            ticks = 0;
            return;
        }
        if (!player.getAbilities().flying && !player.onGround()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, motion.y + LIFT, motion.z);
            for (int i = 0; i < PER_TICK; i++) {
                spawnAround(player);
            }
        }
        ticks--;
    }

    /** Tout oublier : quitter un monde n'est pas la fin d'un saut. */
    public static void clear() {
        ticks = 0;
    }

    /** Une etincelle autour du corps, sans direction privilegiee. */
    private static void spawnAround(Player player) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double radius = 0.35 + RANDOM.nextDouble() * 0.35;
        Vec3 pos = player.position().add(
                Math.cos(angle) * radius,
                0.2 + RANDOM.nextDouble() * 1.4,
                Math.sin(angle) * radius);
        Vec3 velocity = new Vec3(
                (RANDOM.nextDouble() - 0.5) * 0.04,
                0.01 + RANDOM.nextDouble() * 0.03,
                (RANDOM.nextDouble() - 0.5) * 0.04);
        TpParticles.spawn(pos, velocity);
    }
}
