package cn.academy.client.gui.cgui;

/**
 * Ce qu'un morceau de page demande a l'ecran qui le porte.
 *
 * <p>Trois gestes, et pas un de plus : la page, ce qu'un clic fait, et le repose. L'arbre des
 * competences ({@link SkillTreeView}) et les fiches qui s'ouvrent dessus ({@link GuiSurface}) s'en
 * servent tous les deux, et c'est ce qui leur permet de vivre sur deux ecrans differents — la
 * machine du developpeur, qui est un conteneur, et l'application du terminal, qui est une page
 * seule.
 *
 * <p>L'ecran les leur donne par une petite classe anonyme : {@code onClick} et {@code reflow} y
 * sont {@code protected}, et une methode protegee ne peut pas tenir la promesse publique d'une
 * interface.
 */
public interface GuiPageHost {

    /** La page qui porte l'arbre et les fiches, ou {@code null} avant qu'elle n'existe. */
    CWidget page();

    /** Ce que ce widget fait quand on clique dessus. */
    void onClick(CWidget widget, Runnable action);

    /**
     * Repose la page.
     *
     * <p>A appeler des qu'un widget est ne hors du {@code build} : un widget neuf n'a pas encore de
     * place a l'ecran, et le dessiner avant de le poser le poserait au coin.
     */
    void reflow();
}
