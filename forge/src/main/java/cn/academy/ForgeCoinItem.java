package cn.academy;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * La piece cote Forge/NeoForge : l'objet et son lancer sont communs, cette classe n'ajoute que le
 * rendu en main.
 *
 * <p>Portage du {@code TileEntityItemStackRenderer} de l'original, par le moyen d'aujourd'hui : un
 * modele JSON ne sait dessiner que des boites, donc sa tranche est un carre (le joueur l'a vu deux
 * fois) et il ne peut pas montrer l'image telle quelle dans l'inventaire en montrant autre chose
 * dans la main. Voir {@code CoinItemRenderer}.
 */
public class ForgeCoinItem extends CoinItem {

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private cn.academy.client.CoinItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new cn.academy.client.CoinItemRenderer();
                }
                return renderer;
            }
        });
    }
}
