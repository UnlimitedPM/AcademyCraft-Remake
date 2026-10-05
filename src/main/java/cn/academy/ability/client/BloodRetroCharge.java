package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.world.entity.player.Player;

/**
 * Le dezoom de la charge du retour de sang.
 *
 * <p>C'est le retour visuel qui manquait a cette competence : la vue s'elargit a mesure que la
 * charge monte, donc le joueur sait quand la touche est pleine sans avoir a regarder ailleurs.
 * Les deux autres competences qui s'en servent — l'orage et le meltdowner — logent ce calcul dans
 * leur propre classe d'effet ; celle-ci est du meme genre et ne connait que la competence tenue et
 * depuis combien de ticks.
 *
 * <p>Cinquante degres a pleine charge, comme le joueur l'a demande.
 */
public final class BloodRetroCharge {

    /**
     * Les degres ajoutes a la vue quand la charge est pleine.
     *
     * <p>C'est un nombre de <b>degres</b>, ajoute au champ de vision du joueur quel que soit son
     * reglage : voir {@code AbilityClientEvents.onComputeFov}.
     */
    public static final float FOV_DEGREES = 50f;

    /** L'avancement de la charge, de zero a un. */
    private static float progress;

    private BloodRetroCharge() {
    }

    /**
     * Avancer d'un tick de maintien.
     *
     * <p>La competence dit elle-meme la duree de sa charge — c'est le temps qu'il faut tenir pour
     * l'envoyer au maximum — et c'est donc l'echelle de ce dezoom. Une charge qui n'est pas la
     * sienne remet le dezoom a zero.
     */
    public static void tick(Player player, Skill skill, int chargeTicks) {
        if (skill != VecmanipCategory.BLOOD_RETROGRADE) {
            progress = 0f;
            return;
        }
        int max = skill.getMaxChargeTicks(ClientAbilityData.get());
        progress = max <= 0 ? 0f : Math.min(1f, chargeTicks / (float) max);
    }

    /** Les degres a ajouter a la vue ; zero quand aucune charge n'est en cours. */
    public static float fovDegrees() {
        return FOV_DEGREES * progress;
    }

    /** La charge s'arrete, le dezoom avec elle. */
    public static void end() {
        progress = 0f;
    }
}
