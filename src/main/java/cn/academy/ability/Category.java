package cn.academy.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groups related skills (e.g. Vecmanip, Electromaster). Port of the original 1.12.2 Category,
 * simplified: Controllable merged into Skill, config/rendering hooks dropped for now.
 */
public class Category {

    private final String name;
    private final List<Skill> skills = new ArrayList<>();
    private int categoryId = -1;

    /**
     * La teinte de la categorie, ARGB : la couleur du voile d'ecran quand l'aptitude est
     * allumee (voir {@code BackgroundMask}).
     *
     * <p>Portage du {@code colorStyle} de l'original, qu'il posait par {@code setColorStyle}.
     * Il la gardait cote client seulement et partait d'un blanc opaque ; le port en fait un
     * simple entier, disponible des deux cotes, et les quatre categories la posent dans leur
     * constructeur — comme son {@code CatElectromaster} faisait.
     */
    private int colorStyle = 0xFFFFFFFF;

    public Category(String name) {
        this.name = name;
    }

    /**
     * Ajoute une competence a la categorie.
     *
     * <p>Publique, et non protegee : les trois cursus generiques sont batis par une fabrique
     * ({@code GenericSkills.addTo}), qui n'est pas une sous-classe de {@code Category}. Une
     * competence n'appartient qu'a une categorie : {@code Skill.bind} y pose son identifiant, et
     * deux competences du meme nom dans une categorie levent.
     */
    public final void addSkill(Skill skill) {
        if (getSkill(skill.getName()) != null) {
            throw new IllegalStateException("Duplicate skill " + skill.getName() + " in category " + name);
        }
        skill.bind(this, skills.size());
        skills.add(skill);
    }

    public String getName() {
        return name;
    }

    /**
     * Cle de langue du nom affiche, comme {@code ac.ability.<nom>.name}.
     *
     * L'original tirait son nom de {@code I18n} au moment du rendu ; le port
     * expose la cle, ce qui laisse l'appelant decider quand traduire — au rendu
     * seulement, pour que rien ne soit construit cote serveur pour rien.
     */
    public String getDisplayKey() {
        return "ac.ability." + name + ".name";
    }

    public int getCategoryId() {
        return categoryId;
    }

    void setCategoryId(int id) {
        categoryId = id;
    }

    /**
     * Pose la teinte de la categorie, sans opacite.
     *
     * <p>C'est la signature de l'original : trois canaux valent une opacite nulle — un voile
     * qui ne se voit pas. C'est ainsi que vecmanip n'en a aucun.
     */
    public final void setColorStyle(int r, int g, int b) {
        setColorStyle(r, g, b, 0);
    }

    /** Pose la teinte de la categorie, opacite comprise. */
    public final void setColorStyle(int r, int g, int b, int a) {
        colorStyle = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** La teinte de la categorie, ARGB. */
    public final int getColorStyle() {
        return colorStyle;
    }

    public List<Skill> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public Skill getSkill(int id) {
        return id >= 0 && id < skills.size() ? skills.get(id) : null;
    }

    public Skill getSkill(String skillName) {
        for (Skill s : skills) {
            if (s.getName().equals(skillName)) return s;
        }
        return null;
    }
}
