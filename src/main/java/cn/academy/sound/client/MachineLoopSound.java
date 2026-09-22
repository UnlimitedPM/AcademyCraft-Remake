package cn.academy.sound.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Une boucle sonore posee sur une machine, portage de {@code TileEntitySound}.
 *
 * <p>L'original enveloppait le son dans un objet qui relisait la position du block entity a
 * chaque tick et s'arretait quand il devenait invalide. La position, ici, est posee une
 * fois pour toutes : un block entity ne bouge pas, il n'y a donc rien a relire. Reste
 * l'arret, et c'est justement le seul evenement que personne n'annonce — quand le bloc est
 * casse ou le morceau de monde decharge, il n'y a plus personne pour dire que la machine
 * s'est tue.
 *
 * <p>Le son est <b>positionnel</b> : il vient du bloc, pas du joueur. Il baisse donc avec
 * la distance, contrairement aux boucles des competences tenues, qui suivent leur porteur
 * et se jouent a plein volume.
 */
@OnlyIn(Dist.CLIENT)
public class MachineLoopSound extends AbstractTickableSoundInstance {

    private final Level level;
    private final BlockPos pos;

    public MachineLoopSound(Level level, BlockPos pos, SoundEvent event, SoundSource source,
                            float volume) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.level = level;
        this.pos = pos;
        // Le son se repete, et se tait au lieu de tomber a zero : sans cela le moteur
        // arreterait de l'avancer et il ne se couperait jamais.
        this.looping = true;
        this.volume = volume;
        this.pitch = 1.0f;
        // L'attenuation par defaut, celle du `MovingSound` de l'original : lineaire, donc
        // une machine s'entend de loin et pas de l'autre bout du monde.
        this.attenuation = Attenuation.LINEAR;
        // Le centre du bloc, comme `TileEntitySound` qui prenait la position du block
        // entity plus un demi.
        this.x = pos.getX() + 0.5f;
        this.y = pos.getY() + 0.5f;
        this.z = pos.getZ() + 0.5f;
    }

    @Override
    public void tick() {
        // Le block entity s'annonce a chaque tick : s'il a disparu, c'est que le bloc est
        // casse ou que le morceau de monde est parti, et plus personne ne viendra couper
        // la boucle.
        if (level.getBlockEntity(pos) == null) stop();
    }

    /** Coupe la boucle. Publique : l'arret du parent ne se voit que de l'interieur. */
    public void stopLoop() {
        stop();
    }
}
