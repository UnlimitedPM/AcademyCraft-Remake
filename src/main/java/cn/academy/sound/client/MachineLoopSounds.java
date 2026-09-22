package cn.academy.sound.client;

import cn.academy.sound.MachineLoops;
import cn.academy.sound.MachineSounds;
import cn.academy.sound.SoundLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Les boucles des machines en cours, une par machine qui travaille.
 *
 * <p>C'est le cote client de {@link MachineSounds} : le block entity, qui est une classe
 * partagee, ne connait que la facade neutre et ne nomme jamais cette classe-ci. Elle est
 * installee au demarrage du client, et sans elle la facade ne fait rien.
 *
 * <p>Plusieurs machines peuvent tourner en meme temps, donc autant de boucles que de
 * machines — contrairement aux competences tenues, dont une seule est en cours par joueur.
 * Chacune est posee a la position de sa machine, et se coupe quand celle-ci s'arrete, ou
 * quand son block entity disparait (voir {@link MachineLoopSound#tick}).
 */
@OnlyIn(Dist.CLIENT)
public final class MachineLoopSounds implements MachineSounds.Hook {

    private static final Map<BlockPos, MachineLoopSound> RUNNING = new HashMap<>();

    /** Le monde dont les boucles tournent, pour les couper quand on le quitte. */
    private static Level runningLevel;

    private MachineLoopSounds() {}

    /** Installe les boucles des machines cote client. Appele au demarrage du client. */
    public static void install() {
        MachineSounds.install(new MachineLoopSounds());
    }

    @Override
    public void tick(Level level, BlockPos pos, MachineLoops.Loop loop, boolean working) {
        // Un client ne tient qu'un monde a la fois : en changer laisse les boucles de
        // l'ancien derriere lui, et personne ne viendra les eteindre.
        if (level != runningLevel) {
            stopAll();
            runningLevel = level;
        }

        MachineLoopSound sound = RUNNING.get(pos);

        if (!working) {
            if (sound != null) {
                sound.stopLoop();
                RUNNING.remove(pos);
            }
            return;
        }

        // Le cas courant : la machine travaillait deja, sa boucle tourne.
        if (sound != null) return;

        SoundEvent event = SoundLookup.event(loop.event());
        if (event == null) return;

        sound = new MachineLoopSound(level, pos, event, source(loop.source()), loop.volume());
        RUNNING.put(pos, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void stopAll() {
        for (MachineLoopSound sound : RUNNING.values()) sound.stopLoop();
        RUNNING.clear();
    }

    /** La categorie d'un nom neutre, celui de la table des boucles. */
    private static SoundSource source(String name) {
        return MachineLoops.BLOCKS.equals(name) ? SoundSource.BLOCKS : SoundSource.AMBIENT;
    }
}
