package cn.academy.ability.network;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class AbilityNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AcademyCraft.MOD_ID, "ability_main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, ActivateSkillPacket.class,
                ActivateSkillPacket::encode, ActivateSkillPacket::decode, ActivateSkillPacket::handle);
        CHANNEL.registerMessage(nextId++, SyncAbilityDataPacket.class,
                SyncAbilityDataPacket::encode, SyncAbilityDataPacket::decode, SyncAbilityDataPacket::handle);
    }
}
