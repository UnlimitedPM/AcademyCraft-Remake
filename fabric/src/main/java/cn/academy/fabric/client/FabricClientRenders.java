package cn.academy.fabric.client;

import cn.academy.ability.client.arc.ArcRenderer;
import cn.academy.ability.client.MineDetectOverlay;
import cn.academy.ability.client.RailgunHandEffect;
import cn.academy.ability.client.md.MdEffects;
import cn.academy.ability.client.tp.BloodSplashRenderer;
import cn.academy.ability.client.tp.BloodSprayRenderer;
import cn.academy.ability.client.tp.FormulaParticleRenderer;
import cn.academy.ability.client.tp.TpMarkRenderer;
import cn.academy.ability.client.vm.GroundshockCamera;
import cn.academy.ability.client.vm.HandSwing;
import cn.academy.ability.client.vm.PlasmaBodyRenderer;
import cn.academy.ability.client.vm.SmokePuffRenderer;
import cn.academy.ability.client.vm.TornadoRenderer;
import cn.academy.ability.client.vm.VecAccelPreviewRenderer;
import cn.academy.ability.client.vm.WaveRenderer;
import cn.academy.client.SilbarnFrags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;

/**
 * Les effets de client du mod, dessines et ticks sous Fabric.
 *
 * <p>Tout ce qui se voit est commun — chaque rendu est une methode statique neutre depuis le
 * passage des rendus en {@code common/}. Ce qui n'existe pas sous Fabric, c'est le branchement :
 * Forge le fait dans une classe {@code <Nom>Events} par effet, Fabric en a deux evenements pour
 * tout. C'est donc ici que la liste est tenue, et c'est le seul endroit a completer quand un effet
 * de plus arrive.
 *
 * <p>Le stade : Forge dessine ces effets {@code AFTER_PARTICLES}, un stade que Fabric n'a pas. Le
 * plus proche est {@code AFTER_ENTITIES} — les blocs et les entites sont peints, les particules du
 * monde ne le sont pas encore. La difference se voit sur l'ordre avec les particules de vanilla,
 * pas sur les effets eux-memes.
 *
 * <p>Les ticks, eux, n'ont pas d'ordre qui compte : chacun vieillit ses propres effets.
 */
public final class FabricClientRenders {

    private FabricClientRenders() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Les entrees de l'aptitude d'abord : le relachement d'une touche doit couper sa boucle
            // de son au meme tick (voir AbilityClientEvents).
            cn.academy.ability.client.AbilityClientEvents.tick();
            ArcRenderer.tick();
            MineDetectOverlay.tick();
            RailgunHandEffect.tick();
            BloodSprayRenderer.tick();
            GroundshockCamera.tick();
            HandSwing.tick();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Camera camera = context.camera();
            PoseStack pose = new PoseStack();
            float ticks = context.tickDelta();

            // Les arcs d'abord : c'est le dessin le plus « dans le monde », et il prend sa propre
            // pose (il la pousse et la retire lui-meme).
            ArcRenderer.render(pose, camera.getPosition(), camera.getUpVector());

            MdEffects.render(camera, pose, ticks);
            BloodSplashRenderer.render(camera, pose, ticks);
            BloodSprayRenderer.render(camera, pose);
            FormulaParticleRenderer.render(camera, pose, ticks);
            TpMarkRenderer.render(camera, pose, ticks);
            PlasmaBodyRenderer.render(camera, pose, ticks);
            SmokePuffRenderer.render(camera, pose, ticks);
            TornadoRenderer.render(camera, pose, ticks);
            VecAccelPreviewRenderer.render(camera, pose, ticks);
            WaveRenderer.render(camera, pose, ticks);
            cn.academy.ability.client.JetEngineRenderer.render(camera, pose, ticks);
            cn.academy.ability.client.MagManipRenderer.render(camera, pose, ticks);
            cn.academy.ability.client.MineDetectRenderer.render(camera, pose, ticks);
            cn.academy.ability.client.ShieldRenderer.render(camera, pose, ticks);
            cn.academy.ability.client.ThunderClapRenderer.render(camera, pose, ticks);
            RailgunHandEffect.render(camera, pose, ticks);

            // Et les eclats de la bille, qui ont leur propre pose et leur propre tampon.
            SilbarnFrags.render(camera, new PoseStack());
        });

        // Le HUD des competences : le temoin de CP, le rappel des touches, les notifications et le
        // voile du renfort. Le lecteur media manque — son moteur est chez Forge.
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
                (graphics, tickDelta) -> {
                    var window = net.minecraft.client.Minecraft.getInstance().getWindow();
                    cn.academy.ability.client.AbilityHudOverlay.render(graphics,
                            window.getGuiScaledWidth(), window.getGuiScaledHeight());
                });
    }
}
