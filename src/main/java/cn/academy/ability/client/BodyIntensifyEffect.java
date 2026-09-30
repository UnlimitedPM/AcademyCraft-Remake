package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.client.arc.SurroundArcs;
import cn.academy.ability.electromaster.ElectromasterCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Le renfort du corps, cote client : l'onde d'arcs qui grimpe le long du personnage, et
 * l'electricite qui couvre l'ecran tant qu'on charge.
 *
 * <p>Deux choses, portees de deux endroits de l'original, et qu'il faut tenir ensemble parce
 * qu'elles vivent dans le meme geste.
 *
 * <h2>L'onde, autour du corps</h2>
 *
 * <p>Portage d'{@code EntityIntensifyEffect}. L'original semait son gresillement sur SEPT
 * hauteurs — 2, 1,8, 1,5, 1, 0,5, 0 et moins 0,1 bloc au-dessus des pieds — en <b>descendant</b> :
 * une hauteur apres 0, 1, 3, 4, 6, 7 et 8 ticks, de la tete vers le sol. Chacune portait trois
 * arcs — deux ici, pour la raison dite a {@link #ARCS_PER_HEIGHT} — poses sur un <b>anneau</b>
 * de 0,5 a 0,6 bloc de rayon, et chacun vivait trois ticks. Le tout etait fini en quinze : une
 * onde qui descend, pas une nappe.
 *
 * <p>Elle ne se joue qu'au RELACHEMENT, et le serveur l'annonce par {@code BodyIntensifyPacket} :
 * les hauteurs, les arcs et leurs delais sont une affaire d'image, et c'est ce crochet-ci qui les
 * rejoue, en relisant la position du joueur au moment de chaque hauteur — les anneaux suivent
 * donc le corps, comme l'entite cliente de l'original.
 *
 * <h2>L'electricite, sur l'ecran</h2>
 *
 * <p>Portage de {@code CurrentChargingHUD}. Tant que la touche est tenue, l'original couvrait
 * l'ecran de son voile bleu et y faisait scintiller cinq ou six <b>arcs d'ecran</b> — de simples
 * images d'arc, posees sur un anneau de 84 a 96 pour cent du demi-ecran, chacune changeant
 * d'image et s'allumant toute seule. Au relachement il les remplacait par une gerbe de dix a
 * quatorze, plus larges et plus grosses, et laissait tout s'effacer en deux dixiemes de seconde.
 *
 * <p>Ces arcs-la ne viennent pas du serveur : ils n'existent que chez le joueur qui tient la
 * touche, et c'est le client qui sait qu'il la tient. Voir {@link #tickCharge} et
 * {@link #endCharge}, appeles par le meme crochet que les autres effets de charge.
 */
@OnlyIn(Dist.CLIENT)
public final class BodyIntensifyEffect {

    // --- L'ONDE, AUTOUR DU CORPS ---

    /** Les sept hauteurs de l'original, en blocs au-dessus des pieds. */
    public static final double[] HEIGHTS = { 2.0, 1.8, 1.5, 1.0, 0.5, 0.0, -0.1 };

    /** Le tick de chacune : l'onde descend, de la tete vers le sol. */
    public static final int[] DELAYS = { 0, 1, 3, 4, 6, 7, 8 };

    /** L'anneau du corps : de 0,5 a 0,6 bloc, soit juste au-dela de ses 0,3 de rayon. */
    public static final double RING_MIN = 0.5;
    public static final double RING_MAX = 0.6;

    /**
     * Les arcs d'une hauteur : deux.
     *
     * <p>L'original en semait trois — son {@code RandUtils.rangei(3, 4)} exclut le 4, donc
     * toujours trois — et le port en semait trois ou quatre, un de trop. Mais le compte n'est
     * pas toute l'histoire : ses arcs <b>naissent invisibles</b>. Son {@code SubArc} part avec
     * {@code draw = false} et ne se montre qu'a une chance sur cinq par tick, si bien qu'un arc
     * qui ne vit que trois ticks ne se voit guere plus d'un tick sur trois. Les notres naissent
     * visibles et se montrent deux ticks sur trois : trois par anneau se liraient donc comme six
     * chez lui, et le joueur l'a vu tout de suite — « ils sont trop nombreux ». Deux rendent son
     * grain. Cela vaut aussi pour les anneaux des pieds, qui plongeaient dans le sol plus
     * souvent que chez lui.
     */
    public static final int ARCS_PER_HEIGHT = 2;

    /** La vie de l'entite de l'original : quinze ticks, soit trois quarts de seconde. */
    public static final int LIFE_TICKS = 15;

    // --- L'ECRAN ---

    /** Les dix images d'arcs de l'original, telles quelles : chaque arc en tire une. */
    private static final ResourceLocation[] ARC_FRAMES = frames();

    /** La bande de la charge : de 84 a 96 pour cent du demi-ecran, et des images de 25 a 30. */
    private static final double CHARGE_RING_MIN = 0.84;
    private static final double CHARGE_RING_MAX = 0.96;
    private static final double CHARGE_SIZE_MIN = 25.0;
    private static final double CHARGE_SIZE_MAX = 30.0;
    private static final int CHARGE_COUNT_MIN = 5;
    private static final int CHARGE_COUNT_MAX = 6;

    /** La gerbe du relachement : plus large, plus grosse, et plus nombreuse. */
    private static final double BURST_RING_MIN = 0.6;
    private static final double BURST_RING_MAX = 1.0;
    private static final double BURST_SIZE_MIN = 35.0;
    private static final double BURST_SIZE_MAX = 40.0;
    private static final int BURST_COUNT_MIN = 10;
    private static final int BURST_COUNT_MAX = 14;

    /** La vie d'un arc d'ecran : vingt-cinq ticks pour la gerbe, sans fin pour la charge. */
    private static final int BURST_LIFE = 25;
    private static final int CHARGE_LIFE = Integer.MAX_VALUE;

    /** Le scintillement : une chance sur deux fois le frameRate de changer d'image. */
    private static final double FRAME_RATE = 0.3;

    /** Et l'allumage : la charge ne clignote pas, la gerbe si. */
    private static final double CHARGE_SWITCH_RATE = 0.0;
    private static final double BURST_SWITCH_RATE = 0.2;

    /** Le voile : entree en matiere en une demi-seconde, sortie en deux dixiemes. */
    private static final long BLEND_IN_MILLIS = 500L;
    private static final long BLEND_OUT_MILLIS = 200L;

    /** L'opacite des arcs d'ecran : 0,3 pendant la charge, 0,4 pendant la sortie. */
    private static final float CHARGE_ALPHA = 0.3f;
    private static final float BURST_ALPHA = 0.4f;

    private static final RandomSource RANDOM = RandomSource.create();

    /** Le porteur de l'onde, ou moins un quand rien ne se joue. */
    private static int ownerId = -1;

    /** Le tick de l'onde en cours. */
    private static int age;

    /** Les arcs d'ecran poses, et l'etat du voile. */
    private static final List<HudArc> HUD_ARCS = new ArrayList<>();
    private static boolean hudActive;
    private static boolean hudBlendingOut;
    private static long hudStart;
    private static long hudBlendStart;

    private BodyIntensifyEffect() {
    }

    // --- L'ONDE, AUTOUR DU CORPS ---

    /** Lance l'onde sur ce porteur. Appele par le paquet, et par personne d'autre. */
    public static void start(int owner) {
        ownerId = owner;
        age = 0;
    }

    /**
     * Un tick : l'onde du corps, et le vieillissement des arcs d'ecran.
     *
     * <p>Le porteur de l'onde est relu a chaque tick, et non garde de cote : c'est ce qui fait
     * suivre les anneaux quand le joueur avance. Un porteur qui disparait — deconnecte, sorti de
     * la portee — arrete l'onde, sans quoi elle resterait ouverte a jamais.
     */
    public static void tick() {
        tickBody();
        tickHud();
    }

    private static void tickBody() {
        if (ownerId < 0) return;

        Entity owner = owner();
        if (owner == null) {
            ownerId = -1;
            return;
        }

        for (int i = 0; i < HEIGHTS.length; i++) {
            if (DELAYS[i] == age) {
                sow(owner, HEIGHTS[i]);
            }
        }

        if (++age > LIFE_TICKS) {
            ownerId = -1;
        }
    }

    /** Une hauteur de l'onde : deux arcs sur son anneau. */
    private static void sow(Entity owner, double height) {
        Vec3 base = owner.position();
        List<Vec3> points = new ArrayList<>(ARCS_PER_HEIGHT);

        for (int i = 0; i < ARCS_PER_HEIGHT; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2.0;
            double radius = RING_MIN + RANDOM.nextDouble() * (RING_MAX - RING_MIN);
            points.add(base.add(Math.sin(theta) * radius, height, Math.cos(theta) * radius));
        }

        SurroundArcs.spawnAt(SurroundArcs.THIN, points, ownerId, RANDOM);
    }

    /** Le porteur, s'il est dans le monde du client. */
    private static Entity owner() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : minecraft.level.getEntity(ownerId);
    }

    // --- L'ECRAN ---

    /**
     * Un tick de charge : le voile s'ouvre, et les arcs se posent.
     *
     * <p>Appele tant que la touche est tenue, et seulement pour cette competence : c'est le meme
     * crochet que celui du claquement d'orage.
     */
    public static void tickCharge(Skill skill) {
        if (skill != ElectromasterCategory.BODY_INTENSIFY) return;
        if (hudActive) return;

        hudActive = true;
        hudBlendingOut = false;
        hudStart = Util.getMillis();
        hudBlendStart = 0L;
        HUD_ARCS.clear();
        sowHud(false);
    }

    /**
     * Le relachement : le voile s'efface, et la gerbe prend sa place si le renfort a pris.
     *
     * <p>Une charge trop courte ne merite rien du tout — le serveur ne declenche rien — mais le
     * voile, lui, s'efface de la meme facon : c'est ce que faisait l'original a l'abandon.
     */
    public static void endCharge(boolean performed) {
        if (!hudActive || hudBlendingOut) return;

        hudBlendingOut = true;
        hudBlendStart = Util.getMillis();

        if (performed) {
            HUD_ARCS.clear();
            sowHud(true);
        }
    }

    /** Le vieillissement des arcs d'ecran, et la fin du voile. */
    private static void tickHud() {
        for (int i = HUD_ARCS.size() - 1; i >= 0; i--) {
            HUD_ARCS.get(i).tick();
        }

        if (hudActive && hudBlendingOut
                && Util.getMillis() - hudBlendStart > BLEND_OUT_MILLIS) {
            hudActive = false;
            hudBlendingOut = false;
            HUD_ARCS.clear();
        }
    }

    /** Pose une bande d'arcs d'ecran : celle de la charge, ou celle de la gerbe. */
    private static void sowHud(boolean burst) {
        int count = burst
                ? BURST_COUNT_MIN + RANDOM.nextInt(BURST_COUNT_MAX - BURST_COUNT_MIN + 1)
                : CHARGE_COUNT_MIN + RANDOM.nextInt(CHARGE_COUNT_MAX - CHARGE_COUNT_MIN + 1);
        double ringMin = burst ? BURST_RING_MIN : CHARGE_RING_MIN;
        double ringMax = burst ? BURST_RING_MAX : CHARGE_RING_MAX;
        double sizeMin = burst ? BURST_SIZE_MIN : CHARGE_SIZE_MIN;
        double sizeMax = burst ? BURST_SIZE_MAX : CHARGE_SIZE_MAX;
        int life = burst ? BURST_LIFE : CHARGE_LIFE;
        double switchRate = burst ? BURST_SWITCH_RATE : CHARGE_SWITCH_RATE;

        for (int i = 0; i < count; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2.0;
            double phi = ringMin + RANDOM.nextDouble() * (ringMax - ringMin);
            double size = sizeMin + RANDOM.nextDouble() * (sizeMax - sizeMin);
            HUD_ARCS.add(new HudArc(phi * Math.sin(theta), phi * Math.cos(theta), size, life,
                    switchRate));
        }
    }

    /** L'opacite du voile bleu, de zero a un en une demi-seconde, puis l'inverse en 0,2 s. */
    public static float maskAlpha() {
        if (!hudActive) return 0f;

        long now = Util.getMillis();
        if (!hudBlendingOut) {
            return Math.min(1f, (now - hudStart) / (float) BLEND_IN_MILLIS);
        }
        return Math.max(0f, 1f - (now - hudBlendStart) / (float) BLEND_OUT_MILLIS);
    }

    /** L'opacite des arcs d'ecran, et rien du tout quand il n'y en a plus. */
    public static float arcAlpha() {
        if (!hudActive) return 0f;
        return hudBlendingOut ? BURST_ALPHA : CHARGE_ALPHA;
    }

    /** Les arcs d'ecran poses, que le HUD dessine. */
    public static List<HudArc> hudArcs() {
        return HUD_ARCS;
    }

    private static ResourceLocation[] frames() {
        ResourceLocation[] frames = new ResourceLocation[10];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath("academy",
                    "textures/effects/arcs/" + i + ".png");
        }
        return frames;
    }

    /**
     * Un arc d'ecran : une image posee sur un anneau, qui scintille.
     *
     * <p>Portage de {@code SubArc2D}. C'est une simple image, et deux tirages par tick font tout
     * son mouvement : une chance sur deux fois son frameRate de changer d'image, et son
     * switchRate qui l'allume et l'eteint.
     *
     * <p>Sa position est en unites de DEMI-ECRAN — comme chez lui, qui multipliait son x et son y
     * par la moitie de l'ecran — donc de moins un a un. Son cote, lui, est en pixels.
     */
    public static final class HudArc {

        private final double x;
        private final double y;
        private final double size;
        private final int life;
        private final double switchRate;

        private int frame = RANDOM.nextInt(ARC_FRAMES.length);
        private int ticks;
        private boolean lit = true;
        private boolean dead;

        HudArc(double x, double y, double size, int life, double switchRate) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.life = life;
            this.switchRate = switchRate;
        }

        void tick() {
            if (RANDOM.nextDouble() < 0.5 * FRAME_RATE) {
                frame = RANDOM.nextInt(ARC_FRAMES.length);
            }
            if (RANDOM.nextDouble() < 0.9) {
                ticks++;
            }
            if (ticks >= life) {
                dead = true;
            }

            if (lit) {
                if (RANDOM.nextDouble() < 0.4 * switchRate) lit = false;
            } else if (RANDOM.nextDouble() < 0.3 * switchRate) {
                lit = true;
            }
        }

        /** S'il y a quelque chose a dessiner de cet arc, a cette image-ci. */
        public boolean visible() {
            return !dead && lit;
        }

        public ResourceLocation texture() {
            return ARC_FRAMES[frame];
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double size() {
            return size;
        }
    }
}
