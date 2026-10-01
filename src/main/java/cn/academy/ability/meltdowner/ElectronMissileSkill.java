package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.MdMissilePacket;
import cn.academy.entity.EntityMdBall;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * Electron Missile : la derniere competence du meltdowner, portage de {@code ElectronMissile}.
 *
 * <p>Competence <b>tenue</b>, et celle du port qui se joue le plus longtemps : tant que la touche
 * reste enfoncee, le lanceur accumule des billes — une toutes les dix ticks, cinq au plus — et
 * toutes les huit ticks l'une d'elles part sur l'ennemi le plus proche dans les cinq a treize
 * blocs. C'est une arme d'attrition : le surcout d'ouverture est epingle (il ne redescend pas
 * pendant le maintien), l'entretien se paie en CP par tick, et chaque tir se paie en plus.
 *
 * <p>Les billes de l'original etaient des entites visibles, dessinees par un shader ; le port se
 * contentait de les <b>compter</b> dans l'etat du maintien, donc le joueur ne les voyait pas
 * tourner autour de lui. Ce sont maintenant les memes billes que celles de la bombe a electrons,
 * en version <b>silencieuse</b> (voir {@link EntityMdBall#silent}) : elles flottent, elles
 * suivent le porteur, et c'est la competence qui les envoie — jamais elles-memes. L'anneau de
 * plasma qui monte autour du lanceur, lui, est annonce par {@code MdMissilePacket}.
 *
 * <p>A la difference des deux bombes, le missile ne <b>lance</b> aucun rayon : l'original
 * choisissait sa cible a la portee et la frappait directement, et le rayon n'etait qu'un
 * <b>dessin</b> entre la bille et les yeux de la victime. C'est pourquoi ce tir passe par
 * {@link EntityMdBall#strike} et {@link EntityMdBall#flash}, et non par
 * {@link EntityMdBall#shoot}.
 *
 * <p>Une coquille de l'original est corrigee : sa recharge etait calculee par
 * {@code clampi(700, 400, exp)}, qui borne une valeur entre deux bornes donnees a l'envers et
 * retombait donc presque toujours sur 400. Le port suit l'intention — de 700 a 400 ticks selon
 * l'experience, comme toutes les autres courbes du meme genre.
 */
public class ElectronMissileSkill extends Skill {

    /** Le nombre de billes qu'un maintien peut porter, comme l'original. */
    public static final int MAX_BALLS = 5;
    /** Une bille toutes les dix ticks. */
    public static final int SPAWN_PERIOD = 10;
    /** Et un tir toutes les huit, des qu'il y a de quoi. */
    public static final int ATTACK_PERIOD = 8;
    /** Le surcout d'ouverture, epingle pendant tout le maintien. */
    public static final float OPEN_OVERLOAD = 200f;

    /**
     * La duree de vie d'une bille posee : tout le maintien possible, plus son tick.
     *
     * <p>L'original ne donnait aucune duree aux siennes : elles vivaient jusqu'a ce que le
     * contexte les tue en partant. Le port les tue de meme (voir {@link #onHoldEnd}), et cette
     * vie n'est qu'un filet — une bille qui survivrait a son maintien disparaitrait d'elle-meme
     * au bout de dix secondes.
     */
    public static final int BALL_LIFE_TICKS = 201;

    public ElectronMissileSkill() {
        super("electron_missile", 5);
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    /** Le tir se paie et verse son experience depuis l'effet, qui seul sait s'il a touche. */
    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /** Le branchement ne coute pas de CP : c'est l'entretien qui paie. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return OPEN_OVERLOAD;
    }

    /** Entretien : de 12 a 5 CP par tick, comme l'original. */
    public float upkeep(AbilityData data) {
        return lerp(12f, 5f, data.getSkillExp(this));
    }

    /** Et le tir lui-meme : de 60 a 25 CP, plus 9 a 4 de surcout. */
    public float shotCost(AbilityData data) {
        return lerp(60f, 25f, data.getSkillExp(this));
    }

    public float shotOverload(AbilityData data) {
        return lerp(9f, 4f, data.getSkillExp(this));
    }

    /** Degats d'une bille : de 10 a 18. */
    public float damage(AbilityData data) {
        return lerp(10f, 18f, data.getSkillExp(this));
    }

    /**
     * Portee de la chasse : de 5 a 13 blocs.
     *
     * <p>C'est un <b>demi-cote de boite</b>, pas un rayon de sphere : l'original elargissait la
     * boite englobante du joueur ({@code WorldUtils.getEntities}) et gardait le plus proche, sans
     * autre filtre. Une cible dans un coin est donc atteignable jusqu'a {@code range} sur chaque
     * axe, soit un peu moins de 1,42 fois cela sur la diagonale.
     */
    public float range(AbilityData data) {
        return lerp(5f, 13f, data.getSkillExp(this));
    }

    /** Duree maximale du maintien : de 80 a 200 ticks, comme l'original. */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return (int) lerp(80f, 200f, data.getSkillExp(this));
    }

    /** Recharge a la fin : de 700 a 400 ticks. Voir la coquille corrigee dans la classe. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(700f, 400f, data.getSkillExp(this));
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // Le surcout d'ouverture est epingle : sans cela, tenir le missile rembourserait son
        // propre prix au bout de quelques secondes, et l'arme serait gratuite.
        data.setHeldOverload(this, data.getOverload());
    }

    /**
     * Un tick de maintien : l'entretien, puis la bille a poser, puis le tir.
     *
     * <p>L'ordre est celui de l'original : il payait son entretien d'abord et s'arretait la si
     * la reserve ne suivait plus, posait sa bille tous les dix ticks, et tirait tous les huit.
     * L'anneau de plasma s'annonce ensuite, a chaque tick, comme le faisait son
     * {@code MSG_EFFECT_UPDATE}.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int ticks) {
        if (!data.consumeControlPoint(upkeep(data))) return false;

        List<EntityMdBall> balls = EntityMdBall.near(player);
        if (ticks % SPAWN_PERIOD == 0 && balls.size() < MAX_BALLS) {
            player.level().addFreshEntity(
                    EntityMdBall.silent(player.level(), player, BALL_LIFE_TICKS));
        }

        if (ticks != 0 && ticks % ATTACK_PERIOD == 0 && !balls.isEmpty()) {
            LivingEntity target = closest(player, range(data));
            if (target != null && data.perform(shotCost(data), shotOverload(data))) {
                launch(player, balls, target, data);
            }
        }

        announce(player);
        return true;
    }

    /**
     * Un tir : une bille tiree au hasard, et le rayon qui l'annonce.
     *
     * <p>L'original prenait son indice au hasard entre un et le nombre de billes, ce qui revient
     * a tirer une bille au sort : c'est ce qui fait qu'un maintien long ne vide pas ses billes
     * dans l'ordre ou elles sont venues. La cible, elle, est deja choisie — le missile frappe
     * donc <b>directement</b>, sans lancer de rayon, et le rayon qui part de la bille vers ses
     * yeux n'est qu'un dessin.
     */
    private void launch(Player player, List<EntityMdBall> balls, LivingEntity target,
                        AbilityData data) {
        EntityMdBall ball = balls.get(player.getRandom().nextInt(balls.size()));
        Vec3 eyes = new Vec3(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());

        ball.strike(player, target, scaled(damage(data)), true);
        ball.flash(ball.muzzle(), eyes);
        ball.discard();

        data.addSkillExp(this, 0.001f);
    }

    /**
     * La fin du maintien : les billes qui restent sont retirees du monde.
     *
     * <p>C'est le {@code s_onEnd} de l'original, qui tuait tout ce qui restait dans sa liste —
     * aucune bille ne survit au missile qui l'a posee.
     */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        for (EntityMdBall ball : EntityMdBall.near(player)) {
            ball.discard();
        }
    }

    /**
     * L'anneau de plasma du maintien, au lanceur seul.
     *
     * <p>C'est le {@code sendToClient(MSG_EFFECT_UPDATE)} de l'original : un message sans
     * contenu, envoye a celui qui tient la competence et a personne d'autre. Les autres joueurs
     * voient les billes, mais pas la fumee du lanceur.
     */
    private static void announce(Player player) {
        if (player instanceof ServerPlayer server) {
            AbilityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> server),
                    new MdMissilePacket(player.getId()));
        }
    }

    /**
     * L'ennemi le plus proche de la portee, ou {@code null}.
     *
     * <p>L'original parcourait les vivants autour de lui dans une <b>boite</b> — sa
     * {@code WorldUtils.getEntities} elargit la boite englobante du joueur — et gardait le plus
     * proche, sans tri ni filtre de distance. Le port y ajoutait une coupe spherique que
     * l'original n'avait pas : une cible en coin, atteignable chez lui, ne l'etait plus ici, et
     * c'est exactement ce que le joueur a senti comme une portee trop courte. La boite est donc
     * la seule limite, elle est prise sur le corps du joueur comme chez lui, et le plus proche
     * gagne.
     */
    private static LivingEntity closest(Player player, float range) {
        AABB area = player.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, area,
                e -> e instanceof LivingEntity && e.isAlive())) {
            double distance = entity.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = (LivingEntity) entity;
            }
        }
        return best;
    }
}
