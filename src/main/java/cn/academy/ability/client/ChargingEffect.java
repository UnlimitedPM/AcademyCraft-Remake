package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.arc.ArcPattern;
import cn.academy.ability.client.arc.SurroundArcs;
import cn.academy.ability.client.arc.SustainedArcs;
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
     * <p>Ce rythme est celui de {@link SustainedArcs}, que la traction magnetique partage.
     */
    private static final RandomSource RANDOM = RandomSource.create();

    /**
     * L'essaim de la machine : le gabarit le plus fin, mais des arcs de la taille du bloc.
     *
     * <p>ECART ASSUME. Les gabarits de l'original sont tailles pour un <b>corps</b> : son arc
     * fin mesure 1,5 a 2 blocs, et autour d'un joueur cela se lit comme une etincelle. Autour
     * d'une machine d'un bloc, les memes arcs la depassent de deux fois sa taille, et le joueur
     * les a trouves trop grands — ils ressortaient de partout. Ici, donc, des arcs de 0,2 a 0,4
     * bloc, dessines avec le motif le plus fin du port, et huit d'un coup : quatre se lisaient a
     * peine sur un bloc, et le joueur n'en voyait pas assez.
     */
    public static final SurroundArcs.Gabarit MACHINE_SWARM =
            new SurroundArcs.Gabarit(ArcPattern.SURROUND_MICRO, 8, 0.2, 0.4);

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
        // Rien de vise : rien a brancher, donc pas d'eclair. Le serveur refuse l'activation dans
        // ce cas-la, et le joueur voyait l'electricite quand meme — c'est ce qu'il a signale.
        if (block == null) return;

        int ownerId = player.getId();
        Vec3 eye = player.getEyePosition();
        Vec3 target = block.getLocation();

        SustainedArcs.spawn(ArcPattern.CHARGING, eye, target, ownerId);
        if (swarmDue(heldTicks)) {
            sowSwarm(player, block.getBlockPos(), ownerId);
        }
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
     * <p>Et le point de depart se tient a une longueur d'arc du bord, sans quoi un arc ne
     * tombant pres du bord sortait du bloc de presque toute sa taille — le joueur a vu un
     * eclair depasser d'un bloc entier. Voir {@link SurroundArcs#inset}.
     *
     * <p>Il n'y a d'etincelle que sur une vraie machine : viser un mur ne fait que l'arc.
     */
    private static void sowSwarm(Player player, BlockPos pos, int ownerId) {
        BlockEntity entity = player.level().getBlockEntity(pos);
        if (!(entity instanceof EnergyReceiver)) return;

        double inside = SurroundArcs.inset(1.0, MACHINE_SWARM) / 2.0;
        SurroundArcs.spawn(MACHINE_SWARM, Vec3.atCenterOf(pos), inside * 2.0, -inside, inside,
                ownerId, RANDOM);
    }
}
