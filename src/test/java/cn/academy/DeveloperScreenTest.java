package cn.academy;

import cn.academy.client.gui.cgui.SkillTreeView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L'opacite d'un trait de l'arbre des competences, relue sans Minecraft.
 *
 * <p>L'arbre se lit d'un coup d'oeil a la lumiere de ses traits : ce qui est appris brille, ce qui
 * reste a prendre se devine. C'est une regle de l'original, ecrite la-bas en une ligne —
 * {@code mAlpha * (if (learned) 1.0 else 0.4)} — et le port l'avait perdue. Le joueur a pose les
 * deux versions cote a cote : « la luminosite des elements n'est pas la meme pour les 2 ».
 */
class DeveloperScreenTest {

    /** L'opacite d'un noeud, de zero a un : pleine si appris, sept dixiemes, ou un quart. */
    private static final double LEARNED = 1.0;
    private static final double IN_REACH = 0.7;
    private static final double OUT_OF_REACH = 0.25;

    @Test
    @DisplayName("le trait d'une competence apprise garde toute son opacite")
    void leTraitApprisRestePlein() {
        assertEquals(LEARNED, SkillTreeView.linkDim(LEARNED, true), 1.0e-9);
    }

    @Test
    @DisplayName("le trait d'une competence a prendre est attenue a quatre dixiemes")
    void leTraitNonApprisEstAttenue() {
        assertEquals(0.28, SkillTreeView.linkDim(IN_REACH, false), 1.0e-9,
                "sept dixiemes de noeud, et les quatre dixiemes du trait");
        assertEquals(0.10, SkillTreeView.linkDim(OUT_OF_REACH, false), 1.0e-9,
                "un quart de noeud, et les quatre dixiemes du trait");
        assertEquals(SkillTreeView.LINK_UNLEARNED, SkillTreeView.linkDim(1.0, false), 1.0e-9);
    }

    @Test
    @DisplayName("la Developer ne fait pas respirer ses fonds de page")
    void laDeveloperNeRespirePas() {
        // Les ecrans de machine font osciller l'opacite de leurs couches ui_ entre 172 et 216, comme
        // l'original ; celui du developpement, non — la 1.12.2 y dessine ses blancs a 255, et le port
        // les sortait a 182. Un ecran ne se construit pas sans Minecraft : c'est donc la surcharge
        // elle-meme qu'on verifie ici, et elle est la seule chose a ne pas perdre.
        assertDoesNotThrow(() -> DeveloperScreen.class.getDeclaredMethod("breathes"),
                "DeveloperScreen doit surcharger breathes() pour eteindre le souffle");
    }
}
