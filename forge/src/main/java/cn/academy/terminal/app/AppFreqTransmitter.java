package cn.academy.terminal.app;

import cn.academy.terminal.App;

/**
 * L'application « Emetteur de frequence » : le raccordement du materiel sans fil, a la main.
 *
 * <p>Portage de {@code AppFreqTransmitter}. Elle n'a pas de page : son ouverture ferme le terminal et
 * arme un mode de jeu (voir {@code FreqTransmitterClient}) ou chaque clic droit sur un bloc fait
 * avancer le raccordement — Matrix puis noeud, ou noeud puis machine.
 */
public final class AppFreqTransmitter extends App {

    public static final AppFreqTransmitter INSTANCE = new AppFreqTransmitter();

    private AppFreqTransmitter() {
        super("freq_transmitter");
    }

    @Override
    public int getIconSize() {
        return 128;
    }
}
