package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.arc.ClientArcs;
import cn.academy.ability.client.md.MdRays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * La lumiere que les rayons et les eclairs jettent autour d'eux.
 *
 * <p>Le port dessinait ses lasers et ses arcs en emission, mais ils n'eclairaient rien : la
 * lumiere d'un monde vient des blocs, et une bande dessinee par le client n'en pose aucun. Sous
 * un shader, le rayon restait donc une image brillante sans que le sol autour de lui change de
 * teinte, ce que le joueur a signale pour le meltdowner et pour l'electromaster.
 *
 * <p>La seule lumiere que TOUS les shaders comprennent est celle d'un bloc. On pose donc, cote
 * client seulement, des blocs de lumiere ({@code minecraft:light}) le long des rayons vivants et
 * des eclairs vivants, et on les retire quand ils s'eteignent. Les blocs de lumiere sont
 * invisibles et sans collision : ils ne changent rien au jeu, seulement a la lumiere que la
 * carte graphique recoit. Rien n'est envoye au serveur — le serveur ne doit pas connaitre ces
 * lampions, et une synchronisation de chunk les effacerait de toute facon, ce qui est sans
 * consequence puisqu'ils sont reposés au tick suivant tant que l'effet dure.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class EffectLights {

    /** Le niveau d'un lampe : le maximum, comme une torche de lumiere. */
    private static final int LEVEL = 15;

    /** Un lampe tous les quatre blocs : assez pour que la lumiere se lise, sans en poser trop. */
    private static final double STEP = 4.0d;

    /** Le nombre maximal de lampions pour un seul effet, au cas ou le rayon serait tres long. */
    private static final int MAX_PER_EFFECT = 8;

    /** Les positions que NOUS avons posees, pour les retirer quand l'effet s'eteint. */
    private static final Set<BlockPos> PLACED = new HashSet<>();

    private EffectLights() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            // Quitter un monde emporte les lampions avec lui.
            PLACED.clear();
            return;
        }
        // En pause, les effets eux-memes sont figes : on laisse leur lumiere telle quelle.
        if (minecraft.player == null || minecraft.isPaused()) return;

        Set<BlockPos> wanted = new HashSet<>();
        for (MdRays.LiveRay ray : MdRays.live()) {
            sample(level, ray.from(), ray.to(), wanted);
        }
        for (ClientArcs.LiveArc arc : ClientArcs.live()) {
            sample(level, arc.from(), arc.to(), wanted);
        }

        apply(level, wanted);
    }

    /** Pose des lampions le long d'un segment, la ou il passe dans de l'air. */
    private static void sample(ClientLevel level, double[] from, double[] to, Set<BlockPos> out) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.01d) return;

        int steps = (int) Math.min(MAX_PER_EFFECT,
                Math.max(1.0d, Math.ceil(distance / STEP)));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            BlockPos pos = BlockPos.containing(from[0] + dx * t, from[1] + dy * t, from[2] + dz * t);
            // On ne remplace JAMAIS un bloc : un rayon qui traverse une paroi ne doit pas
            // y creuser un trou. Seuls les blocs d'air recoivent un lampion.
            if (level.getBlockState(pos).isAir()) out.add(pos);
        }
    }

    /** Pose ce qui manque, retire ce qui n'est plus demande. */
    private static void apply(ClientLevel level, Set<BlockPos> wanted) {
        for (Iterator<BlockPos> it = PLACED.iterator(); it.hasNext(); ) {
            BlockPos pos = it.next();
            if (wanted.contains(pos)) continue;
            if (isOurs(level.getBlockState(pos))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            it.remove();
        }

        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LEVEL);
        for (BlockPos pos : wanted) {
            BlockState state = level.getBlockState(pos);
            if (isOurs(state)) {
                PLACED.add(pos);
                continue;
            }
            // Le chunk a pu etre recharge entre-temps : on ne pose que sur de l'air.
            if (!state.isAir()) continue;
            level.setBlock(pos, light, Block.UPDATE_ALL);
            PLACED.add(pos);
        }
    }

    /** Vrai pour un lampion que nous avons pose (et pas pour un bloc de lumiere du joueur). */
    private static boolean isOurs(BlockState state) {
        return state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == LEVEL;
    }
}
