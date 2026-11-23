package cn.academy.ability.network;

import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;


/**
 * S2C : un rayon minier vient de s'ouvrir ou de s'arreter, chez son tireur.
 *
 * <p>Le rayon minier etait la seule chose lumineuse du mod qui ne se voyait que chez celui qui
 * la lance : il est entierement calcule sur le client du mineur (voir {@code MineRayEffect}), et
 * aucun paquet ne parlait de lui. Les autres joueurs ne voyaient donc ni le rayon, ni ses
 * etincelles, ni — depuis la 1.0.4 — la lumiere qu'il jette. C'etait deja le cas dans l'original,
 * dont l'entite de rayon n'etait creee que dans un contexte client ; le joueur a trouve cela
 * bizarre, et il a raison : une competence qu'on ne voit pas venir est un piege.
 *
 * <p>Un aller-retour de plus serait bavard : vingt paquets par seconde et par mineur. Le rayon
 * n'a pourtant pas besoin d'etre envoye, parce que le client SAIT deja tout de lui — la position
 * et l'orientation de son tireur sont synchronisees par le jeu, une vingtaine de fois par seconde.
 * On annonce donc seulement <b>le debut et la fin</b>, et chaque client redessine le rayon a partir
 * de la tete du mineur, comme le faisait l'{@code updatePos} de l'original dans son entite.
 *
 * <p>La competence voyage par son nom, comme le genre des rayons du plasma (voir
 * {@link MdRayPacket}) : un nom se relit, resiste a un ajout dans la table, et un nom inconnu
 * n'allume rien plutot que de faire tomber le rendu.
 *
 * <p>Seul {@code handle} nomme une classe de client, et il ne s'execute que la : un serveur dedie
 * ne connait ni les rayons ni leur rendu.
 */
public class MineRaySightPacket {

    /** L'identifiant d'un rayon qui s'ouvre, ou d'un rayon qui s'arrete. */
    private final String skill;
    private final int ownerId;
    private final boolean open;

    public MineRaySightPacket(String skill, int ownerId, boolean open) {
        this.skill = skill;
        this.ownerId = ownerId;
        this.open = open;
    }

    public static void encode(MineRaySightPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skill);
        buf.writeVarInt(msg.ownerId);
        buf.writeBoolean(msg.open);
    }

    public static MineRaySightPacket decode(FriendlyByteBuf buf) {
        return new MineRaySightPacket(buf.readUtf(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(MineRaySightPacket msg,
                              java.util.function.Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> cn.academy.ability.client.md.MineRaySight.see(
                msg.skill, msg.ownerId, msg.open));
        ctx.setPacketHandled(true);
    }

    /** La competence annoncee. Lisible par le test, qui relit l'aller-retour du paquet. */
    String skill() {
        return skill;
    }

    /** L'identifiant du mineur. */
    int ownerId() {
        return ownerId;
    }

    /** Vrai pour une ouverture, faux pour un arret. */
    boolean open() {
        return open;
    }
}
