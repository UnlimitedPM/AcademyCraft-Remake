package cn.academy.energy;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Echanges entre un noeud et les generateurs / recepteurs qui lui sont raccordes.
 *
 * Portage de {@code NodeConn.tick()} de la 1.12.2. Comme
 * {@link WirelessBalancer}, c'est une fonction pure : elle ne connait ni le
 * monde, ni les block entities, ni Forge. Tout se teste donc en JUnit.
 *
 * <h2>Le sens des echanges</h2>
 *
 * D'abord les generateurs <b>poussent</b> vers le noeud, ensuite les recepteurs
 * <b>tirent</b> du noeud. Les deux phases disposent chacune de la bande passante
 * complete du noeud : un noeud peut donc encaisser 150 puis redistribuer 150 dans
 * le meme tick. C'est le comportement de l'original, il n'est pas symetrique par
 * accident.
 *
 * <h2>Qui est servi en premier</h2>
 *
 * Les deux listes sont melangees avant parcours, comme le
 * {@code Collections.shuffle} d'origine. Sans cela, quand la bande passante ne
 * suffit pas a tout le monde, ce seraient toujours les memes machines qui
 * seraient servies. A l'appelant de fournir un {@link Random} : les tests en
 * passent un de graine fixe pour rester reproductibles.
 */
public final class NodeConnection {

    private NodeConnection() {}

    /** Bilan d'un tick, utile aux tests et au debogage. */
    public record Result(double received, double supplied) {}

    /**
     * Un tick d'echange.
     *
     * @param node       noeud dont l'energie est modifiee en place
     * @param generators generateurs raccordes ; la liste est melangee sur place
     * @param receivers  recepteurs raccordes ; la liste est melangee sur place
     * @param random     source de melange, ou {@code null} pour ne pas melanger
     */
    public static Result tick(EnergyNode node,
                              List<? extends EnergyGenerator> generators,
                              List<? extends EnergyReceiver> receivers,
                              Random random) {
        double received = pushFromGenerators(node, generators, random);
        double supplied = pullIntoReceivers(node, receivers, random);
        return new Result(received, supplied);
    }

    /** Les generateurs remplissent le noeud, dans la limite de sa bande passante. */
    private static double pushFromGenerators(EnergyNode node,
                                             List<? extends EnergyGenerator> generators,
                                             Random random) {
        if (generators.isEmpty()) return 0.0d;
        if (random != null) Collections.shuffle(generators, random);

        double transferLeft = node.getBandwidth();
        double received = 0.0d;

        for (EnergyGenerator generator : generators) {
            if (transferLeft <= 0.0d) break;

            double current = node.getEnergy();
            double missing = node.getMaxEnergy() - current;
            double requested = Math.min(transferLeft, Math.min(generator.getBandwidth(), missing));
            if (requested <= 0.0d) continue;

            double provided = generator.provideEnergy(requested);
            // Un generateur qui rend plus que la demande ferait deborder le
            // noeud au-dela de sa capacite : la demande fait foi.
            if (provided > requested) provided = requested;
            if (provided <= 0.0d) continue;

            node.setEnergy(current + provided);
            transferLeft -= provided;
            received += provided;
        }

        return received;
    }

    /** Les recepteurs vident le noeud, dans la limite de sa bande passante. */
    private static double pullIntoReceivers(EnergyNode node,
                                            List<? extends EnergyReceiver> receivers,
                                            Random random) {
        if (receivers.isEmpty()) return 0.0d;
        if (random != null) Collections.shuffle(receivers, random);

        double transferLeft = node.getBandwidth();
        double supplied = 0.0d;

        for (EnergyReceiver receiver : receivers) {
            if (transferLeft <= 0.0d) break;

            double current = node.getEnergy();
            if (current <= 0.0d) break;

            double offered = Math.min(current, Math.min(transferLeft, receiver.getBandwidth()));
            offered = Math.min(receiver.getRequiredEnergy(), offered);
            if (offered <= 0.0d) continue;

            // injectEnergy rend ce qu'elle n'a pas pris : ce qui est entre est la
            // difference. On ne refait pas le calcul de capacite ici, la machine
            // est la seule a savoir ce qu'elle accepte.
            double rejected = receiver.injectEnergy(offered);
            double accepted = offered - rejected;
            if (accepted <= 0.0d) continue;

            node.setEnergy(current - accepted);
            transferLeft -= accepted;
            supplied += accepted;
        }

        return supplied;
    }
}
