package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;

/**
 * Radiation Intensify (β Radiation Intensify) : le passif de niveau 1 du meltdowner.
 *
 * <p>Portage de {@code RadiationIntensify}. Il ne s'active pas et ne se range pas sur une
 * touche : il <b>marque</b> les cibles touchees par un tir du meltdowner, et une cible marquee
 * encaisse plus de degats de tout le monde pendant cinq secondes (voir {@link RadiationMarks}).
 *
 * <p>Deux choses de l'original, qui surprennent, sont reprises telles quelles :
 *
 * <ul>
 * <li>son experience ne s'apprend pas : elle vaut la part de reserve que le joueur a debloquee
 *     (son plafond, rapporte a celui du niveau 5). Plus il a travaille son plasma, plus ses
 *     radiations mordent — et le passif n'a donc pas besoin de monter en s'en servant ;</li>
 * <li>le facteur qu'il applique va de 1,4 a 1,8, soit presque le double au maximum.</li>
 * </ul>
 */
public class RadiationIntensifySkill extends Skill {

    /** Le facteur au depart et au maximum, tels quels dans l'original. */
    public static final float RATE_MIN = 1.4f;
    public static final float RATE_MAX = 1.8f;

    /** La duree d'une marque : soixante ticks, cinq secondes, comme l'original. */
    public static final int MARK_TICKS = 60;

    public RadiationIntensifySkill() {
        super("rad_intensify", 1);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public boolean canControl() {
        return false;
    }

    /** Le facteur que la marque applique, de 1,4 a 1,8 selon l'experience du passif. */
    public float rate(AbilityData data) {
        return lerp(RATE_MIN, RATE_MAX, data.getSkillExp(this));
    }

    /** Son experience se calcule, elle ne se gagne pas : voir la classe. */
    @Override
    public boolean hasComputedExp() {
        return true;
    }

    /**
     * L'experience du passif : {@code clamp(maxCP / maxCP du niveau 5)}.
     *
     * <p>C'est le calcul de l'original ({@code getInitCP(5)}), et il compte la reserve
     * <b>avec</b> ses ajouts et les bonus des cursus : un joueur qui a pousse sa reserve voit
     * donc ses radiations progresser.
     */
    @Override
    public float computeExp(AbilityData data) {
        if (data == null) return 0f;
        float full = AbilityData.baseMaxControlPoint(5);
        if (full <= 0f) return 0f;
        return Math.min(1f, data.getMaxControlPoint() / full);
    }
}
