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
 *       lampion par arc et par position : ils allument un <b>anneau</b> de lampions autour du
 *       joueur ;</li>
 *   <li>chaque rayon pose une flaque au sol <b>la ou il s'arrete</b>, pour que la lumiere se voie
 *       sur une surface, et pas seulement dans l'air.</li>
 * </ul>
 *
 * <h2>La lumiere n'ecrase pas le joueur</h2>
 *
 * <p>La lumiere d'un bloc eclaire TOUT ce qui l'entoure, le modele du joueur compris. La
 * premiere version posait deux lampions a quinze dans le joueur, a hauteur de torse : sous un
 * shader, son visage devenait une torche et le decor, lui, ne gagnait presque rien. Rien ne se
 * pose donc a moins de {@link #MIN_CLEAR} blocs des yeux, et l'aura d'un essaim est un anneau a
 * {@link #AURA_RING} blocs du joueur — la lumiere eclaire le decor, pas le personnage (le joueur,
 * le 17/11).
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class EffectLights {

    /**
     * Le niveau d'un lampion.
     *
     * <p>Neuf, et pas quinze. La lumiere d'un bloc s'eparpille sur autant de blocs que son
     * niveau : a quinze, chaque lampion de rayon allumait une sphere de quinze blocs, les spheres
     * se fondaient les unes dans les autres et le rayon devenait un halo diffus qui ne se lisait
     * pas comme une ligne (le joueur, le 17/11 : « trop diffuse, elle ne fait pas une ligne
     * droite »). A neuf, le long du rayon reste nettement plus clair que les cotes.
     */
    private static final int LEVEL = 12;

    /** Le nombre de cases allumees pour un seul effet, mesurees le long du rayon. */
    private static final int MAX_PER_EFFECT = 16;

    /** Le nombre maximal de lampions toutes sources confondues. */
    private static final int MAX_TOTAL = 48;

    /**
     * Combien de ticks un lampion survit a sa derniere demande.
     *
     * <p>Quatre : le chapelet est contigu, donc un lampion n'a qu'a couvrir les quelques ticks ou
     * une case passe sous le seuil d'allumage. Trois laissait encore clignoter les cases du bout du
     * rayon (le joueur, le 17/11 : « ca clignote encore un peu, ca fait mal aux yeux »), et huit
     * laissaient les lampions d'un rayon qu'on venait de tourner allumes derriere soi.
     */
    private static final int LINGER_TICKS = 4;

    /**
     * Le sursis de l'anneau d'aura, bien plus long.
     *
     * <p>C'est lui qui clignotait le plus : les six lampions de l'anneau s'allument et s'eteignent
     * ENSEMBLE, des que le dernier arc sort du rayon d'aura — un clignotement large, tres visible.
     * Un sursis de dix ticks couvre largement les va-et-vient d'un essaim.
     */
    private static final int AURA_LINGER_TICKS = 10;

    /** Le nombre de blocs verses vers le haut pour trouver la surface d'une eau. */
    private static final int SURFACE_SCAN = 40;

    /** La distance sous laquelle un arc fait partie de l'aura du joueur, et non d'un trajet. */
    private static final double AURA_RADIUS = 6.0d;

    /** Le rayon de l'anneau qui eclaire autour du joueur quand un essaim l'entoure. */
    private static final double AURA_RING = 3.5d;

    /** Le nombre de lampions de cet anneau. */
    private static final int RING_LAMPS = 6;

    /** Rien ne se pose a moins de cette distance des yeux : c'est le visage du joueur. */
    private static final double MIN_CLEAR = 2.5d;

    /** Le nombre de blocs verses sous un rayon quand il cherche le sol. */
    private static final int GROUND_SCAN = 24;

    /** Les lampions poses, et leur temps de vie restant en ticks. */
    private static final Map<BlockPos, Integer> POSED = new HashMap<>();

    /** Les yeux du joueur pour le tick en cours : c'est la que la lumiere ne va pas. */
    private static Vec3 EYES = Vec3.ZERO;

    private EffectLights() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Branche l'integration d'ombre d'Oculus si elle est la (voir OculusShadowCompat), et
        // tient a jour les porteurs d'ombre des machines (voir ShadowCasters).
        cn.academy.compat.oculus.OculusShadowCompat.ensureInstalled();
        cn.academy.compat.oculus.ShadowCasters.tick();

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
        Set<BlockPos> auraLamps = new LinkedHashSet<>();

        // Les yeux du joueur, avant tout : c'est autour d'eux qu'on ne pose rien.
        Vec3 eyes = minecraft.player.getEyePosition();
        EYES = eyes;

        for (MdRays.LiveRay ray : MdRays.live()) {
            sample(level, ray.from(), ray.to(), wanted);
        }

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
            // L'essaim entoure le joueur : un anneau autour de lui, et surtout pas un lampion
            // sur lui — colles a ses yeux, ils faisaient de son visage une torche.
            ring(level, eyes, auraLamps);
        }

        apply(level, wanted, auraLamps);
    }

    /**
     * L'anneau qui sert d'aura a un essaim d'arcs.
     *
     * <p>Un lampion par arc ne tenait pas : les arcs se deplacent toutes les trois ticks et la
     * lumiere clignotait. Un anneau fixe autour du joueur donne une lumiere stable, et a distance
     * de lui — c'est le decor qu'on eclaire, pas son visage.
     */
    private static void ring(ClientLevel level, Vec3 eyes, Set<BlockPos> out) {
        int y = (int) Math.floor(eyes.y - 1.2d);
        for (int i = 0; i < RING_LAMPS; i++) {
            double angle = i * (Math.PI * 2.0d / RING_LAMPS);
            BlockPos pos = BlockPos.containing(eyes.x + Math.cos(angle) * AURA_RING,
                    y, eyes.z + Math.sin(angle) * AURA_RING);
            air(level, pos, out);
        }
    }

    /**
     * Pose des lampions le long d'un segment, et une flaque la ou le rayon s'arrete.
     *
     * <p>On MARCHE LA GRILLE DU MONDE, deux points par bloc : une case allumee ne depend donc que
     * du trajet du rayon, et pas du decoupage en echantillons. C'est ce qui regle le clignotement
     * qui restait : avec des echantillons espaces, le moindre mouvement du rayon decalait TOUTES
     * les positions d'un coup, donc une case s'eteignait et une autre s'allumait a chaque tick sur
     * toute la longueur, et le moteur de lumiere refaisait la lumiere de la section a chaque fois.
     * En marchant les cases, seules celles du bout du rayon changent.
     *
     * <p>La flaque, elle, n'est posee qu'au bout du rayon : un rayon qui passe a dix blocs du sol
     * n'eclaire presque rien, et c'est la ou il touche que la lumiere doit se voir.
     */
    private static void sample(ClientLevel level, double[] from, double[] to, Set<BlockPos> out) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.01d) return;

        int steps = (int) Math.ceil(distance * 2.0d);
        BlockPos previous = null;
        int lit = 0;
        for (int i = 0; i <= steps && lit < MAX_PER_EFFECT; i++) {
            double t = (double) i / steps;
            BlockPos pos = BlockPos.containing(from[0] + dx * t, from[1] + dy * t, from[2] + dz * t);
            if (pos.equals(previous)) continue;
            previous = pos;

            // On ne remplace JAMAIS un bloc : un rayon qui traverse une paroi ne doit pas
            // y creuser un trou. Seuls les blocs d'air recoivent un lampion.
            int before = out.size();
            air(level, pos, out);
            if (out.size() != before) lit++;
        }
        // Et la ou le rayon s'arrete : une lumiere sur une surface, pas seulement dans l'air.
        ground(level, BlockPos.containing(to[0], to[1], to[2]), out);
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

    /**
     * Ajoute ce bloc s'il est de l'air, et si le budget n'est pas epuise.
     *
     * <p>Sous l'eau, il n'y a pas d'air a allumer — et poser un bloc de lumiere a la place d'un
     * bloc d'eau laisserait un TROU visible dans la mer. On eclaire donc la SURFACE au-dessus du
     * rayon : la lumiere y entre et descend ensuite dans l'eau, qui l'affaiblit mais ne l'arrete
     * pas. C'etait la demande du joueur, « sous l'eau ca ne fait juste aucune lumiere ».
     */
    private static void air(ClientLevel level, BlockPos pos, Set<BlockPos> out) {
        if (out.size() >= MAX_TOTAL) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            if (!nearEyes(pos)) out.add(pos.immutable());
            return;
        }
        if (state.getFluidState().isEmpty()) return;

        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < SURFACE_SCAN && !level.getFluidState(cursor).isEmpty(); i++) {
            cursor.move(0, 1, 0);
        }
        if (level.getBlockState(cursor).isAir() && !nearEyes(cursor)) {
            out.add(cursor.immutable());
        }
    }

    /** Vrai trop pres des yeux : la lumiere d'un bloc eclaire aussi le modele du joueur. */
    private static boolean nearEyes(BlockPos pos) {
        double dx = pos.getX() + 0.5d - EYES.x;
        double dy = pos.getY() + 0.5d - EYES.y;
        double dz = pos.getZ() + 0.5d - EYES.z;
        return dx * dx + dy * dy + dz * dz < MIN_CLEAR * MIN_CLEAR;
    }

    /**
     * Pose ce qui manque, fait vieillir ce qui n'est plus demande.
     *
     * <p>Deux sursis : celui des rayons, court, et celui de l'anneau d'aura, long — c'est lui qui
     * evite le clignotement large des six lampions qui vont ensemble.
     */
    private static void apply(ClientLevel level, Set<BlockPos> wanted, Set<BlockPos> aura) {
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LEVEL);

        // Une case demandee par les deux sources prend le plus long des deux sursis.
        Map<BlockPos, Integer> holds = new HashMap<>();
        for (BlockPos pos : wanted) holds.merge(pos, LINGER_TICKS, Math::max);
        for (BlockPos pos : aura) holds.merge(pos, AURA_LINGER_TICKS, Math::max);

        for (Map.Entry<BlockPos, Integer> entry : holds.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (isOurs(state)) {
                // Deja allumee : le sursis se renouvelle, et jamais ne raccourcit.
                POSED.merge(pos, entry.getValue(), Math::max);
                continue;
            }
            // Le chunk a pu etre recharge entre-temps : on ne pose que sur de l'air.
            if (!state.isAir()) continue;
            level.setBlock(pos, light, Block.UPDATE_ALL);
            POSED.put(pos, entry.getValue());
        }

        for (Iterator<Map.Entry<BlockPos, Integer>> it = POSED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            if (holds.containsKey(entry.getKey())) continue;
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
