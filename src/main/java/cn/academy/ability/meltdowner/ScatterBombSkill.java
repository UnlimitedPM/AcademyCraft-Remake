package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bombe a fragmentation, portage de {@code ScatterBomb} : une pluie de billes de plasma
 * posees au-dessus de l'epaule pendant quatre secondes, qui partent toutes ensemble a la
 * fin du maintien.
 *
 * <p>C'est la competence la plus « en deux temps » du mod : rien ne se passe quand on
 * relache, et pourtant tout se decide la. Tenir la touche <b>pose</b> des billes (une
 * toutes les dix ticks, de la premiere seconde a la quatrieme), les relacher les
 * <b>envoie</b> — chacune vers une direction un peu a cote du regard, ou vers un
 * adversaire proche si le joueur a assez d'experience pour que les billes visent toutes
 * seules.
 *
 * <h2>Le contrepoids</h2>
 *
 * L'original ne laissait pas tenir la bombe indefiniment : au bout de dix secondes, elle
 * se retournait contre son lanceur — six points de degats, puis la salve partait quand
 * meme. Le port suit ce compte-la : c'est le seul delai du mod qui se paie en se blessant.
 * La quatrieme seconde passee, le maintien ne coute plus rien : les billes sont en l'air,
 * il ne reste qu'a choisir le moment de les lacher, ou a se faire punir pour avoir
 * attendu.
 *
 * <h2>Les billes</h2>
 *
 * L'original en faisait de vraies entites ({@code EntityMdBall}), qui flottaient autour
 * du joueur et le suivaient. Le port n'a pas d'entite : il retient la <b>position du
 * joueur</b> au moment ou chaque bille est posee, et les rayons partent de la. L'ecart est
 * d'au plus un bloc et quelque sur une portee de quinze, et il n'en reste rien de visible
 * — les billes de l'original n'etaient elles-memes que des particules. Un test fige le
 * calendrier de pose, pour que ce soit bien une bille toutes les dix ticks.
 */
public class ScatterBombSkill extends Skill {

    /** Premiere bille : une seconde apres l'appui, comme {@code ticks >= 20}. */
    public static final int FIRST_BALL_TICK = 20;

    /** Une bille toutes les dix ticks ({@code MOD}). */
    public static final int BALL_INTERVAL = 10;

    /** Derniere bille : quatre secondes apres l'appui ({@code ticks <= 80}). */
    public static final int LAST_BALL_TICK = 80;

    /** Au-dela, la bombe se retourne contre son lanceur ({@code ticks == 200}). */
    public static final int BACKFIRE_TICK = 200;

    /** Degats que la bombe s'inflige a elle-meme, comme l'original. */
    public static final float BACKFIRE_DAMAGE = 6f;

    /** Portee d'une bille ({@code RAY_RANGE}). */
    public static final double BALL_RANGE = 15.0;

    /** Rayon dans lequel les billes cherchent une cible toutes seules ({@code 5}). */
    public static final double AUTO_RANGE = 5.0;

    /**
     * Experience a partir de laquelle les billes visent d'elles-memes.
     *
     * L'original testait {@code exp > 0.5} : la bombe change de nature a mi-parcours, et
     * passe d'un jet de dispersion a une salve qui poursuit.
     */
    public static final float AUTO_TARGET_EXP = 0.5f;

    /** Dispersion d'un jet, en degres de part et d'autre du regard ({@code 25}). */
    public static final float SPREAD_DEGREES = 25f;

    public ScatterBombSkill() {
        super("scatter_bomb", 2);
    }

    /**
     * Une bille est-elle posee a ce tick du maintien ?
     *
     * Le calendrier de l'original : a partir de la premiere seconde, une toutes les dix
     * ticks, jusqu'a la quatrieme inclusivement — sept billes en tout.
     */
    public static boolean spawnsBallAt(int heldTicks) {
        return heldTicks >= FIRST_BALL_TICK
                && heldTicks <= LAST_BALL_TICK
                && (heldTicks - FIRST_BALL_TICK) % BALL_INTERVAL == 0;
    }

    /** Combien de billes le maintien a posees apres ce nombre de ticks. */
    public static int ballCount(int heldTicks) {
        int count = 0;
        for (int tick = 1; tick <= heldTicks; tick++) {
            if (spawnsBallAt(tick)) count++;
        }
        return count;
    }

    /**
     * Combien de billes visent une cible d'elles-memes.
     *
     * L'original en prenait {@code billes x experience}, et seulement a partir de la
     * moitie de l'experience. Une bombe au maximum envoie donc <b>toutes</b> ses billes
     * sur les adversaires proches, et une bombe a peine apprise les jette au hasard.
     */
    public static int autoTargetCount(float exp, int balls) {
        if (exp <= AUTO_TARGET_EXP) return 0;
        return Math.min(balls, (int) (balls * exp));
    }

    /** Degats d'une bille : de 5 a 9, comme l'original. */
    public float ballDamage(AbilityData data) {
        return lerp(5f, 9f, data.getSkillExp(this));
    }

    /**
     * Entretien, par tick, tant que les billes se posent.
     *
     * 3 a 6 CP par tick, comme l'original : huit a dix-sept points pour les quatre
     * secondes de la ponte.
     */
    public float cpPerTick(AbilityData data) {
        return lerp(3f, 6f, data.getSkillExp(this));
    }

    /**
     * La bombe se tient : elle se paie a l'ouverture, s'entretient tant qu'elle pose, et
     * n'envoie ses billes qu'a la fin.
     */
    @Override
    public boolean isHeld() {
        return true;
    }

    /**
     * Pas de duree maximale : c'est {@link #BACKFIRE_TICK} qui termine le maintien, et il
     * le fait dans {@link #onHoldTick} pour pouvoir blesser le lanceur d'abord.
     */
    @Override
    public int getMaxHoldTicks(AbilityData data) {
        return 0;
    }

    /**
     * Cout d'ouverture : de 80 a 60 selon l'experience.
     *
     * C'est le vrai prix de la competence — presque une barre entiere de surcout a son
     * niveau — et l'ouverture epingle la reserve, comme l'original : sans cela, tenir la
     * bombe la rendrait de plus en plus gratuite.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(80f, 60f, data.getSkillExp(this));
    }

    /** Les billes se paient une par une, pendant la ponte : rien a l'ouverture. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /**
     * L'experience se verse a la fin, et elle se compte en billes.
     *
     * L'original ajoutait {@code 0,001 x billes} dans son message de fin : une bombe
     * tenue au maximum rapporte donc sept fois plus qu'une bombe relachee aussitot.
     */
    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // L'original epingle le surcout consomme a l'ouverture (`overloadKeep`) : la
        // reserve ne redescend plus tant que la bombe est tenue.
        data.setHeldOverload(this, data.getOverload());
    }

    /**
     * Un tick de maintien : poser une bille, ou se faire punir.
     *
     * L'ordre de l'original est garde — la bille est posee <b>avant</b> que l'entretien ne
     * soit paye — donc une reserve qui se vide pile sur un tick de pose laisse quand meme
     * sa bille derriere elle.
     */
    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        if (heldTicks >= BACKFIRE_TICK) {
            // Dix secondes de maintien : l'original se blessait de six points et
            // terminait. La salve part quand meme, c'est ce que fait `onHoldEnd`.
            player.hurt(player.damageSources().playerAttack(player), scaled(BACKFIRE_DAMAGE));
            return false;
        }
        if (heldTicks > LAST_BALL_TICK) {
            // Les billes sont en l'air : plus rien ne se paie, il ne reste qu'a choisir
            // le moment de les lacher.
            return true;
        }
        if (spawnsBallAt(heldTicks)) {
            data.addHoldPoint(this, player.getEyePosition(1f));
        }
        return data.consumeControlPoint(cpPerTick(data));
    }

    /**
     * Fin du maintien : les billes partent toutes ensemble.
     *
     * Chacune part vers un peu a cote du regard, ou vers un adversaire proche pour les
     * {@link #autoTargetCount} premieres si la bombe est assez mure ; le premier corps
     * rencontre sur le trajet encaisse. L'experience se verse ici, en comptant les billes.
     */
    @Override
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
        List<Vec3> balls = data.getHoldPoints(this);
        if (balls.isEmpty()) return;

        float exp = data.getSkillExp(this);
        int auto = autoTargetCount(exp, balls.size());
        List<LivingEntity> candidates = auto > 0 ? nearbyTargets(player) : List.of();

        for (Vec3 from : balls) {
            Vec3 dest = null;
            if (auto > 0 && !candidates.isEmpty()) {
                LivingEntity target = candidates.get(player.getRandom().nextInt(candidates.size()));
                dest = new Vec3(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
                auto--;
            }
            if (dest == null) {
                dest = sprayDestination(player);
            }

            Entity hit = TargetingUtil.findEntityAlong(player, from, dest);
            if (hit instanceof LivingEntity living) {
                // `hurtResistantTime = -1` de l'original : une bille touche meme une cible
                // qui vient d'etre frappee, sans quoi une salve de sept billes n'en
                // porterait qu'une ou deux.
                living.invulnerableTime = 0;
                living.hurt(player.damageSources().indirectMagic(player, player),
                        scaled(ballDamage(data)));
            }
        }

        data.addSkillExp(this, 0.001f * balls.size());
    }

    /** Les adversaires assez proches pour que les billes les prennent en chasse. */
    private static List<LivingEntity> nearbyTargets(Player player) {
        AABB area = player.getBoundingBox().inflate(AUTO_RANGE);
        return player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());
    }

    /**
     * Ou part une bille qui ne vise personne : a cote du regard.
     *
     * L'original prenait le point d'impact du regard, puis ajoutait quinze blocs dans une
     * direction tiree au hasard : {@code (nextFloat - 0.5) * 25} degres de lacet et autant
     * de tangage, donc un cone de 25 degres au total. Les deux rotations sont celles de la
     * 1.12.2 ({@code Vec3d.rotateYaw} et {@code rotatePitch}), reprises telles quelles : le
     * lacet tourne autour de la verticale, le tangage autour de l'axe gauche-droite.
     */
    private static Vec3 sprayDestination(Player player) {
        Vec3 begin = TargetingUtil.findImpactPoint(player, BALL_RANGE);
        double yaw = Math.toRadians((player.getRandom().nextFloat() - 0.5f) * SPREAD_DEGREES);
        double pitch = Math.toRadians((player.getRandom().nextFloat() - 0.5f) * SPREAD_DEGREES);
        Vec3 dir = rotateToward(player.getLookAngle(), yaw, pitch);
        return begin.add(dir.scale(BALL_RANGE));
    }

    /** Rotation d'un vecteur, comme {@code Vec3d.rotateYaw(...).rotatePitch(...)}. */
    private static Vec3 rotateToward(Vec3 v, double yaw, double pitch) {
        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double x1 = v.x * cy + v.z * sy;
        double z1 = v.z * cy - v.x * sy;

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double y2 = v.y * cp + z1 * sp;
        double z2 = z1 * cp - v.y * sp;
        return new Vec3(x1, y2, z2);
    }
}
