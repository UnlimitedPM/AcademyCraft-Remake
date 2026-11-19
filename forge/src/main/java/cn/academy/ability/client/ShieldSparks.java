package cn.academy.ability.client;

import cn.academy.ability.client.md.MdSparks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * L'essaim de plasma du bouclier de lumiere, portage du {@code c_update} de son contexte.
 *
 * <h2>Ce que le bouclier montre de lui-meme</h2>
 *
 * <p>Le disque est dessine par {@code ShieldRenderer} ; ce qui lui manquait, c'est son
 * grésillement. L'original en posait une etincelle a <b>trois ticks sur dix</b>, un bloc devant
 * les yeux du porteur, dans un cube de 0,5 bloc de cote — donc sur le disque et autour de lui —
 * avec une derive quasi nulle. C'est la seule chose que le bouclier fait de lui-meme : il ne
 * bouge pas, ne tire pas, ne tourne pas plus que son propre disque.
 *
 * <p>Elle se joue chez le <b>porteur seul</b>, et sans passer par le reseau : l'original la
 * posait dans son propre client, a partir de son propre maintien. Le port lit donc
 * {@code ClientCharge}, qui dit deja quel maintien est ouvert et depuis combien de ticks — c'est
 * ce que fait {@code ShieldRenderer} pour le disque, et les deux se regardent.
 *
 * <p>Les nombres, eux, sont dans {@link ShieldVisuals#SPARK_CHANCE} et ses voisins.
 */
@OnlyIn(Dist.CLIENT)
public final class ShieldSparks {

    private static final Random RANDOM = new Random();

    private ShieldSparks() {}

    /** A appeler a chaque tick client : voir {@code AbilityClientEvents.onClientTick}. */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;

        // Le bouclier n'existe que pendant son maintien, et un maintien ne se synchronise pas :
        // c'est la charge du joueur local qui dit s'il est la. Meme regle que le disque, dans
        // ShieldRenderer — et c'est SA charge qui est lue, pas celle qui vient d'etre ouverte.
        if (!ShieldVisuals.showsShield(ShieldVisuals.SKILL,
                ClientCharge.isSustained(ShieldVisuals.SKILL))) {
            return;
        }

        if (!ShieldVisuals.sparksThisTick(RANDOM.nextFloat())) return;

        MdSparks.spawn(
                ShieldVisuals.sparkBorn(player.getEyePosition(1f), player.getViewVector(1f), RANDOM),
                ShieldVisuals.sparkDrift(RANDOM));
    }
}
