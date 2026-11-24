package cn.academy;

import cn.academy.sound.AcademySounds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * La piece : l'objet qu'on LANCE pour armer le railgun.
 *
 * <p>Portage d'{@code ItemCoin}, et c'est tout ce qu'il fait — une piece ne sert a rien d'autre. Le
 * clic droit la jette en l'air, elle monte, retombe en suivant son lanceur, et revient dans
 * l'inventaire si le railgun n'a pas tire a travers — voir {@link cn.academy.entity.EntityCoinThrowing}.
 * Le joueur n'en jette qu'une a la fois : l'original refusait le second lancer tant que la premiere
 * vivait, et c'est cette piece-la que le tir consomme.
 *
 * <p>Le son ne se joue que chez le SERVEUR : {@code Item#use} est appele des deux cotes, et un son
 * pose aux deux s'entendrait deux fois.
 *
 * <p>Le rendu en main, lui, reste chez Forge : {@code initializeClient} n'existe pas ailleurs. Voir
 * {@code ForgeCoinItem} et {@code CoinItemRenderer}.
 */
public class CoinItem extends Item {

    public CoinItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Une seule piece par joueur, et le client le sait comme le serveur : sa propre piece est
        // chez lui. Refuser ici evite le geste de main pour rien.
        if (cn.academy.entity.EntityCoinThrowing.of(player) != null) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            // L'ANIMATION SPECIALE, et elle n'appartient qu'au client : la rafale d'arcs qui
            // jaillit de la main d'un electromaster qui a son railgun pret. Voir RailgunHandEffect,
            // ou vivent les deux verrous de l'original.
            cn.academy.ability.client.RailgunHandEffect.onCoinThrown(player);
            return InteractionResultHolder.success(stack);
        }

        level.addFreshEntity(new cn.academy.entity.EntityCoinThrowing(level, player));
        // L'ANIMATION SPECIALE : le serveur l'annonce a ceux qui voient le lanceur, comme l'original
        // (MSG_CHARGE_EFFECT, trente blocs a la ronde), et il relit pour cela ses deux verrous. Le
        // lanceur la joue deja chez lui — voir le client, plus haut — et son client ignore l'annonce
        // en vue de premiere personne : elle ne lui revient que s'il regarde son propre modele, ou
        // il n'a pas de main a l'ecran.
        cn.academy.ability.electromaster.RailgunSkill.announceHandEffectIfAllowed(player);
        AcademySounds.playAt(level, player.position(), "entity.flipcoin", 0.5f, 1.0f);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
