package cn.academy.terminal.app;

import cn.academy.terminal.App;

/**
 * L'application Arbre de competences.
 *
 * Portage de {@code AppSkillTree}. C'est la seule application dont le port ait le
 * contenu : les categories et leurs niveaux existent depuis le developpeur, donc
 * son ecran a quelque chose a montrer. Les autres (Reglages, A propos,
 * MisakaCloud, Emetteur de frequence) attendent encore leur contenu.
 *
 * L'ecran lui-meme est enregistre cote client, dans
 * {@code terminal.client.TerminalScreens}.
 */
public final class AppSkillTree extends App {

    public static final AppSkillTree INSTANCE = new AppSkillTree();

    private AppSkillTree() {
        super("skill_tree");
    }

    @Override
    public int getIconSize() {
        return 110;
    }
}
