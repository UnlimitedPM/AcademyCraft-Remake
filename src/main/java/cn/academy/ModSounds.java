package cn.academy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Les sons du mod, portage du {@code sounds.json} de l'original.
 *
 * <p>Les quarante-quatre evenements sont ceux de l'original, avec le meme nom et le meme
 * fichier : le {@code sounds.json} du port a ete converti du sien, ou seule la cle
 * {@code category} a disparu, la 1.20.1 ne la connaissant plus (la categorie se donne a
 * la lecture, et chaque appel la reprend de l'original).
 *
 * <h2>Un evenement doit exister des deux cotes</h2>
 *
 * Un son n'est pas seulement un fichier : c'est un evenement du registre, dont la
 * declaration dans {@code sounds.json} ne dit que les fichiers. Un evenement declare ici
 * mais absent du fichier se joue dans un silence total, sans erreur — d'ou le test qui
 * compare les deux listes dans {@code SoundAssetsTest}.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AcademyCraft.MOD_ID);

    /**
     * Tous les evenements, dans l'ordre du fichier.
     *
     * <p>Declare <b>avant</b> les constantes : leurs initialiseurs l'alimentent, et les
     * initialiseurs statiques s'executent dans l'ordre du texte.
     */
    private static final List<RegistryObject<SoundEvent>> ALL = new ArrayList<>();

    public static final RegistryObject<SoundEvent> ABILITY_DENY = event("ability.deny");
    public static final RegistryObject<SoundEvent> EM_ARC_WEAK = event("em.arc_weak");
    public static final RegistryObject<SoundEvent> EM_ARC_STRONG = event("em.arc_strong");
    public static final RegistryObject<SoundEvent> EM_MINEDETECT = event("em.minedetect");
    public static final RegistryObject<SoundEvent> EM_RAILGUN = event("em.railgun");
    public static final RegistryObject<SoundEvent> EM_MOVE_LOOP = event("em.move_loop");
    public static final RegistryObject<SoundEvent> EM_CHARGE_LOOP = event("em.charge_loop");
    public static final RegistryObject<SoundEvent> EM_INTENSIFY_ACTIVATE = event("em.intensify_activate");
    public static final RegistryObject<SoundEvent> EM_INTENSIFY_LOOP = event("em.intensify_loop");
    public static final RegistryObject<SoundEvent> EM_LF_LOOP = event("em.lf_loop");
    public static final RegistryObject<SoundEvent> EM_MAG_MANIP = event("em.mag_manip");
    public static final RegistryObject<SoundEvent> MD_BALLSHOOT = event("md.ballshoot");
    public static final RegistryObject<SoundEvent> MD_RAY_SMALL = event("md.ray_small");
    public static final RegistryObject<SoundEvent> MD_SHIELD_STARTUP = event("md.shield_startup");
    public static final RegistryObject<SoundEvent> MD_SHIELD_LOOP = event("md.shield_loop");
    public static final RegistryObject<SoundEvent> MD_MELTDOWNER = event("md.meltdowner");
    public static final RegistryObject<SoundEvent> MD_MINE_LOOP = event("md.mine_loop");
    public static final RegistryObject<SoundEvent> MD_MINE_BASIC_STARTUP = event("md.mine_basic_startup");
    public static final RegistryObject<SoundEvent> MD_MINE_LUCK_STARTUP = event("md.mine_luck_startup");
    public static final RegistryObject<SoundEvent> MD_MINE_EXPERT_STARTUP = event("md.mine_expert_startup");
    public static final RegistryObject<SoundEvent> MD_SIMPLE_CHARGE = event("md.simple_charge");
    public static final RegistryObject<SoundEvent> MD_MD_CHARGE = event("md.md_charge");
    public static final RegistryObject<SoundEvent> ENTITY_FLIPCOIN = event("entity.flipcoin");
    public static final RegistryObject<SoundEvent> ENTITY_SILBARN_HEAVY = event("entity.silbarn_heavy");
    public static final RegistryObject<SoundEvent> ENTITY_SILBARN_LIGHT = event("entity.silbarn_light");
    public static final RegistryObject<SoundEvent> TERMINAL_SELECT = event("terminal.select");
    public static final RegistryObject<SoundEvent> TERMINAL_CONFIRM = event("terminal.confirm");
    public static final RegistryObject<SoundEvent> TP_TP = event("tp.tp");
    public static final RegistryObject<SoundEvent> TP_TP_PRE = event("tp.tp_pre");
    public static final RegistryObject<SoundEvent> TP_GUTS = event("tp.guts");
    public static final RegistryObject<SoundEvent> TP_TP_SHIFT = event("tp.tp_shift");
    public static final RegistryObject<SoundEvent> TP_TP_FLASHING = event("tp.tp_flashing");
    public static final RegistryObject<SoundEvent> VECMANIP_BLOOD_RETRO = event("vecmanip.blood_retro");
    public static final RegistryObject<SoundEvent> VECMANIP_DIRECTED_SHOCK = event("vecmanip.directed_shock");
    public static final RegistryObject<SoundEvent> VECMANIP_GROUNDSHOCK = event("vecmanip.groundshock");
    public static final RegistryObject<SoundEvent> VECMANIP_DIRECTED_BLAST = event("vecmanip.directed_blast");
    public static final RegistryObject<SoundEvent> VECMANIP_VEC_ACCEL = event("vecmanip.vec_accel");
    public static final RegistryObject<SoundEvent> VECMANIP_PLASMA_CANNON = event("vecmanip.plasma_cannon");
    public static final RegistryObject<SoundEvent> VECMANIP_STORM_WING = event("vecmanip.storm_wing");
    public static final RegistryObject<SoundEvent> VECMANIP_VEC_DEVIATION = event("vecmanip.vec_deviation");
    public static final RegistryObject<SoundEvent> VECMANIP_VEC_REFLECTION = event("vecmanip.vec_reflection");
    public static final RegistryObject<SoundEvent> VECMANIP_PLASMA_CANNON_T = event("vecmanip.plasma_cannon_t");
    public static final RegistryObject<SoundEvent> MACHINE_IMAG_FUSOR_WORK = event("machine.imag_fusor_work");
    public static final RegistryObject<SoundEvent> MACHINE_MACHINE_WORK = event("machine.machine_work");

    /** Tous les evenements enregistres, dans l'ordre du fichier. */
    public static List<RegistryObject<SoundEvent>> all() {
        return List.copyOf(ALL);
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> event(String name) {
        RegistryObject<SoundEvent> holder = SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID, name)));
        ALL.add(holder);
        return holder;
    }

    private ModSounds() {}
}
