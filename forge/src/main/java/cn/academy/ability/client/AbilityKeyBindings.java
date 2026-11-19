package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Les touches du mod : les quatre touches d'aptitude, et celles des prereglages.
 *
 * <p>L'original n'en donnait que quatre, avec quatre prereglages pour dire ce qu'elles
 * allument. Le port avait fini par en poser trente-cinq, jusqu'a la ponctuation et aux touches
 * de defilement, ce qui n'etait plus utilisable : ces declarations sont parties, et il ne reste
 * ici que ce qui est reellement lu.
 *
 * <p>Le cablage vit dans {@link AbilityClientEvents}.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AbilityKeyBindings {

    /**
     * Les quatre touches d'aptitude, avec les defauts de l'original.
     *
     * <p>Chez lui c'etaient {@code MOUSE_LEFT, MOUSE_RIGHT, R, F}, reglables dans son propre
     * ecran de touches. Ce sont les SEULES touches qui declenchent une competence : le reste
     * depend du <b>prereglage</b> en service, qui dit quelle competence va sur quelle touche.
     * C'est la reponse de l'original a la penurie de touches, et elle remplace les trente-cinq
     * liaisons « une par competence » que le port avait fini par poser sur de la ponctuation
     * et des touches de defilement.
     */
    public static final KeyMapping ABILITY_1 = mouseKey("key.academy.ability_1", InputConstants.MOUSE_BUTTON_LEFT);
    public static final KeyMapping ABILITY_2 = mouseKey("key.academy.ability_2", InputConstants.MOUSE_BUTTON_RIGHT);
    public static final KeyMapping ABILITY_3 = abilityKey("key.academy.ability_3", InputConstants.KEY_R);
    public static final KeyMapping ABILITY_4 = abilityKey("key.academy.ability_4", InputConstants.KEY_F);

    /** Le passage au prereglage suivant, qui fait le tour des quatre. C, comme chez l'original. */
    public static final KeyMapping PRESET_NEXT = abilityKey("key.academy.preset_next", InputConstants.KEY_C);

    /**
     * L'allumage de l'aptitude.
     *
     * <p>V, comme chez l'original ({@code KEY_ACTIVATE_ABILITY} sur {@code Keyboard.KEY_V}).
     * Tant qu'elle est eteinte, aucune competence ne part et le HUD reste cache : c'est la
     * touche qui les fait apparaitre.
     *
     * <p>ATTENTION : le port lie encore une competence sur V ({@code mag_movement}) : la touche
     * fait donc les deux. Le systeme de prereglages de l'original, qui remplace ces touches
     * par competence, reglera la question a la racine.
     */
    public static final KeyMapping TOGGLE_ABILITY = abilityKey("key.academy.toggle_ability", InputConstants.KEY_V);

    /** L'ecran qui regle les prereglages. N, comme chez l'original. */
    public static final KeyMapping PRESET_EDIT = abilityKey("key.academy.preset_edit", InputConstants.KEY_N);

    /**
     * L'ecran de debogage du mod : informations du joueur, puis etat des competences.
     *
     * <p>F4 comme chez l'original. Le jeu s'en sert deja pour changer de mode de jeu, mais
     * seulement pour les operateurs : les autres joueurs n'y perdent rien, et la touche se
     * reconfigure dans les options comme toutes les autres.
     */
    public static final KeyMapping DEBUG_CONSOLE = abilityKey("key.academy.debug_console", InputConstants.KEY_F4);

    private static KeyMapping abilityKey(String description, int defaultKey) {
        return new KeyMapping(description, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
                defaultKey, "key.categories.academy");
    }

    /**
     * La touche d'une place de prereglage.
     *
     * <p>L'ordre est celui des touches d'aptitude, donc celui de {@code BINDINGS} : c'est lui
     * que le rappel des touches lit pour peindre ses lignes, dans l'ordre.
     */
    public static KeyMapping abilityKey(int slot) {
        return switch (slot) {
            case 0 -> ABILITY_1;
            case 1 -> ABILITY_2;
            case 2 -> ABILITY_3;
            default -> ABILITY_4;
        };
    }

    private static KeyMapping mouseKey(String description, int defaultButton) {
        return new KeyMapping(description, KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE,
                defaultButton, "key.categories.academy");
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        // Les quatre touches d'aptitude, et rien d'autre : la competence de chacune vient du
        // prereglage en service, comme chez l'original.
        event.register(ABILITY_1);
        event.register(ABILITY_2);
        event.register(ABILITY_3);
        event.register(ABILITY_4);
        event.register(PRESET_NEXT);
        event.register(PRESET_EDIT);
        event.register(DEBUG_CONSOLE);
        event.register(TOGGLE_ABILITY);
    }
}
