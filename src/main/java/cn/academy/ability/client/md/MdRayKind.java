package cn.academy.ability.client.md;

import cn.academy.AcademyCraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Un genre de rayon du meltdowner : ses textures, ses deux cylindres, sa lueur, sa duree.
 *
 * <p>L'original avait une entite par genre de rayon — {@code EntityMdRaySmall},
 * {@code EntityMDRay}, {@code EntityMdRayBarrage}, et un par rayon minier — mais toutes
 * descendaient de {@code EntityRayBase} et se dessinaient avec le meme composeur : une
 * <b>lueur</b> texturee de trois morceaux, et <b>deux cylindres</b> concentriques. Ce qui
 * changeait d'une entite a l'autre, c'etait les nombres : ceux de la fabrique, et ceux que
 * chaque competence y posait ensuite. Les voici tous, dans un seul endroit.
 *
 * <h2>Les trois morceaux de la lueur</h2>
 *
 * <p>La lueur n'est pas un ruban uniforme : c'est un <b>entre</b>, un <b>milieu</b> et une
 * <b>sortie</b>, trois images differentes posees bout a bout le long du rayon. Le premier
 * morceau fait apparaitre la lueur depuis rien, le dernier la fait disparaitre, et le milieu
 * les relie. Chaque genre a donc trois textures, rangees dans son propre dossier —
 * {@code effects/mdray_small/blend_in.png}, {@code tile.png} et {@code blend_out.png}.
 *
 * <h2>Les deux cylindres</h2>
 *
 * <p>Le <b>coeur</b> est un tube clair et opaque (216, 248, 216, 230 pour le petit rayon) et
 * la <b>gaine</b> un tube plus large, tres transparent (106, 242, 106, 50) : c'est elle qui
 * donne au rayon sa couleur verte et son epaisseur. La lueur, elle, est blanche — elle eclaircit
 * l'ensemble sans le teinter.
 *
 * <h2>Et le son</h2>
 *
 * <p>Chaque rayon s'annonce en nassant : l'original le faisait dans l'entite elle-meme, chez
 * le client, a sa position. Le son fait donc partie du genre, avec son volume — 0,8 pour les
 * petits rayons, 0,5 pour la salve.
 *
 * <p>Il est donne par son <b>nom</b> et non par son objet : un {@code RegistryObject} n'existe
 * que sur un jeu en marche, et les nombres d'un rayon se relisent donc en test, sans lancer
 * Minecraft — c'est tout l'interet de les tenir ici. Le paquet du rayon refait l'objet au
 * moment de jouer.
 */
public record MdRayKind(String name,
                        ResourceLocation glowIn, ResourceLocation glowTile, ResourceLocation glowOut,
                        double glowWidth, float glowAlpha,
                        double innerRadius, Tint inner,
                        double outerRadius, Tint outer,
                        int lifeTicks, long blendInMs, long blendOutMs, long shrinkMs,
                        double sparkRate,
                        String sound, float soundVolume) {

    /** Une couleur telle que l'original la donnait : quatre nombres de 0 a 255. */
    public record Tint(int r, int g, int b, int a) {

        public float red() {
            return r / 255f;
        }

        public float green() {
            return g / 255f;
        }

        public float blue() {
            return b / 255f;
        }

        public float alpha() {
            return a / 255f;
        }
    }

    /**
     * Le petit rayon, {@code mdray_small} : la bille a electrons, la bombe a fragmentation et
     * la salve.
     *
     * <p>Ses nombres sont ceux de {@code SmallMdRayRender} de l'original : un coeur de 3 cm et
     * une gaine de 4,5, une lueur de 0,3 bloc d'opacite 0,5, et une vie de quatorze ticks.
     * Quatorze ticks pour un rayon de quinze blocs, c'est court — sept dixiemes de seconde — et
     * c'est voulu : c'est un eclair de plasma, pas un faisceau tenu.
     *
     * <p>Sa largeur s'effondre sur les cinq derniers dixiemes de seconde et son opacite sur les
     * quatre derniers : le rayon s'effile donc en s'eteignant, au lieu de disparaitre d'un coup.
     *
     * <p>Et il <b>crache</b> : une etincelle par tick, semee quelque part le long de lui-meme,
     * pendant toute sa courte vie.
     */
    public static final MdRayKind SMALL = new MdRayKind("mdray_small",
            texture("mdray_small", "blend_in"), texture("mdray_small", "tile"),
            texture("mdray_small", "blend_out"),
            0.3, 0.5f,
            0.03, new Tint(216, 248, 216, 230),
            0.045, new Tint(106, 242, 106, 50),
            14, 200, 400, 500,
            1.0, "md.ray_small", 0.8f);

    /** La duree du rayon, en millisecondes : cinquante par tick, comme l'original. */
    public long lifeMs() {
        return lifeTicks * 50L;
    }

    /** Le genre qui porte ce nom, ou le petit rayon si le nom est inconnu. */
    public static MdRayKind byName(String name) {
        for (MdRayKind kind : all()) {
            if (kind.name().equals(name)) return kind;
        }
        return SMALL;
    }

    /** Les genres connus, dans l'ordre ou ils sont apparus. */
    public static java.util.List<MdRayKind> all() {
        return java.util.List.of(SMALL);
    }

    private static ResourceLocation texture(String ray, String part) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/" + ray + "/" + part + ".png");
    }
}
