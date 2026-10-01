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

    private MeltdownerCharge() {}

    /**
     * Un tick de charge : l'essaim se seme.
     *
     * <p>Rien n'est garde d'un tick a l'autre — les grains vivent leur vie dans {@code MdSparks},
     * et la charge n'est qu'une source. Une autre charge que celle du meltdowner ne seme rien, et
     * c'est tout ce qu'il y a a dire : les charges qui s'arretent ne rappellent personne.
     */
    public static void tick(Player player, Skill skill) {
        if (skill != MeltdownerCategory.MELTDOWNER) return;

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
}
