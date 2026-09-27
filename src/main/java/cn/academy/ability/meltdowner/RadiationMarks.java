package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Les marques de radiation : portage de {@code MDDamageHelper}.
 *
 * <p>Une cible touchee par un tir du meltdowner, quand le joueur a appris Radiation Intensify,
 * porte une marque de soixante ticks. Tant qu'elle la porte, <b>tout</b> degat qu'elle encaisse
 * est multiplie par le facteur du passif — d'ou qu'il vienne, comme dans l'original : un zombie
 * marque prend aussi plus cher d'une epee.
 *
 * <p>La marque vit dans les donnees persistantes de l'entite, sous les cles de l'original
 * ({@code md_marktick} et {@code md_markrate}), donc elle survit a un rechargement de chunk et
 * n'a pas besoin d'une seconde donnee a maintenir.
 *
 * <p>L'original l'accompagnait de particules chez le client ; le port n'en a jamais portees, et
 * le joueur voit donc le resultat (la cible tombe plus vite) sans la fumee.
 */
public final class RadiationMarks {

    /** Les cles de l'original, telles quelles. */
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
        RadiationIntensifySkill rad = MeltdownerCategory.RADIATION_INTENSIFY;
        if (rad == null || !data.isSkillLearned(rad)) return;

        int ticks = Math.max(RadiationIntensifySkill.MARK_TICKS, ticksLeft(target));
        target.getPersistentData().putInt(MARK_TICK, ticks);
        target.getPersistentData().putFloat(MARK_RATE, rad.rate(data));
    }

    /** La marque avance d'un tick. Sans marque, elle ne fait rien du tout. */
    public static void tick(Entity entity) {
        if (entity == null) return;
        int left = ticksLeft(entity);
        if (left > 0) entity.getPersistentData().putInt(MARK_TICK, left - 1);
    }

    /** La cible porte-t-elle une marque encore vivante ? */
    public static boolean isMarked(Entity entity) {
        return entity != null && ticksLeft(entity) > 0;
    }

    /** Le facteur de degats d'une cible marquee ; 0 quand elle ne l'est pas. */
    public static float rate(Entity entity) {
        return entity == null ? 0f : entity.getPersistentData().getFloat(MARK_RATE);
    }

    /** Les ticks de marque restants. */
    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : entity.getPersistentData().getInt(MARK_TICK);
    }

    /**
     * Applique la marque a un coup recu : le facteur multiplie les degats, comme le faisait
     * l'evenement {@code LivingHurtEvent} de l'original.
     */
    public static void apply(LivingHurtEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(event.getAmount() * rate(event.getEntity()));
        }
    }

    /** Le passif, ou {@code null} si sa categorie n'est pas chargee. */
    public static Skill passive() {
        return MeltdownerCategory.RADIATION_INTENSIFY;
    }
}
