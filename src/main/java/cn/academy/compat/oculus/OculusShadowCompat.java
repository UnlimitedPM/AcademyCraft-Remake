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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Les block entities du mod dans la carte d'ombre d'Oculus.
 *
 * <p>Oculus 1.20.1 a bien un crochet pour rendre les block entities dans la carte d'ombre
 * ({@code ShadowRenderer.renderBlockEntities}), mais sa liste {@code visibleBlockEntities} n'est
 * jamais remplie : aucun rendu de block entity ne projette donc d'ombre, pales d'eolienne et
 * developpeurs compris. Les deux mixins du paquet {@code cn.academy.mixin} appellent
 * {@link #addToShadowPass()} au point d'entree commun d'Oculus, et cette classe y ajoute les
 * machines qui ont ete vues au moins une fois par leur rendu.
 *
 * <p>Rien n'est compile contre Oculus : le champ est lu par reflexion, sous ses deux noms
 * ({@code net.irisshaders.iris} pour les versions recentes, {@code net.coderbot.iris} pour les
 * anciennes). Sans Oculus, la classe n'est jamais appelee et ne fait rien.
 */
@OnlyIn(Dist.CLIENT)
public final class OculusShadowCompat {

    /** Les deux generations d'Oculus, du plus recent au plus ancien. */
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

    /** Le champ statique d'Oculus, resolu une seule fois. */
    private static Field listField;
    private static boolean resolved;

    /** Le drapeau ACTIVE d'Oculus : vrai pendant la construction de la carte d'ombre. */
    private static Field activeField;
    private static boolean activeResolved;

    /** Le wrapper installe dans ShadowRenderingState.function, et sa prochaine tentative. */
    private static boolean installed;
    private static long nextInstallTry;

    private OculusShadowCompat() {}

    /**
     * Enveloppe la fonction d'ombre d'Oculus pour y glisser nos machines.
     *
     * <p>Le mixin couvre le chemin vanilla, ou Oculus lit sa liste ; le chemin Sodium, lui, passe
     * par une lambda qui n'utilise pas cette liste. On remplace donc la fonction elle-meme par un
     * proxy qui dessine nos machines dans le tampon d'ombre, puis appelle l'ancienne fonction.
     * Le dessin en double (quand l'ancienne fonction les voit deja) ne change rien : la carte
     * d'ombre ne garde que la profondeur.
     *
     * <p>A appeler a chaque tick client : sans Oculus, la classe visee n'existe pas et rien ne se
     * passe ; avec Oculus, l'installation se fait une fois puis le drapeau l'arrete.
     */
    public static void ensureInstalled() {
        if (installed) return;
        long now = System.currentTimeMillis();
        if (now < nextInstallTry) return;
        nextInstallTry = now + 1000L;

        for (String stateName : new String[] {
                "net.irisshaders.iris.shadows.ShadowRenderingState",
                "net.coderbot.iris.shadows.ShadowRenderingState"}) {
            try {
                Class<?> state = load(stateName);
                if (state == null) continue;
                Field function = state.getDeclaredField("function");
                function.setAccessible(true);
                Object delegate = function.get(null);
                if (delegate == null) continue;

                Class<?> type = function.getType();
                if (!type.isInterface()) continue;
                Object proxy = Proxy.newProxyInstance(type.getClassLoader(),
                        new Class<?>[] { type }, new ShadowFunction(delegate));
                function.set(null, proxy);
                installed = true;
                return;
            } catch (Throwable ignored) {
                // Version absente, champ inaccessible, ou forme differente : on retentera.
            }
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
                    draw(proxy, args);
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

        private static void draw(Object proxy, Object[] args) {
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

    /** Ajoute les machines suivies a la liste que la carte d'ombre va parcourir. */
    public static void addToShadowPass() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        List<BlockEntity> visible = shadowList();
        if (visible == null) return;

        for (BlockPos pos : List.copyOf(TRACKED)) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (!(entity instanceof WindgenMainBlockEntity)
                    && !(entity instanceof DeveloperBlockEntity)) {
                // La machine a ete cassee ou remplacee : on oublie la position.
                TRACKED.remove(pos);
                continue;
            }
            if (!visible.contains(entity)) {
                visible.add(entity);
            }
        }
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

    /** La liste d'Oculus, ou {@code null} si Oculus n'est pas la (ou a change de forme). */
    @SuppressWarnings("unchecked")
    private static List<BlockEntity> shadowList() {
        if (!resolved) {
            resolved = true;
            for (String name : SHADOW_RENDERERS) {
                try {
                    Class<?> type = load(name);
                    if (type == null) continue;
                    listField = type.getField("visibleBlockEntities");
                    break;
                } catch (Throwable ignored) {
                    // Version absente, ou champ renomme : on essaie la suivante.
                }
            }
        }
        if (listField == null) return null;
        try {
            return (List<BlockEntity>) listField.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Charge une classe d'Oculus, quel que soit le classloader qui la porte.
     *
     * <p>Forge peut mettre chaque mod dans sa propre couche de modules : le classloader de ce
     * mod ne voit alors pas forcement Oculus, tandis que celui du jeu et le contexte du thread
     * le voient. On essaie les trois, sans jamais faire echouer l'appel.
     */
    private static Class<?> load(String name) {
        ClassLoader[] loaders = {
                OculusShadowCompat.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                Minecraft.class.getClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            try {
                return Class.forName(name, false, loader);
            } catch (Throwable ignored) {
                // Essaie le suivant.
            }
        }
        return null;
    }
}
