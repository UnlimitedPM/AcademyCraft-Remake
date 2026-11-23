package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.BlockBreaking;
import cn.academy.ability.Skill;
import cn.academy.ability.network.RadiationMarkPacket;
import cn.academy.platform.network.Network;
import net.minecraft.world.entity.Entity;

/**
 * Les marques de radiation : portage de {@code MDDamageHelper}.
 *
 * <p>Une cible touchee par un tir du meltdowner, quand le joueur a appris Radiation Intensify,
 * porte une marque de soixante ticks. Tant qu'elle la porte, <b>tout</b> degat qu'elle encaisse
 * est multiplie par le facteur du passif — d'ou qu'il vienne, comme dans l'original : un zombie
 * marque prend aussi plus cher d'une epee.
 *
 * <p>La marque vit dans les <b>tags</b> de l'entite, sous les noms de l'original
 * ({@code md_marktick} et {@code md_markrate}). Forge la gardait dans ses donnees persistantes :
 * les tags sont sauvegardes dans le NBT de la meme facon, et ils existent sur les trois
 * chargeurs, donc elle survit toujours a un rechargement de chunk.
 *
 * <p>L'original l'accompagnait de particules chez le client, et le port aussi : la cible marquee
 * fume, un essaim de plasma pose sur son corps — voir {@code RadiationMarksEffect}, qui se joue chez
 * ceux qui VOIENT la cible, la ou le paquet de la marque les annonce. Cette javadoc a longtemps dit
 * le contraire : elle etait fausse.
 */
public final class RadiationMarks {

    /** Les noms de l'original, tels quels. */
    private static final String MARK_TICK = "md_marktick";
    private static final String MARK_RATE = "md_markrate";

    private RadiationMarks() {}

    /**
     * Marque une cible qu'un tir vient de toucher.
     *
     * <p>A appeler <b>apres</b> le coup : la marque ne compte pas pour lui-meme, seulement pour
     * ce qui suit — c'est ce que faisait l'original, qui marquait apres avoir frappe. Une cible
     * deja marquee garde le temps qui lui restait s'il est plus long.
     */
    public static void mark(Entity target, AbilityData data) {
        if (target == null || data == null) return;
        RadiationIntensifySkill rad = passive();
        if (rad == null || !data.isSkillLearned(rad)) return;

        int ticks = Math.max(RadiationIntensifySkill.MARK_TICKS, ticksLeft(target));
        setTicks(target, ticks);
        put(target, MARK_RATE, Float.toString(rad.rate(data)));

        // Et la marque s'annonce a ceux qui voient la cible : c'est le message {@code sync} de
        // l'original, et c'est lui qui fait fumer le plasma chez les voisins du tireur. Sans ce
        // paquet, la fumee ne se verrait que chez celui qui a tire — le seul interet du passif a
        // l'ecran serait perdu pour tout le monde.
        if (!target.level().isClientSide) {
            Network.sendToTracking(new RadiationMarkPacket(target.getId(), ticks), target);
        }
    }

    /**
     * Pose la duree de la marque, telle quelle.
     *
     * <p>Deux appelants : le serveur qui la pose, et le client qui vient de l'apprendre par le
     * paquet de la marque. Les deux ecrivent la <b>meme</b> cle, celle de l'original, si bien que
     * tout ce qui lit la marque — {@link #isMarked}, {@link #ticksLeft} — marche des deux cotes.
     */
    public static void setTicks(Entity target, int ticks) {
        if (target != null) put(target, MARK_TICK, Integer.toString(ticks));
    }

    /** La marque avance d'un tick. Sans marque, elle ne fait rien du tout. */
    public static void tick(Entity entity) {
        if (entity == null) return;
        int left = ticksLeft(entity);
        if (left > 0) put(entity, MARK_TICK, Integer.toString(left - 1));
    }

    /** La cible porte-t-elle une marque encore vivante ? */
    public static boolean isMarked(Entity entity) {
        return entity != null && ticksLeft(entity) > 0;
    }

    /** Le facteur de degats d'une cible marquee ; 0 quand elle ne l'est pas. */
    public static float rate(Entity entity) {
        if (entity == null) return 0f;
        try {
            String value = read(entity, MARK_RATE);
            return value == null ? 0f : Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // Un tag abime (main, datapack, autre mod) ne doit pas faire tomber le tick.
            return 0f;
        }
    }

    /** Les ticks de marque restants. */
    public static int ticksLeft(Entity entity) {
        if (entity == null) return 0;
        try {
            String value = read(entity, MARK_TICK);
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Applique la marque a un coup recu : le facteur multiplie les degats, comme le faisait
     * l'evenement {@code LivingHurtEvent} de l'original.
     *
     * <p>Les degats passent en parametre et ressortent multiplies : le code commun ne connait ni
     * l'evenement Forge ni son equivalent, il ne fait que la regle.
     */
    public static float applyAmount(Entity target, float amount) {
        return isMarked(target) ? amount * rate(target) : amount;
    }

    /** Le passif, ou {@code null} si sa categorie n'est pas chargee. */
    public static RadiationIntensifySkill passive() {
        // Par cle plutot que par la constante de la categorie : ce fichier est commun, et la
        // categorie du meltdowner ne s'y nomme pas.
        Skill skill = BlockBreaking.byKey("meltdowner.rad_intensify");
        return skill instanceof RadiationIntensifySkill intensify ? intensify : null;
    }

    // ------------------------------------------------------------------
    // Le stockage : un tag par nombre
    // ------------------------------------------------------------------

    /** Ecrit un nombre dans les tags de l'entite, en remplacant l'ancien. */
    private static void put(Entity entity, String name, String value) {
        String previous = read(entity, name);
        if (value.equals(previous)) return;
        if (previous != null) entity.removeTag(name + ":" + previous);
        entity.addTag(name + ":" + value);
    }

    /** Lit un nombre dans les tags, ou {@code null} s'il n'y est pas. */
    private static String read(Entity entity, String name) {
        String prefix = name + ":";
        for (String tag : entity.getTags()) {
            if (tag.startsWith(prefix)) return tag.substring(prefix.length());
        }
        return null;
    }
}
