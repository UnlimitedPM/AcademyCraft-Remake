package cn.academy.ability.client.md;

import cn.academy.ability.Skill;
import cn.academy.ability.meltdowner.MineRayBasicSkill;
import cn.academy.ability.meltdowner.MineRayExpertSkill;
import cn.academy.ability.meltdowner.MineRayLuckSkill;
import cn.academy.ability.meltdowner.MineRaySkill;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Les trois rayons miniers, chez le client : le rayon tenu, et les etincelles.
 *
 * <p>C'est le portage de {@code MRContextC} et des trois entites qu'il posait —
 * {@code EntityMineRayBasic}, {@code EntityMineRayExpert} et {@code EntityMineRayLuck}. Leur
 * logique de minage est deja la, dans {@link MineRaySkill} ; ce qui manquait, c'etait ce qu'on
 * <b>voit</b>, et il se trouve que l'original le faisait entierement chez le client.
 *
 * <h2>Un rayon que personne d'autre ne voit</h2>
 *
 * <p>Les rayons du meltdowner et de la salve partent du serveur par un paquet, parce que tout le
 * monde doit les voir. Ceux-ci, non : le contexte client de l'original posait son entite dans
 * <b>son propre</b> monde, et c'est le sien qui la dessinait. Un rayon minier n'existe donc que
 * pour celui qui le tient, et le port fait pareil — aucune entite, aucun paquet. Le serveur mine
 * de son cote et ne sait rien de tout cela.
 *
 * <h2>Ou il se pose, et pourquoi il bouge</h2>
 *
 * <p>L'entite de l'original se reposait a chaque tick : elle partait des <b>yeux</b> de son
 * tireur et finissait quinze blocs plus loin sur son regard. Le port repose les deux bouts a
 * chaque <b>image</b> au lieu de chaque tick — c'est plus doux d'un vingtieme de seconde, et
 * c'est surtout ce que demande l'optimisation de vue, qui se recalcule de toute facon a chaque
 * image : un rayon tenu se recolle donc a la main en continu, et suit le regard sans train.
 *
 * <p>Sa longueur, elle, ne se repose qu'une fois : c'est celle de l'entite, quinze blocs, qui
 * n'est <b>pas</b> la portee de la competence — l'original creusait a dix blocs avec un rayon de
 * quinze pour le rayon de base, et a vingt avec le meme rayon de quinze pour les deux autres.
 * Le rayon depasse donc toujours ce qu'il creuse, et il finit dans la pierre.
 *
 * <h2>Deux sortes d'etincelles</h2>
 *
 * <p>Le rayon en jette le long de lui-meme — une chance sur deux par tick pour celui de base,
 * une sur trois pour les deux autres — et le bloc mine en recoit trois par tick, groupes sur
 * ses deux blocs de large. Ces dernieres <b>tombent</b> : l'original leur donnait la gravite de
 * sa {@code Rigidbody}, 0,01 bloc par tick au carre, et les laissait traverser le monde. Celles
 * du rayon, elles, continuent droit devant elles — c'est la meme fabrique, sans poids.
 *
 * <p>C'est le client qui choisit le bloc, et non le serveur : l'original envoyait ses trois
 * coordonnees par paquet a chaque tick de minage, ce que le port n'a pas a faire puisque le
 * client vise le meme bloc que lui — meme joueur, meme regard, meme monde. Cela lui evite aussi
 * de montrer des etincelles la ou le serveur a refuse, ce qui n'arrive que si la reserve du
 * joueur s'epuise, et le paquet de fin de maintien s'en charge alors.
 */
@OnlyIn(Dist.CLIENT)
public final class MineRayEffect {

    /** La portee du rayon DESSINE, en blocs : les quinze de l'entite de l'original. */
    public static final double BEAM_LENGTH = 15.0;

    /** La chance qu'a le rayon de base de lacher une etincelle, par tick. */
    public static final double BASIC_SPARK_CHANCE = 0.5;

    /** Et celle des deux autres — expert et chance — qui en lachent un peu plus souvent. */
    public static final double OTHER_SPARK_CHANCE = 0.6;

    /** L'etincelle du rayon se pose au hasard sur son regard, jusqu'a dix blocs. */
    public static final double SPARK_REACH = 10.0;

    /** Et elle derive de trois centimetres par tick, dans chaque sens. */
    public static final double SPARK_SPEED = 0.03;

    /** Les etincelles du bloc mine : trois par tick, comme l'original. */
    public static final int BLOCK_SPARKS = 3;

    /** Poses dans la boite du bloc, un peu debordante : de -0,2 a 1,2 bloc. */
    public static final double BLOCK_OFFSET_MIN = -0.2;
    public static final double BLOCK_OFFSET_MAX = 1.2;

    /** Elles partent de six centimetres par tick, et tombent — la gravite de l'original. */
    public static final double BLOCK_SPARK_SPEED = 0.06;
    public static final double BLOCK_SPARK_GRAVITY = 0.01;

    private static final RandomSource RANDOM = RandomSource.create();

    private MineRayEffect() {
    }

    /**
     * Un tick de rayon minier.
     *
     * <p>Le premier tick ouvre le rayon — sa naissance, et donc sa poussee de deux dixiemes de
     * seconde — et les suivants n'en font que les etincelles : la forme, elle, se repose a chaque
     * image, dans {@link #frame}.
     *
     * <p>Le genre vit dans {@link MdRays}, qui tient le rayon lui-meme, et non dans un champ a
     * cote : c'est ce qui fait qu'un monde quitte efface le rayon ET son genre du meme geste, et
     * que le prochain maintien en rouvre un — meme si c'est la meme competence.
     */
    public static void tick(Player player, Skill skill) {
        MdRayKind wanted = kindOf(skill);
        if (wanted == null) {
            // Une autre competence tient la touche : le rayon minier n'a plus rien a faire la.
            end();
            return;
        }
        if (wanted != MdRays.heldKind()) {
            MdRays.hold(wanted, player.getEyePosition(1f),
                    beamEnd(player, player.getEyePosition(1f)));
        }

        sowLookSpark(player, wanted);
        sowBlockSparks(player, (MineRaySkill) skill);
    }

    /**
     * Une image : le rayon se repose sur le regard de son tireur.
     *
     * <p>C'est ici que passe l'optimisation de vue de l'original — {@code MdRayView} — comme pour
     * les rayons nes du serveur : le rayon part des yeux et se dessine sur la main, et il se
     * replace donc a chaque image, exactement comme le faisait le rendu de l'original.
     */
    public static void frame(LocalPlayer player, float partialTick, double[] above,
                             boolean firstPerson) {
        MdRayKind current = MdRays.heldKind();
        if (current == null) return;

        Vec3 from = player.getEyePosition(partialTick);
        Vec3 to = beamEnd(player, from, partialTick);
        double[][] placed = MdRayView.place(current,
                new double[] { from.x, from.y, from.z },
                new double[] { to.x, to.y, to.z }, firstPerson, above);
        MdRays.moveHeld(new Vec3(placed[0][0], placed[0][1], placed[0][2]),
                new Vec3(placed[1][0], placed[1][1], placed[1][2]));
    }

    /** La competence vient de s'arreter : le rayon s'en va. */
    public static void end() {
        MdRays.releaseHeld();
    }

    /**
     * Le serveur dit que ce maintien est fini : le rayon s'en va, si c'est bien le sien.
     *
     * <p>Le nom est compare a celui du genre tenu, pour qu'un message en retard ne puisse pas
     * eteindre le rayon d'un autre rayon minier que le joueur vient d'ouvrir.
     */
    public static void end(String skillName) {
        MdRayKind current = MdRays.heldKind();
        if (current == null || skillName == null || !skillName.equals(skillOf(current))) return;
        end();
    }

    /**
     * Le genre d'un rayon minier, ou {@code null} si la competence n'en est pas un.
     *
     * <p>C'est la seule chose que le port a traduite du contexte de l'original, qui choisissait
     * son entite a la construction de son contexte client. Les nombres, eux, sont dans
     * {@link MdRayKind} : ici, on ne fait que dire lequel des trois.
     */
    @Nullable
    public static MdRayKind kindOf(Skill skill) {
        if (skill instanceof MineRayBasicSkill) return MdRayKind.MINE_BASIC;
        if (skill instanceof MineRayExpertSkill) return MdRayKind.MINE_EXPERT;
        if (skill instanceof MineRayLuckSkill) return MdRayKind.MINE_LUCK;
        return null;
    }

    /** Le nom de la competence d'un genre, quand elle en a un. */
    @Nullable
    public static String skillOf(MdRayKind kind) {
        if (kind == MdRayKind.MINE_BASIC) return "mine_ray_basic";
        if (kind == MdRayKind.MINE_EXPERT) return "mine_ray_expert";
        if (kind == MdRayKind.MINE_LUCK) return "mine_ray_luck";
        return null;
    }

    /**
     * La chance qu'a ce rayon de lacher une etincelle a ce tick.
     *
     * <p>Une sur deux pour celui de base, une sur trois pour les deux autres — c'est l'original,
     * dont les trois entites ne differaient pas que par leurs nombres.
     */
    public static double sparkChance(MdRayKind kind) {
        return kind == MdRayKind.MINE_BASIC ? BASIC_SPARK_CHANCE : OTHER_SPARK_CHANCE;
    }

    /**
     * L'image des etincelles de ce rayon.
     *
     * <p>La bille de plasma pour tout le monde, l'etoile de la chance pour le seul rayon de la
     * chance : c'est la seule difference de tout le plasma, et elle se lit ici.
     */
    public static ResourceLocation sparkTexture(MdRayKind kind) {
        return kind == MdRayKind.MINE_LUCK ? MdSparks.LUCK : MdSparks.PLAIN;
    }

    /** Le bout du rayon : quinze blocs devant les yeux, le long du regard. */
    private static Vec3 beamEnd(Player player, Vec3 from) {
        return beamEnd(player, from, 1f);
    }

    /** Le meme, avec les temps partiels du rendu — le regard tourne entre deux ticks. */
    private static Vec3 beamEnd(Player player, Vec3 from, float partialTick) {
        return from.add(player.getViewVector(partialTick).scale(BEAM_LENGTH));
    }

    /**
     * L'etincelle du rayon lui-meme : une, parfois, posee au hasard sur les dix premiers blocs
     * de son regard.
     *
     * <p>C'est l'{@code onUpdate} de l'entite de l'original, au chiffre pres — et c'est la seule
     * etincelle du rayon de la chance qui prenne son etoile : {@code md_particle_luck}, la ou les
     * deux autres crachent la bille ordinaire.
     */
    private static void sowLookSpark(Player player, MdRayKind kind) {
        if (RANDOM.nextDouble() >= sparkChance(kind)) return;

        Vec3 at = player.getEyePosition(1f)
                .add(player.getViewVector(1f).scale(RANDOM.nextDouble() * SPARK_REACH));
        MdSparks.spawn(at, spread(SPARK_SPEED), 0.0, sparkTexture(kind));
    }

    /**
     * Les trois etincelles du bloc mine, chaque tick, tant qu'il en reste a user.
     *
     * <p>Le bloc est celui que le rayon <b>peut</b> creuser : viser de la pierre avec un rayon
     * qu'elle arrete ne fait rien du tout, chez l'original comme ici. La durete negative — la
     * roche-mere, l'eau — en fait partie : le rayon n'en viendra jamais a bout, mais il la
     * mord, et c'est ce qu'on voit.
     */
    private static void sowBlockSparks(Player player, MineRaySkill mine) {
        BlockPos pos = MineRaySkill.aimedBlock(player, mine.range());
        if (pos == null) return;

        BlockState state = player.level().getBlockState(pos);
        if (!mine.canHarvest(state)) return;

        for (int i = 0; i < BLOCK_SPARKS; i++) {
            Vec3 at = new Vec3(pos.getX() + offset(), pos.getY() + offset(),
                    pos.getZ() + offset());
            MdSparks.spawn(at, spread(BLOCK_SPARK_SPEED), BLOCK_SPARK_GRAVITY, MdSparks.PLAIN);
        }
    }

    /** Une position dans la boite du bloc, un peu debordante des quatre cotes. */
    private static double offset() {
        return BLOCK_OFFSET_MIN + RANDOM.nextDouble() * (BLOCK_OFFSET_MAX - BLOCK_OFFSET_MIN);
    }

    /** Une vitesse tiree au hasard, bornee par celle de sa sorte d'etincelle. */
    private static Vec3 spread(double speed) {
        return new Vec3(random(speed), random(speed), random(speed));
    }

    private static double random(double bound) {
        return (RANDOM.nextDouble() * 2 - 1) * bound;
    }
}
