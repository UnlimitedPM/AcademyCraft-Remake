package cn.academy.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le rappel des touches : ce que chaque ligne montre.
 *
 * <p>La geometrie vient de {@code KeyHintUI} de l'original et l'image du capuchon depend du
 * genre de touche. C'est cette decision-la qui est figee ici : le dessin lui-meme ne se voit
 * qu'en lancant le client.
 */
class KeyHintVisualsTest {

    @Test
    void lIconeSEstompePendantLaRecharge() {
        // L'original dessinait l'icone a 40 % tant que la recharge courait (`alpha = 0.4f`),
        // et pleine sinon : sans cet estompage, une competence en recharge a l'air prete.
        assertEquals(1.0f, KeyHintVisuals.cooldownIconAlpha(0f), 0.0001f, "prete : pleine opacite");
        assertEquals(0.4f, KeyHintVisuals.cooldownIconAlpha(0.01f), 0.0001f);
        assertEquals(0.4f, KeyHintVisuals.cooldownIconAlpha(1f), 0.0001f);
        assertEquals(KeyHintVisuals.COOLDOWN_ICON_ALPHA,
                KeyHintVisuals.cooldownIconAlpha(0.5f), 0.0001f);
    }

    @Test
    void lesTouchesVidesNeSAffichentPas() {
        // L'original ne listait que les touches garnies, et elles se suivaient sans trou :
        // le port dessinait quatre lignes quoi qu'il arrive (signale par le joueur).
        assertEquals(java.util.List.of(0, 2, 3), KeyHintVisuals.visibleSlots(
                java.util.Arrays.asList("arc_gen", null, "dir_shock", "charging")));
        assertEquals(java.util.List.of(1), KeyHintVisuals.visibleSlots(
                java.util.Arrays.asList(null, "charging", null, null)));
        assertEquals(java.util.List.of(), KeyHintVisuals.visibleSlots(
                java.util.Arrays.asList(null, null, null, null)));
        assertEquals(java.util.List.of(0, 1, 2, 3), KeyHintVisuals.visibleSlots(
                java.util.Arrays.asList("a", "b", "c", "d")));
    }

    @Test
    void lesTouchesDeLaSourisOntLeurCapuchon() {
        assertEquals(KeyHintVisuals.Cap.MOUSE_LEFT, KeyHintVisuals.cap(true, 0));
        assertEquals(KeyHintVisuals.Cap.MOUSE_RIGHT, KeyHintVisuals.cap(true, 1));
        assertEquals(KeyHintVisuals.Cap.MOUSE_OTHER, KeyHintVisuals.cap(true, 2),
                "le bouton du milieu, comme la molette");
        assertEquals(KeyHintVisuals.Cap.KEY, KeyHintVisuals.cap(false, 0),
                "une touche du clavier, meme si son code porte le numero zero");
    }

    @Test
    void leCapuchonDUneLettreSuitSaLongueur() {
        // L'original prenait l'image courte pour un nom de deux caracteres au plus (une lettre,
        // un chiffre, « F1 ») et la longue au-dela (« Page Up »).
        assertEquals("key_short", KeyHintVisuals.capTexture(KeyHintVisuals.Cap.KEY, "R"));
        assertEquals("key_short", KeyHintVisuals.capTexture(KeyHintVisuals.Cap.KEY, "F4"));
        assertEquals("key_long", KeyHintVisuals.capTexture(KeyHintVisuals.Cap.KEY, "Page Up"));
        assertEquals("mouse_left", KeyHintVisuals.capTexture(KeyHintVisuals.Cap.MOUSE_LEFT, ""));
        assertEquals("mouse_generic", KeyHintVisuals.capTexture(KeyHintVisuals.Cap.MOUSE_OTHER, ""));
    }

    @Test
    void uneToucheDeSourisNePorteRIen() {
        // Les deux boutons ont leur image ; c'est le bouton generique qui montre son numero.
        assertEquals("", KeyHintVisuals.capLabel(KeyHintVisuals.Cap.MOUSE_LEFT, "Bouton 1", 0));
        assertEquals("", KeyHintVisuals.capLabel(KeyHintVisuals.Cap.MOUSE_RIGHT, "Bouton 2", 1));
        assertEquals("2", KeyHintVisuals.capLabel(KeyHintVisuals.Cap.MOUSE_OTHER, "", 2));
        assertEquals("R", KeyHintVisuals.capLabel(KeyHintVisuals.Cap.KEY, "R", 0));
    }

    @Test
    void laRechargeAssombritUnePartDeLICone() {
        assertEquals(0f, KeyHintVisuals.cooldownFraction(0, 40), "pas de recharge, rien d'assombri");
        assertEquals(1f, KeyHintVisuals.cooldownFraction(40, 40), "recharge entiere : tout est gris");
        assertEquals(0.5f, KeyHintVisuals.cooldownFraction(20, 40), 0.0001f);
        // Une recharge posee sans duree (ou une duree oubliee) ne doit pas faire disparaitre
        // l'icone sous un aplat gris.
        assertEquals(0f, KeyHintVisuals.cooldownFraction(10, 0));
        assertEquals(0f, KeyHintVisuals.cooldownFraction(-5, 40));
        assertEquals(1f, KeyHintVisuals.cooldownFraction(80, 40), "la part ne depasse jamais un");
    }

    @Test
    void laColonneReculeCommeChezLOriginal() {
        // L'original translatait sa colonne de -200 unites avant de dessiner a partir de x=122 :
        // le port part de 0 et doit donc reculer de -78. Perdre ce chiffre pousse le cadre de
        // 18 pixels vers la droite, et l'icone sort de l'ecran (vecu).
        assertEquals(-78, KeyHintVisuals.CONTENT_SHIFT_X);

        // Ce que cela donne sur un ecran de 854x480, avec le defaut 0/30 de l'element : boite
        // posee a x = 822, y = 246 (soit 6 pixels sous le milieu).
        int left = 854 - 32;
        float shift = KeyHintVisuals.CONTENT_SHIFT_X * KeyHintVisuals.SCALE;

        // Le cadre de l'icone doit finir a une douzaine de pixels du bord, comme chez lui...
        float frameRight = left + shift
                + (KeyHintVisuals.FRAME_X + KeyHintVisuals.FRAME_SIZE) * KeyHintVisuals.SCALE;
        assertEquals(842.24f, frameRight, 0.1f);
        assertTrue(frameRight < 854, "le cadre mord sur le bord droit : " + frameRight);

        // ...et la plaque entiere doit tenir dans l'ecran (c'est elle qui va le plus a droite).
        float plateRight = left + shift + KeyHintVisuals.PLATE_W * KeyHintVisuals.SCALE;
        assertTrue(plateRight < 854, "la plaque depasse du bord droit : " + plateRight);

        // Les quatre lignes partent du haut de la boite, donc de 6 pixels sous le milieu, et
        // descendent : c'est ce que montre l'original.
        int top = 480 / 2 - 48 / 2 + 30;
        assertEquals(246, top);
    }

    @Test
    void lesMesuresSontCellesDeLOriginal() {
        // Quatre lignes qui ne se chevauchent pas, et une icone dans son cadre : c'est ce que
        // ces nombres veulent dire, et une faute s'y verrait tout de suite a l'ecran.
        assertEquals(4, KeyHintVisuals.ROWS);
        assertTrue(KeyHintVisuals.ROW_STEP > KeyHintVisuals.PLATE_H,
                "deux lignes ne doivent pas se recouvrir");
        assertTrue(KeyHintVisuals.ICON_X > KeyHintVisuals.FRAME_X,
                "l'icone se pose dans le cadre, pas dessus");
        assertTrue(KeyHintVisuals.ICON_Y > KeyHintVisuals.FRAME_Y);
        assertTrue(KeyHintVisuals.ICON_X + KeyHintVisuals.ICON_SIZE
                        <= KeyHintVisuals.FRAME_X + KeyHintVisuals.FRAME_SIZE,
                "et elle tient dedans");
        assertTrue(KeyHintVisuals.FRAME_X + KeyHintVisuals.FRAME_SIZE <= KeyHintVisuals.PLATE_W,
                "le cadre tient dans la plaque");
    }

    @Test
    void uneCompetenceInconnueNeTrouveRien() {
        // Le prereglage ne retient qu'un NOM : un nom disparu doit rendre null, et non lever.
        assertEquals(null, KeyHintVisuals.skillByName(null));
        assertEquals(null, KeyHintVisuals.skillByName("competence_qui_n_existe_pas"));
    }
}
