package cn.academy.ability.client.arc;

import java.util.List;

/**
 * Les reglages d'un motif d'arc, portage de {@code ArcFactory} et {@code ArcPatterns}.
 *
 * <p>L'original ne dessinait pas ses eclairs a partir d'un modele : il les <b>generait</b>,
 * par une sorte de fractale (un L-system, disait son commentaire) — on part d'un segment,
 * on coupe chaque segment en deux a son milieu, on deplace ce milieu au hasard, et on
 * recommence. Chaque passe divise le deplacement par deux, donc le trait se casse de plus
 * en plus fin, et une branche part de temps en temps sur le cote. C'est ce qui donne
 * l'aspect d'un eclair plutot que d'une ligne brisee.
 *
 * <p>Cinq motifs sortent de la, avec exactement les nombres de l'original : l'arc faible de
 * la genese d'arc, l'arc fin et continu, celui de la charge, le gros arc de l'eclair, et
 * celui des cibles de zone. Un motif plus large et plus branche fait un eclair plus
 * « puissant » a l'oeil — c'est toute la difference entre eux.
 *
 * <p>Le scintillement en fait partie, et il est <b>par motif</b> : l'original le reglait dans
 * chaque competence, mais chacune n'emploie qu'un seul motif — la valeur y est donc chez elle,
 * et le paquet n'a pas a la transporter.
 *
 * <p>Le hasard est amorce : l'original tirait ses motifs a chaque lancement, donc ses
 * eclairs changeaient d'une partie a l'autre, et rien de tout cela n'etait testable. Ici,
 * la meme graine redonne le meme eclair, toujours.
 */
public record ArcPattern(String name, double width, double lengthShrink, double alphaShrink,
                         double maxOffset, double branchFactor, double widthShrink,
                         int passes, double length,
                         double texWiggle, double showWiggle, double hideWiggle, long seed) {

    /** L'arc faible de la genese d'arc : peu de branches, trait moyen. */
    public static final ArcPattern WEAK =
            new ArcPattern("weak", 0.1, 0.7, 0.9, 1.1, 0.15, 0.7, 6, 20, 0.7, 0.1, 0.4, 1L);

    /** L'arc fin et continu de la manipulation magnetique : trait mince, portee longue. */
    public static final ArcPattern THIN_CONTINUOUS =
            new ArcPattern("thin", 0.08, 0.7, 0.9, 1.2, 0.2, 0.7, 5, 20, 1.0, 0.1, 0.6, 2L);

    /** L'arc de la charge en cours : plus branche que le faible. */
    public static final ArcPattern CHARGING =
            new ArcPattern("charging", 0.1, 0.7, 0.9, 1.2, 0.3, 0.7, 5, 20, 0.8, 0.2, 0.8, 3L);

    /** Le gros arc de l'eclair : le trait le plus epais des cinq. */
    public static final ArcPattern STRONG =
            new ArcPattern("strong", 0.3, 0.7, 0.9, 1.4, 0.3, 0.7, 5, 20, 0.5, 0.2, 0.2, 4L);

    /** L'arc des cibles de zone, quand l'eclair rebondit sur les voisines. */
    public static final ArcPattern AOE =
            new ArcPattern("aoe", 0.13, 0.7, 0.9, 1.2, 0.28, 0.7, 5, 20, 0.5, 0.2, 0.2, 5L);

    // ------------------------------------------------------------------
    // Les arcs d'entourage (EntitySurroundArc de l'original)
    // ------------------------------------------------------------------
    //
    // Ce sont les arcs qui GRESILLENT AUTOUR d'une chose plutot que de la relier a une autre :
    // autour du joueur qui charge une machine ou qui s'electrise, autour du bloc que la
    // manipulation magnetique tient en l'air. L'original en avait trois gabarits, tries par
    // epaisseur, et son ArcFactory partait des memes valeurs par defaut que les cinq motifs
    // ci-dessus (lengthShrink 0,7, alphaShrink 0,9) pour n'en changer que cinq :
    //
    //   THIN    largeur 0,2  depassement 0,8  branches 0,7   retrecissement 0,9  3 passes
    //   NORMAL  largeur 0,3  depassement 0,8  branches 0,7   retrecissement 0,9  3 passes
    //   BOLD    largeur 0,35 depassement 1,2  branches 0,45  retrecissement 0,9  3 passes
    //
    // Leur longueur, elle, etait tiree entre deux bornes au moment ou le gabarit etait fabrique
    // (1,5 a 2, 3 a 4, 3,5 a 4,5) : ces bornes vivent dans SurroundArcs, avec la portee des arcs.

    /** L'arc d'entourage fin, celui dont le corps s'entoure chez l'original. */
    public static final ArcPattern SURROUND_THIN =
            new ArcPattern("surround_thin", 0.2, 0.7, 0.9, 0.8, 0.7, 0.9, 3, 1.75, 0.5, 0.2, 0.2, 6L);

    /** L'arc d'entourage moyen : meme dessin, trait plus epais. */
    public static final ArcPattern SURROUND_NORMAL =
            new ArcPattern("surround_normal", 0.3, 0.7, 0.9, 0.8, 0.7, 0.9, 3, 3.5, 0.5, 0.2, 0.2, 7L);

    /** L'arc d'entourage gras, le plus branche et le plus large : l'eclair qui claque. */
    public static final ArcPattern SURROUND_BOLD =
            new ArcPattern("surround_bold", 0.35, 0.7, 0.9, 1.2, 0.45, 0.9, 3, 4.0, 0.5, 0.2, 0.2, 8L);

    /**
     * L'arc d'entourage le plus fin, celui du port : la moitie de l'epaisseur du fin.
     *
     * <p>ECART ASSUME. L'original n'en avait que trois, tailles pour un corps. Autour d'une
     * machine d'un bloc, le joueur a trouve le plus fin encore trop gros — c'est alors sa
     * <b>largeur</b> qui depassait, ses arcs une fois raccourcis a la taille du bloc. Voir
     * {@code ChargingEffect}.
     */
    public static final ArcPattern SURROUND_MICRO =
            new ArcPattern("surround_micro", 0.1, 0.7, 0.9, 0.8, 0.7, 0.9, 3, 0.3, 0.5, 0.2, 0.2, 9L);

    /**
     * Le nombre de variantes tirees par motif.
     *
     * <p>L'original en tirait vingt et changeait de variante toutes les quelques dixiemes
     * de seconde : c'est ce qui fait scintiller l'arc, et ce n'est pas un detail — un
     * eclair parfaitement immobile ne ressemble a rien.
     */
    public static final int VARIANTS = 20;

    /** Les motifs, dans l'ordre ou ils sont nommes : les cinq de l'original, puis l'entourage. */
    public static List<ArcPattern> all() {
        return List.of(WEAK, THIN_CONTINUOUS, CHARGING, STRONG, AOE,
                SURROUND_THIN, SURROUND_NORMAL, SURROUND_BOLD, SURROUND_MICRO);
    }

    /** Le motif qui porte ce nom, ou l'arc faible si le nom est inconnu. */
    public static ArcPattern byName(String name) {
        for (ArcPattern pattern : all()) {
            if (pattern.name().equals(name)) return pattern;
        }
        return WEAK;
    }
}
