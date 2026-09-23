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

    private static final Map<HudElement, ForgeConfigSpec.ConfigValue<List<? extends Double>>> VALUES =
            new EnumMap<>(HudElement.class);

    /** La config chargee, pour pouvoir la reecrire. Posee par l'evenement de chargement. */
    private static ModConfig loaded;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Ou se posent les elements du HUD. Reglables en jeu par",
                        "Settings > Misc > Customize UI, dans le terminal de donnees.")
                .push("gui");

        for (HudElement element : HudElement.values()) {
            VALUES.put(element, builder
                    .comment("Position de « " + element.getName() + " » : ecart en pixels depuis son ancrage.",
                            "Defaut de l'original : " + element.getDefaultX() + " / " + element.getDefaultY())
                    .define(element.getName(), List.of(element.getDefaultX(), element.getDefaultY())));
        }

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

            Double x = pair.get(0);
            Double y = pair.get(1);
            if (x == null || y == null) continue;
            layout.setRaw(element, x, y);
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
}
