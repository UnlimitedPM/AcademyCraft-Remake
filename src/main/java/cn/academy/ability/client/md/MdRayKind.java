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
 * Minecraft — c'est tout l'interet de les tenir ici. Le serveur le retrouve par ce nom au
 * moment de le jouer, par {@code SoundLookup}.
 *
 * <h2>Ou le rayon se pose</h2>
 *
 * <p>Le dernier drapeau, {@code viewOptimize}, est le {@code EntityRayBase.viewOptimize} de
 * l'original. Il l'y posait a <b>vrai</b> pour tous ses rayons, et l'eteignait sur les trois
 * qui ne partent pas du tireur : le rayon de la bombe a electrons, celui de la bombe a
 * fragmentation ({@code SBNetDelegate}), et la salve elle-meme — tous les trois partent de la
 * BILLE. Tout ce qui reste — le pre-rayon de la salve, et les trois rayons miniers — se
 * dessinait donc sur la main de son tireur, par l'optimisation de vue que
 * {@code RendererRayBaseGlow} appliquait a chaque image.
 *
 * <p>Le port tient ce drapeau ici parce que c'est {@code MdRayView} qui le lit, cote client :
 * c'est lui qui recole a la main de son tireur un rayon marque vrai, et laisse les autres ou
 * ils sont nes.
 *
 * <h2>La lueur peut depasser la pointe</h2>
 *
 * <p>Le dernier nombre, {@code glowEndFix}, est le {@code endFix} de {@code RendererRayGlow} de
 * l'original : de combien de blocs sa lueur — le « bandeau » qui tourne avec la camera, et non
 * les deux tubes — depassait la pointe du rayon. Il valait zero partout, sauf sur le railgun, qui
 * s'en servait pour aligner sa lueur sur ses cylindres : {@code 0,3}.
 *
 * <p>C'est cette valeur que reprend le pre-rayon de la salve, a la demande du joueur : son
 * bandeau s'arretait net au milieu de la bille de silicium, qui fait 0,6 de large. La lueur est
 * posee bout a bout entre le depart et la pointe, et c'est son dernier morceau qui avance
 * maintenant de ces trois dixiemes — elle couvre donc la bille entiere au lieu de s'eteindre en
 * son milieu. Les autres rayons gardent zero : ils s'arretent a leur pointe, comme chez
 * l'original.
 */
public record MdRayKind(String name,
                        ResourceLocation glowIn, ResourceLocation glowTile, ResourceLocation glowOut,
                        double glowWidth, float glowAlpha,
                        double innerRadius, Tint inner,
                        double outerRadius, Tint outer,
                        int lifeTicks, long blendInMs, long blendOutMs, long shrinkMs,
                        double sparkRate,
                        String sound, float soundVolume,
                        boolean viewOptimize,
                        double glowEndFix) {

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
            1.0, "md.ray_small", 0.8f, false, 0.0);

    /**
     * La salve de rayons, {@code EntityMdRayBarrage} : la gerbe qui part d'une bille de silicium.
     *
     * <p>Ses nombres sont ceux du petit rayon — c'est le meme dessin, {@code SmallMdRayRender} — a
     * une chose pres : elle vit <b>cinquante</b> ticks et non quatorze. Deux secondes et demie de
     * rayons qui s'agitent autour de la bille, c'est ce qui fait la salve ; l'eclair d'un quart de
     * seconde du petit rayon ne se lirait pas.
     *
     * <p>Elle <b>ne crache pas</b> d'etincelles : celles du petit rayon venaient de son entite,
     * et la salve descend d'une autre — ses 25 a 30 rayons se suffisent.
     *
     * <p>Elle ne se dessine pas toute seule pour autant : {@code MdRays} en fait une gerbe, voir
     * {@code MdBarrage}.
     */
    public static final MdRayKind BARRAGE = new MdRayKind("mdray_barrage",
            texture("mdray_small", "blend_in"), texture("mdray_small", "tile"),
            texture("mdray_small", "blend_out"),
            0.3, 0.5f,
            0.03, new Tint(216, 248, 216, 230),
            0.045, new Tint(106, 242, 106, 50),
            50, 100, 300, 500,
            0.0, "md.ray_small", 0.5f, false, 0.0);

    /**
     * Le pre-rayon : le trait qui annonce la salve, avant qu'elle ne parte.
     *
     * <p>L'original en avait un seul, dont la vie dependait de ce qu'il avait trouve —
     * {@code life = hit ? 50 : 30} — d'ou ces deux genres. Le premier accompagne la salve, le
     * second n'est qu'un eclair de visee.
     *
     * <p>Il est un peu plus <b>gros</b> que le petit rayon — un coeur de 4,5 cm et une gaine de
     * 5,2, une lueur de 0,4 bloc — parce que c'est lui qu'on regarde : c'est le trait qui dit ou
     * l'on a tire.
     *
     * <p>Il nait aux <b>yeux</b> de son tireur — le {@code y0 + 1.6} de l'original — mais il se
     * dessine sur sa main : c'est le drapeau {@code viewOptimize} de sa base, que
     * {@code EntityBarrageRayPre} ne pensait pas a eteindre, et c'est {@code MdRayView} qui
     * l'applique.
     */
    public static final MdRayKind BARRAGE_PRE_HIT = new MdRayKind("mdray_barrage_pre_hit",
            texture("mdray_small", "blend_in"), texture("mdray_small", "tile"),
            texture("mdray_small", "blend_out"),
            0.4, 0.5f,
            0.045, new Tint(216, 248, 216, 230),
            0.052, new Tint(106, 242, 106, 50),
            50, 200, 400, 500,
            0.0, "md.ray_small", 0.8f, true, 0.3);

    /** Et le meme, quand rien n'a ete trouve : trente ticks d'eclair, et c'est tout. */
    public static final MdRayKind BARRAGE_PRE_MISS = new MdRayKind("mdray_barrage_pre_miss",
            texture("mdray_small", "blend_in"), texture("mdray_small", "tile"),
            texture("mdray_small", "blend_out"),
            0.4, 0.5f,
            0.045, new Tint(216, 248, 216, 230),
            0.052, new Tint(106, 242, 106, 50),
            30, 200, 400, 500,
            0.0, "md.ray_small", 0.8f, true, 0.3);

    /**
     * Le faisceau du meltdowner, {@code EntityMDRay} : le tir charge de la categorie.
     *
     * <p>C'est le plus <b>gros</b> des rayons du plasma — un coeur de 17 cm et une gaine de 22, une
     * lueur d'un bloc et demi a 80 % d'opacite — et celui qui vit le plus longtemps : cinquante
     * ticks, deux secondes et demie, dont les sept derniers dixiemes s'effacent. Il a ses propres
     * textures, {@code mdray}, et non celles du petit rayon.
     *
     * <p>Il nait sur son tireur : il se recolle donc a sa main, comme le pre-rayon de la salve et
     * par le meme drapeau. Il crache une etincelle huit ticks sur dix, posee au hasard le long de
     * lui-meme jusqu'a dix blocs. Quant a son son, c'est celui de la competence,
     * {@code md.meltdowner} : c'est la competence qui le joue, au relachement, comme l'original.
     */
    public static final MdRayKind MELTDOWNER = new MdRayKind("mdray",
            texture("mdray", "blend_in"), texture("mdray", "tile"), texture("mdray", "blend_out"),
            1.5, 0.8f,
            0.17, new Tint(216, 248, 216, 230),
            0.22, new Tint(106, 242, 106, 50),
            50, 200, 700, 300,
            0.8, "md.meltdowner", 0.5f, true, 0.0);

    /** La duree du rayon, en millisecondes : cinquante par tick, comme l'original. */
    public long lifeMs() {
        return lifeTicks * 50L;
    }

    /** Est-ce la salve, celle qui doit devenir une gerbe ? */
    public boolean isBarrage() {
        return this == BARRAGE;
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
        return java.util.List.of(SMALL, BARRAGE, BARRAGE_PRE_HIT, BARRAGE_PRE_MISS, MELTDOWNER);
    }

    private static ResourceLocation texture(String ray, String part) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/" + ray + "/" + part + ".png");
    }
}
