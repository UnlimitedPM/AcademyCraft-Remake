package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
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

/**
 * Le rendu du corps de plasma du canon, portage de {@code PlasmaBodyRenderer}.
 *
 * <p>L'original le dessinait d'un seul carre de vingt-deux blocs, tourne vers l'oeil, et son
 * nuanceur y marchait le rayon sur vingt pas en relisant jusqu'a seize boules. Sans nuanceur, le
 * port fait l'inverse : il <b>dessine les boules</b>, une par une, chacune a sa place et de sa
 * couleur — voir {@code PlasmaBodyVisuals}, qui les porte toutes.
 *
 * <p>Le dessin <b>ajoute</b> sa lumiere ({@code SRC_ALPHA / ONE}) : c'est ainsi qu'une dizaine de
 * boules se fondent en un seul corps au lieu de s'empiler, et c'est la seule facon d'approcher un
 * volume qui s'eclaircissait la ou il s'epaississait. L'opacite d'une boule est donc basse — voir
 * {@code DRAW_ALPHA} — et c'est leur recouvrement qui fait le coeur.
 *
 * <p>Comme le reste des effets du port : aucune lumiere (un plasma emet la sienne), aucune face
 * cachee, et <b>pas d'ecriture de profondeur</b> — les boules se croisent en permanence, et si
 * elles ecrivaient la profondeur elles se decouperaient les unes les autres.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class PlasmaBodyRenderer {

    /**
     * L'image d'une boule : un disque BLANC, pris a la bille de plasma du meltdowner.
     *
     * <p>L'original n'en avait pas — son nuanceur calculait la forme — et le port a besoin d'un
     * disque pour la dessiner. Blanc, parce qu'une couleur ne s'obtient qu'en teintant une image
     * blanche : une image deja teintee ne peut pas devenir bleue puis rose.
     */
    private static final ResourceLocation BALL = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/mdball/0.png");

    private PlasmaBodyRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (PlasmaBodies.phase() == PlasmaBodies.Phase.NONE) return;

        float alpha = PlasmaBodies.alpha() * PlasmaBodyVisuals.DRAW_ALPHA;
        if (alpha <= 0f) return;

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();
        float partialTick = event.getPartialTick();
        double age = PlasmaBodies.ageSeconds(partialTick);
        Vec3 centre = PlasmaBodies.centre(partialTick);

        // Les carres sont dans le plan de l'ecran : la gauche et le haut de la camera suffisent,
        // et une boule se presente donc toujours de face.
        org.joml.Vector3f left = event.getCamera().getLeftVector();
        org.joml.Vector3f up = event.getCamera().getUpVector();
        Vec3 across = new Vec3(left.x, left.y, left.z);
        Vec3 upright = new Vec3(up.x, up.y, up.z);

        RenderSystem.setShader(GameRenderer::getRendertypeBeaconBeamShader);
        RenderSystem.setShaderTexture(0, BALL);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (PlasmaBodyVisuals.Ball ball : PlasmaBodies.balls()) {
            Vec3 at = centre.add(PlasmaBodyVisuals.offset(ball, age));
            float[] rgb = PlasmaBodyVisuals.color(ball.size());
            double radius = ball.size() * PlasmaBodyVisuals.DRAW_SCALE;
            quad(buffer, base, camera, at, across.scale(radius), upright.scale(radius), rgb, alpha);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Un carre tourne vers l'oeil.
     *
     * <p>L'ordre des coins suit la convention du projet : celui qui va vers la <b>gauche</b> de la
     * camera lit le cote gauche de l'image, et le haut lit le haut. Sur un disque symetrique cela ne
     * se voit pas, mais c'est la convention correcte et elle ne coute rien.
     */
    private static void quad(BufferBuilder buffer, Matrix4f base, Vec3 camera, Vec3 centre,
                             Vec3 across, Vec3 upright, float[] rgb, float alpha) {
        corner(buffer, base, camera, centre.subtract(across).add(upright), rgb, alpha, 0f, 0f);
        corner(buffer, base, camera, centre.subtract(across).subtract(upright), rgb, alpha, 0f, 1f);
        corner(buffer, base, camera, centre.add(across).subtract(upright), rgb, alpha, 1f, 1f);
        corner(buffer, base, camera, centre.add(across).add(upright), rgb, alpha, 1f, 0f);
    }

    private static void corner(BufferBuilder buffer, Matrix4f base, Vec3 camera, Vec3 at,
                               float[] rgb, float alpha, float u, float v) {
        buffer.vertex(base, (float) (at.x - camera.x), (float) (at.y - camera.y),
                        (float) (at.z - camera.z))
                .color(rgb[0], rgb[1], rgb[2], alpha)
                .uv(u, v)
                .endVertex();
    }
}
