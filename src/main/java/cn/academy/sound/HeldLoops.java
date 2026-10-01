package cn.academy.sound;

/**
 * Quelles competences tenues font une boucle sonore, et laquelle.
 *
 * <p>Portage des {@code FollowEntitySound(...).setLoop()} de l'original : huit
 * competences font tourner un son qui suit le joueur tant que leur effet dure — le
 * bouclier, l'intensification du corps, la traction, la manipulation d'un bloc, le
 * branchement d'une machine, les rayons miniers, les ailes de tempete, et la charge du
 * meltdowner.
 *
 * <h2>Pourquoi une table de noms, et pas des competences</h2>
 *
 * La classe est <b>pure</b> : elle rend des chaines, pas des {@code SoundEvent} — ceux-ci
 * vivent dans un registre, que JUnit ne charge pas. L'ecran, ou plutot le client, fait la
 * traduction. C'est le meme decoupage que pour les tutoriels, et pour la meme raison : le
 * choix se verifie en test, la lecture se fait en jeu.
 *
 * <p>Une simplification assumee : l'original ouvrait certaines boucles plus tard que
 * l'appui — le bouclier quand il etait vraiment leve, les rayons quand ils mordaient le
 * bloc, le branchement quand il trouvait une machine. Le port les ouvre toutes des
 * l'appui, faute de savoir chez le client ce que le serveur a trouve. Le son est donc un
 * peu plus genereux qu'chez l'original, jamais absent.
 */
public final class HeldLoops {

    /**
     * Une boucle : l'evenement a jouer, son volume, et le son de mise en route.
     *
     * <p>Les volumes sont ceux de l'original, qui n'en donnait que deux : 0,3 pour le
     * branchement et les rayons miniers — des sons d'entretien, presque en fond — et le
     * plein pour les autres, qui sont l'effet lui-meme.
     *
     * <p>Le bouclier est le seul a avoir un son de mise en route : l'original le jouait a
     * l'ouverture du maintien, une demi-seconde avant que la boucle ne prenne le relais.
     */
    public record Loop(String event, float volume, String startup) {

        /** Le son aussi suit le joueur : il vient de lui, pas d'un point du monde. */
        public boolean followsPlayer() {
            return true;
        }

        public boolean hasStartup() {
            return startup != null;
        }
    }

    /** Le volume des boucles que l'original laissait en fond. */
    public static final float QUIET = 0.3f;

    private static final Loop NONE = null;

    private HeldLoops() {}

    /**
     * La boucle d'une competence, ou {@code null} si elle n'en a pas.
     *
     * <p>L'identifiant est le nom de la competence, celui qui nomme aussi ses cles de
     * langue et ses fichiers.
     */
    public static Loop forSkill(String skill) {
        if (skill == null) return NONE;
        return switch (skill) {
            case "light_shield" -> new Loop("md.shield_loop", 1.0f, "md.shield_startup");
            case "body_intensify" -> new Loop("em.intensify_loop", 1.0f, null);
            case "mag_movement" -> new Loop("em.move_loop", 1.0f, null);
            case "mag_manip" -> new Loop("em.lf_loop", 1.0f, null);
            case "storm_wing" -> new Loop("vecmanip.storm_wing", 1.0f, null);
            // Le meltdowner ne se tient pas, il se CHARGE : sa boucle est son son de charge, celui
            // que l'original faisait suivre au joueur tant que la touche restait enfoncee.
            case "meltdowner" -> new Loop("md.md_charge", 1.0f, null);
            case "charging" -> new Loop("em.charge_loop", QUIET, null);
            case "mine_ray_basic", "mine_ray_expert", "mine_ray_luck" ->
                new Loop("md.mine_loop", QUIET, null);
            default -> NONE;
        };
    }

    /** Les competences qui font une boucle, pour le test et pour l'ecran. */
    public static java.util.List<String> loopingSkills() {
        return java.util.List.of("light_shield", "body_intensify", "mag_movement", "mag_manip",
                "storm_wing", "meltdowner", "charging", "mine_ray_basic", "mine_ray_expert",
                "mine_ray_luck");
    }
}
