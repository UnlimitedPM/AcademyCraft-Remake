package cn.academy.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Un evenement sonore par son nom, ou {@code null} s'il n'existe pas.
 *
 * <p>Le port nomme ses sons par des chaines partout ou le choix doit se relire en test
 * ({@link HeldLoops}, {@link MachineLoops}) : c'est ici, et une seule fois, que ces noms
 * deviennent des {@code SoundEvent}. Un nom en faute rend {@code null}, et l'appelant se
 * tait au lieu de tomber — un son manquant n'est jamais une raison de planter.
 *
 * <p>Les noms sans espace de noms sont ceux du mod : {@code machine.imag_fusor_work} est
 * {@code academy:machine.imag_fusor_work}.
 */
public final class SoundLookup {

    private SoundLookup() {}

    /** L'evenement porte par un nom, ou {@code null} s'il n'existe pas. */
    public static SoundEvent event(String name) {
        if (name == null) return null;
        ResourceLocation key = ResourceLocation.tryParse(
                name.contains(":") ? name : "academy:" + name);
        if (key == null) return null;
        return BuiltInRegistries.SOUND_EVENT.get(key);
    }
}
