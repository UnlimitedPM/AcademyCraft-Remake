package cn.academy.client.hud;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * La place des elements du HUD, sauvegardee dans la config du mod.
 *
 * <p>C'est ce que faisait l'original : une categorie {@code gui}, une cle par element, et
 * le panneau <i>Customize UI</i> qui reecrit la valeur des qu'on valide un champ. La place
 * du HUD appartient donc au <b>client</b> qui joue, pas au serveur — d'ou une config de
 * type {@code CLIENT}.
 *
 * <p>Aucun type de client n'est nomme ici : la classe est chargee des deux cotes (la config
 * s'enregistre a la construction du mod), et seule la lecture de ses valeurs est utile a
 * l'ecran de reglage.
 */
public final class HudConfig {

    public static final ForgeConfigSpec SPEC;

    /**
     * Le volume du lecteur media, de zero a un.
     *
     * <p>Il vit ici parce que c'est la config du CLIENT, et que le volume appartient au joueur qui
     * ecoute, pas au serveur : le joueur : « quand je me deco/reco le jeu garde en memoire
     * l'intensite du volume que j'ai choisit, parce que devoir le rechanger a chaque fois c'est
     * chiant ». Une preference de client va dans la config du client.
     */
    private static final ForgeConfigSpec.DoubleValue MEDIA_VOLUME;

    private static final Map<HudElement, ForgeConfigSpec.ConfigValue<List<? extends Double>>> VALUES =
            new EnumMap<>(HudElement.class);

    /** Le minerai mis en evidence par la detection de minerais : voir {@link #mineDetectOre}. */
    private static ForgeConfigSpec.ConfigValue<String> MINE_DETECT_ORE;

    /** La config chargee, pour pouvoir la reecrire. Posee par l'evenement de chargement. */
    private static ModConfig loaded;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Ou se posent les elements du HUD. Reglables en jeu par",
                        "Settings > Misc > Customize UI, dans le terminal de donnees.")
                .push("gui");

        for (HudElement element : HudElement.values()) {
            // defineList, et non define : un define dont la valeur par defaut est une liste
            // se fait corriger par Forge a CHAQUE chargement (et a chaque rechargement a
            // chaud), parce que le type attendu est celui de la liste passee en defaut —
            // une liste immuable de java.util — et non une liste tout court. La valeur
            // ecrite etait donc lue comme "incorrecte" et remplacee par le defaut : le
            // joueur deplacait son temoin de CP, Forge le remettait en place au chargement
            // suivant. C'est le journal qui l'a dit :
            //   Incorrect key gui.cpbar was corrected from [-12.0, 12.0] to its default
            VALUES.put(element, builder
                    .comment("Position de « " + element.getName() + " » : ecart en pixels depuis son ancrage.",
                            "Defaut de l'original : " + element.getDefaultX() + " / " + element.getDefaultY())
                    .defineList(element.getName(),
                            List.of(element.getDefaultX(), element.getDefaultY()),
                            o -> o instanceof Double));
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

    /** Retient la config chargee, pour pouvoir la sauvegarder plus tard. */
    public static void capture(ModConfig config) {
        loaded = config;
    }

    /** La place de chaque element, telle qu'elle est ecrite dans la config. */
    public static HudLayout read() {
        HudLayout layout = new HudLayout();
        for (HudElement element : HudElement.values()) {
            ForgeConfigSpec.ConfigValue<List<? extends Double>> value = VALUES.get(element);
            if (value == null) continue;

            List<? extends Double> pair;
            try {
                pair = value.get();
            } catch (Exception e) {
                // Une config illisible ne doit pas empecher le HUD de s'afficher : on garde
                // les defauts de l'original.
                continue;
            }
            if (pair == null || pair.size() < 2) continue;

            // Par Number, et non par Double : une valeur ecrite a la main dans le fichier
            // ("-12" sans decimale) arrive en entier, et la lire en Double leverait une
            // exception au milieu du rendu du HUD.
            Object first = pair.get(0);
            Object second = pair.get(1);
            if (!(first instanceof Number x) || !(second instanceof Number y)) continue;
            layout.setRaw(element, x.doubleValue(), y.doubleValue());
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

        ForgeConfigSpec.ConfigValue<List<? extends Double>> value = VALUES.get(element);
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
        if (loaded == null) return;
        try {
            loaded.save();
        } catch (Exception e) {
            // Ecrire la config peut echouer (disque plein, fichier verrouille) : le jeu ne
            // doit pas tomber pour autant, la place reste juste en memoire.
        }
    }

    /**
     * Le volume du lecteur media tel qu'il a ete quitte.
     *
     * <p>Rend un quand la config n'est pas encore lue (au demarrage) : c'est le volume de l'original.
     */
    public static float mediaVolume() {
        try {
            return (float) (double) MEDIA_VOLUME.get();
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
