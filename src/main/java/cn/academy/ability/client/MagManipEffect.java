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

    /** Le bloc porte par ce client, et son point du tick precedent : le suivi a besoin des deux. */
    private static int lastBlockId = -1;
    private static Vec3 lastTarget;

    private MagManipEffect() {}

    /** A appeler a chaque tick client : voir {@code AbilityClientEvents.onClientTick}. */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        // Le portage du tick precedent ne vaut plus rien : les blocs qu'il ne reprendra pas
        // reprennent leur vol tout seuls. Et ceux du joueur local sont a lui, donc c'est ce
        // client-ci qui les fait vivre. Voir EntityMagManipBlock.markCarried.
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityMagManipBlock block)) continue;
            block.unmarkCarried();
            if (block.getOwner() == client.player) block.markOwnedByLocalPlayer();
        }

        int count = MagManipVisuals.arcsToSow(RANDOM);

        // Le bloc que CE client porte est seme par tickHeld, une fois sa pose du tick posee :
        // ici sa pose a un tick de retard, et l'essaim trainerait derriere lui des qu'il va vite.
        EntityMagManipBlock held = Minecraft.getInstance().player == null
                ? null : carried(Minecraft.getInstance().player);

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EntityMagManipBlock block)) continue;
            if (block == held) continue;
            sow(block, count);
        }
    }

    /**
     * L'essaim d'un bloc : quelques arcs tires DANS le cube qui l'entoure.
     *
     * <p>Les points sont tires DANS le cube, centre sur le bloc : c'est le
     * {@code CubePointFactory(...).setCentered(true)} de l'original, dont le port rend les memes
     * trois nombres par spread. Et on les prend sur la POSE DU JEU — celle du rendu — pour que le
     * gresillement colle au bloc que le joueur voit, et pas a la copie du reseau.
     */
    private static void sow(EntityMagManipBlock block, int count) {
        double half = MagManipVisuals.SURROUND_CUBE / 2.0;
        List<Vec3> points = SurroundArcs.spread(block.displayPosition(),
                MagManipVisuals.SURROUND_CUBE, -half, half, count, RANDOM);
        // Le proprietaire ne sert ici qu'a l'affichage a la premiere personne : c'est un bloc,
        // son identifiant ne sera jamais celui du joueur. L'original ne demandait rien.
        SurroundArcs.spawnAt(SurroundArcs.THIN, points, block.getId(), RANDOM);
    }

    /**
     * Le portage, cote client : le bloc suit le regard tout de suite, sans le detour du reseau.
     *
     * <p>Sans cela, le bloc n'avance que la ou le SERVEUR le met — il le porte de son cote, avec
     * le regard qu'il connait, et n'envoie sa position que tous les deux ticks. Immobile, cela ne
     * se voit pas : le point de portage ne bouge pas, donc les deux versions du bloc se
     * rejoignent. Mais des qu'on tourne la tete, le point de portage balaie un arc, le serveur
     * prend son virage un tick en retard, et le bloc traine derriere le curseur — c'est ce que le
     * joueur a decrit : « ca ne fait ce mouvement de ralenti que lorsque je tourne la tete ».
     *
     * <p>L'original portait le bloc des DEUX cotes : son contexte faisait `updateMoveTo` a chaque
     * tick chez le client comme chez le serveur, et chacun avancait sa copie de 0,2 bloc. Le port
     * fait la meme chose ici : la copie cliente avance vers le point de portage lu sur le regard
     * du joueur, tout de suite. Le serveur reste le maitre — ses positions arrivent tous les deux
     * ticks et recadrent la copie —, mais le mouvement ne l'attend plus.
     */
    public static void tickHeld(net.minecraft.client.player.LocalPlayer player,
                                cn.academy.ability.Skill skill) {
        if (skill != cn.academy.ability.electromaster.ElectromasterCategory.MAG_MANIP) return;

        EntityMagManipBlock block = carried(player);
        if (block == null) return;

        Vec3 target = MagManipVisuals.carryTarget(player.getEyePosition(1f),
                player.getViewVector(1f));
        // ON REPART DE NOTRE POSE, jamais de celle que le reseau vient de poser : c'est ce qui fait
        // que la copie cliente ne recule plus a chaque paquet. Voir displayPosition().
        Vec3 from = block.displayPosition();
        // Et un simple pas, sans collision, comme l'original : le bloc porte traverse ce que le
        // regard balaie au lieu de s'y accrocher — voir le commentaire de EntityMagManipBlock.
        //
        // Le point du tick precedent est a nous, et il repart de zero quand ce n'est plus le meme
        // bloc (une nouvelle prise, un lancer) : c'est lui qui donne le deplacement a suivre.
        if (block.getId() != lastBlockId) {
            lastBlockId = block.getId();
            lastTarget = null;
        }
        Vec3 step = MagManipVisuals.carryStep(from, target, lastTarget != null ? lastTarget : target);
        lastTarget = target;
        block.markCarried();
        block.setDeltaMovement(step);
        // La pose du tick, posee des maintenant : c'est elle que le rendu interpole (l'ancienne
        // etant celle d'ou l'on vient), et c'est elle aussi que suit l'essaim seme juste apres.
        block.poseCarried(from.add(step), from);
        // L'essaim se seme MAINTENANT, sur la pose du tick : seme avec le reste des blocs, il
        // partirait d'un tick en arriere et trainerait derriere le bloc des que le joueur tourne
        // la tete ou se deplace vite.
        sow(block, MagManipVisuals.arcsToSow(RANDOM));
    }

    /** Le bloc que le joueur porte : le sien, et le plus proche de ses yeux. */
    private static EntityMagManipBlock carried(net.minecraft.world.entity.player.Player player) {
        if (Minecraft.getInstance().level == null) return null;

        EntityMagManipBlock best = null;
        double bestDist = 36.0;
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (!(entity instanceof EntityMagManipBlock block)) continue;
            if (block.getOwner() != player) continue;
            double dist = block.distanceToSqr(player);
            if (dist < bestDist) {
                bestDist = dist;
                best = block;
            }
        }
        return best;
    }
}
