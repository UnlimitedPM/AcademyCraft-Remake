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
        // L'original dessinait l'icone a 40 % tant que la recharge courait (`alpha = 0.4f`), et
        // celle de son etat sinon : sans cet estompage, une competence en recharge a l'air prete.
        float wave = KeyHintVisuals.sinWave(0);
        assertEquals(0.4f, KeyHintVisuals.iconAlpha(KeyHintVisuals.State.IDLE, 0.01f, wave), 0.0001f);
        assertEquals(0.4f, KeyHintVisuals.iconAlpha(KeyHintVisuals.State.CHARGE, 1f, wave), 0.0001f);
        assertEquals(KeyHintVisuals.COOLDOWN_ICON_ALPHA,
                KeyHintVisuals.iconAlpha(KeyHintVisuals.State.ACTIVE, 0.5f, wave), 0.0001f);
        // Prete, l'icone prend l'opacite de son etat : 0,7 au repos (constant), et la respiration
        // pour les deux autres.
        assertEquals(0.7f, KeyHintVisuals.iconAlpha(KeyHintVisuals.State.IDLE, 0f, wave), 0.0001f);
        assertEquals(0.7f, KeyHintVisuals.iconAlpha(KeyHintVisuals.State.IDLE, 0f, 1f), 0.0001f);
        assertEquals(0.76f, KeyHintVisuals.iconAlpha(KeyHintVisuals.State.CHARGE, 0f, 0.6f), 0.0001f);
        assertEquals(1.0f, KeyHintVisuals.iconAlpha(KeyHintVisuals.State.ACTIVE, 0f, 1f), 0.0001f);
    }

    @Test
    void lesTroisEtatsOntLaCouleurDeLOriginal() {
        // DelegateState de l'original : IDLE(0.7, transparent), CHARGE(1.0, 0xffffad37),
        // ACTIVE(1.0, 0xff46b3ff), les deux derniers respirant. C'est le halo orange que le
        // joueur appelle « le carre pleinement chargee, utilisable ».
        assertEquals(0x000000, KeyHintVisuals.State.IDLE.glow);
        assertEquals(0xFFAD37, KeyHintVisuals.State.CHARGE.glow);
        assertEquals(0x46B3FF, KeyHintVisuals.State.ACTIVE.glow);
        assertTrue(KeyHintVisuals.State.CHARGE.pulse && KeyHintVisuals.State.ACTIVE.pulse);
        assertTrue(!KeyHintVisuals.State.IDLE.pulse, "l'etat de repos ne respire pas");
    }

    @Test
    void laRespirationResteDansSesBornes() {
        // Formule de l'original sur cent secondes : elle part de 0,8, monte a 1,0 vers les
        // soixante-dix-huit secondes, et ne descend jamais sous 0,6.
        assertEquals(0.8f, KeyHintVisuals.sinWave(0), 0.0001f);
        assertEquals(1.0f, KeyHintVisuals.sinWave(50 * Math.PI / 2), 0.0001f);
        for (double t = 0; t < 100; t += 0.5) {
            float wave = KeyHintVisuals.sinWave(t);
            assertTrue(wave >= 0.6f && wave <= 1.0f, "hors bornes a t=" + t + " : " + wave);
        }
    }

    @Test
    void leHaloNeSEclaireQueSurUnEtatQuiEnAMerite() {
        assertEquals(0f, KeyHintVisuals.glowAlpha(KeyHintVisuals.State.IDLE, 0f, 1f));
        assertEquals(0f, KeyHintVisuals.glowAlpha(KeyHintVisuals.State.CHARGE, 0.5f, 1f),
                "une icone en recharge n'a pas de halo");
        assertEquals(0.8f, KeyHintVisuals.glowAlpha(KeyHintVisuals.State.CHARGE, 0f, 0.8f), 0.0001f);
        assertEquals(0.9f, KeyHintVisuals.glowAlpha(KeyHintVisuals.State.ACTIVE, 0f, 0.9f), 0.0001f);
    }

    @Test
    void leCapuchonGrisSeDecideAvecLaRechargeAussi() {
        // L'original : `!canUseAbility || data.getTickLeft() > 0`. Le port ne regardait que
        // l'aptitude, donc une touche en recharge gardait un capuchon eclatant.
        assertEquals(1.0f, KeyHintVisuals.capTint(true, 0f), 0.0001f);
        assertEquals(0.7f, KeyHintVisuals.capTint(true, 5f), 0.0001f);
        assertEquals(0.7f, KeyHintVisuals.capTint(false, 0f), 0.0001f);
    }

    @Test
    void leFonduDOpaciteGardeLaCouleur() {
        // Le fondu d'apparition touche l'aplat gris d'une recharge comme les textures : seule
        // l'opacite bouge, les trois canaux de couleur ne doivent pas s'en meler.
        assertEquals(0x4C999999, KeyHintVisuals.withAlpha(KeyHintVisuals.COOLDOWN_OVERLAY, 1f));
        assertEquals(0x00999999, KeyHintVisuals.withAlpha(KeyHintVisuals.COOLDOWN_OVERLAY, 0f));
        assertEquals(0x26999999, KeyHintVisuals.withAlpha(KeyHintVisuals.COOLDOWN_OVERLAY, 0.5f));
        assertEquals(0x4C999999, KeyHintVisuals.withAlpha(KeyHintVisuals.COOLDOWN_OVERLAY, 3f),
                "un fondu ne depasse jamais son opacite de depart");
    }

    @Test
    void uneCompetenceOuvertePorteLeHaloBleu() {
        // La regle de l'original : un contexte vivant rend ACTIVE (BLEU), et l'orange est reserve
        // a ce que la competence declare. Le port avait l'inverse — il colorait toute charge en
        // orange — et le joueur l'a vu tout de suite.
        cn.academy.ability.AbilityData data = new cn.academy.ability.AbilityData();
        cn.academy.ability.Skill meltdowner = cn.academy.ability.meltdowner.MeltdownerCategory.MELTDOWNER;

        assertEquals(KeyHintVisuals.State.IDLE, KeyHintVisuals.stateOf(null, data, false));
        assertEquals(KeyHintVisuals.State.IDLE, KeyHintVisuals.stateOf(meltdowner, data, false),
                "aucun maintien ouvert : au repos");
        assertEquals(KeyHintVisuals.State.CHARGE, KeyHintVisuals.stateOf(meltdowner, data, true),
                "l'armement du railgun est une PREPARATION, sans contexte ouvert");

        cn.academy.ability.client.ClientCharge.begin(meltdowner.getName(), 40);
        try {
            assertEquals(KeyHintVisuals.State.ACTIVE,
                    KeyHintVisuals.stateOf(meltdowner, data, false),
                    "une charge en cours est BLEUE, comme tout contexte vivant");
            assertEquals(KeyHintVisuals.State.IDLE,
                    KeyHintVisuals.stateOf(KeyHintVisuals.skillByName("charging"), data, false),
                    "la charge d'une competence ne touche pas les autres");
        } finally {
            cn.academy.ability.client.ClientCharge.end(meltdowner.getName());
        }
    }

    @Test
    void lesCompetencesQuiDeclarentUnePreparationSontOrange() {
        // Les trois {@code getState} de l'original : l'onde de choc sur ses cinq premiers ticks,
        // le canon a plasma avant sa fenetre, les ailes avant de s'ouvrir.
        assertEquals(KeyHintVisuals.State.CHARGE, openAt(cn.academy.ability.vecmanip.VecmanipCategory.GROUNDSHOCK, 3));
        assertEquals(KeyHintVisuals.State.ACTIVE, openAt(cn.academy.ability.vecmanip.VecmanipCategory.GROUNDSHOCK, 5));

        assertEquals(KeyHintVisuals.State.CHARGE, openAt(cn.academy.ability.vecmanip.VecmanipCategory.PLASMA_CANNON, 10));
        assertEquals(KeyHintVisuals.State.ACTIVE, openAt(cn.academy.ability.vecmanip.VecmanipCategory.PLASMA_CANNON, 60));

        assertEquals(KeyHintVisuals.State.CHARGE, openAt(cn.academy.ability.vecmanip.VecmanipCategory.STORM_WING, 70));
        assertEquals(KeyHintVisuals.State.ACTIVE, openAt(cn.academy.ability.vecmanip.VecmanipCategory.STORM_WING, 71));
    }

    /** L'etat d'une competence ouverte depuis {@code ticks} ticks. */
    private static KeyHintVisuals.State openAt(cn.academy.ability.Skill skill, int ticks) {
        cn.academy.ability.client.ClientCharge.begin(skill.getName(), 0);
        try {
            for (int i = 0; i < ticks; i++) {
                cn.academy.ability.client.ClientCharge.tick(skill.getName());
            }
            return KeyHintVisuals.stateOf(skill, new cn.academy.ability.AbilityData(), false);
        } finally {
            cn.academy.ability.client.ClientCharge.end(skill.getName());
        }
    }

    @Test
    void laColonneDesDirectionsReculeDUneColonne() {
        assertEquals(KeyHintVisuals.CONTENT_SHIFT_X, KeyHintVisuals.contentShift(0));
        assertEquals(-278, KeyHintVisuals.contentShift(1));
        assertEquals(KeyHintVisuals.CONTENT_SHIFT_X - 2 * KeyHintVisuals.COLUMN_STEP,
                KeyHintVisuals.contentShift(2));
    }

    @Test
    void leScintillementAUneImageParDirection() {
        // Seul le scintillement dessine une image par direction (les ailes de tempete montrent
        // leur propre icone sur les quatre lignes) : c'est l'original.
        assertEquals(java.util.List.of("a", "d", "w", "s"),
                java.util.Arrays.asList(KeyHintVisuals.DIRECTIONS));
        assertEquals("textures/abilities/teleporter/flashing/a.png",
                KeyHintVisuals.directionIcon("flashing", 0));
        assertEquals("textures/abilities/teleporter/flashing/s.png",
                KeyHintVisuals.directionIcon("flashing", 3));
        assertEquals(null, KeyHintVisuals.directionIcon("flashing", 4));
        assertEquals(null, KeyHintVisuals.directionIcon("storm_wing", 0));
        assertEquals(null, KeyHintVisuals.directionIcon(null, 0));
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
