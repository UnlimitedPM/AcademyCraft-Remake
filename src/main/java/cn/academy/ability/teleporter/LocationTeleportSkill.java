package cn.academy.ability.teleporter;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Teleportation a la marque, portage de {@code LocationTeleport} : marquer des endroits, et
 * y retourner.
 *
 * <h2>Une competence qui passe par un ecran</h2>
 *
 * C'est la seule du port dont la touche n'allume rien : elle <b>ouvre la liste</b> des
 * endroits marques, et c'est en cliquant l'un d'eux qu'on part. L'original faisait exactement
 * cela, et c'est ce qui explique la forme de cette classe : elle ne se contente pas
 * d'executer un effet au relachement, elle offre une <b>liste</b> et une <b>action</b>, que
 * l'ecran appelle par identifiant.
 *
 * <h2>Le prix, et pourquoi il est bizarre</h2>
 *
 * Le cout ne depend pas de la marque mais de la <b>distance</b>, et il grandit comme sa
 * racine carree : {@code (200 a 150) x facteur}, ou le facteur vaut au moins 8 et vaut
 * {@code sqrt(distance)} au-dela de 64 blocs, la distance etant plafonnee a 800. Traverser
 * une dimension double la note. Meme un saut de cinq blocs coute donc son minimum, huit fois
 * la base — sur la reserve du port, une cinquantaine de points, soit la moitie de la barre.
 * La teleportation a la marque est un voyage, pas un deplacement.
 *
 * <h2>On ne part pas seul</h2>
 *
 * Tout ce qui vit autour du lanceur, dans un rayon de cinq blocs, part avec lui — a condition
 * de n'etre pas trop gros ({@code largeur x largeur x hauteur} sous 80). Les passagers d'un
 * bateau, les poules et les villageois suivent donc ; un golem ou un cheval, non. Chacun
 * garde son <b>ecart</b> avec le lanceur : le groupe arrive en formation, et pas empile.
 *
 * <h2>Changer de dimension</h2>
 *
 * Au-dela de <b>80 %</b> d'experience, comme dans l'original, une marque posee dans un autre
 * monde devient accessible — et le voyage coute alors le <b>double</b>. Toute la compagnie
 * suit, comme pour un saut ordinaire.
 *
 * <p>Une marque ne retient que le <b>nom</b> de sa dimension : le serveur peut donc ne pas la
 * connaitre, si le monde vient d'un mod qu'on a retire. Il le dit alors, au lieu de laisser
 * le joueur disparaitre.
 */
public class LocationTeleportSkill extends Skill {

    /** Surcout d'un saut : 240, quelle que soit la distance, comme l'original. */
    public static final float OVERLOAD = 240f;

    /** Distance au-dela de laquelle le prix ne grandit plus, en blocs. */
    public static final double MAX_PRICED_DISTANCE = 800.0;

    /** Facteur minimal du prix : meme un saut court coute huit fois la base. */
    public static final double MIN_FACTOR = 8.0;

    /** Experience a partir de laquelle on peut traverser une dimension. */
    public static final float CROSS_DIMENSION_EXP = 0.8f;

    /** Rayon dans lequel les compagnons sont emportes. */
    public static final double GROUP_RADIUS = 5.0;

    /** Volume maximal d'un compagnon : largeur x largeur x hauteur. */
    public static final double GROUP_SIZE_LIMIT = 80.0;

    /** Recharge d'un saut, de trois a deux secondes. */
    public static final int MIN_COOLDOWN = 20;
    public static final int MAX_COOLDOWN = 30;

    public LocationTeleportSkill() {
        super("location_teleport", 3);
    }

    /**
     * Cette competence ouvre un ecran au lieu de partir : c'est ce que le client lit pour
     * savoir qu'il ne doit rien envoyer a l'appui.
     */
    @Override
    public boolean opensScreen() {
        return true;
    }

    /** Le surcout, fixe : voir {@link #OVERLOAD}. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return OVERLOAD;
    }

    /**
     * Aucun cout a l'allumage : la competence ne part pas a l'appui, elle ouvre une liste.
     *
     * Le prix du voyage se paie sur place, quand une marque est choisie — voir {@link #perform}.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /**
     * Le prix d'un saut : {@code (200 a 150) x facteur x (2 si l'on change de dimension)}.
     *
     * Les deux bornes de l'original sont divisees par 28, comme les autres couts en CP du
     * port ; le facteur, lui, est un nombre pur et reste tel quel. Le resultat se lit en
     * points de la reserve du port : une cinquantaine pour un saut court, davantage pour un
     * long.
     */
    public float cpCost(AbilityData data, double distance, boolean crossDimension) {
        float base = lerp(7.14f, 5.36f, data.getSkillExp(this));
        double factor = Math.max(MIN_FACTOR,
                Math.sqrt(Math.min(MAX_PRICED_DISTANCE, distance)));
        return (float) (base * (crossDimension ? 2.0 : 1.0) * factor);
    }

    /** La traversee de dimension demande 80 % d'experience, comme l'original. */
    public boolean canCrossDimension(AbilityData data) {
        return data.getSkillExp(this) > CROSS_DIMENSION_EXP;
    }

    /** Vrai si la marque est dans une autre dimension que le joueur. */
    public static boolean crossDimension(Player player, LocationMark mark) {
        return !LocationMark.of(player.level()).equals(mark.dimension());
    }

    /**
     * Le niveau d'une marque, ou {@code null} si le serveur ne connait pas cette dimension.
     *
     * <p>Une marque ne retient que le <b>nom</b> de sa dimension — c'est ce qui la rend
     * testable sans registre — donc le serveur peut tres bien ne pas connaitre ce nom : un
     * monde retire par un mod, une sauvegarde d'un autre pack. D'ou le refus explicite
     * plutot qu'un plantage.
     */
    public static ServerLevel dimensionOf(ServerPlayer player, LocationMark mark) {
        ResourceLocation key = ResourceLocation.tryParse(mark.dimension());
        if (key == null) return null;
        return player.server.getLevel(ResourceKey.create(Registries.DIMENSION, key));
    }

    /** La distance entre le joueur et sa marque, en blocs. */
    public static double distanceTo(Player player, LocationMark mark) {
        return Math.sqrt(player.position().distanceToSqr(mark.x(), mark.y(), mark.z()));
    }

    /** Recharge : posee par l'effet, avec ce qu'il a porte. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    /** La recharge qu'un saut pose : de trois secondes a deux, comme l'original. */
    public int cooldown(AbilityData data) {
        return (int) lerp(MAX_COOLDOWN, MIN_COOLDOWN, data.getSkillExp(this));
    }

    /** Un saut long rapporte plus : voir {@link #earnsExpOnEffect()}. */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /** L'experience d'un saut : 0,03 au-dela de 200 blocs, 0,015 en deca. */
    public float expFor(double distance) {
        return distance >= 200 ? 0.03f : 0.015f;
    }

    /**
     * Emporte le joueur — et sa compagnie — jusqu'a la marque.
     *
     * <p>Rend {@code false} si le saut n'a pas eu lieu, et dit pourquoi au joueur. L'ecran
     * grise deja les marques inaccessibles, mais un ecran ne decide rien : c'est ici que le
     * dernier mot est dit, sur un serveur qui seul connait la reserve et la dimension.
     */
    public boolean perform(ServerPlayer player, AbilityData data, int markId) {
        LocationMark dest = data.getMark(markId);
        if (dest == null) return false;

        boolean cross = crossDimension(player, dest);
        ServerLevel target = null;
        if (cross) {
            if (!canCrossDimension(data)) {
                player.displayClientMessage(
                        Component.translatable("ac.ability.teleporter.location_teleport.err_exp"),
                        true);
                return false;
            }
            target = dimensionOf(player, dest);
            if (target == null) {
                // Le serveur ne connait pas cette dimension : une marque posee sur un monde
                // qu'on a retire, par exemple. Le dire vaut mieux qu'y disparaitre.
                player.displayClientMessage(
                        Component.translatable("ac.ability.teleporter.location_teleport.err_dim"),
                        true);
                return false;
            }
        }

        double distance = distanceTo(player, dest);
        // L'original payait sans verifier (`consumeWithForce`) : le prix a deja ete montre a
        // l'ecran, et refuser ici laisserait le joueur devant un bouton qui ne fait rien. Et
        // le facteur vaut 2 des qu'on change de dimension, comme chez lui.
        data.performForced(cpCost(data, distance, cross), OVERLOAD);

        Vec3 from = player.position();
        for (LivingEntity companion : companions(player)) {
            Vec3 offset = companion.position().subtract(from);
            if (companion.isPassenger()) companion.stopRiding();
            if (target != null) {
                companion.teleportTo(target, dest.x() + offset.x, dest.y() + offset.y,
                        dest.z() + offset.z, java.util.Set.of(), companion.getYRot(),
                        companion.getXRot());
            } else {
                companion.teleportTo(dest.x() + offset.x, dest.y() + offset.y, dest.z() + offset.z);
            }
            companion.fallDistance = 0.0f;
        }

        if (player.isPassenger()) player.stopRiding();
        if (target != null) {
            player.teleportTo(target, dest.x(), dest.y(), dest.z(), player.getYRot(),
                    player.getXRot());
        } else {
            player.teleportTo(dest.x(), dest.y(), dest.z());
        }
        player.fallDistance = 0.0f;
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.TP_TP, 0.5f);

        data.addSkillExp(this, expFor(distance));
        data.setCooldown(this, cooldown(data));
        TeleporterCategory.DIM_FOLDING_THEOREM.onTeleported(data);
        return true;
    }

    /**
     * Ceux qui partent avec le lanceur.
     *
     * Le filtre de taille est celui de l'original : {@code largeur x largeur x hauteur} sous
     * 80, ce qui laisse passer un villageois, une poule ou un joueur, et ecarte un cheval
     * comme un golem. Ils sont cherches dans la boite du joueur plutot que dans une sphere,
     * ce qui revient au meme a cette echelle.
     */
    private static List<LivingEntity> companions(Player player) {
        return player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(GROUP_RADIUS),
                e -> e != player && e.isAlive()
                        && e.getBbWidth() * e.getBbWidth() * e.getBbHeight() < GROUP_SIZE_LIMIT);
    }
}
