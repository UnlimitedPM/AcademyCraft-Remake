package cn.academy.ability.client.arc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les eclairs vivants du client.
 *
 * <p>L'original faisait de chaque eclair une <b>entite</b>, envoyee au client avec un
 * message quand la competence partait. Le port n'a pas d'entites d'effet — ses trois
 * rendus de competence lisent un etat — donc les eclairs vivent ici, dans une liste :
 * on les ouvre avec leur duree de vie, le client les fait scintiller a chaque tick, et ils
 * disparaissent tout seuls a la fin. Le rendu les lit ensuite.
 *
 * <p>Aucun type de Minecraft ici, expres : les bouts sont des coordonnees, la fin est un
 * numero de tick du monde, et le hasard est donne par l'appelant. Toute la duree de vie
 * d'un eclair se relit donc en test, sans lancer un jeu.
 */
public final class ClientArcs {

    /**
     * Un eclair vivant.
     *
     * <p>Sa forme est relue a chaque changement de variante, et gardee entre-temps : la
     * decouper a chaque image allouerait des centaines d'objets soixante fois par seconde,
     * pour un motif qui ne change que quelques fois par seconde.
     */
    public static final class LiveArc {

        private final ArcPattern pattern;
        private final double[] from;
        private final double[] to;
        private double clip;
        private long endTick;
        private final int ownerId;
        private final ArcWiggle wiggle;

        private ArcMesh mesh;
        private int meshVariant = -1;

        LiveArc(ArcPattern pattern, double[] from, double[] to, double clip, long endTick,
                int ownerId, ArcWiggle wiggle) {
            this.pattern = pattern;
            this.from = from;
            this.to = to;
            this.clip = clip;
            this.endTick = endTick;
            this.ownerId = ownerId;
            this.wiggle = wiggle;
        }

        /** Le point de depart, en coordonnees du monde. */
        public double[] from() {
            return from;
        }

        /** Le point d'arrivee, en coordonnees du monde. */
        public double[] to() {
            return to;
        }

        /** Le tireur, tel que le serveur l'a nomme. */
        public int ownerId() {
            return ownerId;
        }

        public boolean visible() {
            return wiggle.visible();
        }

        /** Jusqu'a quel tick du monde cet eclair vit. */
        public long endTick() {
            return endTick;
        }

        /** La forme courante, coupee a la longueur demandee. */
        public ArcMesh mesh() {
            if (mesh == null || meshVariant != wiggle.variant()) {
                meshVariant = wiggle.variant();
                mesh = ArcPatterns.variant(pattern, meshVariant).clippedTo(clip);
            }
            return mesh;
        }

        void advance(Random rng) {
            wiggle.advance(rng);
        }

        /**
         * Deplace l'eclair : ses deux bouts suivent ce que le joueur vise, tout de suite.
         *
         * <p>C'est ce qu'il fallait a la charge et a la traction. L'original accrochait son
         * eclair a une entite, donc il suivait le regard par construction ; le port le posait
         * entre deux points fixes, et l'arc mettait dix ticks — une demi-seconde — a se retourner
         * quand le joueur regardait ailleurs. Le joueur l'a vu : "l'eclair met du temps avant de
         * changer de direction".
         */
        void follow(double[] newFrom, double[] newTo, long newEndTick) {
            System.arraycopy(newFrom, 0, from, 0, 3);
            System.arraycopy(newTo, 0, to, 0, 3);
            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double dz = to[2] - from[2];
            clip = Math.sqrt(dx * dx + dy * dy + dz * dz);
            endTick = newEndTick;
        }
    }

    private static final List<LiveArc> ARCS = new ArrayList<>();

    private ClientArcs() {}

    /**
     * Ouvre un eclair entre deux points du monde.
     *
     * <p>{@code lengthFixed} est le drapeau de l'original, dans le meme sens : vrai, l'eclair
     * garde toute la portee de son motif — vingt blocs — meme si les deux bouts sont plus
     * proches ; faux, il se dessine jusqu'au bout vise et pas au-dela. La genese d'arc, dont
     * l'original reglait la longueur sur la portee de la competence, est dans le second cas :
     * sans cela son arc s'arretait au premier bloc rencontre et paraissait tout petit.
     *
     * <p>{@code ownerId} est le tireur. Il ne sert pas a dessiner l'eclair, mais a savoir que
     * c'est le sien : le rendu recollera son depart sur la camera du joueur, comme le faisait
     * l'optimisation de vue de l'original. Voir {@link ArcView}.
     */
    public static void spawn(ArcPattern pattern, double[] from, double[] to, int lifeTicks,
                             boolean lengthFixed, int ownerId, long gameTime, Random rng) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double clip = lengthFixed ? pattern.length() : distance;
        ARCS.add(new LiveArc(pattern, from, to, clip, gameTime + lifeTicks, ownerId,
                new ArcWiggle(pattern, rng.nextInt(ArcPatterns.variants()))));
    }

    /**
     * Un eclair qui dure : le meme, deplace sur sa cible, tant qu'on le rafraichit.
     *
     * <p>A appeler a chaque tick d'un maintien. Il n'y en a jamais deux : le premier appel le
     * pose, les suivants le deplacent et repoussent sa fin. Quand le joueur relache, plus rien
     * ne le rafraichit et il s'eteint tout seul, comme un eclair ordinaire.
     *
     * <p>C'est pour cela qu'il n'est pas re-pose chaque tick : le re-poser donnerait un eclair
     * neuf a chaque fois, donc une nouvelle forme a chaque fois — et le joueur avait deja vu ce
     * defaut-la, trois eclairs superposes. Ici la forme continue de scintiller d'elle-meme.
     */
    public static void sustain(ArcPattern pattern, double[] from, double[] to, int lifeTicks,
                               int ownerId, long gameTime, Random rng) {
        LiveArc arc = held(pattern, ownerId);
        if (arc == null) {
            spawn(pattern, from, to, lifeTicks, false, ownerId, gameTime, rng);
        } else {
            arc.follow(from, to, gameTime + lifeTicks);
        }
    }

    /** L'eclair tenu de ce motif pour ce tireur, s'il est deja la. */
    private static LiveArc held(ArcPattern pattern, int ownerId) {
        for (int i = ARCS.size() - 1; i >= 0; i--) {
            LiveArc arc = ARCS.get(i);
            if (arc.pattern == pattern && arc.ownerId() == ownerId) return arc;
        }
        return null;
    }

    /** Un tick du client : les eclairs scintillent, et les morts s'en vont. */
    public static void tick(long gameTime, Random rng) {
        for (int i = ARCS.size() - 1; i >= 0; i--) {
            LiveArc arc = ARCS.get(i);
            if (gameTime >= arc.endTick) {
                ARCS.remove(i);
                continue;
            }
            arc.advance(rng);
        }
    }

    /** Les eclairs vivants, a dessiner. */
    public static List<LiveArc> live() {
        return ARCS;
    }

    /** Tout oublier : la deconnexion d'un monde n'est pas une fin de competence. */
    public static void clear() {
        ARCS.clear();
    }
}
