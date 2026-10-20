package cn.academy.misc.media.client;

import cn.academy.misc.media.Media;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le lecteur du client : un morceau a la fois, avec sa pause et son volume.
 *
 * <p>Portage de {@code MediaBackend} : lancer un morceau, le mettre en attente, le reprendre
 * ou il en etait, l'arreter, et <b>son propre volume</b>. L'original ne passait pas par le
 * moteur de son du jeu — il prenait son `SoundSystem` par reflexion pour ouvrir une source en
 * flux, avec un volume a lui. Le port passe par le moteur, mais garde l'essentiel : la source
 * <b>MASTER</b> et un volume propre, pour que la musique du jeu puisse etre coupee sans couper
 * le lecteur.
 *
 * <h2>Le volume et la pause, par le meme chemin</h2>
 *
 * Un son ordinaire n'est regle qu'une fois, au lancement : le moteur ne relit plus son volume
 * ensuite. Une {@link TickableSoundInstance}, si : a chaque tick, {@code SoundEngine} refait
 * {@code setVolume(getVolume())} sur le canal (voir {@code tickNonPaused}). C'est donc une
 * instance a tick que joue le lecteur, et c'est ce qui rend la glissiere de volume vivante.
 *
 * <p>La <b>pause</b> est le vrai sujet : le moteur ne sait pas reprendre un flux a une
 * position. Le lecteur arrete donc le son et retient sa position, puis le <b>rejoue depuis le
 * debut, muet</b>, en rendant le volume des que l'horloge atteint cette position — c'est le
 * seul moyen de reprendre exactement ou l'on s'etait arrete, et le rattrapage est silencieux.
 *
 * <p>Le morceau se joue sans position ni attenuation : il s'entend partout, ce qui est le
 * propre d'une musique. Comme l'original, il <b>coupe la musique du jeu</b> tant qu'il joue.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMediaPlayer {

    private static MediaSound current;
    private static Media playing;

    /** Le volume du lecteur, de zero a un : celui de sa glissiere, pas celui du jeu. */
    private static float volume = 1.0f;

    /** L'instant du lancement, en nanosecondes : c'est l'horloge du flux. */
    private static long startedNano;

    /**
     * La position de depart du flux, en secondes.
     *
     * <p>Elle vaut zero pour un morceau lance, et la position de la pause pour un morceau
     * repris : le flux repart toujours du debut, donc c'est lui qui doit rester muet jusqu'a
     * cette position, tandis que la position affichee part de la.
     */
    private static float resumedFrom;

    /** La position de la pause, ou -1 quand le morceau tourne. */
    private static float pausedAt = -1f;

    private ClientMediaPlayer() {}

    /**
     * Le son du lecteur.
     *
     * <p>Il est a tick, donc le moteur relit son volume a chaque tick : c'est ce qui fait
     * vivre la glissiere, et c'est aussi ce qui rend le son audible pile a la bonne position
     * quand un morceau reprend apres une pause.
     */
    private static final class MediaSound extends AbstractSoundInstance
            implements TickableSoundInstance {

        MediaSound(ResourceLocation location) {
            super(location, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
        }

        @Override
        public float getVolume() {
            // Muet pendant le rattrapage d'une reprise, audible ensuite.
            return soundElapsed() >= resumedFrom ? volume : 0.0f;
        }

        /**
         * Un son muet peut quand meme demarrer.
         *
         * <p>Sans cela, le moteur refuse de le lancer (« volume was zero ») et la reprise
         * apres une pause n'aurait jamais de rattrapage a faire.
         */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            // La position et le volume se lisent a la volee : rien a faire ici.
        }

        @Override
        public boolean isStopped() {
            return false;
        }
    }

    /** Lance un morceau, en arretant celui qui tournait. */
    public static void play(Media media) {
        if (media == null) return;
        stop();
        playing = media;
        start(0.0f);
    }

    /**
     * Le meme morceau, a partir d'une position.
     *
     * <p>Le flux repart du debut et reste muet jusqu'a cette position : c'est le prix du
     * moteur du jeu, qui ne sait pas se placer dans un son.
     */
    private static void start(float from) {
        resumedFrom = Math.max(0.0f, from);
        startedNano = System.nanoTime();
        pausedAt = -1.0f;

        current = new MediaSound(playing.sound());

        // La musique du jeu se tait pendant le morceau : c'est ce que faisait l'original a
        // chaque passage de son planificateur.
        Minecraft.getInstance().getMusicManager().stopPlaying();
        Minecraft.getInstance().getSoundManager().play(current);
    }

    /** Met le morceau en attente, a la position ou il est. */
    private static void pause() {
        if (playing == null || pausedAt >= 0.0f) return;

        pausedAt = position();
        stopSound();
    }

    /** Reprend le morceau la ou la pause l'a laisse. */
    private static void resume() {
        if (playing == null || pausedAt < 0.0f) return;
        start(pausedAt);
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

    /** Vrai quand un morceau est en attente. */
    public static boolean isPaused() {
        return playing != null && pausedAt >= 0.0f;
    }

    /** Arrete le morceau en cours, s'il y en a un. */
    public static void stop() {
        stopSound();
        playing = null;
        pausedAt = -1.0f;
        resumedFrom = 0.0f;
    }

    private static void stopSound() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    /** Le volume du lecteur, de zero a un. */
    public static float getVolume() {
        return volume;
    }

    /**
     * Regle le volume du lecteur.
     *
     * <p>Rien a signaler au moteur : le son est a tick, donc il relit ce volume tout seul au
     * tick suivant.
     */
    public static void setVolume(float value) {
        volume = Math.max(0.0f, Math.min(1.0f, value));
    }

    /**
     * La position du morceau, en secondes.
     *
     * <p>C'est l'horloge du lecteur, pas celle du morceau : elle s'arrete pendant une pause,
     * et elle reprend la ou elle s'etait arretee.
     */
    public static float position() {
        if (playing == null) return 0.0f;
        if (pausedAt >= 0.0f) return pausedAt;
        return resumedFrom + soundElapsed();
    }

    /** Depuis combien de temps le flux du moteur joue, en secondes — zero apres une reprise. */
    private static float soundElapsed() {
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
}
