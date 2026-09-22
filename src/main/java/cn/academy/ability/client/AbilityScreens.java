package cn.academy.ability.client;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import net.minecraft.client.Minecraft;

/**
 * L'ecran que certaines competences ouvrent a la place d'un effet.
 *
 * <p>Une seule pour l'instant — la teleportation a la marque — et c'est pourquoi ce n'est
 * qu'un aiguillage : le jour ou une seconde competence voudra sa fenetre, elle viendra
 * s'ajouter ici, et le client continuera de ne rien savoir des competences par leur nom.
 */
public final class AbilityScreens {

    private AbilityScreens() {
    }

    /** Ouvre l'ecran de cette competence, ou ne fait rien si elle n'en a pas. */
    public static void open(Category category, Skill skill) {
        if (skill.opensScreen()) {
            Minecraft.getInstance().setScreen(new LocationTeleportScreen(category, skill));
        }
    }
}
