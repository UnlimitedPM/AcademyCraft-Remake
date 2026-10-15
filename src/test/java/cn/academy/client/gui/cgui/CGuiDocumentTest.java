package cn.academy.client.gui.cgui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le moteur CGui repris de l'original : son lecteur de document, et sa mise en page.
 *
 * <p>C'est ce qui rend l'ecran de teleportation <b>fidele</b> plutot que ressemblant : il ne
 * redessine plus rien a la main, il lit {@code loctele_new.xml} et pose ce que le fichier dit. Les
 * deux se relisent donc ici, sans lancer un jeu — le placement surtout, qui est purement
 * arithmetique : ancrage, pivot, et une echelle de 0,24 qui se multiplie le long de l'arbre.
 *
 * <p>Les nombres du test viennent du fichier lui-meme, et sont calcules pour un ecran de six cent
 * quarante sur trois cent trente-neuf, la taille d'un vrai ecran en vue trois.
 */
class CGuiDocumentTest {

    private static final double EPS = 1e-6;

    /** Un document court, ecrit a la main : deux widgets, un fond, une transformation. */
    private static final String SAMPLE = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <Root><Widget name="root">
              <Component class="cn.lambdalib2.cgui.component.Transform">
                <width>0.0</width><height>0.0</height><scale>0.24</scale>
                <alignWidth>CENTER</alignWidth><alignHeight>CENTER</alignHeight>
              </Component>
              <Widget name="menu">
                <Component class="cn.lambdalib2.cgui.component.Transform">
                  <width>442.0</width><height>530.0</height><x>20.0</x>
                  <y>-179.1666666666667</y>
                </Component>
                <Component class="cn.lambdalib2.cgui.component.DrawTexture">
                  <texture isNull="true"/>
                  <color><red>53</red><green>53</green><blue>53</blue><alpha>128</alpha></color>
                </Component>
                <Widget name="text">
                  <Component class="cn.lambdalib2.cgui.component.Transform">
                    <width>280.0</width><height>55.0</height>
                  </Component>
                  <Component class="cn.lambdalib2.cgui.component.TextBox">
                    <content>blabla</content>
                    <option>
                      <fontSize>43.0</fontSize><align>LEFT</align>
                      <color><red>164</red><green>212</green><blue>233</blue><alpha>127</alpha></color>
                    </option>
                    <heightAlign>CENTER</heightAlign>
                    <allowEdit>false</allowEdit>
                  </Component>
                </Widget>
              </Widget>
            </Widget></Root>
            """;

    @Test
    @DisplayName("un document se lit : ses widgets, ses transformations et ses couleurs")
    void leDocumentSeLit() throws Exception {
        CWidget root = CGuiDocument.parse(SAMPLE);

        assertEquals("root", root.name());
        assertEquals(0.24, root.transform.scale, EPS, "l'echelle du fichier");
        assertEquals("CENTER", root.transform.alignWidth);

        CWidget menu = root.child("menu");
        assertNotNull(menu, "ses enfants");
        assertEquals(442.0, menu.transform.width, EPS);
        assertEquals(530.0, menu.transform.height, EPS);
        assertEquals(-179.1666666666667, menu.transform.y, EPS);

        // Le fond du fichier : une texture absente, donc une couleur.
        assertNotNull(menu.texture);
        assertNull(menu.texture.texture, "le fichier dit isNull, donc pas de texture");
        assertEquals(53, menu.texture.red);
        assertEquals(128, menu.texture.alpha);

        // Et le texte, avec son option — taille, alignement, couleur.
        CWidget text = menu.child("text");
        assertNotNull(text);
        assertEquals("blabla", text.text.content);
        assertEquals(43.0, text.text.fontSize, EPS);
        assertEquals(164, text.text.red);
        assertEquals(127, text.text.alpha);
        assertEquals("CENTER", text.text.heightAlign);

        // Un composant que le port ne connait pas ne casse rien : il est simplement absent.
        assertNull(text.outline);
        assertNull(menu.tint);
    }

    @Test
    @DisplayName("le fichier du vrai ecran tombe la ou l'original le posait")
    void leFichierTombeOuLOriginalLePosait() {
        // Les nombres du document, mis a l'echelle sur un ecran de 640 sur 339 : la racine divise
        // par 0,24 et s'accroche au CENTRE, donc le panneau part du milieu plus vingt fois 0,24.
        CWidget root = CGuiDocument.read("loctele_new");
        assertEquals(0.24, root.transform.scale, EPS);

        CGuiLayout.place(root, 0, 0, 640, 339, 1.0);

        CWidget menu = root.child("menu");
        assertEquals(320 + 20 * 0.24, menu.absX, EPS, "le panneau part du centre, plus vingt unites");
        assertEquals(339 / 2.0 - 179.1666666666667 * 0.24, menu.absY, EPS);
        assertEquals(442 * 0.24, menu.absW, EPS, "et fait 442 sur 530 unites");
        assertEquals(530 * 0.24, menu.absH, EPS);

        // La liste est posee dix-huit unites sous le panneau, et le modele de ligne trente-huit
        // unites plus bas encore : c'est la disposition exacte du fichier.
        CWidget list = root.child("menu").child("list");
        assertEquals(menu.absY + 18 * 0.24, list.absY, EPS);
        assertEquals(menu.absX, list.absX, EPS);

        CWidget element = list.child("elem_template");
        assertEquals(19.2, element.absH, EPS, "une ligne fait 80 unites, soit 19,2 pixels");
        assertEquals(list.absY, element.absY, EPS, "le modele est pose au debut de la liste");

        // Les deux icones d'une ligne : trente-trois unites de cote, a droite du nom.
        assertEquals(menu.absX + 335 * 0.24, element.child("btn_teleport").absX, EPS);
        assertEquals(menu.absX + 375 * 0.24, element.child("btn_remove").absX, EPS);
        assertEquals(33.333333333333336 * 0.24, element.child("btn_teleport").absW, 1e-4);

        // Et le panneau d'explications : le fichier l'ancre au bord DROIT de la racine, donc
        // son x negatif de vingt-trois unites le pose cinq pixels a gauche du milieu de l'ecran.
        // C'est ce que dit le fichier ; c'est l'ecran qui le deplacera contre la liste au moment
        // de s'en servir, comme l'original le faisait de son cote.
        CWidget info = root.child("info");
        assertEquals(320 - 23.33333333333331 * 0.24, info.absX, 1e-4);
        assertEquals(335.83333333333326 * 0.24, info.absW, 1e-4);
        assertTrue(info.absW > 0 && info.absH > 0, "et il a bien une taille");
    }
}
