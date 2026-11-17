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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
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
 * carte graphique recoit.
 *
 * <h2>Pas de clignotement</h2>
 *
 * <p>La premiere version posait et retirait les lampions a chaque tick, en suivant les rayons a
 * la lettre. Comme un rayon miner bouge avec le regard et que les arcs d'un essaim changent de
 * place toutes les trois ticks, une partie des lampions s'allumait et s'eteignait sans arret : le
 * joueur a vu clignoter la lumiere. Trois choses reglent cela ici :
 * <ul>
 *   <li>un lampion <b>survit</b> quelques ticks apres avoir cesse d'etre demande
 *       ({@link #LINGER_TICKS}), ce qui lisse les deplacements d'un tick a l'autre ;</li>
 *   <li>les arcs proches du joueur (les essaims de la charge et du renfort) n'allument plus un
 *       lampion par arc et par position : ils allument une <b>aura stable</b> autour du joueur ;</li>
 *   <li>chaque rayon projette aussi un lampion au sol, sous lui, pour que la lumiere se voie sur
 *       une surface, et pas seulement dans l'air.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class EffectLights {

    /** Le niveau d'un lampion : le maximum, comme une torche de lumiere. */
    private static final int LEVEL = 15;

    /** Un lampion tous les quatre blocs : assez pour que la lumiere se lise. */
    private static final double STEP = 4.0d;

    /** Le nombre maximal de lampions pour un seul effet. */
    private static final int MAX_PER_EFFECT = 8;

    /** Le nombre maximal de lampions toutes sources confondues. */
    private static final int MAX_TOTAL = 48;

    /** Combien de ticks un lampion survit a sa derniere demande. */
    private static final int LINGER_TICKS = 8;

    /** La distance sous laquelle un arc fait partie de l'aura du joueur, et non d'un trajet. */
    private static final double AURA_RADIUS = 6.0d;

    /** Le nombre de blocs verses sous un rayon quand il cherche le sol. */
    private static final int GROUND_SCAN = 24;

    /** Les lampions poses, et leur temps de vie restant en ticks. */
    private static final Map<BlockPos, Integer> POSED = new HashMap<>();

    private EffectLights() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Branche l'integration d'ombre d'Oculus si elle est la (voir OculusShadowCompat).
        cn.academy.compat.oculus.OculusShadowCompat.ensureInstalled();

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            // Quitter un monde emporte les lampions avec lui.
            POSED.clear();
            return;
        }
        // En pause, les effets eux-memes sont figes : on laisse leur lumiere telle quelle.
        if (minecraft.player == null || minecraft.isPaused()) return;

        Set<BlockPos> wanted = new LinkedHashSet<>();

        for (MdRays.LiveRay ray : MdRays.live()) {
            sample(level, ray.from(), ray.to(), wanted);
        }

        Vec3 eyes = minecraft.player.getEyePosition();
        boolean aura = false;
        for (ClientArcs.LiveArc arc : ClientArcs.live()) {
            double[] from = arc.from();
            double[] to = arc.to();
            double midX = (from[0] + to[0]) / 2.0;
            double midY = (from[1] + to[1]) / 2.0;
            double midZ = (from[2] + to[2]) / 2.0;
            if (eyes.distanceToSqr(midX, midY, midZ) <= AURA_RADIUS * AURA_RADIUS) {
                aura = true;
            } else {
                sample(level, from, to, wanted);
            }
        }
        if (aura) {
            // L'essaim entoure le joueur : une aura fixe autour de lui, pas une lumiere par arc.
            BlockPos feet = BlockPos.containing(eyes.x, eyes.y - 0.5d, eyes.z);
            air(level, feet, wanted);
            air(level, feet.above(), wanted);
        }

        apply(level, wanted);
    }

    /** Pose des lampions le long d'un segment, dans l'air et sur le sol en dessous. */
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
            double x = from[0] + dx * t;
            double y = from[1] + dy * t;
            double z = from[2] + dz * t;
            BlockPos pos = BlockPos.containing(x, y, z);
            // On ne remplace JAMAIS un bloc : un rayon qui traverse une paroi ne doit pas
            // y creuser un trou. Seuls les blocs d'air recoivent un lampion.
            air(level, pos, out);
            // Et le sol en dessous : la lumiere se voit sur une surface, pas au milieu du vide.
            ground(level, pos, out);
        }
    }

    /**
     * Le premier air au-dessus du sol sous ce point.
     *
     * <p>Un rayon qui passe a dix blocs du sol n'y depose presque rien (la lumiere perd un niveau
     * par bloc). Le lampion pose au sol donne au contraire un maximum la ou le joueur regarde la
     * lumiere.
     */
    private static void ground(ClientLevel level, BlockPos from, Set<BlockPos> out) {
        int minY = Math.max(level.getMinBuildHeight(), from.getY() - GROUND_SCAN);
        BlockPos.MutableBlockPos cursor = from.mutable();
        while (cursor.getY() > minY && level.getBlockState(cursor).isAir()) {
            cursor.move(0, -1, 0);
        }
        if (level.getBlockState(cursor).isAir()) return;
        air(level, cursor.above(), out);
    }

    /** Ajoute ce bloc s'il est de l'air, et si le budget n'est pas epuise. */
    private static void air(ClientLevel level, BlockPos pos, Set<BlockPos> out) {
        if (out.size() >= MAX_TOTAL) return;
        if (level.getBlockState(pos).isAir()) out.add(pos.immutable());
    }

    /** Pose ce qui manque, fait vieillir ce qui n'est plus demande. */
    private static void apply(ClientLevel level, Set<BlockPos> wanted) {
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LEVEL);

        for (BlockPos pos : wanted) {
            BlockState state = level.getBlockState(pos);
            if (isOurs(state)) {
                POSED.put(pos, LINGER_TICKS);
                continue;
            }
            // Le chunk a pu etre recharge entre-temps : on ne pose que sur de l'air.
            if (!state.isAir()) continue;
            level.setBlock(pos, light, Block.UPDATE_ALL);
            POSED.put(pos, LINGER_TICKS);
        }

        for (Iterator<Map.Entry<BlockPos, Integer>> it = POSED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            if (wanted.contains(entry.getKey())) continue;
            int left = entry.getValue() - 1;
            if (left > 0) {
                entry.setValue(left);
                continue;
            }
            BlockPos pos = entry.getKey();
            if (isOurs(level.getBlockState(pos))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            it.remove();
        }
    }

    /** Vrai pour un lampion que nous avons pose (et pas pour un bloc de lumiere du joueur). */
    private static boolean isOurs(BlockState state) {
        return state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == LEVEL;
    }
}
