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

/**
 * Le rendu du corps de plasma du canon, portage de {@code PlasmaBodyRenderer}.
 *
 * <p>L'original le dessinait d'un seul carre de vingt-deux blocs, tourne vers l'oeil, et son
 * nuanceur y marchait le rayon sur vingt pas en relisant jusqu'a seize boules. Sans nuanceur, le
 * port fait l'inverse : il <b>dessine les boules</b>, une par une, chacune a sa place et de sa
 * couleur — voir {@code PlasmaBodyVisuals}, qui les porte toutes.
 *
 * <p>Le dessin compose en <b>alpha normal</b> ({@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA}), comme
 * l'original : son nuanceur empilait ses vingt pas de la meme facon. C'est aussi ce qui garde les
 * <b>couleurs</b> des boules — en melange ajoute, un rose et un bleu superposes font du blanc, et
 * le joueur n'a vu que ca.
 *
 * <p>Et chaque boule se dessine <b>deux fois</b> : un <b>halo rose</b>, puis son <b>coeur bleu</b>
 * par-dessus — la densite du nuanceur est maximale au centre de chaque boule, donc chacune est rose
 * autour et bleue dedans. Une troisieme piece ferme le tout : le coeur bleu du <b>corps</b>, la ou
 * les densites de toutes les boules s'additionnent. Voir {@code PlasmaBodyVisuals#CORE_DENSITY}.
 *
 * <p>L'ordre est donc en <b>trois passes</b> — tous les halos, puis tous les coeurs de boule, puis
 * celui du corps — et non boule par boule : autrement, le halo d'une boule cacherait le coeur de
 * celle qu'elle recouvre.
 *
 * <p>Comme le reste des effets du port : aucune lumiere (un plasma emet la sienne), aucune face
 * cachee, et <b>pas d'ecriture de profondeur</b> — les boules se croisent en permanence, et si
 * elles ecrivaient la profondeur elles se decouperaient les unes les autres.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class PlasmaBodyRenderer {

    /**
     * L'image d'une boule : un disque BLANC a bord doux, ecrit par {@code make-plasma-ball.mjs}.
     *
     * <p>L'original n'en avait pas — son nuanceur calculait la forme. Le port a donc besoin d'un
     * disque, et il lui faut les deux proprietes que l'original obtenait par le calcul : <b>blanc</b>
     * (une couleur ne s'obtient qu'en teintant, et une image deja teintee ne peut pas devenir bleue
     * PUIS rose) et <b>doux</b> (un bord net se lit comme une pastille et non comme du plasma).
     */
    private static final ResourceLocation BALL = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/plasma_ball.png");

    private PlasmaBodyRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (PlasmaBodies.phase() == PlasmaBodies.Phase.NONE) return;

        float alpha = PlasmaBodies.alpha();
        float face = PlasmaBodyVisuals.coverage(alpha);
        if (face <= 0f) return;

        Vec3 camera = event.getCamera().getPosition();
        Matrix4f base = event.getPoseStack().last().pose();
        float partialTick = event.getPartialTick();
        double age = PlasmaBodies.ageSeconds(partialTick);
        Vec3 centre = PlasmaBodies.centre(partialTick);
        // L'echelle du corps : celle de la colonne du canon, figee a sa naissance. Sous un plafond
        // elle vaut moins de un, et tout le corps se resserre donc avec la colonne au lieu de
        // pendre sous elle comme une masse large comme une maison.
        double scale = PlasmaBodies.scale();

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
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        // Des couleurs tirees une fois : elles ne dependent ni de la boule ni de l'instant.
        float[] ballColor = PlasmaBodyVisuals.ballColor();
        float[] coreColor = PlasmaBodyVisuals.coreColor();

        // Les halos roses d'abord, TOUS : le halo d'une boule ne doit pas effacer le coeur de celle
        // qu'elle recouvre, donc les coeurs passent apres. Voir le commentaire de la classe.
        for (PlasmaBodyVisuals.Ball ball : PlasmaBodies.balls()) {
            Vec3 at = centre.add(PlasmaBodyVisuals.offset(ball, age));
            double halo = PlasmaBodyVisuals.quadRadius(
                    PlasmaBodyVisuals.haloRadius(ball.size(), alpha), scale);
            quad(buffer, base, camera, at, across.scale(halo), upright.scale(halo),
                    ballColor, face);
        }

        // Puis le coeur bleu de chacune, a la meme place et plus petit : c'est la ou sa propre
        // densite passe au bleu. Voir PlasmaBodyVisuals#coreRadius.
        for (PlasmaBodyVisuals.Ball ball : PlasmaBodies.balls()) {
            Vec3 at = centre.add(PlasmaBodyVisuals.offset(ball, age));
            double core = PlasmaBodyVisuals.quadRadius(
                    PlasmaBodyVisuals.coreRadius(ball.size(), alpha), scale);
            quad(buffer, base, camera, at, across.scale(core), upright.scale(core),
                    coreColor, face);
        }

        // Et celui du corps entier, par-dessus : au centre, les densites de toutes les boules
        // s'additionnent, et c'est la que le nuanceur faisait une tache bleue continue.
        double body = PlasmaBodyVisuals.quadRadius(PlasmaBodyVisuals.bodyCoreRadius(alpha), scale);
        quad(buffer, base, camera, centre, across.scale(body), upright.scale(body),
                coreColor, face);

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
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
