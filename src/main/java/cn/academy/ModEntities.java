package cn.academy;

import cn.academy.entity.EntitySilbarn;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Les entites du mod.
 *
 * <p>C'est le premier type d'entite du port : jusqu'ici, toutes les competences agissaient
 * directement sur le monde — un rayon, un coup, un bloc casse — sans jamais rien poser
 * entre deux. La bille du ray barrage est la premiere exception : elle <b>existe</b>
 * pendant quelques secondes, on la lance, elle se pose, et la salve part de la ou elle
 * s'est posee.
 *
 * <p>L'original enregistrait ses entites avec l'annotation {@code @RegEntity} de
 * lambdalib ; le port passe par le registre differe de Forge, comme pour le reste.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AcademyCraft.MOD_ID);

    /**
     * La bille de silicium (le « silbarn »), portage de {@code EntitySilbarn}.
     *
     * Quarante centimetres de cote, comme l'original, suivie sur soixante-quatre blocs et
     * rafraichie tous les dix ticks — la portee et la cadence d'une boule de neige, qui est
     * exactement le meme genre d'objet : petit, lance a la main, et sans equipage.
     */
    public static final RegistryObject<EntityType<EntitySilbarn>> SILBARN =
            ENTITIES.register("silbarn", () -> EntityType.Builder
                    .<EntitySilbarn>of(EntitySilbarn::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("silbarn"));

    /**
     * Le bloc tenu par la manipulation magnetique, portage de {@code MagManipEntityBlock}.
     *
     * Un bloc de taille pleine — un metre de cote, comme celui qu'il represente — suivi de
     * pres et rafraichi souvent : c'est ce que le joueur regarde quand il le jette.
     */
    public static final RegistryObject<EntityType<cn.academy.entity.EntityMagManipBlock>> MAG_MANIP_BLOCK =
            ENTITIES.register("mag_manip_block", () -> EntityType.Builder
                    .<cn.academy.entity.EntityMagManipBlock>of(cn.academy.entity.EntityMagManipBlock::new,
                            MobCategory.MISC)
                    .sized(1f, 1f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("mag_manip_block"));

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        ENTITIES.register(bus);
    }
    private ModEntities() {
    }
}
