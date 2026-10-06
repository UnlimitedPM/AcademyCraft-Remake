package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import cn.academy.ability.Skill;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.client.ClientCharge;
import cn.academy.ability.vecmanip.VecmanipCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Le poing qui frappe, pour le choc dirige et l'onde dirigee.
 *
 * <p>L'original ne se contentait pas de faire partir une onde : <b>sa main bougeait</b>. Pendant
 * la charge elle reculait et se couchait — le joueur armait son coup —, puis au relachement elle
 * partait d'un abattage de trois dixiemes de seconde avant de revenir a sa place. C'est un
 * <b>rendu de la main en premiere personne</b>, et l'original l'obtenait en remplacant purement et
 * simplement le rendu de la main de Minecraft par le sien, avec un empilement de matrices
 * transforme au passage.
 *
 * <h2>Comment le port s'y prend, sans remplacer la main</h2>
 *
 * <p>Il n'a pas besoin d'aller si loin : Forge lui donne un crochet sur le rendu de la main
 * ({@code RenderHandEvent}), et l'empilement de poses qu'il y recoit est <b>exactement</b> celui
 * dans lequel Minecraft dessine la main — le meme repere que l'identite de l'original, ou X va
 * vers la droite, Y vers le haut et Z vers le joueur. Le port se contente donc d'y appliquer le
 * deplacement et les trois rotations du geste, puis laisse Minecraft dessiner la main dedans :
 * meme rendu, meme lumiere, aucun remplacement, et rien d'autre touche.
 *
 * <p>Deux details de cet empilement. Les <b>deux mains</b> s'y succedent et le partagent : le port
 * n'applique donc le geste qu'une fois, sur la main principale, et la seconde en herite — c'est ce
 * que faisait l'original, dont la transformation enveloppait les deux. Et l'empilement est
 * reinitialise a chaque image par le jeu, donc la retouche ne deborde jamais sur l'image suivante.
 *
 * <h2>Quand le geste part, et pourquoi ce n'est pas exactement l'original</h2>
 *
 * <p>Trois instants, trois signaux. Le geste <b>s'arme</b> a l'appui, quand la touche ouvre sa
 * charge — le client le sait tout seul. Il <b>part</b> au relachement, quand la charge a passe son
 * minimum. Et il <b>rentre</b> tout seul, six ticks plus tard, ou des que la charge n'est plus
 * celle qui l'armait.
 *
 * <p>L'original, lui, ne donnait le coup que sur un <b>message du serveur</b> : le choc dirige ne
 * frappait que s'il avait trouve quelque chose, et un poing dans le vide ne bougeait pas. Le port
 * ne le suit pas la-dessus, faute d'un tel message — le serveur ne dit rien au client quand le
 * coup trouve ou ne trouve pas. Le geste part donc au relachement, et rien ne le retient : c'est
 * l'approximation que le port a deja acceptee pour le piquage de la visee du choc au sol, qui
 * obeit au meme genre de signal. Une difference de plus la separe de l'original : sa main
 * s'armait <b>meme quand le coup ne partait pas</b> (charge trop courte), le port ne l'arme que
 * quand la charge est ouverte pour de bon, puisque c'est le meme chemin que sa barre de charge.
 *
 * <p>Rien de tout cela n'a de sens hors du client : la classe ne se charge que la.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class HandSwing {

    /** Les trois temps du geste, et l'absence de geste. */
    private enum Phase { NONE, PREPARE, PUNCH }

    private static Phase phase = Phase.NONE;

    /** La competence qui a arme le geste, ou {@code null}. */
    private static Skill held;

    /** L'age du coup, en ticks. */
    private static int punchTicks;

    private HandSwing() {
    }

    /** Vrai pour les deux competences qui partagent ce geste : voir {@link HandAnim}. */
    private static boolean handles(Skill candidate) {
        return candidate == VecmanipCategory.DIRECTED_SHOCK
                || candidate == VecmanipCategory.DIRECTED_BLASTWAVE;
    }

    /** L'appui : le poing s'arme. Les autres competences n'y touchent pas. */
    public static void begin(Skill skill) {
        if (!handles(skill)) return;
        phase = Phase.PREPARE;
        held = skill;
        punchTicks = 0;
    }

    /** Le relachement : le poing part. */
    public static void punch(Skill skill) {
        if (!handles(skill)) return;
        phase = Phase.PUNCH;
        held = skill;
        punchTicks = 0;
    }

    /**
     * La charge refermee : le poing ne reste pas arme.
     *
     * <p>Un coup en cours de route <b>n'est pas</b> interrompu : c'est ce qui le laisse aller au
     * bout meme quand le relachement qui l'a lance a deja referme la charge.
     */
    public static void end(Skill skill) {
        if (held != skill) return;
        if (phase == Phase.PREPARE) clear();
    }

    /** Tout oublier : le monde change, et plus rien ne s'anime dans celui d'avant. */
    public static void clear() {
        phase = Phase.NONE;
        held = null;
        punchTicks = 0;
    }

    /** Un tick : le coup avance, et une charge abandonnee desarme le poing. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (phase == Phase.PUNCH) {
            if (++punchTicks >= HandAnim.PUNCH_TICKS) clear();
            return;
        }
        if (phase == Phase.PREPARE && detached()) clear();
    }

    /**
     * Vrai quand la charge qui armait le poing n'est plus ouverte.
     *
     * <p>Deux cas, et le second est une correction du port : la charge de CETTE competence s'est
     * refermee — le relachement, un maintien que le serveur a termine — ou bien son age a depasse
     * la fenetre de la sienne : le serveur la ferme a ce moment-la, et le client, lui, ne l'apprend
     * pas. Sans ce second cas, un joueur qui tient sa touche trop longtemps garderait le poing en
     * l'air indefiniment.
     *
     * <p>Et c'est bien SA charge qui est lue, pas « la charge en cours » : le port comparait le nom
     * du poing a celui de la derniere charge ouverte, donc ouvrir une seconde competence pendant
     * que le poing s'armait — preparer un `dir_blast` puis lancer un `vec_accel` — desarmait le
     * poing et annulait son geste. Le joueur : « si je prepare un dir blast avant de faire le vec
     * accel, l'animation est annulee et ca cause pas mal de bug ».
     */
    private static boolean detached() {
        if (held == null) return true;
        if (!ClientCharge.isOpen(held.getName())) return true;
        int max = held.getMaxChargeTicks(ClientAbilityData.get());
        return max > 0 && ClientCharge.getTicks(held.getName()) >= max;
    }

    /** Le geste, applique a l'empilement de poses de la main. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (phase == Phase.NONE) return;
        // La main principale d'abord, et une seule fois : la main secondaire passe par le meme
        // evenement et le meme empilement, donc elle herite du geste sans qu'on le lui applique.
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        boolean preparing = phase == Phase.PREPARE;
        double age = (preparing ? ClientCharge.getTicks(held.getName()) : punchTicks)
                + event.getPartialTick();
        HandAnim.Pose pose = (preparing ? HandAnim.PREPARE : HandAnim.PUNCH)
                .pose(preparing ? HandAnim.prepareTime(age) : HandAnim.punchTime(age));

        PoseStack stack = event.getPoseStack();
        stack.translate(pose.x(), pose.y(), pose.z());
        stack.mulPose(Axis.XP.rotationDegrees((float) pose.rx()));
        stack.mulPose(Axis.YP.rotationDegrees((float) pose.ry()));
        stack.mulPose(Axis.ZP.rotationDegrees((float) pose.rz()));
    }
}
