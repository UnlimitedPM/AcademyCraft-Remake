package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import net.minecraft.nbt.CompoundTag;

/** Client-side read-only cache of the local player's AbilityData, kept in sync via packets. */
public class ClientAbilityData {

    private static final AbilityData DATA = new AbilityData();

    public static void update(CompoundTag tag) {
        DATA.deserializeNBT(tag);
    }

    public static AbilityData get() {
        return DATA;
    }
}
