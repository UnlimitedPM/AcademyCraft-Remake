package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.arc.ArcPattern;
import cn.academy.ability.client.arc.ArcRenderer;
import cn.academy.ability.client.arc.SurroundArcs;
import cn.academy.ability.electromaster.ChargingSkill;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.energy.EnergyReceiver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * L'electricite de la charge, cote client : l'arc de l'oeil vers la cible, et l'essaim sur la
 * machine qui la recoit.
 *
 * <p>L'original tenait cela de son contexte client, qui vivait chez le joueur et tirait ses
 * arcs a chaque tick de maintien — c'est la seule forme de la 1.12.2 qui s'executait la. Le
 * port a le meme crochet, et c'est ici qu'il aboutit (voir {@code AbilityClientEvents}).
 *
 * <p>Rien ne passe par le serveur : ni paquet, ni entite. C'est une <b>image</b>, et le serveur
 * facture de son cote sans rien savoir d'elle — comme l'original.
 */
@OnlyIn(Dist.CLIENT)
public final class ChargingEffect {

    /**
     * La vie de l'arc de charge, en ticks.
     *
     * <p>Un seul arc a la fois, pose pour la duree de ce delai : l'original ne dessinait pas
     * plusieurs eclairs, il en dessinait <b>un</b>, dont la forme changeait au fil des images.
     * Le port faisait l'inverse — un arc par tick, vivant trois ticks — et le joueur a vu trois
     * eclairs superposes. C'est le scintillement de l'arc lui-meme qui doit donner le mouvement,
     * et il le donne : c'est le meme reglage que la genese d'arc, validee.
     *
     * <p>Dix ticks, comme la genese d'arc justement : assez long pour que le trait se lise, assez
     * court pour qu'un nouvel eclair reprenne la place aussitot.
     */
    private static final int ARC_LIFE_TICKS = 10;

    private static final RandomSource RANDOM = RandomSource.create();

    /**
     * L'essaim de la machine : le gabarit le plus fin, mais des arcs de la taille du bloc.
     *
     * <p>ECART ASSUME. Les gabarits de l'original sont tailles pour un <b>corps</b> : son arc
     * fin mesure 1,5 a 2 blocs, et autour d'un joueur cela se lit comme une etincelle. Autour
     * d'une machine d'un bloc, les memes arcs la depassent de deux fois sa taille, et le joueur
     * les a trouves trop grands — ils ressortaient de partout. Ici, donc, des arcs de 0,4 a 0,8
     * bloc : ils tiennent dans le bloc qu'ils entourent.
     */
    public static final SurroundArcs.Gabarit MACHINE_SWARM =
            new SurroundArcs.Gabarit(ArcPattern.SURROUND_THIN, 4, 0.4, 0.8);

    private ChargingEffect() {
    }

    /**
     * Un tick d'electricite, si c'est bien la charge qui tient la touche.
     *
     * <p>Aucun arc autour du joueur : l'original entourait la <b>machine</b> qui recoit, pas
     * celui qui la branche. Voir {@link #sowSwarm}.
     */
    public static void tick(Player player, Skill skill, int heldTicks) {
        if (skill != ElectromasterCategory.CHARGING) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        BlockHitResult block = TargetingUtil.findBlockInSight(player, ChargingSkill.RANGE);
        int ownerId = player.getId();
        Vec3 eye = player.getEyePosition();
        Vec3 target = block == null
                ? eye.add(player.getLookAngle().scale(ChargingSkill.RANGE))
                : block.getLocation();

        if (arcDue(heldTicks)) {
            ArcRenderer.spawn(ArcPattern.CHARGING.name(), eye, target, ARC_LIFE_TICKS, false, ownerId);
        }
        if (block != null && swarmDue(heldTicks)) {
            sowSwarm(player, block.getBlockPos(), ownerId);
        }
    }

    /**
     * L'arc se repose quand le precedent s'eteint : il n'y en a jamais deux.
     *
     * <p>Le premier tick fait exception : sans cela, le compteur du maintien partant de un, le
     * joueur brancherait sa machine et attendrait une demi-seconde avant de voir quoi que ce soit.
     */
    public static boolean arcDue(int heldTicks) {
        return heldTicks <= 1 || heldTicks % ARC_LIFE_TICKS == 0;
    }

    /** L'essaim se re-seme a chaque fois que ses arcs s'eteignent. */
    public static boolean swarmDue(int heldTicks) {
        return heldTicks <= 1 || heldTicks % SurroundArcs.LIFE_TICKS == 0;
    }

    /**
     * L'essaim autour de la machine chargee.
     *
     * <p>L'original la posait sur la position du bloc, dans une boite de sa taille meme : un
     * cube d'un bloc, centre. C'est le gabarit le plus fin des trois — le joueur avait trouve
     * ceux du port trop gros, ils l'etaient deux fois : trop epais, et autour de lui.
     *
     * <p>Il n'y a d'etincelle que sur une vraie machine : viser un mur ne fait que l'arc.
     */
    private static void sowSwarm(Player player, BlockPos pos, int ownerId) {
        BlockEntity entity = player.level().getBlockEntity(pos);
        if (!(entity instanceof EnergyReceiver)) return;

        SurroundArcs.spawn(MACHINE_SWARM, Vec3.atCenterOf(pos), 1.0, -0.5, 0.5,
                ownerId, RANDOM);
    }
}
