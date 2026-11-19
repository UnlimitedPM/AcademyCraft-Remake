package cn.academy.ability.vecmanip;

import cn.academy.sound.AcademySounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Le son des deux veilles de vecmanip, avec un garde-fou : <b>une seule emission par joueur et
 * par instant</b>.
 *
 * <p>Le joueur, 22/10 : « a la mort du zombie par exemple, j'ai l'impression que le son se joue
 * en double, ce qui fait que le son ressort assez fort a l'oreille ». Il avait raison, et les deux
 * emissions partent du <b>meme tick</b> : le coup encaisse joue le son de la veille (nouveau, voir
 * {@link VecReflectionSkill#onDamaged}) et, comme la bete est a portee de la veille, le tick du
 * maintien la fige ou la renvoie et joue le sien. Deux sons identiques superposes, donc deux fois
 * plus fort.
 *
 * <p>Le garde-fou vaut pour les deux veilles, et pour le meme motif : une veille qui arrete ou
 * renvoie plusieurs entites dans un meme tick emettait un claquement par entite, ce qui s'entend
 * comme un seul coup joue trop fort. Une fenetre de quelques ticks suffit — les coups d'un zombie
 * sont espaces de vingt ticks, et une entite deja renvoyee porte sa marque.
 */
public final class VecmanipSounds {

    /**
     * Fenetre pendant laquelle une seconde emission est avalee.
     *
     * <p>Trois ticks (150 ms) : assez pour couvrir deux evenements du meme instant et le tick
     * suivant, trop court pour avaler deux coups distincts.
     */
    public static final int GUARD_TICKS = 3;

    /** Le dernier tick d'emission, par joueur. */
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private VecmanipSounds() {
    }

    /**
     * Joue le son d'une veille une seule fois par joueur et par instant.
     *
     * @param at ou poser le son — l'entite renvoyee ou arretee, ou le porteur pour un coup
     */
    public static void play(ServerLevel level, Player player, RegistryObject<SoundEvent> event, Vec3 at) {
        long now = level.getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null) {
            long age = now - last;
            // Un age negatif signifie un autre monde (monde recharge, heure remise a zero) :
            // on ne l'interprete pas comme « il y a longtemps », sinon le garde-fou se tairait.
            if (age >= 0 && age < GUARD_TICKS) return;
        }
        prune(now);
        LAST.put(player.getUUID(), now);
        AcademySounds.playAt(level, at, event, 0.5f, 1.0f);
    }

    /** Un joueur par monde, la table reste petite : on oublie les entrees d'il y a plus de dix secondes. */
    private static void prune(long now) {
        if (LAST.size() < 64) return;
        LAST.values().removeIf(tick -> now - tick > 200);
    }
}
