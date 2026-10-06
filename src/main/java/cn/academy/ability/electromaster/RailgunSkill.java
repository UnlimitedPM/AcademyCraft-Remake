package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Competence active, portage de {@code Railgun} : un tir tendu, a longue portee, qui traverse ce
 * qu'il trouve et le repousse violemment.
 *
 * <h2>Il ne part pas les mains vides</h2>
 *
 * <p>Portage de l'{@code ItemCoin} et du {@code Delegate} de l'original, qui est tout le rituel du
 * railgun. Il faut <b>lancer une piece</b> — clic droit avec l'objet {@code coin} — et tirer pendant
 * qu'elle <b>retombe</b>, plus de sept dixiemes de son vol (voir {@link CoinToss#READY}) : la piece
 * est alors consommee par le tir. L'autre voie de l'original est le <b>fer</b> — un lingot ou un bloc
 * en main —, egalement consomme. Sans l'un ou l'autre, l'appui ne fait <b>rien du tout</b>, pas meme
 * payer.
 *
 * <p>ECART ASSUME : l'original demandait vingt ticks de maintien pour la voie du fer (son
 * {@code chargeTicks = 20}, un decompte cote client), puis consommait le lingot. Le port tire
 * immediatement dans les deux cas : son systeme de touches ne connait que l'appui et le relachement,
 * et une arme qui met une seconde a partir pour un lingot en main n'apporte rien a la visee.
 */
public class RailgunSkill extends Skill {

    private static final float CP_COST_MIN_EXP = 200f;
    private static final float CP_COST_MAX_EXP = 450f;
    private static final double RANGE = 30;
    private static final double KNOCKBACK = 2.5;

    /** La longueur du rail, comme le rayon de l'original : quarante-cinq blocs. */
    private static final double BEAM_LENGTH = 45.0;

    /** Quinze arcs, comme ses {@code ARC_SIZE}. */
    private static final int BEAM_ARCS = 15;

    /**
     * La vie du tir, en ticks : cinquante, comme le {@code life} de l'original.
     *
     * <p>Le premier essai le faisait vivre quinze ticks — trois quarts de seconde, quand
     * l'original en tenait cinquante (deux secondes et demie). C'est la duree qui manquait le
     * plus au tir : le joueur l'a vue trop courte.
     *
     * <p>Chez lui le rayon s'estompait ensuite en deux temps : sa <b>largeur diminuait</b>
     * pendant 800 millisecondes ({@code widthShrinkTime}), puis il disparaissait en une seconde
     * ({@code blendOutTime}, avec l'entree en matiere a 150). Le port n'a pas encore de quoi
     * retrecir un arc pendant qu'il le dessine : c'est une retouche du moteur d'eclairs, a faire
     * avec sa teinte — voir {@code ArcRenderer}, dont chaque arc tire sa couleur de sa texture
     * et non d'un reglage.
     */
    private static final int BEAM_ARC_TICKS = 50;

    /** L'ecart lateral des arcs autour de l'axe, comme le sien (0,1 a 0,25). */
    private static final double BEAM_WOBBLE = 0.25;

    public RailgunSkill() {
        super("railgun", 4);
    }

    /**
     * Degats : de 110 a 160 selon l'experience.
     *
     * <p>ECART DEMANDE PAR LE JOUEUR : l'original fait 60 a 110, et c'est ce que le port portait.
     * Le railgun est une competence de niveau 4 a cible unique, et il doublait presque le claquement
     * d'orage devenu plus fort (voir {@code ThunderClapSkill.damage}) : la borne basse passe donc
     * au-dessus de la sienne, l'ecart entre les deux se lisant sur la portee (vingt-cinq contre
     * trente blocs) et sur la zone (une cible contre tout le rayon). Le reglage
     * {@code general.damageScale} adoucit toujours l'ensemble sans rien recompiler.
     * Le cout en CP suit l'original : 200 a 450, et il monte avec l'experience.
     */
    public float damage(AbilityData data) {
        return lerp(110f, 160f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 300 a 160 ticks, soit 15 a 8 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(300f, 160f, data.getSkillExp(this));
    }

    /**
     * Experience de l'original : 0,005 pour un tir, 0,01 s'il touche. Le paquet
     * d'activation ne sait pas si le tir a porte, donc c'est le montant du tir qui est
     * verse ; la part du coup au but reviendra avec les evenements de degats.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.005f;
    }

    /** Le cout en CP, repris de l'original : de 200 a 450 selon l'experience. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return CP_COST_MIN_EXP;
    }

    /**
     * Surcout repris de l'original : de 180 a 120 selon l'experience.
     *
     * C'est le tir qui remplit le plus la reserve : sur une reserve de 350 points,
     * deux tirs d'affilee suffisent a mettre le joueur en surcharge.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(180f, 120f, data.getSkillExp(this));
    }

    /**
     * Ce que le tir admet comme munition dans la main : le fer de l'original, tel quel.
     *
     * <p>{@code acceptedItems} valait le lingot et le bloc de fer, et rien d'autre — pas de fer en
     * poudre, pas de minerai. C'est le « ferraillage » de la competence : ce qu'on a sous la main
     * quand on n'a pas de piece.
     */
    public static boolean isAccepted(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.IRON_INGOT)
                || stack.is(net.minecraft.world.item.Items.IRON_BLOCK);
    }

    /**
     * Le tir a-t-il de quoi partir ?
     *
     * <p>La piece retombee d'abord, et le fer ensuite : c'est l'ordre de l'original, dont le
     * delegue regardait la piece en vol avant la main. Le refus est SILENCIEUX — voir
     * {@code ActivateSkillPacket}, qui ne facture rien quand cette question repond non.
     *
     * <p>C'est la seule competence instantanee qui refuse de partir : elle est le premier appelant
     * de ce crochet, qui servait aux maintiens (« y a-t-il un bloc a prendre ? »).
     */
    @Override
    public boolean canStart(Player player, AbilityData data) {
        return hasAmmo(player);
    }

    /** Une piece assez retombee, ou du fer en main. */
    public static boolean hasAmmo(Player player) {
        return cn.academy.entity.EntityCoinThrowing.ready(player) != null
                || isAccepted(player.getMainHandItem());
    }

    /**
     * Le tir consomme sa munition : la piece en vol, ou un fer de la main.
     *
     * <p>C'est le {@code MSG_COIN_PERFORM} de l'original — la piece meurt avant que le tir parte —
     * et son {@code MSG_ITEM_PERFORM} pour le fer, qui retirait un exemplaire sauf en creatif. Rien
     * n'est rendu : c'est le prix du tir, en plus de ses CP et de son surcout.
     */
    private static void consumeAmmo(Player player) {
        cn.academy.entity.EntityCoinThrowing coin = cn.academy.entity.EntityCoinThrowing.ready(player);
        if (coin != null) {
            coin.consume();
            return;
        }
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (isAccepted(held) && !player.getAbilities().instabuild) {
            held.shrink(1);
        }
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // La munition d'abord : le tir part de la piece, et l'original la tuait AVANT de tirer.
        consumeAmmo(player);

        // Le seul son de l'original qui se pose dans le monde plutot qu'au joueur : un tir de
        // railgun s'entend de loin. Il part a CHAQUE tir — le port ne le jouait qu'en touchant,
        // ce qui rendait muet le tir qui manque.
        cn.academy.sound.AcademySounds.playAt(player.level(), player.position(),
                cn.academy.ModSounds.EM_RAILGUN, 0.5f, 1.0f);

        shootBeam(player);

        Entity target = TargetingUtil.findEntityInSight(player, RANGE);
        if (!(target instanceof LivingEntity living)) return;

        living.hurt(player.damageSources().indirectMagic(player, player), scaled(damage(data)));
        Vec3 push = living.position().subtract(player.position()).normalize().scale(KNOCKBACK);
        living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.2, push.z));
        living.hurtMarked = true;
    }

    /**
     * Le rail : quinze arcs semes le long du tir, jusqu'a quarante-cinq blocs.
     *
     * <p>C'est ce que dessinait {@code EntityRailgunFX} : un rayon lumineux, et des arcs
     * gresillant le long de son axe, un tous les un a deux blocs, avec un petit ecart lateral
     * autour de la ligne. Le port n'a pas encore de rendu de rayon — il lui faudrait un trait
     * qu'il n'a pas — mais les arcs, eux, se font avec le moteur d'eclairs qu'il a deja, et
     * c'est eux qui donnent au tir son cote electrique.
     */
    private static void shootBeam(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        net.minecraft.util.RandomSource random = player.getRandom();

        // Le faisceau d'abord : le trait lumineux que tout le reste entoure. C'est un CYLINDRE,
        // pas un ruban — voir ArcRenderer, ou les siens sont dessines.
        //
        // Il part des YEUX, comme dans l'original, et va jusqu'au bout de la portee du tir.
        sendBeam(player, eye, eye.add(look.scale(BEAM_LENGTH)), BEAM_ARC_TICKS);

        for (int i = 0; i < BEAM_ARCS; i++) {
            double start = 1.0 + i * (BEAM_LENGTH - 1.0) / BEAM_ARCS;
            double end = Math.min(BEAM_LENGTH, start + 1.5 + random.nextDouble() * 1.5);
            // Les arcs ne sont pas SUR l'axe : ils gresillent autour, comme les siens. Un rail
            // parfaitement droit ne ressemblerait a rien.
            //
            // ECART ASSUME, et c'est un bug de l'original que le joueur a remarque : chez lui
            // les arcs partaient toujours vers l'est, quelle que soit la visee — sa fabrique
            // les generait le long de l'axe X, et seule leur POSITION etait tournee. Ici les
            // deux bouts sont pris sur la visee, donc les arcs suivent le faisceau.
            Vec3 from = wobble(eye.add(look.scale(start)), random);
            Vec3 to = wobble(eye.add(look.scale(end)), random);
            sendArc(player, cn.academy.ability.client.arc.ArcPattern.RAILGUN.name(), from, to,
                    BEAM_ARC_TICKS);
        }
    }

    /** Un point du rail, ecarte d'un rien de l'axe : la demi-borne laterale de l'original. */
    private static Vec3 wobble(Vec3 point, net.minecraft.util.RandomSource random) {
        return point.add(wobble(random), wobble(random), wobble(random));
    }

    /** Une composante de cet ecart, entre moins et plus {@code BEAM_WOBBLE}. */
    private static double wobble(net.minecraft.util.RandomSource random) {
        return (random.nextDouble() - 0.5) * BEAM_WOBBLE * 2.0;
    }

    /**
     * Envoie le faisceau lui-meme : un cylindre, que le client dessine.
     *
     * <p>Le drapeau du paquet dit « ceci est un faisceau », donc le serveur n'a rien a savoir
     * de sa forme — voir {@code ArcEffectPacket.beam}.
     */
    private static void sendBeam(Player player, Vec3 from, Vec3 to, int ticks) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                cn.academy.ability.network.ArcEffectPacket.beam(from, to, ticks, player.getId()));
    }

    /**
     * Envoie un arc a tous ceux qui voient le tireur.
     *
     * <p>Le motif voyage par son nom, donc le serveur n'a rien a connaitre du rendu — c'est le
     * message d'effet de l'original, celui qui fait qu'on voit le tir venir.
     */
    private static void sendArc(Player player, String pattern, Vec3 from, Vec3 to, int ticks) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                new cn.academy.ability.network.ArcEffectPacket(pattern, from, to, ticks, false,
                        player.getId()));
    }
}
