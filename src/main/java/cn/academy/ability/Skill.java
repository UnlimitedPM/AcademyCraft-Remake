package cn.academy.ability;

import cn.academy.AcademyCraft;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.condition.ConditionDependency;
import cn.academy.ability.develop.condition.ConditionDeveloperType;
import cn.academy.ability.develop.condition.ConditionLevel;
import cn.academy.ability.develop.condition.LearningCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base unit of an ability category (port of original Skill+Controllable merged).
 * A skill is either "active" (triggered by a key, costs Control Points) or "passive"
 * (always in effect while learned, hooks into gameplay events like onDamaged).
 *
 * <h2>Apprentissage</h2>
 *
 * Une competence se situe a un <b>niveau</b> de sa categorie et porte les
 * conditions a remplir pour l'apprendre. C'est le portage de {@code Skill} de la
 * 1.12.2, qui melangeait deja les deux cotes de la competence — ce qu'elle fait et
 * comment on l'obtient.
 */
public abstract class Skill {

    private final String name;
    private final int level;
    private final List<LearningCondition> conditions = new ArrayList<>();
    private final List<Skill> dependencies = new ArrayList<>();
    private Category category;
    private int id = -1;

    @Nullable
    private Skill parent;

    /**
     * Competence sans niveau : elle n'exige que le niveau 0 de sa categorie.
     *
     * Reserve aux competences qui n'ont pas de place dans une progression — les
     * doublures des tests. Une vraie competence declare son niveau : un test verifie
     * qu'aucune competence enregistree ne reste au niveau 0, sinon une omission se
     * verrait seulement en jeu, une competence offerte d'office.
     */
    protected Skill(String name) {
        this(name, 0);
    }

    protected Skill(String name, int level) {
        this.name = name;
        this.level = level;
        // Comme dans l'original : toute competence exige au moins le niveau ou elle
        // se trouve. Pour une competence de niveau 0, la condition est toujours vraie.
        this.conditions.add(ConditionLevel.INSTANCE);
        // Et une machine d'une qualite suffisante : l'original la posait quand la
        // competence rejoignait sa categorie, le port la pose des la construction — le
        // niveau suffit a la calculer, et la competence n'a plus besoin d'une categorie
        // pour savoir qui peut l'enseigner.
        this.conditions.add(new ConditionDeveloperType(getMinimumDeveloperType()));
    }

    /**
     * La machine minimale qui sait enseigner cette competence.
     *
     * Reprend {@code Skill.getMinimumDeveloperType()} au mot pres : les competences des
     * premiers niveaux s'apprennent avec l'objet portable, celles du niveau 3 demandent la
     * machine normale, et les deux derniers niveaux la machine avancee. Une machine plus
     * avancee sait toujours enseigner ce que sait une machine plus modeste (voir
     * {@link DeveloperType}), donc la regle se lit sans table.
     */
    public DeveloperType getMinimumDeveloperType() {
        if (level <= 2) return DeveloperType.PORTABLE;
        if (level <= 3) return DeveloperType.NORMAL;
        return DeveloperType.ADVANCED;
    }

    final void bind(Category category, int id) {
        this.category = category;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public int getId() {
        return id;
    }

    /** Niveau de la categorie a partir duquel cette competence peut s'apprendre. */
    public int getLevel() {
        return level;
    }

    /**
     * Nombre de stimulations au developpeur pour l'apprendre.
     *
     * Formule reprise telle quelle de l'original : trois stimulations de base, et un
     * demi-carre du niveau en plus. Apprendre une competence de niveau 5 coute donc
     * cinq fois plus cher que celle de niveau 1.
     */
    public int getLearningStims() {
        return (int) (3 + level * level * 0.5f);
    }

    /** Cle de langue du nom affiche, comme {@code ac.ability.<categorie>.<nom>.name}. */
    public String getDisplayKey() {
        String categoryName = category == null ? "" : category.getName();
        return "ac.ability." + categoryName + "." + name + ".name";
    }

    public Component getDisplayName() {
        return Component.translatable(getDisplayKey());
    }

    /**
     * L'icone qui represente la competence : celle de l'arbre et de l'ecran des prereglages.
     *
     * <p>Elle vit avec les autres images du mod, sous le nom de sa categorie puis le sien :
     * {@code textures/abilities/<categorie>/skills/<competence>.png}. C'est le chemin de
     * l'original, et les images sont deja livrees.
     */
    public ResourceLocation getHintIcon() {
        String categoryName = category == null ? "generic" : category.getName();
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/abilities/" + categoryName + "/skills/" + name + ".png");
    }

    // ------------------------------------------------------------------
    // Apprentissage : conditions et dependances
    // ------------------------------------------------------------------

    public void addCondition(LearningCondition condition) {
        conditions.add(condition);
    }

    public List<LearningCondition> getConditions() {
        return List.copyOf(conditions);
    }

    /**
     * Declare qu'une autre competence doit etre apprise avant celle-ci.
     *
     * Reprend {@code addSkillDep} de l'original. Une competence dependante de
     * elle-meme est refusee a la construction : c'est une boucle impossible dont le
     * seul effet serait de rendre la competence inapprenable sans rien dire.
     */
    public void addDependency(Skill dependency) {
        addDependency(dependency, 0f);
    }

    /**
     * Declare une dependance qui exige en plus un seuil d'experience dans la parente.
     *
     * Reprend {@code addSkillDep(skill, exp)} de l'original. Une dependance declaree
     * sans seuil vaut 0, donc « apprise » suffit.
     */
    public void addDependency(Skill dependency, float requiredExp) {
        if (dependency == this) {
            throw new IllegalArgumentException("A skill cannot depend on itself: " + name);
        }
        dependencies.add(dependency);
        conditions.add(new ConditionDependency(dependency, requiredExp));
    }

    public List<Skill> getDependencies() {
        return List.copyOf(dependencies);
    }

    /**
     * La competence dont celle-ci decoule dans l'arbre.
     *
     * Reprend {@code setParent}, qui posait du meme coup une condition : dans
     * l'original, la competence parente devait etre apprise. La parente sert donc a
     * deux choses ici — decider ce qui merite d'etre montre, et bloquer
     * l'apprentissage tant qu'elle n'est pas apprise.
     */
    public void setParent(Skill parent) {
        setParent(parent, 0f);
    }

    /**
     * Meme chose, avec le seuil d'experience que l'original demandait parfois dans la
     * parente.
     *
     * {@code railgun.setParent(thunderBolt, 0.3f)} demandait 30 % d'experience dans le
     * thunder bolt avant de debloquer le railgun : avoir appris la parente ne suffisait
     * pas, il fallait s'en etre servi.
     */
    public void setParent(Skill parent, float requiredExp) {
        if (this.parent != null) {
            throw new IllegalStateException("Parent already set on " + name);
        }
        if (parent == this) {
            throw new IllegalArgumentException("A skill cannot be its own parent: " + name);
        }
        this.parent = parent;
        addDependency(parent, requiredExp);
    }

    @Nullable
    public Skill getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isPassive() {
        return false;
    }

    /**
     * Cette competence peut-elle etre rangee sur une touche d'aptitude ?
     *
     * <p>Portage de {@code canControl} de l'original. Une competence passive s'apprend et agit
     * toute seule : la proposer dans l'editeur de prereglaGes laisserait croire qu'elle se
     * lance, et le joueur la chercherait en vain sur le rappel des touches. L'original ne la
     * montrait pas non plus.
     */
    public boolean canControl() {
        return true;
    }

    // ------------------------------------------------------------------
    // Bonus permanents, pour les competences passives qui en donnent
    // ------------------------------------------------------------------

    /**
     * Plafond de reserve ajoute tant que cette competence est apprise.
     *
     * <p>Portage de {@code CalcEvent.MaxCP} : chez l'original, chaque competence passive
     * s'inscrivait au bus d'evenements et ajoutait sa part au moment du calcul. Le port lit la
     * meme chose en interrogeant les competences apprises (voir
     * {@code AbilityData.getPassiveMaxControlPoint}) — le resultat est identique, sans bus.
     */
    public float getMaxControlPointBonus(AbilityData data) {
        return 0f;
    }

    /** Plafond de surcout ajoute tant que cette competence est apprise. */
    public float getMaxOverloadBonus(AbilityData data) {
        return 0f;
    }

    /**
     * Facteur applique a la recuperation de la reserve, tant que cette competence est apprise.
     *
     * <p>Les facteurs se <b>multiplient</b> entre eux, comme des pourcentages qui s'ajoutent
     * l'un a l'autre : c'est ce que faisait l'original, ou l'entrainement mental multipliait la
     * vitesse de recuperation.
     */
    public float getControlPointRecoverScale(AbilityData data) {
        return 1f;
    }

    /**
     * L'experience de cette competence se calcule au lieu de s'apprendre.
     *
     * <p>Portage de {@code expCustomized} de l'original : une competence peut tirer son
     * experience d'un etat du joueur — l'intensification par radiation la tire de sa reserve —
     * au lieu de la gagner en s'en servant. Elle n'a alors pas d'avancement a elle, et
     * {@code AbilityData.getSkillExp} appelle {@link #computeExp} a chaque lecture.
     */
    public boolean hasComputedExp() {
        return false;
    }

    /** L'experience calculee, quand {@link #hasComputedExp()} est vrai. */
    public float computeExp(AbilityData data) {
        return 0f;
    }

    // ------------------------------------------------------------------
    // Experience d'usage
    // ------------------------------------------------------------------

    /**
     * Experience gagnee en utilisant cette competence.
     *
     * Chaque competence de l'original appelait {@code ctx.addSkillExp(...)} avec son
     * propre montant, au moment ou son effet aboutissait : un coup porte, un bouclier
     * qui encaisse, une distance de teleportation. Le paquet d'activation ne peut pas
     * savoir tout cela, donc la valeur est declaree ici et versee a l'activation (voir
     * {@code ActivateSkillPacket}), au montant de base de l'original. Les competences
     * passives, qui ne s'activent pas, ont le leur verse depuis leur propre crochet.
     *
     * Zero signifie « pas encore porte » : un test verifie qu'aucune competence livree
     * n'est restee a zero, sinon elle rapporterait silencieusement une progression nulle
     * et le niveau serait inatteignable.
     */
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /**
     * Cette competence verse-t-elle son experience elle-meme, depuis son effet ?
     *
     * Par defaut non : le paquet d'activation verse {@link #getExpGain} et l'effet peut
     * ajouter une part. Quelques competences n'ont rien a declarer la — la teleportation
     * au marqueur se paie au bloc parcouru, et le paquet ne connait pas la distance — et
     * le disent ici plutot que de laisser croire a un oubli.
     */
    public boolean earnsExpOnEffect() {
        return false;
    }

    /**
     * Cette competence paie-t-elle son cout elle-meme, depuis son effet ?
     *
     * <p>Par defaut non : le paquet d'activation paie les deux ressources avant d'appeler
     * l'effet, ce qui suffit quand le prix ne depend pas de ce que l'effet trouve. Mais
     * certaines competences doivent pouvoir <b>ne rien payer du tout</b> — l'onde de choc
     * ne facture rien a un joueur en l'air, le retour de sang rien a qui ne touche personne
     * — et le paquet, lui, ne le sait pas encore au moment ou il paie.
     *
     * <p>Quand cette methode rend vrai, le paquet saute le paiement et la verification de
     * reserve : c'est l'effet qui appelle {@code data.perform(...)} lui-meme. Les couts
     * declares restent ceux de l'original — ils disent ce que l'effet paiera, et un test
     * les fige — mais l'ordre, lui, appartient a la competence.
     */
    public boolean paysOnEffect() {
        return false;
    }

    /**
     * Multiplicateur d'experience, porte par la competence.
     *
     * L'original en faisait un reglage par competence ({@code exp_incr_speed}, defaut
     * 1.0). La config du port est plate et n'a pas cette arborescence, donc elle est
     * ici sans reglage : le crochet existe pour le jour ou elle l'aura.
     */
    public float getExpIncrSpeed() {
        return 1f;
    }

    /**
     * Duree de la recharge de cette competence, en ticks. 0 = aucune.
     *
     * L'original appelait {@code ctx.setCooldown(...)} apres l'effet, avec une courbe
     * qui dependait de l'experience (15 a 5 ticks pour un arc, 300 a 160 pour le
     * railgun). Le port la declare ici et la pose a l'activation, pour que toutes les
     * competences suivent la meme regle : une competence qui part consomme sa recharge,
     * meme si sa cible lui echappe.
     *
     * Deux competences n'ont pas la leur : celles dont la recharge de l'original
     * dependait du temps de charge du tir (le bouclier et le meltdowner) attendent que
     * ce temps de charge soit porte.
     */
    public int getCooldownTicks(AbilityData data) {
        return 0;
    }

    /**
     * Surcout demande par une activation.
     *
     * Portage de la seconde ressource de l'original ({@code ctx.consume(overload, cp)}) :
     * en plus des CP, chaque competence charge une reserve qui se remplit et met le
     * joueur en surcharge quand elle est pleine. Contrairement aux couts en CP de
     * l'original, ceux du surcout tiennent dans la meme echelle que le port, donc ce
     * sont les vrais chiffres de la 1.12.2.
     */
    public float getOverloadCost(AbilityData data) {
        return 0f;
    }

    // ------------------------------------------------------------------
    // Temps de charge
    // ------------------------------------------------------------------

    /**
     * Cette competence demande-t-elle de garder la touche enfoncee ?
     *
     * L'original avait deux familles : celles qui partent a l'appui (l'arc, le
     * teleport), et celles qui se chargent (le meltdowner, l'acceleration de vecteur).
     * Une competence qui se charge est executee au relachement, avec le temps qu'elle a
     * accumule, que ses degats comme sa recharge peuvent lire via
     * {@code data.getChargeTicks(this)}.
     */
    public boolean isChargeable() {
        return false;
    }

    /** Nombre de ticks de charge au-dela duquel la competence ne gagne plus rien. */
    public int getMaxChargeTicks(AbilityData data) {
        return 0;
    }

    /**
     * Cette competence frappe-t-elle <b>toute seule</b> au bout de sa charge maximale ?
     *
     * <p>Une seule competence de l'original le faisait : l'orage. Son {@code MSG_TICK} envoyait
     * sa fin des que {@code ticks >= MAX_TICKS}, donc la foudre tombait sans qu'on relache la
     * touche. Toutes les autres attendaient le relachement — le meltdowner compris, dont la
     * charge ne se terminait que par {@code MSG_KEYUP}, ou par la limite de securite de cent
     * ticks.
     *
     * <p>Le port, lui, faisait frapper <b>toutes</b> les competences chargees a leur maximum, en
     * une seule boucle ecrite pour l'orage. Le joueur a vu ce que cela donne sur la premiere
     * qu'il a essayee : « le laser part sans que j'aie a relacher la touche ». Le drapeau dit
     * donc qui a le droit de partir tout seul, et il ne repond vrai que la ou l'original le
     * faisait.
     */
    public boolean firesAtMaxCharge() {
        return false;
    }

    /**
     * Ouverture, avant le premier tick de charge.
     *
     * C'est le {@code MSG_MADEALIVE} de l'original, le meme pour les deux familles qui
     * gardent la touche enfoncee. Le meltdowner y epingle le surcout qu'il ne laissera
     * pas redescendre pendant qu'il charge.
     */
    public void onStart(Player player, AbilityData data) {
    }

    /**
     * Cette competence peut-elle commencer avec ce que le joueur vise ?
     *
     * Pour les competences qui gardent la touche enfoncee et qui ont besoin d'une cible
     * precises : l'original appelait son {@code terminate()} dans le {@code MSG_MADEALIVE}
     * quand il n'y avait rien a viser, donc avant d'avoir rien facture. Le port verifie
     * donc avant de payer, et le refus ne laisse ni surcout ni recharge derriere lui.
     */
    public boolean canStart(Player player, AbilityData data) {
        return true;
    }

    /**
     * La competence a-t-elle, dans sa main, ce qu'il lui faut pour s'ouvrir ?
     *
     * <p>C'est l'{@code isHandValid} de l'original, et c'est <b>la seule</b> question qu'une
     * {@link #canStart} puisse poser au joueur sans regarder le monde. Le client s'en sert pour
     * refuser tout de suite un appui que le serveur refusera — voir {@code HoldRefusal} —, et il ne
     * peut pas se tromper : une main est une main, il la voit comme le serveur.
     *
     * <p>C'est justement pourquoi elle est posee <b>a part</b> de {@code canStart} : certaines
     * competences y regardent le monde, et le monde du client est en retard d'un tick — un refus lu
     * la-bas serait peut-etre faux, ce qui est bien pire qu'un scintillement.
     *
     * <p>Par defaut, la competence ne demande rien : elle s'ouvre les mains vides.
     */
    public boolean isHandValid(Player player) {
        return true;
    }

    /**
     * Un tick de charge, tant que la touche reste enfoncee.
     *
     * Retourner {@code false} abandonne la charge : rien n'est lance, rien n'est
     * facture, comme le {@code terminate()} que l'original appelait quand la reserve
     * etait vide. C'est par la que passe l'entretien d'une competence qui se charge :
     * le meltdowner et le thunder clap paient leurs points par tick.
     */
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        return true;
    }

    /**
     * Nombre de ticks de charge en dessous duquel la competence ne part pas.
     *
     * Reprend {@code TICKS_MIN} du meltdowner de l'original : relacher trop tot ne
     * declenche rien du tout, et ne coute donc rien.
     */
    public int getMinChargeTicks(AbilityData data) {
        return 0;
    }

    /**
     * Execution apres une charge.
     *
     * Par defaut, l'activation ordinaire : une competence qui ne se charge pas n'a pas
     * a connaitre cette methode.
     */
    public void onActivateCharged(Player player, AbilityData data, int chargeTicks) {
        onActivate(player, data);
    }

    // ------------------------------------------------------------------
    // Competences tenues
    // ------------------------------------------------------------------

    /**
     * Cette competence agit-elle tant que la touche reste enfoncee ?
     *
     * Troisieme famille de l'original, a cote des instantanees et des chargees : le
     * bouclier ne se lance pas au relachement, il vit pendant tout le maintien et
     * s'entretient par tick. Le compteur de ticks tenus est celui du maintien, et il
     * reste lisible apres la fin, donc la recharge peut en dependre.
     */
    public boolean isHeld() {
        return false;
    }

    /**
     * Cette competence s'ACTIVE et se DESACTIVE-t-elle, plutot que de se tenir ?
     *
     * <p>Un appui l'ouvre, un second la ferme : son maintien est un <b>etat</b>, et il ne doit rien
     * a la touche. C'est la quatrieme famille de l'original, et elle n'existe que par ce qu'il
     * faisait de ses touches : son gestionnaire d'activation <b>terminait</b> le contexte ouvert,
     * la ou les autres attendaient le relachement.
     *
     * <p>Trois competences de vecmanip sont dans ce cas — la deviation, le renvoi et les ailes de
     * tempete —, et le joueur a demande la meme chose que chez l'original : « il faut juste
     * appuyer une fois pour activer / desactiver et c'est tout ».
     *
     * <p>Elle suppose {@link #isHeld()} : c'est le meme maintien, ouverte et fermee autrement.
     */
    public boolean isToggle() {
        return false;
    }

    /** Duree maximale du maintien ; 0 = illimite, borne par les ressources. */
    public int getMaxHoldTicks(AbilityData data) {
        return 0;
    }

    /**
     * Un tick de maintien.
     *
     * Retourner {@code false} termine la competence : c'est ainsi qu'une competence
     * tenue s'arrete quand ses ressources s'epuisent, comme le faisait le context de
     * l'original en appelant {@code terminate()}.
     */
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        return true;
    }

    /**
     * Ce que ce tick de maintien — ou de charge — paiera en reserve.
     *
     * <p>C'est le chiffre que le serveur paie, et c'est <b>le meme</b> que le client rejoue pour
     * l'afficher : les points de controle du menu F4 descendent alors d'un tick a l'autre, au lieu
     * de sauter de synchronisation en synchronisation — le serveur n'envoie son etat que tous les
     * quatre ticks pendant un maintien, donc le joueur voyait sa reserve tomber par paquets. Ses
     * mots : « je vois mes cp diminuer de 20 en 20 par secondes, alors que normalement ca devrait
     * faire un affichage plus joli ou on voit les nombres defiler, comme avec la recharge des cp ».
     * Voir {@code ClientAbilityData}.
     *
     * <p>Une competence qui s'entretient par tick le dit donc <b>ici</b>, et s'en sert pour payer :
     * une seule expression les tient ensemble, et les deux cotes ne peuvent pas diverger. Le defaut
     * est zero — une competence qui ne paie qu'a l'ouverture ou a l'effet n'a rien a dire.
     *
     * <p>{@code ticks} est l'age du maintien (ou de la charge), comme dans {@link #onHoldTick} :
     * c'est ce qui permet a une competence dont l'entretien s'arrete en route — les billes de la
     * bombe a fragmentation, les quarante premiers ticks du claquement d'orage — de le dire.
     */
    public float getTickUpkeep(AbilityData data, int ticks) {
        return 0f;
    }

    /**
     * Ce que ce tick de maintien — ou de charge — ajoutera au <b>surcout</b>, comme {@link
     * #getTickUpkeep} le dit pour la reserve.
     *
     * <p>Meme raison, et meme remede : les ailes de tempete ajoutent leur surcout a chaque tick de
     * vol, et le serveur ne l'envoie que tous les quatre ticks — le joueur voyait donc sa barre
     * monter « d'un certain nombre a chaque fois plutot que d'avoir un beau defilement comme pour
     * les CP ». Ce qui monte au tick se rejoue au tick, et la synchronisation suivante reecrit la
     * valeur vraie.
     *
     * <p>A ne pas confondre avec la <b>descente</b> du surcout : pendant un maintien, le serveur
     * epingle la part qu'il a posee a l'ouverture et ne la fait pas redescendre — c'est le client
     * qui rejoue la montee ({@code ClientAbilityData}), et personne ne rejoue la descente.
     *
     * <p>Defaut : rien, et une seule competence en a besoin ({@code storm_wing}).
     */
    public float getTickUpkeepOverload(AbilityData data, int ticks) {
        return 0f;
    }

    /**
     * Le joueur a relache la touche : l'effet doit-il continuer tout seul ?
     *
     * <p>C'est le quatrieme cas de l'original, et il n'y en a qu'un : le {@code jet_engine}
     * se vise pendant le maintien, se paie au relachement, et <b>vole ensuite pendant une
     * seconde</b>. Le contexte de l'original survivait donc a son propre
     * {@code MSG_MARK_END} ; il ne mourait qu'une fois le vol fini.
     *
     * <p>Rendre {@code true} garde le maintien ouvert : {@link #onHoldTick} continue d'etre
     * appele, et c'est lui qui terminera l'effet en rendant {@code false} a son tour — ce
     * qui passe alors par la fin ordinaire, donc par la recharge. Rendre {@code false}
     * (le defaut) termine le maintien sur-le-champ, comme pour toutes les autres
     * competences tenues.
     *
     * <p>A ne pas confondre avec {@link #onHoldEnd}, qui est la fin — celle-ci arrive plus
     * tard, et une seule fois, alors que ce crochet-la repond au geste du joueur.
     */
    public boolean onRelease(Player player, AbilityData data, int heldTicks) {
        return false;
    }

    /**
     * Un tick de maintien, cote client.
     *
     * <p>La seule forme de l'original qui s'execute chez le joueur : le contexte de la 1.12.2
     * vivait des deux cotes, et son {@code MSG_TICK} client poussait le joueur lui-meme —
     * c'est ainsi que les ailes de tempete volent, en posant la vitesse a chaque tick au lieu
     * de la demander au serveur.
     *
     * <p>{@code local} est ce que le joueur demande, dans son <b>propre repere</b> : +X sa gauche,
     * +Y son haut, +Z son avant, chaque composante valant -1, 0 ou 1 selon les touches tenues. La
     * competence en fait ce qu'elle veut — les ailes de tempete la tournent avec le regard — et
     * rien de tout cela ne part au serveur : il compte ses propres ticks et facture le vol de son
     * cote.
     *
     * <p>Defaut : rien. Seule competence concernee : {@code storm_wing}.
     */
    public void onClientHoldTick(Player player, AbilityData data, int heldTicks, Vec3 local) {
    }

    /**
     * Le relachement, cote client : le moment ou l'original appliquait son effet LUI-MEME.
     *
     * <p>Le contexte de la 1.12.2 vivait des deux cotes, et c'est le <b>client</b> qui posait la
     * vitesse de l'acceleration de vecteur ({@code VecAccelContext.l_perform} : {@code
     * VecUtils.setMotion(player, initSpeed())}) avant de prevenir le serveur, qui se contentait de
     * consommer et de remettre la chute a zero. Le port avait tout mis chez le serveur, et cela se
     * paie : la vitesse fait alors l'aller-retour par un paquet, et elle peut se faire <b>avaler
     * par le tick du joueur</b> qui l'attend — un saut pile a ce moment-la
     * ({@code jumpFromGround} ecrit 0,42 en Y et le paquet arrive avant le tick, pas apres). Le
     * joueur : « si je saute pile poil au bon moment ca annule la competence [...] je n'ai pas de
     * boost ».
     *
     * <p>Appele au relachement, chez le client, avant que la charge ne se referme, et seulement
     * quand elle a tenu assez longtemps pour que le serveur l'execute. {@code heldTicks} est l'age
     * de la charge <b>du client</b> : c'est celui que l'original lisait pour sa vitesse.
     *
     * <p>Ce crochet vient <b>apres</b> le tick du joueur — Forge tire la fin du tick client une fois
     * le monde avance —, donc ce qu'il pose n'est pas ecrase dans la foulee : c'est ce qui rend le
     * geste fiable. Le serveur garde le sien pour les autres et pour le multijoueur.
     *
     * <p>Defaut : rien. Seule competence concernee : {@code vec_accel}.
     */
    public void onClientRelease(Player player, AbilityData data, int heldTicks) {
    }

    /**
     * Le coup, AVANT qu'il ne soit porte — et refuse entierement s'il l'est.
     *
     * <p>{@code LivingAttackEvent} est le tout premier crochet de {@code LivingEntity.hurt} :
     * l'annuler fait sortir la methode sur-le-champ, donc <b>sans degats, sans recul et sans
     * rouge</b>. Le commentaire de l'original dit pourquoi il en avait besoin : « annuler
     * l'evenement de degats provoque quand meme le recul, donc il faut un test avant ». Son renvoi
     * de vecteur s'inscrivait donc la, testait si le coup etait absorbe en entier, l'annulait, et
     * portait le coup lui-meme.
     *
     * <p>Rendre {@code true} consomme le coup : l'evenement est annule et {@link #onDamaged} ne
     * sera meme pas appele. A n'utiliser que pour un refus <b>total</b> — une reduction partielle
     * se fait dans {@code onDamaged}, le seul chemin ou le jeu a deja calcule ses degats.
     *
     * <p>Defaut : rien. Seule competence concernee : {@code vec_reflection}.
     */
    public boolean onAttacked(Player player, AbilityData data, LivingAttackEvent event) {
        return false;
    }

    /**
     * Une action <b>pendant</b> un maintien, venue d'une touche qui n'est pas la sienne.
     *
     * <p>Une seule competence du port s'en sert : le {@code flashing} ecoute les quatre
     * touches de deplacement, et chacune est un saut. L'action est un nombre — la direction
     * — que la competence interprete ; le reste du port n'en recoit jamais.
     *
     * <p>C'est la cinquieme forme de l'original, apres les instantanees, les chargees, les
     * tenues et l'effet qui survit au relachement : un maintien qui attend des ordres.
     */
    public void onHoldAction(Player player, AbilityData data, int action) {
    }

    /**
     * Cette competence ecoute-t-elle les quatre touches de deplacement pendant son maintien ?
     *
     * <p>Dit au client de guetter les touches du jeu, et a elles seules. Le port n'a qu'un
     * cas — le scintillement — mais le drapeau evite au client de connaitre les competences
     * par leur nom.
     */
    public boolean listensToDirections() {
        return false;
    }

    /**
     * Cette competence ouvre-t-elle un ecran au lieu de partir ?
     *
     * <p>Une seule le fait, la teleportation a la marque : sa touche ouvre la liste des
     * endroits marques, et c'est un clic dans cette liste qui declenche le saut. Le client
     * le lit pour savoir qu'il ne doit <b>rien</b> envoyer a l'appui — sans quoi le serveur
     * recevrait une activation pour une competence qui ne s'active pas.
     */
    public boolean opensScreen() {
        return false;
    }

    /**
     * Fin du maintien, quelle qu'en soit la cause : relachement, duree maximale, ou
     * ressources epuisees. L'original le faisait une seule fois, dans
     * {@code MSG_TERMINATED}.
     */
    public void onHoldEnd(Player player, AbilityData data, int heldTicks) {
    }

    /**
     * Interpolation lineaire entre deux valeurs, {@code t} ramene entre 0 et 1.
     *
     * Portage de {@code MathUtils.lerpf} : l'original s'en servait partout pour faire
     * grandir la puissance d'une competence avec son experience. Le port ne s'en sert
     * pour l'instant que pour l'experience elle-meme ; les courbes de puissance par
     * competence viendront s'y brancher.
     */
    protected static float lerp(float from, float to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return from + (to - from) * clamped;
    }

    /** Control Points consumed each time this skill is activated. */
    public float getCpCost() {
        return 0f;
    }

    /**
     * Cout en CP, pour les competences dont le cout suit l'experience.
     *
     * Les competences portees jusqu'ici ont un cout fixe, d'ou le {@link #getCpCost()}
     * sans donnee ; l'original faisait pourtant varier celui du lancer d'objet de 35 a
     * 100. Par defaut, les deux disent la meme chose.
     */
    public float getCpCost(AbilityData data) {
        return getCpCost();
    }

    /** Called server-side when the player triggers this skill's key. */
    public void onActivate(Player player, AbilityData data) {}

    /**
     * Called server-side for every passive skill of every learned category when the
     * owning player takes damage. Return the (possibly modified) damage amount.
     */
    public float onDamaged(Player player, AbilityData data, LivingHurtEvent event) {
        return event.getAmount();
    }

    /**
     * Applique le multiplicateur global de degats ({@code general.damageScale}) a
     * une valeur de degats de base. Les competences actives doivent passer par ici
     * plutot que d'appeler {@code hurt()} avec leur constante brute, afin que le
     * reglage de config reste effectif partout.
     */
    protected static float scaled(float baseDamage) {
        return baseDamage * (float) cn.academy.Config.damageScale;
    }
}
