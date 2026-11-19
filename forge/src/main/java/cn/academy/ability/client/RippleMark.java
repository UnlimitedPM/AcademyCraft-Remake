package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.md.MdRenderType;
import cn.academy.ability.meltdowner.JetEngineVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * La marque au sol : trois ondes qui se relaient, portage de {@code EntityRippleMark} et de
 * {@code RippleMarkRender}.
 *
 * <h2>Une seule marque pour deux competences</h2>
 *
 * <p>Ce n'est pas un hasard si le reacteur du meltdowner et le claquement d'orage la partagent :
 * c'est <b>la meme entite</b> chez l'original — {@code EntityRippleMark}, dessinee par l'unique
 * {@code RippleMarkRender} —, et les deux competences ne faisaient que poser la leur, l'une en
 * vert au point que son regard touche, l'autre en gris a l'endroit ou la foudre tombera. Sa
 * <b>couleur</b> est donc la seule chose qui les distingue, et elle est donnee par l'appelant.
 *
 * <p>C'est la classe de l'original que le port avait ouverte en premier pour le reacteur, ou elle
 * vivait dans {@code JetEngineRenderer} ; elle est ici depuis que le claquement d'orage en a
 * besoin a son tour. Ses <b>courbes</b> — le cycle de trois virgule six secondes, la taille de
 * l'onde qui se resserre, sa hauteur qui monte et son apparition puis son effacement — sont
 * restees dans {@code JetEngineVisuals}, ou elles ont ete portees et ou un test les fige ; cette
 * classe ne porte que le <b>dessin</b>.
 *
 * <h2>Ce qu'une onde est</h2>
 *
 * <p>Un carre <b>horizontal</b> de la texture {@code effects/ripple} : un anneau dessine a plat,
 * pose au point vise. Trois d'entre eux se relaient a un tiers de cycle, ce qui fait la vague
 * continue. Chacun monte de trente centimetres par seconde, se resserre de 1,9 a 1,4 bloc, et
 * apparait puis s'efface en 1,6 seconde.
 *
 * <p>L'original les dessinait <b>sans test de profondeur</b>, parce qu'elles sont posees au niveau
 * du sol : un carre exactement coplanaire avec la terre s'y dispute la profondeur au micron pres.
 * Le port les leve d'un centimetre a la place, et garde le test — c'est la meme regle que partout
 * ailleurs ici : un effet translucide ne dispute pas la profondeur, dans un sens comme dans
 * l'autre.
 *
 * <p>L'<b>opacite</b> de la couleur est ignoree, et c'est exactement ce que faisait l'original :
 * son rendu ecrasait l'alpha de la marque par celui de la courbe
 * ({@code material.color.setAlpha(Colors.f2i(getAlpha(mod)))}), si bien que le 179 du claquement
 * d'orage — sept dixiemes — ne se voyait nulle part. Seules ses trois composantes de couleur
 * comptent, et la courbe decide du reste.
 */
@OnlyIn(Dist.CLIENT)
public final class RippleMark {

    /** La texture de l'onde : celle de l'original, inchangee. */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/ripple.png");

    /** Le centimetre qui separe une onde du sol. Voir le commentaire de la classe. */
    private static final double GROUND_LIFT = 0.01;

    private RippleMark() {
    }

    /**
     * Dessine la marque : ses trois ondes, au point donne.
     *
     * <p>Le dessin se pose dans le repere de la camera — les sommets se comptent en coordonnees du
     * monde moins la position de la camera — comme celui des arcs et des rayons. Rien n'est
     * accumule ici : celui qui appelle decide de la vie de sa marque, et c'est lui qui la repose
     * tant qu'elle doit vivre.
     *
     * @param at         le point du monde ou l'onde se pose
     * @param ageSeconds l'age de la marque, en secondes
     * @param red        sa couleur, trois composantes entre 0 et 1 — voir la classe
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 at,
                            double ageSeconds, float red, float green, float blue) {
        VertexConsumer out = buffers.getBuffer(MdRenderType.of(TEXTURE));

        for (int i = 0; i < JetEngineVisuals.OFFSETS.length; i++) {
            double phase = JetEngineVisuals.phase(ageSeconds, i);
            float alpha = JetEngineVisuals.alpha(phase);
            if (alpha <= 0f) continue;

            float size = JetEngineVisuals.size(phase);
            double height = JetEngineVisuals.height(phase);

            pose.pushPose();
            // Le repere de l'onde : le point vise, monte de ce qu'elle a gagne.
            pose.translate(at.x - camera.x, at.y - camera.y + height + GROUND_LIFT, at.z - camera.z);
            pose.scale(size, 1f, size);
            ripple(out, pose.last().pose(), alpha, red, green, blue);
            pose.popPose();
        }
    }

    /**
     * Un carre horizontal, de la texture seule et teinte de la couleur demandee.
     *
     * <p>Une seule face, comme son maillage : le type de rendu ne trie pas les faces arriere, une
     * onde se voit donc aussi bien d'en dessous. En dessiner une seconde ne ferait que melanger
     * deux fois les memes pixels, et l'onde paraitrait deux fois plus opaque.
     */
    private static void ripple(VertexConsumer out, Matrix4f matrix, float alpha,
                               float red, float green, float blue) {
        out.vertex(matrix, -0.5f, 0f, -0.5f).color(red, green, blue, alpha).uv(0f, 0f).endVertex();
        out.vertex(matrix, 0.5f, 0f, -0.5f).color(red, green, blue, alpha).uv(0f, 1f).endVertex();
        out.vertex(matrix, 0.5f, 0f, 0.5f).color(red, green, blue, alpha).uv(1f, 1f).endVertex();
        out.vertex(matrix, -0.5f, 0f, 0.5f).color(red, green, blue, alpha).uv(1f, 0f).endVertex();
    }
}
