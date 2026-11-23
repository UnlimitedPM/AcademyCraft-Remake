package cn.academy.sound;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Jouer un son du mod, portage de {@code ACSounds}.
 *
 * <p>L'original avait deux gestes. {@code playClient(player, ...)} faisait entendre le son
 * au <b>seul</b> joueur concerne, chez lui : une competence qui s'annonce, un coup qui
 * touche. {@code playClient(world, x, y, z, ...)} posait le son dans le monde, pour tous
 * ceux qui passent par la.
 *
 * <h2>Un seul chemin ici, et il fait les deux</h2>
 *
 * Le port n'a pas de son « purement local » : {@link Player#playNotifySound} fait entendre
 * le son au joueur vise, et c'est tout. Appele sur le client il joue chez lui ; appele sur
 * le serveur il envoie le paquet a ce joueur et a personne d'autre. C'est exactement ce que
 * l'original obtenait en jouant le son cote client, avec un avantage : le port n'a pas
 * besoin d'un paquet a lui pour chaque competence, ni d'un crochet client la ou l'effet
 * vit sur le serveur.
 *
 * <p>La categorie vient de l'appel d'origine — {@code AMBIENT} presque partout,
 * {@code PLAYERS} pour le meltdowner — parce que c'est elle qui decide du curseur de
 * volume que le joueur peut baisser.
 */
public final class AcademySounds {

    /** Le son par defaut des competences, comme les {@code AMBIENT} de l'original. */
    private static final SoundSource DEFAULT_SOURCE = SoundSource.AMBIENT;

    private AcademySounds() {}

    /**
     * Le type du son est un {@link Supplier} et non un {@code RegistryObject} : Forge comme
     * Fabric en fournissent un, et le code commun n'a pas a savoir lequel.
     */
    public static void playAt(Level level, Vec3 pos, Supplier<SoundEvent> event,
                              float volume, float pitch) {
        playAt(level, pos, event, DEFAULT_SOURCE, volume, pitch);
    }

    /** Le meme, avec la categorie de l'original. */
    public static void playAt(Level level, Vec3 pos, Supplier<SoundEvent> event,
                              SoundSource source, float volume, float pitch) {
        SoundEvent sound = event == null ? null : event.get();
        if (sound == null) return;
        level.playSound(null, pos.x, pos.y, pos.z, sound, source, volume, pitch);
    }

    /**
     * Un son pour un seul joueur.
     *
     * <p>C'est le {@code playClient(player, ...)} de l'original, dont tous les appels de
     * competence se servaient : un son de 0,5 de volume que le joueur entend comme s'il
     * venait de lui-meme, sans que ses voisins en sachent rien.
     */
    public static void playFor(Player player, Supplier<SoundEvent> event, float volume) {
        playFor(player, event, volume, 1.0f);
    }

    public static void playFor(Player player, Supplier<SoundEvent> event, float volume,
                               float pitch) {
        playFor(player, event, DEFAULT_SOURCE, volume, pitch);
    }

    public static void playFor(Player player, Supplier<SoundEvent> event,
                               SoundSource source, float volume, float pitch) {
        SoundEvent sound = event == null ? null : event.get();
        if (player == null || sound == null) return;
        player.playNotifySound(sound, source, volume, pitch);
    }

    /**
     * Les memes gestes, par identifiant de son.
     *
     * <p>Les registres de sons sont propres au chargeur ({@code ModSounds} cote Forge) : le code
     * commun nomme donc le son par son identifiant, comme pour les objets, et le registre le
     * retrouve au moment de le jouer.
     */
    public static void playFor(Player player, String soundId, float volume) {
        playFor(player, soundId, volume, 1.0f);
    }

    public static void playFor(Player player, String soundId, float volume, float pitch) {
        playFor(player, soundId, DEFAULT_SOURCE, volume, pitch);
    }

    public static void playFor(Player player, String soundId, SoundSource source,
                               float volume, float pitch) {
        if (player == null || soundId == null) return;
        SoundEvent sound = cn.academy.content.AcademyContent.sound(soundId);
        if (sound == null) return;
        player.playNotifySound(sound, source, volume, pitch);
    }

    public static void playAt(Level level, Vec3 pos, String soundId, float volume, float pitch) {
        if (level == null || soundId == null) return;
        SoundEvent sound = cn.academy.content.AcademyContent.sound(soundId);
        if (sound == null) return;
        level.playSound(null, pos.x, pos.y, pos.z, sound, DEFAULT_SOURCE, volume, pitch);
    }
}
