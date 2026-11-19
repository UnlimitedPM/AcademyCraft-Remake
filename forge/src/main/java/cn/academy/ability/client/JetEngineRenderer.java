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
 * Dessine le bouclier de diamant du reacteur, portage de {@code RenderDiamondShield}.
 *
 * <h2>Les ondes de la marque</h2>
 *
 * <p>Elles ne se dessinent plus ici : c'est {@link RippleMark} — la classe de
 * l'{@code EntityRippleMark} de l'original — qui s'en charge, et c'est le <b>claquement d'orage</b>
 * qui l'a fait demenager. Le port les avait ouvertes dans ce fichier pour le reacteur, mais
 * l'original n'avait qu'une marque et qu'un rendu pour les deux competences ; il en va de meme ici.
 * Ses courbes, elles, sont restees dans {@code JetEngineVisuals}, ou elles ont ete portees et ou un
 * test les fige.
 *
 * <h2>Le bouclier de diamant</h2>
 *
 * <p>Une pyramide a quatre faces, posee un bloc devant les yeux du porteur et 1,1 bloc au-dessus
 * de ses pieds, tournee comme son regard, et mise a l'echelle 1,5. Elle vit pendant tout le vol,
 * et seulement lui.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class JetEngineRenderer {

    private static final ResourceLocation DIAMOND = texture("diamond_shield");

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

    /** Les trois ondes de la marque, au point vise — voir {@link RippleMark}. */
    private static void drawMark(RenderLevelStageEvent event, PoseStack pose,
                                 MultiBufferSource buffers, Vec3 camera) {
        Vec3 at = JetEngineEffect.markAt();
        if (at == null) return;

        double age = JetEngineEffect.markAgeSeconds(event.getPartialTick());
        if (age < 0) return;

        RippleMark.draw(pose, buffers, camera, at, age,
                JetEngineVisuals.RED, JetEngineVisuals.GREEN, JetEngineVisuals.BLUE);
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
