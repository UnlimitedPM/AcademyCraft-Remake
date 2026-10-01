package cn.academy.ability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * S2C : ce maintien n'existe plus, chez le serveur.
 *
 * <h2>Pourquoi ce message existe</h2>
 *
 * <p>Une touche maintenue s'ouvre d'abord chez le <b>client</b>, qui n'attend pas la reponse du
 * serveur pour montrer quelque chose : c'est lui qui tient la touche, et c'est cette avance qui
 * rend le bouclier, la charge et la traction magnetique immediats. Mais le serveur a le dernier
 * mot — il refuse une aptitude eteinte, un brouillage, une recharge, une reserve epuisee, et il
 * peut aussi <b>terminer</b> un maintien avant que la touche soit relachee (sa reserve s'est
 * videe, sa duree maximale est atteinte).
 *
 * <p>Sans ce message, le client continuait donc son animation toute seule, et le joueur voyait
 * un pouvoir qui s'agite sans rien faire : « quand on ne peut pas utiliser le pouvoir, quand on
 * appuie sur la touche, il fait quand meme l'animation mais sans fonctionner ». Le paquet est
 * envoye a chaque fois que le serveur refuse d'ouvrir un maintien <b>et</b> a chaque fois qu'il
 * en termine un, quelle qu'en soit la cause : voir {@code ActivateSkillPacket} et
 * {@code AbilityEvents.endHeld}.
 *
 * <p>Il porte le <b>nom</b> de la competence, et pas seulement un drapeau : c'est ce qui permet
 * au client de ne fermer que le maintien concerne. Un message en retard, parti juste avant que
 * le joueur relance la meme competence, ne peut donc pas eteindre la nouvelle.
 */
public class HoldOverPacket {

    private final String skill;

    public HoldOverPacket(String skill) {
        this.skill = skill;
    }

    public static void encode(HoldOverPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skill);
    }

    public static HoldOverPacket decode(FriendlyByteBuf buf) {
        return new HoldOverPacket(buf.readUtf());
    }

    public static void handle(HoldOverPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.AbilityClientEvents.onHoldOver(msg.skill));
        ctx.setPacketHandled(true);
    }

    /** Le nom de la competence terminee. Lisible par le test, qui relit l'aller-retour. */
    public Optional<String> skillName() {
        return Optional.ofNullable(skill);
    }
}
