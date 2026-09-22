package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Rayon minier de la chance, portage de {@code MineRayLuck} : le meme que l'expert, mais
 * il promet du butin en plus.
 *
 * <p>L'original le faisait casser ses blocs avec un niveau de fortune de trois. Le port
 * demande la meme chose à la 1.20.1 en passant une pioche enchanteresse au calcul des
 * butins : c'est la table de butin du bloc qui decide, donc un minerai donne bien ce qu'il
 * donnerait a un joueur chanceux, et pas un objet invente.
 */
public class MineRayLuckSkill extends MineRaySkill {

    /** Niveau de fortune de l'original, pour ses trois rayons de chance. */
    private static final int FORTUNE = 3;

    public MineRayLuckSkill() {
        super("mine_ray_luck", 5);
    }

    @Override
    public double range() {
        return 20.0;
    }

    @Override
    public int tier() {
        return 5;
    }

    @Override
    public float speed(AbilityData data) {
        return lerp(0.5f, 1f, data.getSkillExp(this));
    }

    @Override
    public float cpPerTick(AbilityData data) {
        return lerp(1.8f, 1.25f, data.getSkillExp(this));
    }

    @Override
    public float expPerBlock() {
        return 0.0003f;
    }

    @Override
    public int cooldown(AbilityData data) {
        return (int) lerp(60f, 30f, data.getSkillExp(this));
    }

    /** Surcout : de 350 a 300, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(350f, 300f, data.getSkillExp(this));
    }

    /** Les butins du bloc, calcules comme si le joueur tenait une pioche fortune III. */
    @Override
    protected List<ItemStack> drops(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        return net.minecraft.world.level.block.Block.getDrops(state, level, pos, null, player,
                fortunePickaxe(FORTUNE));
    }
}
