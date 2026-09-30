package cn.academy.ability.client;

import cn.academy.ability.client.arc.SurroundArcs;
import cn.academy.ability.electromaster.MagManipVisuals;
import cn.academy.entity.EntityMagManipBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Le gresillement qui colle au bloc tenu par la manipulation magnetique, cote client.
 *
 * <p>Portage de l'{@code EntitySurroundArc} que l'original accrochait a son bloc. Il posait
 * l'entite a sa PREMIERE mise a jour, et seulement si le monde etait client — donc rien ne
 * passait par le reseau, et rien n'existait sur le serveur. Cette entite vivait
 * <b>233333</b> ticks, suivait le bloc a chaque tick en recopiant sa position, et mourait avec
 * lui : {@code if (isDead) getTarget().setDead()}. Autour d'un cube de 1,3 bloc — la taille du
 * bloc, 1, multipliee par le {@code sizeMultiplyer} de 1,3 — {@code ArcType.THIN} tenait quatre
 * arcs vivants a la fois.
 *
 * <p>Le port n'a pas d'entite d'arcs d'entourage : ce crochet-ci re-seme quelques arcs a chaque
 * tick autour de chaque bloc tenu, dans le meme cube et avec le meme gabarit. Un arc ne vit que
 * trois ticks — {@code SurroundArcs.LIFE_TICKS} — donc l'essaim suit le bloc avec un tick de
 * retard, et le nombre d'arcs semes par tick est calcule pour en garder quatre vivants : voir
 * {@link MagManipVisuals#arcsToSow}.
 *
 * <p>Il gresille tant que le bloc vit, donc aussi pendant qu'il vole apres le lancer, et
 * s'arrete quand il se pose — la ou l'original tuait ses arcs en meme temps que le bloc. Rien
 * ici n'est une competence : aucun arc ne part tant qu'aucun bloc n'existe.
 */
@OnlyIn(Dist.CLIENT)
public final class MagManipEffect {

    private static final RandomSource RANDOM = RandomSource.create();

    private MagManipEffect() {}

    /** A appeler a chaque tick client : voir {@code AbilityClientEvents.onClientTick}. */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        int count = MagManipVisuals.arcsToSow(client.level.getGameTime());
        double half = MagManipVisuals.SURROUND_CUBE / 2.0;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityMagManipBlock block)) continue;

            // Les points sont tires DANS le cube, centre sur le bloc : c'est le
            // CubePointFactory(...).setCentered(true) de l'original, dont le port rend les
            // memes trois nombres par spread.
            List<Vec3> points = SurroundArcs.spread(block.position(),
                    MagManipVisuals.SURROUND_CUBE, -half, half, count, RANDOM);
            // Le proprietaire ne sert ici qu'a l'affichage a la premiere personne : c'est un
            // bloc, son identifiant ne sera jamais celui du joueur. L'original ne demandait rien.
            SurroundArcs.spawnAt(SurroundArcs.THIN, points, block.getId(), RANDOM);
        }
    }
}
