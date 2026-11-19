package cn.academy.entity;

import cn.academy.DeveloperBlockEntity;
import cn.academy.WindgenMainBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Le porteur d'ombre d'une machine : une entite vide, posee sur le bloc.
 *
 * <p>Un shader ne dessine pas les block entities dans sa carte d'ombre quand le pack ne le
 * demande pas — et Complementary le refuse dans TOUS ses profils ({@code shadowBlockEntities =
 * false}). Les pales de l'eolienne et les developpeurs n'avaient donc aucune ombre, quoi que le
 * mod fasse du cote d'Oculus. Les ENTITES, elles, projettent une ombre des que le pack le permet
 * ({@code shadowEntities}, vrai par defaut chez Complementary) : c'est le chemin des mobs.
 *
 * <p>D'ou cette entite : elle ne dessine rien a l'ecran, ne collisionne pas, ne se pioche pas, et
 * n'existe que chez le client. Son rendu ({@code ShadowCasterRenderer}) delegue au rendu de block
 * entity de la machine, et seulement pendant la carte d'ombre. Elle ne sert donc qu'a preter a la
 * machine le chemin d'ombre d'une entite.
 *
 * <p>Elle se detruit d'elle-meme des que la machine n'est plus la : c'est ce qui la rend sure
 * quand un chunk se decharge ou qu'une machine est cassee.
 */
public class EntityShadowCaster extends Entity {

    public EntityShadowCaster(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        // Aucun culling : ni le frustum, ni la portee de rendu des entites. Le porteur ne dessine
        // rien a l'ecran, et pendant la carte d'ombre c'est la CARTE qui decide de ce qu'elle
        // garde — pas la camera du joueur. Sans cela, une machine a quinze ou trente blocs sortait
        // du frustum des entites et son ombre disparaissait alors que le soleil la projetait
        // encore (le joueur, le 18/11).
        this.noCulling = true;
        setNoGravity(true);
    }

    /**
     * Toujours « assez proche » : la portee des entites ne doit pas decider des ombres.
     *
     * <p>Sans cette reponse, le jeu ecarte le porteur au-dela d'une portee calculee sur la taille
     * de sa boite, et l'ombre s'arrete bien avant la portee de la carte d'ombre.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        // Rien a synchroniser : l'entite n'existe que chez le client qui l'a posee.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Jamais sauvegardee : elle se reconstruit a partir des machines visibles.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Voir readAdditionalSaveData.
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            // Une entite pareille n'a rien a faire sur un serveur : elle vient d'un client.
            discard();
            return;
        }
        if (!casts(level().getBlockEntity(blockPosition()))) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** Vrai pour une machine que nos rendus savent ombrer. */
    public static boolean casts(BlockEntity entity) {
        return entity instanceof WindgenMainBlockEntity || entity instanceof DeveloperBlockEntity;
    }
}
