package cn.academy.client.hud;

import cn.academy.platform.config.ConfigSpec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * La place des elements du HUD, sauvegardee dans la config du mod.
 *
 * <p>C'est ce que faisait l'original : une categorie {@code gui}, une cle par element, et le
 * panneau <i>Customize UI</i> qui reecrit la valeur des qu'on valide un champ. La place du HUD
 * appartient donc au <b>client</b> qui joue, pas au serveur — d'ou un fichier a part,
 * {@code academy-client.toml}, la ou le chargeur range les reglages du client.
 *
 * <p>Le fichier est ecrit par {@link ConfigSpec}, la meme petite lecture TOML que la config
 * commune : Forge n'est plus nomme ici, et le meme code sert les trois chargeurs.
 */
public final class HudConfig {

    public static final ConfigSpec SPEC;

    /**
     * Le volume du lecteur media, de zero a un.
     *
     * <p>Il vit ici parce que c'est la config du CLIENT, et que le volume appartient au joueur qui
     * ecoute, pas au serveur : le joueur : « quand je me deco/reco le jeu garde en memoire
     * l'intensite du volume que j'ai choisit, parce que devoir le rechanger a chaque fois c'est
     * chiant ». Une preference de client va dans la config du client.
     */
    private static final ConfigSpec.DoubleValue MEDIA_VOLUME;

    private static final Map<HudElement, ConfigSpec.DoubleListValue> VALUES =
            new EnumMap<>(HudElement.class);

    /** Le minerai mis en evidence par la detection de minerais : voir {@link #mineDetectOre}. */
    private static final ConfigSpec.StringValue MINE_DETECT_ORE;

    static {
        ConfigSpec.Builder builder = new ConfigSpec.Builder();
        builder.comment("Ou se posent les elements du HUD. Reglables en jeu par",
                        "Settings > Misc > Customize UI, dans le terminal de donnees.")
                .push("gui");

        for (HudElement element : HudElement.values()) {
            // Une liste de deux doubles, et non deux cles : c'est la forme de l'original, et
            // elle se relit d'un coup.
            VALUES.put(element, builder
                    .comment("Position de « " + element.getName() + " » : ecart en pixels depuis son ancrage.",
                            "Defaut de l'original : " + element.getDefaultX() + " / " + element.getDefaultY())
                    .defineList(element.getName(),
                            List.of(element.getDefaultX(), element.getDefaultY()),
                            -100000d, 100000d));
        }

        builder.pop();

        builder.comment("Le volume du lecteur media, de zero a un : il est retenu d'une session a",
                        "l'autre, comme les reglages du jeu.",
                        "The media player's volume, from zero to one; kept from one session to the next.")
                .push("media");
        MEDIA_VOLUME = builder.defineInRange("volume", 1.0d, 0.0d, 1.0d);
        builder.pop();

        builder.comment("Le minerai que la detection de minerais met en EVIDENCE : les autres",
                        "restent bleus. Vide : aucun n'est mis en avant.",
                        "The ore the mine detector highlights; the others stay blue.")
                .push("mine_detect");
        MINE_DETECT_ORE = builder.define("ore", "");
        builder.pop();

        SPEC = builder.build();
    }

    private HudConfig() {}

    /** Lit la config du client, que le chargeur vient de localiser. */
    public static void load(Path configDir) {
        if (configDir == null) return;
        SPEC.setFile(configDir.resolve("academy-client.toml"));
        SPEC.load();
    }

    /** La place de chaque element, telle qu'elle est ecrite dans la config. */
    public static HudLayout read() {
        HudLayout layout = new HudLayout();
        for (HudElement element : HudElement.values()) {
            ConfigSpec.DoubleListValue value = VALUES.get(element);
            if (value == null) continue;

            List<Double> pair;
            try {
                pair = value.get();
            } catch (Exception e) {
                // Une config illisible ne doit pas empecher le HUD de s'afficher : on garde
                // les defauts de l'original.
                continue;
            }
            if (pair == null || pair.size() < 2) continue;
            layout.setRaw(element, pair.get(0), pair.get(1));
        }
        return layout;
    }

    /**
     * Ecrit la nouvelle place d'un element et sauvegarde.
     *
     * <p>Rend faux si la valeur sort des bornes : c'est ce refus qui fait passer le champ au
     * rouge dans l'ecran de reglage.
     */
    public static boolean write(HudElement element, double x, double y) {
        if (!HudLayout.isValid(x) || !HudLayout.isValid(y)) return false;

        ConfigSpec.DoubleListValue value = VALUES.get(element);
        if (value == null) return false;

        List<Double> pair = new ArrayList<>(2);
        pair.add(x);
        pair.add(y);
        value.set(pair);
        save();
        return true;
    }

    /** Reecrit le fichier. Une sauvegarde impossible laisse la valeur en memoire. */
    public static void save() {
        SPEC.save();
    }

    /**
     * Le volume du lecteur media tel qu'il a ete quitte.
     *
     * <p>Rend un quand la config n'est pas encore lue (au demarrage) : c'est le volume de l'original.
     */
    public static float mediaVolume() {
        try {
            return MEDIA_VOLUME.get().floatValue();
        } catch (Exception e) {
            return 1.0f;
        }
    }

    /** Ecrit le volume du lecteur media et sauvegarde : c'est la glissiere de l'application. */
    public static void writeMediaVolume(float volume) {
        // Arrondi au centieme : un flottant ecrit tel quel donne « 0.4000000059604645 » dans le
        // fichier, et personne n'a envie de lire ca pour regler son volume.
        MEDIA_VOLUME.set(Math.round(volume * 100.0f) / 100.0);
        save();
    }

    /**
     * Le minerai mis en evidence par la detection, ou une chaine vide.
     *
     * <p>Rend vide quand la config n'est pas encore lue : un eclat qui partirait avant le premier
     * chargement ne met donc rien en avant, comme l'original.
     */
    public static String mineDetectOre() {
        try {
            String id = MINE_DETECT_ORE.get();
            return id == null ? "" : id;
        } catch (Exception e) {
            return "";
        }
    }

    /** Ecrit le minerai mis en evidence et sauvegarde : c'est le clic de l'ecran de filtre. */
    public static void writeMineDetectOre(String id) {
        MINE_DETECT_ORE.set(id == null ? "" : id);
        save();
    }
}
