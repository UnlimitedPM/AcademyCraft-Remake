package cn.academy.ability.develop;

import net.minecraft.nbt.CompoundTag;

/**
 * L'etat d'un apprentissage, sans machine et sans joueur.
 *
 * <p>Portage du coeur de {@code DevelopData} : dans quel etat on est, combien de
 * stimulations il faut, et ou l'on en est. Le port avait mis ces champs dans le block
 * entity du developeur ; l'objet portable a besoin exactement des memes, donc ils vivent
 * ici — et comme la classe ne connait ni Minecraft ni le monde, un test unitaire peut la
 * relire tick par tick.
 *
 * <h2>La cadence, et un detail de l'original</h2>
 *
 * Une stimulation dure {@code tps} ticks... plus un. L'original incrementait son compteur
 * puis le comparait avec {@code tps} en strictement superieur, donc la premiere
 * stimulation demandait {@code tps + 1} ticks. Le port garde ce detail plutot que de le
 * corriger en silence : ce serait un changement de vitesse non demande, et il est fige par
 * un test.
 */
public final class DevelopProgress {

    /** Etat d'un apprentissage, repris de {@code DevelopData.DevState}. */
    public enum DevState {
        IDLE,
        DEVELOPING,
        FAILED,
        DONE
    }

    private DevState state = DevState.IDLE;
    private int stimulations;
    private int maxStimulations;
    private int ticksInStimulation;

    /** Ouvre un apprentissage de {@code maxStimulations} stimulations. */
    public void begin(int maxStimulations) {
        state = DevState.DEVELOPING;
        this.maxStimulations = Math.max(0, maxStimulations);
        stimulations = 0;
        ticksInStimulation = 0;
    }

    /** Ferme l'apprentissage, reussi ou non. Rien n'est conserve d'un essai a l'autre. */
    public void finish(boolean success) {
        state = success ? DevState.DONE : DevState.FAILED;
        stimulations = 0;
        maxStimulations = 0;
        ticksInStimulation = 0;
    }

    /** Remet tout a zero, comme si rien ne s'etait jamais passe. */
    public void reset() {
        finish(false);
        state = DevState.IDLE;
    }

    /**
     * Avance d'un tick.
     *
     * @param ticksPerStimulation le {@code tps} de la machine employee
     * @return vrai si une stimulation vient de s'achever, donc s'il faut verifier si
     *         l'apprentissage est termine
     */
    public boolean tick(int ticksPerStimulation) {
        if (!isDeveloping()) return false;
        if (++ticksInStimulation > ticksPerStimulation) {
            ticksInStimulation = 0;
            stimulations++;
            return true;
        }
        return false;
    }

    /** Vrai si toutes les stimulations prevues sont faites. */
    public boolean allStimulationsDone() {
        return stimulations >= maxStimulations;
    }

    public boolean isDeveloping() {
        return state == DevState.DEVELOPING;
    }

    public DevState getState() {
        return state;
    }

    public int getStimulations() {
        return stimulations;
    }

    public int getMaxStimulations() {
        return maxStimulations;
    }

    public int getTicksInStimulation() {
        return ticksInStimulation;
    }

    /**
     * Avancement entre 0 et 1, comme {@code DevelopData.getDevelopProgress} : les
     * stimulations faites, plus la fraction de la stimulation en cours.
     */
    public double progress(int ticksPerStimulation) {
        if (!isDeveloping() || maxStimulations <= 0) return 0.0d;
        double done = (double) stimulations / maxStimulations;
        double current = (double) ticksInStimulation / maxStimulations / ticksPerStimulation;
        return Math.min(1.0d, done + current);
    }

    /** Copie l'etat d'un autre avancement. */
    public void copyFrom(DevelopProgress other) {
        state = other.state;
        stimulations = other.stimulations;
        maxStimulations = other.maxStimulations;
        ticksInStimulation = other.ticksInStimulation;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("state", state.ordinal());
        tag.putInt("stim", stimulations);
        tag.putInt("max_stim", maxStimulations);
        tag.putInt("tick_this_stim", ticksInStimulation);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        state = stateOf(tag.getInt("state"));
        stimulations = tag.getInt("stim");
        maxStimulations = tag.getInt("max_stim");
        ticksInStimulation = tag.getInt("tick_this_stim");
    }

    /** L'etat d'un rang sauvegarde, ou {@code IDLE} s'il ne veut rien dire. */
    public static DevState stateOf(int ordinal) {
        DevState[] values = DevState.values();
        if (ordinal < 0 || ordinal >= values.length) return DevState.IDLE;
        return values[ordinal];
    }
}
