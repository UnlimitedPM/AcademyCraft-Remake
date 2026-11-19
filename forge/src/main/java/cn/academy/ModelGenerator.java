package cn.academy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ModelGenerator {
    public static void main(String[] args) {
        // Chemins vers tes dossiers (vérifie qu'ils correspondent à ton PC)
        String texturePath = "src/main/resources/assets/academy/textures/item";
        String modelPath = "src/main/resources/assets/academy/models/item";

        File folder = new File(texturePath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            System.out.println("Dossier textures non trouvé !");
            return;
        }

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".png")) {
                String name = file.getName().replace(".png", "");
                generateJson(name, modelPath);
            }
        }
    }

    private static void generateJson(String name, String path) {
        String content = "{\n" +
                "  \"parent\": \"minecraft:item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"academy:item/" + name + "\"\n" +
                "  }\n" +
                "}";

        try (FileWriter writer = new FileWriter(path + "/" + name + ".json")) {
            writer.write(content);
            System.out.println("Généré : " + name + ".json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}