package cn.academy.ability.client;

import cn.academy.ability.Skill;
import cn.academy.ability.client.md.MdSparks;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.ability.meltdowner.MeltdownerVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * L'essaim du meltdowner, pendant que le joueur charge son tir.
 *
 * <p>C'est le retour visuel de la charge : des grains de plasma tournent autour du joueur et
 * montent lentement tant que la touche est enfoncee. L'original les semait dans son contexte
 * client, a chaque tick ; le port les seme ici, du meme endroit, la ou {@code ThunderClapEffect}
 * gresille deja pour l'orage.
 *
 * <p>Les grains sont ceux du plasma — la meme {@code MdParticleFactory} que les etincelles des
 * rayons et l'essaim du bouclier — donc {@code MdSparks} les porte, et rien de nouveau n'est
 * dessine : ce fichier ne fait que les tirer et les poser.
 *
 * <p>Ou ils se posent est l'affaire de {@link MeltdownerVisuals} : aux pieds de celui qui charge,
 * a hauteur d'yeux pour les autres.
 */
public final class MeltdownerCharge {

    /**
     * Le hasard de l'essaim.
     *
     * <p>Le sien, et non celui du joueur : celui-ci rend un {@code RandomSource}, que les nombres
     * du plasma — ceux de {@link MeltdownerVisuals}, qui se relisent en test — ne connaissent pas.
     * C'est le meme choix que {@code MdSparks} et {@code MdRays}, qui portent le leur.
     */
    private static final Random RANDOM = new Random();

    /**
     * Ou en est la charge, de 0 a 1 ; 0 quand rien ne charge.
     *
     * <p>Garde ici parce que le dezoom se lit a chaque IMAGE, depuis le calcul du champ de
     * vision, et non au tick : le tick le repose, la vue le lit. C'est le meme montage que celui
     * de l'orage, voir {@code ThunderClapEffect.progress}.
     */
    private static float progress;

    private MeltdownerCharge() {}

    /**
     * Un tick de charge : l'essaim se seme.
     *
     * <p>Rien n'est garde d'un tick a l'autre — les grains vivent leur vie dans {@code MdSparks},
     * et la charge n'est qu'une source. Une autre charge que celle du meltdowner ne seme rien, et
     * c'est tout ce qu'il y a a dire : les charges qui s'arretent ne rappellent personne.
     */
    public static void tick(Player player, Skill skill, int chargeTicks) {
        if (skill != MeltdownerCategory.MELTDOWNER) {
            // Une autre charge : la vue reprend sa place tout de suite, sans attendre la fin de
            // celle-ci. Les charges qui s'arretent ne rappellent personne.
            progress = 0f;
            return;
        }

        // Le dezoom suit la charge et s'arrete a son maximum : au-dela, le tir ne gagne plus rien,
        // donc la vue non plus.
        int max = Math.max(1, skill.getMaxChargeTicks(ClientAbilityData.get()));
        progress = Math.min(1f, chargeTicks / (float) max);

        Random random = RANDOM;
        // L'original mesurait cette hauteur sur le joueur LOCAL : celui qui charge voit l'essaim a
        // ses pieds, les autres le voient monter autour de son torse.
        double baseY = player.getY() + (Minecraft.getInstance().player == player
                ? 0.0
                : MeltdownerVisuals.SWARM_EYE_HEIGHT);

        int count = MeltdownerVisuals.swarmCount(random);
        for (int i = 0; i < count; i++) {
            Vec3 offset = MeltdownerVisuals.swarmOffset(
                    MeltdownerVisuals.swarmRadius(random),
                    MeltdownerVisuals.swarmAngle(random),
                    MeltdownerVisuals.swarmHeight(random));
            MdSparks.spawn(
                    new Vec3(player.getX() + offset.x, baseY + offset.y, player.getZ() + offset.z),
                    MeltdownerVisuals.swarmVelocity(random));
        }
    }

    /**
     * Le dezoom de la charge, en degres a ajouter au champ de vision.
     *
     * <p>Il grandit avec la charge, comme celui de l'orage, et vaut zero quand rien ne charge —
     * c'est ce que la vue lit a chaque image.
     */
    public static float fovDegrees() {
        return MeltdownerVisuals.FOV_DEGREES * progress;
    }

    /** La charge s'arrete : la vue reprend sa place. */
    public static void end() {
        progress = 0f;
    }
}
