package cn.academy.discord;

import java.io.IOException;

/**
 * Le tuyau vers Discord : on y ecrit des trames, on lit ce qui arrive.
 *
 * <p>Une interface, pour deux raisons. Les deux systemes n'ont pas la meme prise — un tube
 * nomme sous Windows, une socket locale ailleurs — et surtout, un faux tuyau se relit en
 * JUnit : la conversation entiere avec Discord se verifie donc sans Discord.
 */
public interface DiscordTransport extends AutoCloseable {

    /** Ecrit une trame entiere, ou leve une exception si Discord n'est plus la. */
    void write(byte[] frame) throws IOException;

    /**
     * Lit ce qui est disponible, sans attendre.
     *
     * <p>Rend le nombre d'octets lus, zero s'il n'y a rien pour l'instant, ou -1 quand la
     * conversation est fermee.
     */
    int read(byte[] buffer) throws IOException;

    @Override
    void close();
}
