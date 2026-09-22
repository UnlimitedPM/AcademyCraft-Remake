package cn.academy.ability.develop;

import cn.academy.ModItems;
import cn.academy.DeveloperPortableItem;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * Le developpeur portable : l'objet tenu, et l'apprentissage qui va avec.
 *
 * <p>Portage de {@code PortableDevData}, qui etait la seconde implementation de
 * {@code IDeveloper} — celle qui tire son energie d'un <b>objet</b> au lieu d'un tampon de
 * machine. L'objet lui-meme est {@link DeveloperPortableItem} ; ici vit ce qui n'appartient
 * ni a l'objet ni a la machine : l'avancement de celui qui s'en sert.
 *
 * <h2>Ou vit l'apprentissage, ici</h2>
 *
 * <p>Dans l'original, `DevelopData` etait une donnee <b>du joueur</b>, et le developeur
 * n'en etait qu'un outil : la progression suivait donc le joueur, portable ou machine. Le
 * port a mis celle de la machine sur la machine ; celle du portable est sur le joueur,
 * parce qu'elle n'a pas d'autre endroit ou vivre — l'objet, lui, ne retient que son
 * energie.
 *
 * <p>Consequence assumee, et voulue : ranger le portable pendant un apprentissage
 * l'interrompt, comme l'original qui ne trouvait plus d'objet a qui demander l'energie. Le
 * savoir, lui, reste au joueur.
 */
public class PortableDevData implements Developer, INBTSerializable<CompoundTag> {

    private final DevelopProgress progress = new DevelopProgress();

    /** Categorie visee, ou -1. */
    private int categoryId = -1;

    /** Competence visee, ou -1 si c'est le niveau de la categorie qui monte. */
    private int skillId = -1;

    /**
     * L'action en cours, non sauvegardee.
     *
     * <p>Le changement de categorie ne se deduit pas d'un identifiant : il faut l'avoir
     * decidee, avec ce que le joueur avait en main. On la garde donc pendant qu'elle se
     * deroule, et un rechargement la perd — l'apprentissage echoue alors, comme pour une
     * machine.
     */
    @Nullable
    private DevelopAction decided;

    /** Le porteur, non sauvegarde : il est repose a l'attachement de la capacite. */
    @Nullable
    private Player owner;

    public void setOwner(@Nullable Player owner) {
        this.owner = owner;
    }

    @Override
    public DeveloperType getDeveloperType() {
        return DeveloperType.PORTABLE;
    }

    /** L'objet tenu, ou {@code null} : hors de la main, le portable n'est plus rien. */
    @Nullable
    private ItemStack heldItem() {
        if (owner == null) return null;
        ItemStack stack = owner.getMainHandItem();
        return stack.is(ModItems.DEVELOPER_PORTABLE.get()) ? stack : null;
    }

    @Override
    public double getEnergy() {
        ItemStack stack = heldItem();
        return stack == null ? 0.0d : DeveloperPortableItem.getEnergy(stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) DeveloperType.PORTABLE.getEnergy();
    }

    /** Avancement de l'apprentissage, ou 0 s'il n'y en a pas. */
    public DevelopProgress getProgressData() {
        return progress;
    }

    @Override
    public double getProgress() {
        return progress.progress(getDeveloperType().getTps());
    }

    @Override
    public DevelopProgress.DevState getState() {
        return progress.getState();
    }

    @Override
    public int getCategoryId() {
        return categoryId;
    }

    @Override
    public int getSkillId() {
        return skillId;
    }

    /** Un objet tenu en main ne se raccorde a aucun reseau. */
    @Override
    public boolean isLinked() {
        return false;
    }

    // ------------------------------------------------------------------
    // Lancement et deroulement
    // ------------------------------------------------------------------

    @Override
    public boolean startDeveloping(ServerPlayer player, int requestedCategoryId,
                                   int requestedSkillId) {
        Category category = CategoryManager.INSTANCE.getCategory(requestedCategoryId);
        if (category == null) return false;

        DevelopAction candidate = buildAction(category, requestedSkillId);
        return candidate != null && startDeveloping(player, candidate);
    }

    @Override
    public boolean startDeveloping(ServerPlayer player, DevelopAction candidate) {
        if (progress.isDeveloping()) return false;
        // Sans objet en main, il n'y a rien pour payer : l'original refusait de la meme
        // facon, en ne trouvant simplement pas d'objet.
        if (heldItem() == null) return false;
        if (!candidate.validate(player, getDeveloperType())) return false;

        categoryId = candidate.getCategoryId();
        skillId = candidate.getSkillId();
        decided = candidate;
        progress.begin(candidate.getStimulations(player));
        return true;
    }

    /**
     * Un tick d'apprentissage.
     *
     * <p>C'est le porteur qui paie, avec son objet — et lui seul : le prix sort de la main
     * tendue, comme dans l'original ou l'energie venait de `IFItemManager.pull`. Si
     * l'objet a disparu ou n'a plus de quoi payer, tout est perdu, exactement comme une
     * machine qui n'est plus alimentee.
     */
    public void tick(ServerPlayer player) {
        if (!progress.isDeveloping()) return;

        ItemStack stack = heldItem();
        DevelopAction action = action();
        if (stack == null || action == null) {
            fail();
            return;
        }

        DeveloperType type = getDeveloperType();
        double cost = type.getEnergyPerTick();
        if (DeveloperPortableItem.getEnergy(stack) < cost) {
            fail();
            return;
        }
        DeveloperPortableItem.discharge(stack, cost);

        if (!progress.tick(type.getTps())) return;
        if (!progress.allStimulationsDone()) return;

        // Derniere verification, au dernier moment : un joueur qui perd son niveau ou sa
        // categorie entre-temps echoue apres avoir paye, comme avec la machine.
        boolean success = action.validate(player, type);
        if (success) action.onLearned(player);
        progress.finish(success);
        categoryId = -1;
        skillId = -1;
        decided = null;
    }

    @Override
    public void abort() {
        if (progress.isDeveloping()) fail();
    }

    private void fail() {
        progress.finish(false);
        categoryId = -1;
        skillId = -1;
        decided = null;
    }

    /** L'action en cours, ou celle qu'on peut reconstruire a partir de la cible. */
    @Nullable
    private DevelopAction action() {
        if (decided != null) return decided;
        Category category = CategoryManager.INSTANCE.getCategory(categoryId);
        if (category == null) return null;
        return buildAction(category, skillId);
    }

    @Nullable
    private static DevelopAction buildAction(Category category, int targetSkillId) {
        // Un changement de categorie ne se reconstruit pas : il se decide avec ce que le
        // joueur a en main. Une donnee rechargee ne le retrouve donc pas, et
        // l'apprentissage echoue — c'est le prix de ne pas sauvegarder d'action.
        if (targetSkillId == DevelopActionReset.SKILL_ID) return null;
        if (targetSkillId < 0) return new DevelopActionLevel(category);
        Skill skill = category.getSkill(targetSkillId);
        return skill == null ? null : new DevelopActionSkill(skill);
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = progress.serializeNBT();
        tag.putInt("category", categoryId);
        tag.putInt("skill", skillId);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        progress.deserializeNBT(tag);
        categoryId = tag.getInt("category");
        skillId = tag.contains("skill") ? tag.getInt("skill") : -1;
    }

    /** Reprend l'apprentissage d'un autre porteur, a la mort ou au changement de dimension. */
    public void copyFrom(PortableDevData other) {
        progress.copyFrom(other.progress);
        categoryId = other.categoryId;
        skillId = other.skillId;
    }
}
