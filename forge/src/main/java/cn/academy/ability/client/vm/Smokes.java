package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les bouffees de fumee du choc au sol, portage de {@code SmokeEffect}.
 *
 * <p>C'est l'effet de l'original, au chiffre pres : une <b>bouffee de deux blocs</b> — ses quads
 * vont de -1 a 1, echelle comprise — posee sur la moitie des blocs casses, qui monte doucement et
 * s'efface en une seconde et demie. Le port s'en etait d'abord passe, en semant deux poussieres de
 * fumee de vanilla : le joueur a refuse l'echange, et il avait raison — ce n'est pas la meme fumee,
 * donc il n'y avait rien a comparer.
 *
 * <h2>Sa vie, en trois temps</h2>
 *
 * <p>Elle vit deux fois : une fois pour sa <b>bouffee</b>, une fois pour son <b>opacite</b>. La
 * bouffee dure quatre secondes, toujours. L'opacite, elle, se lit sur un temps <b>etire</b> — un
 * modificateur tire entre 0,5 et 0,7 — donc elle monte en trois dixiemes de ce temps, se tient
 * jusqu'a un et demi, et s'efface jusqu'a deux. Passe ce temps-la, la bouffee est encore la, mais
 * invisible ; c'est l'original, et c'est ce qui fait qu'une grande bouffee dure plus longtemps
 * qu'une petite.
 *
 * <p>Quatre images, tirees a la naissance : l'atlas en porte un carre de deux sur deux, et une
 * bouffee garde la sienne toute sa vie.
 */
@OnlyIn(Dist.CLIENT)
public final class Smokes {

    /** L'echelle de la bouffee : ses quads vont de -1 a 1, donc deux blocs. */
    public static final double SIZE = 1.0;

    /** L'atlas est un carre de deux images sur deux. */
    public static final int FRAMES = 4;

    /** Sa vie : quatre secondes, et un modificateur d'opacite tire entre 0,5 et 0,7. */
    public static final double LIFE_SECONDS = 4.0;
    public static final double LIFE_MODIFIER_MIN = 0.5;
    public static final double LIFE_MODIFIER_MAX = 0.7;

    /** Les seuils de l'opacite, en multiples du modificateur : montee, tenue, effacement. */
    public static final double FADE_IN = 0.3;
    public static final double HOLD = 1.5;
    public static final double FADE_OUT = 2.0;

    /** Ce qu'elle prend en naissant : une derive de trois centimetres, et une montee de trois a six. */
    public static final double DRIFT = 0.03;
    public static final double RISE_MIN = 0.03;
    public static final double RISE_MAX = 0.06;

    /** Et le flou de sa naissance : trente centimetres autour du centre, vingt au-dessus. */
    public static final double SPREAD = 0.3;
    public static final double TOP = 0.2;

    /** Son image : la fumee de l'original, telle quelle. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AcademyCraft.MOD_ID, "textures/effects/smokes.png");

    /** Une bouffee vivante. */
    public static final class Puff {

        private final double[] prev;
        private final double[] pos;
        private final double[] vel;
        private final int frame;
        private final double lifeModifier;
        private int ageTicks;

        Puff(double[] pos, double[] vel, int frame, double lifeModifier) {
            this.prev = pos.clone();
            this.pos = pos;
            this.vel = vel;
            this.frame = frame;
            this.lifeModifier = lifeModifier;
        }

        /** Sa colonne dans l'atlas. */
        public int frame() {
            return frame;
        }

        /**
         * Ou elle est, vue par une image.
         *
         * <p>Elle avance au tick et se dessine a chaque image : sans cet entre-deux, ses trois
         * centimetres de derive se feraient par bonds, et cela se verrait.
         */
        public Vec3 at(float partialTick) {
            return new Vec3(
                    prev[0] + (pos[0] - prev[0]) * partialTick,
                    prev[1] + (pos[1] - prev[1]) * partialTick,
                    prev[2] + (pos[2] - prev[2]) * partialTick);
        }

        /** Son age en secondes, vu par une image. */
        public double ageSeconds(float partialTick) {
            return (ageTicks + partialTick) / 20.0;
        }

        /**
         * Son opacite a cette image-la : elle monte, se tient, puis s'efface.
         *
         * <p>Elle se lit sur le temps <b>etire</b> par le modificateur, donc une grande bouffee
         * dure plus longtemps qu'une petite — c'est l'original, et cela se voit : les plus grosses
         * trainent apres les autres.
         */
        public float alpha(float partialTick) {
            double stretched = ageSeconds(partialTick) / lifeModifier;
            if (stretched <= FADE_IN) {
                return (float) (stretched / FADE_IN);
            }
            if (stretched <= HOLD) {
                return 1f;
            }
            if (stretched <= FADE_OUT) {
                return (float) (1 - (stretched - HOLD) / (FADE_OUT - HOLD));
            }
            return 0f;
        }

        /** Vrai quand la bouffee a fini de vivre : quatre secondes, opacite comprise. */
        boolean dead() {
            return ageSeconds(0) >= LIFE_SECONDS;
        }

        /** Un tick : elle avance de sa vitesse, sans poids — l'original n'en avait pas. */
        void advance() {
            System.arraycopy(pos, 0, prev, 0, 3);
            pos[0] += vel[0];
            pos[1] += vel[1];
            pos[2] += vel[2];
            ageTicks++;
        }
    }

    private static final List<Puff> LIVE = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private Smokes() {
    }

    /** Les bouffees posees. */
    public static List<Puff> live() {
        return LIVE;
    }

    /** Tout effacer : le monde change, et rien de ce qui fumait n'y fume plus. */
    public static void clear() {
        LIVE.clear();
    }

    /** La colonne d'une image dans l'atlas : deux images par ligne. */
    public static float frameU(int frame) {
        return (frame % 2) / 2f;
    }

    /** Et sa ligne. */
    public static float frameV(int frame) {
        return (frame / 2) / 2f;
    }

    /**
     * Allume une bouffee a l'endroit annonce.
     *
     * <p>Tout le reste se tire ici, et non chez le serveur : la vitesse, l'image, le modificateur de
     * vie — et le flou de la naissance, trente centimetres autour du point annonce. L'original les
     * tirait dans l'entite de la bouffee, chez le client ; le port fait pareil, et le serveur n'a
     * donc a dire que <b>ou</b> — le dessus du bloc casse.
     */
    public static Puff puff(Vec3 where) {
        double[] pos = {
                where.x + (RANDOM.nextDouble() * 2 - 1) * SPREAD,
                where.y + RANDOM.nextDouble() * TOP,
                where.z + (RANDOM.nextDouble() * 2 - 1) * SPREAD };
        double[] vel = {
                (RANDOM.nextDouble() * 2 - 1) * DRIFT,
                RISE_MIN + RANDOM.nextDouble() * (RISE_MAX - RISE_MIN),
                (RANDOM.nextDouble() * 2 - 1) * DRIFT };
        Puff puff = new Puff(pos, vel, RANDOM.nextInt(FRAMES),
                LIFE_MODIFIER_MIN + RANDOM.nextDouble() * (LIFE_MODIFIER_MAX - LIFE_MODIFIER_MIN));
        LIVE.add(puff);
        return puff;
    }

    /** Un tick : les bouffees montent, et celles qui ont fini s'en vont. */
    public static void tick() {
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            Puff puff = LIVE.get(i);
            if (puff.dead()) {
                LIVE.remove(i);
            } else {
                puff.advance();
            }
        }
    }
}
