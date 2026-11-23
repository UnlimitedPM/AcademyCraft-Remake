package cn.academy.client.gui.cgui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

/**
 * La console du developpeur : le texte qui defile, la saisie, et les commandes.
 *
 * <p>Portage de la {@code Console} de {@code SkillTree.scala}. L'original ne montrait pas un arbre
 * vide a un joueur sans aptitude : il posait cette console a la place de l'arbre, et c'est elle qui
 * lui apprenait qu'il fallait taper {@code learn}. Le port s'en etait passe — il affichait le texte
 * d'un coup et remplacait la frappe par un bouton — et le joueur a reclame la mecanique :
 * « quand j'ouvre l'ability developer sans pouvoir, il n'y a pas l'animation du texte ni meme la
 * mecanique de devoir taper /learn pour apprendre le pouvoir ».
 *
 * <h2>Ce que fait la console</h2>
 *
 * <p>Elle empile des <b>taches</b> et les joue une par une, comme l'original : un texte s'ecrit
 * lettre par lettre ({@code slowPrint}), une sequence de nombres defile puis s'efface
 * ({@code animSequence}), une pause laisse respirer, et une tache peut attendre un evenement du
 * monde — la fin d'un developpement, par exemple. Les lignes s'empilent dans une fenetre de dix,
 * et la derniere porte la saisie avec son curseur clignotant.
 *
 * <p>Rien ici ne connait Minecraft : les textes arrivent deja traduits, et l'horloge est passee a
 * {@link #tick(double)}. C'est ce qui permet de la relire en JUnit, la ou un ecran ne se teste pas.
 */
public final class DeveloperConsole {

    /** La fenetre de la console : dix lignes, comme {@code Console.MaxLines}. */
    public static final int MAX_LINES = 10;

    /** L'invite, celle de l'original. */
    public static final String HEAD = "OS >";

    /** Un caractere toutes les centiemes de seconde : le {@code PerCharTime} de l'original. */
    private static final double PER_CHAR = 0.01;

    /** Le temps d'affichage d'un nombre de la sequence de demarrage. */
    private static final double ANIM_STEP = 0.3;

    /** La respiration apres la banniere, avant l'echec du demarrage. */
    private static final double PAUSE_INIT = 0.4;

    /**
     * Les textes de la console, deja traduits.
     *
     * <p>L'original les lisait dans son fichier de langue a chaque appel ; le port les resout une
     * fois, a la construction, pour que cette classe reste sans Minecraft.
     *
     * @param progress le texte de la ligne de progression, avec le pourcentage deja formate
     */
    public record Texts(String init, String bootFailed, String invalidCat, String learnHint,
                        String override, String invalidCommand, IntFunction<String> progress,
                        String devBegin, String devSucc, String devFail,
                        String resetBegin, String resetSucc, String resetFail,
                        String resetFailDev, String resetFailOther) {}

    /**
     * Une tache de la console : le {@code Task} de l'original.
     *
     * <p>Elle est publique parce que l'ecran en ecrit — celle qui attend la fin d'un developpement
     * lit l'etat de la machine, que cette classe ne connait pas.
     */
    public interface Task {
        default void begin(double now) {}

        default void update(double now) {}

        default void finish() {}

        boolean isFinished(double now);
    }

    private final Texts texts;
    private final Random random;

    private final List<String> outputs = new ArrayList<>();
    private final Deque<Task> queue = new ArrayDeque<>();
    private final Map<String, Runnable> commands = new LinkedHashMap<>();

    private Task current;
    private String input = "";
    private double now;
    private boolean rebuild;
    private boolean startupDone;

    /** La tache qui attend la frappe : elle ne finit jamais, et c'est elle qui porte le curseur. */
    private final Task inputTask = new Task() {
        @Override
        public void begin(double at) {
            output(HEAD);
        }

        @Override
        public boolean isFinished(double at) {
            return false;
        }
    };

    /**
     * La console, avec sa sequence de demarrage.
     *
     * @param emergency   vrai quand le joueur tient la bobine magnetique : l'original annoncait
     *                    alors une surcharge du systeme et proposait {@code reset}
     * @param hasDeveloper vrai quand une machine est ouverte : sans elle, la console ne propose
     *                    aucune commande — c'est le cas de l'application du terminal
     */
    public DeveloperConsole(Texts texts, String playerName, boolean emergency, boolean hasDeveloper,
                            Random random) {
        this.texts = texts;
        this.random = random;

        enqueue(slowPrint(texts.init().replace("%s", playerName)));
        pause(PAUSE_INIT);

        // La sequence de l'original : six nombres qui montent, un dernier plus haut, puis l'echec.
        List<String> sequence = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            sequence.add((i * 10 + random.nextInt(6) - 3) + "%");
        }
        sequence.add((64 + random.nextInt(4)) + "%");
        sequence.add(texts.bootFailed());
        animSequence(ANIM_STEP, sequence);

        String startup = emergency ? texts.override()
                : hasDeveloper ? texts.invalidCat() + texts.learnHint()
                : texts.invalidCat();
        enqueue(slowPrint(startup));
    }

    // ------------------------------------------------------------------
    // Le temps
    // ------------------------------------------------------------------

    /**
     * Un pas d'horloge : la tache courante avance, et la suivante prend sa place.
     *
     * <p>C'est le {@code FrameEvent} de l'original, mot pour mot : finir, mettre a jour, puis
     * prendre la tache suivante — ou l'attente de frappe quand la file est vide.
     */
    public void tick(double at) {
        this.now = at;

        if (current != null && current.isFinished(at)) {
            current.finish();
            current = null;
        }
        if (current != null) current.update(at);

        if (current == null) {
            current = queue.isEmpty() ? inputTask : queue.poll();
            current.begin(at);
        }
    }

    /** L'instant du dernier pas, en secondes. */
    public double now() {
        return now;
    }

    // ------------------------------------------------------------------
    // Les taches
    // ------------------------------------------------------------------

    /** Empile une tache. Si la console attendait la frappe, la ligne en cours est validee. */
    public void enqueue(Task task) {
        queue.offer(task);
        if (current == inputTask) {
            outputln(input);
            current = null;
        }
    }

    /** Un texte ecrit d'un coup. */
    public Task print(String text) {
        return new Task() {
            @Override
            public void begin(double at) {
                output(text);
            }

            @Override
            public boolean isFinished(double at) {
                return true;
            }
        };
    }

    /** Un texte ecrit lettre par lettre : le {@code slowPrintTask} de l'original. */
    public Task slowPrint(String text) {
        return new Task() {
            private int index;
            private double last;

            @Override
            public void begin(double at) {
                last = at;
            }

            @Override
            public void update(double at) {
                int count = (int) ((at - last) / PER_CHAR);
                if (count <= 0) return;
                int end = Math.min(text.length(), index + count);
                output(text.substring(index, end));
                last += count * PER_CHAR;
                index = end;
            }

            @Override
            public boolean isFinished(double at) {
                return index == text.length();
            }
        };
    }

    /** Une attente, en secondes. */
    public void pause(double seconds) {
        enqueue(new Task() {
            private double born;

            @Override
            public void begin(double at) {
                born = at;
            }

            @Override
            public boolean isFinished(double at) {
                return at - born >= seconds;
            }
        });
    }

    /**
     * Une suite de textes qui se remplacent : chacun s'ecrit, tient un instant, puis s'efface.
     *
     * <p>C'est l'{@code animSequence} de l'original, et c'est ce qui donne son air de machine qui
     * demarre : les nombres montent, puis disparaissent d'un coup de retours arriere.
     */
    public void animSequence(double seconds, List<String> texts) {
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            boolean last = i == texts.size() - 1;
            enqueue(new Task() {
                private double born;

                @Override
                public void begin(double at) {
                    born = at;
                    output(text);
                }

                @Override
                public boolean isFinished(double at) {
                    return at - born >= seconds;
                }

                @Override
                public void finish() {
                    if (!last) output("\b".repeat(text.length()));
                }
            });
        }
    }

    /**
     * Demande la reconstruction de l'ecran, une fois le texte de fin lu.
     *
     * <p>C'est l'{@code enqueueRebuild} de l'original : sans elle, l'arbre remplacerait la console
     * des que l'aptitude arrive, et le joueur ne verrait jamais « Development successful ».
     */
    public void enqueueRebuild() {
        enqueue(new Task() {
            @Override
            public void begin(double at) {
                rebuild = true;
            }

            @Override
            public boolean isFinished(double at) {
                return true;
            }
        });
    }

    /** Vrai quand la console a fini son travail et demande a etre remplacee. */
    public boolean rebuildRequested() {
        return rebuild;
    }

    /**
     * Vrai quand la console attend la frappe, sans rien avoir en cours.
     *
     * <p>C'est ce qui dit a l'ecran qu'elle peut etre remplacee par l'arbre : tant qu'une tache
     * court — la banniere, la sequence, la progression d'un apprentissage — la console doit rester,
     * sinon son animation disparait avant d'avoir ete vue.
     */
    public boolean idle() {
        return current == inputTask && queue.isEmpty();
    }

    /**
     * Vrai quand la sequence de demarrage a fini de se jouer, une fois pour toutes.
     *
     * <p>L'ecran s'en sert pour ne pas remplacer la console au milieu de sa banniere : une aptitude
     * qui arrive pendant l'animation la laisse finir, et c'est seulement apres qu'elle cede la
     * place. Le drapeau reste vrai — la console ne rejoue pas son demarrage.
     */
    public boolean startupFinished() {
        if (idle()) startupDone = true;
        return startupDone;
    }

    /**
     * Un pourcentage, sans zero inutile et SANS le signe : {@code 1}, {@code 10}, {@code 100}.
     *
     * <p>Le signe appartient a la phrase de la langue ({@code Progress: %s%}), et non a ce nombre :
     * le mettre des deux cotes donnait {@code 0%%49%} — le joueur l'a vu sur sa capture. Et
     * l'original ecrivait {@code 01%} (son {@code fmt} ajoutait un zero sous dix), ce que le joueur
     * ne veut pas : « normalement il n'y a pas de 0 affiche pour rien, juste 1% pas 001% ».
     *
     * <p>La tache de progression n'efface donc plus un nombre FIXE de caracteres : elle retient la
     * longueur de ce qu'elle a ecrit et l'efface, ce qui marche pour les trois tailles.
     */
    public static String percent(int value) {
        return String.valueOf(value);
    }

    // ------------------------------------------------------------------
    // Les lignes
    // ------------------------------------------------------------------

    /**
     * Ecrit dans la derniere ligne, en respectant les deux caracteres de l'original.
     *
     * <p>{@code \b} efface un caractere — c'est ainsi que la sequence de demarrage s'efface et que
     * le pourcentage se reecrit — et {@code \n} ouvre une ligne. La fenetre ne garde que les dix
     * dernieres.
     */
    public void output(String content) {
        StringBuilder line = new StringBuilder(
                outputs.isEmpty() ? "" : outputs.remove(outputs.size() - 1));

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\b') {
                if (line.length() > 0) line.setLength(line.length() - 1);
            } else if (ch == '\n') {
                outputs.add(line.toString());
                outputs.add("");
                line = new StringBuilder(outputs.remove(outputs.size() - 1));
            } else {
                line.append(ch);
            }
        }

        outputs.add(line.toString());
        while (outputs.size() > MAX_LINES) outputs.remove(0);
    }

    public void outputln(String content) {
        output(content + '\n');
    }

    public void outputln() {
        output("\n");
    }

    /**
     * Les lignes a dessiner, la derniere portant la saisie et son curseur.
     *
     * <p>Le curseur clignote une demi-seconde sur deux, comme le {@code "_"} de l'original.
     */
    public List<String> lines() {
        List<String> out = new ArrayList<>(outputs);
        if (out.isEmpty()) out.add("");

        if (current == inputTask) {
            int last = out.size() - 1;
            out.set(last, out.get(last) + input + (blink() ? "_" : ""));
        }
        return out;
    }

    private boolean blink() {
        return ((int) (now * 1000) % 1000) < 500;
    }

    // ------------------------------------------------------------------
    // La saisie
    // ------------------------------------------------------------------

    /** Une commande que la console sait executer. */
    public void command(String name, Runnable action) {
        commands.put(name, action);
    }

    public void type(char character) {
        input += character;
    }

    public void backspace() {
        if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
    }

    /**
     * Valide la ligne : la commande part, ou la console dit qu'elle n'existe pas.
     *
     * <p>La ligne se vide APRES l'execution, et non avant : c'est ce qui la laisse s'ecrire dans la
     * console. L'original faisait exactement cela — {@code parseCommand(input)} puis
     * {@code input = ""} — et l'ordre inverse effacait le mot qu'on venait de taper, parce que
     * {@link #enqueue} recopie la ligne courante avant de passer a la suite.
     */
    public void submit() {
        String name = input;
        Runnable action = commands.get(name);
        if (action == null) {
            input = "";
            enqueue(print(texts.invalidCommand()));
        } else {
            action.run();
            input = "";
        }
    }

    /** La ligne en cours de frappe, sans le curseur. */
    public String input() {
        return input;
    }

    /** Les textes de cette console : l'ecran s'en sert pour ses commandes. */
    public Texts texts() {
        return texts;
    }
}
