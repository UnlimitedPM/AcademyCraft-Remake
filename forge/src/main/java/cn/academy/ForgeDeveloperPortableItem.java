package cn.academy;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Le developeur portable cote Forge/NeoForge : l'objet et son energie sont communs, cette classe
 * n'ajoute que le rendu en main.
 *
 * <p>Portage du TEISR de l'original ({@code ItemDeveloper.initClient}). Le modele JSON ne peut pas
 * montrer l'image dans la case et le boitier en volume dans la main — d'ou ce rendu en code, qui
 * n'existe que chez Forge : {@code initializeClient} est une methode du chargeur.
 */
public class ForgeDeveloperPortableItem extends DeveloperPortableItem {

    public ForgeDeveloperPortableItem() {
        super();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private cn.academy.client.render.DeveloperPortableRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new cn.academy.client.render.DeveloperPortableRenderer();
                }
                return renderer;
            }
        });
    }
}
