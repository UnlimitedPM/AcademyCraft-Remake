package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.client.render.ObjMesh;
import cn.academy.client.render.ObjModels;
import cn.academy.entity.EntityMagHook;
import cn.academy.entity.MagHookVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le crochet magnetique en vol et plante, portage de {@code RendererMagHook}.
 *
 * <p>Un modele OBJ, et deux selon son etat : {@code maghook} ferme tant qu'il vole,
 * {@code maghook_open} — sa pince ouverte — des qu'il mord une paroi. Le crochet qui vient de
 * s'accrocher se lit donc de loin, sans avoir a deviner s'il a pris.
 *
 * <h2>Trois choses du repere de l'original</h2>
 *
 * <ul>
 *   <li><b>Il tourne sur deux axes</b> : le lacet d'abord, un quart de tour en plus pour coucher
 *       son axe sur celui du monde, puis le tangage, autour de l'axe Z — c'est le
 *       {@code glRotated(-yaw + 90, 0, 1, 0)} suivi du {@code glRotated(pitch - 90, 0, 0, 1)} de
 *       l'original, et c'est ce qui couche le crochet a plat contre un plafond ou un sol.</li>
 *   <li><b>L'echelle</b> est celle du fichier, {@link MagHookVisuals#MODEL_SCALE} : le modele est
 *       dessine en unites de l'OBJ, pas en blocs.</li>
 *   <li><b>Et un crochet plante ne glisse pas</b> : l'original repeignait sa position sur la face
 *       au lieu de la laisser s'interpoler, ce qui fait claquer le crochet contre le mur au lieu de
 *       l'y faire deriver pendant une image. Voir {@link EntityMagHook#snapPosition()}.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class MagHookRenderer extends EntityRenderer<EntityMagHook> {

    /** Le crochet ferme, en vol. */
    private static final ResourceLocation FLYING = ObjModels.model("maghook");

    /** Le crochet ouvert, plante. */
    private static final ResourceLocation PLANTED = ObjModels.model("maghook_open");

    /**
     * Sa texture, dans {@code textures/models}, comme celle de la bille de silicium : un rendu
     * d'entite la lit lui-meme, il ne passe pas par l'atlas des blocs.
     */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/models/maghook_model.png");

    public MagHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityMagHook entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        boolean planted = entity.isHit();

        // Le jeu a deja pose la pose sur la position interpolee du crochet. Un crochet plante se
        // recale sur sa face, sans interpolation.
        Vec3 shown = entity.getPosition(partialTick);
        Vec3 at = planted ? entity.snapPosition() : shown;

        pose.pushPose();
        pose.translate(at.x - shown.x, at.y - shown.y, at.z - shown.z);

        // Les deux angles : ceux de la face quand il est plante, ceux de la visee quand il vole —
        // et la visee s'interpole, sinon le crochet avancerait par saccades de vingt images.
        float hookYaw = planted ? MagHookVisuals.yawFor(entity.hitSide()) : yaw;
        float hookPitch = planted ? MagHookVisuals.pitchFor(entity.hitSide())
                : Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        pose.mulPose(Axis.YP.rotationDegrees(-hookYaw + 90f));
        pose.mulPose(Axis.ZP.rotationDegrees(hookPitch - 90f));

        pose.scale(MagHookVisuals.MODEL_SCALE, MagHookVisuals.MODEL_SCALE,
                MagHookVisuals.MODEL_SCALE);
        ObjMesh mesh = ObjModels.get(planted ? PLANTED : FLYING);
        for (String group : MagHookVisuals.GROUPS) {
            ObjModels.draw(mesh, group, TEXTURE, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMagHook entity) {
        return TEXTURE;
    }
}
