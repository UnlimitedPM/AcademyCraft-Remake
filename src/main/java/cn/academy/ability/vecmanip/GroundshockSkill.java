package cn.academy.ability.vecmanip;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.util.Plotter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * L'onde de choc, portage de {@code Groundshock} : le joueur frappe le sol et une tranche
 * de terrain s'effondre devant lui, dans l'axe de son regard.
 *
 * <p>L'onde part des <b>pieds</b>, pas de la main, et elle a deux ressources : une
 * <i>energie</i> de chantier, depensee bloc par bloc, et un <i>nombre de pas</i>. Chaque
 * pas avance d'un bloc, ecrase une bande de cinq colonnes de large (le centre, puis une
 * de chaque cote, puis deux de chaque cote, de moins en moins souvent), et tente de
 * casser les trois blocs au-dessus du marcheur. La pierre devient de la pierre taillee et
 * coute 0,4, l'herbe devient de la terre et coute 0,2, la terre grasse ne bouge pas et
 * coute 0,1, le reste coute 0,5 — et un corps dans la bande coute 1, prend les degats, et
 * part en l'air.
 *
 * <h2>Le sol, le prix, et ce qui n'arrive pas</h2>
 *
 * Rien ne part si le joueur n'est pas <b>au sol</b>, et rien n'est facture dans ce cas :
 * l'original apres avoir verifie le sol, le port paie donc dans l'effet plutot qu'a
 * l'activation — comme la teleportation a la marque, dont la touche n'allume rien. La
 * recharge non plus n'est posee par l'effet que s'il a eu lieu (voir
 * {@link #getCooldownTicks}).
 *
 * <p>La touche se tient, avec un minimum de cinq ticks : relacher avant, rien. Il n'y a
 * pas de maximum — l'original laissait tenir aussi longtemps qu'on voulait, et frappait
 * toujours.
 *
 * <h2>A pleine experience, le sol entier y passe</h2>
 *
 * A exactement 1,0 d'experience, l'onde fait une seconde passe : tout ce qui, dans un
 * carre de dix blocs de cote sur deux de haut, a une durete inferieure ou egale a 0,6 est
 * casse <b>avec</b> ses butins — terres, sables, graviers. C'est le seul moment ou
 * l'onde ramasse quelque chose : pendant la marche, l'original cassait sans rien laisser.
 *
 * <h2>Deux details de l'original corriges</h2>
 *
 * <ul>
 * <li>son son de destruction etait joue meme sur de l'air — trois blocs de haut, cinq
 *     colonnes de large, a chaque pas : des dizaines de sons par coup, pour rien. Le port
 *     ne le joue que lorsqu'un bloc disparait vraiment, ce qui est l'intention ;</li>
 * <li>un regard pile a la verticale n'a plus d'horizon a suivre, et son marcheur levait
 *     une exception. Le port prend alors la direction du nord, comme les autres
 *     competences du port qui se posent la meme question.</li>
 * </ul>
 *
 * <p>Non porte : le regard du joueur que l'original faisait plonger pendant la charge
 * (un retour visuel cote client, sans crochet pour le reproduire ici), et les particules
 * de poussiere envoyees au client pour chaque bloc affecte.
 */
public class GroundshockSkill extends Skill {

    /** En dessous, le coup ne part pas : {@code localTick >= 5} chez l'original. */
    public static final int MIN_TICKS = 5;

    /** Un bloc du sol sur trois est reellement casse : {@code groundBreakProb}. */
    public static final double GROUND_BREAK_PROB = 0.3;

    /** Demi-cote du carre de la passe finale, et sa hauteur. */
    public static final int SWEEP_HORIZONTAL = 5;
    public static final int SWEEP_DOWN = 1;
    public static final int SWEEP_UP = 1;

    /** La passe finale ne casse que le sol meuble : {@code breakHardness}. */
    public static final float SWEEP_MAX_HARDNESS = 0.6f;

    /** Ce que coute chaque genre de bloc ecrase, et chaque corps touche. */
    public static final double STONE_COST = 0.4;
    public static final double GRASS_COST = 0.2;
    public static final double FARMLAND_COST = 0.1;
    public static final double BLOCK_COST = 0.5;
    public static final double ENTITY_COST = 1.0;

    /** Les cinq colonnes de chaque pas, en multiples de la perpendiculaire au regard. */
    public static final int[] LATERAL_STEPS = {0, 1, -1, 2, -2};

    /** Et leur chance de tomber : le centre toujours, les bords une fois sur trois. */
    public static final double[] LATERAL_CHANCES = {1.0, 0.7, 0.7, 0.3, 0.3};

    /** Le corps touche part d'en bas : {@code rangef(0.6, 0.9) x lerpf(0.8, 1.3)}. */
    public static final double Y_SPEED_BASE = 0.6;
    public static final double Y_SPEED_SPAN = 0.3;
    public static final float Y_SPEED_EXP_MIN = 0.8f;
    public static final float Y_SPEED_EXP_MAX = 1.3f;

    /** 0,002 par corps touche, 0,001 pour le coup lui-meme. */
    public static final float ENTITY_EXP = 0.002f;
    public static final float CAST_EXP = 0.001f;

    /** Les trois blocs tentes au-dessus du marcheur, a chaque pas. */
    private static final int COLUMN_HEIGHT = 3;

    public GroundshockSkill() {
        super("ground_shock", 1);
    }

    // ------------------------------------------------------------------
    // Courbes, reprises de l'original
    // ------------------------------------------------------------------

    /** L'energie de chantier : 60 au depart, 120 a pleine experience. */
    public double energy(AbilityData data) {
        return lerp(60f, 120f, data.getSkillExp(this));
    }

    /** Les degats : 4 a 6. */
    public float damage(AbilityData data) {
        return lerp(4f, 6f, data.getSkillExp(this));
    }

    /** Le cout en CP : 80 a 150, comme l'original. */
    public float consumption(AbilityData data) {
        return lerp(80f, 150f, data.getSkillExp(this));
    }

    /** Le surcout : 15 a 10, comme l'original. */
    public float overload(AbilityData data) {
        return lerp(15f, 10f, data.getSkillExp(this));
    }

    /** Le nombre de pas de la marche : 10 a 25. */
    public int maxIterations(AbilityData data) {
        return (int) lerp(10f, 25f, data.getSkillExp(this));
    }

    /** La part des blocs casses qui laissent leur butin, dans la passe finale. */
    public float dropRate(AbilityData data) {
        return lerp(0.3f, 1.0f, data.getSkillExp(this));
    }

    /** La vitesse verticale donnee a un corps touche, pour un tirage dans [0, 1). */
    public double ySpeed(AbilityData data, double random) {
        return (Y_SPEED_BASE + Y_SPEED_SPAN * random)
                * lerp(Y_SPEED_EXP_MIN, Y_SPEED_EXP_MAX, data.getSkillExp(this));
    }

    /** La recharge qu'un coup pose : de quatre secondes a deux. */
    public int cooldown(AbilityData data) {
        return (int) lerp(80f, 40f, data.getSkillExp(this));
    }

    /** Tout est verse par le coup : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    // ------------------------------------------------------------------
    // Charge
    // ------------------------------------------------------------------

    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public int getMinChargeTicks(AbilityData data) {
        return MIN_TICKS;
    }

    /** Aucun maximum : l'original frappait quel que soit le temps tenu. */
    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return 0;
    }

    /**
     * Aucun cout a l'appui, aucun a l'activation.
     *
     * Le prix se paie dans l'effet, apres le controle du sol : l'original ne consommait
     * rien du tout quand le joueur etait en l'air, et cela ne peut se decider qu'ici. Voir
     * {@link Skill#paysOnEffect()} — sans quoi le paquet paierait le surcout avant que
     * l'onde ait pu constater qu'elle ne partait pas.
     */
    @Override
    public boolean paysOnEffect() {
        return true;
    }

    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public float getOverloadCost(AbilityData data) {
        return overload(data);
    }

    /** La recharge est posee par le coup : voir {@link #cooldown}. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    // ------------------------------------------------------------------
    // L'effet
    // ------------------------------------------------------------------

    @Override
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        // Le sol d'abord : en l'air, il ne se passe rien et rien n'est facture.
        if (!player.onGround()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!data.perform(consumption(data), overload(data))) return;

        new Shock(level, player, data, this).cast();

        // Le volume 2 de l'original : l'onde de choc est la competence la plus bruyante
        // de la categorie, et c'est voulu.
        cn.academy.sound.AcademySounds.playFor(player,
                cn.academy.ModSounds.VECMANIP_GROUNDSHOCK, 2f);

        data.addSkillExp(this, CAST_EXP);
        data.setCooldown(this, cooldown(data));
    }

    /**
     * La direction de la marche : le regard du joueur, ramene a l'horizontale.
     *
     * <p>L'onde suit le lacet du regard et ignore son inclinaison : viser le sol plus loin
     * ne change pas la longueur du chemin, seulement celui des cinq rangs. Fonction pure,
     * donc verifiable — et le cas d'un regard pile a la verticale y est traite : il n'y a
     * plus d'horizon, le nord prend le relais.
     */
    public static Vec3 walkDirection(Vec3 look) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /**
     * Le decalage lateral d'une des cinq colonnes d'un rang.
     *
     * <p>Portage de {@code Vec3d.rotateYaw(planeLook, 90)} multiplie par le pas : la
     * perpendiculaire au regard, gardee dans le plan des trois coordonnees — donc
     * <b>inclinee avec le regard</b>, puisque la composante verticale est conservee. Un
     * joueur qui regarde vers le bas deplace donc ses rangs vers le bas.
     */
    public static Vec3 lateralDelta(Vec3 look, int index) {
        Vec3 rot = rotateYaw(look, Math.PI / 2);
        return rot.scale(LATERAL_STEPS[index]);
    }

    /** La cellule visee par une des cinq colonnes, depuis le rang courant. */
    public static BlockPos lateralCell(BlockPos row, Vec3 look, int index) {
        Vec3 delta = lateralDelta(look, index);
        return new BlockPos(
                (int) Math.floor(row.getX() + delta.x),
                (int) Math.floor(row.getY() + delta.y),
                (int) Math.floor(row.getZ() + delta.z));
    }

    /** Rotation d'un vecteur autour de la verticale, comme {@code Vec3d.rotateYaw}. */
    private static Vec3 rotateYaw(Vec3 v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos + v.z * sin, v.y, v.z * cos - v.x * sin);
    }

    /**
     * Une onde en cours de route.
     *
     * <p>Ce que l'original gardait dans les locales de son {@code s_perform} : l'energie
     * qui reste, le nombre de pas, les blocs deja vus et les corps deja frappes. Les deux
     * ensembles servent au meme chose — ne rien faire deux fois — et le corps ne doit pas
     * etre frappe deux fois non plus, meme si son bloc est traverse par plusieurs colonnes.
     */
    private static final class Shock {

        private final ServerLevel level;
        private final Player player;
        private final AbilityData data;
        private final GroundshockSkill skill;
        private final RandomSource random;

        /**
         * Le regard, <b>tel quel</b> : c'est lui qui incline les colonnes.
         *
         * <p>La marche, elle, ne prend que son lacet (voir {@link #walkDirection}) — deux
         * lectures differentes du meme vecteur, comme dans l'original, ou le marcheur
         * recevait un vecteur aplati et les colonnes la perpendicularite du vecteur
         * entier.
         */
        private final Vec3 look;

        private final double dropRate;
        private final double ySpeed;
        private final Set<BlockPos> seen = new HashSet<>();
        private final Set<Entity> struck = new HashSet<>();
        private double energy;

        /** Les pas qui restent : l'original avait un compteur local, ici le meme. */
        private int iterations;

        Shock(ServerLevel level, Player player, AbilityData data, GroundshockSkill skill) {
            this.level = level;
            this.player = player;
            this.data = data;
            this.skill = skill;
            this.random = player.getRandom();
            this.look = player.getLookAngle();
            this.energy = skill.energy(data);
            this.iterations = skill.maxIterations(data);
            this.dropRate = skill.dropRate(data);
            this.ySpeed = skill.ySpeed(data, random.nextDouble());        }

        void cast() {
            walk();
            // Tout ce qui reste est depense d'un coup : la passe finale ne compte plus.
            energy = Double.MAX_VALUE;
            if (data.getSkillExp(skill) >= 1f) {
                sweep();
            }
        }

        /** La marche : un pas par iteration, tant qu'il reste de l'energie et des pas. */
        private void walk() {
            Vec3 ahead = walkDirection(look);
            Plotter plotter = new Plotter(player.getBlockX(), player.getBlockY() - 1,
                    player.getBlockZ(), ahead.x, 0, ahead.z);

            while (energy > 0 && iterations > 0) {
                BlockPos row = plotter.next();
                iterations--;

                for (int i = 0; i < LATERAL_STEPS.length; i++) {
                    lateral(row, i);
                }
            }
        }

        /**
         * Une des cinq colonnes d'un pas.
         *
         * <p>L'ordre de l'original est garde, y compris ce qui surprend : le bloc qui part
         * en morceaux quand le sort tombe bien est celui du <b>rang</b>, pas la colonne
         * qu'on vient d'ecraser. C'est la que le sol se creuse sous les pas du marcheur.
         */
        private void lateral(BlockPos row, int index) {
            BlockPos cell = lateralCell(row, look, index);

            if (random.nextDouble() < LATERAL_CHANCES[index]
                    && !level.getBlockState(cell).isAir() && seen.add(cell)) {
                energy -= flatten(cell);
                if (random.nextDouble() < GROUND_BREAK_PROB) {
                    breakAt(row, false);
                }
                strike(row);
            }

            // Les trois blocs au-dessus du marcheur, a chaque colonne et sans tirage : une
            // onde qui avance ne laisse pas de mur derriere elle.
            for (int up = 1; up <= COLUMN_HEIGHT; up++) {
                breakAt(row.above(up), false);
            }
        }

        /**
         * Ecraser un bloc sans le casser : la pierre devient de la pierre taillee, l'herbe
         * de la terre, la terre grasse ne bouge pas. Rend ce que cela coute en energie.
         */
        private double flatten(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.STONE)) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                return STONE_COST;
            }
            if (state.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                return GRASS_COST;
            }
            if (state.is(Blocks.FARMLAND)) {
                return FARMLAND_COST;
            }
            return BLOCK_COST;
        }

        /**
         * Casser un bloc, si l'energie le permet.
         *
         * <p>Portage de {@code breakWithForce} : la durete du bloc se paie, les blocs
         * increvables et les liquides sont ignores, et celui qui disparait laisse son
         * butin seulement si on le demande. Le port ne joue le son que lorsqu'un bloc
         * disparait vraiment — l'original le jouait aussi sur de l'air, des dizaines de
         * fois par coup (voir l'entete de la classe).
         */
        private void breakAt(BlockPos pos, boolean drop) {
            if (!cn.academy.Config.destroyBlocks) return;

            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FARMLAND) || !state.getFluidState().isEmpty()) return;

            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0f || energy < hardness) return;
            energy -= hardness;

            if (state.isAir()) return;

            if (drop && random.nextDouble() < dropRate) {
                List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY);
                for (ItemStack stack : drops) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }

            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.ANVIL_DESTROY, SoundSource.AMBIENT, 0.5f, 1f);
        }

        /** Ceux qui se trouvent dans le rang, une seule fois chacun. */
        private void strike(BlockPos row) {
            AABB box = new AABB(row.getX() - 0.2, row.getY() - 0.2, row.getZ() - 0.2,
                    row.getX() + 1.4, row.getY() + 2.2, row.getZ() + 1.4);

            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (entity == player || !struck.add(entity)) continue;
                energy -= ENTITY_COST;
                entity.hurt(player.damageSources().indirectMagic(player, player),
                        scaled(skill.damage(data)));
                entity.setDeltaMovement(entity.getDeltaMovement().x, ySpeed,
                        entity.getDeltaMovement().z);
                data.addSkillExp(skill, ENTITY_EXP);
            }
        }

        /**
         * La passe de maitrise : a pleine experience, tout le sol meuble du carre y passe.
         *
         * <p>C'est la seule fois ou l'onde ramasse : les blocs casses pendant la marche ne
         * laissaient rien, comme dans l'original.
         */
        private void sweep() {
            int x0 = player.getBlockX();
            int y0 = player.getBlockY();
            int z0 = player.getBlockZ();

            for (int x = x0 - SWEEP_HORIZONTAL; x < x0 + SWEEP_HORIZONTAL; x++) {
                for (int y = y0 - SWEEP_DOWN; y < y0 + SWEEP_UP; y++) {
                    for (int z = z0 - SWEEP_HORIZONTAL; z < z0 + SWEEP_HORIZONTAL; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (level.getBlockState(pos).getDestroySpeed(level, pos) <= SWEEP_MAX_HARDNESS) {
                            breakAt(pos, true);
                        }
                    }
                }
            }
        }
    }
}
