package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Branchement Forge des entrees de l'aptitude.
 *
 * <p>Tout le cablage est commun — voir {@link AbilityClientEvents}, qui lit les touches, envoie ses
 * demandes au serveur et tient les charges du client. Ce qui reste ici, ce sont les evenements du
 * chargeur : les deux qui <b>prennent</b> une touche au jeu (le clic et les directions), le champ de
 * vision, la molette du saut traversant, et le tick de client.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public final class AbilityInputEvents {

    private AbilityInputEvents() {
    }

    /**
     * Le clic qui porte une competence n'est ni un coup d'epee ni un usage d'objet.
     *
     * <p>C'est le jeu qui demande, et on lui repond non : le bras ne frappe pas, l'objet ne part pas.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() && !event.isUseItem()) return;
        if (!AbilityClientEvents.takesInteraction(event.getKeyMapping())) return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    /** Les directions que la competence prend : la marche se refuse juste avant que le jeu s'en serve. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!AbilityClientEvents.takesDirections()) return;
        if (event.getEntity() != Minecraft.getInstance().player) return;

        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        AbilityClientEvents.tick();
    }

    /** Le champ de vision qui s'elargit pendant une charge. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        event.setNewFovModifier(AbilityClientEvents.fovDelta(event.getFovModifier()));
    }

    /** La molette du saut traversant : un cran, un bloc — et la molette ne change pas d'objet. */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (AbilityClientEvents.scroll(event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }
}
