package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Le rendu des bouffees de fumee du choc au sol.
 *
 * <p>Chaque bouffee est un quad tourne vers l'oeil — c'est ce que l'original faisait, en
 * l'orientant sur la direction qui va de la camera a la bouffee. Elle se dessine sans ecrire la
 * profondeur, comme tout ce qui est translucide, et sans cull : on la voit des deux cotes.
 *
 * <p>L'atlas est un carre de deux images sur deux, et chaque bouffee garde la sienne toute sa vie.
 * Une bouffee dont l'opacite est retombee a zero n'est plus dessinee du tout, mais elle vit encore
 * quelques secondes.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class SmokePuffRenderer {

    private SmokePuffRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (Smokes.live().isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();
        float partialTick = event.getPartialTick();
        org.joml.Vector3f left = event.getCamera().getLeftVector();
        org.joml.Vector3f up = event.getCamera().getUpVector();
        Vec3 across = new Vec3(left.x, left.y, left.z);
        Vec3 upright = new Vec3(up.x, up.y, up.z);

        RenderSystem.setShader(GameRenderer::getRendertypeBeaconBeamShader);
        RenderSystem.setShaderTexture(0, Smokes.TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (Smokes.Puff puff : Smokes.live()) {
            float alpha = puff.alpha(partialTick);
            if (alpha <= 0f) continue;

            Vec3 centre = puff.at(partialTick);
            float u = Smokes.frameU(puff.frame());
            float v = Smokes.frameV(puff.frame());
            Vec3 a = across.scale(Smokes.SIZE);
            Vec3 b = upright.scale(Smokes.SIZE);

            vertex(buffer, base, camera, centre.subtract(a).subtract(b), u, v, alpha);
            vertex(buffer, base, camera, centre.add(a).subtract(b), u + 0.5f, v, alpha);
            vertex(buffer, base, camera, centre.add(a).add(b), u + 0.5f, v + 0.5f, alpha);
            vertex(buffer, base, camera, centre.subtract(a).add(b), u, v + 0.5f, alpha);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f base, Vec3 camera, Vec3 at,
                               float u, float v, float alpha) {
        buffer.vertex(base, (float) (at.x - camera.x), (float) (at.y - camera.y),
                        (float) (at.z - camera.z))
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }
}
