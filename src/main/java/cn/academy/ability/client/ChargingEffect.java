package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.arc.ArcPattern;
import cn.academy.ability.client.arc.ArcRenderer;
import cn.academy.ability.client.arc.SurroundArcs;
import cn.academy.ability.electromaster.ChargingSkill;
import cn.academy.ability.electromaster.ElectromasterCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * L'electricite de la charge, cote client : l'arc de l'oeil vers la cible, et l'entourage.
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
     * La vie d'un arc de charge, en ticks.
     *
     * <p>Trois, et un nouvel arc a chaque tick : il y en a donc toujours trois vivants, qui se
     * remplacent sans que le trait disparaisse. C'est ce qui donne l'eclair <b>continu</b> de la
     * charge, la ou celui de la genese d'arc vit dix ticks et clignote.
     */
    private static final int LIFE_TICKS = 3;

    private static final RandomSource RANDOM = RandomSource.create();

    /** Pour l'entourage : la boite de l'original fait 1,3 fois la taille de la chose entouree. */
    private static final double SURROUND_SCALE = 1.3;

    private ChargingEffect() {
    }

    /** Un tick d'electricite, si c'est bien la charge qui tient la touche. */
    public static void tick(Player player, Skill skill, int heldTicks) {
        if (skill != ElectromasterCategory.CHARGING) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        int ownerId = player.getId();
        Vec3 eye = player.getEyePosition();
        Vec3 target = aimTarget(player, eye);

        ArcRenderer.spawn(ArcPattern.CHARGING.name(), eye, target, LIFE_TICKS, false, ownerId);

        // L'entourage se re-seme tous les trois ticks, comme le faisait l'essaim de l'original
        // quand le dernier de ses arcs s'eteignait.
        if (heldTicks % SurroundArcs.LIFE_TICKS == 0) {
            double width = player.getBbWidth() * SURROUND_SCALE;
            SurroundArcs.spawn(SurroundArcs.THIN, player.position(), width, 0.0,
                    player.getBbHeight() * SURROUND_SCALE, ownerId, RANDOM);
        }
    }

    /**
     * Ou l'arc doit finir : le bloc vise, ou la portee du regard s'il n'y en a pas.
     *
     * <p>C'est exactement le calcul de la genese d'arc, et c'est celui de l'original : chez lui,
     * le contexte visait la machine a charger, et l'arc s'arretait dessus.
     */
    private static Vec3 aimTarget(Player player, Vec3 eye) {
        BlockHitResult block = TargetingUtil.findBlockInSight(player, ChargingSkill.RANGE);
        return block == null ? eye.add(player.getLookAngle().scale(ChargingSkill.RANGE))
                : block.getLocation();
    }
}
