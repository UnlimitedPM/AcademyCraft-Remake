package cn.academy.ability.client.tp;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * La retombee du scintillement : des etincelles autour du joueur pendant qu'il redescend.
 *
 * <p>Demande du joueur, 22/10 : « on doit avoir un effet qui nous fait retomber lentement pendant
 * un certain temps avec des particules autour du joueur ». La descente lente, elle, est celle du
 * {@code GravityCancellor} de l'original et vit cote serveur ({@code AbilityData.suspendGravity}) ;
 * ce qui se voit autour du joueur est ici, chez son porteur seul — exactement comme l'original,
 * dont le cancellor etait une affaire de client.
 *
 * <p>Une <b>gerbe</b> au depart (le saut vient de partir), puis quelques etincelles par tick tant
 * que le joueur descend. Elles s'arretent des qu'il touche le sol : une retombee posee ne fait plus
 * d'etincelles, et le compte a rebours s'ecoule quand meme (quarante ticks, comme la retombee).
 *
 * <p>Les etincelles sont celles de {@link TpParticles}, l'image de la teleportation : meme famille
 * que les traînées du saut traversant et les marques de la visee.
 */
public final class FlashingFall {

    /** La duree de la retombee, en ticks : celle du {@code GravityCancellor} de l'original. */
    public static final int TICKS = 40;

    /** Les etincelles du saut lui-meme, et celles de chaque tick de descente. */
    public static final int BURST = 14;
    public static final int PER_TICK = 2;

    private static final Random RANDOM = new Random();

    /** Les ticks de descente qui restent ; zero quand il n'y a rien a voir. */
    private static int ticks;

    private FlashingFall() {
    }

    /** Un saut vient de partir : une gerbe, et le compte a rebours de la descente. */
    public static void begin(Player player) {
        ticks = TICKS;
        if (player == null) return;
        for (int i = 0; i < BURST; i++) {
            spawnAround(player);
        }
    }

    /** Un tick : quelques etincelles tant que le joueur descend, puis plus rien. */
    public static void tick(Player player) {
        if (ticks <= 0) return;
        if (player == null) {
            ticks = 0;
            return;
        }
        if (!player.onGround() && !player.getAbilities().flying) {
            for (int i = 0; i < PER_TICK; i++) {
                spawnAround(player);
            }
        }
        ticks--;
    }

    /** Tout oublier : la deconnexion d'un monde n'est pas une fin de saut. */
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
