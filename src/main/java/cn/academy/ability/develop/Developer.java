package cn.academy.ability.develop;

/**
 * Un developeur d'aptitudes, quelle que soit sa forme.
 *
 * <p>Portage de {@code IDeveloper} de la 1.12.2, ou l'interface servait justement a cela :
 * {@code TileDeveloper} et {@code PortableDevData} etaient deux implementations de la meme
 * chose, et {@code DevelopData} n'en connaissait aucune des deux. Le port avait fondu les
 * deux dans le block entity ; l'objet portable a besoin de la meme separation.
 *
 * <p>L'interface porte ce dont l'ecran a besoin pour s'afficher et ce dont le serveur a
 * besoin pour lancer un apprentissage — pas plus. Une implementation doit savoir
 * <b>payer</b> (l'energie lui appartient : un tampon pour le bloc, l'objet tenu pour le
 * portable) et <b>ou elle en est</b>.
 */
public interface Developer {

    DeveloperType getDeveloperType();

    /** Energie disponible, celle qui paiera les prochaines stimulations. */
    double getEnergy();

    default int getEnergyStored() {
        return (int) getEnergy();
    }

    int getMaxEnergyStored();

    /** Avancement de l'apprentissage en cours, entre 0 et 1. */
    double getProgress();

    DevelopProgress.DevState getState();

    /** Categorie visee, ou -1. */
    int getCategoryId();

    /** Competence visee, ou -1 si c'est le niveau de la categorie qui monte. */
    int getSkillId();

    /**
     * Lance un apprentissage deja decide.
     *
     * <p>C'est le chemin qu'emprunte le changement de categorie : il ne se deduit pas
     * d'un identifiant de competence, puisque c'est la main du joueur qui le decide, et
     * il doit donc etre construit avant d'etre lance.
     */
    boolean startDeveloping(net.minecraft.server.level.ServerPlayer player, DevelopAction action);

    /**
     * Lance un apprentissage en le designant par sa cible.
     *
     * @param skillId l'identifiant de la competence dans sa categorie, ou -1 pour faire
     *                monter la categorie d'un cran
     * @return vrai si l'apprentissage a pu demarrer
     */
    boolean startDeveloping(net.minecraft.server.level.ServerPlayer player, int categoryId,
                            int skillId);

    /** Interrompt l'apprentissage en cours, sans rien rendre. */
    void abort();

    /**
     * Vrai si le developeur est raccorde a un reseau energetique.
     *
     * <p>Le bloc l'est ; l'objet portable tire son energie de lui-meme, donc jamais.
     * L'ecran s'en sert pour dire au joueur ce qu'il doit brancher.
     */
    boolean isLinked();
}
