package cn.academy.ability.client.tp;

import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.teleporter.PenetrateTeleportSkill;
import cn.academy.ability.teleporter.TeleporterCategory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * La marque de teleportation : le fantome qui montre ou l'on va atterrir.
 *
 * <p>C'est le portage d'{@code EntityTPMarking} et de ses trois contextes clients — la
 * teleportation au marqueur, celle qui traverse les murs, et le scintillement. L'original en
 * faisait une <b>entite cliente</b> : le contexte la posait chez son tireur, la deplacait a chaque
 * tick sur la destination du moment, et la tuait a la fin du maintien. Le port n'a pas d'entites
 * d'effet, donc elle vit ici — une position, une orientation, un age — et
 * {@code TpMarkRenderer} la dessine.
 *
 * <h2>Une seule destination, mais trois facons de la trouver</h2>
 *
 * <p>Chaque competence dit la sienne, et c'est la <b>meme</b> fonction que celle qui deplacera le
 * joueur : {@code MarkTeleportSkill.destination}, {@code FlashingSkill.destination} et
 * {@code PenetrateTeleportSkill.destination}. Il n'y a donc pas deux geometries a tenir d'accord,
 * et le fantome ne peut pas mentir sur l'endroit ou l'on arrive — c'est tout l'interet de la
 * marque.
 *
 * <p>Le scintillement ne montre rien tant qu'aucune touche de direction n'est enfoncee : chez
 * l'original, c'etait la touche elle-meme qui allumait l'anneau, et son relachement qui faisait
 * partir le saut. Une fois partie, la marque s'efface et le fantome disparait avec elle.
 *
 * <p>Le saut traversant, lui, se pose a <b>hauteur d'yeux</b> de sa destination, et il sait si
 * l'on peut y atterrir : dans un mur, le fantome devient rouge et le saut sera refuse. C'est
 * l'original, qui lisait son {@code dest.available} a chaque tick.
 *
 * <h2>Son age</h2>
 *
 * <p>La marque vit tant que la competence vit, et son age ne sert qu'a une chose : le defilement
 * de ses sept images. Il repart de zero a chaque nouvelle marque, donc deux teleportations de
 * suite ne reprennent pas l'animation ou la precedente l'avait laissee.
 */
@OnlyIn(Dist.CLIENT)
public final class TeleportMark {

    /** La chance qu'a la marque de lacher une etincelle, par tick — l'original : 0,4. */
    public static final double SPARK_CHANCE = 0.4;

    /** La boite ou elles naissent : un bloc de large, de part et d'autre de la marque. */
    public static final double SPARK_RADIUS = 1.0;

    /**
     * Et leur hauteur, sous les pieds de la marque.
     *
     * <p>De moins un bloc quatre a zero, ce qui n'est pas une erreur de signe : l'original posait
     * ses particules a {@code ranged(0,2, 1,6) - 1,6}, un reste de calcul ou la position de la
     * marque etait prise pour une hauteur d'yeux. Le port garde le nombre tel quel — c'est celui
     * qui a ete valide a l'epoque — et il tombe juste : la marque se tient a hauteur de cou, donc
     * ses etincelles se semment autour de son torse.
     */
    public static final double SPARK_LOW = -1.4;
    public static final double SPARK_HIGH = 0.0;

    /** Leur vitesse : trois centimetres par tick, un peu plus vers le haut que vers le bas. */
    public static final double SPARK_SPEED = 0.03;
    public static final double SPARK_RISE = 0.05;

    /** La teinte d'une marque sans rien a signaler : le blanc de l'original. */
    public static final int COLOR_NORMAL = 0xFFFFFFFF;

    /** Et celle d'un fantome qui signale un obstacle : son rouge. */
    public static final int COLOR_THREATENING = 0xFFFF3333;

    /**
     * Le gris du vide, pour la boite du lancer d'objet : l'original, 0xba dans les trois canaux.
     *
     * <p>Opaches, tous les quatre : l'original ecrivait bien une transparence dans sa
     * {@code Color} — 0xba ici, 0x2a pour l'orange, 0xa0 et 0xb4 pour la chair — mais son rendu
     * l'ignorait. Son marqueur etait dessine sans melange, donc son alpha n'etait jamais lu et la
     * boite sortait pleine. La garder translucide, c'est ce qui faisait paraitre la notre delavee ;
     * l'alpha d'origine reste note ici, mais il ne se voit pas.
     */
    public static final int COLOR_VOID = 0xFFBABABA;

    /**
     * Et son ORANGE des qu'une creature est visee.
     *
     * <p>C'est bien un orange, et non le rouge de la chair : les deux competences ont chacune
     * leurs teintes, et celle-ci est la sienne.
     *
     * <p>Attention a l'ordre des canaux : l'original ecrit {@code new Color(0xba, 0xb2, 0x23,
     * 0x2a)}, qui est du <b>RGBA</b> — rouge, vert, bleu, alpha — alors qu'un entier Java se lit
     * ARGB. Les recopier tels quels donnerait un rouge (0xba de rouge, 0x23 de vert), et c'est
     * exactement l'erreur qui a ete faite ici : l'orange du lancer d'objet s'affichait rouge.
     */
    public static final int COLOR_HIT_ORANGE = 0xFFBAB223;

    /** Le gris eteint de la chair qui ne trouve rien, et son rouge quand elle trouve. */
    public static final int COLOR_FLESH_IDLE = 0xFF4A4A4A;
    public static final int COLOR_FLESH_HIT = 0xFFB91919;

    /** La boite du lancer d'objet dans le vide : un demi-bloc, comme son marqueur. */
    public static final double VOID_BOX = 0.5;

    /** Celle de la chair quand elle ne trouve personne : un bloc entier. */
    public static final double FLESH_BOX = 1.0;

    private static final RandomSource RANDOM = RandomSource.create();

    /** La ou la marque se tient, ou {@code null} s'il n'y en a pas. */
    @Nullable
    private static Vec3 position;

    /** Et ou elle se tenait au tick precedent : le rendu interpole entre les deux. */
    @Nullable
    private static Vec3 previous;

    /** L'orientation du tireur, relue au tick comme l'original recopiait sa rotation. */
    private static float yaw;

    /** La couleur de la marque : le fantome en est teinte, la boite en est dessinee. */
    private static int color = COLOR_NORMAL;

    /** Et sa forme : le fantome du joueur, ou une boite aux dimensions demandees. */
    private static Shape shape = Shape.GHOST;

    /** Si la marque se tient sur la ligne des pieds : voir {@link #onLine} et {@link #interpolated}. */
    private static boolean line;

    /** Et ou elle se tient sur cette ligne, a quelle distance des pieds. */
    private static double along;
    private static double previousAlong;

    /** La hauteur d'yeux du tireur, pour retrouver ses pieds depuis la camera. */
    private static double eyeHeight = 1.62;

    /**
     * En deca de cette distance, la marque glisse d'un tick a l'autre ; au-dela, elle saute.
     *
     * <p>Trois blocs : c'est plus que ce qu'une creature qui marche ou qu'un regard qui balaie
     * parcourt en un tick, et bien moins que ce qu'un changement de point fait.
     */
    public static final double SMOOTH_DISTANCE = 3.0;

    /** Son age, en ticks : il ne sert qu'au defilement de ses images. */
    private static int ageTicks;

    private TeleportMark() {
    }

    /**
     * Un tick de marque, si c'est bien une competence de teleportation qui tient la touche.
     *
     * <p>Rien ne se passe pour les autres : la marque s'efface, ce qui evite qu'un fantome reste
     * plante dans le decor quand le joueur change de competence sans relacher.
     */
    public static void tick(Player player, Skill skill, int chargeTicks, int aimed) {
        Seat seat = seat(player, skill, chargeTicks, aimed);
        if (seat == null) {
            end();
            return;
        }

        // Une marque qui nait : son age repart de zero, donc son animation aussi — et elle n'a pas
        // de position precedente, sans quoi le rendu la ferait glisser depuis la derniere.
        if (position == null) ageTicks = 0;
        Vec3 next = seat.position();
        previous = follow(position, next);
        position = next;

        // Ou la marque tombe par rapport a la ligne qui part des PIEDS le long du regard : c'est
        // cette ligne-la que le rendu sait tenir dans l'axe du curseur — voir interpolated.
        Vec3 feet = player.position();
        Vec3 look = player.getViewVector(1.0f);
        eyeHeight = player.getEyeHeight();
        double nextAlong = alongLine(feet, look, next);
        boolean nextLine = onLine(feet, look, next);
        // Si elle n'y etait pas au tick d'avant, il n'y a rien a interpoler : on prend la distance
        // d'aujourd'hui, et le rendu se posera dessus.
        previousAlong = line ? along : nextAlong;
        along = nextAlong;
        line = nextLine;
        color = seat.color();
        shape = seat.shape();
        yaw = player.getYRot();
        sowSpark();
        ageTicks++;
    }

    /** La competence est finie : le fantome s'en va. */
    public static void end() {
        position = null;
        previous = null;
        line = false;
        along = 0;
        previousAlong = 0;
        ageTicks = 0;
        color = COLOR_NORMAL;
        shape = Shape.GHOST;
    }

    /** Ou la marque se tient, ou {@code null}. Lu par le rendu, qui n'a rien d'autre a savoir. */
    @Nullable
    public static Vec3 position() {
        return position;
    }

    /**
     * D'ou part l'interpolation de l'image : le point precedent, ou le nouveau s'il est trop loin.
     *
     * <p>Le rendu glisse entre les deux derniers ticks de la marque — c'est ce qui fait suivre une
     * creature qui marche sans saccades. Mais un <b>saut</b> n'est pas un deplacement : le regard
     * qui accroche un autre bloc, le saut qui sort de la matiere, la competence qui change de cas —
     * autant de pas de plusieurs blocs qui ne sont pas un chemin. Les interpoler faisait traverser
     * au fantome tout ce qu'il y a entre les deux, et se voyait en jeu comme un bond sans raison.
     *
     * <p>Fonction pure, donc verifiable : en deca de {@link #SMOOTH_DISTANCE} on glisse, au-dela on
     * se pose.
     */
    public static Vec3 follow(@Nullable Vec3 from, Vec3 to) {
        return from != null && from.distanceTo(to) <= SMOOTH_DISTANCE ? from : to;
    }

    /** L'ecart lateral tolere pour dire qu'une marque se tient sur la ligne du regard, en blocs. */
    public static final double ON_GAZE = 0.05;

    /**
     * A quelle distance des pieds, le long du regard, se trouve ce point.
     *
     * <p>Fonction pure, donc verifiable.
     */
    public static double alongLine(Vec3 feet, Vec3 look, Vec3 at) {
        return at.subtract(feet).dot(look);
    }

    /**
     * Vrai si la marque se tient sur la ligne qui part des <b>pieds</b> le long du regard.
     *
     * <p>C'est la ligne des marques qu'on pose au bout du regard : dans le vide, le fantome se tient
     * une hauteur d'yeux sous ce bout, donc exactement sur la ligne des pieds — celle qui part de
     * ceux du joueur et suit son regard. Le saut traversant y arrive aussi, en marchant depuis les
     * pieds.
     *
     * <p>Les autres marques n'y sont pas, et c'est voulu : celles qu'on pose sur la face d'un mur
     * sont decalees de soixante centimetres sur le cote, et celles qu'on pose sur une creature se
     * tiennent sur elle. Celles-la se dessinent comme des points du monde.
     *
     * <p>Fonction pure, donc verifiable : on mesure la distance du point a la droite.
     */
    public static boolean onLine(Vec3 feet, Vec3 look, Vec3 at) {
        Vec3 to = at.subtract(feet);
        double along = to.dot(look);
        if (along <= 0) return false;
        return to.subtract(look.scale(along)).length() <= ON_GAZE;
    }

    /**
     * Ou dessiner la marque, a l'instant de l'image et non a celui du tick.
     *
     * <p>La marque est reposee vingt fois par seconde — c'est le rythme du tick — alors qu'une
     * creature visee, elle, se deplace a chaque image de l'ecran. La dessiner telle quelle la faisait
     * donc avancer par saccades d'un vingtieme de seconde : c'est ce qui se voyait des qu'une
     * creature bougeait, la boite sautant d'un point au suivant au lieu de la suivre.
     *
     * <p>Mais une marque posee sur la ligne du regard ne se glisse pas comme un point du monde : les
     * deux ticks qu'on interpolerait sont pris sur <b>deux regards differents</b>, et le glissement
     * passe par la <b>corde</b> de l'arc — la marque sort de la visee d'un cote, d'autant plus
     * qu'elle est loin. Elle se redessine donc sur la ligne de la <b>camera</b>, la seule qui porte
     * le curseur et connaisse l'orientation de l'image ; seule sa distance s'interpole, celle qui
     * bouge vraiment d'un tick a l'autre.
     */
    @Nullable
    public static Vec3 interpolated(double partialTick, Vec3 camera, Vector3f look) {
        if (position == null) return null;
        if (previous == null) return position;
        if (!line) return previous.lerp(position, partialTick);

        // Les pieds du tireur, vus de la camera : c'est de la que part la ligne.
        Vec3 feet = camera.subtract(0, eyeHeight, 0);
        double distance = previousAlong + (along - previousAlong) * partialTick;
        return feet.add(look.x() * distance, look.y() * distance, look.z() * distance);
    }

    /** L'orientation du tireur, en degres. */
    public static float yaw() {
        return yaw;
    }

    /** La couleur de la marque, ARGB. */
    public static int color() {
        return color;
    }

    /** Sa forme : le fantome du joueur, ou une boite a ses dimensions. */
    public static Shape shape() {
        return shape;
    }

    /** L'age de la marque, en ticks. */
    public static int ageTicks() {
        return ageTicks;
    }

    /**
     * La forme de la marque.
     *
     * <p>Deux formes dans l'original, et deux seulement. Le <b>fantome</b> du joueur, pour les
     * quatre competences qui teleportent le corps : lui seul a une silhouette, et le modele la lui
     * donne — d'ou une taille nulle ici. Et la <b>boite</b> de son {@code EntityMarker}, pour les
     * deux competences qui visent autre chose : la ou l'objet tombera, et la creature a qui il
     * arrachera les chairs. Ses dimensions sont alors celles que la competence demande — un bloc
     * pour du vide, la boite de la creature pour elle.
     */
    public record Shape(double width, double height) {

        /** Le fantome du joueur : c'est le modele qui lui donne sa taille, pas la marque. */
        public static final Shape GHOST = new Shape(0.0, 0.0);

        /** Une boite cubique, comme le {@code marker.width = marker.height} de l'original. */
        public static Shape box(double size) {
            return new Shape(size, size);
        }

        /** Ou aux deux dimensions de la creature visee : sa largeur et sa hauteur. */
        public static Shape box(double width, double height) {
            return new Shape(width, height);
        }

        /** Vrai quand c'est une boite : le rendu sait alors quoi dessiner. */
        public boolean isBox() {
            return width > 0.0 && height > 0.0;
        }
    }

    /** Ou la marque se pose, de quelle couleur, et sous quelle forme. */
    public record Seat(Vec3 position, int color, Shape shape) {
    }

    /**
     * Ou la competence en cours emmenerait son joueur, ou {@code null} s'il n'y a pas de marque.
     *
     * <p>Les trois competences a marque n'en veulent pas au meme moment : la teleportation au
     * marqueur en montre une pendant toute sa charge — sa portee grandit avec elle — le
     * scintillement seulement tant qu'une touche de direction est enfoncee ({@code aimed}, zero
     * quand il n'y en a pas), et le saut traversant tant que sa touche se tient, a la distance que
     * la molette lui a donnee.
     */
    @Nullable
    static Seat seat(Player player, Skill skill, int chargeTicks, int aimed) {
        if (skill == TeleporterCategory.MARK_TELEPORT) {
            return new Seat(TeleporterCategory.MARK_TELEPORT.destination(player,
                    ClientAbilityData.get(), chargeTicks), COLOR_NORMAL, Shape.GHOST);
        }
        if (skill == TeleporterCategory.FLASHING && aimed != 0) {
            return new Seat(TeleporterCategory.FLASHING.destination(player, ClientAbilityData.get(),
                    aimed), COLOR_NORMAL, Shape.GHOST);
        }
        // Le saut court vise pendant tout son maintien, comme les deux precedents : le fantome se
        // pose la ou le saut deposerait son joueur, et c'est la meme fonction qui l'y deposera.
        if (skill == TeleporterCategory.SHIFT_TELEPORT) {
            return new Seat(TeleporterCategory.SHIFT_TELEPORT.destination(player,
                    ClientAbilityData.get()), COLOR_NORMAL, Shape.GHOST);
        }
        // Le lancer d'objet, lui, ne montre pas un fantome mais la BOITE de l'original. Elle a la
        // taille de ce qu'elle designe — un demi-bloc dans le vide, la creature ENTIERE quand il y
        // en a une : ses pieds pour plancher, sa taille pour plafond, et non sa tete. Sa teinte le
        // dit aussi : gris quand rien n'est vise, ORANGE des qu'une creature se trouve sous le
        // geste. Le rouge est a la chair, pas a lui.
        if (skill == TeleporterCategory.THREATENING_TELEPORT) {
            Entity found = TeleporterCategory.THREATENING_TELEPORT.aimed(player,
                    ClientAbilityData.get());
            if (found instanceof LivingEntity living) {
                return new Seat(living.position(), COLOR_HIT_ORANGE,
                        Shape.box(living.getBbWidth(), living.getBbHeight()));
            }
            return new Seat(TeleporterCategory.THREATENING_TELEPORT.dropPosition(player,
                    ClientAbilityData.get()), COLOR_VOID, Shape.box(VOID_BOX));
        }
        // La chair, elle, a ses deux gris et son rouge a elle, et sa boite a la taille de la
        // creature qu'elle trouve — la sienne, sans grossissement : ses pieds pour plancher, son
        // sommet pour plafond, exactement comme celle du lancer d'objet.
        //
        // Dans le vide, elle se pose la ou le REGARD butte, comme le lancer d'objet, et non au
        // bout de la ligne des pieds. L'original prenait la position du joueur — ses pieds — et
        // cette ligne-la passe sous le sol des qu'on baisse les yeux : la boite s'y enfoncait au
        // lieu de rester sous le curseur.
        if (skill == TeleporterCategory.FLESH_RIPPING) {
            Entity found = TeleporterCategory.FLESH_RIPPING.aimed(player, ClientAbilityData.get());
            if (found instanceof LivingEntity living) {
                return new Seat(living.position(), COLOR_FLESH_HIT,
                        Shape.box(living.getBbWidth(), living.getBbHeight()));
            }
            return new Seat(TargetingUtil.findImpactPoint(player,
                    TeleporterCategory.FLESH_RIPPING.range(ClientAbilityData.get())),
                    COLOR_FLESH_IDLE, Shape.box(FLESH_BOX));
        }
        if (skill == TeleporterCategory.PENETRATE_TELEPORT && TeleportAim.active()) {
            PenetrateTeleportSkill.Destination destination =
                    TeleporterCategory.PENETRATE_TELEPORT.destination(player,
                            ClientAbilityData.get(), TeleportAim.distance());
            // Le fantome se pose sur la destination elle-meme : c'est un point aux pieds, et le
            // rendu sait desormais qu'il doit y poser les siens — voir TpMarkRenderer.GHOST_LIFT.
            // L'original ajoutait une hauteur d'yeux ici, ce qui n'etait qu'un rattrapage du meme
            // decalage, fait a moitie : son scintillement, lui, s'en passait.
            return new Seat(destination.position(),
                    destination.available() ? COLOR_NORMAL : COLOR_THREATENING, Shape.GHOST);
        }
        return null;
    }

    /** Les etincelles que la marque semme autour d'elle, une fois par tick sur deux et demie. */
    private static void sowSpark() {
        if (position == null || RANDOM.nextDouble() >= SPARK_CHANCE) return;

        Vec3 at = new Vec3(
                position.x + spread(SPARK_RADIUS),
                position.y + SPARK_LOW + RANDOM.nextDouble() * (SPARK_HIGH - SPARK_LOW),
                position.z + spread(SPARK_RADIUS));
        Vec3 velocity = new Vec3(spread(SPARK_SPEED),
                RANDOM.nextDouble() * SPARK_RISE, spread(SPARK_SPEED));
        TpParticles.spawn(at, velocity);
    }

    /** Un decalage tire dans les deux sens. */
    private static double spread(double bound) {
        return (RANDOM.nextDouble() * 2 - 1) * bound;
    }
}
