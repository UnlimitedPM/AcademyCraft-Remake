package cn.academy.misc.media.client;

import cn.academy.AcademyCraft;
import cn.academy.misc.media.Media;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Le lecteur du client : un morceau a la fois, avec sa pause et son volume.
 *
 * <p>Portage de {@code MediaBackend}, et <b>par le meme chemin que lui</b>. L'original ne
 * passait pas par l'interface de sons du jeu : il prenait son {@code SoundSystem} (la
 * bibliotheque paulscode) par reflexion, ouvrait sa propre source en flux, et commandait
 * cette source — {@code setVolume} pour le volume, {@code pause} / {@code play} pour la pause
 * et la reprise. La source OpenAL garde sa position quand on la met en pause, donc la reprise
 * repart exactement ou l'on s'etait arrete ; le temps affiche venait de
 * {@code millisecondsPlayed}.
 *
 * <h2>La meme chose en 1.20.1</h2>
 *
 * <p>La 1.13 a remplace paulscode par sa propre bibliotheque, {@code com.mojang.blaze3d.audio}.
 * Mais le principe tient toujours : le moteur garde un <b>canal</b> par son, et ce canal sait
 * se mettre en pause, repartir et changer de volume ({@code Channel.pause}, {@code unpause},
 * {@code setVolume}). Le lecteur va donc le chercher — {@code SoundEngine.instanceToChannel},
 * par reflexion, exactement comme l'original allait chercher son {@code sndSystem} — et lui
 * parle directement.
 *
 * <p>C'est ce qui rend les deux comportements corrects : une pause qui <b>reprend a la bonne
 * position</b> sans rien rejouer, et une glissiere de volume qui agit <b>tout de suite</b> (un
 * son ordinaire n'est regle qu'une fois, au lancement, par l'interface publique du moteur).
 *
 * <p>Le morceau se joue sans position ni attenuation : il s'entend partout, ce qui est le
 * propre d'une musique. Comme l'original, il <b>coupe la musique du jeu</b> tant qu'il joue.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMediaPlayer {

    /** Le champ du moteur de son, et celui de ses canaux : les deux prises de l'original. */
    private static final Field SOUND_ENGINE = field(SoundManager.class, "soundEngine");
    private static final Field CHANNELS = field(SoundEngine.class, "instanceToChannel");

    private static MediaSound current;
    private static Media playing;

    /** Le canal du moteur pour ce morceau : c'est lui qui porte la pause et le volume. */
    @Nullable
    private static ChannelAccess.ChannelHandle channel;

    /** Le volume du lecteur, de zero a un : celui de sa glissiere, pas celui du jeu. */
    private static float volume = cn.academy.client.hud.HudConfig.mediaVolume();

    /** L'instant du lancement, en nanosecondes : c'est l'horloge de la position affichee. */
    private static long startedNano;

    /** La position de la pause, ou -1 quand le morceau tourne. */
    private static float pausedAt = -1f;

    /**
     * La position du morceau au moment d'une deconnexion, ou -1 quand il n'y en a pas eu.
     *
     * <p>Partir du monde arrete tous les sons du jeu ({@code Minecraft.clearLevel} appelle
     * {@code soundManager.stop()}), et le canal du morceau meurt avec eux. Le lecteur retient donc
     * ou il en etait, et le morceau repart de la au retour — c'est ce que le joueur attendait :
     * « si on ecoute de la musique et qu'on se deconnecte pendant qu'on l'ecoutait, quand on se
     * reconnecte, la musique ne reprend pas de la ou on l'avait laissee ».
     */
    private static float suspendedAt = -1f;

    /** Vrai quand le morceau TOURNAIT au moment de la deconnexion, faux s'il etait en attente. */
    private static boolean suspendedPlaying;

    /** Vrai quand le jeu est en pause : le moteur a donc mis tous ses sons en attente. */
    private static boolean gamePaused;

    /** Vrai quand c'est le JEU qui a mis le morceau en attente, et non le joueur. */
    private static boolean pausedByGame;

    private ClientMediaPlayer() {}

    /**
     * Le son du lecteur.
     *
     * <p>Rien de particulier : c'est un son comme les autres, sur la source MASTER (pour que
     * la glissiere « Musique » du jeu ne le coupe pas). C'est son <b>canal</b> qui est
     * commande a la main.
     *
     * <p>Il sait aussi reprendre ailleurs qu'au debut : voir {@link #skip}.
     */
    private static final class MediaSound extends AbstractSoundInstance {

        /** Les secondes a passer avant de vraiment commencer a jouer : zero au lancement. */
        private final float skip;

        MediaSound(ResourceLocation location, float skip) {
            super(location, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
            this.skip = skip;
        }

        @Override
        public float getVolume() {
            return volume;
        }

        /**
         * Le flux du morceau, avance de {@link #skip} secondes.
         *
         * <p>Le moteur ne sait pas se deplacer dans un flux : il ouvre le fichier et lit. Pour
         * reprendre au milieu, on lui donne donc un flux qui <b>lit et jette</b> ce qu'il faut
         * avant de rendre la main. C'est ce qui rend une reprise possible apres une deconnexion —
         * la seule autre solution etait de tout rejouer depuis le debut.
         */
        @Override
        public java.util.concurrent.CompletableFuture<AudioStream> getStream(
                SoundBufferLibrary library, Sound sound, boolean looping) {
            return super.getStream(library, sound, looping)
                    .thenApply(stream -> skip <= 0.05f
                            ? stream : new SkippingStream(stream, skip));
        }
    }

    /**
     * Un flux qui commence ailleurs qu'a zero.
     *
     * <p>Il decode et jette la tete du morceau a la premiere lecture — le moteur n'attache un flux
     * qu'une fois, et compte sur sa premiere lecture pour remplir son tampon : rendre {@code null}
     * laisserait la source sans rien a jouer, et elle ne s'en remettrait pas.
     */
    private static final class SkippingStream implements AudioStream {

        private final AudioStream inner;
        private long remainingBytes;

        SkippingStream(AudioStream inner, float seconds) {
            this.inner = inner;
            AudioFormat format = inner.getFormat();
            long bytesPerFrame = (long) format.getChannels()
                    * Math.max(1, format.getSampleSizeInBits() / 8);
            long perSecond = (long) format.getSampleRate() * bytesPerFrame;

            // Le saut tombe sur une FRAME entiere : couper au milieu d'un echantillon stереo fait
            // refuser le tampon par OpenAL (« Assigning buffer data: Invalid parameter »), et le
            // morceau repart alors sans un son.
            this.remainingBytes = Math.round(perSecond * seconds) / bytesPerFrame * bytesPerFrame;
        }

        @Override
        public AudioFormat getFormat() {
            return inner.getFormat();
        }

        @Override
        public java.nio.ByteBuffer read(int size) throws java.io.IOException {
            while (remainingBytes > 0) {
                java.nio.ByteBuffer skipped = inner.read(size);
                if (skipped == null) return null;

                int available = skipped.remaining();
                if (available <= remainingBytes) {
                    remainingBytes -= available;
                    continue;
                }
                skipped.position(skipped.position() + (int) remainingBytes);
                remainingBytes = 0;
                return skipped;
            }
            return inner.read(size);
        }

        @Override
        public void close() throws java.io.IOException {
            inner.close();
        }
    }

    /** Lance un morceau, en arretant celui qui tournait. */
    public static void play(Media media) {
        if (media == null) return;
        stop();
        playing = media;
        pausedAt = -1.0f;
        begin(0.0f);
    }

    /**
     * Le debut d'un morceau, ou sa reprise en cours de route.
     *
     * @param from la seconde ou commencer, zero pour le debut
     */
    private static void begin(float from) {
        // L'horloge repart de la position demandee : l'affichage et le son disent la meme chose.
        startedNano = System.nanoTime() - (long) (from * 1_000_000_000.0f);
        channel = null;

        current = new MediaSound(playing.sound(), from);

        // La musique du jeu se tait pendant le morceau : c'est ce que faisait l'original a
        // chaque passage de son planificateur.
        Minecraft.getInstance().getMusicManager().stopPlaying();
        Minecraft.getInstance().getSoundManager().play(current);

        // Le canal vient d'etre cree : on y pose le volume du lecteur, ou la pause qui l'habite.
        ChannelAccess.ChannelHandle handle = channel();
        if (handle == null) return;
        if (pausedAt >= 0.0f) {
            hold(handle);
        } else {
            handle.execute(ch -> ch.setVolume(volume));
        }
    }

    /**
     * Le joueur quitte le monde : l'horloge s'arrete et la position est retenue.
     *
     * <p>Sans cela le conteur continuait de tourner pendant tout le temps passe au menu — le
     * joueur l'avait remarque : « j'ai l'impression que quand on se deconnecte, le conteur tourne
     * encore en arriere plan » — et le morceau reprenait, au retour, a une position qui n'existait
     * nulle part.
     */
    public static void suspend() {
        if (playing == null) {
            suspendedAt = -1f;
            return;
        }

        suspendedPlaying = pausedAt < 0.0f;
        suspendedAt = position();
        if (suspendedPlaying) pausedAt = suspendedAt;
    }

    /**
     * Le joueur revient : le morceau reprend ou il s'etait arrete.
     *
     * <p>Le canal precedent est mort avec le monde, d'ou un morceau rejoue — mais rejoue
     * <b>depuis la position retenue</b>, pas depuis le debut. Un morceau qui etait en attente
     * revient en attente, muet et immobile, et sa touche le reprend ou il en etait.
     */
    public static void resumeAfterLogin() {
        if (playing == null || suspendedAt < 0.0f) return;

        float at = suspendedAt;
        suspendedAt = -1f;
        pausedAt = suspendedPlaying ? -1.0f : at;
        begin(at);
    }

    /**
     * Le canal du moteur pour le morceau en cours, ou {@code null}.
     *
     * <p>Le moteur tient {@code SoundEngine.instanceToChannel}, une table de son vers son
     * canal ; la cle est le son lui-meme. Tout est prive, d'ou la reflexion — c'est le meme
     * geste que l'original, qui allait chercher le {@code SoundSystem} du jeu.
     */
    @Nullable
    private static ChannelAccess.ChannelHandle channel() {
        if (channel != null) return channel;
        if (current == null || SOUND_ENGINE == null || CHANNELS == null) return null;

        try {
            Object engine = SOUND_ENGINE.get(Minecraft.getInstance().getSoundManager());
            Object table = engine == null ? null : CHANNELS.get(engine);
            if (table instanceof Map<?, ?> channels) {
                Object found = channels.get(current);
                if (found instanceof ChannelAccess.ChannelHandle handle) {
                    channel = handle;
                    // Le moteur a pose le volume du son multiplie par celui de la source ; le gain
                    // de l'ecouteur porte deja ce dernier, donc on repose le notre — ou zero, si le
                    // morceau est en attente.
                    if (pausedAt >= 0.0f) {
                        hold(handle);
                    } else {
                        handle.execute(ch -> ch.setVolume(volume));
                    }
                }
            }
        } catch (IllegalAccessException | RuntimeException e) {
            // Sans le canal, le morceau se joue quand meme : seule la pause perd son effet.
            return null;
        }
        return channel;
    }

    /** Met le morceau en attente, exactement la ou il est. */
    private static void pause() {
        if (playing == null || pausedAt >= 0.0f) return;

        pausedAt = position();
        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) hold(handle);
    }

    /** Reprend le morceau la ou la pause l'a laisse : le canal n'a pas bouge. */
    private static void resume() {
        if (playing == null || pausedAt < 0.0f) return;

        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) {
            float gain = volume;
            handle.execute(ch -> {
                ch.setVolume(gain);
                ch.unpause();
            });
        }

        // L'horloge repart de la position de la pause : la source, elle, n'a jamais avance.
        startedNano = System.nanoTime() - (long) (pausedAt * 1_000_000_000.0f);
        pausedAt = -1.0f;
    }

    /**
     * Met le canal en attente <b>et</b> a zero.
     *
     * <p>Les deux, et pas seulement la pause : le moteur remet tous ses canaux en marche quand un
     * ecran se ferme, et le temps qu'on le remette en pause, la source avait deja repris — « le son
     * reprend pendant une micro seconde mais ca s'entend a l'oreille ». Le gain a zero, lui, le
     * moteur n'y touche jamais : meme si la source repart, elle repart muette, et la pause reposee
     * juste apres ne se voit plus. C'est ce qui rend la reprise fiable au lieu d'etre probable.
     */
    private static void hold(ChannelAccess.ChannelHandle handle) {
        handle.execute(ch -> {
            ch.setVolume(0.0f);
            ch.pause();
        });
    }

    /** Bascule entre lecture et attente, sur le morceau en cours. */
    public static void togglePause() {
        if (playing == null) return;
        if (pausedAt >= 0.0f) {
            resume();
        } else {
            pause();
        }
    }

    /**
     * Le morceau en attente le reste, meme quand le moteur remet tout en marche.
     *
     * <p>{@code Minecraft.setScreen(null)} appelle {@code soundManager.resume()}, qui remet en
     * marche <b>tous</b> les canaux du moteur — le notre compris. Fermer un ecran, n'importe lequel,
     * relancait donc la musique mise en pause dans le terminal, alors que le port la croyait
     * toujours en attente : le bouton montrait lecture et l'horloge ne bougeait plus, mais le son,
     * lui, repartait — le joueur : « si je referme le data terminal, la musique se relance d'elle
     * meme ».
     *
     * <p>Le moteur ne demande pas son avis au son : il n'y a donc pas d'autre facon de tenir la
     * pause que de la reposer apres lui. C'est une commande sur le canal, pas un rechargement :
     * elle ne coute rien, et un canal deja en pause ne bouge pas.
     */
    private static void keepPaused() {
        if (playing == null || pausedAt < 0.0f) return;

        ChannelAccess.ChannelHandle handle = channel();
        if (handle != null) hold(handle);
    }

    /**
     * Repose la pause <b>tout de suite</b> apres la fermeture d'un ecran.
     *
     * <p>Le tick suffit a tenir la pause, mais pas a la tenir sans un trou : entre le moment ou le
     * moteur remet ses canaux en marche et le tick suivant, il s'ecoule jusqu'a cinquante
     * millisecondes — le joueur l'entend. Les ecrans du mod appellent donc ceci des que leur
     * {@code setScreen} a rendu la main : la pause se repose dans la meme passe du moteur de son,
     * et le gain reste a zero de bout en bout.
     */
    public static void reassertPause() {
        keepPaused();
    }

    /**
     * Le battement du lecteur : une tache par tick, et seulement quand un morceau est en attente.
     *
     * <p>C'est un abonne distinct, et pas le lecteur lui-meme, parce que Forge ne veut pas
     * d'abonnement sur une classe qui n'a pas de constructeur public : celui du lecteur est prive,
     * comme il se doit pour une classe qui n'est qu'un etat statique.
     */
    @Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
    public static final class Keeping {

        private Keeping() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            watchGamePause();
            keepPaused();
        }

        /** Le monde quitte : l'horloge s'arrete, et la position est retenue. */
        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            suspend();
        }

        /** Le monde rejoint : le morceau reprend ou il s'etait arrete. */
        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            resumeAfterLogin();
        }
    }

    /**
     * L'horloge s'arrete et repart avec la pause du jeu.
     *
     * <p>Le menu Echap met <b>tous</b> les sons du moteur en attente, celui du morceau compris : le
     * son se tait donc bien, mais l'horloge du lecteur, elle, continuait de tourner — le joueur :
     * « malgre le fait que la musique se mette bien en pause, le chrono lui continue de defiler au
     * lieu de s'arreter avec le jeu ». Et ce n'etait pas qu'un affichage : au retour, le son
     * reprenait a sa vraie position et l'horloge annoncait une autre seconde.
     *
     * <p>La pause du jeu se reconnait a {@code Minecraft.isPaused()} : c'est le meme drapeau que
     * celui qui decide du {@code soundManager.pause()} de vanilla, donc l'horloge s'arrete
     * exactement quand les sons s'arretent.
     */
    private static void watchGamePause() {
        boolean paused = Minecraft.getInstance().isPaused();
        if (paused == gamePaused) return;
        gamePaused = paused;

        if (playing == null) return;
        if (paused) {
            if (pausedAt < 0.0f) {
                pausedAt = position();
                pausedByGame = true;
            }
        } else if (pausedByGame) {
            // Le jeu rend la main : l'horloge repart de la ou la pause l'a laissee, et le son
            // reprend son volume — la mise en attente l'avait mis a zero.
            pausedByGame = false;
            float at = pausedAt;
            pausedAt = -1.0f;
            startedNano = System.nanoTime() - (long) (at * 1_000_000_000.0f);

            ChannelAccess.ChannelHandle handle = channel();
            if (handle != null) handle.execute(ch -> ch.setVolume(volume));
        }
    }

    /**
     * La config du client, relue quand elle arrive : c'est elle qui porte le volume du lecteur.
     *
     * <p>C'est un abonne de la meme classe, sur le bus MOD : la config se charge apres la
     * construction du mod, donc le volume lu au demarrage peut etre celui par defaut.
     */
    @Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class Configuring {

        private Configuring() {}

        @SubscribeEvent
        public static void onLoad(net.minecraftforge.fml.event.config.ModConfigEvent event) {
            if (event.getConfig().getSpec() != cn.academy.client.hud.HudConfig.SPEC) return;
            volume = cn.academy.client.hud.HudConfig.mediaVolume();
        }
    }

    /** Vrai quand un morceau est en attente. */
    public static boolean isPaused() {
        return playing != null && pausedAt >= 0.0f;
    }

    /** Arrete le morceau en cours, s'il y en a un. */
    public static void stop() {
        stopSound();
        playing = null;
        pausedAt = -1.0f;
        suspendedAt = -1f;
        pausedByGame = false;
    }

    private static void stopSound() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
        channel = null;
    }

    /** Le volume du lecteur, de zero a un. */
    public static float getVolume() {
        return volume;
    }

    /**
     * Regle le volume du lecteur, et le fait entendre tout de suite.
     *
     * <p>Le canal est commande directement : l'interface publique du moteur ne regle un son
     * qu'au lancement, et {@code updateCategoryVolume(MASTER, …)} ne touche que le gain de
     * l'ecouteur — c'est pour cela que la glissiere ne faisait rien jusqu'ici.
     */
    public static void setVolume(float value) {
        volume = Math.max(0.0f, Math.min(1.0f, value));

        ChannelAccess.ChannelHandle handle = channel();
        // Une glissiere ne reveille pas un morceau en attente : elle regle le volume qu'il aura en
        // reprenant, et reste donc muette tant qu'il est en pause.
        if (handle != null) {
            float gain = pausedAt >= 0.0f ? 0.0f : volume;
            handle.execute(ch -> ch.setVolume(gain));
        }
        saved = false;
    }

    /** Vrai quand le volume en memoire est deja ecrit dans la config. */
    private static boolean saved = true;

    /**
     * Ecrit le volume choisi dans la config du client.
     *
     * <p>A l'instant ou l'on quitte la glissiere, et pas a chaque pixel de son glissement : la
     * config se reecrit en entier, et la traîner de bout en bout l'ecrirait cent fois.
     */
    public static void saveVolume() {
        if (saved) return;
        saved = true;
        cn.academy.client.hud.HudConfig.writeMediaVolume(volume);
    }

    /**
     * La position du morceau, en secondes.
     *
     * <p>C'est l'horloge du lecteur : elle s'arrete pendant une pause, et reprend la ou elle
     * s'etait arretee. La source, elle, ne bouge pas.
     */
    public static float position() {
        if (playing == null) return 0.0f;
        if (pausedAt >= 0.0f) return pausedAt;
        return (System.nanoTime() - startedNano) / 1_000_000_000.0f;
    }

    /** La position du morceau, en secondes — le {@code playedTime} de l'original. */
    public static float elapsedSeconds() {
        return position();
    }

    /** Bascule : le morceau demande s'il n'est pas en cours, sinon silence. */
    public static void toggle(Media media) {
        if (isPlaying(media)) {
            stop();
        } else {
            play(media);
        }
    }

    public static boolean isPlaying(Media media) {
        return media != null && media.equals(playing);
    }

    /** Le morceau en cours, ou {@code null}. */
    public static Media current() {
        return playing;
    }

    /** Un champ prive par son nom, ou {@code null} si le jeu l'a deplace. */
    @Nullable
    private static Field field(Class<?> owner, String name) {
        try {
            Field found = owner.getDeclaredField(name);
            found.setAccessible(true);
            return found;
        } catch (NoSuchFieldException | RuntimeException e) {
            return null;
        }
    }
}
