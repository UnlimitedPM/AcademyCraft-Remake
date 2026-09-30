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
        // Le laser se voit quoi qu'on vise — demande du joueur, et c'est ainsi chez l'original
        // aussi : son arc partait vers la portee du regard quand rien ne le retenait. C'est le
        // BRANCHEMENT qui demande une vraie machine, et il se verifie par l'essaim plus bas.
        Vec3 target = block == null
                ? eye.add(player.getLookAngle().scale(ChargingSkill.RANGE))
                : block.getLocation();

        SustainedArcs.spawn(ArcPattern.CHARGING, eye, target, ownerId);
        if (block != null && swarmDue(heldTicks)) {
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
     * <p>L'original la posait sur la position du bloc, dans une boite de sa taille meme : son
     * {@code CubePointFactory(1, 1, 1)}, un cube d'un bloc centre sur la machine — et des arcs
     * MOYENS, son {@code ArcType.NORMAL} : six, longs de 3 a 4 blocs, donc 0,9 a 1,2 une fois la
     * mise a l'echelle de son dessin retrouvee. Le port les avait raccourcis a 0,2, croyant
     * qu'ils etaient trop grands : ils l'etaient, mais trois fois, et c'etait l'echelle qui
     * manquait. Le joueur l'a dit : « il faudrait aussi que tu le fasses pour le chargeur ».
     *
     * <p>Il n'y a d'etincelle que sur une vraie machine : viser un mur ne fait que l'arc.
     */
    private static void sowSwarm(Player player, BlockPos pos, int ownerId) {
        BlockEntity entity = player.level().getBlockEntity(pos);
        if (!(entity instanceof EnergyReceiver)) return;

        // Le cube du bloc lui-meme : de son centre a ses faces, et pas au-dela.
        SurroundArcs.spawn(SurroundArcs.NORMAL, Vec3.atCenterOf(pos), 1.0, -0.5, 0.5, ownerId,
                RANDOM);
    }
}
