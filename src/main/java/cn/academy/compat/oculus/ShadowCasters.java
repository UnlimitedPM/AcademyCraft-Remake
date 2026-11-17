package cn.academy.compat.oculus;

import cn.academy.ModEntities;
import cn.academy.entity.EntityShadowCaster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Les porteurs d'ombre des machines : une entite vide par machine visible.
 *
 * <p>Pourquoi une entite, et pas le crochet d'Oculus de {@link OculusShadowCompat} seul : un shader
 * ne dessine les block entities dans sa carte d'ombre que si le pack le demande, et Complementary
 * le refuse dans tous ses profils ({@code shadowBlockEntities = false} dans son
 * {@code shaders.properties}). Aucun crochet cote block entity ne peut donc marcher avec ce pack.
 * Les entites, si : {@code shadowEntities} vaut vrai par defaut, et c'est le chemin qu'empruntent
 * les mobs pour projeter leur ombre.
 *
 * <p>On pose donc, chez le client seulement, une entite invisible sur chaque machine vue par son
 * rendu. Son rendu delegue au rendu de la machine, uniquement pendant la carte d'ombre. Elle
 * disparait d'elle-meme quand la machine n'est plus la.
 *
 * <p>La creation se fait au tick client, et pas dans le rendu : on ne modifie pas la liste des
 * entites pendant qu'une autre boucle la parcourt.
 */
@OnlyIn(Dist.CLIENT)
public final class ShadowCasters {

    /** Les machines vues depuis le dernier tick, et qui meritent un porteur. */
    private static final Set<BlockPos> WANTED = new LinkedHashSet<>();

    /** Les porteurs vivants, par machine. */
    private static final Map<BlockPos, EntityShadowCaster> LIVE = new HashMap<>();

    /** Le monde des porteurs : en changer les emporte tous. */
    private static ClientLevel level;

    /** Les identifiants des entites locales : negatifs, comme le veut un client. */
    private static int nextId = -1;

    private ShadowCasters() {}

    /** Une machine vient d'etre dessinee : il lui faut un porteur d'ombre. */
    public static void want(BlockPos pos) {
        WANTED.add(pos.immutable());
    }

    /** Une fois par tick client : cree les porteurs qui manquent, oublie les disparus. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel current = minecraft.level;
        if (current != level) {
            level = current;
            LIVE.clear();
            WANTED.clear();
        }
        if (current == null) return;
        if (LIVE.isEmpty() && WANTED.isEmpty()) return;

        LIVE.entrySet().removeIf(entry -> entry.getValue().isRemoved());

        for (BlockPos pos : WANTED) {
            EntityShadowCaster known = LIVE.get(pos);
            if (known != null && !known.isRemoved()) continue;
            if (!EntityShadowCaster.casts(current.getBlockEntity(pos))) continue;

            EntityShadowCaster caster = new EntityShadowCaster(ModEntities.SHADOW_CASTER.get(), current);
            // Pose sur le COIN du bloc : le rendu de block entity prend son origine la, et la pose
            // d'une entite y amene deja l'origine du monde.
            caster.setPos(pos.getX(), pos.getY(), pos.getZ());
            caster.setOldPosAndRot();
            caster.setId(nextId--);
            if (current.addFreshEntity(caster)) {
                LIVE.put(pos, caster);
            }
        }
        WANTED.clear();
    }
}
