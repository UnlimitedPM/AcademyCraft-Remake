package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.md.MdRayKind;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.MdRayPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

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

    /** Le client rejoue ce chiffre pour ses nombres : voir {@link Skill#getTickUpkeep}. */
    @Override
    public float getTickUpkeep(AbilityData data, int ticks) {
        return ticks <= TICKS_TOLE ? chargeCpCost(data) : 0f;
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
        return data.consumeControlPoint(getTickUpkeep(data, chargeTicks));
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

        beam(player);

        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(skillDamage(player), scaled(damage(data)));
    }

    /**
     * Le faisceau : ce que le tir devient une fois lache.
     *
     * <p>L'original le posait chez son lanceur seul, par son {@code MSG_PERFORM}. Le port
     * l'annonce a tous ceux qui voient le tireur, comme celui de la salve : les degats du
     * meltdowner ne tombent pas sur lui seulement, il serait etrange que les autres ne voient
     * rien venir.
     *
     * <p>Le rayon voyage par le paquet des rayons du plasma, avec ses deux bouts et son tireur :
     * c'est le client qui le recolle a la main de celui qui a tire — voir {@code MdRayView}. Ses
     * deux points sont ceux de l'original, et ils ne prennent pas la meme origine : voir
     * {@link MeltdownerVisuals}.
     */
    private void beam(Player player) {
        if (!(player instanceof ServerPlayer server)) return;

        Vec3 look = player.getViewVector(1f);
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> server),
                new MdRayPacket(MdRayKind.MELTDOWNER.name(),
                        MeltdownerVisuals.beamFrom(player.getEyePosition(1f), look),
                        MeltdownerVisuals.beamTo(player.position(), look,
                                MeltdownerVisuals.BEAM_LENGTH),
                        player.getId()));
    }
}
