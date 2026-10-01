package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.md.MdBarrage;
import cn.academy.ability.client.md.MdRayKind;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.MdRayPacket;
import cn.academy.entity.EntitySilbarn;
import cn.academy.sound.SoundLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Salve de rayons, portage de {@code RayBarrage} : deux competences en une, selon ce que le
 * regard trouve devant lui.
 *
 * <h2>Sans bille</h2>
 *
 * C'est un tir simple : la premiere creature a vingt blocs encaisse 25 a 60 points. Rien de
 * plus, et c'est la version pauvre de la competence.
 *
 * <h2>Avec une bille</h2>
 *
 * Si le regard tombe sur une <b>bille de silicium en vol</b> — pas une bille deja posee —
 * la salve s'amorce sur elle : la bille explose, et tout ce qui se trouve dans un cone de
 * 55 degres <b>autour du regard du lanceur</b> encaisse 10 a 18 points. La distinction
 * compte : le cone part de <b>l'oeil du joueur</b>, pas de la bille. On peut donc poser sa
 * bille dans un couloir, se placer ailleurs, et faucher tout ce qui passe devant soi — la
 * bille n'est qu'un declencheur, et c'est ce qui en fait une arme de couloir plutot qu'une
 * grenade.
 *
 * <h2>Un cone qui n'est pas rond</h2>
 *
 * L'original donnait a son cone une portee de 55 degres, mais s'en servait deux fois de
 * deux facons differentes : 27,5 degres de part et d'autre en <b>lacet</b> (la moitie), et
 * 55 degres de part et d'autre en <b>tangage</b> (le tout). Le cone est donc deux fois plus
 * haut que large — une tranche verticale, pas un entonnoir. C'est repris tel quel, et fige
 * par un test : personne ne devinerait ces proportions en lisant la competence en jeu.
 *
 * <h2>Une coquille de l'original</h2>
 *
 * Le calcul du tangage d'une cible y utilisait {@code sqrt(dz*dz + dz*dz)} au lieu de
 * {@code sqrt(dx*dx + dz*dz)} : la distance horizontale etait fausse des que la cible
 * n'etait pas exactement dans l'axe, donc le cone vertical s'ouvrait de travers. Le port
 * suit l'intention — la vraie distance horizontale — comme il l'a fait pour la coquille du
 * bouclier.
 */
public class RayBarrageSkill extends Skill {

    /** Portee du tir simple et du cone ({@code RAY_DIST}). */
    public static final double RANGE = 20.0;

    /** Portee de la recherche de la bille, en blocs ({@code DISPLAY_RAY_DIST}). */
    public static final double DISPLAY_RANGE = 20.0;

    /** La portee du cone, celle de l'original : elle ne sert pas deux fois de la meme facon. */
    public static final double CONE_RANGE = 55.0;

    /** Demi-angle en lacet : la moitie de la portee. */
    public static final double HALF_YAW = CONE_RANGE / 2.0;

    /** Demi-angle en tangage : la portee entiere. Le cone est deux fois plus haut que large. */
    public static final double HALF_PITCH = CONE_RANGE;

    public RayBarrageSkill() {
        super("ray_barrage", 4);
    }

    /** Degats du tir simple, quand aucune bille n'amorce la salve : de 25 a 60. */
    public float plainDamage(AbilityData data) {
        return lerp(25f, 60f, data.getSkillExp(this));
    }

    /** Degats de la salve, pour tout ce qui est dans le cone : de 10 a 18. */
    public float scatteredDamage(AbilityData data) {
        return lerp(10f, 18f, data.getSkillExp(this));
    }

    /**
     * Cette cible est-elle dans le cone ?
     *
     * Portage de la double verification de l'original : l'ecart de lacet et l'ecart de
     * tangage entre le regard et la direction de la cible. Le lacet se ramene dans
     * {@code [-180, 180]} pour que viser vers l'ouest ne fasse pas passer une cible
     * situee juste a cote pour une cible a l'oppose.
     *
     * <p>Le vecteur attendu est l'ecart entre l'oeil de la cible et celui du lanceur, comme
     * dans l'original.
     */
    public static boolean inCone(Vec3 look, Vec3 toTarget) {
        if (toTarget.lengthSqr() < 1.0E-6) return true;

        double yawLook = Math.atan2(look.x, look.z);
        double yawTarget = Math.atan2(toTarget.x, toTarget.z);
        double yawOffset = Math.toDegrees(normalize(yawTarget - yawLook));

        double pitchLook = Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z));
        double pitchTarget = Math.atan2(toTarget.y, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z));
        double pitchOffset = Math.toDegrees(pitchTarget - pitchLook);

        return Math.abs(yawOffset) <= HALF_YAW && Math.abs(pitchOffset) <= HALF_PITCH;
    }

    /** Ramene un angle dans {@code [-PI, PI]}. */
    private static double normalize(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    /**
     * Cout : 450 a 380 CP, comme l'original.
     *
     * C'est la competence la plus chere du meltdowner, et de loin — mais elle se paie en
     * surcout plus encore qu'en CP.
     */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(450f, 380f, data.getSkillExp(this));
    }

    /** Surcout : de 300 a 140, comme l'original — un tiers d'une barre pleine au depart. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(300f, 140f, data.getSkillExp(this));
    }

    /** Recharge : de cinq secondes a deux, comme l'original. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(100f, 40f, data.getSkillExp(this));
    }

    /** 0,005 par salve, quelle que soit la version : le tir, la salve, ou rien. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.005f;
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // Le regard est relu ici, une seule fois : l'original faisait de meme au moment
        // de payer, donc tourner la tete apres coup ne change rien a la salve.
        Entity inSight = TargetingUtil.findEntityInSight(player, DISPLAY_RANGE);

        if (inSight instanceof EntitySilbarn ball && !ball.isHit()) {
            // La bille explose, et c'est elle qui sert de point de depart a la salve.
            // `burst` et non `markHit` : l'original se postait elle-meme en collision, donc
            // c'est le son lourd qui se jouait.
            ball.burst();
            // Le trait s'arrete sur la bille, et vit assez longtemps pour accompagner la salve :
            // c'est le `hit` de l'original. Il part de la MAIN et non de l'oeil, sinon il nait dans
            // la camera et remplit l'ecran du tireur.
            ray(player, MdRayKind.BARRAGE_PRE_HIT, MdBarrage.handOrigin(
                    player.getEyePosition(1f), player.getViewVector(1f)), ball.position());
            // Et la gerbe part de la bille, dans l'axe du regard du tireur.
            ray(player, MdRayKind.BARRAGE, ball.position(),
                    ball.position().add(player.getViewVector(1f)));
            barrage(player, data);
            return;
        }

        // Sans bille, il ne reste qu'un tir simple, sur ce que le regard a trouve. Le trait
        // s'arrete la ou il a frappe — sur les yeux de la creature, ou sur le bloc devant.
        Vec3 impact = inSight instanceof LivingEntity living
                ? living.getEyePosition()
                : TargetingUtil.findImpactPoint(player, RANGE);
        ray(player, MdRayKind.BARRAGE_PRE_MISS,
                MdBarrage.handOrigin(player.getEyePosition(1f), player.getViewVector(1f)), impact);

        if (inSight instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.hurt(player.damageSources().indirectMagic(player, player),
                    scaled(plainDamage(data)));
            RadiationMarks.mark(living, data);
        }
    }

    /**
     * Le rayon s'annonce : le paquet pour ceux qui voient le tireur, et le son.
     *
     * <p>C'est le {@code MSG_EFFECT_PRERAY} de l'original, et ses deux rayons ensembles — l'un
     * qui finit sur la bille, l'autre qui en part — y passent tous les deux. L'original les
     * envoyait a son lanceur seul ; le port les envoie a tous ceux qui voient le tireur, comme le
     * rayon de ses billes de plasma : les degats de la salve tombent sur tout le monde, il serait
     * etrange que les autres ne voient rien venir.
     *
     * <p>Le son se joue <b>chez le serveur</b>, au meme endroit que celui des billes : c'est le
     * seul chemin qui marche, le client ne trouvant ses sons que dans son propre registre.
     */
    private void ray(Player player, MdRayKind kind, Vec3 from, Vec3 to) {
        if (!(player instanceof ServerPlayer server)) return;
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> server),
                new MdRayPacket(kind.name(), from, to));

        SoundEvent sound = SoundLookup.event(kind.sound());
        if (sound != null) {
            player.level().playSound(null, from.x, from.y, from.z, sound, SoundSource.AMBIENT,
                    kind.soundVolume(), 1f);
        }
    }

    /**
     * La salve : tout ce qui est dans le cone, autour du regard du lanceur.
     *
     * La portee est celle de l'original, vingt blocs. Le cone est ensuite verifie sur
     * l'ecart des deux regards — celui du lanceur et celui de la cible — ce qui revient a
     * la double verification de l'original, mais sur un volume cubique plutot que sur la
     * boite englobante de ses quatre coins.
     */
    private void barrage(Player player, AbilityData data) {
        Vec3 look = player.getViewVector(1f);
        Vec3 eye = player.getEyePosition(1f);
        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(RANGE);

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive())) {
            Vec3 to = target.getEyePosition().subtract(eye);
            if (to.length() > RANGE) continue;
            if (!inCone(look, to)) continue;

            // `hurtResistantTime = -1` de l'original : tout le monde encaisse, y compris ce
            // qui vient d'etre frappe.
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().indirectMagic(player, player),
                    scaled(scatteredDamage(data)));
            RadiationMarks.mark(target, data);
        }
    }
}
