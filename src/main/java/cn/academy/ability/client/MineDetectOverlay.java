package cn.academy.ability.client;

import java.util.ArrayList;
import java.util.List;

import cn.academy.AcademyCraft;
import cn.academy.ability.electromaster.MetalTargets;
import cn.academy.ability.electromaster.MineDetectSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * L'eclat de la detection de minerais, chez le joueur : le temps qu'il dure, et les minerais
 * qu'il a allumes.
 *
 * <p>C'est le portage de l'entite de rendu de l'original, sans entite : l'etat vit ici, le
 * balayage se relit sur le monde du client — comme l'original, qui lisait deja les blocs chez
 * le joueur — et le dessin se fait pendant le rendu du monde, comme le bouclier de lumiere.
 *
 * <p>Le balayage n'a lieu que tous les cinq ticks, et s'arrete au-dela de huit mille quatre
 * cents minerais : c'est le rythme et la borne de l'original, et sans eux un rayon de
 * vingt-huit blocs se relirait cent quatre-vingt mille fois par seconde.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class MineDetectOverlay {

    /** Un minerai allume : ou il est, et le palier de couleur qu'il a gagne. */
    public record Ore(BlockPos pos, int tier) {}

    /** Fin de l'eclat, en ticks du client. */
    private static int endTick = -1;
    private static double range;
    private static boolean advanced;
    private static int scanCooldown;

    private static List<Ore> ores = List.of();

    private MineDetectOverlay() {}

    /**
     * L'eclat commence : le client sait qu'il a le droit de regarder.
     *
     * <p>Appele par le paquet, donc seulement quand le serveur a paye la competence. Le
     * balayage est demande tout de suite, sans attendre la periode : un eclat dont les
     * premiers minerais n'apparaitraient que cinq ticks plus tard aurait l'air en retard.
     */
    public static void begin(float range, boolean advanced) {
        LocalPlayer player = Minecraft.getInstance().player;
        endTick = (player == null ? 0 : player.tickCount) + MineDetectSkill.TIME;
        MineDetectOverlay.range = MineDetectVisuals.capRange(range);
        MineDetectOverlay.advanced = advanced;
        scanCooldown = 0;
        ores = List.of();
    }

    /** Vrai tant que l'eclat dure. */
    public static boolean active(LocalPlayer player) {
        return player != null && player.tickCount < endTick;
    }

    public static double range() {
        return range;
    }

    public static boolean advanced() {
        return advanced;
    }

    public static List<Ore> ores() {
        return ores;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (!active(player)) {
            ores = List.of();
            return;
        }
        if (scanCooldown-- > 0) return;
        scanCooldown = MineDetectVisuals.SCAN_PERIOD;
        ores = scan(player);
    }

    /**
     * Relit les minerais autour du joueur.
     *
     * <p>Une sphere, et l'air ignore : c'est le {@code getBlocksWithin} de l'original, avec sa
     * borne sur le nombre de minerais trouves. Rien n'est trie : l'ordre du balayage est celui
     * du monde, et personne ne le voit.
     */
    private static List<Ore> scan(LocalPlayer player) {
        Level level = player.level();
        BlockPos centre = player.blockPosition();
        int radius = (int) Math.ceil(range);
        double limit = range * range;

        List<Ore> found = new ArrayList<>();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > limit) continue;
                    BlockPos pos = centre.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (!MetalTargets.isOreBlock(state)) continue;
                    found.add(new Ore(pos.immutable(),
                            MineDetectVisuals.tierOf(advanced, MetalTargets.harvestTier(state))));
                    if (found.size() >= MineDetectVisuals.SCAN_LIMIT) return found;
                }
            }
        }
        return found;
    }
}
