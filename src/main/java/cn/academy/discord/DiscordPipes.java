package cn.academy.discord;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ouvrir le tuyau de Discord.
 *
 * <p>Discord ecoute sur des canaux numerotes, {@code discord-ipc-0} jusqu'a
 * {@code discord-ipc-9} : l'usage est de prendre le premier qui s'ouvre. S'il n'y en a aucun,
 * c'est que le client Discord n'est pas lance — ce n'est pas une erreur, seulement une
 * raison de retenter plus tard.
 *
 * <p>Sous Windows c'est un tube nomme, et Java n'y accede pas tout seul : on passe donc par
 * JNA, que Minecraft livre deja (le mod n'ajoute aucune dependance). Ailleurs c'est une
 * socket locale, que le JDK sait ouvrir depuis sa version 16.
 *
 * <p>Ce fichier est le seul du mod qui parle a un systeme precis, et il n'est charge que par
 * le client : un serveur dedie ne le voit jamais.
 */
public final class DiscordPipes {

    /** Le nombre de canaux que Discord ouvre. */
    public static final int CHANNELS = 10;

    private DiscordPipes() {}

    /** Ouvre le premier canal qui repond, ou leve une exception si Discord n'est pas la. */
    public static DiscordTransport open() throws IOException {
        IOException last = null;
        for (int index = 0; index < CHANNELS; index++) {
            try {
                return open(index);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("aucun canal Discord");
    }

    private static DiscordTransport open(int index) throws IOException {
        return isWindows() ? openPipe(index) : openSocket(index);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    // ------------------------------------------------------------------
    // Windows : le tube nomme, par JNA
    // ------------------------------------------------------------------

    /** Le strict necessaire de kernel32 : ouvrir, lire, ecrire, fermer. */
    private interface Kernel32 extends Library {

        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        Pointer CreateFileA(String name, int access, int share, Pointer attributes,
                            int creation, int flags, Pointer template);

        boolean ReadFile(Pointer handle, byte[] buffer, int size, IntByReference read, Pointer overlapped);

        boolean WriteFile(Pointer handle, byte[] buffer, int size, IntByReference written, Pointer overlapped);

        boolean PeekNamedPipe(Pointer handle, Pointer buffer, int size, IntByReference read,
                              IntByReference available, IntByReference left);

        boolean CloseHandle(Pointer handle);
    }

    /** GENERIC_READ | GENERIC_WRITE, en trente-deux bits signes. */
    private static final int ACCESS = (int) 0x80000000L | 0x40000000;

    /** OPEN_EXISTING : on se branche sur un tube qui existe, on n'en cree pas. */
    private static final int OPEN_EXISTING = 3;

    private static DiscordTransport openPipe(int index) throws IOException {
        String name = "\\\\.\\pipe\\discord-ipc-" + index;
        Pointer handle = Kernel32.INSTANCE.CreateFileA(name, ACCESS, 0, null, OPEN_EXISTING, 0, null);
        if (handle == null || Pointer.nativeValue(handle) == -1L) {
            throw new IOException("tube " + name + " introuvable");
        }
        return new WindowsPipe(handle);
    }

    private static final class WindowsPipe implements DiscordTransport {

        private final Pointer handle;

        WindowsPipe(Pointer handle) {
            this.handle = handle;
        }

        @Override
        public void write(byte[] frame) throws IOException {
            IntByReference written = new IntByReference();
            if (!Kernel32.INSTANCE.WriteFile(handle, frame, frame.length, written, null)
                    || written.getValue() != frame.length) {
                throw new IOException("Discord a refuse l'ecriture");
            }
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            // PeekNamedPipe est ce qui permet de lire sans attendre : sans lui, la lecture
            // d'un tube se bloque jusqu'a ce que Discord parle, et le fil resterait coince
            // au lieu de poser les fiches suivantes.
            IntByReference available = new IntByReference();
            if (!Kernel32.INSTANCE.PeekNamedPipe(handle, null, 0, null, available, null)) {
                return -1;
            }
            int ready = Math.min(available.getValue(), buffer.length);
            if (ready <= 0) return 0;

            IntByReference got = new IntByReference();
            if (!Kernel32.INSTANCE.ReadFile(handle, buffer, ready, got, null)) return -1;
            return got.getValue();
        }

        @Override
        public void close() {
            Kernel32.INSTANCE.CloseHandle(handle);
        }
    }

    // ------------------------------------------------------------------
    // Linux et macOS : la socket locale
    // ------------------------------------------------------------------

    private static DiscordTransport openSocket(int index) throws IOException {
        IOException last = null;
        for (Path directory : directories()) {
            try {
                SocketChannel channel = SocketChannel.open(
                        UnixDomainSocketAddress.of(directory.resolve("discord-ipc-" + index)));
                channel.configureBlocking(false);
                return new UnixSocket(channel);
            } catch (Exception e) {
                last = new IOException(e);
            }
        }
        throw last != null ? last : new IOException("socket discord-ipc-" + index + " introuvable");
    }

    /**
     * Ou Discord pose ses sockets.
     *
     * <p>L'ordre suit celui des implementations du protocole : le dossier de session du
     * bureau d'abord, puis le dossier temporaire. Le dossier du systeme est la pour les
     * installations qui n'ont ni l'un ni l'autre.
     */
    private static List<Path> directories() {
        List<Path> directories = new ArrayList<>(3);
        String runtime = System.getenv("XDG_RUNTIME_DIR");
        if (runtime != null && !runtime.isBlank()) directories.add(Path.of(runtime));

        String temporary = System.getenv("TMPDIR");
        if (temporary != null && !temporary.isBlank()) directories.add(Path.of(temporary));

        directories.add(Path.of("/tmp"));
        return directories;
    }

    private static final class UnixSocket implements DiscordTransport {

        /** Le nombre de tours d'ecriture avant d'abandonner, si le tampon du systeme est plein. */
        private static final int WRITE_TRIES = 1000;

        private final SocketChannel channel;

        UnixSocket(SocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void write(byte[] frame) throws IOException {
            ByteBuffer buffer = ByteBuffer.wrap(frame);
            for (int tries = 0; buffer.hasRemaining(); tries++) {
                if (channel.write(buffer) == 0 && tries > WRITE_TRIES) {
                    throw new IOException("Discord n'absorbe plus rien");
                }
            }
        }

        @Override
        public int read(byte[] data) throws IOException {
            return channel.read(ByteBuffer.wrap(data));
        }

        @Override
        public void close() {
            try {
                channel.close();
            } catch (IOException ignored) {
                // Fermer ce qui est deja ferme n'est pas une erreur qui merite un message.
            }
        }
    }
}
