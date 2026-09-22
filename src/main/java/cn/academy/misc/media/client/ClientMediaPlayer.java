package cn.academy.misc.media.client;

import cn.academy.misc.media.Media;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le lecteur du client : un morceau a la fois.
 *
 * <p>Portage de ce que {@code MediaBackend} faisait de visible : lancer un morceau, l'arreter,
 * savoir lequel tourne. L'original allait plus loin — il decodait l'OGG pour en connaitre la
 * duree, dessinait une barre de progression et laissait la lecture continuer quand on fermait
 * l'ecran, ce que le port garde ; ce qu'il ne garde pas, c'est la barre, faute de decodeur.
 *
 * <p>Le morceau se joue en source <b>musicale</b> : la glissiere de musique du jeu le coupe,
 * comme elle coupe la musique du jeu lui-meme. Sans position ni attenuation, il s'entend
 * partout, ce qui est le propre d'une musique.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMediaPlayer {

    private static SoundInstance current;
    private static Media playing;

    private ClientMediaPlayer() {}

    /** Lance un morceau, en arretant celui qui tournait. */
    public static void play(Media media) {
        if (media == null) return;
        stop();

        current = new SimpleSoundInstance(
                media.sound(), SoundSource.MUSIC, 1.0f, 1.0f,
                SoundInstance.createUnseededRandom(),
                false, 0, SoundInstance.Attenuation.NONE,
                0.0d, 0.0d, 0.0d, true);
        playing = media;
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
