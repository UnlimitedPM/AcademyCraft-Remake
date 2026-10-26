package cn.academy.ability.client;

import java.util.ArrayList;
import java.util.List;

import cn.academy.AcademyCraft;
import cn.academy.ability.electromaster.MetalTargets;
import cn.academy.ability.electromaster.MineDetectSkill;
import cn.academy.sound.client.FollowSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
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

    /** Un minerai allume : ou il est, le palier de couleur qu'il a gagne, et s'il est le CHOIX. */
    public record Ore(BlockPos pos, int tier, boolean selected) {}

    /** Fin de l'eclat, en ticks du client. */
    private static int endTick = -1;
    private static double range;
    private static boolean advanced;
    private static int scanCooldown;

    /** L'eclat tient-il par la reserve ? Alors c'est le paquet d'arret qui l'eteint. */
    private static boolean holding;

    /**
     * Et au plus tard, celui-la : trois minutes.
     *
     * <p>Un paquet d'arret perdu (deconnexion, changement de dimension) laisserait sinon des
     * minerais allumes pour toujours. Le serveur, lui, s'arrete bien avant : c'est une borne de
     * securite, pas une duree.
     */
    private static int holdEnd = -1;

    private static List<Ore> ores = List.of();

    /**
     * Le grondement de l'eclat, et ce qu'il lui reste a jouer, en ticks.
     *
     * <p>L'ogg dure 6,8 secondes — cent trente-sept ticks —, et l'eclat peut tenir bien plus
     * longtemps que lui : a l'origine le pouvoir durait cinq secondes, donc un seul envoi
     * suffisait. Depuis qu'il vit tant que la reserve paie, le son se <b>relance</b> a chaque
     * fin du fichier, et c'est {@link #stop()} qui le fait taire — le joueur, 26/10 : « le son
     * du pouvoir [ne reste pas] pendant tout le pouvoir ».
     */
    private static final int SOUND_TICKS = 137;

    private static SoundInstance sound;
    private static int soundTicks;

    private MineDetectOverlay() {}

    /**
     * L'eclat commence : le client sait qu'il a le droit de regarder.
     *
     * <p>Appele par le paquet, donc seulement quand le serveur a paye la competence. Le
     * balayage est demande tout de suite, sans attendre la periode : un eclat dont les
     * premiers minerais n'apparaitraient que cinq ticks plus tard aurait l'air en retard.
     */
    public static void begin(float range, boolean advanced, int ticks, boolean hold) {
        LocalPlayer player = Minecraft.getInstance().player;
        int now = player == null ? 0 : player.tickCount;
        endTick = now + ticks;
        holding = hold;
        holdEnd = now + 3 * 60 * 20;
        MineDetectOverlay.range = MineDetectVisuals.capRange(range);
        MineDetectOverlay.advanced = advanced;
        scanCooldown = 0;
        ores = List.of();

        // Le son de l'eclat, chez le seul joueur concerne : l'original le jouait dans le meme
        // gestionnaire client, sur le meme message. Et il le SUIT : c'est un grondement de plusieurs
        // secondes, et pose a une position il restait derriere le joueur qui avançait — « le bruit ne
        // nous suit pas » (26/10). Voir FollowSound.
        playSound(player);
    }

    /**
     * Joue le grondement de l'eclat, et retient de quoi l'eteindre et le relancer.
     *
     * <p>Appele a l'ouverture, puis par {@link #onClientTick} quand le fichier est arrive a son
     * terme : c'est le meme son qui repart, sur le joueur, sans coupure notable.
     */
    private static void playSound(LocalPlayer player) {
        // Un eclat precedent peut gronder encore : un seul grondement a la fois.
        stopSound();
        sound = FollowSound.play(player, cn.academy.ModSounds.EM_MINEDETECT.get(),
                SoundSource.AMBIENT, 0.5f, 1.0f);
        soundTicks = SOUND_TICKS;
    }

    /** Eteint le grondement, a l'arret de l'eclat. */
    private static void stopSound() {
        FollowSound.stop(sound);
        sound = null;
        soundTicks = 0;
    }

    /**
     * Vrai tant que l'eclat dure.
     *
     * <p>Deux cas, et c'est le SERVEUR qui decide dans les deux : les {@code ticks} du paquet
     * d'ouverture (cent, l'eclat de l'original), puis le maintien, qu'il tient tant que la
     * reserve paie — jusqu'au paquet d'arret.
     *
     * <p>Le miroir client des maintiens n'est PAS consulte : il ne porte pas les maintiens (voir
     * {@code ClientAbilityData}), donc il repondait toujours « non » et l'eclat s'eteignait au bout
     * des cinq secondes alors que le serveur, lui, continuait de payer — ce que le joueur a vu :
     * « apres les 5 secondes, l'effet cesse de fonctionner mais il consume quand meme mes CP ».
     */
    public static boolean active(LocalPlayer player) {
        if (player == null) return false;
        return player.tickCount < endTick || (holding && player.tickCount < holdEnd);
    }

    /**
     * L'eclat s'arrete : le serveur ne le paie plus.
     *
     * <p>Appele par {@code MineDetectStopPacket}, et c'est la seule facon de l'eteindre — le
     * client ne decide pas, puisqu'il ne sait pas ce qui reste dans la reserve.
     */
    public static void stop() {
        holding = false;
        endTick = -1;
        holdEnd = -1;
        ores = List.of();
        stopSound();
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
            if (sound != null) stopSound();
            return;
        }
        // Le son tient l'eclat : l'ogg dure 6,8 secondes, l'eclat peut en durer trente.
        if (sound != null && --soundTicks <= 0) playSound(player);
        // En pause, la sphere n'a pas change : on ne la balaie pas pour rien. Voir ClientPause.
        if (ClientPause.frozen()) return;
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
                            MineDetectVisuals.tierOf(advanced, MetalTargets.harvestTier(state)),
                            MineDetectFilter.isSelected(state)));
                    if (found.size() >= MineDetectVisuals.SCAN_LIMIT) return found;
                }
            }
        }
        return found;
    }
}
