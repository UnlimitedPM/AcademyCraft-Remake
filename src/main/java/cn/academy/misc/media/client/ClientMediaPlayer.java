package cn.academy.misc.media.client;

import cn.academy.misc.media.Media;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le lecteur du client : un morceau a la fois.
 *
 * <p>Portage de {@code MediaBackend} : lancer un morceau, l'arreter, savoir lequel tourne, et
 * <b>son propre volume</b>. L'original ne passait pas par le moteur de son du jeu — il prenait
 * son `SoundSystem` par reflexion pour ouvrir une source en flux, avec un volume a lui, dans
 * sa configuration. Le port passe par le moteur, mais garde l'essentiel : la source
 * <b>MASTER</b> et un volume propre, pour que la musique du jeu puisse etre coupee sans couper
 * le lecteur — le joueur avait justement la glissiere « Musique » a zero.
 *
 * <p>Le morceau se joue sans position ni attenuation : il s'entend partout, ce qui est le
 * propre d'une musique. Comme l'original, il <b>coupe la musique du jeu</b> tant qu'il joue.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMediaPlayer {

    private static SoundInstance current;
    private static Media playing;

    /** Le volume du lecteur, de zero a un : celui de sa glissiere, pas celui du jeu. */
    private static float volume = 1.0f;

    /** Le tick client du lancement : c'est lui qui donne la position du morceau. */
    private static int startTick;

    private ClientMediaPlayer() {}

    /**
     * Le son du lecteur.
     *
     * <p>Son volume se lit a chaque fois que le moteur en a besoin, donc la glissiere de
     * l'ecran agit sur un morceau deja lance.
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

        current = new MediaSound(media.sound());
        playing = media;
        startTick = Minecraft.getInstance().player == null
                ? 0 : Minecraft.getInstance().player.tickCount;

        // La musique du jeu se tait pendant le morceau : c'est ce que faisait l'original a
        // chaque passage de son planificateur.
        Minecraft.getInstance().getMusicManager().stopPlaying();
        Minecraft.getInstance().getSoundManager().play(current);
    }

    /** Arrete le morceau en cours, s'il y en a un. */
    public static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
            playing = null;
        }
    }

    /** Le volume du lecteur, de zero a un. */
    public static float getVolume() {
        return volume;
    }

    /**
     * Regle le volume du lecteur, et le fait entendre tout de suite.
     *
     * <p>Le moteur ne relit le volume d'un son que lorsqu'une categorie change de volume : on
     * lui redonne donc celui de MASTER, ce qui recalcule tous ses canaux.
     */
    public static void setVolume(float value) {
        volume = Math.max(0.0f, Math.min(1.0f, value));
        if (current == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().updateSourceVolume(SoundSource.MASTER,
                minecraft.options.getSoundSourceVolume(SoundSource.MASTER));
    }

    /**
     * Depuis combien de temps le morceau tourne, en secondes.
     *
     * <p>C'est ce que l'original appelait le {@code playedTime} de son lecteur. Le port le
     * deduit du tick du joueur : vingt ticks par seconde, et rien a tenir a jour.
     */
    public static float elapsedSeconds() {
        var player = Minecraft.getInstance().player;
        if (playing == null || player == null) return 0f;
        return (player.tickCount - startTick) / 20.0f;
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
