package cn.academy.terminal.tutorial;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le markdown des tutoriels, mis a plat en morceaux dessinables.
 *
 * <p>Les fichiers de l'original sont du markdown tres simple : des paragraphes, quelques
 * mots en gras ({@code __texte__}), des images ({@code ![titre](chemin)}) et des touches
 * ({@code ![key id="ability_activation"]}). Le rendu de l'original etait un
 * {@code GLMarkdownRenderer} de LambdaLib2 ; le port n'en a pas, et n'en a pas besoin :
 * <b>la mise en page appartient a l'ecran</b>, qui a deja ses polices et ses mesures.
 *
 * <p>Ce que cette classe fait, et elle seule : couper le texte en morceaux, reconnaitre une
 * image (une ligne qui n'est QUE ca), et remplacer une balise de touche par le nom qu'on lui
 * donne. Le reste — retour a la ligne, taille, defilement — vit dans l'ecran.
 *
 * <p>Elle ne touche ni a Minecraft ni au client : {@code TutorialMarkdownTest} la relit
 * sans demarrer le jeu, et c'est ce qui permet de garder les treize tutoriels livres
 * verifiables.
 */
public final class TutorialMarkdown {

    /**
     * Un morceau : soit une ligne de texte ({@code text}), soit une image ({@code image}).
     *
     * <p>Une ligne de texte vide est un saut de paragraphe, et non un oubli.
     */
    public record Block(String text, String image) {

        public boolean isImage() {
            return image != null;
        }
    }

    /** Une ligne qui n'est QUE l'image : le titre du lien ne sert qu'a la lecture du fichier. */
    private static final Pattern IMAGE_LINE =
            Pattern.compile("^!\\[[^\\]]*\\]\\(([^)]+)\\)$");

    /** La balise de touche, telle qu'elle s'ecrit dans les fichiers. */
    private static final Pattern KEY_TAG =
            Pattern.compile("!\\[key\\s+id=\"([^\"]+)\"]");

    private TutorialMarkdown() {}

    /**
     * Met un contenu en morceaux.
     *
     * @param content le contenu du tutoriel, deja passe par {@code TutorialText}
     * @param keyName le nom d'une touche, ou {@code null} si elle est inconnue
     */
    public static List<Block> parse(String content, Function<String, String> keyName) {
        List<Block> blocks = new ArrayList<>();
        if (content == null || content.isEmpty()) return blocks;

        // Les trois balises de structure sont deja parties (TutorialText les a lues) : il ne
        // reste que le corps. Mais un fichier peut garder une balise de titre au milieu d'un
        // paragraphe, et on ne veut pas la dessiner.
        for (String raw : content.split("\\r?\\n", -1)) {
            String line = raw.trim();

            Matcher image = IMAGE_LINE.matcher(line);
            if (image.matches()) {
                blocks.add(new Block(null, image.group(1).trim()));
                continue;
            }

            if (line.isEmpty()) {
                // Deux vides de suite ne font qu'un paragraphe. Apres une image, le vide est
                // garde : c'est lui qui separe la capture du texte qui la commente.
                TutorialMarkdown.Block last = blocks.isEmpty() ? null
                        : blocks.get(blocks.size() - 1);
                if (last == null || last.isImage() || !last.text().isEmpty()) {
                    blocks.add(new Block("", null));
                }
                continue;
            }

            blocks.add(new Block(inline(line, keyName), null));
        }

        // Un paragraphe vide en fin de contenu ne sert a rien.
        while (!blocks.isEmpty() && !blocks.get(blocks.size() - 1).isImage()
                && blocks.get(blocks.size() - 1).text().isEmpty()) {
            blocks.remove(blocks.size() - 1);
        }
        return blocks;
    }

    /**
     * Le texte d'une ligne, sans ses balises.
     *
     * <p>Les touches sont remplacees par leur nom (l'original ecrivait {@code V}, {@code Left
     * Alt}...) ; une touche inconnue donne {@code ???}, comme chez lui. Le gras et l'italique
     * perdent leurs marques : la police du port n'a pas de variante a dessiner ici, et des
     * etoiles au milieu d'une phrase se liraient comme une faute.
     */
    static String inline(String line, Function<String, String> keyName) {
        String out = KEY_TAG.matcher(line).replaceAll(result -> {
            String name = keyName == null ? null : keyName.apply(result.group(1));
            return Matcher.quoteReplacement(name == null || name.isEmpty() ? "???" : name);
        });

        out = out.replace("__", "").replace("**", "");
        return out;
    }
}
