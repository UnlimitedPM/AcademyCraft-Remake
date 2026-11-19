package cn.academy.sound.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Une boucle sonore qui suit un joueur, portage de {@code FollowEntitySound}.
 *
 * <p>L'original enveloppait le son dans un objet qui relisait la position de l'entite a
 * chaque tick et se repetait. C'est exactement ce que fait {@link
 * AbstractTickableSoundInstance} : il faut donc en heriter, et non prendre un
 * {@code SimpleSoundInstance} tout fait, qui ne sait pas se repeter.
 *
 * <p>L'attenuation est mise a {@code NONE} : le son vient du joueur et se fait entendre a
 * plein volume, comme le {@code playClient} de l'original qui le jouait chez lui. Son
 * volume ne depend donc pas de l'endroit ou il se trouve — sans quoi il baisserait
 * quand le joueur s'eloigne de lui-meme, ce qui n'a pas de sens.
 */
public class PlayerLoopSound extends AbstractTickableSoundInstance {

    private final Player player;

    public PlayerLoopSound(Player player, SoundEvent event, SoundSource source, float volume) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.player = player;
        // Le son se repete, et se tait au lieu de tomber a zero : sans cela le moteur
        // arreterait de l'avancer et il ne se couperait jamais.
        this.looping = true;
        this.volume = volume;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.NONE;
        follow();
    }

    @Override
    public void tick() {
        if (player == null || player.isRemoved() || player.isDeadOrDying()) {
            stop();
            return;
        }
        follow();
    }

    private void follow() {
        this.x = (float) player.getX();
        this.y = (float) player.getY();
        this.z = (float) player.getZ();
    }

    /** Coupe la boucle. Publique : l'arret du parent ne se voit que de l'interieur. */
    public void stopLoop() {
        stop();
    }
}
