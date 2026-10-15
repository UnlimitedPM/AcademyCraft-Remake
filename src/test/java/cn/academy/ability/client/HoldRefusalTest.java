package cn.academy.ability.client;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les refus d'ouverture, relus par le client avant de montrer quoi que ce soit.
 *
 * <p>C'est ce qui empeche le debut du bouclier et son son d'apparaitre pour un maintien que le
 * serveur va refuser. Chaque refus du serveur a donc son test ici, et l'inverse aussi : une
 * ouverture legitime ne doit jamais etre bloquee par le client, sans quoi le joueur croirait sa
 * competence cassee.
 */
class HoldRefusalTest {

    private static final Skill SHIELD = MeltdownerCategory.LIGHT_SHIELD;

    /** Un joueur pret : aptitude allumee, rien en cours, et de quoi payer. */
    private static AbilityData ready() {
        AbilityData data = new AbilityData();
        data.setCategoryLevel(MeltdownerCategory.INSTANCE, 3);
        data.learnSkill(SHIELD);
        data.setActivated(true);
        data.setControlPoint(data.getMaxControlPoint());
        return data;
    }

    @Test
    @DisplayName("un joueur pret n'est pas refuse")
    void unJoueurPretPasse() {
        assertFalse(HoldRefusal.refusesStart(SHIELD, ready(), true),
                "rien ne doit empecher une activation legitime");
    }

    @Test
    @DisplayName("une main qui n'a pas ce qu'il faut refuse")
    void laMainQuiNaRienRefuse() {
        // C'est le seul refus que le client lise sur le JOUEUR lui-meme, et non sur ses nombres :
        // le depose au loin veut un bloc, le lancer d'objet veut quelque chose. Une main est une
        // main — le client la voit comme le serveur — donc celui-la ne peut pas etre lu a tort.
        // Sans lui, l'appui les mains vides ouvrait le maintien pour rien, et le joueur en voyait
        // le debut scintiller avant que le serveur ne le referme.
        assertTrue(HoldRefusal.refusesStart(SHIELD, ready(), false),
                "une competence qui demande une main ne s'ouvre pas sans elle");

        // Et quand elle ne demande rien, la main n'a pas voix au chapitre.
        assertFalse(HoldRefusal.refusesStart(SHIELD, ready(), true));
    }

    @Test
    @DisplayName("l'aptitude eteinte refuse")
    void lAptitudeEteinteRefuse() {
        AbilityData data = ready();
        data.setActivated(false);
        assertTrue(HoldRefusal.refusesStart(SHIELD, data, true));
        assertTrue(HoldRefusal.refusesScreen(data), "et l'ecran d'une competence reste ferme");
    }

    @Test
    @DisplayName("l'ecran s'ouvre encore quand il a quelque chose a dire")
    void lEcranSOuvrePourDireCeQuiManque() {
        // Le refus d'ecran ne juge que l'allumage et le brouillage, et c'est voulu : une recharge,
        // une surcharge ou une reserve trop courte sont les refus d'un EFFET. L'ecran, lui, doit
        // pouvoir s'ouvrir pour dire ce qui manque et annoncer les prix — l'original laissait sa
        // liste de marques s'ouvrir avec une reserve vide.
        assertFalse(HoldRefusal.refusesScreen(ready()), "un joueur pret a son ecran");

        AbilityData recharge = ready();
        recharge.setCooldown(SHIELD, 40);
        assertTrue(HoldRefusal.refusesStart(SHIELD, recharge, true));
        assertFalse(HoldRefusal.refusesScreen(recharge), "mais il peut lire ses marques");

        // Et une reserve vide non plus : c'est le cas qui compte le plus, parce que c'est
        // justement celui ou le joueur a besoin de lire les prix et les refus de chaque marque.
        AbilityData pauvre = ready();
        pauvre.setControlPoint(0f);
        assertFalse(HoldRefusal.refusesScreen(pauvre),
                "il doit voir ce que chaque marque coute, meme sans de quoi payer");
    }

    @Test
    @DisplayName("une recharge en cours refuse")
    void laRechargeRefuse() {
        AbilityData data = ready();
        data.setCooldown(SHIELD, 40);
        assertTrue(HoldRefusal.refusesStart(SHIELD, data, true));
    }

    @Test
    @DisplayName("la surcharge qui redescend refuse")
    void laSurchargeRefuse() {
        // Le verrou de l'original vaut pour toute la descente : une competence qui vient de
        // surchauffer ne se relance pas au milieu de sa propre recuperation. Le drapeau se pose
        // en FRANCHISSANT le maximum, comme dans l'original — c'est `addOverload` qui le fait, et
        // `perform` est le chemin public qui y mene.
        AbilityData data = ready();
        assertFalse(data.isOverloadRecovering(), "au depart, la reserve de surcout est saine");

        assertTrue(data.perform(0f, data.getMaxOverload()), "le surcout se paie");

        assertTrue(data.isOverloadRecovering(), "le maximum franchi met le joueur en surcharge");
        assertTrue(HoldRefusal.refusesStart(SHIELD, data, true));
    }

    @Test
    @DisplayName("une reserve trop juste refuse")
    void laReserveTropJusteRefuse() {
        // Le prix d'ouverture, celui que `perform` exige. Il se lit sur une competence qui en a
        // un : le bouclier, lui, n'ouvre rien en CP — son prix est son entretien, et son surcout.
        Skill bolt = cn.academy.ability.electromaster.ElectromasterCategory.THUNDER_BOLT;
        AbilityData data = new AbilityData();
        data.setCategoryLevel(bolt.getCategory(), 5);
        data.learnSkill(bolt);
        data.setActivated(true);

        float cost = bolt.getCpCost(data);
        assertTrue(cost > 0f, "le prix doit etre positif pour que le test dise quelque chose : " + cost);

        data.setControlPoint(cost - 1f);
        assertTrue(HoldRefusal.refusesStart(bolt, data, true),
                "un point de moins que le prix doit suffire a refuser : " + cost);

        // Et au prix exact, l'ouverture passe : c'est la meme frontiere que le serveur, dont le
        // `perform` refuse sur un strictement-plus-petit.
        data.setControlPoint(cost);
        assertFalse(HoldRefusal.refusesStart(bolt, data, true));
    }
}
