package cn.academy.ability.client.tp;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Le rendu des taches de sang du retour de sang.
 *
 * <p>Chaque tache est un carre pose contre une face de bloc, incline dans son plan et tourne vers
 * l'exterieur : elle se dessine donc des <b>deux cotes</b> — un mur se voit de sa piece comme de
 * l'autre — sans ecrire la profondeur, sinon deux taches qui se chevauchent se decouperaient l'une
 * l'autre. Le reste est l'ordinaire de la maison : la lueur de l'original est une texture, donc
 * une image et pas un nuanceur.
 *
 * <p>Le monde est aussi ce qui les fait vivre : elles s'en vont au bout d'une minute, ou des que
 * le bloc qui les porte disparait — c'est le tick d'horloge, plus bas.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class BloodSprayRenderer {

    /** Les deux familles d'images, et leurs trois tirages. */
    private static final String GRND = "grnd";
    private static final String WALL = "wall";

    private BloodSprayRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (BloodSprays.live().isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();

        RenderSystem.setShader(GameRenderer::getRendertypeBeaconBeamShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        boolean drawing = false;
        ResourceLocation bound = null;
        for (BloodSprays.Spray spray : BloodSprays.live()) {
            ResourceLocation texture = texture(spray);
            if (!texture.equals(bound)) {
                if (drawing) {
                    BufferUploader.drawWithShader(buffer.end());
                    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                }
                RenderSystem.setShaderTexture(0, texture);
                bound = texture;
                drawing = true;
            }
            quad(buffer, base, camera, spray);
        }

        if (drawing) BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Un tick d'horloge : les taches vieillissent, et celles qui n'ont plus de bloc s'en vont. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Level level = Minecraft.getInstance().level;
        if (level != null) BloodSprays.tick(level);
    }

    /** L'image d'une tache : sa famille, et son tirage. */
    static ResourceLocation texture(BloodSprays.Spray spray) {
        String family = BloodSprays.family(spray);
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/blood_spray/" + family + "/" + spray.frame() + ".png");
    }

    /** Les quatre coins d'une tache, dans le plan de sa face et inclines de son angle. */
    static Vec3[] corners(BloodSprays.Spray spray) {
        Vec3 centre = spray.centre();
        Vec3 along = BloodSprays.inPlane(spray.face(), 1, 0);
        Vec3 across = BloodSprays.inPlane(spray.face(), 0, 1);

        float radians = BloodSprays.radians(spray);
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);
        double half = spray.size() / 2;

        Vec3 right = along.scale(cos).add(across.scale(sin)).scale(half);
        Vec3 down = across.scale(cos).subtract(along.scale(sin)).scale(half);

        return new Vec3[] {
                centre.subtract(right).subtract(down),
                centre.add(right).subtract(down),
                centre.add(right).add(down),
                centre.subtract(right).add(down),
        };
    }

    private static void quad(BufferBuilder buffer, Matrix4f base, Vec3 camera,
                             BloodSprays.Spray spray) {
        Vec3[] corners = corners(spray);
        float[][] uv = { { 0f, 1f }, { 1f, 1f }, { 1f, 0f }, { 0f, 0f } };
        for (int i = 0; i < 4; i++) {
            Vec3 corner = corners[i];
            buffer.vertex(base, (float) (corner.x - camera.x), (float) (corner.y - camera.y),
                            (float) (corner.z - camera.z))
                    .color(1f, 1f, 1f, 1f)
                    .uv(uv[i][0], uv[i][1])
                    .endVertex();
        }
    }
}
