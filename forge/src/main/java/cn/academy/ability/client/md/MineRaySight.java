package cn.academy.ability.client.md;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Le rayon minier des AUTRES joueurs.
 *
 * <p>Le rayon du joueur lui-meme est mene par {@code MineRayEffect}, qui le repose a chaque image
 * sur son propre regard. Celui d'un voisin n'existait pas : le rayon minier est entierement
 * calcule sur le client de son tireur, et rien ne le publiait. On recevait donc l'attaque sans
 * rien voir — et, depuis que les competences eclairent, sans en voir la lumiere non plus.
 *
 * <p>Le paquet ne dit que <b>debut</b> et <b>fin</b> (voir {@code MineRaySightPacket}) : entre les
 * deux, chaque image repose le rayon sur la tete de son tireur, exactement comme le faisait
 * l'{@code updatePos} de l'entite de l'original. Position et orientation d'un joueur sont deja
 * synchronisees par le jeu, donc rien n'est invente — et le trafic reste de deux paquets par
 * rayon, au lieu de vingt par seconde.
 *
 * <p>Ce qui reste chez le mineur, faute d'etre envoye : ses etincelles (celles du rayon et celles
 * du bloc). Elles ne changeaient pas la lecture de la scene, et les envoyer aurait demande un
 * paquet par etincelle.
 *
 * <p>La lumiere, elle, vient toute seule : {@code MdRays.live()} porte aussi ces rayons-la, et
 * c'est de cette liste que {@code EffectLights} tire ses lampions.
 */
public final class MineRaySight {

    /** Les rayons vus, par identifiant de mineur. */
    private static final Map<Integer, MdRays.LiveRay> SIGHTS = new HashMap<>();

    private MineRaySight() {}

    /**
     * Le serveur annonce qu'un rayon minier s'ouvre ou se ferme chez quelqu'un.
     *
     * <p>Le rayon du joueur lui-meme est ignore : il est deja la, mene par {@code MineRayEffect},
     * sans le moindre retard de reseau.
     */
    public static void see(String skill, int ownerId, boolean open) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getId() == ownerId) return;

        MdRays.LiveRay previous = SIGHTS.remove(ownerId);
        if (previous != null) MdRays.close(previous);
        if (!open) return;

        MdRayKind kind = MineRayEffect.kindOf(skill);
        if (kind == null) return;

        // Le rayon nait sur la tete de son tireur s'il est deja la ; sinon il attend la premiere
        // image, ou il sera pose pour de bon (ou abandonne si le tireur n'existe pas).
        Vec3 eye = eyeOf(minecraft.level, ownerId);
        if (eye == null) return;
        SIGHTS.put(ownerId, MdRays.open(kind, eye, eye));
    }

    /**
     * Une image : chaque rayon vu se repose sur la tete de son tireur.
     *
     * <p>Le decalage de vue est celui d'un rayon qui n'est pas le notre — la main du tireur — donc
     * le meme {@code MdRayView.place} que pour les rayons annonces par le serveur.
     */
    public static void frame(ClientLevel level, float partialTick, double[] above) {
        if (SIGHTS.isEmpty()) return;

        for (Iterator<Map.Entry<Integer, MdRays.LiveRay>> it = SIGHTS.entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<Integer, MdRays.LiveRay> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Player miner) || !miner.isAlive() || miner.isSpectator()) {
                MdRays.close(entry.getValue());
                it.remove();
                continue;
            }
            Vec3 from = miner.getEyePosition(partialTick);
            Vec3 to = from.add(miner.getViewVector(partialTick).scale(MineRayEffect.BEAM_LENGTH));
            double[][] placed = MdRayView.place(entry.getValue().kind(),
                    new double[] { from.x, from.y, from.z },
                    new double[] { to.x, to.y, to.z }, false, above);
            MdRays.move(entry.getValue(),
                    new Vec3(placed[0][0], placed[0][1], placed[0][2]),
                    new Vec3(placed[1][0], placed[1][1], placed[1][2]));
        }
    }

    /** Tout oublier : un monde quitte n'a plus de voisins. */
    public static void clear() {
        for (MdRays.LiveRay ray : SIGHTS.values()) MdRays.close(ray);
        SIGHTS.clear();
    }

    /** Les yeux d'un joueur du monde, ou {@code null} s'il n'y est pas (encore). */
    private static Vec3 eyeOf(ClientLevel level, int ownerId) {
        if (level == null) return null;
        Entity entity = level.getEntity(ownerId);
        if (!(entity instanceof Player player)) return null;
        return player.getEyePosition(1f);
    }
}
