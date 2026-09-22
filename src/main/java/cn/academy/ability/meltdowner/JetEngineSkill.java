package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Reacteur, portage de {@code JetEngine} : viser un point a douze blocs, et s'y faire
 * projeter en traversant ce qui s'y trouve.
 *
 * <p>C'est la competence la plus rapide du mod, et la seule dont l'effet <b>commence au
 * relachement</b> : tant que la touche est enfoncee, elle ne fait que montrer ou l'on
 * part — un marqueur pose au sol, dans l'original — et c'est en relachant qu'on part. Le
 * vol dure huit ticks pour rejoindre la cible, puis continue tout droit jusqu'a quinze
 * ticks : la quantite de mouvement est posee une fois, et rien ne l'arrete. L'original
 * laissait donc son porteur <b>depasser</b> sa cible de presque une fois la distance
 * visee — c'est ce qui en fait un deplacement et non une teleportation, et le port suit
 * le meme calcul plutot que de s'arreter proprement sur la cible.
 *
 * <h2>Le quatrieme cas</h2>
 *
 * Pour que cela soit possible, {@link Skill#onRelease} existe : un effet qui rend
 * {@code true} garde son maintien ouvert apres le relachement, et c'est son propre tick
 * qui le terminera. L'original faisait exactement cela — son contexte survivait a son
 * {@code MSG_MARK_END}, et ne mourait qu'a la fin du vol, cote client.
 *
 * <h2>Ce qu'on traverse</h2>
 *
 * A chaque tick du vol, l'original lancait un rayon entre la position precedente et la
 * nouvelle, et frappait le premier <b>vivant</b> rencontre pour 7 a 20 points. Traverser
 * une foule fait donc mal a tout le monde, et traverser une barque ne fait rien.
 *
 * <h2>Paiement</h2>
 *
 * L'original payait au relachement (170 a 140 CP, 60 a 50 de surcout). Le port paie a
 * l'appui, comme toute competence tenue — le vol en herite donc meme si le joueur relache
 * aussitot. Le surcout est le prix reel : a son niveau, c'est un quart de la reserve.
 */
public class JetEngineSkill extends Skill {

    /** Ticks de vol pour rejoindre la cible ({@code TIME}). */
    public static final int FLIGHT_TIME = 8;

    /** Au-dela, le vol s'arrete : quinze ticks, cible depassee ({@code LIFETIME}). */
    public static final int LIFETIME = 15;

    /** Portee de la visee : douze blocs, blocs seulement ({@code getDest}). */
    public static final double AIM_RANGE = 12.0;

    public JetEngineSkill() {
        super("jet_engine", 4);
    }

    /**
     * Ou le porteur se trouve apres {@code flightTick} ticks de vol.
     *
     * Portage exact du {@code VecUtils.lerp(start, target, ticks / TIME)} de l'original :
     * le huitieme tick est sur la cible, et le quinzieme est a 1,875 fois la distance —
     * presque un deuxieme bond, sans rien pour le freiner.
     */
    public static Vec3 pathPosition(Vec3 start, Vec3 target, int flightTick) {
        double t = flightTick / (double) FLIGHT_TIME;
        return start.add(target.subtract(start).scale(t));
    }

    /** Vitesse posee a chaque tick : la cible divisee par le temps de vol. */
    public static Vec3 flightVelocity(Vec3 start, Vec3 target) {
        return target.subtract(start).scale(1.0 / FLIGHT_TIME);
    }

    /** Degats de ce qu'on traverse : de 7 a 20 selon l'experience. */
    public float flightDamage(AbilityData data) {
        return lerp(7f, 20f, data.getSkillExp(this));
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Pas de duree maximale : la visee peut durer tant que la touche est tenue, et le vol
     * se termine lui-meme au quinzieme tick.
     */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return 0;
    }

    /**
     * Cout du vol : 170 a 140 CP chez l'original, divises par 28 a l'echelle du port.
     *
     * La visee, elle, n'est facturee par aucun tick — l'original ne faisait que verifier
     * qu'il aurait de quoi payer, ce que le port fait d'avance en payant a l'appui.
     */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(6.07f, 5f, data.getSkillExp(this));
    }

    /** Surcout du vol : de 60 a 50, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(60f, 50f, data.getSkillExp(this));
    }

    /** Recharge : de trois secondes a une seconde et demie, comme l'original. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(60f, 30f, data.getSkillExp(this));
    }

    /** Tout se verse au depart du vol : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * Le relachement : le vol commence ici.
     *
     * La cible est relue au moment ou l'on part, comme l'original ({@code getDest} du
     * serveur, au moment du {@code MSG_MARK_END}) : viser puis tourner la tete avant de
     * relacher ne change donc pas la destination. Le repere de temps du maintien retient
     * le tick du depart, pour que le vol sache ou il en est.
     */
    @Override
    public boolean onRelease(Player player, AbilityData data, int heldTicks) {
        // Un effet qui continue tout seul doit avoir de quoi se reperer : le tick de
        // depart (`mark`), le point de depart (`origin`) et la cible (`point`).
        data.setHoldOrigin(this, player.position());
        data.setHoldPoint(this, TargetingUtil.findImpactPoint(player, AIM_RANGE));
        data.setHoldMark(this, heldTicks);

        // 0,004 pour le vol, comme l'original. L'experience d'usage d'une competence tenue
        // se verse depuis son effet : le paquet ne voit pas ce qui s'est passe.
        data.addSkillExp(this, 0.004f);
        return true;
    }

    /**
     * Un tick de maintien : la visee, puis le vol.
     *
     * Le repere de temps distingue les deux temps : tant qu'il n'y en a pas, la touche est
     * seulement tenue ; une fois pose, le tick du vol se compte a partir de lui, et le vol
     * se termine au-dela de {@link #LIFETIME}.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        int releasedAt = data.getHoldMark(this);
        if (releasedAt < 0) return true;

        int flightTick = heldTicks - releasedAt;
        if (flightTick > LIFETIME) return false;

        fly(player, data, flightTick);
        return true;
    }

    /** Un tick de vol : se deplacer, et percuter ce qui se trouve sur le trajet. */
    private void fly(Player player, AbilityData data, int flightTick) {
        Vec3 start = data.getHoldOrigin(this);
        Vec3 target = data.getHoldPoint(this);
        if (start == null || target == null) return;

        Vec3 previous = player.position();
        Vec3 next = pathPosition(start, target, flightTick);

        // L'original faisait descendre son porteur de sa monture au premier tick de vol.
        if (player.isPassenger()) player.stopRiding();

        player.teleportTo(next.x, next.y, next.z);
        player.setDeltaMovement(flightVelocity(start, target));
        // La chute est remise a zero a chaque tick : on ne se blesse pas en arrivant.
        player.fallDistance = 0f;

        // Le rayon part de la position precedente, comme le `lastTickPos` de l'original.
        Entity hit = TargetingUtil.findEntityAlong(player, previous, next,
                e -> e instanceof LivingEntity);
        if (hit instanceof LivingEntity living) {
            living.hurt(player.damageSources().indirectMagic(player, player),
                    scaled(flightDamage(data)));
        }
    }
}
