package cn.academy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BlockModelGenerator {
    public static void main(String[] args) {
        // Chemins (Vérifie qu'ils correspondent à ton projet)
        String texturePath = "src/main/resources/assets/academy/textures/block";
        String statePath = "src/main/resources/assets/academy/blockstates";
        String blockModelPath = "src/main/resources/assets/academy/models/block";
        String itemModelPath = "src/main/resources/assets/academy/models/item";

        File folder = new File(texturePath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            System.out.println("Dossier textures/block non trouvé !");
            return;
        }

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".png")) {
                String name = file.getName().replace(".png", "");

                generateBlockState(name, statePath);
                generateBlockModel(name, blockModelPath);
                generateItemModel(name, itemModelPath);

                System.out.println("Généré : " + name + " (State, BlockModel, ItemModel)");
            }
        }
    }

    private static void generateBlockState(String name, String path) {
        String content = "{\n" +
                "  \"variants\": {\n" +
                "    \"\": { \"model\": \"academy:block/" + name + "\" }\n" +
                "  }\n" +
                "}";
        writeFile(path + "/" + name + ".json", content);
    }

    private static void generateBlockModel(String name, String path) {
        String content = "{\n" +
                "  \"parent\": \"minecraft:block/cube_all\",\n" +
                "  \"textures\": {\n" +
                "    \"all\": \"academy:block/" + name + "\"\n" +
                "  }\n" +
                "}";
        writeFile(path + "/" + name + ".json", content);
    }

    private static void generateItemModel(String name, String path) {
        // J'avais mis une accolade "}" en trop ici ! Voici la version propre :
        String content = "{\n" +
                "  \"parent\": \"academy:block/" + name + "\"\n" +
                "}";
        writeFile(path + "/" + name + ".json", content);
    }

    private static void writeFile(String fullPath, String content) {
        try (FileWriter writer = new FileWriter(fullPath)) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}