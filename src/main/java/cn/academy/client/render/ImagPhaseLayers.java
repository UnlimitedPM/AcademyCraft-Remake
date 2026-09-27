package cn.academy.client.render;

/**
 * Les trois nappes de l'imag phase liquide, calculees sans Minecraft.
 *
 * <p>Portage de {@code RenderImagPhaseLiquid}. L'original ne dessinait pas le fluide : sa
 * texture est un noir uni ({@code academy:blocks/black}, comme l'eau de la 1.12.2 quand on
 * ne veut pas la voir). Ce sont ces nappes qui lui donnent son aspect — trois quads
 * translucides qui nagent a la surface, chacun a sa hauteur, sa vitesse et sa densite, et
 * qui suivent le niveau du liquide.
 *
 * <p>Un bloc de fluide, dans l'original, c'etait exactement ceci : trois quads par bloc, et
 * rien d'autre. C'est peu, mais se tromper de formule se voit tout de suite — d'ou ce
 * calcul a part, relu en JUnit.
 */
public final class ImagPhaseLayers {

    /** Le nombre de nappes disponibles, comme les trois textures de l'original. */
    public static final int COUNT = 3;

    /** En dessous de cette opacite, l'original ne dessinait rien du tout. */
    public static final float MIN_ALPHA = 0.1f;

    private ImagPhaseLayers() {}

    /**
     * L'opacite d'un bloc de fluide vu de {@code distance} blocs.
     *
     * <p>Un liquide qui s'eloigne s'efface : a 45 blocs il ne reste plus rien, ce qui est
     * aussi la distance a laquelle un lac cesse d'interesser quelqu'un.
     */
    public static float alpha(double distance) {
        return (float) (1.0d / (1.0d + 0.2d * distance));
    }

    /**
     * La hauteur de la pile de nappes, tiree de celle du fluide : {@code 1.2 * sqrt(h)}.
     *
     * <p>Elle n'est donc pas celle du liquide : la pile deborde un peu, ce qui fait
     * disparaitre les nappes d'un lac profond dans les blocs de fluide au-dessus.
     */
    public static double stackHeight(double fluidHeight) {
        return 1.2d * Math.sqrt(Math.max(0.0d, fluidHeight));
    }

    /** Une nappe : sa hauteur, ses deux vitesses de defilement, et sa densite. */
    public record Layer(double height, double speedU, double speedV, double density) {

        /** Le decalage de la texture sur cet axe, en tours de texture. */
        public double offset(double speed, double time) {
            return (time * speed) % 1.0d;
        }

        /** Le decalage en u a cet instant. */
        public double offsetU(double time) {
            return offset(speedU, time);
        }

        /** Le decalage en v a cet instant. */
        public double offsetV(double time) {
            return offset(speedV, time);
        }
    }

    /**
     * Les nappes a dessiner, dans l'ordre de l'original.
     *
     * <p>La troisieme n'apparait que sur un fluide assez profond : c'est la seule condition
     * que l'original posait, et elle se lit sur la hauteur de la pile et non sur celle du
     * liquide.
     */
    public static Layer[] layers(double fluidHeight) {
        double ht = stackHeight(fluidHeight);
        Layer basse = new Layer(-0.3d * ht, 0.3d, 0.2d, 0.7d);
        Layer haute = new Layer(0.35d * ht, 0.3d, 0.05d, 0.7d);
        return ht > 0.5d
                ? new Layer[] { basse, haute, new Layer(0.7d * ht, 0.1d, 0.25d, 0.7d) }
                : new Layer[] { basse, haute };
    }
}
