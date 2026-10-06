package cn.academy.ability.vecmanip;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Le retour de sang, portage de {@code BloodRetrograde} : le joueur touche ce qu'il a sous
 * la main, et le sang reflue.
 *
 * <p>La plus courte portee du port — <b>deux blocs</b> — pour le plus gros coup : 30 a 60
 * points de degats, la ou le railgun en fait 110 a 160 pour un rayon de vingt-cinq. C'est
 * une competence de contact, et elle ne part que si la main trouve quelque chose : sans
 * cible, <b>rien n'est facture</b>, pas meme une recharge. Le paquet ne peut pas le savoir
 * a l'avance, donc le prix se paie dans l'effet — voir {@link Skill#paysOnEffect()}.
 *
 * <p>La touche n'a pas de duree : l'original tirait tout seul au bout de trente ticks, mais
 * ce n'etait qu'un garde-fou de confort. Le port attend le relachement, comme le thunder clap
 * (voir l'ecart assume dans {@code ThunderClapSkill}) : tenir plus longtemps n'apporte rien,
 * puisque les degats ne dependent pas de la charge.
 *
 * <p>Non porte : l'effet de sang (les eclaboussures et les jets sur les murs, envoyes au
 * client par l'original), et le ralentissement du joueur pendant la charge — l'original
 * baissait pour cela {@code capabilities.walkSpeed}, un reglage que le deplacement n'utilise
 * pas (il lit l'attribut de vitesse) : le port ne reprend pas un geste qui ne changeait rien.
 */
public class BloodRetrogradeSkill extends Skill {

    /** La portee du contact : deux blocs, comme l'original. */
    public static final double REACH = 2.0;

    /** Les degats : 30 a 60, le plus gros coup du port. */
    public static final float DAMAGE_MIN = 30f;
    public static final float DAMAGE_MAX = 60f;

    /** Le cout en CP : 280 a 350, comme l'original, et il monte avec l'experience. */
    public static final float CP_MIN = 280f;
    public static final float CP_MAX = 350f;

    /** Le surcout : 55 a 40, comme l'original. */
    public static final float OVERLOAD_MIN = 55f;
    public static final float OVERLOAD_MAX = 40f;

    /** 0,002 par contact porte. */
    public static final float EXP = 0.002f;

    /** Les neuf directions de la gerbe, en degres de tangage : celles de l'original. */
    public static final int[] SPRAY_ANGLES = { 0, 30, 45, 60, 80, -30, -45, -60, -80 };

    /** Le flou du lacet : vingt degres de chaque cote. */
    public static final float SPRAY_YAW_JITTER = 20f;

    /** La portee de chaque direction : cinq blocs, et un demi-bloc en arriere du depart. */
    public static final double SPRAY_RANGE = 5.0;
    public static final double SPRAY_BACK = 0.5;

    /** Le depart se prend a six dixiemes de la hauteur de la victime. */
    public static final double SPRAY_HEAD = 0.6;

    /** Et chaque bloc rencontre en prend deux. */
    public static final int SPRAYS_PER_HIT = 2;

    private static final java.util.Random RANDOM = new java.util.Random();

    public BloodRetrogradeSkill() {
        super("blood_retro", 4);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    public float damage(AbilityData data) {
        return lerp(DAMAGE_MIN, DAMAGE_MAX, data.getSkillExp(this));
    }

    public float consumption(AbilityData data) {
        return lerp(CP_MIN, CP_MAX, data.getSkillExp(this));
    }

    public float overload(AbilityData data) {
        return lerp(OVERLOAD_MIN, OVERLOAD_MAX, data.getSkillExp(this));
    }

    /** La recharge qu'un contact pose : de quatre secondes et demie a deux. */
    public int cooldown(AbilityData data) {
        return (int) lerp(90f, 40f, data.getSkillExp(this));
    }

    /** Tout est verse par le contact : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * Le prix se paie dans l'effet, et seulement s'il y a un contact.
     *
     * <p>L'original ne facturait rien quand la touche ne trouvait personne : c'etait son
     * client qui verifiait la cible avant d'envoyer quoi que ce soit. Le port fait ce
     * controle sur le serveur, donc au meme endroit que le paiement — voir
     * {@link Skill#paysOnEffect()}.
     */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    @Override
    public float getCpCost() {
        return 0f;
    }

    /** Le surcout declare, et paye par l'effet avec le reste. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    /** La recharge est posee par le contact : voir {@link #cooldown}. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    // ------------------------------------------------------------------
    // La charge, et le contact
    // ------------------------------------------------------------------

    @Override
    public boolean isChargeable() {
        return true;
    }

    /** Aucun minimum : la touche part des qu'elle trouve quelque chose a toucher. */
    @Override
    public int getMinChargeTicks(AbilityData data) {
        return 0;
    }

    /** Et aucun maximum : l'original s'arrêtait a trente ticks, sans que cela change rien. */
    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return 0;
    }

    /**
     * Ce que la main trouve, s'il y a quelque chose.
     *
     * <p>Le rayon est celui de l'original : deux blocs, et <b>des corps vivants seulement</b>
     * — viser une barque ne rend pas de sang. Fonction de lecture, donc : le serveur la refait
     * au relachement, la ou l'original faisait confiance a la cible annoncee par son client.
     */
    public LivingEntity touch(Player player) {
        Vec3 eye = player.getEyePosition(1f);
        Vec3 end = eye.add(player.getViewVector(1f).scale(REACH));
        Entity found = TargetingUtil.findEntityAlong(player, eye, end,
                e -> e instanceof LivingEntity);
        return found instanceof LivingEntity living ? living : null;
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        LivingEntity target = touch(player);
        // Rien sous la main : rien du tout, et rien de facture.
        if (target == null) return;
        if (!data.perform(consumption(data), overload(data))) return;

        // Le sang ne coule que si les chairs ont ete ouvertes : l'original semait ses taches sans
        // regarder, et le joueur l'a vu — « parfois les monstres ne subissent aucun degat », et des
        // taches apparaissaient quand meme. Une cible encore invulnerable, ou un coup refuse faute
        // de reserve, n'ouvre rien, et ne doit donc rien tacher.
        boolean wounded = target.hurt(player.damageSources().indirectMagic(player, player),
                scaled(damage(data)));
        // Et le sang qui gicle de la plaie : l'original posait une eclaboussure sur la cible
        // frappee, et le port ne le faisait pas — c'est le meme retour que la teleporteuse, qui
        // passe par le meme paquet. Voir BloodSplashes.
        cn.academy.ability.network.BloodSplashPacket.send(target);
        // Le son du coup, entendu du seul joueur qui l'a porte : c'est le
        // `playClient(player, "vecmanip.blood_retro", AMBIENT, 1.0f)` de l'original.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.VECMANIP_BLOOD_RETRO, 1f);
        // Et les taches au sol : l'original les tirait de la TETE de sa victime, neuf directions de
        // cinq blocs, deux par bloc rencontre. Le port les tire du meme endroit et avec les memes
        // nombres, mais du SERVEUR : lui seul sait qui a ete touche, donc lui seul sait s'il y a
        // lieu d'en semer. Le client les semait avant, et le joueur a vu ce que cela donne — des
        // taches quand rien n'est touche, et trop de taches sur les murs.
        if (wounded) spray(player, target);
        data.addSkillExp(this, EXP);
        data.setCooldown(this, cooldown(data));
    }

    /**
     * Les eclaboussures du coup, autour de la victime.
     *
     * <p>Neuf directions depuis sa tete : droit devant, puis par paires a trente, quarante-cinq,
     * soixante et quatre-vingts degres de tangage, avec un lacet flou de vingt degres. Chacune qui
     * rencontre un bloc y pose deux taches, sur la face frappee — le client ne fait ensuite que les
     * poser, il ne choisit rien.
     */
    private void spray(Player player, LivingEntity target) {
        net.minecraft.world.level.Level level = player.level();
        Vec3 head = target.position().add(0, target.getBbHeight() * SPRAY_HEAD, 0);
        java.util.List<net.minecraft.core.BlockPos> positions = new java.util.ArrayList<>();
        java.util.List<net.minecraft.core.Direction> faces = new java.util.ArrayList<>();

        for (int angle : SPRAY_ANGLES) {
            float yaw = target.getYHeadRot() + (RANDOM.nextFloat() * 2f - 1f) * SPRAY_YAW_JITTER;
            Vec3 look = Vec3.directionFromRotation(angle, yaw);
            Vec3 from = head.subtract(look.scale(SPRAY_BACK));
            Vec3 to = head.add(look.scale(SPRAY_RANGE));

            net.minecraft.world.phys.BlockHitResult hit = level.clip(
                    new net.minecraft.world.level.ClipContext(from, to,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) continue;

            for (int i = 0; i < SPRAYS_PER_HIT; i++) {
                positions.add(hit.getBlockPos());
                faces.add(hit.getDirection());
            }
        }

        cn.academy.ability.network.BloodSprayPacket.send(target, positions, faces);
    }
}
