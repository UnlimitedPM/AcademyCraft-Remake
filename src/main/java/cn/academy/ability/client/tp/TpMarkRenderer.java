package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Dessine les effets de la teleportation : le fantome de la marque, et ses etincelles.
 *
 * <p>C'est le portage de deux choses de l'original : {@code MarkRender}, qui dessinait
 * {@code EntityTPMarking}, et le rendu de {@code TPParticleFactory}, qui dessinait ses
 * etincelles. Les deux se ressemblent — un aplat pale, sans eclairage — mais pas sur un point :
 * le fantome se voit <b>au travers des murs</b>, et les etincelles non.
 *
 * <h2>Le fantome est un joueur</h2>
 *
 * <p>L'original le dessinait avec un {@code ModelBiped} — le modele du joueur — et sa texture
 * animee de sept images. C'est donc un personnage de deux blocs, et non une icone : on voit la
 * silhouette entiere se tenir la ou l'on va atterrir, et c'est ce qui rend la competence lisible
 * de loin.
 *
 * <p>Le modele est rebati ici a la disposition d'UV de la 1.12, et non pris au modele du joueur
 * de la 1.20.1 : la texture de l'original est un atlas de <b>64 sur 32</b> — agrandi quatre fois,
 * mais c'est cet atlas-la que ses boites suivaient — alors que le modele moderne suit celui de
 * 64 sur 64, ou les jambes sont ailleurs. Prendre le modele moderne aurait envoye les jambes du
 * fantome chercher leurs pixels dans le vide.
 *
 * <h2>Sept images</h2>
 *
 * <p>La marque est animee : sept images de {@code effects/tp_mark}, une toutes les deux ticks et
 * demie, donc un tour en dix-sept ticks et demi. C'est l'animation de l'original, au chiffre
 * pres, et c'est elle qui fait « apparaitre » le fantome plutot que de le planter la d'un coup.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class TpMarkRenderer {

    /** Les sept images de la marque, dans l'ordre de l'original. */
    public static final int FRAMES = 7;

    /** Le temps de chacune, en ticks : l'original en passait une toutes les deux ticks et demie. */
    public static final double FRAME_TICKS = 2.5;

    private static final ResourceLocation[] TEXTURES = frames();

    /** Le modele du fantome, construit a la premiere image et garde ensuite. */
    private static HumanoidModel<LivingEntity> model;

    private TpMarkRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (TeleportMark.position() == null && TpParticles.live().isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();

        Vec3 mark = TeleportMark.position();
        if (mark != null) {
            drawMark(buffers, pose, camera, mark);
        }

        if (!TpParticles.live().isEmpty()) {
            Vector3f left = event.getCamera().getLeftVector();
            Vector3f up = event.getCamera().getUpVector();
            double[][] screen = { { left.x, left.y, left.z }, { up.x, up.y, up.z } };

            long now = Util.getMillis();
            VertexConsumer out = buffers.getBuffer(TpRenderType.particle(TpParticles.TEXTURE));
            for (TpParticles.Spark spark : TpParticles.live()) {
                drawSpark(out, pose.last(), camera, screen, spark, now);
            }
        }

        buffers.endBatch();
    }

    /**
     * L'image de la marque a cet age, portage de {@code (ticksExisted / 2.5) % tex.length}.
     *
     * <p>Fonction pure, donc verifiable : sept images, chacune pendant deux ticks et demi, et
     * l'animation reprend au debut apres dix-sept ticks et demi.
     */
    public static int frame(int ageTicks) {
        return (int) (ageTicks / FRAME_TICKS) % FRAMES;
    }

    /** Les sept images, dans l'ordre ou l'original les enchainait. */
    private static ResourceLocation[] frames() {
        ResourceLocation[] frames = new ResourceLocation[FRAMES];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                    "textures/effects/tp_mark/" + i + ".png");
        }
        return frames;
    }

    /** Le fantome : le joueur debout, tourne comme son tireur, sans ecriture de profondeur. */
    private static void drawMark(MultiBufferSource buffers, PoseStack pose, Vec3 camera, Vec3 at) {
        VertexConsumer out = buffers.getBuffer(
                TpRenderType.mark(TEXTURES[frame(TeleportMark.ageTicks())]));

        pose.pushPose();
        pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        // Le fantome regarde ou son tireur regarde : c'est le rendu standard d'une entite, a cent
        // quatre-vingts degres moins le lacet. On le voit donc de dos quand on vise devant soi,
        // comme une silhouette qui montre la direction du saut.
        pose.mulPose(Axis.YP.rotationDegrees(180f - TeleportMark.yaw()));
        model().renderToBuffer(pose, out, 0, 0, 1f, 1f, 1f, 1f);
        pose.popPose();
    }

    /** Une etincelle : un carre qui regarde la camera, comme celles du plasma. */
    private static void drawSpark(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                  double[][] screen, TpParticles.Spark spark, long now) {
        float alpha = spark.alpha(now);
        if (alpha <= 0f) return;

        double[] pos = spark.pos();
        double half = spark.size() / 2.0;
        double[] left = screen[0];
        double[] up = screen[1];

        sparkVertex(out, pose, camera, pos, left, up, half, -half, 0f, 0f, alpha);
        sparkVertex(out, pose, camera, pos, left, up, half, half, 0f, 1f, alpha);
        sparkVertex(out, pose, camera, pos, left, up, -half, half, 1f, 1f, alpha);
        sparkVertex(out, pose, camera, pos, left, up, -half, -half, 1f, 0f, alpha);
    }

    private static void sparkVertex(VertexConsumer out, PoseStack.Pose pose, Vec3 camera,
                                    double[] pos, double[] left, double[] up, double along,
                                    double high, float u, float v, float alpha) {
        out.vertex(pose.pose(),
                        (float) (pos[0] + left[0] * along + up[0] * high - camera.x),
                        (float) (pos[1] + left[1] * along + up[1] * high - camera.y),
                        (float) (pos[2] + left[2] * along + up[2] * high - camera.z))
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }

    /** Le modele du fantome, construit une fois. */
    private static HumanoidModel<LivingEntity> model() {
        if (model == null) {
            model = new HumanoidModel<>(biped());
        }
        return model;
    }

    /**
     * Un joueur debout, aux boites et aux UV de la 1.12.
     *
     * <p>Ce sont les nombres de {@code ModelBiped} : la tete de huit pixels de cote en haut, le
     * corps de huit sur douze, les bras de quatre sur douze poses aux epaules, les jambes de
     * quatre sur douze posees sous le corps. Les sept parties sont celles que l'original rendait
     * une a une, la casquette comprise — c'est son {@code SimpleModelBiped}, et elle est vide ici
     * comme la plupart du temps la-bas.
     */
    private static ModelPart biped() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -8f, -4f, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4f, -8f, -4f, 8, 8, 8, new CubeDeformation(0.5f)),
                PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4f, 0f, -2f, 8, 12, 4), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3f, -2f, -2f, 4, 12, 4), PartPose.offset(-5f, 2f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-1f, -2f, -2f, 4, 12, 4), PartPose.offset(5f, 2f, 0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2f, 0f, -2f, 4, 12, 4), PartPose.offset(-1.9f, 12f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2f, 0f, -2f, 4, 12, 4), PartPose.offset(1.9f, 12f, 0f));

        // Soixante-quatre sur trente-deux : l'atlas de la 1.12, et non celui du joueur moderne.
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
