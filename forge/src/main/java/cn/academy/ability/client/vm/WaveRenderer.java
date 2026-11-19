package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Le rendu des ondes de vecmanip, portage du rendu de {@code WaveEffect}.
 *
 * <p>Chaque anneau est un carre de lueur pose <b>perpendiculairement</b> a l'axe du coup : la pile
 * s'ouvre donc devant le poing, et non face au spectateur. C'est le repere du lanceur qui
 * l'oriente — son lacet puis son tangage —, et la pile avance d'un quarantieme de bloc par tick.
 *
 * <h2>Deux etats, et pas ceux du reste du port</h2>
 *
 * <p>L'original dessinait ses ondes <b>au travers des murs</b> et sans trier leurs faces : le
 * test de profondeur tombe donc, et c'est ce qui se voit le mieux quand on se bat dans un couloir
 * — l'onde du choc dirige s'ouvre meme si le mur la cache.
 *
 * <p>Comme le test de profondeur tombe, ces carres ne peuvent pas passer par un type de rendu
 * range : ce genre de type <b>rebat son propre etat</b> au moment du vidage, donc le notre serait
 * repris avant la premiere image. Ils se dessinent tout de suite, avec leur etat pose a la main —
 * la meme recette que la detection de minerais, qui a paye la lecon avant eux.
 *
 * <p>Et le <b>brouillard</b> est repousse le temps du dessin, comme pour elle : sans cela, la
 * cecite d'une detection en cours repeindrait l'onde en noir.
 *
 * <p>Enfin l'age se lit <b>entre deux ticks</b> : ce qui vieillit ici — l'echelle, l'opacite,
 * l'avancee — lissait sinon a vingt images par seconde, ce que le joueur a vu. Voir
 * {@code VecWaves#ageAt}.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class WaveRenderer {

    /** L'image de l'original, telle quelle : un disque de lueur doux. */
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/glow_circle.png");

    /** Le brouillard, repousse a l'infini le temps du dessin — voir l'entete. */
    private static final float FOG_AWAY = 1.0E9f;

    private WaveRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        List<VecWaves.Wave> waves = VecWaves.live();
        if (waves.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();
        float partialTick = event.getPartialTick();

        RenderSystem.setShaderFogStart(FOG_AWAY);
        RenderSystem.setShaderFogEnd(FOG_AWAY);
        RenderSystem.setShader(GameRenderer::getRendertypeBeaconBeamShader);
        RenderSystem.setShaderTexture(0, GLOW);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (VecWaves.Wave wave : waves) {
            Matrix4f frame = new Matrix4f(base)
                    .translate((float) (wave.position().x - camera.x),
                            (float) (wave.position().y - camera.y),
                            (float) (wave.position().z - camera.z))
                    .rotateY((float) Math.toRadians(-wave.yaw()))
                    .rotateX((float) Math.toRadians(wave.pitch()));
            // L'age se lit ENTRE deux ticks, pas au tick : sinon la lueur qui s'efface avance par
            // sauts de vingt par seconde. Voir VecWaves#ageAt.
            double age = VecWaves.ageAt(wave, partialTick);
            double scale = VecWaves.sizeScale(age);
            double drift = VecWaves.drift(age);

            for (VecWaves.Ring ring : wave.rings()) {
                float alpha = (float) VecWaves.alpha(age, ring);
                if (alpha <= 0f) continue;

                double half = ring.size() * scale / 2;
                double z = (float) (drift + ring.offset());
                quad(buffer, frame, half, z, alpha);
            }
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Un anneau : le carre de lueur, dans le plan de la pile. */
    private static void quad(BufferBuilder buffer, Matrix4f frame, double half, double z,
                             float alpha) {
        vertex(buffer, frame, -half, -half, z, 0f, 0f, alpha);
        vertex(buffer, frame, half, -half, z, 1f, 0f, alpha);
        vertex(buffer, frame, half, half, z, 1f, 1f, alpha);
        vertex(buffer, frame, -half, half, z, 0f, 1f, alpha);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f frame, double x, double y, double z,
                               float u, float v, float alpha) {
        buffer.vertex(frame, (float) x, (float) y, (float) z)
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .endVertex();
    }
}
