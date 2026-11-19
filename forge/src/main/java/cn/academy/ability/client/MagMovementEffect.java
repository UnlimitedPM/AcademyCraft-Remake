package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.arc.ArcPattern;
import cn.academy.ability.client.arc.SustainedArcs;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.electromaster.MagMovementSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * La traction magnetique, cote client : un arc fin continu de l'oeil vers ce qui est vise.
 *
 * <p>La competence tire le joueur vers ce qu'il regarde — l'original y accrochait un grappin
 * qu'il faisait voler. Le port ne dessine que le lien : un trait fin et continu, celui du motif
 * {@code thin_continuous} de l'original, qui est justement le sien pour cette competence.
 *
 * <p>Un seul arc a la fois, comme la charge, et rien qui passe par le serveur : le serveur tire
 * le joueur de son cote sans rien savoir de ce dessin.
 *
 * <p>La cible est relue a chaque tick, comme le regard : c'est ce que fait le serveur pour
 * l'entite suivie, et un bloc vise ne bouge pas. L'original fixait la sienne a l'appui ; la
 * difference ne se voit que si le joueur vise ailleurs en plein vol.
 */
@OnlyIn(Dist.CLIENT)
public final class MagMovementEffect {

    private MagMovementEffect() {
    }

    /** Un tick de traction, si c'est bien elle qui tient la touche. */
    public static void tick(Player player, Skill skill, int heldTicks) {
        if (skill != ElectromasterCategory.MAG_MOVEMENT) return;
        if (Minecraft.getInstance().level == null) return;

        Vec3 eye = player.getEyePosition();
        Vec3 target = aimTarget(player, eye);
        SustainedArcs.spawn(ArcPattern.THIN_CONTINUOUS, eye, target, player.getId());
    }

    /**
     * Ou le regard se pose : le bloc touche, ou la portee de la competence.
     *
     * <p>ECART ASSUME, demande du joueur : son laser se voit <b>quoi qu'il regarde</b>, meme
     * dans le vide. C'est la depense qui exige du metal, et elle se decide cote serveur a chaque
     * tick — voir {@code MagMovementSkill.onHoldTick}. L'original, lui, terminait la competence
     * des l'appui quand il n'y avait rien a quoi s'accrocher.
     */
    private static Vec3 aimTarget(Player player, Vec3 eye) {
        double range = MagMovementSkill.getMaxDistance();
        BlockHitResult block = TargetingUtil.findBlockInSight(player, range);
        return block == null ? eye.add(player.getLookAngle().scale(range)) : block.getLocation();
    }
}
