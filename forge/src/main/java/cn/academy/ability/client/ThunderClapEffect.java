package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.arc.SurroundArcs;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.electromaster.ThunderClapSkill;
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
 * <p>La taille, elle, a ete raccourcie une fois : le joueur trouvait ces arcs beaucoup trop
 * grands, et ils l'etaient — mais c'etait l'echelle qui manquait, pas la borne. Une fois la
 * mise a l'echelle de l'original retrouvee, le gras reprend ses vraies bornes, celles de
 * {@link SurroundArcs#BOLD} : cinq arcs de 1,05 a 1,35 bloc. Entre-temps le joueur avait vu
 * des arcs trois fois trop courts, et l'a dit ainsi : « ils sont un peu trop petit, peut-etre
 * que tu n'avais pas bien remis leur vraie taille d'origine depuis la modif ».
 *
 * <p>Comme les autres effets du genre, tout est cote client et rien ne voyage : chacun voit
 * l'eclair s'amasser autour de celui qui le prepare, sans que le serveur ait a en savoir quoi
 * que ce soit.
 *
 * <h2>Et la marque au sol</h2>
 *
 * <p>Le claquement d'orage ne se declenche qu'au relachement, et il frappe un point que le joueur
 * ne voit pas : l'endroit ou son regard touche, jusqu'a quarante blocs. L'original lui donnait
 * donc une <b>marque au sol</b> — la meme {@code EntityRippleMark} que le reacteur du meltdowner,
 * vue par le meme rendu — posee des le debut de la charge au point d'impact, relue a chaque tick,
 * et tuee avec la charge. Sans elle, le joueur chargeait a l'aveugle.
 *
 * <p>Elle n'existait que chez le lanceur ({@code if(isLocal)}), et c'est encore le cas ici :
 * l'entourage d'arcs non plus n'est pas annonce, et il n'y a rien a voir sur quelqu'un qui prepare
 * quelque chose sans savoir ou il le lancera.
 *
 * <p>Sa couleur est celle de l'original, un gris clair — ses 204 sur 255, avec un alpha de 179 que
 * son propre rendu ecrasait par celui de la courbe, voir {@link RippleMark}. Une originalite : son
 * calcul de repli ecrivait {@code hitX = mo.z}, si bien qu'une visee qui ne touchait rien posait
 * la marque a une abscisse egale a sa cote ; le port suit l'intention et prend le meme point que
 * le serveur, {@code TargetingUtil.findImpactPoint}, donc la marque tombe <b>exactement</b> la ou
 * la foudre tombera.
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

    /**
     * La couleur de la marque : un gris clair, les 204 sur 255 de l'original.
     *
     * <p>L'alpha de son 179 n'est pas repris : son rendu l'ecrasait par celui de la courbe de
     * l'onde, donc il ne se voyait nulle part. Voir {@link RippleMark}.
     */
    public static final float MARK_RED = 204 / 255f;
    public static final float MARK_GREEN = 204 / 255f;
    public static final float MARK_BLUE = 204 / 255f;

    /** Où en est la charge montree, de 0 a 1 ; 0 quand rien ne charge. */
    private static float progress;

    /** Le point ou la foudre tombera, relu a chaque tick de charge. Nul, rien a dessiner. */
    private static Vec3 markAt;

    /** L'age de la marque, en ticks client depuis sa naissance. */
    private static int markTicks;

    private ThunderClapEffect() {
    }

    /** Le dezoom de la charge, en degres a ajouter au champ de vision. */
    public static float fovDegrees() {
        return FOV_DEGREES * progress;
    }

    /** Ou les ondes se posent, ou {@code null} s'il n'y a rien a dessiner. */
    public static Vec3 markAt() {
        return markAt;
    }

    /**
     * L'age de la marque, en secondes, ou -1 s'il n'y en a pas.
     *
     * <p>Le tick partiel compte : sans lui, les ondes avanceraient par saccades, et c'est
     * justement leur glissement continu qui fait la vague — c'est le meme calcul que celui du
     * reacteur, voir {@code JetEngineEffect.markAgeSeconds}.
     */
    public static double markAgeSeconds(double partialTick) {
        return markAt == null ? -1 : (markTicks + partialTick) / 20.0;
    }

    /** La charge s'arrete : la vue reprend sa place, et la marque s'en va. */
    public static void end() {
        progress = 0f;
        markAt = null;
        markTicks = 0;
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
            // Une autre charge. Elle ne doit PAS emporter celle de l'orage pour autant : le joueur
            // peut tenir deux charges a la fois, chacune ayant son propre compteur (voir
            // ClientCharge), et cette methode est appelee pour chacune d'elles. La vue et la marque
            // ne se rangent donc que si l'orage lui-meme ne charge plus.
            if (!ClientCharge.isOpen(ElectromasterCategory.THUNDER_CLAP.getName())) end();
            return;
        }

        // Le dezoom suit la charge, et s'arrete a son maximum : au-dela, l'orage ne grossit
        // plus, donc la vue non plus.
        int max = Math.max(1, skill.getMaxChargeTicks(ClientAbilityData.get()));
        progress = Math.min(1f, chargeTicks / (float) max);

        // ET LA MARQUE, avant tout le reste : elle se repose a chaque tick, sans exception, la
        // ou la foudre tombera. Son age, lui, ne repart pas a zero — c'est celui de la marque,
        // pas celui du point, et c'est ce qui fait glisser les trois ondes.
        if (markAt == null) markTicks = 0;
        else markTicks++;
        markAt = TargetingUtil.findImpactPoint(player, ThunderClapSkill.AIM_RANGE);

        if (chargeTicks % SurroundArcs.LIFE_TICKS != 0) return;

        // Le centre est au milieu du corps : la boite monte du bassin a la tete. L'originale
        // le prenait sur la boite de collision de l'entite, multipliee par 1,3 — c'est le meme
        // endroit, et c'est ce qui fait que les arcs tournent AUTOUR du joueur.
        Vec3 centre = player.getBoundingBox().getCenter();
        SurroundArcs.spawn(SurroundArcs.BOLD, centre, SIZE_XZ, MIN_Y, MAX_Y, player.getId(),
                player.getRandom());
    }
}
