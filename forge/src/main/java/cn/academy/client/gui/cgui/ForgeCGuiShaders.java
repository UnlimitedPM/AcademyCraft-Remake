package cn.academy.client.gui.cgui;

import cn.academy.AcademyCraft;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Chargement des programmes du moteur d'interface, cote Forge/NeoForge.
 *
 * <p>La logique de dessin est commune ; seul l'evenement d'enregistrement est propre au chargeur.
 * Chaque programme est compile dans son propre essai : un fichier illisible laisse son champ nul et
 * le dessin s'abstient, au lieu d'empecher le client de demarrer.
 */
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ForgeCGuiShaders {

    private ForgeCGuiShaders() {
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        CGuiShaders.resetPrograms();
        for (String name : CGuiShaders.PROGRAMS) {
            try {
                ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                        CGuiShaders.programId(name), DefaultVertexFormat.POSITION_COLOR_TEX);
                CGuiShaders.install(name, shader);
                event.registerShader(shader, loaded -> CGuiShaders.install(name, loaded));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("academy-cgui")
                        .warn("Le programme {} n'a pas pu etre compile ; le dessin s'abstiendra", name, e);
            }
        }
    }
}
