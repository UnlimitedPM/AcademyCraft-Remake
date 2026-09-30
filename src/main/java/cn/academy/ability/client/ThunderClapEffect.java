package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.client.arc.SurroundArcs;
import cn.academy.ability.electromaster.ElectromasterCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * L'electricite de l'orage qui s'amasse : un essaim d'arcs gras autour du joueur, tant
 * qu'il tient la charge.
 *
 * <p>C'est l'{@code EntitySurroundArc} de l'original, dans sa variante {@code BOLD} — la plus
 * large et la plus branchee des trois, celle des grandes occasions. L'original en posait un
 * a l'appui, le laissait vivre pendant toute la charge et ne le retirait que dix ticks apres
 * la fin. Le port n'a pas d'entite d'arc : il seme des arcs courts qui vivent trois ticks et
 * se renouvellent, ce qui donne le meme gresillement continu — voir {@link SurroundArcs}.
 *
 * <p>Comme les autres effets du genre, tout est cote client et rien ne voyage : chacun voit
 * l'eclair s'amasser autour de celui qui le prepare, sans que le serveur ait a en savoir quoi
 * que ce soit.
 */
public final class ThunderClapEffect {

    /**
     * La boite d'ou l'electricite part, et le dezoom de la vue. Deux etats de classe : c'est
     * un effet du joueur local, et il n'y en a qu'un.
     */
    private static final double SIZE_XZ = 0.5;
    private static final double MIN_Y = -0.5;
    private static final double MAX_Y = 0.7;

    /**
     * Le dezoom a pleine charge, en <b>degres</b> de champ de vision.
     *
     * <p>Un nombre de degres et non une part : c'est ce que le joueur a demande pour l'essayer,
     * et c'est plus lisible — « quarante degres de plus » se verifie a l'oeil, « vingt-cinq pour
     * cent » ne se voit qu'en comparant deux reglages.
     */
    private static final float FOV_DEGREES = 40f;

    /** Où en est la charge montree, de 0 a 1 ; 0 quand rien ne charge. */
    private static float progress;

    private ThunderClapEffect() {
    }

    /** Le dezoom de la charge, en degres a ajouter au champ de vision. */
    public static float fovDegrees() {
        return FOV_DEGREES * progress;
    }

    /** La charge s'arrete : la vue reprend sa place. */
    public static void end() {
        progress = 0f;
    }

    /**
     * Un tick de charge : l'entourage du joueur gresille.
     *
     * <p>Appele a chaque tick de la charge, mais ne seme qu'un tick sur {@link
     * SurroundArcs#LIFE_TICKS} : chaque vague d'arcs vit trois ticks, et la suivante prend la
     * releve. Semer a chaque tick en poserait trois fois trop, et l'eclair deviendrait un
     * brouillard.
     *
     * @param chargeTicks les ticks de charge comptes par le client
     */
    public static void tick(Player player, Skill skill, int chargeTicks) {
        if (skill != ElectromasterCategory.THUNDER_CLAP) {
            // Une autre charge : la vue reprend sa place tout de suite, sans attendre la fin
            // de celle-ci. Les charges qui s'arretent ne rappellent personne.
            progress = 0f;
            return;
        }

        // Le dezoom suit la charge, et s'arrete a son maximum : au-dela, l'orage ne grossit
        // plus, donc la vue non plus.
        int max = Math.max(1, skill.getMaxChargeTicks(ClientAbilityData.get()));
        progress = Math.min(1f, chargeTicks / (float) max);

        if (chargeTicks % SurroundArcs.LIFE_TICKS != 0) return;

        // Le centre est au milieu du corps : la boite monte du bassin a la tete. L'originale
        // le prenait sur la boite de collision de l'entite, multipliee par 1,3 — c'est le meme
        // endroit, et c'est ce qui fait que les arcs tournent AUTOUR du joueur.
        Vec3 centre = player.getBoundingBox().getCenter();
        SurroundArcs.spawn(SurroundArcs.CLAP, centre, SIZE_XZ, MIN_Y, MAX_Y, player.getId(),
                player.getRandom());
    }
}
