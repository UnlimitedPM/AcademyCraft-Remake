package cn.academy.ability.client;

import cn.academy.AcademyCraft;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.electromaster.ElectromasterCategory;
import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.ActivateSkillPacket;
import cn.academy.ability.network.ActivateSkillPacket.Phase;
import cn.academy.ability.network.FlashingPacket;
import cn.academy.ability.teleporter.TeleporterCategory;
import cn.academy.ability.vecmanip.VecmanipCategory;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
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

    @Nullable
    private static net.minecraft.client.player.LocalPlayer minecraftPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }

    /** La direction visee par le clavier, ou 0 : le scintillement saute au relachement. */
    private static int aimed;

    /** L'etat des quatre touches au tick precedent, pour guetter les relachements. */
    private static final boolean[] directionHeld = new boolean[4];

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
            // L'aptitude eteinte, le serveur refusera l'activation : le client n'ouvre donc pas
            // le maintien du tout. Avant, il l'ouvrait quand meme, et le tick suivant rejouait
            // l'animation du pouvoir pour rien — c'est ce que le joueur a signale, sa touche V
            // eteinte. Le PRESS part quand meme, pour que le serveur dise pourquoi il refuse.
            if (!ClientAbilityData.get().isActivated()) {
                send(category, skill, Phase.PRESS);
                return;
            }
            binding.charging = skill.isChargeable() || skill.isHeld();
            send(category, skill, Phase.PRESS);
            if (skill.isChargeable()) {
                // Une charge sans maximum n'a rien a montrer : la barre serait pleine des le
                // premier tick. L'original, lui, faisait plonger le regard du joueur pendant
                // la charge — un retour visuel cote client que le port n'a pas encore de
                // crochet pour reproduire. Le bouclier, lui, a son propre temoin.
                int max = skill.getMaxChargeTicks(ClientAbilityData.get());
                if (max > 0) ClientCharge.begin(skill.getName(), max);
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
                }
            }
            // L'electricite des charges : l'orage s'amase autour de celui qui le prepare.
            // Comme les precedents, des images et rien d'autre — voir ThunderClapEffect.
            if (skill.isChargeable()) {
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    ThunderClapEffect.tick(player, skill, ClientCharge.getTicks());
                    // Et l'electricite de l'ecran du renfort, qui se pose des le premier tick de
                    // la charge : voir BodyIntensifyEffect.
                    BodyIntensifyEffect.tickCharge(skill);
                    // L'orage tombe TOUT SEUL au bout de sa charge maximale : l'original
                    // terminait sa charge a MAX_TICKS pour frapper, sans attendre que la touche
                    // se relache. Le serveur ne peut pas s'en charger — c'est le client qui tient
                    // la touche — donc la charge se termine ici, par le meme chemin que le
                    // relachement.
                    int max = skill.getMaxChargeTicks(ClientAbilityData.get());
                    if (max > 0 && ClientCharge.getTicks() >= max) {
                        binding.charging = false;
                        ClientCharge.end();
                        ThunderClapEffect.end();
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
        BodyIntensifyEffect.endCharge(performed);
        endDirections();
        send(category, skill, Phase.RELEASE);
    }

    /**
     * Le dezoom de la charge de l'orage : plus elle monte, plus la vue s'elargit.
     *
     * <p>C'est le retour visuel que l'original donnait, et qui manquait ici. Il l'obtenait en
     * ralentissant la marche du joueur, dont le champ de vision se tire aussi — le port ne
     * touche pas au deplacement, il elargit la vue directement.
     *
     * <p>C'est un <b>nombre de degres</b> ajoute au champ du joueur, donc il se voit quel que soit
     * son reglage, et il grandit avec la charge.
     */
    @SubscribeEvent
    public static void onComputeFov(net.minecraftforge.client.event.ComputeFovModifierEvent event) {
        float degrees = ThunderClapEffect.fovDegrees();
        if (degrees <= 0f) return;

        // Le facteur attendu est un rapport au champ de BASE — celui du reglage, pas celui de
        // l'image en cours — donc les degres se divisent par lui.
        int base = net.minecraft.client.Minecraft.getInstance().options.fov().get();
        if (base <= 0) return;
        event.setNewFovModifier(event.getFovModifier() + degrees / base);
    }

    /**
     * Les quatre directions du scintillement, dans l'ordre de l'original.
     *
     * <p>Les touches du jeu ne sont pas detournees : on marche normalement en visant, et
     * c'est seulement le relachement qui fait partir le saut — exactement comme l'original,
     * qui ajoutait ses ecouteurs par-dessus ceux du jeu. La touche enfoncee ne fait que
     * <b>viser</b> : l'original affichait un anneau a l'endroit de l'arrivee, et le port, qui
     * n'a pas ce rendu, garde le meme geste.
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
    }

    /** Oublie la visee en cours : le maintien est fini, ou une autre touche a pris la main. */
    private static void endDirections() {
        aimed = 0;
        java.util.Arrays.fill(directionHeld, false);
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
