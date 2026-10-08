package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.electromaster.RailgunSkill;
import cn.academy.ability.preset.client.ClientPresetData;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * La rafale d'arcs qui jaillit de la main : l'animation speciale de l'electromaster quand il
 * lance sa piece.
 *
 * <p>Portage de {@code RailgunHandEffect}, et il n'y a rien a comprendre de plus que ce que
 * faisait l'original : <b>quarante images de quarante millisecondes</b> — un peu plus d'une
 * seconde et demie d'electricite — posees devant la main, l'une apres l'autre, puis la derniere
 * reste et l'effet s'oublie. Les images sont {@code textures/effects/arc_burst/0.png} a
 * {@code 39.png}, telles quelles.
 *
 * <h2>Quand elle part, et c'est tout son interet</h2>
 *
 * <p>Pas a chaque lancer de piece : l'original la gardait derriere <b>deux</b> verrous, et le
 * joueur les a redemandes mot pour mot — « uniquement quand on a le pouvoir d'un electromaster
 * et uniquement si la competence du railgun est presente dans nos competences actives
 * actuelles ». C'est la condition {@code spawn} de {@code Railgun.onThrowCoin} :
 *
 * <ul>
 *   <li>{@code CPData.canUseAbility} : l'aptitude allumee, pas de surcharge pleine, pas de
 *       brouillage — {@link AbilityData}, dont le port de la regle est cite ailleurs ;</li>
 *   <li>{@code PresetData.getCurrentPreset.hasControllable(this)} : le railgun est sur une
 *       touche du <b>prereglage en service</b>. Avoir appris la competence ne suffit donc pas,
 *       et c'est ce qui fait la difference entre « je suis electromaster » et « mon railgun est
 *       pret ».</li>
 * </ul>
 *
 * <p>Les deux se lisent chez le CLIENT, sur ses deux miroirs de donnees
 * ({@link ClientAbilityData} et {@link ClientPresetData}) : rien ne voyage, et le joueur voit
 * l'electricite au moment ou il jette sa piece, sans attendre un aller-retour. C'est ce que
 * faisait l'original — son client avait sa propre copie de la piece, donc sa propre decision.
 *
 * <p>En revanche l'original la montrait AUSSI aux autres : son serveur l'annoncait a trente
 * blocs a la ronde, et chacun la dessinait alors sur la main du lanceur, de trois quarts. Le port
 * ne le fait pas encore — voir {@link #onCoinThrown}.
 *
 * <h2>Ou elle se pose</h2>
 *
 * <p>Les nombres sont ceux de l'original, au signe pres, et ce signe vaut la peine d'etre dit :
 * son repere etait celui du mannequin qui portait l'effet, ou l'avant est Z <b>positif</b>, alors
 * que celui du rendu de la main de Minecraft a l'avant en Z <b>negatif</b>. Son {@code -.24} est
 * donc le {@code -0.24} d'ici : devant les yeux, un peu a droite et un peu bas — la place de la
 * main qui vient de lacher la piece. Le carre fait deux unites avant echelle et 0,4 apres, donc
 * huit dixiemes de bloc, dont le dessin n'occupe que le milieu : c'est un jaillissement d'arcs,
 * pas un flash d'ecran.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class RailgunHandEffect {

    /** Les images de la rafale : quarante, comme le {@code COUNT} de l'original. */
    private static final int FRAME_COUNT = 40;

    /** Et leur cadence : quarante millisecondes chacune, comme son {@code PER_FRAME}. */
    private static final double PER_FRAME_MS = 40;

    /** Ce que la rafale dure, en ticks : quarante fois quarante millisecondes, arrondi au tick. */
    private static final int LIFE_TICKS = 32;

    /** Ou elle se pose, devant la main — les trois decalages de l'original, en blocs. */
    private static final float HAND_X = 0.26f;
    private static final float HAND_Y = -0.15f;
    private static final float HAND_Z = -0.24f;

    /** La demi-largeur du carre avant echelle, et son echelle : deux unites, puis 0,4. */
    private static final float HALF = 1.0f;
    private static final float SCALE = 0.4f;

    /** L'age de la rafale en cours, en ticks, ou {@code -1} quand il n'y en a pas. */
    private static int age = -1;

    /**
     * Les rafales des AUTRES joueurs, par numero d'entite, et leur age.
     *
     * <p>Elles vivent separement de la notre : plusieurs peuvent courir en meme temps, et elles
     * n'ont rien a voir avec la main du joueur local — c'est le modele de l'autre qui les porte.
     */
    private static final Map<Integer, Integer> OTHERS = new HashMap<>();

    /** Ou tombe la main d'un joueur en vue de trois quarts, en blocs : devant, de biais, en haut. */
    private static final double HAND_FORWARD = 0.35;
    private static final double HAND_SIDE = 0.30;
    private static final double HAND_HEIGHT = 1.25;

    /** Les images, et un type de rendu par image : les construire a chaque image allouerait. */
    private static final ResourceLocation[] FRAMES = frames();
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    private RailgunHandEffect() {
    }

    /**
     * Le joueur vient de jeter une piece : la rafale part si elle y a droit.
     *
     * <p>Appele par le client seulement — voir {@code ModItems.CoinItem.use} — et c'est aussi le
     * seul endroit ou l'effet nait : les autres joueurs ne la voient pas. L'original, lui,
     * l'annoncait a trente blocs ({@code MSG_CHARGE_EFFECT}) et la dessinait sur la main du
     * lanceur, en trois quarts par-dessus le marche. Ce sera l'affaire d'un paquet et d'une
     * couche de rendu du modele du joueur, et le port n'en a encore aucune.
     */
    public static void onCoinThrown(Player player) {
        if (!allowed()) return;
        age = 0;
    }

    /**
     * La <b>voie du fer</b> : la touche du railgun, un lingot ou un bloc de fer en main.
     *
     * <p>C'est le second chemin de l'original — voir {@code isAccepted} de {@code Railgun} — et sa
     * rafale partait exactement la meme : son {@code Delegate.onKeyDown} appelait
     * {@code spawnClientEffect} des que la main tenait du fer. Elle ne passe donc pas par la piece,
     * et n'attend pas non plus la reponse du serveur : le geste part a l'appui, comme chez lui.
     *
     * <p>Un mot sur les verrous, ici : celui du PREREGLAGE ne se pose pas, puisque appuyer sur cette
     * touche prouve deja que le railgun y est range ; celui de l'APTITUDE, le port le garde, comme
     * partout ailleurs — une main qui ne peut rien faire ne fait rien.
     *
     * @param skill la competence de la touche pressee
     * @param player le joueur local, ou {@code null} s'il n'y en a pas
     */
    public static void onIronAimed(Skill skill, Player player) {
        if (player == null || skill != ElectromasterCategory.RAILGUN) return;
        if (!RailgunSkill.isAccepted(player.getMainHandItem())) return;
        if (!ClientAbilityData.get().isActivated()) return;
        age = 0;
    }

    /** Les deux verrous de l'original : l'aptitude ouverte, et le railgun sur une touche. */
    private static boolean allowed() {
        AbilityData data = ClientAbilityData.get();
        // CPData.canUseAbility, mot pour mot.
        if (!data.isActivated() || data.isOverloadRecovering() || data.isInterfered()) return false;
        // PresetData.getCurrentPreset.hasControllable(railgun).
        return ClientPresetData.get().getCurrent().contains(ElectromasterCategory.RAILGUN.getName());
    }

    /** Un tick : la rafale vieillit, et s'oublie quand ses images sont passees. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Pause ouverte, l'electricite se fige : elle reprendra ou elle en etait. Voir ClientPause.
        if (cn.academy.ability.client.ClientPause.frozen()) return;
        if (age >= 0 && ++age >= LIFE_TICKS) age = -1;

        for (Iterator<Map.Entry<Integer, Integer>> it = OTHERS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> entry = it.next();
            entry.setValue(entry.getValue() + 1);
            if (entry.getValue() >= LIFE_TICKS) it.remove();
        }
    }

    /**
     * Le serveur annonce la rafale d'un joueur : c'est celle des AUTRES, et elle se dessine sur son
     * modele, de trois quarts.
     *
     * <p>Sauf si c'est la notre et qu'on regarde en premiere personne : elle joue alors deja, dans
     * le repere de la main (voir {@link #onRenderHand}), et la dessiner une seconde fois la
     * doublerait. En vue de trois quarts, en revanche, il n'y a pas de main a l'ecran : c'est cette
     * voie-la qui la montre, sur notre propre modele.
     */
    public static void onAnnounced(int playerId) {
        var me = Minecraft.getInstance().player;
        if (me != null && me.getId() == playerId
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        OTHERS.put(playerId, 0);
    }

    /**
     * La rafale d'un autre joueur, dessinee dans le monde.
     *
     * <p>L'original la posait a un endroit FIXE de son porteur — un bloc huit dixiemes au-dessus de
     * ses pieds, et un bloc devant — sans suivre le bras, qui bouge. Le port prend la main DROITE,
     * qui est celle qui lance : en vue de trois quarts elle tombe devant, de biais et a hauteur de
     * poitrine.
     *
     * <p>Le carre regarde la CAMERA, et c'est ce que le port ajoute a l'original : lui le dessinait
     * dans le repere du joueur, donc de profil pour qui se tenait de cote. Une etincelle qu'on ne
     * voit pas ne sert a rien.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (OTHERS.isEmpty()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        boolean drawn = false;

        for (Map.Entry<Integer, Integer> entry : OTHERS.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof Player player)) continue;
            int frame = frameAt(entry.getValue(), partialTick);
            if (frame >= FRAME_COUNT) continue;

            Vec3 at = handOf(player, partialTick);
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            faceCamera(pose, camera.subtract(at));
            pose.scale(SCALE, SCALE, 1f);
            quad(buffers.getBuffer(type(FRAMES[frame])), pose.last().pose());
            pose.popPose();
            drawn = true;
        }

        if (drawn) buffers.endBatch();
    }

    /** Ou est la main droite de ce joueur, en blocs du monde. */
    private static Vec3 handOf(Player player, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0, flat.x);
        return player.getPosition(partialTick)
                .add(flat.scale(HAND_FORWARD)).add(side.scale(HAND_SIDE))
                .add(0.0, HAND_HEIGHT, 0.0);
    }

    /** Tourne le carre pour qu'il regarde la camera, d'ou qu'elle vienne. */
    private static void faceCamera(PoseStack pose, Vec3 toCamera) {
        double length = toCamera.length();
        if (length < 1.0E-6) return;
        double yaw = Math.toDegrees(Math.atan2(-toCamera.x, toCamera.z));
        double pitch = Math.toDegrees(Math.asin(-toCamera.y / length));
        pose.mulPose(Axis.YP.rotationDegrees((float) -yaw));
        pose.mulPose(Axis.XP.rotationDegrees((float) pitch));
    }

    /**
     * La rafale, dessinee dans l'empilement de poses de la main.
     *
     * <p>Une seule fois par image : l'evenement passe pour les deux mains, et le port n'en veut
     * qu'une — la principale, celle qui a lance la piece.
     *
     * <p>Le carre est dans le plan X Y de ce repere, et c'est exactement ce qu'il faut : ce repere
     * EST celui de la camera, donc un carre qui lui fait face est un carre qui fait face au
     * joueur, vu de face quoi qu'il regarde. L'original le dessinait avec une maille de
     * « billboard » pour la meme raison.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (age < 0) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        int frame = frameAt(event.getPartialTick());
        if (frame >= FRAME_COUNT) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(HAND_X, HAND_Y, HAND_Z);
        pose.scale(SCALE, SCALE, 1f);
        quad(event.getMultiBufferSource().getBuffer(type(FRAMES[frame])), pose.last().pose());
        pose.popPose();
    }

    /** L'image de la rafale a un instant donne : une toutes les quarante millisecondes. */
    private static int frameAt(int age, float partialTick) {
        return (int) ((age + partialTick) * 50.0 / PER_FRAME_MS);
    }

    /** L'image de la rafale du joueur, a cette image-ci. */
    private static int frameAt(float partialTick) {
        return frameAt(age, partialTick);
    }

    /** Le carre de l'image : deux triangles, dans le plan de la camera, image a l'endroit. */
    private static void quad(VertexConsumer out, org.joml.Matrix4f pose) {
        out.vertex(pose, -HALF, -HALF, 0f).color(255, 255, 255, 255).uv(0f, 1f).endVertex();
        out.vertex(pose, HALF, -HALF, 0f).color(255, 255, 255, 255).uv(1f, 1f).endVertex();
        out.vertex(pose, HALF, HALF, 0f).color(255, 255, 255, 255).uv(1f, 0f).endVertex();
        out.vertex(pose, -HALF, HALF, 0f).color(255, 255, 255, 255).uv(0f, 0f).endVertex();
    }

    /** Les quarante images de la rafale, dans l'ordre. */
    private static ResourceLocation[] frames() {
        ResourceLocation[] result = new ResourceLocation[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            result[i] = ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                    "textures/effects/arc_burst/" + i + ".png");
        }
        return result;
    }

    /**
     * Le type de rendu d'une image : une image transparente, sans eclairage.
     *
     * <p>C'est celui des arcs et du plasma, mot pour mot — voir {@code ArcRenderer} — et c'est le
     * meme besoin que chez l'original, qui posait son melange a la main
     * ({@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA}), coupait le tri des faces arriere, et dessinait
     * avec un programme qui ne connait <b>ni normale ni lumiere</b> : une etincelle emet la
     * sienne, et l'eclairer avec la lumiere du monde l'eteindrait dans le noir.
     *
     * <p>ET IL N'ECRIT PAS LA PROFONDEUR, comme les effets du plasma — meme piege, meme parade.
     * La rafale est un carre de huit dixiemes de bloc pose a un quart de bloc des yeux, donc
     * DEVANT la main ; si ses pixels ecrivaient la profondeur, la main — dessinee apres lui, et
     * plus loin — serait refusee par le test de profondeur sur TOUTE la surface du carre, y
     * compris la ou il n'y a rien a voir. Le joueur l'a dit ainsi : « cette animation cache ma
     * main, l'objet que j'ai en main ». Ce n'est pas la rafale qui cache la main, c'est son
     * carre vide : en n'ecrivant plus la profondeur, l'arcs se pose par-dessus la main et la
     * main reste ou elle est.
     */
    private static RenderType type(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, tex -> RenderType.create("academy_railgun_hand",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                GameRenderer::getRendertypeBeaconBeamShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                                "academy_railgun_hand",
                                () -> {
                                    RenderSystem.enableBlend();
                                    RenderSystem.blendFunc(SourceFactor.SRC_ALPHA,
                                            DestFactor.ONE_MINUS_SRC_ALPHA);
                                },
                                () -> {
                                    RenderSystem.disableBlend();
                                    RenderSystem.defaultBlendFunc();
                                }))
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .createCompositeState(true)));
    }
}
