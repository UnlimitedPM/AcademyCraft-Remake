package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Le canon a plasma, portage de {@code PlasmaCannon} : tenir la touche noue une boule quinze
 * blocs au-dessus de la tete, et la relacher la fait tomber sur ce que le regard touche.
 *
 * <p>C'est la derniere competence de vecmanip, et la plus chere des deux cotes : un niveau 5
 * qui vide la reserve pendant qu'il charge, et qui explose tout autour du point d'arrivee.
 * C'est aussi la seule du port a se jouer en <b>trois temps</b> :
 *
 * <ul>
 * <li>la <b>charge</b> — 60 ticks au depart, 30 au maximum d'experience — se paie par tick,
 *     de 18 a 25 CP (divises par 28). Contrairement au bouclier et aux ailes, ce n'est pas
 *     l'experience qui raccourcit la charge pour rien : plus on sait faire, plus la boule
 *     monte vite et plus elle coute par tick ;</li>
 * <li>le <b>tir</b>, au relachement, et seulement si la charge est complete — l'original
 *     mourait sans rien faire si la touche partait trop tot. Il verse 0,008 d'experience,
 *     pose la boule la ou le regard s'arrete, et ouvre une recharge de 1000 a 600 ticks
 *     (cinquante a trente secondes : la plus longue du port) ;</li>
 * <li>le <b>vol</b>, enfin, d'un bloc par tick en ligne droite. C'est le meme mecanisme que
 *     le reacteur du meltdowner : l'effet survit au relachement ({@code onRelease} rend
 *     vrai), et c'est le maintien qui le termine.</li>
 * </ul>
 *
 * <h2>L'explosion</h2>
 *
 * Rien de ce qui vit a moins de <b>dix</b> blocs du point d'arrivee n'y survit : 80 a 150
 * points de degats, et les immunites d'apres-coup remises a zero pour que l'explosion qui
 * suit passe elle aussi — c'est le {@code hurtResistantTime = -1} de l'original, et c'est ce
 * qui fait qu'une cible encaisse les deux. L'explosion elle-meme est une vraie explosion de
 * 12 a 15 de puissance.
 *
 * <p>Le porteur n'est pas epargne, et c'est l'original : sa boule part de quinze blocs
 * au-dessus de sa tete, mais si elle explose a ses pieds, il meurt avec le reste.
 *
 * <h2>Trois ecarts assumes, et une coquille</h2>
 *
 * <p><b>La coquille</b> : l'original consultait la permission de casser avant
 * {@code doExplosionA}, et n'appelait jamais l'etape qui detruit les blocs — au point qu'une
 * explosion a plasma ne laissait pas un trou. Le port suit l'intention : quand
 * {@code general.destroyBlocks} le permet, l'explosion casse ; sinon elle ne laisse que ses
 * degats et ses particules.
 *
 * <p><b>L'explosion partait du point vise</b>, meme quand la boule s'etait arretee sur un mur
 * en route : elle traversait donc la pierre pour frapper derriere. Le port fait partir les
 * degats et l'explosion de la boule, la ou elle s'est arretee.
 *
 * <p>Et <b>la recharge est posee a la fin du vol</b>, quand l'original la posait au moment du
 * tir : une seconde ou deux de difference, le temps que la boule arrive.
 *
 * <p>Non portes : la tornade qui tourne sous la boule ({@code TornadoEffect}), le son de
 * charge et le suivi de position qui servait a le dessiner. Touche par defaut : {@code END}.
 */
public class PlasmaCannonSkill extends Skill {

    /** La charge : 60 ticks au depart, 30 au maximum d'experience. */
    public static final float CHARGE_MIN_EXP = 60f;
    public static final float CHARGE_MAX_EXP = 30f;

    /** Et elle se paie par tick : 18 a 25 CP, divises par 28. */
    public static final float TICK_CP_MIN_EXP = 0.64f;
    public static final float TICK_CP_MAX_EXP = 0.89f;

    /** Le surcout epingle a l'ouverture, en attendant le vol : 500 a 400, verbatim. */
    public static final float PIN_MIN_EXP = 500f;
    public static final float PIN_MAX_EXP = 400f;

    /** Ce que la boule fait a ce qu'elle trouve : 80 a 150 points de degats. */
    public static final float DAMAGE_MIN_EXP = 80f;
    public static final float DAMAGE_MAX_EXP = 150f;

    /** La puissance de l'explosion : 12 a 15. */
    public static final float POWER_MIN_EXP = 12f;
    public static final float POWER_MAX_EXP = 15f;

    /** La recharge : 1000 ticks au depart, 600 au maximum — de 50 a 30 secondes. */
    public static final int COOLDOWN_MIN_EXP = 600;
    public static final int COOLDOWN_MAX_EXP = 1000;

    /** La portee de la visee : cent blocs, vivants compris. */
    public static final double RANGE = 100.0;

    /** La boule nait quinze blocs au-dessus de la tete, comme dans l'original. */
    public static final double START_HEIGHT = 15.0;

    /** Elle avance d'un bloc par tick, sur au plus 240 ticks. */
    public static final double SPEED = 1.0;
    public static final int FLIGHT_TICKS = 240;

    /** Et elle part quand il ne lui reste plus que un bloc et demi a faire. */
    public static final double ARRIVAL = 1.5;

    /** Ce qu'elle frappe : tout ce qui est a moins de dix blocs. */
    public static final double BLAST_RANGE = 10.0;

    /** 0,008 d'experience par tir, verses au relachement. */
    public static final float EXP_PER_SHOT = 0.008f;

    public PlasmaCannonSkill() {
        super("plasma_cannon", 5);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    /** Le temps de charge de la boule. */
    public float chargeTime(AbilityData data) {
        return lerp(CHARGE_MIN_EXP, CHARGE_MAX_EXP, data.getSkillExp(this));
    }

    /** Le meme, en ticks entiers. */
    public int chargeTicks(AbilityData data) {
        return (int) chargeTime(data);
    }

    /** Ce que la charge coute par tick, plus cher quand on sait faire. */
    public float chargeCost(AbilityData data) {
        return lerp(TICK_CP_MIN_EXP, TICK_CP_MAX_EXP, data.getSkillExp(this));
    }

    /** Ce que la boule charge a la surcharge, et qui reste epingle jusqu'a la fin. */
    public float pin(AbilityData data) {
        return lerp(PIN_MIN_EXP, PIN_MAX_EXP, data.getSkillExp(this));
    }

    /** Ce qu'elle fait a ce qu'elle trouve. */
    public float damage(AbilityData data) {
        return lerp(DAMAGE_MIN_EXP, DAMAGE_MAX_EXP, data.getSkillExp(this));
    }

    /** La puissance de l'explosion qui suit. */
    public float power(AbilityData data) {
        return lerp(POWER_MIN_EXP, POWER_MAX_EXP, data.getSkillExp(this));
    }

    // ------------------------------------------------------------------
    // Le vol, et ses calculs
    // ------------------------------------------------------------------

    /**
     * La position suivante de la boule.
     *
     * Portage du {@code tryMove} de l'original : la direction du point vise, a un bloc par
     * tick — et rien du tout quand il reste moins d'un bloc a faire, pour ne pas depasser la
     * cible d'un cote puis de l'autre.
     */
    public static Vec3 travel(Vec3 from, Vec3 destination) {
        Vec3 delta = destination.subtract(from);
        if (delta.length() < SPEED) return from;
        return from.add(delta.normalize().scale(SPEED));
    }

    /** La boule est-elle arrivee ? L'original s'arretait a un bloc et demi. */
    public static boolean arrived(Vec3 position, Vec3 destination) {
        return position.distanceTo(destination) < ARRIVAL;
    }

    // ------------------------------------------------------------------
    // Le maintien : charge, puis vol
    // ------------------------------------------------------------------

    @Override
    public boolean isHeld() {
        return true;
    }

    /** Aucune duree : la charge tient tant que la reserve suit, et le vol a son propre compte. */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return 0;
    }

    /** Le prix d'ouverture est le surcout epingle, et rien d'autre. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return pin(data);
    }

    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(COOLDOWN_MAX_EXP, COOLDOWN_MIN_EXP, data.getSkillExp(this));
    }

    /** L'experience se verse au tir, pas a l'appui. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * A l'ouverture : epingler le surcout, et poser la boule au-dessus de la tete.
     *
     * <p>L'original capturait cette position a la naissance de son contexte, donc a l'appui :
     * une boule ne suit pas le joueur qui marche, et c'est ce qui la fait partir de derriere
     * lui. L'epingle, elle, est la meme que celle du bouclier et des ailes — l'original
     * reposait la surcharge a cette valeur a chaque tick.
     */
    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
        data.setHoldPoint(this, player.position().add(0, START_HEIGHT, 0));
    }

    /**
     * Un tick de maintien : la charge, puis le vol.
     *
     * <p>Le repere du maintien dit ou l'on en est : tant qu'il n'y en a pas, la boule se
     * charge et se paie ; une fois pose par le tir, l'effet est en vol.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        if (data.getHoldMark(this) < 0) {
            // La charge : elle se paie par tick, et une reserve qui manque l'abandonne.
            return data.consumeControlPoint(chargeCost(data));
        }

        if (!(player.level() instanceof ServerLevel level)) return true;
        Vec3 ball = data.getHoldPoint(this);
        Vec3 destination = data.getHoldOrigin(this);
        if (ball == null || destination == null) return false;

        Vec3 next = travel(ball, destination);
        // Un mur sur le trajet arrete la boule : c'est la qu'elle explose.
        boolean blocked = !next.equals(ball) && hitsBlock(level, player, ball, next);
        data.setHoldPoint(this, next);

        if (blocked || heldTicks - data.getHoldMark(this) >= FLIGHT_TICKS
                || arrived(next, destination)) {
            explode(level, player, data, next);
            return false;
        }
        return true;
    }

    /**
     * Le relachement : le tir, si la charge est complete.
     *
     * <p>Rend {@code true} quand la boule part : le maintien reste ouvert, et c'est lui qui le
     * terminera a l'arrivee — le quatrieme cas de l'original, celui du reacteur. Sous la
     * charge minimale, l'original mourait sans rien faire, et le port aussi : rien n'est
     * verse, rien n'est pose.
     */
    @Override
    public boolean onRelease(Player player, AbilityData data, int heldTicks) {
        if (heldTicks < chargeTicks(data)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;

        // Le son de la charge qui s'acheve : l'original le jouait au tick ou son compteur
        // atteignait le temps de charge, c'est-a-dire a ce moment precis.
        cn.academy.sound.AcademySounds.playFor(player,
                cn.academy.ModSounds.VECMANIP_PLASMA_CANNON_T, 0.5f);

        data.addSkillExp(this, EXP_PER_SHOT);
        data.setHoldOrigin(this, aim(level, player));
        data.setHoldMark(this, heldTicks);
        return true;
    }

    // ------------------------------------------------------------------
    // La visee, et l'explosion
    // ------------------------------------------------------------------

    /**
     * Le point que le regard touche : un bloc, ou un corps, a cent blocs.
     *
     * Le rayon de l'original s'arretait aux deux, et prenait le premier rencontre : le port
     * cherche le corps sur le segment qui va jusqu'au bloc, donc le plus proche gagne.
     */
    private static Vec3 aim(ServerLevel level, Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1.0f).scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Entity body = TargetingUtil.findEntityAlong(player, from, block.getLocation(),
                entity -> entity instanceof LivingEntity);
        return body == null ? block.getLocation() : body.position();
    }

    /** Un bloc entre deux positions, sur le trajet de la boule. */
    private static boolean hitsBlock(ServerLevel level, Player player, Vec3 from, Vec3 to) {
        HitResult hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS;
    }

    /**
     * L'explosion.
     *
     * <p>Ce qui vit a moins de dix blocs du point d'arrivee est frappe de 80 a 150 points,
     * immunites remises a zero juste apres pour que l'explosion qui suit ne soit pas absorbee.
     * Le porteur n'est pas epargne : l'original frappait tout ce qu'il trouvait, lui compris.
     *
     * <p>L'explosion, elle, est une vraie explosion, avec ses degats, son bruit et ses
     * particules : la permission de casser ne decide que des blocs, et c'est ce que l'original
     * voulait dire — voir la coquille racontee plus haut.
     */
    private void explode(ServerLevel level, Player player, AbilityData data, Vec3 center) {
        float damage = scaled(damage(data));
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                new AABB(center, center).inflate(BLAST_RANGE))) {
            entity.hurt(player.damageSources().indirectMagic(player, player), damage);
            if (entity instanceof LivingEntity living) {
                living.invulnerableTime = 0;
            }
        }

        boolean destroy = cn.academy.Config.destroyBlocks;
        level.explode(player, center.x, center.y, center.z, power(data), false,
                destroy ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }
}
