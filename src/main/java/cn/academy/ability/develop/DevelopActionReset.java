package cn.academy.ability.develop;

import cn.academy.FactorItem;
import cn.academy.ModItems;
import cn.academy.ability.AbilityCapability;
import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Changer de categorie : oublier tout ce qu'on sait, et repartir dans une autre.
 *
 * <p>Portage de {@code DevelopActionReset}. C'est le troisieme apprentissage du
 * developpeur, et le seul que la machine <b>avancee</b> sache mener : il ne fait pas
 * monter un niveau, il remplace un arbre entier par un autre, en perdant un cran au
 * passage.
 *
 * <h2>Les trois cles de l'original</h2>
 *
 * <p>Il fallait un joueur deja avance (niveau 3 au moins), la machine avancee, une
 * <b>bobine magnetique</b> en main — elle est consommee — et un <b>facteur d'induction</b>
 * d'une autre categorie dans l'inventaire : c'est ce facteur qui dit vers quoi on
 * bascule, et il est consomme lui aussi. Le port garde les trois. La seule adaptation est
 * de savoir <b>quelle</b> categorie on quitte : l'original n'en avait qu'une, le port en
 * tient une par categorie et prend donc la <b>plus haute</b>, comme il le fait deja pour
 * le palier de surcout.
 */
public class DevelopActionReset implements DevelopAction {

    /** Le niveau minimal pour songer a changer de categorie. */
    public static final int MIN_LEVEL = 3;

    /**
     * L'identifiant de competence qui dit « changement de categorie ».
     *
     * <p>Un apprentissage se designe par {@code (categorie, competence)}, avec -1 pour
     * « le niveau de la categorie ». Ce -2 est la troisieme sorte d'apprentissage : il
     * permet a une machine de se rappeler ce qu'elle faisait sans garder l'action
     * elle-meme.
     */
    public static final int SKILL_ID = -2;

    private final Category abandoned;
    private final Category adopted;

    public DevelopActionReset(Category abandoned, Category adopted) {
        this.abandoned = abandoned;
        this.adopted = adopted;
    }

    /** La categorie qu'on quitte. */
    public Category getAbandoned() {
        return abandoned;
    }

    /** La categorie qu'on prend, celle du facteur consomme. */
    public Category getAdopted() {
        return adopted;
    }

    @Override
    public int getCategoryId() {
        return abandoned.getCategoryId();
    }

    @Override
    public int getSkillId() {
        return SKILL_ID;
    }

    /** Dix stimulations par niveau a oublier : l'original comptait exactement cela. */
    @Override
    public int getStimulations(Player player) {
        return levelOf(player) * 10;
    }

    @Override
    public boolean validate(Player player, DeveloperType developer) {
        // Tout est reverifie au dernier moment, y compris ce qui a servi a choisir
        // l'action : le joueur peut avoir range sa bobine ou son facteur entre-temps, et
        // il echoue alors apres avoir paye — c'est la regle de l'original.
        DevelopActionReset again = find(player, developer);
        return again != null && again.getAbandoned() == abandoned
                && again.getAdopted() == adopted;
    }

    @Override
    public void onLearned(Player player) {
        AbilityData data = dataOf(player);
        if (data == null) return;

        // L'original baissait d'un cran en changeant de categorie : on ne repart pas de
        // zero, mais on paie un niveau pour le voyage.
        int carried = Math.max(0, data.getCategoryLevel(abandoned) - 1);

        // On oublie l'arbre entier — c'est ce que faisait `setCategory`, qui remplacait la
        // categorie du joueur, et avec elle tout ce qu'il savait.
        for (Skill skill : abandoned.getSkills()) {
            data.forgetSkill(skill);
        }
        data.setCategoryLevel(abandoned, 0);
        data.setCategoryLevel(adopted, carried);

        // Les prereglages ranges les allumaient : ils ne veulent plus rien dire. L'original
        // les effacait sur le meme evenement.
        cn.academy.ability.preset.PresetTracker.clearOnCategoryChange(player);

        // La bobine et le facteur y passent, comme dans l'original.
        player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        FactorItem.consume(player);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            cn.academy.advancements.AcademyAdvancements.award(serverPlayer,
                    cn.academy.advancements.AcademyAdvancements.CONVERT_CATEGORY);
        }
    }

    /**
     * L'action de changement de categorie, ou {@code null} si une condition manque.
     *
     * <p>Une seule fonction pour les trois cles, parce que le menu a besoin des trois a la
     * fois : c'est elle que le bouton de l'ecran interroge, et elle que le serveur
     * reverifie au clic et a la fin.
     */
    @Nullable
    public static DevelopActionReset find(Player player, DeveloperType developer) {
        if (developer != DeveloperType.ADVANCED) return null;

        AbilityData data = dataOf(player);
        if (data == null) return null;

        Category abandoned = abandonedCategory(data, MIN_LEVEL);
        if (abandoned == null) return null;

        if (!player.getMainHandItem().is(ModItems.MAGNETIC_COIL.get())) return null;

        Category adopted = FactorItem.otherCategoryIn(player, abandoned);
        if (adopted == null) return null;

        return new DevelopActionReset(abandoned, adopted);
    }

    /**
     * Le nom de la categorie qu'un joueur sacrifierait : la plus haute, si elle passe le seuil.
     *
     * <p>Pure, donc relue en JUnit : c'est la seule decision un peu nouee de tout le
     * changement de categorie, parce que l'original n'avait qu'une categorie par joueur et
     * que le port doit donc choisir la sienne. Les ex aequo se tranchent par le nom, pour
     * que deux categories au meme niveau donnent toujours la meme reponse.
     */
    @Nullable
    public static String abandonedCategoryName(AbilityData data, int minLevel) {
        String best = null;
        int bestLevel = 0;
        for (var entry : data.getCategoryLevels().entrySet()) {
            int level = entry.getValue();
            if (level > bestLevel || (level == bestLevel && level > 0
                    && best != null && entry.getKey().compareTo(best) < 0)) {
                best = entry.getKey();
                bestLevel = level;
            }
        }
        return bestLevel >= minLevel ? best : null;
    }

    /** La categorie qu'un joueur sacrifierait, ou {@code null} si aucune ne passe le seuil. */
    @Nullable
    public static Category abandonedCategory(AbilityData data, int minLevel) {
        String name = abandonedCategoryName(data, minLevel);
        return name == null ? null : CategoryManager.INSTANCE.getCategory(name);
    }

    /** Le niveau de la categorie qu'on quitte, 0 si elle n'a jamais ete apprise. */
    private int levelOf(Player player) {
        AbilityData data = dataOf(player);
        return data == null ? 0 : data.getCategoryLevel(abandoned);
    }

    @Nullable
    private static AbilityData dataOf(Player player) {
        return player.getCapability(AbilityCapability.ABILITY_DATA).resolve().orElse(null);
    }

    /** Le prix d'un changement annonce a l'ecran, sans construire l'action. */
    public static int stimulationsFor(AbilityData data) {
        String abandoned = abandonedCategoryName(data, MIN_LEVEL);
        return abandoned == null ? 0 : data.getCategoryLevels().getOrDefault(abandoned, 0) * 10;
    }
}
