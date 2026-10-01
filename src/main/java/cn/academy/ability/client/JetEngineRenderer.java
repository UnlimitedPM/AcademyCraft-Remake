package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.client.md.MdRenderType;
import cn.academy.ability.meltdowner.JetEngineVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Dessine les deux effets du reacteur, portage de {@code RippleMarkRender} et de
 * {@code RenderDiamondShield}.
 *
 * <h2>Les ondes de la marque</h2>
 *
 * <p>Trois carres <b>horizontaux</b> de la texture {@code effects/ripple}, poses au point vise et
 * decales d'un tiers de cycle. Chacun monte de trente centimetres par seconde, se resserre de 1,9 a
 * 1,4 bloc, et apparait puis s'efface en 1,6 seconde. Les trois ensemble font une vague continue —
 * c'est tout ce que le joueur voit de sa competence avant de partir.
 *
 * <p>L'original les dessinait <b>sans test de profondeur</b>, parce qu'elles sont posees au niveau
 * du sol : un carre exactement coplanaire avec la terre s'y dispute la profondeur au micron pres.
 * Le port les leve d'un centimetre a la place, et garde le test — c'est la meme regle que partout
 * ailleurs ici : un effet translucide ne dispute pas la profondeur, dans un sens comme dans
 * l'autre.
 *
 * <h2>Le bouclier de diamant</h2>
 *
 * <p>Une pyramide a quatre faces, posee un bloc devant les yeux du porteur et 1,1 bloc au-dessus
 * de ses pieds, tournee comme son regard, et mise a l'echelle 1,5. Elle vit pendant tout le vol,
 * et seulement lui.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class JetEngineRenderer {

    private static final ResourceLocation RIPPLE = texture("ripple");
    private static final ResourceLocation DIAMOND = texture("diamond_shield");

    /** Le centimetre qui separe une onde du sol. Voir le commentaire de la classe. */
    private static final double GROUND_LIFT = 0.01;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();

        drawMark(event, pose, buffers, camera);
        drawDiamond(event, pose, buffers, camera);

        buffers.endBatch();
    }

    /** Les trois ondes de la marque, au point vise. */
    private static void drawMark(RenderLevelStageEvent event, PoseStack pose,
                                 MultiBufferSource buffers, Vec3 camera) {
        Vec3 at = JetEngineEffect.markAt();
        if (at == null) return;

        double age = JetEngineEffect.markAgeSeconds(event.getPartialTick());
        if (age < 0) return;

        VertexConsumer consumer = buffers.getBuffer(MdRenderType.of(RIPPLE));

        for (int i = 0; i < JetEngineVisuals.OFFSETS.length; i++) {
            double phase = JetEngineVisuals.phase(age, i);
            float alpha = JetEngineVisuals.alpha(phase);
            if (alpha <= 0f) continue;

            float size = JetEngineVisuals.size(phase);
            double height = JetEngineVisuals.height(phase);

            pose.pushPose();
            // Le repere de l'onde : le point vise, monte de ce qu'elle a gagne.
            pose.translate(at.x - camera.x, at.y - camera.y + height + GROUND_LIFT, at.z - camera.z);
            pose.scale(size, 1f, size);
            ripple(consumer, pose.last().pose(), alpha);
            pose.popPose();
        }
    }

    /**
     * Un carre horizontal, de la texture seule et teinte du vert de l'original.
     *
     * <p>Une seule face, comme son maillage : le type de rendu ne trie pas les faces arriere, une
     * onde se voit donc aussi bien d'en dessous. En dessiner une seconde ne ferait que melanger
     * deux fois les memes pixels, et l'onde paraitrait deux fois plus opaque.
     */
    private static void ripple(VertexConsumer out, Matrix4f matrix, float alpha) {
        out.vertex(matrix, -0.5f, 0f, -0.5f)
                .color(JetEngineVisuals.RED, JetEngineVisuals.GREEN, JetEngineVisuals.BLUE, alpha)
                .uv(0f, 0f).endVertex();
        out.vertex(matrix, 0.5f, 0f, -0.5f)
                .color(JetEngineVisuals.RED, JetEngineVisuals.GREEN, JetEngineVisuals.BLUE, alpha)
                .uv(0f, 1f).endVertex();
        out.vertex(matrix, 0.5f, 0f, 0.5f)
                .color(JetEngineVisuals.RED, JetEngineVisuals.GREEN, JetEngineVisuals.BLUE, alpha)
                .uv(1f, 1f).endVertex();
        out.vertex(matrix, -0.5f, 0f, 0.5f)
                .color(JetEngineVisuals.RED, JetEngineVisuals.GREEN, JetEngineVisuals.BLUE, alpha)
                .uv(1f, 0f).endVertex();
    }

    /** La pyramide du bouclier, devant les yeux de son porteur. */
    private static void drawDiamond(RenderLevelStageEvent event, PoseStack pose,
                                    MultiBufferSource buffers, Vec3 camera) {
        if (JetEngineEffect.flightTick() < 0) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        float partialTick = event.getPartialTick();
        Vec3 look = player.getViewVector(partialTick);
        Vec3 at = player.getPosition(partialTick)
                .add(look.scale(JetEngineVisuals.SHIELD_FORWARD))
                .add(0, JetEngineVisuals.SHIELD_HEIGHT, 0);

        double yaw = Math.toDegrees(Math.atan2(-look.x, look.z));
        double pitch = Math.toDegrees(Math.asin(-look.y));

        VertexConsumer consumer = buffers.getBuffer(MdRenderType.of(DIAMOND));

        pose.pushPose();
        pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) -yaw));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) pitch));
        float scale = JetEngineVisuals.SHIELD_SCALE;
        pose.scale(scale, scale, scale);
        pyramid(consumer, pose.last().pose());
        pose.popPose();
    }

    /**
     * La pyramide de l'original, face par face : une base carree de deux blocs de cote dans le
     * plan de l'ecran, et une pointe un bloc devant. Les quatre triangles se partagent la pointe,
     * et chaque coin garde ses coordonnees de texture — c'est ce qui donne a la face son degrade,
     * et pas une teinte plate.
     */
    private static void pyramid(VertexConsumer out, Matrix4f matrix) {
        // Le couple de coins de chaque triangle des `setTriangles` de l'original, dans l'ordre.
        face(out, matrix, -1f, 0f, 0f, 0f, 0f, -1f, 1f, 1f);
        face(out, matrix, 0f, -1f, 1f, 1f, 1f, 0f, 0f, 0f);
        face(out, matrix, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f);
        face(out, matrix, 0f, 1f, 1f, 1f, -1f, 0f, 0f, 0f);
    }

    /**
     * Une face : deux coins de la base, puis la pointe.
     *
     * <p>La pointe est comptee deux fois. Le type de rendu du plasma n'accepte que des quads, et
     * c'est la seule maniere de lui donner le triangle de l'original : deux sommets confondus, et
     * l'arete qui les joint ne couvre aucun pixel.
     */
    private static void face(VertexConsumer out, Matrix4f matrix,
                             float ax, float ay, float au, float av,
                             float bx, float by, float bu, float bv) {
        out.vertex(matrix, ax, ay, 0f).color(1f, 1f, 1f, 1f).uv(au, av).endVertex();
        out.vertex(matrix, bx, by, 0f).color(1f, 1f, 1f, 1f).uv(bu, bv).endVertex();
        out.vertex(matrix, 0f, 0f, 1f).color(1f, 1f, 1f, 1f).uv(0f, 1f).endVertex();
        out.vertex(matrix, 0f, 0f, 1f).color(1f, 1f, 1f, 1f).uv(0f, 1f).endVertex();
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/" + name + ".png");
    }
}
