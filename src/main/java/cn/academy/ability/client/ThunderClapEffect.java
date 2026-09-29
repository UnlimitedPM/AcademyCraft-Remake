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
     * La boite d'ou l'electricite part.
     *
     * <p>Plus large que le corps et haute comme lui : un arc qui nait au bord d'une boite en
     * sort de toute sa longueur, donc les bornes restent serrees autour du joueur.
     */
    private static final double SIZE_XZ = 1.5;
    private static final double MIN_Y = -0.2;
    private static final double MAX_Y = 2.0;

    private ThunderClapEffect() {
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
        if (skill != ElectromasterCategory.THUNDER_CLAP) return;
        if (chargeTicks % SurroundArcs.LIFE_TICKS != 0) return;

        // Le centre est au milieu du corps : la boite monte du sol a la tete.
        Vec3 centre = player.position();
        SurroundArcs.spawn(SurroundArcs.BOLD, centre, SIZE_XZ, MIN_Y, MAX_Y, player.getId(),
                player.getRandom());
    }
}
