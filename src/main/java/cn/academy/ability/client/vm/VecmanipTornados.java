package cn.academy.ability.client.vm;

import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.vecmanip.PlasmaCannonSkill;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Les tornades vivantes de vecmanip, chez le client : les ailes de tempete et la colonne du canon.
 *
 * <p>Chez l'original, chacune etait une <b>entite cliente</b> que le contexte de la competence
 * posait chez son porteur. Le port n'a pas d'entites d'effet : elles vivent ici, et
 * {@code TornadoRenderer} les dessine.
 *
 * <h2>Ce qui les fait vivre et mourir</h2>
 *
 * <p>Les deux se lisent sur un <b>maintien</b>, et aucune ne demande de message au serveur — le
 * client sait deja tout ce qu'il faut : quelle competence il tient, depuis combien de ticks, et ou
 * se trouve son porteur.
 *
 * <ul>
 *   <li>Les <b>ailes</b> montent avec la charge ({@code chargeTime} les ouvre), se tiennent tant
 *       que la touche est enfoncee, puis s'effacent en quinze ticks.</li>
 *   <li>La <b>colonne</b> se dresse en vingt ticks sous la boule du canon, pendant sa charge
 *       seulement : des que la boule part, elle meurt — comme l'original, dont la colonne mourait
 *       des que son canon passait a l'etat de tir.</li>
 * </ul>
 *
 * <p>Et la colonne se pose <b>au sol</b>, vingt blocs sous la boule : c'est l'original, qui
 * descendait son regard jusqu'a trouver un bloc et y plantait sa tornade. Un canon charge au-dessus
 * du vide la laisse donc flotter dans l'air, vingt blocs plus bas.
 */
public final class VecmanipTornados {

    /** Le tirage des formes : une tornade se tire une fois, a sa naissance. */
    private static final RandomSource RANDOM = RandomSource.create();

    /** Une tornade posee dans le monde : son repere, sa forme, et son opacite du moment. */
    public static final class Live {

        private final TornadoVisuals.Layout layout;
        private Vec3 position;
        private float yaw;
        private float pitch;
        private Player following;
        private double alpha;
        private boolean dying;
        private int fadeTick;
        private int age;

        private Live(TornadoVisuals.Layout layout, Vec3 position, float yaw, float pitch) {
            this.layout = layout;
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public TornadoVisuals.Layout layout() {
            return layout;
        }

        public Vec3 position() {
            return position;
        }

        public float yaw() {
            return yaw;
        }

        public float pitch() {
            return pitch;
        }

        /**
         * Le porteur que cette tornade suit, ou rien si elle est posee.
         *
         * <p>Les ailes suivent leur porteur, et le rendu les relit a chaque image avec les
         * valeurs <b>interpolees</b> : sans cela, elles resteraient au tick precedent et
         * traineraient derriere le joueur des qu'il tourne la tete.
         */
        public Player following() {
            return following;
        }

        /** Son opacite, avant le facteur de dessin : voir {@code TornadoVisuals.DRAW_ALPHA}. */
        public double alpha() {
            return alpha;
        }

        /** Son age, en ticks, depuis sa naissance. */
        public int age() {
            return age;
        }
    }

    private static Live wings;
    private static Live cannon;

    private VecmanipTornados() {
    }

    /** Un tick de maintien : c'est ici que les deux tornades naissent et suivent leur porteur. */
    public static void tickHeld(Player player, cn.academy.ability.Skill skill, int ticks) {
        if (skill == VecmanipCategory.STORM_WING) {
            float chargeTime = VecmanipCategory.STORM_WING.chargeTime(ClientAbilityData.get());
            if (wings == null) {
                wings = new Live(TornadoVisuals.wings(RANDOM), player.position(),
                        player.getYRot(), player.getXRot());
            }
            wings.dying = false;
            wings.fadeTick = 0;
            wings.following = player;
            wings.position = player.position().add(0, TornadoVisuals.SHOULDERS, 0);
            // L'orientation du CORPS, et non celle de la tete : les ailes tiennent au dos, donc
            // elles tournent avec le personnage. Le port suivait la visee, et le joueur a vu ce
            // que cela donne — regarder tout droit avec le corps en travers fait trainer les
            // ailes, comme si elles ne suivaient pas.
            wings.yaw = player.yBodyRot;
            wings.pitch = player.getXRot();
            // Le chargeur de l'original ne comptait pas de la meme facon que celui du port, mais
            // la montree se lit sur le meme nombre : les ticks du maintien en cours.
            wings.alpha = ticks <= chargeTime
                    ? TornadoVisuals.wingsChargeAlpha(ticks, chargeTime)
                    : TornadoVisuals.WINGS_ALPHA;
            // Et la poussiere : douze grains par tick, tant que le maintien est ouvert. Elle leur
            // survit — un grain vit sa vie jusqu'au bout, comme chez l'original. Voir WingDust.
            //
            // Elle est semee ICI, et non apres le retour plus bas : la branche des ailes sort de
            // la methode, et le port avait pose le semeur juste derriere — il ne semait donc
            // jamais rien.
            WingDust.spawn(player);
            return;
        }

        if (skill == VecmanipCategory.PLASMA_CANNON) {
            // La colonne vit tant que le maintien est ouvert, et pas un tick de moins : elle
            // s'efface au relachement, quand la boule part. La borner a la duree de charge la
            // faisait disparaitre sous les yeux du joueur pendant qu'il chargeait encore.
            if (cannon == null) {
                cannon = new Live(TornadoVisuals.cannon(RANDOM),
                        ground(player, player.position()
                                .add(0, PlasmaCannonSkill.START_HEIGHT, 0)),
                        0, 0);
            }
            cannon.alpha = TornadoVisuals.cannonRiseAlpha(cannon.age);
        }
    }

    /** La fin d'un maintien : ce qui vivait s'efface, et ne se rallume plus. */
    public static void end(cn.academy.ability.Skill skill) {
        if (skill == VecmanipCategory.STORM_WING && wings != null) {
            wings.dying = true;
            wings.fadeTick = 0;
        }
        if (skill == VecmanipCategory.PLASMA_CANNON && cannon != null) {
            cannon.dying = true;
        }
    }

    /**
     * Un tick du client : les effacements avancent, et les mortes s'en vont.
     *
     * <p>C'est aussi ce qui repare un maintien interrompu sans relachement — un monde quitte, une
     * competence changee : les deux tornades s'effacent au lieu de rester plantees la.
     */
    public static void tick() {
        if (wings != null) {
            wings.age++;
            if (wings.dying) {
                wings.fadeTick++;
                if (wings.fadeTick >= TornadoVisuals.WINGS_FADE_TICKS) {
                    wings = null;
                } else {
                    wings.alpha = TornadoVisuals.wingsFadeAlpha(wings.fadeTick);
                }
            }
        }
        if (cannon != null) {
            cannon.age++;
            if (cannon.dying) {
                cannon.fadeTick++;
                if (cannon.fadeTick >= TornadoVisuals.CANNON_DEATH_TICKS) {
                    cannon = null;
                } else {
                    cannon.alpha = TornadoVisuals.cannonFadeAlpha(cannon.fadeTick);
                }
            }
        }
    }

    /** Ce qu'il y a a dessiner, cette image. */
    public static List<Live> live() {
        List<Live> out = new ArrayList<>(2);
        if (wings != null && wings.alpha > 0) out.add(wings);
        if (cannon != null && cannon.alpha > 0) out.add(cannon);
        return out;
    }

    /** Tout oublier : la deconnexion d'un monde n'est pas une fin de competence. */
    public static void clear() {
        wings = null;
        cannon = null;
    }

    /** Le sol sous un point, ou vingt blocs plus bas : le point de pose de la colonne. */
    private static Vec3 ground(Player player, Vec3 from) {
        Vec3 down = from.subtract(0, 20, 0);
        var hit = player.level().clip(new ClipContext(from, down, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? down : hit.getLocation();
    }
}
