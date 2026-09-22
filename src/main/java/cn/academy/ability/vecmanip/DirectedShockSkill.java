package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Choc dirige, portage de {@code DirectedShock} : un coup de poing qui se charge et envoie
 * valser ce qu'il trouve a trois blocs.
 *
 * <p>C'est la <b>racine</b> de vecmanip : dans l'original, l'acceleration de vecteur et le
 * choc au sol descendent de lui. Sa touche se tient, et le coup part au relachement — mais il
 * a une fenetre : relache trop tot (moins de 6 ticks) il ne se passe rien, et trop tard (plus
 * de 50) non plus. L'original laissait meme tenir jusqu'a dix secondes pour rien.
 *
 * <h2>Ce qu'il fait, et ce qu'il coute</h2>
 *
 * Un seul rayon, de trois blocs, sur le premier vivant : 7 a 15 points de degats, et le
 * malheureux part en arriere et en l'air — d'autant plus que le joueur a d'experience : la
 * poussee n'existe pas avant un quart d'experience. Un coup dans le vide coute quand meme les
 * deux ressources, et ne rapporte presque rien : 0,001 contre 0,0035 pour un coup porte. Et la
 * recharge ne se pose que si le poing a touche quelque chose.
 *
 * <h2>Une coquille de l'original, la quatrieme</h2>
 *
 * La poussee se calculait sur les trois axes, mais l'axe Z recevait la composante
 * <b>verticale</b> : {@code motionZ = delta.y * -0.7} au lieu de {@code delta.z}. Une cible
 * droit devant, a la meme hauteur, ne reculait donc pas du tout — elle montait seulement.
 * Le port suit l'intention — chaque axe avec sa composante — comme il l'a fait pour les trois
 * autres coquilles rencontrees.
 */
public class DirectedShockSkill extends Skill {

    /** Portee du coup : trois blocs, comme l'original. */
    public static final double REACH = 3.0;

    /** En dessous, le coup ne part pas : {@code MIN_TICKS}. */
    public static final int MIN_TICKS = 6;

    /** Au dela, le coup est perdu : {@code MAX_ACCEPTED_TICKS}. */
    public static final int MAX_TICKS = 50;

    /** Experience a partir de laquelle le coup fait vraiment partir : {@code 0.25}. */
    public static final float KNOCKBACK_EXP = 0.25f;

    /** Force de la poussee : {@code 0.7}. */
    public static final double KNOCKBACK = 0.7;

    /**
     * Le soulevement de la poussee : {@code 0.6}, retranche a la verticale de la direction
     * inversee — donc ajoute a celle de la direction directe.
     */
    public static final double KNOCKBACK_LIFT = 0.6;

    /** Le dixieme de bloc qui decolle la cible du sol avant la poussee. */
    public static final double KNOCKBACK_LIFT_OFF = VecmanipPush.LIFT_OFF;

    /** Petite poussee supplementaire, dans l'axe du coup ({@code 0.24}). */
    public static final double SHOVE = VecmanipPush.SHOVE;

    public DirectedShockSkill() {
        // Le nom est celui de l'original, qui appelait la competence "dir_shock" alors que
        // son fichier s'appelait DirectedShock.
        super("dir_shock", 1);
    }

    /** Degats du coup : de 7 a 15 selon l'experience. */
    public float damage(AbilityData data) {
        return lerp(7f, 15f, data.getSkillExp(this));
    }

    /** Cout en CP : 50 a 100 chez l'original, divises par 28. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(1.79f, 3.57f, data.getSkillExp(this));
    }

    /** Surcout : de 18 a 12, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(18f, 12f, data.getSkillExp(this));
    }

    /**
     * Recharge : de trois secondes a une, comme l'original.
     *
     * Elle est posee <b>par le coup</b>, et seulement s'il a touche : un poing dans le vide
     * ne fait pas attendre. Le paquet ne pose donc rien de lui-meme.
     */
    public int cooldown(AbilityData data) {
        return (int) lerp(60f, 20f, data.getSkillExp(this));
    }

    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    /** Tout se verse au coup : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * Le choc se charge : la touche reste enfoncee, et le coup part au relachement.
     */
    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public int getMinChargeTicks(AbilityData data) {
        return MIN_TICKS;
    }

    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return MAX_TICKS;
    }

    /**
     * La fenetre se referme au bout de cinquante ticks.
     *
     * L'original gardait son contexte ouvert jusqu'a deux cents ticks pour ne rien en faire :
     * relacher apres cinquante ne declenchait rien du tout. Le port abandonne la charge au
     * meme moment, ce qui donne exactement le meme resultat — rien — sans laisser un poing
     * arme pendant huit secondes.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        return chargeTicks < MAX_TICKS;
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        Entity target = TargetingUtil.findEntityInSight(player, REACH);

        if (!(target instanceof LivingEntity living)) {
            // Un coup dans le vide : l'original versait quand meme un peu d'experience.
            data.addSkillExp(this, 0.0010f);
            return;
        }

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));

        if (data.getSkillExp(this) >= KNOCKBACK_EXP) {
            living.setDeltaMovement(knockback(player, living));
            // L'original desolidarisait la cible du sol d'un dixieme de bloc avant de la
            // pousser, sans quoi la poussee se perdait dans la friction du premier tick.
            living.setPos(living.getX(), living.getY() + KNOCKBACK_LIFT_OFF, living.getZ());
        }

        // La petite poussee du coup lui-meme : un vingt-quatrieme de bloc, dans l'axe qui
        // va du joueur a la cible — pas dans l'axe du regard, la cible n'etant pas
        // forcement devant soi.
        Vec3 axis = living.position().subtract(player.position());
        if (axis.lengthSqr() > 0) {
            living.setDeltaMovement(living.getDeltaMovement().add(axis.normalize().scale(SHOVE)));
        }

        data.setCooldown(this, cooldown(data));
        data.addSkillExp(this, 0.0035f);
    }

    /**
     * La poussee : la cible est jetee loin du poing, et soulevee.
     *
     * <p>Le calcul lui-meme vit dans {@link VecmanipPush#push}, partage avec l'onde de choc
     * dirigee : seule la force change d'une competence a l'autre. Ici 0,7 de poussee et 0,6
     * de soulevement, et la coquille de l'original corrigee la-bas une fois pour toutes.
     */
    public static Vec3 knockback(Player player, LivingEntity target) {
        return knockbackVelocity(player.getEyePosition(), target.getEyePosition());
    }

    /**
     * La meme poussee, mais a partir des deux points de visee : fonction pure, donc
     * verifiable sans monde. L'enveloppe au-dessus n'est la que pour lire les yeux.
     *
     * <p>Une cible pile devant le joueur, a la meme hauteur, part donc en arriere et vers le
     * haut : {@code (0, 0.36, 0.60)} pour une cible a un bloc vers +Z.
     */
    public static Vec3 knockbackVelocity(Vec3 playerEye, Vec3 targetEye) {
        return VecmanipPush.push(playerEye, targetEye, KNOCKBACK_LIFT, KNOCKBACK);
    }
}
