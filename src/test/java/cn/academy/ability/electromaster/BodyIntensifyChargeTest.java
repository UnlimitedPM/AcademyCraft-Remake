package cn.academy.ability.electromaster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les nombres du renfort : ce que la charge coute, ce qu'elle dure, et ce qu'elle donne.
 *
 * <p>Trois choses qu'aucune porte ne voit : le cout d'un tick, la duree des effets — tiree du temps
 * tenu, comme chez l'original — et la table des paliers, qui a remplace son tirage au sort.
 */
class BodyIntensifyChargeTest {

    /** Une ligne de la table des paliers, sans les types du jeu : voir {@code Boost}. */
    private static BodyIntensifySkill.Boost force(int level) {
        return new BodyIntensifySkill.Boost(BodyIntensifySkill.Kind.FORCE, level);
    }

    private static BodyIntensifySkill.Boost speed(int level) {
        return new BodyIntensifySkill.Boost(BodyIntensifySkill.Kind.SPEED, level);
    }

    private static BodyIntensifySkill.Boost regeneration(int level) {
        return new BodyIntensifySkill.Boost(BodyIntensifySkill.Kind.REGENERATION, level);
    }

    private static BodyIntensifySkill.Boost famine(int level) {
        return new BodyIntensifySkill.Boost(BodyIntensifySkill.Kind.FAMINE, level);
    }

    @Test
    @DisplayName("les paliers du renfort, bornes comprises, tels que le joueur les a donnes")
    void lesPaliersDuRenfort() {
        // 0 a 24 % : la force, et la famine qui la paie.
        assertEquals(List.of(force(1), famine(1)), BodyIntensifySkill.boostsFor(0f), "au depart");
        assertEquals(List.of(force(1), famine(1)), BodyIntensifySkill.boostsFor(0.24f),
                "juste avant le premier palier");

        // 25 a 49 % : la vitesse s'ajoute.
        assertEquals(List.of(force(1), speed(1), famine(1)),
                BodyIntensifySkill.boostsFor(0.25f), "le premier palier");
        assertEquals(List.of(force(1), speed(1), famine(1)),
                BodyIntensifySkill.boostsFor(0.49f), "juste avant le deuxieme");

        // 50 a 74 % : la regeneration s'ajoute.
        assertEquals(List.of(force(1), speed(1), regeneration(1), famine(1)),
                BodyIntensifySkill.boostsFor(0.5f), "le deuxieme palier");
        assertEquals(List.of(force(1), speed(1), regeneration(1), famine(1)),
                BodyIntensifySkill.boostsFor(0.74f), "juste avant le troisieme");

        // 75 a 99 % : la famine s'en va.
        assertEquals(List.of(force(1), speed(1), regeneration(1)),
                BodyIntensifySkill.boostsFor(0.75f), "le troisieme palier");
        assertEquals(List.of(force(1), speed(1), regeneration(1)),
                BodyIntensifySkill.boostsFor(0.99f), "juste avant la maitrise");

        // 100 % : tout d'un cran, et la famine toujours absente.
        assertEquals(List.of(force(1), speed(2), regeneration(2)),
                BodyIntensifySkill.boostsFor(1f), "a pleine experience");
    }

    @Test
    @DisplayName("la table ne repete aucune famille, et ne pose jamais plus de quatre effets")
    void laTableNeSeRepetePas() {
        for (float exp = 0f; exp <= 1f; exp += 0.05f) {
            List<BodyIntensifySkill.Boost> boosts = BodyIntensifySkill.boostsFor(exp);
            List<BodyIntensifySkill.Kind> kinds = new ArrayList<>();
            for (BodyIntensifySkill.Boost boost : boosts) {
                assertTrue(kinds.add(boost.kind()), "famille repetee a " + exp + " : " + boost);
            }
            // Quatre au plus : c'est le palier du milieu, le seul qui porte la famine avec les trois
            // autres — celui qui « laisse de quoi manger ».
            assertTrue(boosts.size() <= 4,
                    "le renfort ne pose jamais plus de quatre effets : " + boosts);
        }
    }

    @Test
    @DisplayName("le niveau et l'entretien tombent de 20 a 15 CP")
    void lEntretienDeLaCharge() {
        assertEquals(20f, BodyIntensifySkill.cpPerTick(0.0), 1e-4);
        assertEquals(15f, BodyIntensifySkill.cpPerTick(1.0), 1e-4);
    }

    @Test
    @DisplayName("la duree d'un effet suit le temps tenu et le facteur")
    void laDureeSuitLeTempsTenu() {
        assertEquals(60, BodyIntensifySkill.buffTime(40, 1.0, 1.5f), "1,5 fois quarante");
        assertEquals(200, BodyIntensifySkill.buffTime(40, 2.0, 2.5f),
                "2,5 fois quarante, au plus");
        assertEquals(1.5f, BodyIntensifySkill.timeFactor(0.0), 1e-4);
        assertEquals(2.5f, BodyIntensifySkill.timeFactor(1.0), 1e-4);
    }

    @Test
    @DisplayName("la charge dure de 10 a 40 ticks, et ne coute rien a l'ouverture")
    void lesBornesDeLaCharge() {
        BodyIntensifySkill skill = new BodyIntensifySkill();

        assertEquals(BodyIntensifySkill.MIN_TIME, skill.getMinChargeTicks(null));
        assertEquals(BodyIntensifySkill.MAX_TIME, skill.getMaxChargeTicks(null));
        assertEquals(0f, skill.getCpCost(), 1e-4,
                "la charge se paie tick par tick, pas d'un coup a l'ouverture");
    }
}
