package cn.academy.ability.client.vm;

import cn.academy.AcademyCraft;
import cn.academy.ability.Skill;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Le coup de camera du choc au sol : le regard se leve pendant la charge, puis pique au coup.
 *
 * <p>L'original ne se contentait pas d'un effet au sol : il <b>bougeait la visee du joueur</b>,
 * et c'est ce qui donne au coup son poids. Deux temps, deux gestes :
 *
 * <ul>
 *   <li>pendant la <b>charge</b>, le regard monte doucement — vingt centiemes de degre par tick,
 *       montes en quatre ticks, tenus jusqu'a vingt, retombes en cinq. C'est peu, et c'est fait
 *       pour : le joueur voit qu'il arme quelque chose, sans que sa visee lui echappe ;
 *   <li>au <b>coup</b>, le regard pique de trois virgule quatre degres par tick pendant quatre
 *       ticks — le coup de bélier tombe, et la tete suit.
 * </ul>
 *
 * <p>Les deux nombres sont ceux de l'original, au chiffre pres, et les deux sens sont les siens :
 * la charge <b>monte</b> ({@code rotationPitch -= ...}, un tangage negatif etant un regard vers le
 * haut), le coup <b>descend</b> ({@code += 3.4f}). Ce qu'il en disait lui-meme :
 * « make player's look direction slash down ».
 *
 * <p>C'est une retouche de la visee du <b>client</b>, et rien d'autre : le serveur apprend la
 * nouvelle orientation au tick suivant, par le chemin ordinaire du deplacement. L'original faisait
 * exactement pareil, pour exactement le meme resultat.
 *
 * <p>Rien de tout cela n'a de sens hors du client : la classe ne se charge que la.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class GroundshockCamera {

    /** La montee de la charge : vingt centiemes de degre par tick, au plus fort. */
    public static final float PULL_DEGREES = 0.2f;

    /** Le piquage du coup : trois virgule quatre degres par tick. */
    public static final float SLASH_DEGREES = 3.4f;

    /** Et sa duree : quatre ticks. */
    public static final int SLASH_TICKS = 4;

    /** La montee tient son plein de quatre a vingt ticks, et retombe en cinq. */
    public static final int PULL_RAMP = 4;
    public static final int PULL_HOLD = 20;
    public static final int PULL_FADE = 25;

    /** Les ticks de piquage qui restent, ou zero. */
    private static int slashTicks;

    private GroundshockCamera() {
    }

    /**
     * L'ouverture de la montee, tick par tick, de zero a un.
     *
     * <p>C'est la courbe de l'original, et c'est tout ce qu'il y a a verifier sans lancer le jeu :
     * une montee douce, un palier, une retombee, puis plus rien.
     */
    public static float pull(int tick) {
        if (tick < PULL_RAMP) return tick / (float) PULL_RAMP;
        if (tick <= PULL_HOLD) return 1f;
        if (tick <= PULL_FADE) return 1f - (tick - PULL_HOLD) / (float) (PULL_FADE - PULL_HOLD);
        return 0f;
    }

    /** Un tick de charge : le regard monte. */
    public static void charge(Player player, Skill skill, int ticks) {
        if (skill != VecmanipCategory.GROUNDSHOCK) return;
        float delta = pull(ticks) * PULL_DEGREES;
        if (delta <= 0f) return;
        // Un tangage qui diminue est un regard qui MONTE : la convention n'est pas celle des
        // degres d'inclinaison, et c'est l'original qui l'ecrit ainsi.
        player.setXRot(player.getXRot() - delta);
    }

    /** Le coup porte : le regard pique, quatre ticks durant. */
    public static void slash(Skill skill) {
        if (skill != VecmanipCategory.GROUNDSHOCK) return;
        slashTicks = SLASH_TICKS;
    }

    /** Un tick d'horloge : le piquage du coup porte, s'il est en cours. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || slashTicks <= 0) return;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.setXRot(player.getXRot() + SLASH_DEGREES);
        }
        slashTicks--;
    }
}
