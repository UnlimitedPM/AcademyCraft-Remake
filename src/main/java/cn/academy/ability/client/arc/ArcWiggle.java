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
 * d'unite suffirait a rendre l'arc immobile (par seconde) ou epileptique (par image).
 */
public final class ArcWiggle {

    /** Chance, par tick, de changer de variante. */
    public static final double TEX_WIGGLE = 0.5;

    /** Chance, par tick, de disparaitre. */
    public static final double SHOW_WIGGLE = 0.2;

    /** Chance, par tick, de revenir. */
    public static final double HIDE_WIGGLE = 0.2;

    private boolean visible = true;
    private int variant;

    public ArcWiggle(int variant) {
        this.variant = Math.floorMod(variant, ArcPatterns.variants());
    }

    /**
     * Un tick du client.
     *
     * <p>Le hasard est donne par l'appelant : c'est ce qui permet de relire toute la
     * decision en test, avec un hasard qui repond toujours la meme chose.
     */
    public void advance(Random rng) {
        if (rng.nextDouble() < TEX_WIGGLE) {
            variant = rng.nextInt(ArcPatterns.variants());
        }

        // Un seul des deux, comme dans l'original : un eclair qui disparait ne peut pas
        // revenir dans le meme tick.
        if (visible && rng.nextDouble() < SHOW_WIGGLE) {
            visible = false;
        } else if (!visible && rng.nextDouble() < HIDE_WIGGLE) {
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
