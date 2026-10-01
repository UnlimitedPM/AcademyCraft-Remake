package cn.academy.ability.client.md;

import cn.academy.AcademyCraft;
import cn.academy.ability.meltdowner.MdBallVisuals;
import cn.academy.entity.EntityMdBall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * Le rendu de la bille de plasma, portage du {@code RenderIcon} d'{@code EntityMdBall}.
 *
 * <p>Deux carres l'un sur l'autre, tous les deux tournes vers le joueur : la <b>lueur</b>, un
 * halo pale de 0,7 bloc, et le <b>coeur</b>, un disque plein de 0,5 pris dans cinq images
 * tournantes. C'est tout ce qu'est une bille de plasma — deux images superposees qui
 * clignotent.
 *
 * <p>Ce qui la fait vivre est dans les nombres : son opacite monte, se tient, puis gonfle au
 * moment du tir (voir {@link MdBallVisuals#alpha}), son
 * scintillement bat d'une image a l'autre, et l'image du coeur change deux fois sur huit. Le
 * tout se lit par <b>image</b> et non par tick : l'original avançait son scintillement dans son
 * propre rendu, et c'est ce qui lui donne sa vibration electrique.
 */
public class MdBallRenderer extends EntityRenderer<EntityMdBall> {

    /** Le halo, et les cinq images du coeur. */
    private static final ResourceLocation GLOW = texture("glow");
    private static final ResourceLocation[] CORES = {
            texture("0"), texture("1"), texture("2"), texture("3"), texture("4"),
    };

    public MdBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        // Une bille de plasma n'a pas d'ombre : elle ne touche pas le sol.
        this.shadowRadius = 0f;
    }

    @Override
    public void render(EntityMdBall entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        // Le scintillement avance a chaque image, comme l'original : c'est ce qui le rend
        // independant de la cadence du serveur.
        entity.advanceRender(entity.level().getRandom());

        double age = entity.ageSeconds();
        float alpha = MdBallVisuals.alpha(age, entity.lifeTicks());
        if (alpha <= 0f) return;

        pose.pushPose();
        // La bille se dessine la ou son porteur est, image comprise : voir
        // {@link MdBallVisuals#snapOffset}, qui rattrape l'interpolation du reseau.
        snapToSpawner(entity, partialTick, pose);
        // Puis a hauteur d'yeux, quelle que soit sa position logique : c'est le decalage de
        // l'original, et c'est ce qui la fait coincider avec le depart de son rayon.
        pose.translate(0.0, MdBallVisuals.RENDER_HEIGHT, 0.0);
        // Le balancement : la bille vibre autour de sa place au lieu d'y etre posee.
        pose.translate(MdBallVisuals.wobbleX(age), MdBallVisuals.wobbleY(age),
                MdBallVisuals.wobbleZ(age));

        // Les deux carres sont dans le plan de l'ecran : la gauche et le haut de la camera
        // suffisent, et la bille se presente donc toujours de face.
        Camera camera = this.entityRenderDispatcher.camera;
        Vector3f leftVector = camera.getLeftVector();
        Vector3f upVector = camera.getUpVector();
        double[] left = { leftVector.x, leftVector.y, leftVector.z };
        double[] up = { upVector.x, upVector.y, upVector.z };

        float wiggle = (float) entity.wiggle();
        float size = MdBallVisuals.size();

        MultiBufferSource.BufferSource batch =
                net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();

        // Le halo d'abord, le coeur par-dessus : c'est l'ordre de l'original.
        VertexConsumer glow = batch.getBuffer(MdRenderType.of(GLOW));
        quad(glow, pose.last(), left, up, MdBallVisuals.GLOW_SIZE * size / 2,
                alpha * (0.3f + 0.7f * wiggle));

        VertexConsumer core = batch.getBuffer(MdRenderType.of(CORES[entity.texture()]));
        quad(core, pose.last(), left, up, MdBallVisuals.CORE_SIZE * size / 2,
                alpha * (0.8f + 0.2f * wiggle));

        batch.endBatch();
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /**
     * Repose la pose sur la place exacte de la bille : porteur, image comprise, plus son ecart.
     *
     * <p>Sans cela, la bille se dessine a la position que le client a interpolee — trois ticks
     * de retard sur son porteur — et elle tremble donc des que le joueur marche.
     */
    private static void snapToSpawner(EntityMdBall ball, float partialTick, PoseStack pose) {
        net.minecraft.world.entity.player.Player spawner = ball.spawner();
        if (spawner == null) return;

        net.minecraft.world.phys.Vec3 offset = MdBallVisuals.snapOffset(
                spawner.getPosition(partialTick), ball.sub(), ball.getPosition(partialTick));
        pose.translate(offset.x, offset.y, offset.z);
    }

    /** Un carre face a la camera, centre sur l'origine de la pose. */
    private static void quad(VertexConsumer out, PoseStack.Pose pose, double[] left, double[] up,
                             double half, float alpha) {
        if (alpha <= 0f) return;
        corner(out, pose, left, up, half, -half, 0f, 0f, alpha);
        corner(out, pose, left, up, half, half, 0f, 1f, alpha);
        corner(out, pose, left, up, -half, half, 1f, 1f, alpha);
        corner(out, pose, left, up, -half, -half, 1f, 0f, alpha);
    }

    private static void corner(VertexConsumer out, PoseStack.Pose pose, double[] left, double[] up,
                               double along, double high, float u, float v, float alpha) {
        out.vertex(pose.pose(),
                        (float) (left[0] * along + up[0] * high),
                        (float) (left[1] * along + up[1] * high),
                        (float) (left[2] * along + up[2] * high))
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMdBall entity) {
        return CORES[0];
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/mdball/" + name + ".png");
    }
}
