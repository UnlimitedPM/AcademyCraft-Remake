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

    /**
     * La vie d'un rayon <b>tenu</b> : les {@code 233333} ticks de l'original, soit trois heures.
     *
     * <p>Ce n'est pas une duree, c'est une absence de duree : un rayon minier ne s'eteint pas
     * tout seul, c'est le client qui le tue quand la touche se relache. L'original lui donnait
     * quand meme une vie, et la voici — au bout de trois heures de maintien ininterrompu, il
     * s'efface pour de bon, comme chez l'original.
     */
    public static final int HELD_TICKS = 233333;

    /**
     * Le rayon minier de base, {@code EntityMineRayBasic} : le rayon tenu de la pierre.
     *
     * <p>C'est le petit rayon, au chiffre pres — memes textures, {@code mdray_small}, meme coeur
     * de trois centimetres, meme gaine de quatre et demi, meme lueur de 0,3 bloc a 50 % — a trois
     * choses pres. Il <b>vit</b> au lieu de clignoter : l'original lui donnait la vie de ses
     * rayons tenus, {@link #HELD_TICKS}, et c'est le client qui le tuait a la fin du maintien. Il
     * <b>ne crache donc pas tout seul</b> : ses etincelles ont leur propre rythme et leur propre
     * vitesse, que {@code MineRayEffect} semme — d'ou ce {@code sparkRate} laisse a zero, qui ne
     * veut pas dire « aucun » mais « pas par moi ». Et son <b>son</b> n'est pas le sien : c'est la
     * boucle du maintien, {@code md.mine_loop}, que {@code HeldLoops} ouvre tant que la touche
     * reste enfoncee, et que le genre rappelle ici pour qu'il n'y ait qu'un endroit ou la lire.
     *
     * <p>Il se dessine sur la main de son tireur — {@code viewOptimize} — et sa portee n'est pas
     * celle de sa competence : quinze blocs de rayon pour dix blocs creuses. C'est l'original qui
     * le voulait ainsi, et cela se voit quand on mine de loin.
     */
    public static final MdRayKind MINE_BASIC = new MdRayKind("mdray_mine_basic",
            texture("mdray_small", "blend_in"), texture("mdray_small", "tile"),
            texture("mdray_small", "blend_out"),
            0.3, 0.5f,
            0.03, new Tint(216, 248, 216, 230),
            0.045, new Tint(106, 242, 106, 50),
            HELD_TICKS, 200, 400, 300,
            0.0, "md.mine_loop", 0.3f, true, 0.0);

    /**
     * Le rayon minier de l'expert, {@code EntityMineRayExpert} : le meme, une taille au-dessus.
     *
     * <p>Ses textures sont les siennes — {@code mdray_expert} — et son coeur est un peu plus large
     * que celui du rayon de base : 4,5 cm contre 3, et 5,6 de gaine contre 4,5. Il a surtout une
     * lueur de 0,5 bloc, dont le dessin de l'original rabaissait l'opacite a 50 % a chaque image :
     * sa fabrique en annoncait 70, son {@code doRender} la ramenait a 50, et c'est 50 qu'on voit.
     * Le coeur a de meme une opacite de 180 et non de 230, pour la meme raison.
     *
     * <p>Comme son cadet, il vit jusqu'au relachement, et ses etincelles sont celles de
     * {@code MineRayEffect} — une sur trois par tick, elles, contre une sur deux pour le rayon de
     * base.
     */
    public static final MdRayKind MINE_EXPERT = new MdRayKind("mdray_mine_expert",
            texture("mdray_expert", "blend_in"), texture("mdray_expert", "tile"),
            texture("mdray_expert", "blend_out"),
            0.5, 0.5f,
            0.045, new Tint(216, 248, 216, 180),
            0.056, new Tint(106, 242, 106, 50),
            HELD_TICKS, 200, 400, 300,
            0.0, "md.mine_loop", 0.3f, true, 0.0);

    /**
     * Le rayon minier de la chance, {@code EntityMineRayLuck} : le rayon de l'expert, mais dore.
     *
     * <p>Il perce et il use exactement comme l'expert — memes nombres, memes quinze blocs — et
     * c'est sa <b>couleur</b> qui le distingue : son coeur est presque blanc (241, 229, 247) et sa
     * gaine violette (205, 166, 232), la ou les deux autres sont vertes. Sa lueur fait 0,45 bloc a
     * 60 %, entre les deux autres.
     *
     * <p>Et ses etincelles ne sont pas les memes : l'original donnait a celles de son rayon la
     * texture {@code md_particle_luck}, une etoile a quatre branches, la ou les deux autres
     * crachent la bille de plasma ordinaire. C'est le seul rayon du port qui ait deux textures
     * d'etincelle — et c'est {@code MineRayEffect} qui s'en souvient, pas le genre.
     */
    public static final MdRayKind MINE_LUCK = new MdRayKind("mdray_mine_luck",
            texture("mdray_luck", "blend_in"), texture("mdray_luck", "tile"),
            texture("mdray_luck", "blend_out"),
            0.45, 0.6f,
            0.04, new Tint(241, 229, 247, 230),
            0.05, new Tint(205, 166, 232, 50),
            HELD_TICKS, 200, 400, 300,
            0.0, "md.mine_loop", 0.3f, true, 0.0);

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
        return java.util.List.of(SMALL, BARRAGE, BARRAGE_PRE_HIT, BARRAGE_PRE_MISS, MELTDOWNER,
                MINE_BASIC, MINE_EXPERT, MINE_LUCK);
    }

    private static ResourceLocation texture(String ray, String part) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/effects/" + ray + "/" + part + ".png");
    }
}
