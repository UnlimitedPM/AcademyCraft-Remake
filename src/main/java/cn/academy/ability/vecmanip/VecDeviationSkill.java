package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * La deviation de vecteur, portage de {@code VecDeviation} : tant qu'on la tient, ce qui vole
 * autour de soi s'arrete net, et les coups portes font moins mal.
 *
 * <p>C'est une competence <b>tenue</b>, et la plus defensive de vecmanip. Elle fait deux
 * choses a la fois, et c'est ce qui la rend interessante a porter :
 *
 * <ul>
 * <li>a chaque tick, elle paie son entretien puis <b>arrete</b> tout ce qui entre dans les
 *     cinq blocs : les projectiles voient leur mouvement remis a zero, et les boules de feu
 *     sont eteintes — la grosse explose sur place, comme l'original ;</li>
 * <li>tant qu'elle tient, les degats recus sont <b>reduits</b> de 40 % a 90 %, payes sur la
 *     reserve a proportion de ce qu'on encaisse.</li>
 * </ul>
 *
 * <h2>Les nombres du renvoi</h2>
 *
 * <p>Le joueur a demande que les deux veilles de vecmanip <b>echangent leurs couts en CP</b> :
 * la deviation se paie donc 15 a 11 par tick, 300 a 160 par point de difficulte d'une entite
 * arretee et 20 a 15 par point de degats amorti — exactement ce que le renvoi payait autrefois.
 * Le renvoi, lui, a repris les anciens nombres de la deviation. La <b>surcharge</b> ne bouge pas :
 * la deviation epingle toujours 80 a 50 a l'ouverture, le renvoi 350 a 250.
 *
 * <p>Le surcout d'ouverture est <b>epingle</b> : il ne redescend pas tant que la competence
 * tient, sinon la tenir rembourserait son propre prix. L'original le reposait a la main a
 * chaque tick ; le port s'en remet a l'epingle des maintiens, qui dit exactement la meme
 * chose — voir {@code AbilityData#setHeldOverload}.
 *
 * <h2>Ce qu'elle n'arrete pas</h2>
 *
 * Tout n'est pas deviable : {@link EntityAffection} decide, et sa config par defaut exclut
 * <b>tout ce qui vit</b> — les creatures comme on les connait, et les objets au sol. On arrete
 * ce qui vole, pas ce qui marche. Une entite deviee est <b>marquee</b> : elle ne sera plus
 * touchee, ce qui empeche une cible immobile de rapporter de l'experience a chaque tick.
 *
 * <h2>Un quiproquo qui n'en etait pas un</h2>
 *
 * Le port a d'abord cru que sa reduction de degats appelait {@code ctx.consume} avec ses
 * arguments inverses, comme le fait le bouclier de lumiere. Il n'en est rien : la signature
 * de l'original est {@code consume(overload, cp)}, donc {@code ctx.consume(0, consumption)}
 * paie bien la <b>reserve</b> et rien d'autre — ce que le port fait aussi, par
 * {@code consumeControlPoint}. Le piege existe pourtant, et il vaut la peine d'etre note : le
 * port range ses deux ressources dans l'autre ordre, {@code perform(cp, overload)}, si bien
 * que la meme ligne recopiee telle quelle paierait la surcharge a la place de la reserve.
 *
 * <p>Non porte : les ondes visuelles et le son de l'original.
 */
public class VecDeviationSkill extends Skill {

    /** Le rayon de la veille : cinq blocs autour du joueur. */
    public static final double RANGE = 5.0;

    /**
     * L'entretien : 15 a 11 CP par tick — les nombres du renvoi, echanges par le joueur.
     *
     * <p>L'original donnait 13 a 5 a la deviation et 15 a 11 au renvoi. Le joueur a demande que les
     * deux veilles echangent leurs couts, pour que celle qu'il considere comme la plus forte soit
     * aussi la plus chere : la deviation paie donc les nombres du renvoi, et le renvoi les siens.
     * Voir le commentaire de la classe.
     */
    public static final float TICK_CP_MIN = 15f;
    public static final float TICK_CP_MAX = 11f;

    /** Le surcout epingle a l'ouverture : 80 a 50, inchange. */
    public static final float PIN_MIN = 80f;
    public static final float PIN_MAX = 50f;

    /**
     * Ce que coute chaque entite arretee : 300 a 160 par point de difficulte — les nombres du renvoi.
     *
     * <p>La deviation les payait 15 a 12, sans compter ce que valait l'entite. C'est l'autre moitie
     * de l'echange demande par le joueur : ce que le renvoi payait pour retourner une fleche, la
     * deviation le paie maintenant pour l'arreter.
     */
    public static final float ENTITY_CP_MIN = 300f;
    public static final float ENTITY_CP_MAX = 160f;

    /**
     * Ce qu'un coup encaisse coute : 20 a 15 CP par point de degats — les nombres du renvoi.
     *
     * <p>La deviation payait 15 a 12 au plus par coup, quelle que soit sa force. Elle suit
     * maintenant ce qu'elle amortit, comme le renvoi suit ce qu'il renvoie.
     */
    public static final float DAMAGE_CP_MIN = 20f;
    public static final float DAMAGE_CP_MAX = 15f;

    /** La reduction : de 40 % a 90 % des degats. */
    public static final float RESIST_REDUCTION_MIN = 0.4f;
    public static final float RESIST_REDUCTION_MAX = 0.9f;

    /** 0,001 par point de difficulte d'une entite arretee. */
    public static final float EXP_PER_DIFFICULTY = 0.001f;

    /** Et 0,0006 par point de degats amorti. */
    public static final float EXP_PER_DAMAGE = 0.0006f;

    /**
     * Au-dela, il n'y a plus de degats a amortir mais un coup de scene : l'original laissait
     * passer, le port aussi.
     */
    public static final float BIG_HIT = 9999f;

    /**
     * La force de l'explosion d'une grosse boule de feu.
     *
     * L'original la lisait sur la boule elle-meme ({@code explosionPower}), un champ que la
     * 1.20.1 garde prive et que seul le ghast regle. Sa valeur par defaut est la bonne.
     */
    public static final int FIREBALL_POWER = 1;

    public VecDeviationSkill() {
        super("vec_deviation", 2);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    /** L'entretien par tick. */
    public float tickCost(AbilityData data) {
        return lerp(TICK_CP_MIN, TICK_CP_MAX, data.getSkillExp(this));
    }

    /** Le client rejoue ce chiffre pour ses nombres : voir {@link Skill#getTickUpkeep}. */
    @Override
    public float getTickUpkeep(AbilityData data, int ticks) {
        return tickCost(data);
    }

    /** Ce qu'une entite arretee coute, selon ce qu'elle vaut. */
    public float entityCost(AbilityData data, float difficulty) {
        return difficulty * lerp(ENTITY_CP_MIN, ENTITY_CP_MAX, data.getSkillExp(this));
    }

    /** Le surcout epingle a l'ouverture, le temps du maintien. */
    public float pin(AbilityData data) {
        return lerp(PIN_MIN, PIN_MAX, data.getSkillExp(this));
    }

    /**
     * Ce qu'un coup encaisse coute, proportionnel a ce qu'il fait mal.
     *
     * <p>Paiement force, comme chez le renvoi : c'est ce qui se paie jusqu'a la derniere goutte,
     * donc le dernier coup encaisse ne laisse pas de dette derriere lui.
     */
    public float damageCost(AbilityData data, float damage) {
        return lerp(DAMAGE_CP_MIN, DAMAGE_CP_MAX, data.getSkillExp(this)) * damage;
    }

    /** La part des degats que la deviation epargne. */
    public float reduction(AbilityData data) {
        return lerp(RESIST_REDUCTION_MIN, RESIST_REDUCTION_MAX, data.getSkillExp(this));
    }

    /** Tout est verse par ce que la veille arrete, et par ce qu'elle amortit. */
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

    /**
     * Et elle se <b>bascule</b> : un appui l'ouvre, un second la ferme.
     *
     * <p>C'est l'original, dont le gestionnaire d'activation terminait le contexte deja ouvert
     * ({@code KeyDelegates.contextActivate} et {@code ActivateHandlers.terminatesContext}). Le port
     * la tenait jusqu'au relachement, et le joueur a demande la difference.
     */
    @Override
    public boolean isToggle() {
        return true;
    }

    /** Aucune duree : elle tient tant que la reserve suit, ou jusqu'au second appui. */
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
     * L'original lisait la surcharge juste apres l'avoir payee, et la repoussait a cette
     * valeur a chaque tick : elle ne pouvait donc que monter. L'epingle du port dit la meme
     * chose — plus rien ne redescend pendant le maintien — d'ou l'equivalent, en une ligne.
     */
    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        // L'entretien d'abord : une reserve qui ne suit plus termine le maintien.
        if (!data.consumeControlPoint(getTickUpkeep(data, heldTicks))) return false;
        if (!(player.level() instanceof ServerLevel level)) return true;

        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(RANGE);

        for (Entity entity : level.getEntitiesOfClass(Entity.class, area)) {
            if (entity == player || EntityAffection.isMarked(entity)) continue;

            EntityAffection.Affect affect = EntityAffection.affect(entity);
            if (affect.excluded()) continue;

            // Chaque entite arretee se paie sans verification : la veille est deja ouverte, et
            // refuser la laisserait passer ce qu'on a promis d'arreter. Et c'est la RESERVE
            // qu'elle coute, comme le `consumeWithForce(0, comsumption)` de l'original — voir
            // ENTITY_CP_MIN. Son prix suit ce que l'entite vaut, comme chez le renvoi.
            data.performForced(entityCost(data, affect.difficulty()), 0f);
            stop(level, entity);

            // Le son se pose sur l'entite arretee, comme le `MSG_PLAY` de l'original :
            // c'est ce qui donne a la veille son rythme de petits claquements.
            cn.academy.sound.AcademySounds.playAt(level, entity.position(),
                    cn.academy.ModSounds.VECMANIP_VEC_DEVIATION, 0.5f, 1.0f);

            // Et l'onde de l'arret : une seule, et petite, posee sur la tete de ce qui vient
            // d'etre fige — l'original en posait une ici meme. Voir VecWaves.
            cn.academy.ability.network.VecWavePacket.send(player, entity.getEyePosition(),
                    player.getYRot(), player.getXRot(), 1, 0.6);

            data.addSkillExp(this, EXP_PER_DIFFICULTY * affect.difficulty());
        }
        return true;
    }

    /**
     * Arreter une entite.
     *
     * <p>Trois cas, comme l'original : la grosse boule de feu explose sur place (en respectant
     * la regle du monde sur le grief), la petite s'eteint simplement, et tout le reste voit son
     * mouvement remis a zero — une fleche y perd en plus ses degats, car elle reste dans le
     * monde et pourrait retomber sur quelqu'un.
     */
    private void stop(ServerLevel level, Entity entity) {
        if (entity instanceof LargeFireball fireball) {
            entity.discard();
            // La 1.20.1 a remplace le drapeau `mobGriefing` par un mode d'explosion : MOB
            // est celui qui respecte la regle du monde, exactement comme l'original qui la
            // lisait avant de la passer. Le feu, lui, reste allume comme chez lui.
            level.explode(null, fireball.getX(), fireball.getY(), fireball.getZ(), FIREBALL_POWER,
                    true, Level.ExplosionInteraction.MOB);
            return;
        }
        if (entity instanceof SmallFireball) {
            entity.discard();
            return;
        }

        if (entity instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(0);
        }
        entity.setDeltaMovement(Vec3.ZERO);
        EntityAffection.mark(entity);
    }

    // ------------------------------------------------------------------
    // La reduction de degats
    // ------------------------------------------------------------------

    /**
     * Ce qu'un coup recu coute, et ce qu'il fait.
     *
     * <p>Appelee par {@code AbilityEvents} pendant le maintien seulement : hors de la veille,
     * il n'y a pas de deviation. L'original s'inscrivait aux evenements a l'ouverture et s'en
     * retirait a la fin ; le port lit le maintien, ce qui revient au meme et survit a un
     * rechargement.
     */
    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        float amount = event.getAmount();
        if (amount > BIG_HIT) return amount;

        data.performForced(damageCost(data, amount), 0f);
        data.addSkillExp(this, amount * EXP_PER_DAMAGE);
        return amount * (1f - reduction(data));
    }
}
