package cn.academy.misc.media.client;

import cn.academy.AcademyCraft;
import cn.academy.misc.media.Media;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Le lecteur du client : un morceau a la fois, avec sa pause et son volume.
 *
 * <p>Portage de {@code MediaBackend}, et <b>par le meme chemin que lui</b>. L'original ne
 * passait pas par l'interface de sons du jeu : il prenait son {@code SoundSystem} (la
 * bibliotheque paulscode) par reflexion, ouvrait sa propre source en flux, et commandait
 * cette source — {@code setVolume} pour le volume, {@code pause} / {@code play} pour la pause
 * et la reprise. La source OpenAL garde sa position quand on la met en pause, donc la reprise
 * repart exactement ou l'on s'etait arrete ; le temps affiche venait de
 * {@code millisecondsPlayed}.
 *
 * <h2>La meme chose en 1.20.1</h2>
 *
 * <p>La 1.13 a remplace paulscode par sa propre bibliotheque, {@code com.mojang.blaze3d.audio}.
 * Mais le principe tient toujours : le moteur garde un <b>canal</b> par son, et ce canal sait
 * se mettre en pause, repartir et changer de volume ({@code Channel.pause}, {@code unpause},
 * {@code setVolume}). Le lecteur va donc le chercher — {@code SoundEngine.instanceToChannel},
 * par reflexion, exactement comme l'original allait chercher son {@code sndSystem} — et lui
 * parle directement.
 *
 * <p>C'est ce qui rend les deux comportements corrects : une pause qui <b>reprend a la bonne
 * position</b> sans rien rejouer, et une glissiere de volume qui agit <b>tout de suite</b> (un
 * son ordinaire n'est regle qu'une fois, au lancement, par l'interface publique du moteur).
 *
 * <p>Le morceau se joue sans position ni attenuation : il s'entend partout, ce qui est le
 * propre d'une musique. Comme l'original, il <b>coupe la musique du jeu</b> tant qu'il joue.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMediaPlayer {

    /** Le champ du moteur de son, et celui de ses canaux : les deux prises de l'original. */
    private static final Field SOUND_ENGINE = field(SoundManager.class, "soundEngine");
    private static final Field CHANNELS = field(SoundEngine.class, "instanceToChannel");

    private static MediaSound current;
    private static Media playing;

    /** Le canal du moteur pour ce morceau : c'est lui qui porte la pause et le volume. */
    @Nullable
    private static ChannelAccess.ChannelHandle channel;

    /** Le volume du lecteur, de zero a un : celui de sa glissiere, pas celui du jeu. */
    private static float volume = 1.0f;

    /** L'instant du lancement, en nanosecondes : c'est l'horloge de la position affichee. */
    private static long startedNano;

    /** La position de la pause, ou -1 quand le morceau tourne. */
    private static float pausedAt = -1f;

    private ClientMediaPlayer() {}

    /**
     * Le son du lecteur.
     *
     * <p>Rien de particulier : c'est un son comme les autres, sur la source MASTER (pour que
     * la glissiere « Musique » du jeu ne le coupe pas). C'est son <b>canal</b> qui est
     * commande a la main.
     */
    private static final class MediaSound extends AbstractSoundInstance {

        MediaSound(ResourceLocation location) {
            super(location, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
        }

        @Override
        public float getVolume() {
            return volume;
        }
    }

    /** Lance un morceau, en arretant celui qui tournait. */
    public static void play(Media media) {
        if (media == null) return;
        stop();
        playing = media;
        start();
    }

    private static void start() {
        startedNano = System.nanoTime();
        pausedAt = -1.0f;
        channel = null;

        current = new MediaSound(playing.sound());

        // La musique du jeu se tait pendant le morceau : c'est ce que faisait l'original a
        // chaque passage de son planificateur.
        Minecraft.getInstance().getMusicManager().stopPlaying();
        Minecraft.getInstance().getSoundManager().play(current);

        // Le canal vient d'etre cree : on y pose le volume du lecteur.
        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) handle.execute(ch -> ch.setVolume(volume));
    }

    /**
     * Le canal du moteur pour le morceau en cours, ou {@code null}.
     *
     * <p>Le moteur tient {@code SoundEngine.instanceToChannel}, une table de son vers son
     * canal ; la cle est le son lui-meme. Tout est prive, d'ou la reflexion — c'est le meme
     * geste que l'original, qui allait chercher le {@code SoundSystem} du jeu.
     */
    @Nullable
    private static ChannelAccess.ChannelHandle channel() {
        if (channel != null) return channel;
        if (current == null || SOUND_ENGINE == null || CHANNELS == null) return null;

        try {
            Object engine = SOUND_ENGINE.get(Minecraft.getInstance().getSoundManager());
            Object table = engine == null ? null : CHANNELS.get(engine);
            if (table instanceof Map<?, ?> channels) {
                Object found = channels.get(current);
                if (found instanceof ChannelAccess.ChannelHandle handle) {
                    channel = handle;
                    // Le moteur a pose le volume du son multiplie par celui de la source ;
                    // le gain de l'ecouteur porte deja ce dernier, donc on repose le notre.
                    handle.execute(ch -> ch.setVolume(volume));
                }
            }
        } catch (IllegalAccessException | RuntimeException e) {
            // Sans le canal, le morceau se joue quand meme : seule la pause perd son effet.
            return null;
        }
        return channel;
    }

    /** Met le morceau en attente, exactement la ou il est. */
    private static void pause() {
        if (playing == null || pausedAt >= 0.0f) return;

        pausedAt = position();
        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) handle.execute(Channel::pause);
    }

    /** Reprend le morceau la ou la pause l'a laisse : le canal n'a pas bouge. */
    private static void resume() {
        if (playing == null || pausedAt < 0.0f) return;

        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) handle.execute(Channel::unpause);

        // L'horloge repart de la position de la pause : la source, elle, n'a jamais avance.
        startedNano = System.nanoTime() - (long) (pausedAt * 1_000_000_000.0f);
        pausedAt = -1.0f;
    }

    /** Bascule entre lecture et attente, sur le morceau en cours. */
    public static void togglePause() {
        if (playing == null) return;
        if (pausedAt >= 0.0f) {
            resume();
        } else {
            pause();
        }
    }

    /**
     * Le morceau en attente le reste, meme quand le moteur remet tout en marche.
     *
     * <p>{@code Minecraft.setScreen(null)} appelle {@code soundManager.resume()}, qui remet en
     * marche <b>tous</b> les canaux du moteur — le notre compris. Fermer un ecran, n'importe lequel,
     * relancait donc la musique mise en pause dans le terminal, alors que le port la croyait
     * toujours en attente : le bouton montrait lecture et l'horloge ne bougeait plus, mais le son,
     * lui, repartait — le joueur : « si je referme le data terminal, la musique se relance d'elle
     * meme ».
     *
     * <p>Le moteur ne demande pas son avis au son : il n'y a donc pas d'autre facon de tenir la
     * pause que de la reposer apres lui. C'est une commande sur le canal, pas un rechargement :
     * elle ne coute rien, et un canal deja en pause ne bouge pas.
     */
    private static void keepPaused() {
        if (playing == null || pausedAt < 0.0f) return;

        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) handle.execute(Channel::pause);
    }

    /**
     * Le battement du lecteur : une tache par tick, et seulement quand un morceau est en attente.
     *
     * <p>C'est un abonne distinct, et pas le lecteur lui-meme, parce que Forge ne veut pas
     * d'abonnement sur une classe qui n'a pas de constructeur public : celui du lecteur est prive,
     * comme il se doit pour une classe qui n'est qu'un etat statique.
     */
    @Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
    public static final class Keeping {

        private Keeping() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            keepPaused();
        }
    }

    /** Vrai quand un morceau est en attente. */
    public static boolean isPaused() {
        return playing != null && pausedAt >= 0.0f;
    }

    /** Arrete le morceau en cours, s'il y en a un. */
    public static void stop() {
        stopSound();
        playing = null;
        pausedAt = -1.0f;
    }

    private static void stopSound() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
        channel = null;
    }

    /** Le volume du lecteur, de zero a un. */
    public static float getVolume() {
        return volume;
    }

    /**
     * Regle le volume du lecteur, et le fait entendre tout de suite.
     *
     * <p>Le canal est commande directement : l'interface publique du moteur ne regle un son
     * qu'au lancement, et {@code updateCategoryVolume(MASTER, …)} ne touche que le gain de
     * l'ecouteur — c'est pour cela que la glissiere ne faisait rien jusqu'ici.
     */
    public static void setVolume(float value) {
        volume = Math.max(0.0f, Math.min(1.0f, value));

        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) handle.execute(ch -> ch.setVolume(volume));
    }

    /**
     * La position du morceau, en secondes.
     *
     * <p>C'est l'horloge du lecteur : elle s'arrete pendant une pause, et reprend la ou elle
     * s'etait arretee. La source, elle, ne bouge pas.
     */
    public static float position() {
        if (playing == null) return 0.0f;
        if (pausedAt >= 0.0f) return pausedAt;
        return (System.nanoTime() - startedNano) / 1_000_000_000.0f;
    }

    /** La position du morceau, en secondes — le {@code playedTime} de l'original. */
    public static float elapsedSeconds() {
        return position();
    }

    /** Bascule : le morceau demande s'il n'est pas en cours, sinon silence. */
    public static void toggle(Media media) {
        if (isPlaying(media)) {
            stop();
        } else {
            play(media);
        }
    }

    public static boolean isPlaying(Media media) {
        return media != null && media.equals(playing);
    }

    /** Le morceau en cours, ou {@code null}. */
    public static Media current() {
        return playing;
    }

    /** Un champ prive par son nom, ou {@code null} si le jeu l'a deplace. */
    @Nullable
    private static Field field(Class<?> owner, String name) {
        try {
            Field found = owner.getDeclaredField(name);
            found.setAccessible(true);
            return found;
        } catch (NoSuchFieldException | RuntimeException e) {
            return null;
        }
    }
}
