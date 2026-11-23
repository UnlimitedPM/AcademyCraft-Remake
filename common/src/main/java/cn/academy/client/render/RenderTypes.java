package cn.academy.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * La fabrique de types de rendu que vanilla garde privee.
 *
 * <p>{@code RenderType.create} est {@code protected} — Forge l'ouvre par son propre access
 * transformer, et le widener de {@code common/} ne suffit pas a tous les coups. Ce fichier le
 * demande donc par reflexion, une fois, et sert les deux endroits qui en ont besoin
 * ({@code CGuiShaders} et {@code MdRenderType}).
 *
 * <p>Un echec ne casse rien : l'appelant recoit un type translucent d'entite, qui dessine quand
 * meme. C'est le comportement voulu — un effet moins joli vaut mieux qu'un client qui tombe.
 */
public final class RenderTypes {

    private static java.lang.reflect.Method factory;

    private RenderTypes() {
    }

    /** Construit un type de rendu, ou un type de secours si la fabrique reste fermee. */
    public static RenderType create(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                                    boolean affectsCrumbling, boolean sortOnUpload,
                                    RenderType.CompositeState state) {
        try {
            if (factory == null) {
                factory = RenderType.class.getDeclaredMethod("create",
                        String.class, VertexFormat.class, VertexFormat.Mode.class, int.class,
                        boolean.class, boolean.class, RenderType.CompositeState.class);
                factory.setAccessible(true);
            }
            return (RenderType) factory.invoke(null, name, format, mode, size,
                    affectsCrumbling, sortOnUpload, state);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return RenderType.entityTranslucent(
                    new net.minecraft.resources.ResourceLocation("minecraft", "textures/misc/white.png"));
        }
    }
}
