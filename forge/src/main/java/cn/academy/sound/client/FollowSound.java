package cn.academy.sound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Un son qui SUIT une entite pendant toute sa duree, au lieu de rester ou il a commence.
 *
 * <p>Demande du joueur du 26/10, sur le detecteur de minerais : « le son du pouvoir se joue
 * uniquement la ou on a activé la compétence, le bruit ne nous suit pas ». C'est exactement ce que
 * fait {@code playNotifySound}, que le port utilisait : il pose un son a une POSITION, et cette
 * position ne bouge plus — le joueur qui avance laisse donc son propre son derriere lui, et un
 * grondement de huit secondes se termine a l'autre bout du tunnel.
 *
 * <p>{@link EntityBoundSoundInstance} fait le contraire : a chaque tick il recopie la position de
 * l'entite (voir son {@code tick}), et il s'arrete de lui-meme si elle disparait. C'est ce que
 * l'original obtenait en attachant son son au joueur, et c'est la seule facon d'avoir un son
 * « pose sur nous » en 1.20.1.
 *
 * <p>Client seulement, comme tout ce qui touche au {@code SoundManager} : un son joue chez celui qui
 * le demande, jamais chez les autres.
 */
@OnlyIn(Dist.CLIENT)
public final class FollowSound {

    private FollowSound() {}

    /**
     * Joue {@code event} sur {@code entity}, et l'y laisse pendant toute la duree du son.
     *
     * @param source la categorie de volume, celle que le joueur peut baisser
     * @return l'instance jouee, pour pouvoir l'arreter — voir {@link #stop}. Nul si rien n'a
     *     ete joue.
     */
    public static SoundInstance play(Entity entity, SoundEvent event, SoundSource source,
                            float volume, float pitch) {
        if (entity == null || event == null) return null;
        EntityBoundSoundInstance instance = new EntityBoundSoundInstance(
                event, source, volume, pitch, entity, entity.level().random.nextLong());
        Minecraft.getInstance().getSoundManager().play(instance);
        return instance;
    }

    /**
     * Eteint un son rendu par {@link #play}, s'il tourne encore.
     *
     * <p>C'est ce qui manquait a l'eclat de la detection de minerais : son grondement dure
     * 6,8 secondes (cent trente-sept ticks), et une fois repose pendant le maintien il fallait
     * pouvoir le faire taire au second appui — sinon il continuait seul jusqu'a son terme, sur
     * un pouvoir deja eteint.
     */
    public static void stop(SoundInstance instance) {
        if (instance == null) return;
        Minecraft.getInstance().getSoundManager().stop(instance);
    }
}
