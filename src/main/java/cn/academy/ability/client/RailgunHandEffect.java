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
 * <h2>Ou elle se pose : la convention de TOUT le mod, retrouvee dans le vrai code</h2>
 *
 * <p>Le joueur a tranche, et il connait le vrai mod : « elle n'est pas litteralement dans la main,
 * mais dans la main de la meme maniere que les autres competences, pour juste avoir l'impression que
 * c'est dans la main sans l'etre vraiment » — puis, devant un premier essai a cote de la plaque :
 * « c'est aussi cense etre la meme logique que pour l'arc gen, mais regarde aussi le vrai code ».
 * Il avait raison, et le vrai code dit tout : les arcs de {@code arc_gen}, de la charge, du
 * mouvement magnetique et du thunder bolt sont tous des {@code EntityArc}, dont le constructeur
 * pose l'entite a {@code player.posY + eyeHeight} et lui donne le REGARD du joueur — puis dont le
 * rendu appelle {@code ViewOptimize.fix}. Et {@code ViewOptimize} l'annonce dans son propre
 * commentaire : « transforms the origin to the player's hand in thirdPerson or firstPerson ».
 *
 * <p>C'est donc cette convention que le port reprend, et rien d'autre : <b>l'ancre aux yeux, dans le
 * repere du regard</b>, plus un decalage qui depend de QUI REGARDE — les nombres de
 * {@code ViewOptimize}, ecrits ici. Voir {@link #EYE_DOWN} et {@link #SEEN_DOWN} : la difference est
 * la hauteur, et c'est elle qui fait tout l'effet.
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
     * OU elle se pose : l'ancre est aux YEUX du porteur, et le decalage depend de QUI REGARDE.
     *
     * <p>C'est la convention de TOUT le mod d'origine, et le joueur a eu raison de me renvoyer
     * vers elle : les arcs de {@code arc_gen}, de la charge, du mouvement magnetique et du
     * thunder bolt sont tous des {@code EntityArc}, et son constructeur pose l'entite a
     * {@code player.posY + eyeHeight} avant de lui donner le regard du joueur — puis son rendu
     * appelle {@code ViewOptimize.fix}. Et {@code ViewOptimize} dit tout dans son propre
     * commentaire : « transforms the origin to the player's hand in thirdPerson or firstPerson ».
     * C'est cela, la main sans y etre.
     *
     * <p>Et ses trois nombres ne se lisent pas « devant, haut, droite » dans cet ordre : son repere
     * sort des deux rotations de {@code EntityArc.Renderer} — son lacet plus quatre-vingt-dix autour
     * de MOINS Y, puis son tangage autour de MOINS Z — et, en le calculant, on retrouve son <b>X
     * pour l'AVANT</b> du regard, son <b>Y pour le haut</b>, et son <b>Z pour la DROITE</b> du
     * joueur. Ses noms trahissent le contraire, et s'y fier a coute un aller-retour.
     *
     * <p>AINSI, ET C'EST LA CONVENTION QU'ON GARDE :
     *
     * <ul>
     *   <li><b>Premiere personne</b> — les nombres que le joueur a valides en jeu : 24 cm devant,
     *       10 cm a droite, 15 cm sous les yeux. Ce sont ceux de la rafale elle-meme ({@code 0.26}
     *       et {@code -0.15} de l'original, l'abscisse rapprochee a sa demande), et ils sont dans le
     *       repere du regard.</li>
     *   <li><b>Trois quarts</b> — les nombres de {@code ViewOptimize} : 15 cm devant, 23 cm a droite,
     *       <b>80 cm sous les yeux</b>. C'est le bras d'un homme vu de l'exterieur, et c'est ce qui
     *       manquait : la vue de trois quarts n'a pas la main collee au bas de l'ecran, elle a un
     *       vrai bras qui pend.</li>
     * </ul>
     *
     * <p>UNE DIFFERENCE ASSUMEE avec {@code ViewOptimize} : son decalage d'avant vaut {@code -0.05}
     * dans les deux vues, c'est-a-dire cinq centimetres EN ARRIERE des yeux. Ses arcs s'en moquent —
     * ce sont de longs rubans qui partent vers l'avant, et ce qui se voit, c'est leur longueur — mais
     * la rafale, elle, est une IMAGE posee en un point : en arriere des yeux, elle serait derriere la
     * camera et ne se verrait pas du tout. Sa composante d'avant est donc positive dans les deux vues.
     */
    private static final double EYE_FORWARD = 0.24;
    private static final double EYE_DOWN = 0.15;
    private static final double EYE_SIDE = 0.10;

    private static final double SEEN_FORWARD = 0.15;
    private static final double SEEN_DOWN = 0.80;
    private static final double SEEN_SIDE = 0.23;

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
     * <p>CHAQUE RAFALE EST POSEe par la convention de {@code ViewOptimize} — l'ancre aux yeux de
     * son porteur, le decalage du regard qui la regarde. Voir le commentaire de la classe : c'est
     * la regle de tous les arcs de l'original, et c'est ce que le joueur a demande en la nommant
     * (« la meme logique que pour l'arc gen »).
     *
     * <p>Le carre regarde la CAMERA, et c'est ce que le port ajoute a l'original : lui le dessinait
     * dans le repere du joueur, donc de profil pour qui se tenait de cote. Une etincelle qu'on ne
     * voit pas ne sert a rien.
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

            // LA MAIN SANS Y ETRE, et la regle est celle de ViewOptimize : qui regarde par ses
            // propres yeux recoit le decalage de premiere personne, tout autre regard celui de trois
            // quarts. C'est ce qui fait qu'une rafale tombe juste pour tout le monde — et pour un
            // joueur qui se regarde lui-meme, la vue decide, pas son nom.
            boolean throughMyEyes = player == Minecraft.getInstance().player
                    && Minecraft.getInstance().options.getCameraType().isFirstPerson();
            Vec3 at = handOf(player, partialTick, throughMyEyes);
            float scale = throughMyEyes ? SCALE_HAND : SCALE_WORLD;

            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            faceCamera(pose, camera.subtract(at));
            pose.scale(scale, scale, 1f);
            quad(buffers.getBuffer(type(FRAMES[frame])), pose.last().pose());
            pose.popPose();
            drawn = true;
        }

        if (drawn) buffers.endBatch();
    }

    /**
     * Le point de la "main" d'un porteur : ses YEUX, plus le decalage du regard qui regarde.
     *
     * <p>Le repere est celui de son regard — l'avant est sa visee, le haut son haut, la droite sa
     * droite — et c'est le repere que {@code ViewOptimize} decalait, celui de tous les arcs de
     * l'original. Le passage de ses trois nombres aux trois d'ici est explique sur
     * {@link #EYE_SIDE} : son X est l'avant, et son Z la droite.
     */
    private static Vec3 handOf(Player player, float partialTick, boolean throughMyEyes) {
        Vec3 look = player.getViewVector(partialTick).normalize();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        Vec3 up = right.cross(look).normalize();

        double forward = throughMyEyes ? EYE_FORWARD : SEEN_FORWARD;
        double down = throughMyEyes ? EYE_DOWN : SEEN_DOWN;
        double side = throughMyEyes ? EYE_SIDE : SEEN_SIDE;

        return player.getEyePosition(partialTick)
                .add(look.scale(forward)).add(right.scale(side)).add(up.scale(-down));
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
