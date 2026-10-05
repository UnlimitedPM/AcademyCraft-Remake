package cn.academy.ability.vecmanip;

import java.util.List;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

/**
 * L'onde de choc dirigee, portage de {@code DirectedBlastwave} : le joueur frappe dans le
 * vide devant lui, et un cube de terrain explose la ou son regard s'est arrete.
 *
 * <p>C'est la troisieme marche de vecmanip, et la fille de l'onde de choc au sol — mais
 * elle ne creuse pas le sol, elle <b>vise</b> : un rayon de quatre blocs, et le centre de
 * l'explosion est ce qu'il a trouve. Trois cas, comme l'original :
 *
 * <ul>
 * <li>un corps vivant : le centre est a sa hauteur d'yeux ;</li>
 * <li>un bloc : le centre est <b>aux coordonnees du bloc</b>, pas sur sa face ;</li>
 * <li>rien : le centre est a quatre blocs devant le joueur, au bout du regard.</li>
 * </ul>
 *
 * <p>Le rayon s'arrete au plus proche des deux : viser une bete derriere une vitre explose
 * la vitre, pas la bete.
 *
 * <h2>Ce que l'explosion fait</h2>
 *
 * Elle projette <b>toutes</b> les entites a moins de trois blocs du centre — pas seulement
 * celles qui vivent : un objet au sol part comme le reste, ce que l'original faisait deja.
 * Puis elle casse les blocs du cube, dans une sphere de rayon 6 en distance au carre, avec
 * deux chances sur trois de tenter chaque bloc a faible experience, huit sur dix a la
 * meilleure. La durete qu'elle accepte de briser grandit par paliers : 2,9 puis 25 puis 55,
 * c'est-a-dire tout ce qui est cassable — la progression est la meme que celle de
 * l'acceleration de vecteur, a ceci pres qu'ici elle ne <b>casse</b> rien de dur avant, elle
 * ne le supporte simplement pas.
 *
 * <h2>Une cinquieme coquille de l'original</h2>
 *
 * L'original calculait une poussee de 1,2 — la meme fonction que le choc dirige — puis
 * l'ecrasait une ligne plus bas avec une bousculade de 0,24, parce que son {@code setMotion}
 * <b>ecrase</b> le mouvement au lieu de l'ajouter, la ou le choc dirige le modifiait sur
 * place. Le client, lui, rejouait la poussee pour que le rendu suive : l'intention etait
 * bien de projeter, et le port garde donc les deux — la poussee de 1,2 <b>et</b> la
 * bousculade de 0,24 qui vient s'y ajouter, comme le choc dirige.
 *
 * <p>Non porte : l'onde visuelle envoyee au client, et le son.
 */
public class DirectedBlastwaveSkill extends Skill {

    /** La touche se tient, et la fenetre est celle du choc dirige : 6 a 50 ticks. */
    public static final int MIN_TICKS = 6;
    public static final int MAX_TICKS = 50;

    /** Le rayon de visee : quatre blocs, comme l'original. */
    public static final double REACH = 4.0;

    /** Le cote du cube de l'explosion : trois blocs de part et d'autre du centre. */
    public static final int BLAST_RANGE = 3;

    /** La sphere, en distance au carre : {@code distSq <= 6}. */
    public static final int BLAST_RADIUS_SQ = 6;

    /**
     * L'original allait de moins trois a <b>plus deux</b> sur chaque axe.
     *
     * Ce n'est pas une symetrie : sa boucle etait ecrite {@code (x - 3) until (x + 3)}, la
     * borne haute exclue. Le port la garde telle quelle — le centre reste le centre, mais le
     * cote positif du cube est ampute d'un bloc, et cela se verrait sur le relief laisse
     * derriere.
     */
    public static final int BLAST_LOW = -3;
    public static final int BLAST_HIGH = 2;

    /** Les degats : 10 a 25. */
    public static final float DAMAGE_MIN = 10f;
    public static final float DAMAGE_MAX = 25f;

    /** Le cout en CP : 160 a 200, comme l'original, et il monte avec l'experience. */
    public static final float CP_MIN = 160f;
    public static final float CP_MAX = 200f;

    /** Le surcout : 50 a 30, comme l'original. */
    public static final float OVERLOAD_MIN = 50f;
    public static final float OVERLOAD_MAX = 30f;

    /** Une chance sur deux de tenter chaque bloc, huit sur dix a la meilleure. */
    public static final float BREAK_PROB_MIN = 0.5f;
    public static final float BREAK_PROB_MAX = 0.8f;

    /** La part des blocs casses qui laissent leur butin. */
    public static final float DROP_RATE_MIN = 0.4f;
    public static final float DROP_RATE_MAX = 0.9f;

    /** Les paliers de durete acceptee, et celui ou ils changent. */
    public static final float HARDNESS_LOW = 2.9f;
    public static final float HARDNESS_MID = 25f;
    public static final float HARDNESS_HIGH = 55f;
    public static final float HARDNESS_STEP_1 = 0.25f;
    public static final float HARDNESS_STEP_2 = 0.5f;

    /** La poussee : 1,2, avec 0,4 de soulevement. */
    public static final double PUSH_LIFT = 0.4;
    public static final double PUSH_FORCE = 1.2;

    /** L'experience : plus quand la vague a trouve quelqu'un. */
    public static final float EXP_EFFECTIVE = 0.0025f;
    public static final float EXP_EMPTY = 0.0012f;

    public DirectedBlastwaveSkill() {
        // Le nom est celui de l'original : le fichier s'appelait DirectedBlastwave, la
        // competence "dir_blast".
        super("dir_blast", 3);
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

    public float breakProbability(AbilityData data) {
        return lerp(BREAK_PROB_MIN, BREAK_PROB_MAX, data.getSkillExp(this));
    }

    public float dropRate(AbilityData data) {
        return lerp(DROP_RATE_MIN, DROP_RATE_MAX, data.getSkillExp(this));
    }

    /** La recharge : de quatre secondes a deux et demie. */
    public int cooldown(AbilityData data) {
        return (int) lerp(80f, 50f, data.getSkillExp(this));
    }

    /**
     * La durete la plus elevee que l'onde accepte de briser, par paliers d'experience.
     *
     * Fonction pure, donc verifiable : c'est elle qui decide si l'onde ouvre une pierre
     * (1,5), une pierre taillee (2) ou une obsidienne (50). A faible experience elle
     * s'arrete a 2,9 ; a pleine experience, plus rien de cassable ne lui resiste.
     */
    public static float breakHardness(float exp) {
        if (exp < HARDNESS_STEP_1) return HARDNESS_LOW;
        if (exp < HARDNESS_STEP_2) return HARDNESS_MID;
        return HARDNESS_HIGH;
    }

    /** Vrai si la vague a trouve quelqu'un : c'est ce qui decide du gain d'experience. */
    public static float expGain(boolean effective) {
        return effective ? EXP_EFFECTIVE : EXP_EMPTY;
    }

    /** Le hasard des effets : l'original secouait son onde de quelques degres. */
    private static final RandomSource RANDOM = RandomSource.create();

    /** Un ecart au hasard, en degres, autour du regard. */
    private static float jitter(int degrees) {
        return RANDOM.nextFloat() * 2 * degrees - degrees;
    }

    // ------------------------------------------------------------------
    // Le coup
    // ------------------------------------------------------------------

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
     * La fenetre se referme a cinquante ticks, comme pour le choc dirige.
     *
     * L'original tolérait deux cents ticks de maintien pour ne rien en faire : relacher
     * apres cinquante ne declenchait rien du tout. Le port abandonne la charge au meme
     * moment, ce qui donne le meme resultat sans laisser un poing arme huit secondes.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        return chargeTicks < MAX_TICKS;
    }

    @Override
    public float getCpCost(AbilityData data) {
        return consumption(data);
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    @Override
    public int getCooldownTicks(AbilityData data) {
        return cooldown(data);
    }

    /** Tout est verse par le coup, qui seul sait s'il a trouve quelqu'un. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * Le centre de l'explosion, dans l'ordre de l'original.
     *
     * <p>Un corps vivant l'emporte sur un bloc s'il est plus proche, et un bloc l'emporte
     * sinon ; sans rien, le centre est au bout du regard. Le point n'est pas le meme selon
     * le cas, et c'est tout l'interet du rayon : une bete touchée place le centre a ses
     * <b>yeux</b>, un bloc touche le place a ses <b>coordonnees</b> — donc au coin du bloc,
     * pas sur la face frappee.
     *
     * <p>L'entite qui sert au rayon est vivante, comme le selecteur de l'original ; celles
     * qui seront projetees, elles, ne le sont pas forcement (voir {@link #blast}).
     */
    public Vec3 targetPoint(Player player) {
        Vec3 eye = player.getEyePosition(1f);
        Vec3 look = player.getViewVector(1f);

        Entity found = TargetingUtil.findEntityAlong(player, eye, eye.add(look.scale(REACH)),
                e -> e instanceof LivingEntity);
        BlockHitResult block = TargetingUtil.findBlockInSight(player, REACH);

        if (found != null) {
            double toEntity = found.getEyePosition().distanceToSqr(eye);
            double toBlock = block == null ? Double.MAX_VALUE : block.getLocation().distanceToSqr(eye);
            if (toEntity <= toBlock) return found.getEyePosition();
        }
        if (block != null) return Vec3.atLowerCornerOf(block.getBlockPos());
        return TargetingUtil.fallbackPoint(player, REACH);
    }

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        if (!(player.level() instanceof ServerLevel level)) return;
        blast(level, player, data, targetPoint(player));
    }

    /** Le coup lui-meme : ce que l'onde trouve autour de son centre. */
    private void blast(ServerLevel level, Player player, AbilityData data, Vec3 point) {
        boolean effective = false;

        // Le son part du centre de l'onde, comme dans l'original : c'est la seule des
        // competences de vecmanip qui se fait entendre dans le monde plutot qu'au joueur.
        cn.academy.sound.AcademySounds.playAt(level, point,
                cn.academy.ModSounds.VECMANIP_DIRECTED_BLAST, 0.5f, 1.0f);

        // Et l'onde elle-meme : deux ou trois anneaux qui s'ouvrent a sept dixiemes du chemin
        // entre la tete et le point vise, legerement de travers. C'est le geste de l'original,
        // qui tirait ces trois nombres au sort a chaque coup. Voir VecWaves.
        Vec3 head = player.getEyePosition(1f);
        cn.academy.ability.network.VecWavePacket.send(player,
                head.add(point.subtract(head).scale(0.7)),
                player.getYHeadRot() + jitter(20), player.getXRot() + jitter(10),
                2 + level.random.nextInt(2), 1);

        // D'abord les corps : tous ceux du cube, vivants ou non.
        AABB box = new AABB(point.x - BLAST_RANGE, point.y - BLAST_RANGE, point.z - BLAST_RANGE,
                point.x + BLAST_RANGE, point.y + BLAST_RANGE, point.z + BLAST_RANGE);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (entity == player) continue;
            if (entity instanceof LivingEntity living) {
                living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
            }
            project(player, entity);
            effective = true;
        }

        breakAround(level, player, data, point);

        data.addSkillExp(this, expGain(effective));
    }

    /**
     * Projette une entite : la poussee, la bousculade, et le decollage.
     *
     * <p>L'original appliquait les deux — la poussee de 1,2 et la bousculade de 0,24 — mais
     * la seconde ecrasait la premiere (voir l'entete de la classe) ; le port les ajoute.
     * La bousculade, elle, ne depend d'aucune experience : c'est elle qui fait bouger ce qui
     * ne se blesse pas.
     */
    private static void project(Player player, Entity entity) {
        Vec3 push = VecmanipPush.push(player.getEyePosition(), entity.getEyePosition(),
                PUSH_LIFT, PUSH_FORCE);
        Vec3 shove = VecmanipPush.shove(player.position(), entity.position());
        entity.setDeltaMovement(push.add(shove));
        // Le dixieme de bloc de l'original : sans lui, la poussee se perd dans la friction
        // du premier tick pour une cible collee au sol.
        entity.setPos(entity.getX(), entity.getY() + VecmanipPush.LIFT_OFF, entity.getZ());
    }

    /**
     * Le cube de terrain autour du centre.
     *
     * <p>Deux passes dans une seule, comme l'original : a pleine experience le bloc casse
     * laisse <b>son objet</b>, sans tirage — c'est ce qui distingue la maitrise — tandis
     * qu'en dessous il laisse ses butins normaux, seulement si le tirage le veut bien.
     */
    private void breakAround(ServerLevel level, Player player, AbilityData data, Vec3 point) {
        int x = (int) Math.round(point.x);
        int y = (int) Math.round(point.y);
        int z = (int) Math.round(point.z);

        float probability = breakProbability(data);
        float hardnessLimit = breakHardness(data.getSkillExp(this));
        boolean mastery = data.getSkillExp(this) >= 1f;

        for (int i = x + BLAST_LOW; i <= x + BLAST_HIGH; i++) {
            for (int j = y + BLAST_LOW; j <= y + BLAST_HIGH; j++) {
                for (int k = z + BLAST_LOW; k <= z + BLAST_HIGH; k++) {
                    int dx = i - x;
                    int dy = j - y;
                    int dz = k - z;
                    int distSq = dx * dx + dy * dy + dz * dz;
                    // Le centre y passe toujours ; les autres, une fois sur deux au debut.
                    if (distSq > BLAST_RADIUS_SQ) continue;
                    if (distSq != 0 && level.getRandom().nextFloat() >= probability) continue;

                    breakBlock(level, player, data, new BlockPos(i, j, k), hardnessLimit, mastery);
                }
            }
        }
    }

    /** Un bloc du cube : la durete decide, le butin suit. */
    private void breakBlock(ServerLevel level, Player player, AbilityData data, BlockPos pos,
                            float hardnessLimit, boolean mastery) {
        if (!cn.academy.Config.destroyBlocks) return;

        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0f || hardness > hardnessLimit) return;

        if (mastery) {
            // L'original donnait l'objet du bloc lui-meme, sans rien tirer au sort : c'est
            // la recompense de la maitrise. Un bloc sans objet (l'eau, par exemple) ne
            // donne rien — l'original aurait rendu une pile vide.
            ItemStack stack = new ItemStack(state.getBlock().asItem());
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        stack);
            }
        } else if (level.getRandom().nextFloat() < dropRate(data)) {
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY);
            for (ItemStack drop : drops) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            }
        }

        level.removeBlock(pos, false);
    }
}
