package cn.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AcademyCraft.MOD_ID)
public class AcademyCraft {
    public static final String MOD_ID = "academy";

    public AcademyCraft(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Enregistrement des registres
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Enregistrement de la classe d'événements pour le seau
        MinecraftForge.EVENT_BUS.register(this);
    }

    // --- POINT 2.1 : Transformer le seau de liquide en seau d'eau ---
    @SubscribeEvent
    public void onBucketFill(FillBucketEvent event) {
        net.minecraft.world.level.Level world = (net.minecraft.world.level.Level) event.getLevel();
        if (event.getTarget() instanceof BlockHitResult hit) {
            BlockPos pos = hit.getBlockPos();
            if (world.getBlockState(pos).getBlock() == ModBlocks.PHASE_LIQUID_BLOCK.get()) {
                // On donne un seau d'eau et on vide le bloc
                event.setFilledBucket(new ItemStack(Items.WATER_BUCKET));
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    @SubscribeEvent
    public void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        net.minecraft.world.entity.LivingEntity entity = event.getEntity();

        if (entity.isInFluidType(ModFluids.PHASE_LIQUID_TYPE.get())) {
            net.minecraft.world.phys.Vec3 motion = entity.getDeltaMovement();

            // --- LE MIXTE PARFAIT ---
            // horizontalFactor : ralentit le déplacement gauche/droite/avant/arrière (0.8 = -20%)
            // verticalFactor : ralentit la poussée vers le haut (0.5 = -50%)
            double horizontalFactor = 0.8;
            double verticalFactor = 0.5;

            if (motion.y > 0) {
                // On applique le ralentissement horizontal ET la galère de remontée
                entity.setDeltaMovement(motion.x * horizontalFactor, motion.y * verticalFactor, motion.z * horizontalFactor);
            } else {
                // Même quand on ne monte pas, on garde le ralentissement horizontal pour le côté visqueux
                entity.setDeltaMovement(motion.x * horizontalFactor, motion.y, motion.z * horizontalFactor);
            }
        }
    }
}