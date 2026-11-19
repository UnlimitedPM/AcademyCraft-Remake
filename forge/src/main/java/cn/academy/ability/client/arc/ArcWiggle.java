package cn.academy.ability.client.arc;

import java.util.Random;

/**
 * Le scintillement d'un eclair, portage de {@code EntityArc.onUpdate}.
 *
 * <p>Deux choses vacillent, et c'est tout ce qui separe un eclair d'un fil de fer :
 * <ul>
 *   <li>sa <b>forme</b> : a chaque tick, une chance sur deux de passer a une autre des
 *       vingt variantes du motif ;</li>
 *   <li>sa <b>presence</b> : une chance sur cinq de disparaitre, et une chance sur cinq de
 *       revenir. Un eclair qui clignote, donc, plutot qu'un objet pose dans le monde.</li>
 * </ul>
 *
 * <p>Ces trois nombres sont ceux de l'original, et ils sont <b>par tick</b> : les changer
 * d'unite suffirait a rendre l'arc immobile (par seconde) ou epileptique (par image). Ils
 * viennent du motif, parce que l'original les reglait dans la competence : la genese d'arc
 * laissait son eclair visible huit ticks sur dix, la charge en cours la moitie.
 */
public final class ArcWiggle {

    private final double texWiggle;
    private final double showWiggle;
    private final double hideWiggle;

    private boolean visible = true;
    private int variant;

    public ArcWiggle(ArcPattern pattern, int variant) {
        this.texWiggle = pattern.texWiggle();
        this.showWiggle = pattern.showWiggle();
        this.hideWiggle = pattern.hideWiggle();
        this.variant = Math.floorMod(variant, ArcPatterns.variants());
    }

    /**
     * Un tick du client.
     *
     * <p>Le hasard est donne par l'appelant : c'est ce qui permet de relire toute la
     * decision en test, avec un hasard qui repond toujours la meme chose.
     */
    public void advance(Random rng) {
        if (rng.nextDouble() < texWiggle) {
            variant = rng.nextInt(ArcPatterns.variants());
        }

        // Un seul des deux, comme dans l'original : un eclair qui disparait ne peut pas
        // revenir dans le meme tick.
        if (visible && rng.nextDouble() < showWiggle) {
            visible = false;
        } else if (!visible && rng.nextDouble() < hideWiggle) {
            visible = true;
        }
    }

    /** Vrai si l'arc doit etre dessine ce tick-ci. */
    public boolean visible() {
        return visible;
    }

    /** La variante de motif tiree. */
    public int variant() {
        return variant;
    }
}
