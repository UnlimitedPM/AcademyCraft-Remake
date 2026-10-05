package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.ability.client.md.MineRayEffect;
import cn.academy.ability.client.tp.TeleportAim;
import cn.academy.ability.client.tp.TeleportMark;
import cn.academy.ability.network.TeleportDistancePacket;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.ActivateSkillPacket;
import cn.academy.ability.network.ActivateSkillPacket.Phase;
import cn.academy.ability.network.FlashingPacket;
import cn.academy.ability.teleporter.PenetrateTeleportSkill;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, value = Dist.CLIENT)
public class AbilityClientEvents {

    /**
     * Une touche d'aptitude, et le rang qu'elle occupe dans le prereglage.
     *
     * <p>Les quatre touches ne declenchent rien par elles-memes : c'est le prereglage en
     * service qui dit ce qu'elles allument, et il peut changer d'un instant a l'autre. La
     * competence est donc relue a chaque appui — c'est le cablage complet de l'original.
     */
    private static final class Binding {

        final KeyMapping key;
        final int presetSlot;

        /** Vrai entre l'appui et le relachement d'une competence qui se charge ou se tient. */
        boolean charging;

        /** La touche etait-elle enfoncee au tick precedent ? Voir {@code tick}. */
        boolean wasDown;

        Binding(KeyMapping key, int presetSlot) {
            this.key = key;
            this.presetSlot = presetSlot;
        }
    }

    /**
     * LES QUATRE TOUCHES D'APTITUDE, ET RIEN D'AUTRE.
     *
     * <p>Le port avait fini par lier chaque competence a sa propre touche — trente-cinq
     * liaisons, jusqu'a la ponctuation et aux touches de defilement — parce que c'etait le
     * chemin le plus court. C'est le systeme de presets de l'original qui les remplace :
     * quatre touches, et le joueur choisit dans son prereglage quelle competence va dessus.
     */
    private static final List<Binding> BINDINGS = List.of(
            new Binding(AbilityKeyBindings.ABILITY_1, 0),
            new Binding(AbilityKeyBindings.ABILITY_2, 1),
            new Binding(AbilityKeyBindings.ABILITY_3, 2),
            new Binding(AbilityKeyBindings.ABILITY_4, 3));

    /**
     * La competence prend le pas sur l'action de base du clic.
     *
     * <p>Dans l'original, appuyer sur une touche d'aptitude ne faisait QUE la competence :
     * le coup d'epee et l'utilisation de l'objet n'avaient pas lieu. Le port, lui, laissait
     * faire les deux, parce que sa touche d'aptitude n'est qu'une liaison de plus posee sur
     * le meme bouton de souris. C'est ici que la chose se decide, et nulle part ailleurs :
     * le jeu demande s'il peut lancer son action, et on lui repond non.
     *
     * <p>La comparaison porte sur la <b>touche physique</b> et non sur le nom de l'action :
     * elle vaut donc aussi pour un joueur qui a deplace ses touches. Et elle ne s'applique
     * que si l'aptitude est allumee et qu'une competence occupe ce rang du prereglage —
     * sinon le clic reste un clic.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() && !event.isUseItem()) return;
        if (skillOnSameKey(event.getKeyMapping()) == null) return;

        // Rien d'autre que la competence : le bras ne frappe pas, et l'objet ne part pas.
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    /** La competence qui occupe la meme touche que cette action de base, s'il y en a une. */
    @Nullable
    private static Skill skillOnSameKey(KeyMapping base) {
        if (!ClientAbilityData.get().isActivated()) return null;
        for (Binding binding : BINDINGS) {
            if (binding.key.getKey().equals(base.getKey())) return skillOf(binding);
        }
        return null;
    }

    /**
     * Les touches de deplacement, quand une competence les prend.
     *
     * <p>C'est ici, et nulle part ailleurs, que la marche se refuse : le jeu vient de lire son
     * clavier et de remplir cet objet, et il va s'en servir a l'instant pour deplacer le joueur.
     * Mettre ses quatre directions a zero juste avant lui prend donc les touches pour de bon,
     * sans jamais toucher aux touches elles-memes.
     *
     * <p>C'est ce qu'il faut faire plutot que de relacher la touche du jeu : le systeme repete
     * une touche tenue une trentaine de fois par seconde, apres le delai de repetition — une
     * demi-seconde a peine —, et le jeu empile ces repetitions comme des appuis. Une touche
     * relachee de force se rallume donc toute seule, et le joueur se remettait a marcher au bout
     * d'une demi-seconde : c'est ce qu'il a vu. Ici la touche reste celle du jeu — l'original la
     * lit pour viser — et seule la marche est refusee. Voir {@link #tickDirections}.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!directionsTaken) return;
        var player = minecraftPlayer();
        if (player == null || event.getEntity() != player) return;

        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // La reserve et le surcout remontent d'un tick, chez le client aussi : la
        // synchronisation du serveur ne passe que tous les dix ticks, et sans ce rejeu les
        // nombres du F4 et les deux barres du temoin de CP avanceraient par bonds. Un
        // maintien en cours fige la part de surcout qu'il epingle, comme chez le serveur.
        // Voir ClientAbilityData.tick.
        ClientAbilityData.tick(ClientCharge.getSkill() != null);
        // Et les notifications du mod vieillissent d'un tick, comme tout le reste du HUD.
        cn.academy.client.hud.NotificationHud.tick();
        // Le renfort du corps a besoin du meme crochet : son onde s'egrene sur huit ticks, et ses
        // arcs d'ecran scintillent a chaque tick — voir BodyIntensifyEffect.
        BodyIntensifyEffect.tick();
        // Et le bloc tenu par la manipulation magnetique gresille tout le temps qu'il vit :
        // l'essaim se reensemence a chaque tick, voir MagManipEffect.
        MagManipEffect.tick();
        // Les rayons du plasma et leurs etincelles : leur forme se lit en millisecondes et se
        // dessine a chaque image, mais c'est bien au tick qu'ils meurent et que les etincelles
        // avancent — voir MdRays et MdSparks.
        //
        // Quitter un monde les emporte : ils appartiennent au monde, pas au jeu — et un rayon
        // TENU ne meurt jamais tout seul, lui. Voir MineRayEffect.
        if (net.minecraft.client.Minecraft.getInstance().level == null) {
            cn.academy.ability.client.md.MdRays.clear();
            cn.academy.ability.client.md.MdSparks.clear();
            MineRayEffect.end();
            TeleportMark.end();
            TeleportAim.end();
            cn.academy.ability.client.tp.TpParticles.clear();
            // Le sang de la chair arrachee appartient au monde lui aussi : il ne vit que dix
            // ticks, mais un monde quitte entre-temps en garderait les taches a l'ecran pour
            // rien. Voir BloodSplashes.
            cn.academy.ability.client.tp.BloodSplashes.clear();
        }
        cn.academy.ability.client.md.MdRays.tick();
        cn.academy.ability.client.md.MdSparks.tick();
        // Les etincelles de la teleportation vieillissent au meme rythme, et leur marque se
        // repose au tick suivant — voir TeleportMark et TpParticles.
        cn.academy.ability.client.tp.TpParticles.tick();
        cn.academy.client.SilbarnFrags.tick();
        // Et le gresillement du bouclier de lumiere : son disque se dessine a chaque image, mais
        // son essaim se seme au tick, chez son porteur seul — voir ShieldSparks.
        ShieldSparks.tick();
        JetEngineEffect.tick();
        // Et la fumee du plasma sur les cibles marquees par la radiation : c'est tout ce que le
        // passif montre, et le serveur l'annonce a ceux qui les voient — voir
        // RadiationMarksEffect.
        cn.academy.ability.client.md.RadiationMarksEffect.tick();

        for (Binding binding : BINDINGS) {
            tick(binding);
        }

        tickPresetKeys();

        // L'allumage et l'extinction de l'aptitude : le serveur decide, et renvoie l'etat
        // complet. Rien ne part tant qu'elle est eteinte, et le HUD reste cache.
        if (AbilityKeyBindings.TOGGLE_ABILITY.consumeClick()) {
            AbilityNetwork.CHANNEL.sendToServer(new cn.academy.ability.network.ToggleAbilityPacket(
                    !ClientAbilityData.get().isActivatedRaw()));
        }

        // L'ecran de debogage du mod, comme avant : F4 fait le tour de ses trois etats. C'est
        // la seule fenetre ou les points de controle et la surcharge se lisent.
        if (AbilityKeyBindings.DEBUG_CONSOLE.consumeClick()) {
            DebugConsole.cycle();
        }

        // Les boucles sonores des maintiens, une seule a la fois : le port retient la
        // competence en cours, et le suivi du joueur se fait tout seul. Appele apres les
        // touches, pour que le relachement coupe la boucle au meme tick.
        cn.academy.sound.client.LoopSounds.tick(
                net.minecraft.client.Minecraft.getInstance().player, ClientCharge.getSkill());
    }

    /**
     * La competence d'une touche.
     *
     * <p>Une touche fixe la tient de son cablage ; une touche d'aptitude la demande au
     * prereglage en service, qui peut avoir change depuis le dernier tick. Le nom est
     * cherche dans les quatre categories parce que le prereglage ne retient que lui — comme
     * partout dans le port.
     */
    @Nullable
    private static Skill skillOf(Binding binding) {
        return skillByName(cn.academy.ability.preset.client.ClientPresetData
                .skillAt(binding.presetSlot));
    }

    /** Une competence par son nom, ou {@code null} si aucune categorie ne la porte. */
    @Nullable
    private static Skill skillByName(@Nullable String name) {
        if (name == null) return null;
        for (Category category : CategoryManager.INSTANCE.getCategories()) {
            Skill skill = category.getSkill(name);
            if (skill != null) return skill;
        }
        return null;
    }

    /**
     * Ce que les deux touches de prereglage declenchent.
     *
     * <p>Le changement est demande au serveur, qui renvoie ensuite l'etat complet : le
     * client ne modifie jamais son exemplaire, il n'a donc aucun moyen de diverger. La
     * condition de l'original — n'y toucher que si l'aptitude est eveillee — n'est pas
     * reprise : le port n'a pas d'etat « eveille », et une touche qui ne fait rien sans
     * rien dire serait pire que le contraire.
     */
    private static void tickPresetKeys() {
        if (minecraftPlayer() == null) return;

        if (AbilityKeyBindings.PRESET_NEXT.consumeClick()) {
            var data = cn.academy.ability.preset.client.ClientPresetData.get();
            AbilityNetwork.CHANNEL.sendToServer(
                    cn.academy.ability.preset.network.PresetActionPacket
                            .switchTo(data.getCurrentId() + 1));
        }

        if (AbilityKeyBindings.PRESET_EDIT.consumeClick()) {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new cn.academy.ability.preset.client.PresetEditScreen());
        }
    }

    /**
     * Le serveur dit que ce maintien n'existe plus : le client ferme le sien.
     *
     * <p>Il est appele par {@code HoldOverPacket}, dans deux cas qui n'en font qu'un pour le
     * joueur : le serveur a <b>refuse</b> l'ouverture (aptitude eteinte, brouillage, recharge,
     * reserve trop juste, rien a viser) ou il a <b>termine</b> un maintien deja ouvert (sa reserve
     * s'est videe, sa duree maximale est atteinte).
     *
     * <p>La touche est peut-etre encore enfoncee : c'est le serveur qui a fini, pas le joueur.
     * On ferme donc exactement ce que le relachement fermerait — le temoin, l'orage, la gerbe du
     * renfort, les directions du scintillement — mais <b>sans rien renvoyer</b> : le serveur a
     * deja tout termine de son cote, et lui repondre relancerait un maintien pour rien. Le
     * cablage de la touche retombe aussi, sans quoi le prochain relachement enverrait une
     * demande de fin pour un maintien qui n'existe plus.
     */
    public static void onHoldOver(String skillName) {
        if (skillName == null || !skillName.equals(ClientCharge.getSkill())) return;

        for (Binding binding : BINDINGS) {
            Skill skill = skillOf(binding);
            if (skill != null && skillName.equals(skill.getName())) binding.charging = false;
        }

        ClientCharge.end();
        ThunderClapEffect.end();
        BodyIntensifyEffect.endCharge(false);
        MineRayEffect.end(skillName);
        TeleportMark.end();
        TeleportAim.end();
        endDirections();
    }

    @Nullable
    private static net.minecraft.client.player.LocalPlayer minecraftPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }

    /**
     * La main du joueur a-t-elle ce que la competence demande ?
     *
     * <p>C'est le seul refus d'ouverture que le client lise sur le <b>joueur</b> lui-meme plutot
     * que sur ses nombres — le depose au loin veut un bloc, le lancer d'objet veut quelque chose —,
     * et il ne peut pas se tromper : une main est une main, il la voit comme le serveur. Sans lui,
     * un appui les mains vides ouvrait le maintien pour rien, et le joueur en voyait le debut
     * scintiller avant que le serveur ne le referme. Voir {@code Skill#isHandValid}.
     *
     * <p>Un joueur qu'on ne connait pas laisse passer : le client ne refuse que ce dont il est sur.
     */
    private static boolean handOk(Skill skill) {
        var player = minecraftPlayer();
        return player == null || skill.isHandValid(player);
    }

    /** La direction visee par le clavier, ou 0 : le scintillement saute au relachement. */
    private static int aimed;

    /** L'etat des quatre touches au tick precedent, pour guetter les relachements. */
    private static final boolean[] directionHeld = new boolean[4];

    /**
     * Vrai tant qu'une competence tient les touches de deplacement.
     *
     * <p>Leve par {@link #tickDirections} a chaque tick du maintien, baisse par
     * {@link #endDirections} : c'est ce drapeau, et lui seul, que lit le refus de la marche —
     * il n'a pas besoin de savoir de quelle competence il s'agit. Voir {@link #onMovementInput}.
     */
    private static boolean directionsTaken;

    private static void tick(Binding binding) {
        Skill skill = skillOf(binding);
        if (skill == null) return;
        Category category = skill.getCategory();
        if (category == null) return;

        // Un clic n'est un APPUI que si la touche n'etait pas deja enfoncee.
        //
        // C'est le piege du clavier : le systeme repet une touche tenue (une trentaine de fois
        // par seconde, soit un clic tous les deux ticks), et `KeyboardHandler` empile CES
        // REPETITIONS comme des clics — `set(touche, true)` suivi de `KeyMapping.click`. Tout ce
        // qui consomme la file les prend donc pour des appuis. Pendant un maintien le serveur
        // ignore poliment ces appuis en trop (`if (data.isCharging(skill)) return`), mais ils
        // restent EN ATTENTE : des que le maintien se termine — au relachement, justement — le
        // premier clic en retard la ROUVRE, et paie son surcout. Vecu : la charge sur F payait
        // deux ou trois fois son ouverture pour un seul geste, « comme si j'avais spamme ».
        //
        // La file est videe dans tous les cas : c'est ce qui empeche les clics en trop de
        // ressortir plus tard. Un appui bref (touche enfoncee ET relachee entre deux ticks)
        // reste vu, puisque `wasDown` etait faux.
        boolean down = binding.key.isDown();
        boolean clicked = binding.key.consumeClick();
        boolean pressed = clicked && !binding.wasDown;
        binding.wasDown = down;

        // Appui : l'original envoyait MSG_KEYDOWN. Une competence qui se charge ouvre
        // son compteur, une competence tenue vit a partir de maintenant, une autre part
        // tout de suite.
        if (pressed) {
            // Une competence qui ouvre un ecran ne part pas : c'est la liste des marques qui
            // decide, et c'est un clic dedans qui enverra quelque chose au serveur.
            if (skill.opensScreen()) {
                AbilityScreens.open(category, skill);
                return;
            }
            // Le serveur refusera-t-il cette ouverture ? Le client ne l'ouvre alors pas du tout,
            // et l'appui part quand meme pour que le serveur dise pourquoi il refuse. Il le dit
            // mieux que le client ne saurait le deviner, et c'est le seul qui decide.
            //
            // Les trois refus qui se voient ici sont ceux de `ActivateSkillPacket` : l'aptitude
            // eteinte, une recharge en cours, et la surcharge en descente. Le quatrieme — la
            // reserve trop juste — est le prix d'ouverture, celui que `perform` exige. Le
            // brouillage s'y ajoute.
            //
            // Le port ouvrait le maintien avant de demander, et le refermait au retour du
            // serveur : le joueur voyait donc le debut du bouclier et entendait son son a chaque
            // appui refuse, « quand on est en cooldown ou en overload, on peut toujours activer
            // brievement le pouvoir, ce qui lance le son et affiche tres rapidement le debut du
            // bouclier ».
            //
            // Ces nombres sont ceux du CLIENT, qui ne peut que sous-estimer ce que le joueur a :
            // sa reserve et sa surcharge ne bougent que par son propre tick et par les envois du
            // serveur, donc elles sont toujours au moins aussi bonnes que les vraies. Un refus lu
            // ici est donc un refus certain, et une activation legitime ne peut pas etre empechee.
            // Et ce que la main doit tenir : c'est le seul refus que le client lise sur le joueur
            // lui-meme, et il ne peut pas s'y tromper — une main est une main. Sans lui, un appui
            // les mains vides ouvrait le maintien pour rien, et le joueur en voyait le debut
            // scintiller avant que le serveur ne le referme. Voir Skill#isHandValid.
            if (HoldRefusal.refusesStart(skill, ClientAbilityData.get(), handOk(skill))) {
                send(category, skill, Phase.PRESS);
                return;
            }
            binding.charging = skill.isChargeable() || skill.isHeld();
            send(category, skill, Phase.PRESS);
            // Le saut traversant se VISE : sa distance part de sa portee maximale, et la molette
            // la regle ensuite cran par cran. C'est le seul geste du mod qui se regle avant de
            // partir — voir TeleportAim.
            if (skill == TeleporterCategory.PENETRATE_TELEPORT) {
                TeleportAim.begin(TeleporterCategory.PENETRATE_TELEPORT.maxDistance(ClientAbilityData.get()));
            }
            if (skill.isChargeable()) {
                // Le compteur s'ouvre MEME quand la charge n'a pas de maximum : son age sert a
                // autre chose qu'a remplir une barre — la portee du fantome de teleportation
                // grandit avec lui. Le port ne l'ouvrait que pour les charges bornees, donc ce
                // fantome restait cloue a deux blocs et n'avancait jamais.
                ClientCharge.begin(skill.getName(), skill.getMaxChargeTicks(ClientAbilityData.get()));
            } else if (skill.isHeld()) {
                ClientCharge.beginSustained(skill.getName());
            }
        }

        if (!binding.charging) return;
        if (binding.key.isDown()) {
            ClientCharge.tick();
            // Le scintillement vise avec les touches de deplacement pendant tout son
            // maintien : c'est la seule competence qui ecoute autre chose que sa touche.
            if (skill.listensToDirections()) {
                tickDirections(category, skill);
            }
            // Et les ailes de tempete lisent le mouvement a chaque tick, chez le joueur :
            // c'est ce que faisait le MSG_TICK client de l'original. La direction est celle
            // du dernier appui, comme son currentDir.
            if (skill.isHeld()) {
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    skill.onClientHoldTick(player, ClientAbilityData.get(), ClientCharge.getTicks(), aimed);
                    // L'electricite des maintiens : l'arc de la charge et son essaim, celui de
                    // la traction magnetique. Des images, rien d'autre — voir leurs classes.
                    ChargingEffect.tick(player, skill, ClientCharge.getTicks());
                    MagMovementEffect.tick(player, skill, ClientCharge.getTicks());
                    // Et le bloc de la manipulation magnetique, que le client porte lui aussi :
                    // sans cela il attend les positions du serveur, et traine derriere le regard
                    // des qu'on tourne la tete. Voir MagManipEffect.tickHeld.
                    MagManipEffect.tickHeld(player, skill);
                    // Et le rayon minier, qui n'existe QUE pour son tireur : l'original posait son
                    // entite chez le client de celui qui le tenait, et personne d'autre ne la
                    // voyait. Le serveur, lui, creuse de son cote sans rien en dire. Voir
                    // MineRayEffect.
                    MineRayEffect.tick(player, skill);
                    // Et la marque de teleportation, qui n'existe que chez son tireur elle aussi, et
                    // qui ne s'allume que sur la touche de direction visee : voir TeleportMark.
                    TeleportMark.tick(player, skill, 0, aimed);                }
            }
            // L'electricite des charges : l'orage s'amase autour de celui qui le prepare.
            // Comme les precedents, des images et rien d'autre — voir ThunderClapEffect.
            if (skill.isChargeable()) {
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    ThunderClapEffect.tick(player, skill, ClientCharge.getTicks());
                    // Et le plasma du meltdowner, qui tourne autour de celui qui le charge :
                    // l'essaim de l'original, aux memes nombres. Voir MeltdownerCharge.
                    MeltdownerCharge.tick(player, skill, ClientCharge.getTicks());
                    // Et l'electricite de l'ecran du renfort, qui se pose des le premier tick de
                    // la charge : voir BodyIntensifyEffect.
                    BodyIntensifyEffect.tickCharge(skill);
                    // Et le fantome de la teleportation au marqueur, dont la portee grandit avec la
                    // charge : il se pose des le premier tick, lui aussi. Voir TeleportMark.
                    TeleportMark.tick(player, skill, ClientCharge.getTicks(), 0);
                    // L'orage tombe TOUT SEUL au bout de sa charge maximale : l'original
                    // terminait sa charge a MAX_TICKS pour frapper, sans attendre que la touche
                    // se relache. Le serveur ne peut pas s'en charger — c'est le client qui tient
                    // la touche — donc la charge se termine ici, par le meme chemin que le
                    // relachement.
                    //
                    // C'est la SEULE competence dans ce cas, et elle le dit elle-meme : le port
                    // appliquait ce chemin a TOUTES les competences chargees, et le joueur a vu ce
                    // que cela donne sur la premiere qu'il a essayee — « le laser part sans que
                    // j'aie a relacher la touche ».
                    int max = skill.getMaxChargeTicks(ClientAbilityData.get());
                    if (max > 0 && ClientCharge.getTicks() >= max && skill.firesAtMaxCharge()) {
                        binding.charging = false;
                        ClientCharge.end();
                        ThunderClapEffect.end();
                        MeltdownerCharge.end();
                        // Une charge qui va jusqu'a son plafond a passe le minimum : le renfort
                        // prend, et sa gerbe aussi.
                        BodyIntensifyEffect.endCharge(true);
                        endDirections();
                        send(category, skill, Phase.RELEASE);
                        return;
                    }
                }
            }
            return;
        }

        // Relachement : l'original envoyait MSG_KEYUP et le serveur executait la
        // competence avec le temps qu'il avait compte de son cote.
        //
        // Ce que le client doit savoir du temps tenu se lit MAINTENANT : une charge trop courte
        // ne declenche rien du tout chez le serveur (voir ActivateSkillPacket), et le renfort
        // n'aura donc pas de gerbe. C'est le meme minimum, relu ici.
        boolean performed = skill.isChargeable()
                && ClientCharge.getTicks() >= skill.getMinChargeTicks(ClientAbilityData.get());
        binding.charging = false;
        ClientCharge.end();
        ThunderClapEffect.end();
        MeltdownerCharge.end();
        // Le rayon minier n'a pas de fin en douceur : l'original tuait son entite sur-le-champ, et
        // c'est ce que fait ce crochet.
        MineRayEffect.end(skill.getName());
        // Le fantome de la teleportation s'en va au meme moment — sa competence est finie — et la
        // visee du saut traversant avec lui : la molette ne regle plus rien.
        TeleportMark.end();
        TeleportAim.end();
        BodyIntensifyEffect.endCharge(performed);
        endDirections();
        send(category, skill, Phase.RELEASE);
    }

    /**
     * Le dezoom de la charge : plus elle monte, plus la vue s'elargit.
     *
     * <p>C'est le retour visuel que l'original donnait a ses charges, et qui manquait ici. Il
     * l'obtenait en ralentissant la marche du joueur, dont le champ de vision se tire aussi — le
     * port ne touche pas au deplacement, il elargit la vue directement.
     *
     * <p>Deux competences s'en servent : l'orage, dont c'etait le seul retour, et le meltdowner,
     * qui l'a recu a la demande du joueur et a la moitie de ses degres. Chacune dit la sienne, et
     * celle d'une charge qui n'est pas la sienne vaut zero.
     *
     * <p>C'est un <b>nombre de degres</b> ajoute au champ du joueur, donc il se voit quel que soit
     * son reglage, et il grandit avec la charge.
     */
    @SubscribeEvent
    public static void onComputeFov(net.minecraftforge.client.event.ComputeFovModifierEvent event) {
        float degrees = ThunderClapEffect.fovDegrees() + MeltdownerCharge.fovDegrees();
        if (degrees <= 0f) return;

        // Le facteur attendu est un rapport au champ de BASE — celui du reglage, pas celui de
        // l'image en cours — donc les degres se divisent par lui.
        int base = net.minecraft.client.Minecraft.getInstance().options.fov().get();
        if (base <= 0) return;
        event.setNewFovModifier(event.getFovModifier() + degrees / base);
    }

    /**
     * La molette du saut traversant.
     *
     * <p>Le saut traversant est la seule teleportation du mod qui se règle, et c'est la molette
     * qui la regle : un cran, un bloc. Le fantome suit, donc le joueur voit sa destination
     * s'approcher ou s'eloigner avant de partir.
     *
     * <p>La molette est <b>prise</b> tant que la visee dure — l'evenement est annule — sinon
     * chaque cran changerait aussi d'objet dans la barre, et le joueur se retrouverait avec une
     * pioche en main au moment de partir. C'est le {@code canUseMouseWheel} de l'original.
     *
     * <p>Et la distance part au serveur a chaque cran : c'est lui qui fait le saut, et il n'a pas
     * de molette pour la connaitre autrement. Voir {@code TeleportDistancePacket}.
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!TeleportAim.active()) return;
        event.setCanceled(true);

        Skill skill = TeleporterCategory.PENETRATE_TELEPORT;
        TeleportAim.scroll(event.getScrollDelta(),
                ((PenetrateTeleportSkill) skill).maxDistance(ClientAbilityData.get()));

        Category category = skill.getCategory();
        if (category == null) return;
        AbilityNetwork.CHANNEL.sendToServer(new TeleportDistancePacket(category.getCategoryId(),
                skill.getId(), (float) TeleportAim.distance()));
    }

    /**
     * Les quatre directions du scintillement, dans l'ordre de l'original.
     *
     * <p>La competence <b>prend</b> les touches de deplacement : tant qu'on la tient, W, A, S
     * et D ne font plus avancer — elles visent, et le relachement fait partir le saut. C'est
     * la meme idee que le clic sur une touche d'aptitude, qui n'attaque plus quand une
     * competence occupe le bouton : un pouvoir prend le pas sur l'action de base de la touche
     * dont il se sert. L'original se contentait d'ecouter par-dessus le jeu et laissait
     * marcher ; c'est le joueur qui a demande la difference.
     *
     * <p>La visee, elle, se lit sur la touche du jeu, sans detour : c'est cette touche qui dit
     * les appuis et les relachements, et rien ne vient plus la contredire. La marche est refusee
     * plus loin, une fois le clavier lu — voir {@link #onMovementInput}.
     */
    private static void tickDirections(Category category, Skill skill) {
        KeyMapping[] keys = movementKeys();
        for (int i = 0; i < keys.length; i++) {
            boolean down = keys[i].isDown();
            if (down && !directionHeld[i]) {
                aimed = i + 1;
            } else if (!down && directionHeld[i] && aimed == i + 1) {
                AbilityNetwork.CHANNEL.sendToServer(new FlashingPacket(
                        category.getCategoryId(), skill.getId(), i + 1));
                aimed = 0;
            }
            directionHeld[i] = down;
        }
        // Et la competence prend les touches pour de bon : c'est ce drapeau que lira le refus de
        // la marche, au prochain tick du joueur.
        directionsTaken = true;
    }

    /**
     * Oublie la visee en cours, et rend les touches : le maintien est fini, ou une autre touche
     * a pris la main.
     */
    private static void endDirections() {
        aimed = 0;
        java.util.Arrays.fill(directionHeld, false);
        directionsTaken = false;
    }

    /**
     * Les touches de deplacement du jeu, dans l'ordre des quatre directions.
     *
     * <p>Lues au moment de l'appel et non gardees : les touches sont recreees a chaque
     * changement de reglages, et une reference gardee continuerait de viser les anciennes.
     */
    private static KeyMapping[] movementKeys() {
        net.minecraft.client.Options options = net.minecraft.client.Minecraft.getInstance().options;
        return new KeyMapping[] { options.keyLeft, options.keyRight, options.keyUp, options.keyDown };
    }

    private static void send(Category category, Skill skill, Phase phase) {
        AbilityNetwork.CHANNEL.sendToServer(
                new ActivateSkillPacket(category.getCategoryId(), skill.getId(), phase));
    }
}
