package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.AbilityEvents;
import cn.academy.ability.Skill;
import cn.academy.ability.client.md.MdRayKind;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.MdRayPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import net.minecraftforge.network.PacketDistributor;

/** Active skill, port of original Meltdowner: sustained plasma beam, single-target hitscan damage. */
public class MeltdownerSkill extends Skill {

    private static final double RANGE = 20;

    /** Le tir ne part pas en dessous d'une seconde de charge : {@code TICKS_MIN}. */
    public static final int TICKS_MIN = 20;

    /** Au-dela de deux secondes, charger davantage ne change plus rien : {@code TICKS_MAX}. */
    public static final int TICKS_MAX = 40;

    /**
     * Au bout de combien de ticks de touche TENUE la charge se clot : {@code TICKS_TOLE} de
     * l'original, ou la charge s'abandonnait d'elle-meme.
     *
     * <p>Ce moment n'arrivait jamais dans le port : {@code AbilityData.tickCharges} plafonne le
     * compteur a {@code TICKS_MAX} (c'est la charge UTILE, celle qui dose les degats), donc rien ne
     * terminait plus la charge, qui durait tant que la reserve suivait. Le joueur l'a vu : « si je
     * reste appuyer sur la touche, je peux la charger a l'infini alors que dans le vrai mod, si on
     * reste juste appuyer, au bout d'un moment ca annule la competence, a la place, je veux que ca la
     * lance a la fin ».
     *
     * <p>Donc, ici : le tir PART. Rien d'autre ne change — c'est la charge la plus longue possible,
     * donc un tir a pleine puissance, avec la recharge qui va avec.
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
        // Et le compte de la touche tenue repart de zero : c'est lui, et non le compteur de charge,
        // qui sait depuis combien de temps on appuie — voir onChargeTick.
        data.setHoldMark(this, 0);
    }

    /**
     * Un tick de charge : l'entretien, et le compte de la touche tenue.
     *
     * <p>Le compteur de la charge est plafonne a {@code TICKS_MAX} par
     * {@code AbilityData.tickCharges} — c'est la <b>charge utile</b>, celle qui dose les degats — donc
     * il ne peut pas dire depuis combien de temps la touche est enfoncee. C'est la marque de maintien
     * qui compte, remise a zero a l'ouverture ({@link #onStart}), et c'est elle qui decide du tir de
     * cloture : voir {@link #TICKS_TOLE}.
     *
     * <p>Ce tir passe par {@code AbilityEvents.endHeld}, comme un vrai relachement : la recharge est
     * lue avant que le compteur ne soit oublie, l'effet de fin passe, et le client est prevenu — sans
     * quoi son animation continuerait toute seule.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        int held = data.getHoldMark(this) + 1;
        if (held >= TICKS_TOLE) {
            onActivate(player, data);
            if (player instanceof ServerPlayer server) {
                AbilityEvents.endHeld(server, data, this);
            }
            return false;
        }
        data.setHoldMark(this, held);
        return data.consumeControlPoint(getTickUpkeep(data, chargeTicks));
    }

    /**
     * Facteur de charge des <b>degats</b> : <b>2,4</b> juste au minimum, <b>2,8</b> a pleine charge.
     *
     * <p>L'original valait 0,8 a 1,2, et le port le suivait. Le joueur l'a fait monter d'un coup :
     * « je veux changer le facteur de charge de 0.8 - 1.2 a 2.4 - 2.8, parce que la maintenant par
     * rapport au railgun elle manque cruellement de degats » — puis, voyant que la recharge et
     * l'experience montaient avec lui : « fait en sorte de remettre sont temps de chargement comme
     * avant et sont gain d'exp aussi ». Seul ce facteur-ci a donc bouge ; la recharge et l'experience
     * se lisent sur {@link #cooldownRate}, qui garde les chiffres du vrai mod.
     */
    public float timeRate(AbilityData data) {
        int ticks = Math.min(data.getChargeTicks(this), TICKS_MAX);
        return lerp(2.4f, 2.8f, (ticks - TICKS_MIN) / (float) (TICKS_MAX - TICKS_MIN));
    }

    /**
     * Le facteur de <b>recharge</b> et d'<b>experience</b> : 0,8 a 1,2, celui de l'original.
     *
     * <p>C'est le {@link #timeRate} d'avant la demande du joueur, garde tel quel pour ces deux
     * courbes : elles etaient trop lourdes une fois multipliees par le facteur des degats — trente-six
     * secondes d'attente au depart de l'experience, et une experience triplee.
     */
    public float cooldownRate(AbilityData data) {
        int ticks = Math.min(data.getChargeTicks(this), TICKS_MAX);
        return lerp(0.8f, 1.2f, (ticks - TICKS_MIN) / (float) (TICKS_MAX - TICKS_MIN));
    }

    /**
     * Degats repris de l'original : de 18 a 50 selon l'experience, le tout multiplie par
     * le facteur de charge des degats. Un tir tenu au maximum fait donc moitie plus mal qu'un
     * tir relache tout juste passe.
     *
     * <p>Les deux bouts, chiffres : au depart de l'experience un tir minimal fait <b>43,2</b>
     * (2,4 x 18) et un tir tenu <b>50,4</b> (2,8 x 18) ; a l'experience maximale, <b>120</b> et
     * <b>140</b>. La marque de radiation, elle, multiplie encore ce total de 1,4 a 1,8 (voir
     * {@link RadiationIntensifySkill}) : 140 deviennent 252.
     *
     * <p>Et depuis la demande du joueur — « fait en sorte qu'il frappe tous les monstres qui passe
     * dans le rayon et pas uniquement 1 seul monstre » —, ce total n'est plus pour UNE cible : il
     * s'applique a <b>chacun</b> des vivants que le rayon traverse. Voir {@link #onActivate}.
     */
    public float damage(AbilityData data) {
        return timeRate(data) * lerp(18f, 50f, data.getSkillExp(this));
    }

    /**
     * Experience : 0,002 de base, multiplie par le facteur de la recharge — celui de l'original, que
     * le joueur a demande de retrouver : « et sont gain d'exp aussi ».
     */
    @Override
    public float getExpGain(AbilityData data) {
        return cooldownRate(data) * 0.002f;
    }

    /**
     * Recharge reprise de l'original : 15 a 7 secondes selon l'experience, multipliees par le
     * facteur de la recharge — celui du vrai mod, 0,8 a 1,2 : <b>12 a 18 secondes</b> au depart de
     * l'experience, <b>5,6 a 8,4</b> a la fin.
     *
     * <p>Elle a suivi le facteur des degats le temps d'une version — jusqu'a trente-six secondes —,
     * et le joueur l'a fait redescendre : « fait en sorte de remettre sont temps de chargement comme
     * avant ». Les deux facteurs sont donc separes.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) (cooldownRate(data) * 20 * lerp(15f, 7f, data.getSkillExp(this)));
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

    /**
     * Le tir : le faisceau, et tout ce qu'il traverse.
     *
     * <p>L'original ne frappait qu'<b>une</b> cible, la premiere du regard. Le joueur a demande autre
     * chose : « fait en sorte qu'il frappe tous les monstres qui passe dans le rayon et pas uniquement
     * 1 seul monstre ». Le rayon est donc parcouru d'un bout a l'autre — vingt blocs, ou jusqu'au
     * premier mur — et chaque vivant qu'il coupe encaisse les degats, du plus proche au plus loin.
     */
    @Override
    public void onActivate(Player player, AbilityData data) {
        // La seule competence du port qui se declare dans la categorie des joueurs :
        // c'est ce que l'original demandait, pour que le curseur des competences la
        // baisse avec le reste.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.MD_MELTDOWNER,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);

        beam(player);

        for (LivingEntity living : inTheBeam(player)) {
            living.hurt(skillDamage(player), scaled(damage(data)));
        }
    }

    /**
     * Tout ce que le faisceau traverse, du plus proche au plus loin.
     *
     * <p>Le rayon part des yeux et va jusqu'a {@link #RANGE} blocs, ou jusqu'au premier mur : c'est
     * la ligne du regard, et un mur arrete la lumiere comme il arrete les rayons des autres
     * competences du port.
     *
     * <p>Chaque vivant dont la boite est <b>coupee</b> par ce segment est touche : c'est
     * {@code AABB.clip} qui le dit, donc pas de faux positif sur une bete posee a cote. Le tri final
     * fait partir les coups du plus proche au plus loin, comme un vrai faisceau qui avance.
     */
    private static List<LivingEntity> inTheBeam(Player player) {
        Vec3 from = player.getEyePosition(1f);
        Vec3 to = from.add(player.getViewVector(1f).scale(RANGE));

        net.minecraft.world.phys.BlockHitResult wall = player.level().clip(
                new net.minecraft.world.level.ClipContext(from, to,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (wall.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            to = wall.getLocation();
        }

        List<LivingEntity> touched = new java.util.ArrayList<>();
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(from, to).inflate(1.0),
                e -> e != player && e.isPickable() && !e.isSpectator())) {
            if (living.getBoundingBox().inflate(0.3).clip(from, to).isPresent()) {
                touched.add(living);
            }
        }
        touched.sort(java.util.Comparator.comparingDouble(l -> l.position().distanceToSqr(from)));
        return touched;
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
