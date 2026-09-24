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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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

        // Appui : l'original envoyait MSG_KEYDOWN. Une competence qui se charge ouvre
        // son compteur, une competence tenue vit a partir de maintenant, une autre part
        // tout de suite.
        if (binding.key.consumeClick()) {
            // Une competence qui ouvre un ecran ne part pas : c'est la liste des marques qui
            // decide, et c'est un clic dedans qui enverra quelque chose au serveur.
            if (skill.opensScreen()) {
                AbilityScreens.open(category, skill);
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
                }
            }
            return;
        }

        // Relachement : l'original envoyait MSG_KEYUP et le serveur executait la
        // competence avec le temps qu'il avait compte de son cote.
        binding.charging = false;
        ClientCharge.end();
        endDirections();
        send(category, skill, Phase.RELEASE);
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
