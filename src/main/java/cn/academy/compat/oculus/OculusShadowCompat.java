package cn.academy.compat.oculus;

import cn.academy.DeveloperBlockEntity;
import cn.academy.WindgenMainBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Les block entities du mod dans la carte d'ombre d'Oculus.
 *
 * <p>Oculus 1.20.1 a bien un crochet pour rendre les block entities dans la carte d'ombre
 * ({@code ShadowRenderer.renderBlockEntities}), mais sa liste {@code visibleBlockEntities} n'est
 * jamais remplie : aucun rendu de block entity ne projette donc d'ombre, pales d'eolienne et
 * developpeurs compris.
 *
 * <p>Le passage d'ombre d'Oculus appelle une fonction interchangeable,
 * {@code ShadowRenderingState.function} : c'est elle que l'on enveloppe, pour dessiner nos
 * machines dans la carte avant de laisser Oculus faire son propre travail. La fonction est
 * privee et n'a pas de crochet public cote Forge : elle se lit donc par reflexion, sous ses deux
 * noms ({@code net.irisshaders.iris} pour les versions recentes, {@code net.coderbot.iris} pour
 * les anciennes). Rien n'est compile contre Oculus ; sans Oculus, cette classe ne fait rien.
 *
 * <p>Le crochet est revalide une fois par seconde : Embeddium (et Rubidium) remplacent cette
 * fonction quand ils prennent la main sur le rendu, et une installation faite une fois pour
 * toutes serait alors perdue sans que rien ne le dise.
 */
@OnlyIn(Dist.CLIENT)
public final class OculusShadowCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("academy-oculus");

    /** L'etat d'ombre des deux generations d'Oculus, du plus recent au plus ancien. */
    private static final String[] STATE_NAMES = {
            "net.irisshaders.iris.shadows.ShadowRenderingState",
            "net.coderbot.iris.shadows.ShadowRenderingState"
    };

    /** Les rendus d'ombre des deux generations d'Oculus : ils portent le drapeau ACTIVE. */
    private static final String[] SHADOW_RENDERERS = {
            "net.irisshaders.iris.shadows.ShadowRenderer",
            "net.coderbot.iris.pipeline.ShadowRenderer"
    };

    /** Un plafond : une grande installation ne doit pas remplir la carte d'ombre a elle seule. */
    private static final int MAX_TRACKED = 512;

    /** Les machines deja vues par leur rendu, donc susceptibles de projeter une ombre. */
    private static final Set<BlockPos> TRACKED = new LinkedHashSet<>();

    /** Le monde des positions suivies : changer de monde vide la liste. */
    private static ClientLevel trackedLevel;

    /** Le drapeau ACTIVE d'Oculus : vrai pendant la construction de la carte d'ombre. */
    private static Field activeField;
    private static boolean activeResolved;

    /** Le proxy installe a la place de la fonction d'Oculus. */
    private static Object proxy;

    /** La prochaine verification du crochet : au plus une par seconde. */
    private static long nextCheck;

    /** Ce qui a deja ete dit au journal : on ne le repete pas a chaque seconde. */
    private static boolean reported;

    private OculusShadowCompat() {}

    /**
     * Enveloppe la fonction d'ombre d'Oculus pour y glisser nos machines.
     *
     * <p>Le chemin vanilla lit une liste qu'Oculus oublie de remplir ; le chemin Sodium, lui,
     * remplace la fonction par la sienne. On remplace donc la fonction elle-meme par un proxy
     * qui dessine nos machines dans le tampon d'ombre, puis appelle l'ancienne fonction.
     *
     * <p>A appeler a chaque tick client : sans Oculus, la classe visee n'existe pas et rien ne se
     * passe ; avec Oculus, la verification d'une seconde a l'autre ne coute qu'une lecture de
     * champ.
     */
    public static void ensureInstalled() {
        long now = System.currentTimeMillis();
        if (now < nextCheck) return;
        nextCheck = now + 1000L;

        try {
            install();
        } catch (Throwable error) {
            report(error);
        }
    }

    /**
     * Enveloppe la fonction d'ombre d'Oculus, si elle n'est pas deja la notre.
     *
     * <p>Le passage d'ombre d'Oculus ne dessine pas les block entities tout seul : la liste qu'il
     * parcourt n'est jamais remplie, et le chemin Sodium remplace la fonction entiere. Le proxy
     * dessine donc les machines lui-meme, puis laisse la fonction d'origine faire son travail.
     */
    private static void install() throws Exception {
        for (String stateName : STATE_NAMES) {
            Class<?> state = load(stateName);
            if (state == null) continue;

            Field function = field(state, "function");
            if (function == null) continue;
            function.setAccessible(true);

            Object current = function.get(null);
            if (current == null) continue;
            if (current == proxy) {
                // Toujours en place : rien a faire, et c'est le cas ordinaire.
                return;
            }

            Class<?> type = function.getType();
            if (!type.isInterface()) continue;
            ClassLoader loader = type.getClassLoader() != null
                    ? type.getClassLoader() : OculusShadowCompat.class.getClassLoader();
            Object wrapped = Proxy.newProxyInstance(loader, new Class<?>[] { type },
                    new ShadowFunction(current));
            function.set(null, wrapped);
            proxy = wrapped;
            if (!reported) {
                reported = true;
                LOGGER.info("Oculus detecte : les machines du mod passent par la carte d'ombre ({}).",
                        stateName);
            }
            return;
        }
        report(null);
    }

    /** Un champ declare, ou {@code null} si la version d'Oculus ne le porte pas. */
    private static Field field(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Ce qui a echoue, dit une seule fois : un journal par seconde ne sert a personne. */
    private static void report(Throwable error) {
        if (reported) return;
        reported = true;
        if (error == null) {
            LOGGER.debug("Oculus absent (ou d'une forme inconnue) : pas de carte d'ombre a alimenter.");
        } else {
            LOGGER.warn("Oculus : le crochet de la carte d'ombre a echoue.", error);
        }
    }

    /**
     * Le proxy installé a la place de la fonction d'Oculus.
     *
     * <p>Il ne connait de la signature que ce dont il a besoin : la pose, le tampon, la position
     * de la camera et le temps partiel. Les deux generations d'Oculus ont la meme tete pour ces
     * cinq valeurs, meme si celle de 1.20.1 recente ajoute un booleen en fin de liste.
     */
    private static final class ShadowFunction implements InvocationHandler {

        private final Object delegate;

        ShadowFunction(Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("renderBlockEntities")) {
                try {
                    draw(args);
                } catch (Throwable ignored) {
                    // Ne jamais empecher Oculus de dessiner ses propres ombres.
                }
                return method.invoke(delegate, args);
            }
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("toString")) return "academy shadow wrapper";
            return method.invoke(delegate, args);
        }

        private static void draw(Object[] args) {
            PoseStack pose = null;
            MultiBufferSource.BufferSource buffers = null;
            double[] camera = new double[3];
            int cameraCount = 0;
            float tick = 0f;

            for (Object arg : args) {
                if (arg instanceof PoseStack value) {
                    pose = value;
                } else if (arg instanceof MultiBufferSource.BufferSource value) {
                    buffers = value;
                } else if (arg instanceof Double value) {
                    if (cameraCount < 3) camera[cameraCount++] = value;
                } else if (arg instanceof Float value) {
                    tick = value;
                }
            }
            if (pose == null || buffers == null) return;

            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) return;
            BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();
            // On dessine nous-memes, sans passer par la liste d'Oculus : elle est vide sur le
            // chemin vanilla, et le chemin Sodium la remplace par la sienne. Le dessin est le
            // meme que le sien — meme pose, meme translation vers la camera d'ombre.
            for (BlockPos pos : List.copyOf(TRACKED)) {
                BlockEntity entity = level.getBlockEntity(pos);
                if (!(entity instanceof WindgenMainBlockEntity)
                        && !(entity instanceof DeveloperBlockEntity)) {
                    TRACKED.remove(pos);
                    continue;
                }
                pose.pushPose();
                pose.translate(pos.getX() - camera[0], pos.getY() - camera[1],
                        pos.getZ() - camera[2]);
                dispatcher.render(entity, tick, pose, buffers);
                pose.popPose();
            }
        }
    }

    /** Une machine vient d'etre dessinee : elle pourra projeter une ombre des l'image suivante. */
    public static void track(BlockPos pos) {
        // Le crochet s'installe aussi par ici : une machine vue suffit, sans attendre qu'une
        // competence allume quoi que ce soit.
        ensureInstalled();

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        if (level != trackedLevel) {
            TRACKED.clear();
            trackedLevel = level;
        }
        if (TRACKED.size() >= MAX_TRACKED) {
            TRACKED.remove(TRACKED.iterator().next());
        }
        TRACKED.add(pos.immutable());
    }

    /**
     * Sommes-nous dans la carte d'ombre d'Oculus ?
     *
     * <p>Sert aux rendus qui n'ont pas la meme forme selon le passage : le developpeur, par
     * exemple, est translucide a l'ecran mais doit ecrire la profondeur dans la carte d'ombre.
     */
    public static boolean inShadowPass() {
        if (!activeResolved) {
            activeResolved = true;
            for (String name : SHADOW_RENDERERS) {
                try {
                    Class<?> type = load(name);
                    if (type == null) continue;
                    activeField = type.getField("ACTIVE");
                    break;
                } catch (Throwable ignored) {
                    // Version absente, ou champ renomme : on essaie la suivante.
                }
            }
        }
        if (activeField == null) return false;
        try {
            return activeField.getBoolean(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Charge une classe d'Oculus, quel que soit le chargeur qui la porte.
     *
     * <p>Forge met chaque mod dans son propre module : le chargeur de ce mod ne voit pas
     * forcement Oculus, et celui du jeu ne le voit pas davantage. La couche de modules du jeu,
     * elle, les enumere tous — c'est la seule liste complete qui existe.
     */
    private static Class<?> load(String name) {
        for (ClassLoader loader : loaders()) {
            try {
                return Class.forName(name, false, loader);
            } catch (Throwable ignored) {
                // Essaie le suivant.
            }
        }
        return null;
    }

    /** Tous les chargeurs qui peuvent porter Oculus, du plus probable au plus general. */
    private static List<ClassLoader> loaders() {
        Set<ClassLoader> out = new LinkedHashSet<>();
        add(out, OculusShadowCompat.class.getClassLoader());
        add(out, Thread.currentThread().getContextClassLoader());
        add(out, Minecraft.class.getClassLoader());
        try {
            ModuleLayer layer = FMLLoader.getGameLayer();
            for (Module module : layer.modules()) add(out, module.getClassLoader());
        } catch (Throwable ignored) {
            // Pas de couche de modules : les chargeurs ci-dessus restent essayes.
        }
        add(out, ClassLoader.getSystemClassLoader());
        return new ArrayList<>(out);
    }

    private static void add(Set<ClassLoader> loaders, ClassLoader loader) {
        if (loader != null) loaders.add(loader);
    }
}
