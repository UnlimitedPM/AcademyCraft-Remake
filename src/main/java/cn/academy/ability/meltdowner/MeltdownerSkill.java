package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Active skill, port of original Meltdowner: sustained plasma beam, single-target hitscan damage. */
public class MeltdownerSkill extends Skill {

    private static final double RANGE = 20;

    /** Le tir ne part pas en dessous d'une seconde de charge : {@code TICKS_MIN}. */
    public static final int TICKS_MIN = 20;

    /** Au-dela de deux secondes, charger davantage ne change plus rien : {@code TICKS_MAX}. */
    public static final int TICKS_MAX = 40;

    /**
     * Limite de securite de l'original : au-dela, la charge s'abandonne d'elle-meme.
     *
     * Elle n'aurait pas du etre atteinte — la charge est plafonnee a {@code TICKS_MAX} —
     * mais l'original la gardait, et une charge abandonnee vaut mieux qu'une charge qui
     * consomme sans fin.
     */
    public static final int TICKS_TOLE = 100;

    public MeltdownerSkill() {
        super("meltdowner", 3);
    }

    /**
     * Le meltdowner est la competence qui se chargeait dans l'original : la touche
     * reste enfoncee, et le tir part au relachement avec ce qui a ete accumule.
     */
    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public int getMinChargeTicks(AbilityData data) {
        return TICKS_MIN;
    }

    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return TICKS_MAX;
    }

    /**
     * Entretien de la charge, par tick.
     *
     * L'original demandait 10 a 15 CP par tick, et le port reprend ses chiffres :
     * une vingtaine de points pour un tir tenu au maximum, en plus de son cout
     * d'ouverture.
     */
    public float chargeCpCost(AbilityData data) {
        return lerp(10f, 15f, data.getSkillExp(this));
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // L'original epingle le surcout de l'ouverture pendant toute la charge (le
        // `overloadKeep` de son contexte) : sans cela, la reserve redescendrait pendant
        // qu'on charge et le tir finirait par ne plus rien couter.
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        if (chargeTicks > TICKS_TOLE) return false;
        return data.consumeControlPoint(chargeCpCost(data));
    }

    /**
     * Facteur de charge de l'original : 0,8 juste au minimum, 1,2 a pleine charge.
     *
     * <p>C'est lui qui fait toute la difference entre un tir rapide et un tir tenu :
     * il multiplie les degats, la recharge et le gain d'experience de la meme facon.
     */
    public float timeRate(AbilityData data) {
        int ticks = Math.min(data.getChargeTicks(this), TICKS_MAX);
        return lerp(0.8f, 1.2f, (ticks - TICKS_MIN) / (float) (TICKS_MAX - TICKS_MIN));
    }

    /**
     * Degats repris de l'original : de 18 a 50 selon l'experience, le tout multiplie par
     * le facteur de charge. Un tir tenu au maximum fait donc moitie plus mal qu'un tir
     * relache tout juste passe.
     */
    public float damage(AbilityData data) {
        return timeRate(data) * lerp(18f, 50f, data.getSkillExp(this));
    }

    /**
     * Experience : 0,002 de base, multiplie par le facteur de charge comme dans
     * l'original — tenir son tir fait donc progresser plus vite.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return timeRate(data) * 0.002f;
    }

    /**
     * Recharge reprise de l'original : 15 a 7 secondes selon l'experience, multipliees
     * par le facteur de charge. Tenir le tir le plus longtemps le rend plus puissant
     * mais le rend aussi plus long a revenir.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) (timeRate(data) * 20 * lerp(15f, 7f, data.getSkillExp(this)));
    }

    /**
     * Rien a l'appui : le tir se paie par tick de charge.
     *
     * <p>L'original posait 200 a 170 de surcout a l'ouverture et 10 a 15 CP par tick de
     * charge, et rien d'autre. Le port y ajoutait 20 CP fixes, une facture qu'il ne
     * devait pas — c'est {@link #chargeCpCost} qui dit le prix du tir.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /**
     * Surcout repris de l'original : de 200 a 170 selon l'experience, a l'ouverture
     * du tir.
     *
     * L'original drainait encore 10 a 15 points par tick pendant la charge ; ce
     * drainage n'est pas porte, comme les couts en CP de cet original qui supposent
     * une toute autre echelle de reserve.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(200f, 170f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // La seule competence du port qui se declare dans la categorie des joueurs :
        // c'est ce que l'original demandait, pour que le curseur des competences la
        // baisse avec le reste.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.MD_MELTDOWNER,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);

        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
    }
}
