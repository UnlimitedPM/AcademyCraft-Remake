package cn.academy.sound;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

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

    /** Un son dans le monde, a une position : tous ceux qui passent l'entendent. */
    public static void playAt(Level level, Vec3 pos, RegistryObject<SoundEvent> event,
                              float volume, float pitch) {
        playAt(level, pos, event, DEFAULT_SOURCE, volume, pitch);
    }

    /** Le meme, avec la categorie de l'original. */
    public static void playAt(Level level, Vec3 pos, RegistryObject<SoundEvent> event,
                              SoundSource source, float volume, float pitch) {
        if (event == null || !event.isPresent()) return;
        level.playSound(null, pos.x, pos.y, pos.z, event.get(), source, volume, pitch);
    }

    /**
     * Un son du mod, donne par son <b>nom</b> plutot que par son objet.
     *
     * <p>C'est ce qu'il faut aux effets dont les nombres se relisent en test : un
     * {@code RegistryObject} n'existe que sur un jeu en marche, et le simple fait de le
     * <b>nommer</b> dans une table de constantes fait tomber un test unitaire sur un
     * « Not bootstrapped ». Les classes d'effet gardent donc le nom du son, et l'evenement se
     * refabrique ici, au moment de le jouer — sa portee de seize blocs est celle par defaut,
     * exactement comme les entrees de {@code sounds.json}.
     */
    public static void playAt(Level level, Vec3 pos, String name, float volume, float pitch) {
        if (level == null || name == null) return;
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, name)),
                DEFAULT_SOURCE, volume, pitch);
    }

    /**
     * Un son pour un seul joueur.
     *
     * <p>C'est le {@code playClient(player, ...)} de l'original, dont tous les appels de
     * competence se servaient : un son de 0,5 de volume que le joueur entend comme s'il
     * venait de lui-meme, sans que ses voisins en sachent rien.
     */
    public static void playFor(Player player, RegistryObject<SoundEvent> event, float volume) {
        playFor(player, event, volume, 1.0f);
    }

    public static void playFor(Player player, RegistryObject<SoundEvent> event, float volume,
                               float pitch) {
        playFor(player, event, DEFAULT_SOURCE, volume, pitch);
    }

    public static void playFor(Player player, RegistryObject<SoundEvent> event,
                               SoundSource source, float volume, float pitch) {
        if (player == null || event == null || !event.isPresent()) return;
        player.playNotifySound(event.get(), source, volume, pitch);
    }
}
