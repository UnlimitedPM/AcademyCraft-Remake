package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * La reflexion de vecteur, portage de {@code VecReflection} : tant qu'on la tient, ce qui
 * vole autour de soi est <b>renvoye la ou l'on regarde</b>, et les coups recus repartent vers
 * celui qui les a donnes.
 *
 * <p>C'est la seconde competence de vecmanip a s'appuyer sur {@link EntityAffection}, et la
 * soeur de {@code vec_deviation} : meme veille, meme marque, meme exclusion de tout ce qui vit
 * — mais quatre blocs au lieu de cinq (l'original : {@code range = 4}), et un sort tres
 * different pour ce qu'elle trouve. La deviation <b>arrete</b>, la reflexion <b>retourne</b> :
 *
 * <ul>
 * <li>une entite ordinaire repart vers le point que le regard touche a vingt blocs, <b>a sa
 *     propre vitesse</b> — une fleche rapide repart vite, une bille posee ne part pas ;</li>
 * <li>une <b>boule de feu</b> ne se contente pas de repartir : l'original la tuait et posait
 *     une boule neuve au meme endroit, marquee, lancee vers ce point. C'est le renvoi d'un
 *     projectile au sens propre, le tir change de camp ;</li>
 * <li>et les coups recus sont renvoyes a leur auteur : 60 % des degats au depart, jusqu'a
 *     120 % au maximum — si bien qu'a la fin une reflexion rend plus qu'elle n'a pris, et que
 *     le coup est <b>annule</b> plutot que reduit.</li>
 * </ul>
 *
 * <h2>Deux couts, et deux paiements differents</h2>
 *
 * Ouvrir la veille charge la reserve a {@code overloadToKeep} (350, puis 250) et l'<b>epingle</b> :
 * l'original la repoussait a cette valeur a chaque tick, sans quoi tenir la veille remboursait
 * son propre prix. L'entretien, lui, se paie en reserve — 15 a 11 CP par tick chez l'original,
 * divises par 28 comme partout — et chaque entite renvoyee se paie <b>sans verification</b>
 * (300 a 160 par point de difficulte, divises par 28) : refuser la laisserait passer ce qu'on a
 * promis de renvoyer.
 *
 * <h2>Deux coquilles de l'original corrigees</h2>
 *
 * <p>La premiere est un <b>garde-fou mort</b> : {@code handleAttack} posait {@code _isAttacking
 * = true} puis testait {@code if (!_isAttacking)} — donc jamais vrai, et le renvoi, son paiement
 * et son experience ne se produisaient <b>jamais</b>. Seule la reduction de degats restait, et
 * l'intention est ecrite juste au-dessus, dans le commentaire de l'original : un renvoi ne se
 * renvoie pas, ce qui evite qu'un gardien et ses epines, ou deux joueurs qui se renvoient un
 * coup, ne bouclent sans fin. Le port suit l'intention, avec le garde-fou a sa place.
 *
 * <p>La seconde est la <b>comparaison du test anticipe</b> : le code prevoyait
 * {@code reflectDamage >= 1} la ou il fallait comparer au montant encaisse. Comme un renvoi
 * atteint 1 des qu'un coup depasse un point et demi, l'original annulait
 * {@code LivingAttackEvent} pour presque tout et son porteur ne prenait plus rien du tout — au
 * lieu de ne rien prendre <b>que</b> quand la reflexion couvre le coup. Le port compare au
 * montant, et n'annule que ce qui est entierement renvoye.
 *
 * <p>Un detail de l'original, en revanche, n'est pas suivi : il lisait l'auteur du coup avec
 * {@code getImmediateSource} — la fleche, pas l'archer — et renvoyer sur une fleche ne faisait
 * rien, puisqu'une fleche ne se blesse pas. Le port renvoie a qui a frappe, ce qui est ce que
 * l'original voulait dire.
 *
 * <p>Non portes : le temoin de renvoi ({@code MSG_REFLECT_ENTITY} : le port n'a pas de
 * particules), le test anticipe sur {@code LivingAttackEvent} (le port n'a pas ce crochet, donc
 * un coup entierement renvoye ne laisse que sa poussee, celle de vanilla), et le
 * {@code ReflectEvent} que l'original exposait aux autres mods.
 */
public class VecReflectionSkill extends Skill {

    /** La veille : quatre blocs autour du joueur (l'original : {@code range = 4}). */
    public static final double RANGE = 4.0;

    /** Le point vise par un renvoi : vingt blocs devant les yeux. */
    public static final double AIM_RANGE = 20.0;

    /** L'entretien : 15 a 11 CP par tick, comme l'original. */
    public static final float TICK_CP_MIN = 15f;
    public static final float TICK_CP_MAX = 11f;

    /** Ce que coute chaque entite renvoyee : 300 a 160 par point de difficulte, comme l'original. */
    public static final float ENTITY_CP_MIN = 300f;
    public static final float ENTITY_CP_MAX = 160f;

    /** Le surcout epingle a l'ouverture : 350 a 250, verbatim. */
    public static final float PIN_MIN = 350f;
    public static final float PIN_MAX = 250f;

    /** Ce qu'un coup encaisse coute : 20 a 15 CP par point de degats, comme l'original. */
    public static final float DAMAGE_CP_MIN = 20f;
    public static final float DAMAGE_CP_MAX = 15f;

    /** La part des degats renvoyee a l'auteur du coup : de 60 % a 120 %. */
    public static final float REFLECT_MIN = 0.6f;
    public static final float REFLECT_MAX = 1.2f;

    /** 0,0008 par point de difficulte d'une entite renvoyee, 0,0004 par point de degats recus. */
    public static final float EXP_PER_DIFFICULTY = 0.0008f;
    public static final float EXP_PER_DAMAGE = 0.0004f;

    /** Le coup de scene : au-dela, l'original laissait passer sans rien tenter. */
    public static final float BIG_HIT = 9999f;

    /**
     * La force de l'explosion d'une grosse boule de feu relancee.
     *
     * L'original recopiait celle de la boule qu'il renvoyait ; la 1.20.1 garde ce champ prive
     * et ne le donne qu'au constructeur. Seul le ghast le regle, et sa valeur par defaut est
     * donc la bonne.
     */
    public static final int FIREBALL_POWER = 1;

    /**
     * Le garde-fou du renvoi, le temps d'un coup.
     *
     * L'original en avait un par contexte, donc par joueur. Un seul suffit ici : un renvoi
     * s'applique dans l'appel qui l'a declenche, sur le fil du serveur, donc deux joueurs ne
     * peuvent pas s'y trouver en meme temps.
     */
    private static boolean reflecting;

    public VecReflectionSkill() {
        super("vec_reflection", 4);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    /** La part des degats renvoyee, de 60 % a 120 % selon l'experience. */
    public float reflectRatio(AbilityData data) {
        return lerp(REFLECT_MIN, REFLECT_MAX, data.getSkillExp(this));
    }

    /** L'entretien par tick. */
    public float tickCost(AbilityData data) {
        return lerp(TICK_CP_MIN, TICK_CP_MAX, data.getSkillExp(this));
    }

    /** Ce qu'une entite renvoyee coute, selon ce qu'elle vaut. */
    public float entityCost(AbilityData data, float difficulty) {
        return difficulty * lerp(ENTITY_CP_MIN, ENTITY_CP_MAX, data.getSkillExp(this));
    }

    /** Ce qu'un coup encaisse coute a la reserve, proportionnel a ce qu'il fait mal. */
    public float damageCost(AbilityData data, float damage) {
        return lerp(DAMAGE_CP_MIN, DAMAGE_CP_MAX, data.getSkillExp(this)) * damage;
    }

    /** Le surcout epingle a l'ouverture, le temps du maintien. */
    public float pin(AbilityData data) {
        return lerp(PIN_MIN, PIN_MAX, data.getSkillExp(this));
    }

    /** Tout est verse par ce que la veille renvoie, et par ce qu'elle rend aux coups. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    // ------------------------------------------------------------------
    // Le maintien
    // ------------------------------------------------------------------

    @Override
    public boolean isHeld() {
        return true;
    }

    /** Aucune duree : elle tient tant que la reserve suit, ou jusqu'au relachement. */
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

    /** Aucune recharge : c'est un maintien, il se termine et se reprend. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    /**
     * A l'ouverture : epingler le surcout du moment.
     *
     * Meme mecanique que la deviation de vecteur, et pour la meme raison : l'original lisait la
     * surcharge juste apres l'avoir payee, et la repoussait a cette valeur a chaque tick.
     */
    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        // L'entretien d'abord : une reserve qui ne suit plus termine la veille.
        if (!data.consumeControlPoint(tickCost(data))) return false;
        if (!(player.level() instanceof ServerLevel level)) return true;

        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(RANGE);
        // Le point que le regard touche : c'est la que repart tout ce qui est renvoye.
        Vec3 aim = TargetingUtil.findImpactPoint(player, AIM_RANGE);

        for (Entity entity : level.getEntitiesOfClass(Entity.class, area)) {
            if (entity == player || EntityAffection.isMarked(entity)) continue;

            EntityAffection.Affect affect = EntityAffection.affect(entity);
            if (affect.excluded()) continue;

            // Chaque entite renvoyee se paie sans verification : la veille est deja ouverte,
            // et refuser ici laisserait passer ce qu'on a promis de retourner.
            data.performForced(entityCost(data, affect.difficulty()), 0f);

            if (entity instanceof Fireball fireball) {
                relaunch(level, fireball, aim);
            } else {
                entity.setDeltaMovement(VecmanipPush.redirect(aim, entity.getEyePosition(),
                        entity.getDeltaMovement().length()));
                EntityAffection.mark(entity);
            }

            // Le son se pose sur ce qui vient d'etre retourne, comme la soeur inverse de la
            // veille : l'original envoyait le meme message d'effet, a la meme position.
            cn.academy.sound.AcademySounds.playAt(level, entity.position(),
                    cn.academy.ModSounds.VECMANIP_VEC_REFLECTION, 0.5f, 1.0f);

            // Et l'onde du renvoi : deux anneaux, plus grands que ceux de la deviation, sur la
            // tete de ce qui vient de repartir. Voir VecWaves.
            cn.academy.ability.network.VecWavePacket.send(player, entity.getEyePosition(),
                    player.getYHeadRot(), player.getXRot(), 2, 1.1);

            data.addSkillExp(this, EXP_PER_DIFFICULTY * affect.difficulty());
        }
        return true;
    }

    /**
     * Renvoyer une boule de feu.
     *
     * <p>L'original la tuait et posait une boule neuve a sa place, marquee, lancee vers le
     * point vise et a la vitesse de l'ancienne. Une grosse boule reste une grosse boule — elle
     * garde son pouvoir explosif, celui que l'original recopiait et que le port repose a sa
     * valeur par defaut, la 1.20.1 ne laissant plus le lire. Une petite suit son tireur si
     * elle en a un, et se pose sinon par sa position : ce sont les deux constructeurs que
     * l'original choisissait deja.
     */
    private void relaunch(ServerLevel level, Fireball source, Vec3 aim) {
        Vec3 velocity = VecmanipPush.redirect(aim, source.getEyePosition(),
                source.getDeltaMovement().length());
        Entity owner = source.getOwner();

        Fireball fresh;
        if (source instanceof LargeFireball && owner instanceof LivingEntity shooter) {
            fresh = new LargeFireball(level, shooter, 0, 0, 0, FIREBALL_POWER);
        } else if (owner instanceof LivingEntity shooter) {
            fresh = new SmallFireball(level, shooter, 0, 0, 0);
        } else {
            fresh = new SmallFireball(level, source.getX(), source.getY(), source.getZ(),
                    aim.x, aim.y, aim.z);
        }

        fresh.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(),
                source.getXRot());
        fresh.setDeltaMovement(velocity);
        EntityAffection.mark(fresh);
        source.discard();
        level.addFreshEntity(fresh);
    }

    // ------------------------------------------------------------------
    // Le renvoi des coups
    // ------------------------------------------------------------------

    /**
     * Ce qu'un coup recu coute, et ce qu'il devient.
     *
     * <p>Appelee par {@code AbilityEvents} pendant le maintien seulement : hors de la veille,
     * il n'y a pas de renvoi. L'original s'inscrivait aux evenements a l'ouverture et s'en
     * retirait a la fin ; le port lit le maintien, ce qui revient au meme et survit a un
     * rechargement.
     *
     * <p>La part renvoyee est lue <b>avant</b> que l'experience du coup ne soit versee, comme
     * dans l'original : un coup ne se renforce donc pas de l'experience qu'il vient de donner.
     */
    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        float amount = event.getAmount();
        if (amount > BIG_HIT) return amount;

        float reflected = amount * reflectRatio(data);
        // Le renvoi d'un renvoi ne se paie pas et ne se renvoie pas : c'est le garde-fou de
        // l'original, celui qui evite la boucle entre deux reflets ou avec les epines d'un
        // gardien.
        if (reflecting) return reduce(event, amount - reflected);

        reflecting = true;
        try {
            data.performForced(damageCost(data, amount), 0f);
            data.addSkillExp(this, amount * EXP_PER_DAMAGE);

            Entity attacker = event.getSource().getEntity();
            if (attacker instanceof LivingEntity living && living != player) {
                living.hurt(player.damageSources().indirectMagic(player, player),
                        scaled(reflected));
                // Et l'onde du renvoi, devant le joueur et du cote de l'attaquant : l'original
                // posait la sienne a un demi-bloc de sa tete, dans cette direction-la. Voir
                // VecWaves.
                Vec3 head = player.getEyePosition(1f);
                Vec3 toward = living.getEyePosition().subtract(head);
                Vec3 at = player.position().add(0, 0.4 + player.getRandom().nextDouble() * 0.9, 0)
                        .add(toward.lengthSqr() > 1e-6 ? toward.normalize().scale(0.5) : Vec3.ZERO);
                cn.academy.ability.network.VecWavePacket.send(player, at,
                        player.getYHeadRot(), player.getXRot(), 2, 1.1);
            }
        } finally {
            reflecting = false;
        }
        return reduce(event, amount - reflected);
    }

    /**
     * Ce qu'il reste du coup, et son annulation quand la reflexion a tout couvert.
     *
     * <p>L'original rendait un montant negatif et posait l'annulation juste apres ; la 1.20.1
     * ecrirait une vie negative, donc le port rend zero. Reste une difference assumee : chez
     * l'original le test anticipe annulait le coup <b>avant</b> que la poussee de vanilla ne
     * soit posee, ici l'annulation arrive apres, et la poussee subsiste.
     */
    private static float reduce(LivingHurtEvent event, float remaining) {
        if (remaining <= 0f) {
            event.setCanceled(true);
            return 0f;
        }
        return remaining;
    }
}
