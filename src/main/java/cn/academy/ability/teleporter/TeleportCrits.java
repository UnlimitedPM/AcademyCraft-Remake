package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Les coups critiques des teleportations : portage de {@code TPSkillHelper}.
 *
 * <p>L'original en faisait un bonus de deux competences passives : <b>Dimension Folding
 * Theorem</b> (niveau 1) et <b>Space Fluctuation</b> (niveau 4) augmentent toutes les deux la
 * chance qu'un coup de teleporteur devienne critique. Le joueur l'a bien dit : Dimension
 * Folding n'est pas un pouvoir, c'est un bonus applique aux autres competences — et ce bonus
 * n'existait pas dans le port, ce qui la rendait inutile.
 *
 * <p>Trois paliers, essayes dans l'ordre, le premier qui passe emporte le coup : 1,3x avec les
 * deux seules competences de base, puis 1,6x et 2,6x, que Space Fluctuation ouvre a lui seul.
 * Chaque critique verse aussi de l'experience aux deux passives, ce qui est la seule facon de
 * les faire monter : elles ne s'activent jamais.
 *
 * <p>La partie qui se relit sans Minecraft ({@link #chance}, {@link #tier}, {@link #damage})
 * est pure, et prend les tirages en parametre : c'est ce qui la rend fiable en test, un tirage
 * au hasard ne se verifiant pas.
 */
public final class TeleportCrits {

    /**
     * Les trois paliers de l'original, et le facteur de degats de chacun.
     *
     * <p>L'ordre compte : le premier palier dont le tirage passe emporte le coup, donc un coup
     * tres chanceux ne rapporte pas les trois.
     */
    public static final float[] RATES = {1.3f, 1.6f, 2.6f};

    private TeleportCrits() {}

    /**
     * La chance d'un palier, reprise de {@code TPSkillHelper.prob}.
     *
     * @param dimFolding l'experience de Dimension Folding Theorem, ou <b>-1</b> s'il n'est pas
     *                   appris — c'est la convention de l'original, ou un {@code tryLerp} sur
     *                   -1 rendait zero
     * @param spaceFluct la meme chose pour Space Fluctuation
     */
    public static float chance(int tier, float dimFolding, float spaceFluct) {
        return switch (tier) {
            case 0 -> tryLerp(0.10f, 0.20f, dimFolding) + tryLerp(0.18f, 0.25f, spaceFluct);
            case 1 -> tryLerp(0.10f, 0.15f, spaceFluct);
            case 2 -> tryLerp(0.01f, 0.03f, spaceFluct);
            default -> throw new IllegalArgumentException("palier inconnu : " + tier);
        };
    }

    private static float tryLerp(float from, float to, float exp) {
        // -1 : la competence n'est pas apprise, elle ne donne donc rien du tout. C'est ce que
        // faisait l'original, et c'est ce qui rend le premier palier impossible sans elles.
        if (exp < 0f) return 0f;
        return from + Math.min(1f, exp) * (to - from);
    }

    /**
     * Le palier d'un coup : le premier des trois dont le tirage passe, ou {@code -1} si aucun.
     *
     * @param rolls trois tirages dans [0,1[, dans l'ordre des paliers. Ils appartiennent a
     *              l'appelant : c'est ce qui permet de figer un critique en test.
     */
    public static int tier(float dimFolding, float spaceFluct, float[] rolls) {
        for (int i = 0; i < RATES.length; i++) {
            if (rolls[i] < chance(i, dimFolding, spaceFluct)) return i;
        }
        return -1;
    }

    /** Les degats d'un palier : le coup est multiplie par le facteur du palier. */
    public static float damage(float base, int tier) {
        return tier < 0 ? base : base * RATES[tier];
    }

    /** L'experience que l'original versait a Dimension Folding Theorem selon le palier. */
    public static float expForDimFolding(int tier) {
        return (tier + 1) * 0.005f;
    }

    /** Et celle qu'il versait a Space Fluctuation, quelle que soit la force du coup. */
    public static float expForSpaceFluct() {
        return 0.0001f;
    }

    /**
     * Frappe une cible avec les critiques des teleportations (portage de
     * {@code TPSkillHelper.attack} et {@code attackIgnoreArmor}, qui ne differaient que par le
     * type de degats — {@code indirectMagic}, celui qui traverse l'armure, ici comme dans le
     * reste du port).
     */
    public static void strike(Player player, AbilityData data, LivingEntity target, float base) {
        float dimFolding = expOf(data, TeleporterCategory.DIM_FOLDING_THEOREM);
        float spaceFluct = expOf(data, TeleporterCategory.SPACE_FLUCTUATION);
        int tier = tier(dimFolding, spaceFluct, new float[] {
                player.getRandom().nextFloat(),
                player.getRandom().nextFloat(),
                player.getRandom().nextFloat()});

        if (tier >= 0) {
            data.addSkillExp(TeleporterCategory.DIM_FOLDING_THEOREM, expForDimFolding(tier));
            data.addSkillExp(TeleporterCategory.SPACE_FLUCTUATION, expForSpaceFluct());
            // L'original prevenait le joueur en clair : c'est ce qui rend le critique lisible,
            // sans quoi il ne verrait qu'une victime qui tombe plus vite.
            player.displayClientMessage(Component.translatable("ac.ability.teleporter.crithit",
                    String.format(Locale.ROOT, "%.1f", RATES[tier])), false);
            // Et la gerbe de formule autour de la victime : c'est tout ce que les deux passives
            // du teleporteur donnent a voir, et l'original la semait a chaque critique. Voir
            // TeleportCritPacket et FormulaParticles.
            cn.academy.ability.network.TeleportCritPacket.send(target);
        }

        target.hurt(Skill.skillDamage(player), damage(base, tier));
    }

    /** L'experience d'une competence, ou -1 pour « pas apprise » : la convention de l'original. */
    private static float expOf(AbilityData data, Skill skill) {
        return data.isSkillLearned(skill) ? data.getSkillExp(skill) : -1f;
    }
}
