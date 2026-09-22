package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Competence tenue, portage de LightShield : un bouclier qui absorbe les coups et
 * repousse ce qui le touche, tant que la touche reste enfoncee.
 *
 * <p>Le port en faisait une passive qui absorbait la moitie de tout, en permanence.
 * L'original etait un maintien : il fallait le tenir, il avait une duree maximale, il
 * s'entretenait par tick, et l'absorber coutait une seconde fois. C'est cette version-la
 * qui est portee ici — ce qui lui rend au passage sa recharge et son surcout, qui
 * attendaient tous les deux ce mecanisme.
 *
 * <h2>Deux echelles</h2>
 *
 * Les <b>degats</b> absorbes, les <b>degats</b> de contact et le <b>surcout</b> sont
 * repris tels quels de l'original. Les <b>couts en CP</b>, eux, sont ramenes a l'echelle
 * de la reserve du port (100 points, la ou l'original en avait 2800 a ce niveau) : la
 * duree de maintien qui en resulte est la meme, une centaine de ticks sur une reserve
 * pleine, bornee par {@code MAX_TIME}.
 */
public class LightShieldSkill extends Skill {

    /** Rayon du bouclier, en blocs, comme l'original. */
    private static final double REACH = 3.0;

    /** Demi-angle du bouclier : l'original ne frappait que devant, sur 60 degres. */
    private static final double HALF_ANGLE = 60.0;

    /** Une absorption toutes les 18 ticks au plus, comme {@code ACTION_INTERVAL}. */
    private static final int ACTION_INTERVAL = 18;

    public LightShieldSkill() {
        super("light_shield", 2);
    }

    /**
     * Duree maximale du maintien : {@code MAX_TIME} de l'original, de 120 a 180 ticks.
     */
    public float maxTime(AbilityData data) {
        return lerp(120f, 180f, data.getSkillExp(this));
    }

    /**
     * Entretien, par tick.
     *
     * L'original demandait 9 a 4 CP par tick sur une reserve de 2800 ; rapporte a 100,
     * cela fait 1 a 0,7. Meme duree de maintien, donc : une centaine de ticks sur une
     * reserve pleine, le reste du temps etant couvert par la regeneration.
     */
    public float holdCpCost(AbilityData data) {
        return lerp(1f, 0.7f, data.getSkillExp(this));
    }

    /** Degats absorbes par coup : de 15 a 50, comme l'original. */
    public float absorbDamage(AbilityData data) {
        return lerp(15f, 50f, data.getSkillExp(this));
    }

    /** Degats infliges a ce qui touche le bouclier : de 2 a 6, comme l'original. */
    public float touchDamage(AbilityData data) {
        return lerp(2f, 6f, data.getSkillExp(this));
    }

    /** Cout d'un coup absorbe ou inflige, en CP (environ 2 % de la reserve). */
    public float cpPerHit(AbilityData data) {
        return lerp(2f, 1f, data.getSkillExp(this));
    }

    /** Surcout d'un coup absorbe ou inflige : de 5 a 3, comme l'original. */
    public float overloadPerHit(AbilityData data) {
        return lerp(5f, 3f, data.getSkillExp(this));
    }

    /** Surcout d'ouverture : de 110 a 60, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(110f, 60f, data.getSkillExp(this));
    }

    /** Le bouclier n'a pas de cout en CP a l'ouverture : il s'entretient ensuite. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /**
     * Recharge : de deux fois la duree tenue a une fois, selon l'experience.
     *
     * L'original la posait a la fin du maintien, avec le nombre de ticks tenus. D'ou
     * l'interet de garder le compteur lisible apres la fin : la recharge en depend.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        int held = data.getChargeTicks(this);
        return (int) lerp(2f * held, held, data.getSkillExp(this));
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return (int) maxTime(data);
    }

    @Override
    public void onHoldStart(Player player, AbilityData data) {
        // L'original epingle le surcout consomme a l'ouverture : sans cela la reserve
        // redescendrait pendant le maintien, donc tenir le bouclier rendrait son
        // surcout au fur et a mesure et l'ouverture finirait par ne plus rien couter.
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        // L'entretien se paie par tick : quand la reserve est vide, le bouclier tombe.
        if (!data.consumeControlPoint(holdCpCost(data))) return false;
        // 1e-6 par tick, comme l'original : un bouclier se gagne surtout en absorbant.
        data.addSkillExp(this, 1e-6f);
        damageAround(player, data);
        return true;
    }

    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        // L'original ralentissait le joueur cinq secondes a la fin du bouclier.
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
    }

    /**
     * Absorbe une part des degats subis.
     *
     * Portage de {@code handleAttacked} : au plus une absorption toutes les 18 ticks,
     * et seulement si la reserve peut payer le coup. Le bouclier doit etre tenu — c'est
     * tout l'objet de cette competence.
     */
    @Override
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        if (!data.isCharging(this)) return event.getAmount();

        int held = data.getChargeTicks(this);
        int mark = data.getHoldMark(this);
        if (mark >= 0 && held - mark <= ACTION_INTERVAL) return event.getAmount();

        // L'original appelait ici ctx.consume avec ses deux arguments dans l'ordre
        // inverse (les CP en premier) : le joueur aurait gagne 50 points de surcout
        // pour 5 CP, donc une surcharge immediate par coup absorbe. Le port suit
        // l'intention — le surcout de 5 a 3, les CP de 30 a 50 — et non la coquille.
        if (!data.perform(cpPerHit(data), overloadPerHit(data))) return event.getAmount();

        data.setHoldMark(this, held);
        data.addSkillExp(this, 0.001f);
        return Math.max(0f, event.getAmount() - absorbDamage(data));
    }

    /** Inflige les degats de contact a ce qui touche le bouclier. */
    private void damageAround(Player player, AbilityData data) {
        Level level = player.level();
        AABB area = player.getBoundingBox().inflate(REACH);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            if (!isInFront(player, target)) continue;
            // hurtResistantTime de l'original : pas d'acharnement sur la meme cible.
            if (target.hurtTime > 0) continue;
            if (!data.perform(cpPerHit(data), overloadPerHit(data))) return;
            target.hurt(player.damageSources().indirectMagic(player, player), scaled(touchDamage(data)));
            data.addSkillExp(this, 0.001f);
        }
    }

    /** Vrai si la cible est dans le demi-angle devant le joueur, comme l'original. */
    private static boolean isInFront(Player player, LivingEntity target) {
        Vec3 look = player.getLookAngle();
        Vec3 to = target.position().subtract(player.position());
        double flat = to.x * to.x + to.z * to.z;
        if (flat < 1.0E-6) return true;
        double angle = Math.toDegrees(Math.atan2(look.x * to.z - look.z * to.x, look.x * to.x + look.z * to.z));
        return Math.abs(angle) <= HALF_ANGLE;
    }
}
