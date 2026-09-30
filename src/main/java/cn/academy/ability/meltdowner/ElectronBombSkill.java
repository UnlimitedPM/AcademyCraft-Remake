package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.entity.EntityMdBall;
import net.minecraft.world.entity.player.Player;

/**
 * La bombe a electrons, portage d'{@code ElectronBomb} : le premier tir du meltdowner.
 *
 * <p>Elle ne lance pas une grenade. L'original <b>lache une bille</b> devant son porteur —
 * {@link EntityMdBall} — qui flotte la une seconde en scintillant, puis tire son rayon vers ce
 * que le regard touche, a quinze blocs. Le trait frappe la premiere cible qu'il rencontre, et
 * rien d'autre : c'est une bille de plasma, pas une explosion.
 *
 * <p>Le port faisait autrement : une explosion instantanee d'un rayon de trois blocs au point
 * vise. C'etait plus simple, et c'etait faux — l'original ne frappait que d'une cible, et le
 * temps de la bille est tout le sel de la competence. La duree de vie, elle, fond avec
 * l'experience : vingt ticks, puis cinq seulement passe 80 % — la bombe devient alors presque
 * instantanee.
 */
public class ElectronBombSkill extends Skill {

    /** La portee du regard, et donc la longueur du rayon. */
    public static final double RANGE = 15;

    public ElectronBombSkill() {
        super("electron_bomb", 1);
    }

    /** Degats repris de l'original : de 6 a 12 selon l'experience. */
    public float damage(AbilityData data) {
        return lerp(6f, 12f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 20 a 10 ticks, soit 1 a 0,5 seconde. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(20f, 10f, data.getSkillExp(this));
    }

    /** 0,005 au lancer, comme dans l'original. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.005f;
    }

    /**
     * La bombe ne coute rien du tout.
     *
     * <p>L'original ne payait ni CP ni surcout dans cette competence : son
     * {@code s_Execute} lachait la bille, versait l'experience, posait sa recharge et
     * s'arretait la. Le port lui avait donne 30 CP et 200 de surcout — une facture
     * inventee, et corrigee ici. Ce qui la retient, c'est sa recharge de 20 a 10 ticks.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /** Aucun surcout non plus : voir {@link #getCpCost()}. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return 0f;
    }

    /**
     * Le lancer : une bille, et rien de plus.
     *
     * <p>Tout le reste est dans la bille : elle porte les degats du trait qu'elle tirera, et
     * c'est elle qui marque sa cible au nom du passif de radiation. La competence, elle, ne
     * fait que la lacher — exactement comme le {@code s_Execute} de l'original.
     */
    @Override
    public void onActivate(Player player, AbilityData data) {
        int life = data.getSkillExp(this) > MdBallVisuals.IMPROVED_EXP
                ? MdBallVisuals.LIFE_IMPROVED_TICKS
                : MdBallVisuals.LIFE_TICKS;

        player.level().addFreshEntity(
                new EntityMdBall(player.level(), player, life, scaled(damage(data))));
    }
}
