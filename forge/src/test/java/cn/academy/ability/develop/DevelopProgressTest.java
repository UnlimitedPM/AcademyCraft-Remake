package cn.academy.ability.develop;

import cn.academy.ability.develop.DevelopProgress.DevState;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'avancement d'un apprentissage : la cadence, les etats, et la sauvegarde.
 *
 * <p>Ces regles etaient dans le block entity du developeur, ou seul un GameTest pouvait les
 * voir. Elles vivent maintenant dans une classe sans Minecraft, partagee par la machine et
 * par l'objet portable — donc un test pur peut les relire tick par tick, et une faute de
 * cadence ne se paie plus d'un lancement de jeu.
 */
class DevelopProgressTest {

    /** Le tps d'une machine normale, pour les calculs de cadence. */
    private static final int TPS = 20;

    @Test
    void unApprentissageNeufEstOuvertEtVide() {
        DevelopProgress progress = new DevelopProgress();
        progress.begin(5);

        assertEquals(DevState.DEVELOPING, progress.getState());
        assertTrue(progress.isDeveloping());
        assertEquals(5, progress.getMaxStimulations());
        assertEquals(0, progress.getStimulations());
        assertFalse(progress.allStimulationsDone());
    }

    @Test
    void uneStimulationDureTpsPlusUnTicks() {
        DevelopProgress progress = new DevelopProgress();
        progress.begin(2);

        // L'original incrementait son compteur PUIS le comparait en strictement
        // superieur : la premiere stimulation demandait donc tps + 1 ticks. Le port
        // garde ce detail plutot que de le corriger en silence.
        for (int i = 0; i < TPS; i++) {
            assertFalse(progress.tick(TPS), "la stimulation ne peut pas finir au tick " + (i + 1));
        }
        assertTrue(progress.tick(TPS), "la stimulation s'acheve au tick " + (TPS + 1));
        assertEquals(1, progress.getStimulations());
    }

    @Test
    void lavancementCompteLaStimulationEnCours() {
        DevelopProgress progress = new DevelopProgress();
        progress.begin(2);

        assertEquals(0.0d, progress.progress(TPS), 0.0001d, "rien n'est fait");

        // Une stimulation sur deux : la moitie.
        for (int i = 0; i < TPS + 1; i++) progress.tick(TPS);
        assertEquals(0.5d, progress.progress(TPS), 0.0001d, "une stimulation sur deux");

        // Et la moitie de la suivante compte aussi : 0,5 + 0,5 / 2 = 0,75. L'original
        // lisait la fraction du tick en cours dans le total des stimulations.
        for (int i = 0; i < (TPS + 1) / 2; i++) progress.tick(TPS);
        assertEquals(0.75d, progress.progress(TPS), 0.02d, "avec sa stimulation en cours");

        for (int i = 0; i < TPS + 1; i++) progress.tick(TPS);
        assertTrue(progress.allStimulationsDone(), "les deux stimulations sont faites");
        assertEquals(1.0d, progress.progress(TPS), 0.0001d, "l'apprentissage est complet");
    }

    @Test
    void unApprentissageFiniNeComptePlus() {
        DevelopProgress progress = new DevelopProgress();
        progress.begin(2);
        for (int i = 0; i < 2 * (TPS + 1); i++) progress.tick(TPS);
        progress.finish(true);

        assertEquals(DevState.DONE, progress.getState());
        assertEquals(0, progress.getStimulations());
        assertEquals(0, progress.getMaxStimulations());
        assertEquals(0.0d, progress.progress(TPS), 0.0001d, "un apprentissage fini n'a plus d'avancement");

        // Et il n'avance plus : c'est la machine qui decide quand un apprentissage est
        // en cours, pas le compteur.
        assertFalse(progress.tick(TPS));
        assertEquals(0, progress.getStimulations());
    }

    @Test
    void unEchecEtUnRetourAuReposSontDeuxChosesDifferentes() {
        DevelopProgress failed = new DevelopProgress();
        failed.begin(3);
        failed.finish(false);
        assertEquals(DevState.FAILED, failed.getState(), "un echec se dit : l'ecran l'affiche");

        DevelopProgress reset = new DevelopProgress();
        reset.begin(3);
        reset.reset();
        assertEquals(DevState.IDLE, reset.getState(), "un retour au repos efface jusqu'a l'echec");
    }

    @Test
    void letatFaitUnAllerRetourParLaSauvegarde() {
        DevelopProgress progress = new DevelopProgress();
        progress.begin(4);
        for (int i = 0; i < TPS + 1; i++) progress.tick(TPS);
        for (int i = 0; i < 3; i++) progress.tick(TPS);

        CompoundTag tag = progress.serializeNBT();

        DevelopProgress reread = new DevelopProgress();
        reread.deserializeNBT(tag);

        assertEquals(progress.getState(), reread.getState());
        assertEquals(progress.getStimulations(), reread.getStimulations());
        assertEquals(progress.getMaxStimulations(), reread.getMaxStimulations());
        assertEquals(progress.getTicksInStimulation(), reread.getTicksInStimulation());
        assertEquals(progress.progress(TPS), reread.progress(TPS), 0.0001d);
    }

    @Test
    void unEtatIllisibleRendLeRepos() {
        // Un rang qui ne veut rien dire — une sauvegarde d'une version ou il y avait
        // moins d'etats — ne doit pas laisser la machine en marche.
        assertEquals(DevState.IDLE, DevelopProgress.stateOf(-1));
        assertEquals(DevState.IDLE, DevelopProgress.stateOf(42));
        assertEquals(DevState.DEVELOPING, DevelopProgress.stateOf(DevState.DEVELOPING.ordinal()));
    }

    @Test
    void laCopieEmporteToutLetat() {
        DevelopProgress source = new DevelopProgress();
        source.begin(3);
        for (int i = 0; i < TPS + 1; i++) source.tick(TPS);

        DevelopProgress copy = new DevelopProgress();
        copy.copyFrom(source);

        assertEquals(DevState.DEVELOPING, copy.getState());
        assertEquals(1, copy.getStimulations());
        assertEquals(3, copy.getMaxStimulations());
        assertEquals(source.progress(TPS), copy.progress(TPS), 0.0001d);
    }
}
