package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

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
 * Les <b>degats</b> de contact et le <b>surcout</b> sont repris tels quels de l'original. Les
 * <b>couts en CP</b>, eux, sont ramenes a l'echelle de la reserve du port (100 points, la ou
 * l'original en avait 2800 a ce niveau).
 *
 * <h2>Ce que le port change, sur demande</h2>
 *
 * L'original bornait le maintien dans le temps ({@code MAX_TIME}) et n'<b>absorbait</b> qu'une
 * part du coup : quinze a cinquante points, au plus une fois toutes les dix-huit ticks. Le joueur
 * a demande autre chose, en deux temps : « j'aimerais que tant qu'on a des cp, on puisse garder le
 * bouclier actif (qu'il ne soit pas limite avec du temps) », puis « la maintenant le "bouclier" n'a
 * plus ou moins RIEN d'un bouclier puisque on se prend quand meme tous les coups. J'aimerais que
 * tant qu'on a ce bouclier d'active, on ne prenne pas de degats (du genre que ca ne se montre meme
 * pas qu'on ait ete touche) et du cote ou on regarde bien sur, on peut quand meme se faire toucher
 * par l'arriere ».
 *
 * <p>Donc : <b>aucune duree maximale</b> — c'est la reserve qui borne, par l'entretien de chaque
 * tick et par le prix de chaque coup refuse —, et <b>le coup de face ne passe pas du tout</b>,
 * refus sur {@code LivingAttackEvent} comme le renvoi de vecteur : pas de degats, pas de recul,
 * pas de rouge. Derriere, en revanche, le joueur encaisse tout, exactement comme avant.
 *
 * <p>Et une fleche arretée de face <b>fond dans le bouclier</b> au lieu de retomber intacte : voir
 * {@link #melt}. Le joueur a demande la meme chose pour celles qui passent <b>devant</b> lui sans le
 * toucher : voir {@link #meltAround}, qui balaie chaque tick la portee du bouclier.
 *
 * <p>L'eventail, enfin, est celui de l'original dans les trois cas — frapper, fondre, proteger :
 * soixante degres de part et d'autre, mesures a plat. Le joueur a confirme : « la protection est
 * trop large : ramene-la a l'eventail de l'original (±60°) ».
 */
public class LightShieldSkill extends Skill {

    /** Rayon du bouclier, en blocs, comme l'original. */
    private static final double REACH = 3.0;

    /**
     * Demi-angle du bouclier : l'original frappait sur soixante degres, et c'est aussi ce qu'il
     * protegeait — sa {@code isEntityReachable} servait aux deux. Le port n'a plus qu'un chiffre pour
     * les trois usages : frapper, fondre les fleches, et refuser les coups.
     */
    private static final double HALF_ANGLE = 60.0;

    public LightShieldSkill() {
        super("light_shield", 2);
    }

    /**
     * Duree de <b>reference</b> : {@code MAX_TIME} de l'original, de 120 a 180 ticks.
     *
     * <p>Elle ne borne plus le maintien — le joueur a demande que seuls les CP l'arretent — mais
     * elle compte encore la recharge : sans elle, un bouclier tenu un quart d'heure imposerait une
     * demi-heure d'attente, ce qui n'etait pas demande et punirait l'usage qu'il vient de decrire.
     */
    public float maxTime(AbilityData data) {
        return lerp(120f, 180f, data.getSkillExp(this));
    }

    /**
     * Entretien, par tick.
     *
     * 9 a 4 CP par tick, comme l'original : une centaine de ticks sur une reserve
     * pleine de niveau 1, le reste du temps etant couvert par la regeneration.
     */
    public float holdCpCost(AbilityData data) {
        return lerp(9f, 4f, data.getSkillExp(this));
    }

    /** Le client rejoue ce chiffre pour ses nombres : voir {@link Skill#getTickUpkeep}. */
    @Override
    public float getTickUpkeep(AbilityData data, int ticks) {
        return holdCpCost(data);
    }

    /** Degats infliges a ce qui touche le bouclier : de 2 a 6, comme l'original. */
    public float touchDamage(AbilityData data) {
        return lerp(2f, 6f, data.getSkillExp(this));
    }

    /** Cout d'un coup absorbe ou inflige, en CP : de 50 a 30, comme l'original. */
    public float cpPerHit(AbilityData data) {
        return lerp(50f, 30f, data.getSkillExp(this));
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
     * <p>L'original la posait a la fin du maintien, avec le nombre de ticks tenus. Le bouclier
     * n'ayant plus de duree maximale, ce compte est <b>plafonne</b> a {@link #maxTime} : tenir plus
     * longtemps que l'ancien maximum ne creuse pas une attente demesuree.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        int held = Math.min(data.getChargeTicks(this), (int) maxTime(data));
        return (int) lerp(2f * held, held, data.getSkillExp(this));
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // L'original epingle le surcout consomme a l'ouverture : sans cela la reserve
        // redescendrait pendant le maintien, donc tenir le bouclier rendrait son
        // surcout au fur et a mesure et l'ouverture finirait par ne plus rien couter.
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        // L'entretien se paie par tick : quand la reserve est vide, le bouclier tombe.
        if (!data.consumeControlPoint(getTickUpkeep(data, heldTicks))) return false;
        // 1e-6 par tick, comme l'original : un bouclier se gagne surtout en absorbant.
        data.addSkillExp(this, 1e-6f);
        damageAround(player, data);
        meltAround(player);
        return true;
    }

    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        // L'original ralentissait le joueur cinq secondes a la fin du bouclier.
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
    }

    /**
     * Le coup de <b>face</b> ne passe pas du tout : pas de degats, pas de recul, pas de rouge.
     *
     * <p>Le refus est fait ici, sur {@code LivingAttackEvent}, et non dans {@code onDamaged} : c'est
     * le tout premier crochet de {@code LivingEntity.hurt}, l'annuler en fait sortir la methode
     * sur-le-champ. C'est la meme raison que pour le renvoi de vecteur, et la meme demande du
     * joueur : « que ca ne se montre meme pas qu'on ait ete touche ». Une reduction partielle, elle,
     * se ferait dans {@code onDamaged} — le port n'en fait plus, le bouclier refuse ou laisse tout
     * passer.
     *
     * <p>Le coup se paie quand meme, en CP et en surcout : c'est ce prix, avec l'entretien de chaque
     * tick, qui borne le bouclier maintenant qu'il n'a plus de duree. S'il ne peut pas payer, le coup
     * passe — et l'entretien du tick suivant fera tomber le bouclier de toute facon.
     *
     * <p>Ce qui n'a <b>pas d'auteur</b> — la lave, le feu, une chute — n'a pas de cote : le bouclier
     * ne s'en mele pas. Devant ou derriere n'a de sens que pour un coup porte par quelqu'un.
     */
    @Override
    public boolean onAttacked(Player player, AbilityData data, LivingAttackEvent event) {
        if (!data.isCharging(this)) return false;
        if (!comesFromTheFront(player, event.getSource())) return false;
        if (!data.perform(cpPerHit(data), overloadPerHit(data))) return false;
        if (melts(event.getSource().getDirectEntity())) {
            melt(player, (AbstractArrow) event.getSource().getDirectEntity());
        }
        data.addSkillExp(this, 0.001f);
        return true;
    }

    /**
     * La fleche arretée par le bouclier <b>fond</b> : elle est detruite, et non renvoyee.
     *
     * <p>Le joueur : « j'aimerais que quand je me prend une fleche, au lieu de la renvoyer ca la
     * detruise, comme si elle fondait dans mon bouclier ». Une fleche dont le coup est annule
     * retombait en effet par terre, intacte et ramassable — elle ne repartait pas vers son tireur,
     * mais elle restait la, alors que rien de ce qui touche un bouclier de plasma ne devrait y
     * survivre.
     *
     * <p>Le test est celui de <b>l'entite</b>, et non d'une liste : tout ce qui est un
     * {@code AbstractArrow} y passe — la fleche ordinaire, la spectrale, l'enduite, et toutes celles
     * des autres mods —, ce qui est exactement ce que le joueur a demande : « je veux que toutes les
     * fleches se fassent bruler ». La seule exception est le <b>trident</b>, aussi un
     * {@code AbstractArrow} : « mais pas un trident non plus ». Le detruire ferait perdre son arme au
     * tireur, ce que le bouclier du jeu ne fait pas davantage.
     *
     * <p>Le gresillement est celui d'un feu qu'on eteint, et il se joue <b>cote serveur</b> — c'est
     * le seul chemin qui marche pour un son, voir {@code EntityMdBall.flash}.
     */
    private static void melt(Player player, AbstractArrow arrow) {
        arrow.discard();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** Vrai pour tout ce qui doit fondre : toute fleche, mais pas un trident. */
    private static boolean melts(Entity entity) {
        return entity instanceof AbstractArrow arrow && !(arrow instanceof ThrownTrident);
    }

    /**
     * Les fleches qui passent <b>devant</b> le bouclier fondent, meme sans le toucher.
     *
     * <p>Le joueur : « si je vois une fleche passer devant moi mais que je suis pas vraiment dans la
     * ligne de mire, elle passe et c'est tout [...] je voudrais faire en sorte que ca le detruise
     * meme 1 bloc plus loin que mon bouclier ». Le disque se dessine un bloc devant les yeux —
     * {@code ShieldVisuals.DISTANCE} — et la zone qui fond va jusqu'a {@link #REACH} blocs, donc
     * deux blocs au-dela du disque, dans le meme eventail que le reste : ce qui passe dans le dos ne
     * fond pas.
     *
     * <p>Ce balayage ne coute rien de plus que l'entretien : il ne frappe personne, et une fleche
     * detruite n'est pas un ennemi qu'on blesse. C'est l'entretien de chaque tick qui borne tout ca.
     */
    private static void meltAround(Player player) {
        AABB area = player.getBoundingBox().inflate(REACH);
        for (AbstractArrow arrow : player.level().getEntitiesOfClass(AbstractArrow.class, area,
                LightShieldSkill::melts)) {
            if (!isInFront(player, arrow.position())) continue;
            melt(player, arrow);
        }
    }

    /**
     * Vrai si le coup vient du cote que le joueur regarde : l'eventail de l'original, ±60° a plat.
     *
     * <p>C'est le meme chiffre que celui des degats de contact et de la fonte des fleches, parce que
     * c'est le meme chez l'original : sa {@code isEntityReachable} servait a la fois a choisir qui le
     * bouclier frappait et a decider si un coup etait absorbe. Le port avait ouvert la protection a
     * un demi-plan de 180° ; le joueur l'a trouve bizarre et a demande le retour aux 60°.
     *
     * <p>Le plan se lit a plat, comme pour le cone : regarder le ciel ne doit pas rendre invulnerable
     * a ce qui monte des pieds.
     */
    private static boolean comesFromTheFront(Player player, DamageSource source) {
        Entity attacker = source.getEntity();
        Vec3 from = attacker != null ? attacker.position() : source.getSourcePosition();
        return from != null && isInFront(player, from);
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
            target.hurt(skillDamage(player), scaled(touchDamage(data)));
            RadiationMarks.mark(target, data);
            data.addSkillExp(this, 0.001f);
        }
    }

    /**
     * Vrai si cette position est dans le demi-angle devant le joueur, comme l'original.
     *
     * <p>C'est l'{@code isEntityReachable} du vrai mod, au chiffre pres : un angle mesure a plat,
     * entre le regard et la direction de la cible. Elle sert aux trois usages du bouclier.
     */
    private static boolean isInFront(Player player, Vec3 where) {
        Vec3 look = player.getLookAngle();
        Vec3 to = where.subtract(player.position());
        double flat = to.x * to.x + to.z * to.z;
        if (flat < 1.0E-6) return true;
        double angle = Math.toDegrees(Math.atan2(look.x * to.z - look.z * to.x, look.x * to.x + look.z * to.z));
        return Math.abs(angle) <= HALF_ANGLE;
    }

    /** La meme chose pour une bete : c'est la meme regle, celle de l'original. */
    private static boolean isInFront(Player player, LivingEntity target) {
        return isInFront(player, target.position());
    }
}
