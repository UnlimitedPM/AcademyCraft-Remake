package cn.academy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Le bruit du feu qui s'eteint, joue chez celui qui brule.
 *
 * <p>Le serveur l'envoie deja aux autres — mais au joueur lui-meme il n'arrivait pas, et le
 * joueur l'a dit deux fois : « je n'entends toujours pas le son de mon personnage qui arrete de
 * bruler ». Le son est donc joue ici, en local, au moment ou le liquide eteint le corps : rien a
 * attendre du reseau, rien a perdre si un paquet passe mal.
 *
 * <p>Cette classe n'existe que cote client : elle est appelee par {@code DistExecutor}, depuis
 * {@code ImagPhaseLiquidBlock}, qui tourne des deux cotes.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientExtinguish {

    /** Le dernier tick et le dernier corps servis : un son par tick, pas un par bloc touche. */
    private static long lastTick = Long.MIN_VALUE;
    private static int lastEntity = Integer.MIN_VALUE;

    private ClientExtinguish() {
    }

    /**
     * Joue le son si l'entite est le joueur local — et une seule fois par tick.
     *
     * <p>Les autres corps n'ont pas besoin de ce detour : le serveur les fait entendre a tout le
     * monde, et deux sons pour un seul evenement s'entendraient comme un echo.
     *
     * <p>Un corps qui barbote touche <b>plusieurs</b> blocs de liquide (les jambes, la tete), et
     * {@code entityInside} est appele une fois par bloc : sans ce garde-fou, le joueur entendait
     * le meme son deux fois dans le meme tick.
     */
    public static void playFor(Entity entity, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity != minecraft.player || minecraft.level == null) return;
        long now = minecraft.level.getGameTime();
        if (now == lastTick && entity.getId() == lastEntity) return;
        lastTick = now;
        lastEntity = entity.getId();
        minecraft.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0f, pitch, false);
    }
}
