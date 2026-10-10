package cn.academy.ability.electromaster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.ForgeEventFactory;

import java.lang.reflect.Method;

/**
 * Un villageois touche par l'eclair de l'electromaster devient une <b>sorciere</b>, comme sous la
 * foudre du jeu.
 *
 * <p>Le joueur : « je voudrais la meme chose avec les villageois pour qu'ils deviennent des
 * sorcieres comme c'est le cas dans minecraft vanilla » — puis, une fois la chose en jeu : « oui
 * effectivement je voudrais avec le taux de 30 % ». C'est la contrepartie du creeper charge (voir
 * {@link CreeperCharge}) : chez le jeu, les deux naissent du meme endroit — la foudre qui tombe sur
 * une bete — et comme le port ne lance pas de vrai eclair, il doit le faire lui-meme.
 *
 * <p>La regle est celle de vanilla, reprise instruction par instruction de
 * {@code Villager.thunderHit}, <b>relu au bytecode du jeu livre</b> :
 *
 * <ul>
 *   <li><b>trois chances sur dix</b>, comme le creeper — et c'est la le seul endroit ou le port
 *       s'ecarte du jeu : <b>vanilla ne tire aucun nombre au sort</b>, un villageois foudroye
 *       devient une sorciere a coup sur. Le joueur a demande le taux du creeper apres avoir vu la
 *       conversion fonctionner, et la borne est la meme que la sienne : stricte, donc 0,3 pile ne
 *       passe pas ;</li>
 *   <li>la difficulte <b>paisible</b> l'empeche — c'est la premiere ligne de vanilla ;</li>
 *   <li>seuls les <b>villageois</b> : ni le marchand ambulant (un {@code WanderingTrader} est un
 *       {@code AbstractVillager}, pas un {@code Villager}), ni le villageois zombie ;</li>
 *   <li>les <b>enfants</b> y passent aussi : le bytecode de vanilla ne pose aucune question d'age,
 *       et une sorciere n'a de toute facon pas de taille d'enfant ({@code Witch} n'a pas de
 *       {@code setBaby}) ;</li>
 *   <li>la sorciere herite du <b>nom</b> (« Georges » reste « Georges ») et du <b>sans IA</b> de
 *       l'ancien, et elle demande a ne pas etre oubliee au loin ({@code setPersistenceRequired}) ;</li>
 *   <li>les deux crochets de Forge, {@code canLivingConvert} et {@code onLivingConvert}, sont
 *       appeles la ou vanilla les appelle : un autre mod peut refuser la conversion. Attention a
 *       l'ordre, il est contre-intuitif — <b>le troisieme parametre de {@code canLivingConvert} est
 *       un consommateur VIDE chez vanilla</b> (relu au bytecode : {@code lambda$thunderHit$8} ne
 *       fait que revenir) et le crochet ne l'appelle PAS : la conversion se fait apres lui, et non
 *       dedans. La mettre dedans ne convertit jamais personne.</li>
 * </ul>
 *
 * <p>Deux choses que le port fait differemment, et il faut le dire :
 *
 * <ul>
 *   <li>Vanilla, quand la conversion n'a pas lieu, retombe sur {@code AbstractVillager.thunderHit}
 *       — le feu et les cinq degats de foudre. Ici il n'y a pas de {@code LightningBolt} a lui
 *       donner : les degats sont deja ceux de la competence, poses juste avant.</li>
 *   <li>Vanilla libere les <b>points d'interet</b> du villageois (son lit, son etablissement) par
 *       {@code releaseAllPois}, une methode <b>privee</b> — relue au bytecode, puis dans
 *       {@code srg_to_official_1.20.1.tsrg} pour son nom du jeu livre. Elle est donc atteinte par
 *       reflexion, et si elle ne repond pas, la conversion se fait quand meme : une place de
 *       village occupee par un fantome vaut mieux qu'un plantage au premier eclair.</li>
 * </ul>
 *
 * <p>Et une bete <b>morte</b> ne se convertit pas : l'eclair de la competence frappe d'abord, et un
 * villageois qui n'a pas survecu n'a plus rien a devenir. Vanilla ne peut pas tomber dans ce cas,
 * sa foudre ne choisissant que des vivants ; ici la competence peut tuer.
 */
public final class WitchConversion {

    /**
     * Le nom du jeu livre n'est pas devine, il est releve : la ligne
     * {@code m_35524_ ()V releaseAllPois} de {@code srg_to_official_1.20.1.tsrg}.
     */
    private static final String[] POI_RELEASE_NAMES = {"releaseAllPois", "m_35524_"};

    /** Trois chances sur dix : le meme chiffre que le creeper, et la meme borne stricte. */
    public static final float CHANCE = 0.3f;

    private static Method poiRelease;
    private static boolean looked;

    private WitchConversion() {
    }

    /**
     * Change ce villageois en sorciere, si le tirage passe, si c'est un villageois vivant, et si le
     * jeu le veut bien.
     *
     * @param target la bete que la competence vient de frapper
     * @param roll   le tirage, entre 0 et 1 (chaque competence tient son propre hasard)
     * @return vrai quand la conversion a eu lieu
     */
    public static boolean tryConvert(LivingEntity target, float roll) {
        if (roll >= CHANCE) {
            return false;
        }
        if (!(target instanceof Villager villager) || !villager.isAlive()) {
            return false;
        }
        if (!(villager.level() instanceof ServerLevel level)
                || level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        // Le crochet de Forge D'ABORD, la ou vanilla le pose, et il peut refuser. Son troisieme
        // parametre est le consommateur VIDE de vanilla — un autre mod y mettrait l'identifiant
        // d'une bete qu'il aurait fabriquee lui-meme — et le crochet NE L'APPELLE PAS : la
        // conversion se fait donc APRES lui, jamais dedans.
        if (!ForgeEventFactory.canLivingConvert(villager, EntityType.WITCH, ignoredId -> {
        })) {
            return false;
        }
        Witch witch = EntityType.WITCH.create(level);
        if (witch == null) {
            return false;
        }
        turn(level, villager, witch);
        return true;
    }

    /** La conversion elle-meme, instruction pour instruction celle de {@code Villager.thunderHit}. */
    private static void turn(ServerLevel level, Villager villager, Witch witch) {
        witch.moveTo(villager.getX(), villager.getY(), villager.getZ(),
                villager.getYRot(), villager.getXRot());
        witch.finalizeSpawn(level, level.getCurrentDifficultyAt(witch.blockPosition()),
                MobSpawnType.CONVERSION, null, null);
        witch.setNoAi(villager.isNoAi());
        if (villager.hasCustomName()) {
            witch.setCustomName(villager.getCustomName());
            witch.setCustomNameVisible(villager.isCustomNameVisible());
        }
        witch.setPersistenceRequired();
        ForgeEventFactory.onLivingConvert(villager, witch);
        level.addFreshEntityWithPassengers(witch);
        releasePois(villager);
        villager.discard();
    }

    /**
     * Rend au village le lit et l'etablissement du villageois qui s'en va.
     *
     * <p>Vanilla le fait dans sa conversion, sans quoi la place resterait occupee par une bete qui
     * n'existe plus : un poste de travail pris par un fantome, et le village qui n'y envoie plus
     * personne. La methode est privee, donc elle passe par son nom — les deux, celui du
     * developpement et celui du jeu livre.
     */
    private static void releasePois(Villager villager) {
        if (!looked) {
            looked = true;
            for (String name : POI_RELEASE_NAMES) {
                try {
                    Method method = Villager.class.getDeclaredMethod(name);
                    method.setAccessible(true);
                    poiRelease = method;
                    break;
                } catch (ReflectiveOperationException ignored) {
                    // Le nom suivant, ou rien du tout : la conversion se fait quand meme.
                }
            }
        }
        if (poiRelease == null) {
            return;
        }
        try {
            poiRelease.invoke(villager);
        } catch (ReflectiveOperationException ignored) {
            // Tant pis pour les points d'interet : la sorciere, elle, est bien la.
        }
    }
}
