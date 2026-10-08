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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
 * blocs a la ronde, et chacun la dessinait alors sur son porteur. Le port le fait — voir
 * {@link #onAnnounced} — et il la dessine de la meme facon pour tout le monde.
 *
 * <h2>Ou elle se pose : les deux branches du vrai code, reproduites</h2>
 *
 * <p>Le joueur a tranche, et il connait le vrai mod : « elle n'est pas litteralement dans la main,
 * mais dans la main de la meme maniere que les autres competences, pour juste avoir l'impression que
 * c'est dans la main sans l'etre vraiment » ; puis, devant deux essais a cote de la plaque : « c'est
 * aussi cense etre la meme logique que pour l'arc gen, mais lit le vrai code et essaie de le faire ».
 * C'est ce qui a ete fait, et il n'y a plus rien a interpreter : l'original a DEUX branches, elles
 * sont reproduites telles quelles — voir {@link #EYE_SIDE} et {@link #SEEN_UP}, ou chacune est
 * chiffree et commentee.
 *
 * <p>Ce qu'il faut savoir du chemin, parce qu'il faut trois fichiers pour le lire : le hook est
 * appele par {@code RenderDummy}, dans le repere du MANNEQUIN du joueur — sa position, son lacet, et
 * son tangage en premiere personne seulement. Ce mannequin y ajoute deja quelque chose : le tangage
 * en premiere personne, et {@code ViewOptimize.fixThirdPerson()} en trois quarts — le decalage de
 * "main" de toute la librairie, dont le commentaire dit « transforms the origin to the player's hand
 * in thirdPerson or firstPerson ». Le hook pose ensuite SON decalage par-dessus, et les deux
 * s'additionnent. Rien n'est a chercher ailleurs.
 *
 * <p>ET L'EFFET SE DESSINE DANS LE MONDE, comme le bouclier de lumiere et comme les arcs — jamais
 * dans le rendu de la main, et c'est une lecon payee :
 *
 * <ul>
 *   <li>un carre pose dans le rendu de la main est dessine AVANT elle et ECRIT la profondeur : ses
 *       pixels vides cachaient la main et l'objet tenu (« cette animation cache ma main ») ;</li>
 *   <li>les autres joueurs n'ont pas de main a l'ecran, et il aurait fallu deux rendus et un
 *       placement par modele ;</li>
 *   <li>c'est le procede de tous les pouvoirs du port — le bouclier, les rayons, les arcs, les
 *       tornades — donc un effet de plus n'invente rien.</li>
 * </ul>
 *
 * <p>ET SA TAILLE N'EST PAS LA MEME DES DEUX COTES, ce que le joueur a vu avant que le code ne le
 * dise : « on voit bien les eclairs en troisieme vue, mais ils sont tres petits, ils tiennent dans
 * la main, alors que dans le vrai ils debordent ». L'original ne pose son echelle de 0,4 que dans
 * sa branche de PREMIERE personne — huit dixiemes de bloc, la taille d'une main — et ne met
 * <b>aucune</b> echelle de l'autre : le carre y fait ses deux unites, donc <b>deux blocs</b>, et
 * l'electricite deborde du bras et de l'epaule. Voir {@link #SCALE_HAND} et {@link #SCALE_WORLD}.
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

    /**
     * PREMIERE PERSONNE : les nombres de l'original, dans le repere de son mannequin.
     *
     * <p>Son {@code renderHand} commencait par rejoindre les YEUX ({@code glTranslated(0,
     * cos(pitch) * eyeHeight, sin(pitch) * eyeHeight)} depuis les pieds), puis decalait de
     * {@code (.26, -.15, -.24)}. Dans son repere — +X la droite du joueur, +Y son haut, -Z son
     * regard — cela fait <b>26 cm a droite, 15 cm en bas, 24 cm devant</b>.
     *
     * <p>Son abscisse, elle, a bouge deux fois, et dans les deux sens : 26 cm d'abord, que le joueur
     * trouvait « un peu trop de la droite » ; puis 10, qu'il a trouve « un peu trop vers la gauche ».
     * C'est donc le milieu, et c'est la seule valeur qui ne vient pas de l'original.
     */
    private static final double EYE_SIDE = 0.18;
    private static final double EYE_DOWN = 0.15;
    private static final double EYE_FORWARD = 0.24;

    /**
     * TROIS QUARTS : les pieds, un bloc au-dessus, 15 cm a droite et 77 cm devant — A L'HORIZONTALE.
     *
     * <p>C'est la branche {@code else} de l'original, plus ce que le mannequin y ajoute deja. Ce
     * chemin-la est en DEUX morceaux, et il faut les additionner :
     *
     * <ul>
     *   <li>{@code RenderDummy} applique {@code ViewOptimize.fixThirdPerson()} avant d'appeler le
     *       hook — soit {@code (0.15, -0.8, 0.23)}, le decalage de "main" de toute la librairie ;</li>
     *   <li>et le hook lui-meme pose alors {@code glTranslated(0, 1.8, -1)} — 1,8 au-dessus et un
     *       bloc d'avance.</li>
     * </ul>
     *
     * <p>Le total, dans le repere du mannequin (+X la droite, +Y le haut, -Z le regard) : <b>15 cm a
     * droite, 1 bloc au-dessus des pieds, 77 cm devant</b>. Un metre au-dessus des pieds, c'est la
     * poitrine : la rafale flotte a hauteur de bras, devant l'epaule, ce qui est bien ce qu'on voit
     * de quelqu'un qui lance quelque chose.
     *
     * <p>Et comme ce decalage est pose AVANT la rotation de tangage du mannequin, son avance est
     * <b>horizontale</b> : la rafale ne monte ni ne descend quand son porteur leve les yeux.
     *
     * <p>Sa taille, elle, n'est pas mise a l'echelle : le carre garde ses deux unites, donc deux
     * blocs. Voir {@link #SCALE_WORLD}.
     */
    private static final double SEEN_UP = 1.00;
    private static final double SEEN_SIDE = 0.15;
    private static final double SEEN_FORWARD = 0.77;

    /** La demi-largeur du carre, avant echelle : deux unites, comme son billboard. */
    private static final float HALF = 1.0f;

    /**
     * Son echelle en PREMIERE personne : 0,4, donc huit dixiemes de bloc — la taille d'une main.
     * C'est la seule echelle de l'original, et elle n'est que dans sa branche de main.
     */
    private static final float SCALE_HAND = 0.4f;

    /**
     * Et son echelle en TROIS QUARTS : aucune, soit les deux unites du carre.
     *
     * <p>L'original n'en posait pas la, et c'est ce que le joueur a vu de lui-meme : « en troisieme
     * vue ils sont tres petits, ils tiennent dans la main, alors que dans le vrai ils debordent ».
     * Deux blocs d'electricite autour du bras, donc, et non huit dixiemes.
     */
    private static final float SCALE_WORLD = 1.0f;

    /**
     * Les rafales en cours, par numero de joueur, et leur age en ticks.
     *
     * <p>La NOTRE y est comme les autres : depuis que l'effet se dessine dans le monde et non dans
     * le rendu de la main, il n'y a plus deux chemins mais un seul — ce qui a supprime d'un coup le
     * crochet de la main, le double rendu, et le risque de cacher la main.
     */
    private static final Map<Integer, Integer> PLAYING = new HashMap<>();

    /** Les images, et un type de rendu par image : les construire a chaque image allouerait. */
    private static final ResourceLocation[] FRAMES = frames();
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    private RailgunHandEffect() {
    }

    /**
     * Le joueur vient de jeter une piece : la rafale part si elle y a droit.
     *
     * <p>Appele par le client seulement — voir {@code ModItems.CoinItem.use} — et c'est ce qui rend
     * le geste instantane : le lanceur n'attend pas que son serveur ait annonce quoi que ce soit.
     * L'annonce arrive ensuite, et {@link #onAnnounced} ne la repose pas.
     */
    public static void onCoinThrown(Player player) {
        if (!allowed()) return;
        PLAYING.put(player.getId(), 0);
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
        PLAYING.put(player.getId(), 0);
    }

    /** Les deux verrous de l'original : l'aptitude ouverte, et le railgun sur une touche. */
    private static boolean allowed() {
        AbilityData data = ClientAbilityData.get();
        // CPData.canUseAbility, mot pour mot.
        if (!data.isActivated() || data.isOverloadRecovering() || data.isInterfered()) return false;
        // PresetData.getCurrentPreset.hasControllable(railgun).
        return ClientPresetData.get().getCurrent().contains(ElectromasterCategory.RAILGUN.getName());
    }

    /** Un tick : chaque rafale vieillit, et s'oublie quand ses images sont passees. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Pause ouverte, l'electricite se fige : elle reprendra ou elle en etait. Voir ClientPause.
        if (cn.academy.ability.client.ClientPause.frozen()) return;

        for (Iterator<Map.Entry<Integer, Integer>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> entry = it.next();
            entry.setValue(entry.getValue() + 1);
            if (entry.getValue() >= LIFE_TICKS) it.remove();
        }
    }

    /**
     * Le serveur annonce la rafale d'un joueur : c'est celle qu'on voit sur son porteur.
     *
     * <p>Une rafale DEJA en cours n'est pas relancee : le lanceur s'est annonce a lui-meme en
     * jetant sa piece (voir {@link #onCoinThrown}), et l'annonce lui revient un ou deux ticks plus
     * tard — la reposer ferait sauter son animation de retour en arriere.
     */
    public static void onAnnounced(int playerId) {
        if (PLAYING.containsKey(playerId)) return;
        PLAYING.put(playerId, 0);
    }

    /**
     * Les rafales, dessinees dans le monde — le procede du bouclier de lumiere.
     *
     * <p>CHAQUE RAFALE est posee par les deux branches du vrai hook, reproduites : la main de celui
     * qui regarde par ses yeux, le flottement devant l'epaule pour tout autre regard — voir
     * {@link #EYE_SIDE} et {@link #SEEN_UP}. La vue se decide par la meme regle que le mannequin de
     * l'original : « c'est moi, ET je regarde en premiere personne ».
     *
     * <p>Et le carre est ORIENTE COMME LE REGARD de son porteur, comme lui : ses deux rotations sont
     * celles du mannequin, lacet puis tangage. En premiere personne c'est exactement la camera, ce
     * qui est bien la raison d'etre de cette branche-la.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (PLAYING.isEmpty()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        boolean drawn = false;

        for (Map.Entry<Integer, Integer> entry : PLAYING.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof Player player)) continue;
            int frame = frameAt(entry.getValue(), partialTick);
            if (frame >= FRAME_COUNT) continue;

            // LES DEUX BRANCHES DE L'ORIGINAL, mot pour mot : la main de celui qui regarde par ses
            // yeux, le flottement devant l'epaule pour tout autre regard. Voir les constantes.
            boolean throughMyEyes = player == Minecraft.getInstance().player
                    && Minecraft.getInstance().options.getCameraType().isFirstPerson();
            Vec3 at = anchorOf(player, partialTick, throughMyEyes);
            float scale = throughMyEyes ? SCALE_HAND : SCALE_WORLD;

            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            // Et il oriente son carre COMME LE REGARD de son porteur — ses deux rotations sont
            // celles du mannequin, lacet puis tangage — et non vers la camera. En premiere personne
            // c'est la meme chose, et c'est bien pour cela que cette branche-la existe.
            faceLook(pose, player, partialTick);
            pose.scale(scale, scale, 1f);
            quad(buffers.getBuffer(type(FRAMES[frame])), pose.last().pose());
            pose.popPose();
            drawn = true;
        }

        if (drawn) buffers.endBatch();
    }

    /**
     * L'ancre de la rafale d'un porteur : ses YEUX en premiere personne, ses PIEDS en trois quarts.
     *
     * <p>Ce sont les deux branches de {@code RailgunHandEffect.renderHand}, mot pour mot — voir
     * {@link #EYE_SIDE} et {@link #SEEN_UP}, ou chacune est expliquee.
     *
     * <p>ET LES AXES VIENNENT DU LACET ET DU TANGAGE, pas du vecteur du regard. C'est le bug que le
     * joueur a vu : « si je vise a 100% en haut ou en bas, l'animation se decale ». Un vecteur du
     * regard qui pointe droit en l'air n'a plus d'horizontale du tout, et les deux axes qu'on en
     * tirait — sa droite et son haut — s'ecrasaient alors sur une direction de secours, fixe dans le
     * monde : la rafale sautait d'un coup a un endroit qui n'avait plus rien a voir avec la visee.
     * Le lacet et le tangage, eux, ne degenerent jamais, et c'est d'eux que le mannequin de l'original
     * tirait son repere — deux rotations, dans cet ordre. Ils sont INTERPOLES au temps partiel, comme
     * partout ailleurs, sinon la rafale avancerait par saccades d'un tick.
     */
    private static Vec3 anchorOf(Player player, float partialTick, boolean throughMyEyes) {
        float yaw = yawOf(player, partialTick);
        float pitch = pitchOf(player, partialTick);

        // Les trois axes du mannequin, en coordonnees du monde : sa droite,
        // son haut et SON ARRIERE (le regard est l'arriere retourne).
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 up = new Vec3(-Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch),
                Math.sin(pitch) * Math.cos(yaw));
        Vec3 back = new Vec3(Math.cos(pitch) * Math.sin(yaw), Math.sin(pitch),
                -Math.cos(pitch) * Math.cos(yaw));

        if (!throughMyEyes) {
            // Les pieds, un bloc plus haut, 15 cm a droite et 77 cm devant — et cette avance est
            // HORIZONTALE, comme chez lui : le decalage est pose avant la rotation de tangage.
            return player.getPosition(partialTick)
                    .add(0.0, SEEN_UP, 0.0)
                    .add(right.scale(SEEN_SIDE)).add(forward.scale(SEEN_FORWARD));
        }

        // Les yeux, puis les trois decalages dans le repere du regard : a droite, en bas, et devant.
        return player.getEyePosition(partialTick)
                .add(right.scale(EYE_SIDE)).add(up.scale(-EYE_DOWN)).add(back.scale(-EYE_FORWARD));
    }

    /** Le lacet du joueur, interpole, en radians. */
    private static float yawOf(Player player, float partialTick) {
        return (float) Math.toRadians(Mth.lerp(partialTick, player.yRotO, player.getYRot()));
    }

    /** Et son tangage, interpole lui aussi, en radians. */
    private static float pitchOf(Player player, float partialTick) {
        return (float) Math.toRadians(Mth.lerp(partialTick, player.xRotO, player.getXRot()));
    }

    /**
     * Tourne le carre comme le REGARD du porteur : ses deux rotations de mannequin, lacet puis
     * tangage.
     *
     * <p>Les memes angles que l'ancre, et pour la meme raison : tires du lacet et du tangage, ils ne
     * sautent pas quand la visee passe par la verticale.
     */
    private static void faceLook(PoseStack pose, Player player, float partialTick) {
        pose.mulPose(Axis.YP.rotationDegrees(-(float) Math.toDegrees(yawOf(player, partialTick))));
        pose.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(pitchOf(player, partialTick))));
    }

    /** L'image de la rafale a un instant donne : une toutes les quarante millisecondes. */
    private static int frameAt(int age, float partialTick) {
        return (int) ((age + partialTick) * 50.0 / PER_FRAME_MS);
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
     * <p>ET IL N'ECRIT PAS LA PROFONDEUR, comme les effets du plasma — meme piege, meme parade,
     * et il reste indispensable maintenant que la rafale se dessine dans le monde. Elle y passe
     * AVANT la main, qui vient tout a la fin du rendu, et elle est a un quart de bloc des yeux :
     * si ses pixels ecrivaient la profondeur, la main — dessinee apres elle, et plus loin — serait
     * refusee par le test de profondeur sur TOUTE la surface de son carre, y compris la ou il n'y a
     * rien a voir. Le joueur l'a dit ainsi : « cette animation cache ma main, l'objet que j'ai en
     * main ». Ce n'etait pas la rafale qui cachait la main, c'etait son carre vide : en n'ecrivant
     * plus la profondeur, les arcs se posent derriere la main et la main reste ou elle est.
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
