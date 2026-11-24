package cn.academy.terminal.client;

import cn.academy.AcademyCraftCommon;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Ouverture du terminal par une touche.
 *
 * L'original utilisait Alt gauche ({@code open_data_terminal}) et affichait un
 * overlay permanent. Ici la meme touche ouvre une page, et le terminal ne s'ouvre
 * que s'il est installe : c'est le seul controle d'acces, comme dans l'original ou
 * l'overlay refusait de s'afficher sans la donnee.
 *
 * <h2>La touche agit au RELACHEMENT, pas a l'appui</h2>
 *
 * C'est le {@code onKeyUp} de l'original ({@code TerminalUI.keyHandler}), et ce n'est pas un
 * detail de style : agir a l'appui faisait ouvrir puis refermer le terminal en boucle des qu'on
 * gardait la touche enfoncee. La raison est dans {@code KeyboardHandler} : quand un ecran est
 * ouvert, chaque REPETITION de la touche ({@code GLFW_REPEAT}) est livree a
 * {@code Screen.keyPressed}, et des que l'ecran se referme la repetition suivante redevient un
 * clic de {@code KeyMapping}. Ouvrir sur l'appui et fermer sur l'appui se renvoyaient donc la
 * balle toutes les trente millisecondes.
 *
 * <p>La touche est donc lue PHYSIQUEMENT ({@code InputConstants.isKeyDown}), comme le
 * {@code Keyboard.isKeyDown} de l'original, et non par {@code KeyMapping.consumeClick} : ce
 * dernier ne compte que les clics recus hors d'un ecran, donc il ne saurait pas refermer.
 * L'etat est retenu d'un tick a l'autre, ce qui donne un appui et un relachement par
 * enfoncement, repetitions comprises.
 */
public class TerminalKeyBindings {

    public static final KeyMapping OPEN_TERMINAL = new KeyMapping(
            "key.academy.open_terminal", InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT, "key.categories.academy");

    /** L'etat de la touche au tick precedent : c'est lui qui distingue un relachement. */
    private static boolean wasDown;

    /** Les touches a declarer chez le chargeur, qui seul connait son registre. */
    public static void register(java.util.function.Consumer<KeyMapping> sink) {
        sink.accept(OPEN_TERMINAL);
    }

    /** Un tick de client : l'ouverture et la fermeture, au relachement. */
    public static void tick() {
        boolean down = isDown();
        boolean released = wasDown && !down;
        wasDown = down;
        if (!released) return;

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null) return;

        // Refermer : la page est a nous, et c'est le meme geste que pour ouvrir — un appui,
        // puis on relache. Une saisie de chat en cours garde la page ouverte : la touche y
        // ecrit, elle ne la ferme pas.
        if (minecraft.screen instanceof TerminalScreen terminal) {
            if (!terminal.isTypingChat()) terminal.onClose();
            return;
        }

        // Un autre ecran (inventaire, coffre, chat du jeu) : la touche ne fait rien, comme le
        // `isPlayerInGame` de l'original qui avortait des qu'un ecran etait ouvert.
        if (minecraft.screen != null) return;
        if (!ClientTerminalData.get().isTerminalInstalled()) return;

        minecraft.setScreen(new TerminalScreen());
    }

    /**
     * La touche est-elle enfoncee, physiquement ?
     *
     * <p>Une touche du clavier se lit par {@code InputConstants.isKeyDown}, un bouton de souris
     * par GLFW : le joueur peut rebinder le terminal sur son clic du milieu, et
     * {@code isKeyDown} ne connait que des touches.
     */
    private static boolean isDown() {
        return cn.academy.platform.client.KeyState.isDown(OPEN_TERMINAL);
    }
}
