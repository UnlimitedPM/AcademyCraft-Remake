package cn.academy.sound.client;

import cn.academy.sound.HeldLoops;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * La boucle sonore du maintien en cours, chez le joueur.
 *
 * <p>Une seule a la fois, comme dans l'original ou chaque contexte tenait la sienne :
 * le port retient la competence en cours ({@code ClientCharge}) et lance ou coupe la
 * boucle quand elle change. Rien a eteindre a la main, donc : ni a la fin du maintien,
 * ni au relachement, ni a la mort.
 *
 * <p>La table des boucles vit dans {@link HeldLoops}, qui ne connait pas Minecraft et se
 * relit en JUnit ; ici on ne fait que traduire un nom en son, et le faire tourner.
 */
@OnlyIn(Dist.CLIENT)
public final class LoopSounds {

    private static PlayerLoopSound running;
    private static String runningSkill;

    private LoopSounds() {}

    /**
     * Appele a chaque tick client, apres les touches.
     *
     * Le cas courant est « rien n'a change » : on ne fait alors rien du tout, et la boucle
     * continue de suivre son joueur toute seule.
     */
    public static void tick(LocalPlayer player, String skill) {
        if (runningSkill != null && runningSkill.equals(skill)) return;

        stop();
        if (player == null || skill == null) return;

        HeldLoops.Loop loop = HeldLoops.forSkill(skill);
        if (loop == null) return;

        SoundEvent event = event(loop.event());
        if (event == null) return;

        // Le son de mise en route, s'il y en a un, part juste avant la boucle.
        if (loop.hasStartup()) {
            SoundEvent startup = event(loop.startup());
            if (startup != null) {
                player.playNotifySound(startup, net.minecraft.sounds.SoundSource.AMBIENT, 0.5f,
                        1.0f);
            }
        }

        running = new PlayerLoopSound(player, event, net.minecraft.sounds.SoundSource.AMBIENT,
                loop.volume());
        runningSkill = skill;
        Minecraft.getInstance().getSoundManager().play(running);
    }

    /** Coupe la boucle en cours, s'il y en a une. */
    public static void stop() {
        if (running != null) {
            running.stopLoop();
            running = null;
        }
        runningSkill = null;
    }

    /** L'evenement porte par un nom, ou {@code null} s'il n'existe pas. */
    private static SoundEvent event(String name) {
        ResourceLocation key = ResourceLocation.tryParse(
                name.contains(":") ? name : "academy:" + name);
        if (key == null) return null;
        return BuiltInRegistries.SOUND_EVENT.get(key);
    }
}
