package cn.academy.client.gui.cgui;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.LearningHelper;
import net.minecraft.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * L'arbre des competences : ses noeuds, ses traits, et ce qui les anime.
 *
 * <p>C'est la figure de l'original — chaque competence a sa place, reliee par un trait a celle
 * dont elle depend, son icone au milieu d'un disque, et l'experience acquise qui remplit l'anneau
 * autour d'elle — sortie de l'ecran du developpeur pour etre montree deux fois : sur la machine
 * (voir {@code DeveloperScreen}) et dans l'application du terminal (voir {@code SkillTreeScreen}).
 * L'original faisait exactement cela : un seul {@code Common.initialize(developer)}, appele avec la
 * machine ou sans elle.
 *
 * <h2>L'arbre se dessine au fil du temps</h2>
 *
 * <p>Chaque noeud apparait en fondu, decale de son rang, et le trait qui le relie a son parent
 * pousse du noeud vers lui. La souris ecarte tout l'arbre d'un dixieme de la distance qui la separe
 * du centre, ce qui donne au fond un relief de quelques unites. Rien n'est fige a l'ouverture : tout
 * se recalcule a chaque image.
 *
 * <h2>Ce que l'arbre ne fait pas</h2>
 *
 * <p>Il ne decide de rien : le clic sur un noeud est confie a l'ecran, qui ouvre sa fiche. Une
 * machine y met de quoi apprendre, l'application du terminal s'y contente de lire.
 */
public final class SkillTreeView {

    /**
     * La boite d'un noeud, et le trait qui le relie a son parent.
     *
     * <p>Le trait s'arrete a {@code 12,2} du centre de chaque noeud alors que le disque en fait
     * {@code 11,5} : il ne mord donc jamais dessus, et c'est pour cela que l'ordre de leur dessin ne
     * se voit pas.
     */
    private static final double NODE_BOX = 16.0;
    private static final double LINK_INSET = 12.2;
    private static final double LINK_WIDTH = 5.5;
    private static final double LINK_FADE = 5.0;

    /**
     * Ce qui reste a l'opacite d'un trait quand la competence n'est pas apprise : quatre dixiemes.
     *
     * <p>C'est le facteur de l'original — {@code mAlpha * (if (learned) 1.0 else 0.4)} — et il
     * manquait au port : un trait se dessinait a l'opacite de son noeud, deux fois et demie trop
     * clair des que la competence n'etait pas apprise. Le joueur a compare les deux versions cote a
     * cote : « la luminosite des elements n'est pas la meme pour les 2 ».
     */
    public static final double LINK_UNLEARNED = 0.4;
    /** L'apparition : un fondu par noeud, decale de son rang de huit centiemes de seconde. */
    private static final double APPEAR_STEP = 0.08;
    private static final double APPEAR_SPEED = 10.0;

    /** Le survol : un cinquieme de plus, atteint en un dixieme de seconde. */
    private static final double HOVER_SCALE = 1.2;
    private static final double HOVER_TRANSIT = 0.1;

    /** Le relief : tout l'arbre se decale de dix unites au plus, en sens inverse de la souris. */
    private static final double PARALLAX = 10.0;

    /**
     * Le fond de la zone de l'arbre.
     *
     * <p>L'image se dessine un centieme plus grande que la zone, et c'est <b>ce centieme</b> qui la
     * fait glisser : le relief de l'arbre, c'est le rapport entre ces quelques dixiemes d'unite et
     * les dix unites que parcourent les noeuds.
     */
    private static final String AREA_BACKDROP =
            "academy:textures/guis/effect/effect_developer_background.png";
    private static final double BACKDROP_SLIDE = 0.01;
    private static final double BACKDROP_WINDOW = 1.0 / 1.01;

    /** Le chemin de la zone dans la page. */
    private static final String AREA = "parent_right/area";

    /**
     * Un noeud de l'arbre, et de quoi l'animer.
     *
     * <p>{@code dim} est l'opacite que l'original donne a une competence selon son etat : pleine si
     * elle est apprise, sept dixiemes si celle dont elle depend l'est — elle est alors a portee — et
     * un quart sinon, c'est-a-dire a peine visible. Toute la progression se lit la, sans un mot.
     */
    private static final class Node {
        final Skill skill;
        final CWidget widget;
        final boolean learned;
        final double dim;
        final double born;
        boolean hovered;
        double since;

        Node(Skill skill, CWidget widget, boolean learned, double dim, double born) {
            this.skill = skill;
            this.widget = widget;
            this.learned = learned;
            this.dim = dim;
            this.born = born;
            this.since = born;
        }
    }

    private final GuiPageHost host;

    private final List<Node> nodes = new ArrayList<>();

    /** Ce que l'arbre montre, sous forme de phrase comparable : voir {@link #signature}. */
    private String signature = "";

    /** L'instant d'ouverture, en secondes : c'est l'origine de tous les fondus. */
    private final double opened;

    public SkillTreeView(GuiPageHost host) {
        this.host = host;
        this.opened = now();
    }

    /** Vrai quand l'arbre n'a aucun noeud : il n'y a alors rien a animer. */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /** La zone de l'arbre, ou {@code null} si la page ne la porte pas. */
    private CWidget area() {
        CWidget page = host.page();
        CWidget right = page == null ? null : page.child("parent_right");
        return right == null ? null : right.child("area");
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /**
     * L'arbre : un noeud par competence, et un trait par parente.
     *
     * <p>Toutes les competences de la categorie ne s'y trouvent pas. Une competence n'apparait que si
     * elle vaut la peine d'etre montree — si le joueur a le niveau pour elle, s'il l'a deja apprise,
     * ou si celle dont elle depend est apprise. C'est {@link LearningHelper} qui en decide, et
     * l'original s'en servait exactement de meme : l'arbre s'ouvre au fur et a mesure.
     */
    public void build(Category category, AbilityData data, Consumer<Skill> onSkill) {
        nodes.clear();

        CWidget area = area();
        if (area == null) return;

        area.children().clear();

        // Le fond de la zone, sur la zone elle-meme : c'est le moteur qui le dessine avant les
        // enfants, donc entre le fond de la page et les noeuds, a sa place exacte. Le dessiner
        // depuis l'ecran le posait sous toute la page, qui l'effacait.
        if (area.texture == null) area.texture = new CWidget.DrawTexture();
        area.texture.texture = AREA_BACKDROP;
        area.texture.u0 = slide(0);
        area.texture.v0 = slide(0);
        area.texture.u1 = area.texture.u0 + BACKDROP_WINDOW;
        area.texture.v1 = area.texture.v0 + BACKDROP_WINDOW;

        if (category == null) {
            note(null, data);
            return;
        }

        int index = 0;
        for (Skill skill : category.getSkills()) {
            if (!LearningHelper.canBePotentiallyLearned(data, skill)) continue;

            boolean learned = data.isSkillLearned(skill);
            Node node = new Node(skill, node(skill.getName()), learned,
                    dim(data, skill, learned), APPEAR_STEP * index);
            area.add(node.widget);
            host.onClick(node.widget, () -> onSkill.accept(skill));

            // Le trait vers le parent : il part du noeud et vise le centre de l'autre, en s'arretant
            // a douze unites et demie de chacun des deux.
            Skill parent = skill.getParent();
            if (parent != null) node.widget.line = link(skill, parent);

            nodes.add(node);
            index++;
        }

        // Le trait connait le noeud d'ou il part : son survol ecarte le premier bout, comme celui du
        // noeud qui le porte ecarte le second. Sans cela, le disque grossi passait par-dessus le bout
        // et le joueur voyait le trait au travers du cerne de progression.
        Map<Skill, CWidget> bySkill = new HashMap<>();
        for (Node node : nodes) bySkill.put(node.skill, node.widget);
        for (Node node : nodes) {
            if (node.widget.line == null) continue;
            Skill parent = node.skill.getParent();
            node.widget.line.source = parent == null ? null : bySkill.get(parent);
        }

        note(category, data);
    }

    /**
     * Rebatit l'arbre si ce qu'il montre a change.
     *
     * @return vrai quand l'arbre a ete refait, et que l'ecran doit donc le reposer
     */
    public boolean refresh(Category category, AbilityData data, Consumer<Skill> onSkill) {
        if (unchanged(category, data)) return false;
        build(category, data, onSkill);
        return true;
    }

    /**
     * Vrai quand ce que l'arbre montre n'a pas bouge.
     *
     * <p>L'original rebatissait tout son ecran apres un apprentissage, par un evenement. Le port
     * regarde plutot ce qui a change : la categorie, le niveau, et quelles competences sont
     * apprises. Tant que rien de tout cela ne bouge, l'arbre ne se refait pas — et des qu'une
     * competence s'ouvre, un noeud de plus apparait et son fondu repart pour lui.
     */
    public boolean unchanged(Category category, AbilityData data) {
        return signature(category, data).equals(signature);
    }

    /**
     * Note ce que la zone montre, quand ce n'est pas l'arbre.
     *
     * <p>Un joueur qui n'a encore aucune aptitude ne voit pas un arbre vide mais la console qui lui
     * dit comment entrer dans le mod. C'est toujours la zone qui le montre, et sans cette note
     * l'ecran la rebatirait a chaque image.
     */
    public void note(Category category, AbilityData data) {
        signature = signature(category, data);
    }

    /** Ce que l'arbre montre, sous forme d'une phrase comparable. */
    public String signature(Category category, AbilityData data) {
        if (category == null) return "-";
        StringBuilder out = new StringBuilder(category.getName());
        out.append('/').append(data.getCategoryLevel(category)).append('/');
        for (Skill skill : category.getSkills()) {
            out.append(data.isSkillLearned(skill) ? '+' : '-');
        }
        return out.toString();
    }

    /** L'opacite d'un noeud : voir {@link Node}. */
    private static double dim(AbilityData data, Skill skill, boolean learned) {
        if (learned) return 1.0;
        Skill parent = skill.getParent();
        return parent == null || data.isSkillLearned(parent) ? 0.7 : 0.25;
    }

    private static CWidget node(String name) {
        CWidget node = new CWidget("skill_" + name);
        node.transform.width = NODE_BOX;
        node.transform.height = NODE_BOX;
        node.transform.alignWidth = "LEFT";
        node.transform.alignHeight = "TOP";
        node.transform.scale = 1.0;
        node.disc = new CWidget.Disc();
        return node;
    }

    /**
     * Le trait qui relie une competence a celle dont elle depend, dans les unites de son noeud.
     *
     * <p>Le repere est celui de la boite du noeud : son centre est donc a huit unites, et le vecteur
     * vers le parent se lit directement dans les coordonnees de l'arbre — celles que les classes de
     * categorie ont reprises de l'original. Les deux bouts reculent de {@code 12,2} le long de ce
     * vecteur, et c'est ce qui laisse les deux disques degages.
     */
    private static CWidget.Line link(Skill skill, Skill parent) {
        double dx = parent.getGuiX() - skill.getGuiX();
        double dy = parent.getGuiY() - skill.getGuiY();
        double norm = Math.sqrt(dx * dx + dy * dy);
        double half = NODE_BOX / 2;

        CWidget.Line line = new CWidget.Line();
        line.thickness = LINK_WIDTH;
        line.progress = 0;
        line.x0 = half;
        line.y0 = half;
        line.x1 = half;
        line.y1 = half;
        if (norm <= 0) return line;

        double ux = dx / norm * LINK_INSET;
        double uy = dy / norm * LINK_INSET;
        line.x0 = dx + half - ux;
        line.y0 = dy + half - uy;
        line.x1 = half + ux;
        line.y1 = half + uy;
        return line;
    }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    /**
     * Ce qui bouge a chaque image : le relief, le survol, les fondus, et le trait qui pousse.
     *
     * <p>Les positions se posent ici, apres que le placement les a lues — les noeuds sont donc
     * dessines avec la place de l'image precedente, en retard d'une image. Cela ne se voit pas : un
     * soixantieme de seconde, sur un deplacement de dix unites au total. L'echelle du survol, elle,
     * se lit au dessin et s'applique donc tout de suite.
     *
     * @param blocked vrai quand une fiche est ouverte : elle est modale, et un noeud ne doit ni
     *                s'allumer ni repondre a travers son voile
     */
    public void frame(AbilityData data, double mouseX, double mouseY, int screenWidth,
                      int screenHeight, boolean blocked) {
        if (nodes.isEmpty()) return;

        double time = now();
        // Le relief se lit sur l'ecran entier, pas sur la zone de l'arbre : c'est ce que faisait
        // l'original, et c'est ce qui fait que le fond bouge meme quand la souris est sur le panneau.
        double dx = clamp01(mouseX / Math.max(1, screenWidth)) - 0.5;
        double dy = clamp01(mouseY / Math.max(1, screenHeight)) - 0.5;

        drawAreaBackdrop(dx, dy);

        for (Node node : nodes) {
            node.widget.transform.x = node.skill.getGuiX() - dx * PARALLAX;
            node.widget.transform.y = node.skill.getGuiY() - dy * PARALLAX;

            boolean hovered = !blocked && node.widget.isPointWithin(mouseX, mouseY);
            if (hovered != node.hovered) {
                node.hovered = hovered;
                node.since = time;
            }

            double transit = clamp01((time - node.since) / HOVER_TRANSIT);
            double scale = node.hovered
                    ? lerp(1.0, HOVER_SCALE, transit)
                    : lerp(HOVER_SCALE, 1.0, transit);

            double dt = Math.max(0, time - opened - node.born);
            CWidget.Disc disc = node.widget.disc;
            if (disc != null) {
                disc.icon = node.skill.getHintIcon().toString();
                disc.drawScale = scale;
                // L'anneau dit l'experience acquise, et l'icone grisee dit que la competence n'est
                // pas encore apprise. Les deux tiennent dans le meme noeud, et c'est ce qui fait
                // qu'on lit l'arbre d'un coup d'oeil : ce qui brille appartient au joueur, ce qui
                // grisonne reste a prendre.
                disc.drawFill = node.learned;
                disc.fill = data.getSkillExp(node.skill);
                disc.grey = !node.learned;
                // LE FONDU SE CLAMPE AVANT LA MULTIPLICATION. L'original ecrit
                // {@code mAlpha * clampd(0, 1, dt * 10.0)} : le fondu monte jusqu'a un, PUIS la
                // teinte du noeud le reduit. Le port multipliait d'abord et clamait ensuite — et
                // un produit qui depasse un une fois pour toutes ne redescend jamais : tout
                // atteignait donc la pleine opacite en une fraction de seconde, quelle que soit la
                // teinte du noeud. C'est ce que le joueur a vu d'un bout a l'autre de l'arbre :
                // « toutes les competences sont reliees avec la ligne blanche », et les noeuds
                // lointains « pas assez sombres ». Deux dixiemes de seconde suffisaient a effacer
                // les trois teintes de l'original — 1, 0,7 et 0,25.
                disc.backAlpha = alpha(appearAlpha(node.dim, dt));
                disc.iconAlpha = alpha(appearAlpha(node.dim, dt - APPEAR_STEP));
            }
            if (node.widget.line != null) {
                // Meme regle pour le trait, et c'est la ou elle se voyait le plus : son opacite
                // finale est {@code mAlpha * (if (learned) 1.0 else 0.4)}, une CONSTANTE — le fondu
                // ne dit que jusqu'ou le trait a pousse. Le port le melangeait a l'opacite, et le
                // produit saturait : les traits d'une competence non apprise sortaient blancs, et
                // c'est exactement le reproche du joueur — « la ligne est grise si on n'a pas
                // appris l'autre competence, et la ce n'est pas le cas ».
                node.widget.line.progress = clamp01(LINK_FADE * dt);
                node.widget.line.alpha = alpha(linkDim(node.dim, node.learned));
            }
        }
    }

    /**
     * L'opacite d'un trait : celle de son noeud, et le facteur des competences non apprises.
     *
     * <p>Voir {@link #LINK_UNLEARNED}. Une competence apprise garde son trait plein ; toutes les
     * autres le portent a quatre dixiemes, qu'elles soient a portee (sept dixiemes) ou non (un quart).
     */
    public static double linkDim(double dim, boolean learned) {
        return dim * (learned ? 1.0d : LINK_UNLEARNED);
    }

    /**
     * L'opacite d'un noeud pendant son apparition : son fondu, puis sa teinte.
     *
     * <p>C'est l'ordre de l'original — {@code mAlpha * clampd(0, 1, dt * 10.0)}, le fondu d'abord,
     * la teinte ensuite — et il n'est pas interchangeable. Le port faisait le produit puis bornait
     * le resultat a un, ce qui revient a effacer la teinte des que le fondu depasse un : les trois
     * valeurs de l'original, 1, 0,7 et 0,25, se retrouvaient toutes a 1 au bout de deux dixiemes de
     * seconde. Le joueur : « toutes les competences sont reliees avec la ligne blanche », et les
     * competences hors de portee « pas assez sombres ».
     *
     * @param dim     la teinte du noeud : voir {@link Node}
     * @param elapsed le temps depuis le debut de son fondu, en secondes ; une valeur negative borne
     *                le resultat a zero
     */
    public static double appearAlpha(double dim, double elapsed) {
        return dim * clamp01(APPEAR_SPEED * elapsed);
    }

    /**
     * Fait glisser le fond de la zone de l'arbre dans son cadre.
     *
     * <p>C'est l'image de l'original — {@code effect_developer_background} — un centieme plus
     * <b>grande</b> que la zone : elle deborde donc de cette marge, et la fenetre s'y deplace au
     * rythme de la souris. Elle ne bouge presque pas : juste assez pour que la zone paraisse posee
     * devant quelque chose. Les noeuds, eux, se deplacent de dix unites, et c'est ce decalage entre
     * les deux qui fait le relief.
     */
    private void drawAreaBackdrop(double dx, double dy) {
        CWidget area = area();
        if (area == null || area.texture == null) return;

        area.texture.u0 = slide(dx * BACKDROP_SLIDE);
        area.texture.v0 = slide(dy * BACKDROP_SLIDE);
        area.texture.u1 = area.texture.u0 + BACKDROP_WINDOW;
        area.texture.v1 = area.texture.v0 + BACKDROP_WINDOW;
    }

    /**
     * Ou la fenetre s'ouvre dans l'image.
     *
     * <p>L'original ne la fait pas partir du bord : elle se pose a un demi-centieme du centre, ce qui
     * laisse de quoi glisser d'un cote comme de l'autre. C'est son {@code scale()}.
     */
    private static double slide(double offset) {
        return (offset - 0.5) * BACKDROP_WINDOW + 0.5;
    }

    /** Une opacite de zero a 255, bornee. */
    private static int alpha(double fraction) {
        return (int) Math.round(clamp01(fraction) * 255);
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static double now() {
        return Util.getMillis() / 1000.0;
    }
}
