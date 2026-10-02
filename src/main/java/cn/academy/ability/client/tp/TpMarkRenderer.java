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

    /** La longueur d'un trait de coin, en fraction de la largeur de la boite : l'original, 0,2. */
    public static final double DASH = 0.2;

    /**
     * L'epaisseur d'un trait, en blocs.
     *
     * <p>L'original dessinait ses traits en {@code glLineWidth(3f)}, donc trois pixels quelle que
     * soit la distance. La 1.20.1 ne sait plus regler cette largeur, donc l'epaisseur se donne en
     * blocs : celle-ci fait trois a quatre pixels a quatre blocs de distance, la ou l'on regarde
     * une marque.
     */
    public static final double STROKE = 0.02;

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

        // La position d'image, et non celle du tick : une creature visee bouge a chaque image, et
        // la marque qui la suit doit bouger avec elle. Voir TeleportMark.interpolated.
        Vec3 mark = TeleportMark.interpolated(event.getPartialTick());
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
        // Deux formes, et deux seulement : le fantome de joueur des quatre competences qui
        // teleportent le corps, et la boite de l'original pour celles qui visent autre chose.
        if (TeleportMark.shape().isBox()) {
            drawBox(buffers, pose, camera, at);
            return;
        }

        VertexConsumer out = buffers.getBuffer(
                TpRenderType.mark(TEXTURES[frame(TeleportMark.ageTicks())]));

        pose.pushPose();
        pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        // Deux rotations, et dans cet ordre : le fantome se tourne comme son tireur, PUIS il est
        // retourne sens dessus dessous. C'est le `glRotated(-yaw)` suivi du `glScaled(-1,-1,1)` de
        // l'original, et les DEUX sont necessaires. Le modele d'un joueur est decrit la tete en
        // bas : c'est le rendu de toute entite vivante qui le retourne, par le meme
        // `scale(-1, -1, 1)` — voir `LivingEntityRenderer` — donc sans ce retournement le fantome
        // se tient sur la tete, et son origine se retrouve aux pieds au lieu du cou.
        //
        // Le lacet est celui de l'original, `-yaw` et non `180 - yaw` : le fantome regarde son
        // tireur, qui est toujours derriere lui. Il se retourne donc vers celui qui le regarde.
        pose.mulPose(Axis.YP.rotationDegrees(-TeleportMark.yaw()));
        pose.mulPose(Axis.ZP.rotationDegrees(180f));

        // La teinte vient de la marque elle-meme, et non d'un drapeau : chaque competence a sa
        // couleur, celles de l'original — le gris du vide, le rouge de la cible.
        int rgb = TeleportMark.color();
        float[] tint = { ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f };
        model().renderToBuffer(pose, out, 0, 0, tint[0], tint[1], tint[2], 1f);
        pose.popPose();
    }

    /**
     * La boite : le marqueur de l'original, pour les deux competences qui visent autre chose.
     *
     * <p>C'est un <b>coin</b> et non un contour : a chacun des huit coins de la boite, l'original
     * tirait trois petits traits vers l'interieur — un par axe — et rien ne reliait deux coins
     * entre eux. D'ou ces trous sur ses aretes, qui la font ressembler a un cadre en pointilles
     * plutot qu'a une cage, et qu'un simple contour ne rendrait pas.
     *
     * <p>Elle est posee <b>sur</b> la marque et non centree dessus : sa base est a la position, sa
     * hauteur monte au-dessus. C'est ce qui fait qu'une creature visee est couverte en entier —
     * ses pieds sur le sol, et sa boite jusqu'au sommet de sa tete.
     */
    private static void drawBox(MultiBufferSource buffers, PoseStack pose, Vec3 camera, Vec3 at) {
        TeleportMark.Shape shape = TeleportMark.shape();
        int argb = TeleportMark.color();
        float red = ((argb >> 16) & 0xFF) / 255f;
        float green = ((argb >> 8) & 0xFF) / 255f;
        float blue = (argb & 0xFF) / 255f;
        float alpha = ((argb >>> 24) & 0xFF) / 255f;

        double width = shape.width();
        double height = shape.height();
        double half = width / 2.0;
        double dash = DASH * width;

        // Tout se calcule par rapport a la camera : c'est le repere dans lequel la pose ecrit, et
        // celui ou un ruban sait de quel cote se tourner — la camera y est a l'origine.
        Vec3 base = at.subtract(camera);
        VertexConsumer out = buffers.getBuffer(TpRenderType.box());

        for (int ix = 0; ix <= 1; ix++) {
            for (int iy = 0; iy <= 1; iy++) {
                for (int iz = 0; iz <= 1; iz++) {
                    Vec3 corner = base.add(ix * width - half, iy * height, iz * width - half);
                    double dx = (ix == 0 ? dash : -dash);
                    double dy = (iy == 0 ? dash : -dash);
                    double dz = (iz == 0 ? dash : -dash);
                    stroke(out, pose, corner, new Vec3(dx, 0, 0), red, green, blue, alpha);
                    stroke(out, pose, corner, new Vec3(0, dy, 0), red, green, blue, alpha);
                    stroke(out, pose, corner, new Vec3(0, 0, dz), red, green, blue, alpha);
                }
            }
        }
    }

    /**
     * Un trait, dans le repere de la camera.
     *
     * <p>Ce n'est pas une ligne mais un <b>ruban</b> : un rectangle plat, tourne vers la camera, et
     * epais de quelques centimetres. L'original tirait ses traits en {@code glLineWidth(3f)}, donc
     * epais ; la 1.20.1 ne sait plus regler l'epaisseur d'une ligne — un trait de ligne y fait un
     * pixel, et rien d'autre — et un ruban est la seule facon d'avoir un trait large.
     *
     * <p>Il ne faut pas le confondre avec un petit parallelepipede, qui donnerait un <b>tube</b> a
     * section carree : a l'oeil, ce n'est plus un trait mais un cadre en barres. Le ruban, lui, se
     * retourne vers celui qui regarde et reste plat, donc se lit comme la ligne epaisse de
     * l'original, de n'importe ou.
     */
    private static void stroke(VertexConsumer out, PoseStack pose, Vec3 from, Vec3 offset,
                               float red, float green, float blue, float alpha) {
        Vec3 dir = offset.normalize();
        // La camera est a l'origine du repere, donc `from` est deja le vecteur qui va d'elle au
        // trait, et le produit vectoriel donne la perpendiculaire qui fait face a l'ecran.
        Vec3 side = dir.cross(from);
        if (side.lengthSqr() < 1.0E-7) {
            // On regarde le trait dans son axe : il n'a plus de largeur a montrer, et n'importe
            // quelle perpendiculaire fera l'affaire, puisqu'il est de profil.
            side = dir.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0E-7) side = dir.cross(new Vec3(1, 0, 0));
        }
        Vec3 half = side.normalize().scale(STROKE / 2.0);

        // Les deux bouts debordent d'une demi-epaisseur. Sans ce debordement, trois rubans qui se
        // rejoignent a un coin laissent un trou a cet endroit : chacun est decale de SA
        // perpendiculaire, donc leurs extremites ne se recouvrent pas exactement, et le coin parait
        // vide. En debordant, les trois se croisent et le coin est bouche.
        Vec3 cap = dir.scale(STROKE / 2.0);
        Vec3 from2 = from.subtract(cap);
        Vec3 to = from.add(offset).add(cap);

        vertex(out, pose, from2.subtract(half), red, green, blue, alpha);
        vertex(out, pose, to.subtract(half), red, green, blue, alpha);
        vertex(out, pose, to.add(half), red, green, blue, alpha);
        vertex(out, pose, from2.add(half), red, green, blue, alpha);
    }

    /** Un sommet de ruban : une position et une couleur, rien d'autre. */
    private static void vertex(VertexConsumer out, PoseStack pose, Vec3 at,
                               float red, float green, float blue, float alpha) {
        out.vertex(pose.last().pose(), (float) at.x, (float) at.y, (float) at.z)
                .color(red, green, blue, alpha).endVertex();
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
