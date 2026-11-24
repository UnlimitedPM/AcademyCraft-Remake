package cn.academy.fabric.client;

import cn.academy.PhaseGeneratorBlock;
import cn.academy.SolarGenBlock;
import cn.academy.WindgenBaseBlock;
import cn.academy.client.render.ObjModels;
import cn.academy.fabric.FabricContent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Les machines qui n'ont pas de modele de bloc, cote Fabric.
 *
 * <p>Quatre d'entre elles sont dessinees par un fichier OBJ que Forge posait par son chargeur de
 * modele — absent sous Fabric, ou elles ne se voyaient pas du tout. Elles passent donc ici par un
 * rendu de block entity, avec les chiffres releves dans leurs fichiers de modele, qui restent la
 * reference : {@code models/block/solar_gen.json}, {@code phase_generator_N.json},
 * {@code windgen_base.json} et {@code windgen_main.json}.
 *
 * <p>Le matrix, lui, n'a jamais eu de modele de bloc : c'est deja un rendu de block entity — commun,
 * et partage avec Forge. Il est simplement declare ici.
 *
 * <p>Rien de tout cela n'est propre a Fabric dans son dessin : ce fichier ne dit que <b>ou</b> poser
 * chaque modele, et c'est la seule chose que le chargeur de modele de Forge savait faire pour nous.
 */
public final class FabricMachineModels {

    private FabricMachineModels() {
    }

    public static void register() {
        // La generatrice solaire : son panneau incline. Le modele est ecrit en grands nombres (il
        // vient de la 1.12.2) et tient dans un bloc une fois reduit a 1,4 % — le chiffre du modele
        // de Forge, garde tel quel.
        BlockEntityRenderers.register(FabricContent.SOLAR_GEN_TYPE,
                FabricObjRenderer.provider(new FabricObjRenderer.Placement(
                        ObjModels.model("solar"),
                        state -> ObjModels.texture("solar_model"),
                        0.5d, 0.0d, 0.5d, 0.014f,
                        SolarGenBlock.FACING, false)));

        // La machine a phase : cinq images selon son niveau, et la meme coque.
        BlockEntityRenderers.register(FabricContent.PHASE_GEN_TYPE,
                FabricObjRenderer.provider(new FabricObjRenderer.Placement(
                        ObjModels.model("ip_gen"),
                        state -> ObjModels.texture(
                                "ip_gen" + state.getValue(PhaseGeneratorBlock.LEVEL) + "_model"),
                        0.5d, 0.0d, 0.5d, 1.0f,
                        PhaseGeneratorBlock.FACING, false)));

        // Le pied de l'eolienne : deux images, celle du repos et celle qui tourne.
        BlockEntityRenderers.register(FabricContent.WINDGEN_BASE_TYPE,
                FabricObjRenderer.provider(new FabricObjRenderer.Placement(
                        ObjModels.model("windgen_base"),
                        state -> ObjModels.texture(state.getValue(WindgenBaseBlock.POWERED)
                                ? "windgen_base_model"
                                : "windgen_base_disabled_model"),
                        0.5d, -0.006d, 0.5d, 1.0f,
                        WindgenBaseBlock.FACING, true)));

        // La nacelle : le corps ET les pales — le rendu des pales est commun, il vient de Forge.
        BlockEntityRenderers.register(FabricContent.WINDGEN_MAIN_TYPE,
                FabricWindgenMainRenderer.provider());

        // Le matrix : ses neuf blocs, dessines par le rendu commun.
        BlockEntityRenderers.register(FabricContent.MATRIX_TYPE,
                cn.academy.client.render.MatrixRenderer::new);
    }
}
