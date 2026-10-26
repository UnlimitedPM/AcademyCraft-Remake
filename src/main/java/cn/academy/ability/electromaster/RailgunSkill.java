package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Competence active, portage de {@code Railgun} : un tir tendu, a longue portee, qui traverse ce
 * qu'il trouve et le repousse violemment.
 *
 * <h2>Il ne part pas les mains vides</h2>
 *
 * <p>Portage de l'{@code ItemCoin} et du {@code Delegate} de l'original, qui est tout le rituel du
 * railgun. Il faut <b>lancer une piece</b> — clic droit avec l'objet {@code coin} — et tirer pendant
 * qu'elle <b>retombe</b>, plus de sept dixiemes de son vol (voir {@link CoinToss#READY}) : la piece
 * est alors consommee par le tir. L'autre voie de l'original est le <b>fer</b> — un lingot ou un bloc
 * en main —, egalement consomme. Le <b>cuivre</b> s'y est ajoute a la demande du joueur : « il faut
 * aussi rajouter le cuivre pour le railgun ». Sans l'un ou l'autre, l'appui ne fait <b>rien du
 * tout</b>, pas meme payer.
 *
 * <h2>Les deux voies ne partent pas au meme moment</h2>
 *
 * <p>La <b>piece</b> part a l'appui, tout de suite : c'est un QTE, et sa fenetre ne dure que le temps
 * du vol. Le <b>fer</b> — et le cuivre avec lui —, lui, demande de garder la touche <b>vingt ticks</b> — une seconde — avant que
 * le coup ne parte, et relacher avant l'echeance ne tire rien du tout. C'est le {@code chargeTicks =
 * 20} du {@code Delegate} de l'original, un decompte cote client que le port n'avait pas repris : le
 * joueur l'a remarque aussitot — « si je lance le railgun avec un lingot ou un bloc de fer
 * directement, le clic lance le railgun directement alors que normalement il y a un petit temps
 * d'attente ». Pendant ces vingt ticks, la rafale d'arcs jaillit deja de la main, comme chez
 * l'original, qui la faisait partir a l'appui et non au tir.
 */
public class RailgunSkill extends Skill {

    /** Le grand tir : un rail plus LONG que les quarante-cinq blocs du normal, et plus large. */
    public static final double COMBO_BEAM_LENGTH = 70.0;
    public static final double COMBO_RADIUS = 3.0;
    public static final int COMBO_BEAM_ARCS = 24;

    /**
     * Ce qu'un BLOC ajoute au faisceau : trente pour cent de plus, en longueur, en largeur et en
     * arcs.
     *
     * <p>Demande du joueur le 26/10 : « augmente la taille du laser de 30 % quand je l'utilise
     * avec un bloc ». C'est le fer en bloc de l'original, et le bloc arrache par mag_manip : la
     * meme matiere, mais neuf fois plus — le faisceau se lit donc a l'echelle de ce qu'on y met.
     */
    public static final double BLOCK_BEAM_SCALE = 1.3;

    /**
     * La recharge posee sur l'aimantation quand le tir lui prend son bloc : quarante ticks.
     *
     * <p>Sa veille n'en declare aucune (voir {@code MagManipSkill.getCooldownTicks}) : endHeld
     * n'y posait donc rien, et le joueur pouvait la rallumer dans la foulee et arracher un autre
     * bloc — c'est exactement ce qu'il a signale le 26/10 : « il faut que le mag manip soit en
     * cooldown juste apres, la ce n'est pas le cas, donc ça lance un bloc juste apres ».
     */
    public static final int MAGNET_COOLDOWN_TICKS = 40;

    /**
     * L'energie qu'un bloc de roche ordinaire coute a une colonne.
     *
     * <p>Le budget d'une colonne ne vient plus d'un chiffre fixe mais de sa <b>profondeur</b> :
     * c'est elle que la munition decide (voir {@link #digLength}). La durete ne sert donc plus qu'a
     * ARRETER les blocs durs — l'obsidienne, la bedrock —, et le tunnel va au bout du trait dans
     * la roche comme dans la terre. Avant, le budget de l'original suffisait a la piece mais coupait
     * le bloc a mi-chemin : « il ne casse pas assez de blocs a chaque passage ».
     *
     * <p>Trois virgule deux : la pierre vaut 1,5, le deepslate 3 — les deux passent, l'obsidienne
     * (50) arrete sa colonne.
     */
    public static final float DIG_ENERGY_PER_BLOCK = 3.2f;

    /**
     * Jusqu'ou chacune creuse, en multiple de la longueur du trait : c'est LA profondeur du tunnel.
     *
     * <p>Le budget d'une colonne en decoule ({@code profondeur x DIG_ENERGY_PER_BLOCK}) : la
     * munition decide donc de la LONGUEUR du trou, et la durete ne fait plus que l'arreter sur un
     * bloc dur. Corrige le 26/10 — « ce n'est pas le dig_width que je voulais, je parle de la
     * profondeur du tunnel, pas de son rayon » : la piece perce le trait entier, le lingot les deux
     * tiers, et le bloc la meme part que la piece.
     *
     * <p>Tous trois a la meme part depuis le 26/10 au soir : « la maintenant il mine environ 130
     * bloc de longueur, fait en sorte que ce soit plutot 90 blocs ». Le bloc percait une fois et
     * demie le trait, soit 137 blocs en tir combine — trop. Il perce donc la meme part que la piece,
     * et reste PLUS LONG qu'elle par son faisceau, qui est deja trente pour cent plus grand (voir
     * {@link #BLOCK_BEAM_SCALE}) : 91 blocs en tir combine, 65 seul.
     */
    public static final double COIN_DIG_LENGTH = 1.0;
    public static final double INGOT_DIG_LENGTH = 0.65;
    public static final double BLOCK_DIG_LENGTH = 1.0;

    /** Le pas du disque de creusement : le {@code STEP} de l'original, 0,9 bloc. */
    private static final double DIG_STEP = 0.9;

    /** Les trois barres de degats, par munition puis par le tir combine. */
    public static final float COIN_DAMAGE_MIN = 75f, COIN_DAMAGE_MAX = 125f;
    public static final float IRON_DAMAGE_MIN = 40f, IRON_DAMAGE_MAX = 80f;
    public static final float COMBO_DAMAGE_MIN = 150f, COMBO_DAMAGE_MAX = 200f;

    private static final float CP_COST_MIN_EXP = 200f;
    private static final float CP_COST_MAX_EXP = 450f;
    private static final double KNOCKBACK = 2.5;

    /** La longueur du rail, comme le rayon de l'original : quarante-cinq blocs. */
    private static final double BEAM_LENGTH = 45.0;

    /** Quinze arcs, comme ses {@code ARC_SIZE}. */
    private static final int BEAM_ARCS = 15;

    /**
     * La vie du tir, en ticks : cinquante, comme le {@code life} de l'original.
     *
     * <p>Le premier essai le faisait vivre quinze ticks — trois quarts de seconde, quand
     * l'original en tenait cinquante (deux secondes et demie). C'est la duree qui manquait le
     * plus au tir : le joueur l'a vue trop courte.
     *
     * <p>Chez lui le rayon s'estompait ensuite en deux temps : sa <b>largeur diminuait</b>
     * pendant 800 millisecondes ({@code widthShrinkTime}), puis il disparaissait en une seconde
     * ({@code blendOutTime}, avec l'entree en matiere a 150). Le port n'a pas encore de quoi
     * retrecir un arc pendant qu'il le dessine : c'est une retouche du moteur d'eclairs, a faire
     * avec sa teinte — voir {@code ArcRenderer}, dont chaque arc tire sa couleur de sa texture
     * et non d'un reglage.
     */
    private static final int BEAM_ARC_TICKS = 50;

    /**
     * La vie des ARCS, en ticks : trente, et c'est la plus COURTE des deux.
     *
     * <p>L'original effacait ses eclairs bien avant la fin de son rayon : son
     * {@code EntityRailgunFX.onUpdate} appelle {@code arcHandler.clear()} des que
     * {@code ticksExisted == 30}, alors que le rayon, lui, vit cinquante ticks et s'efface sur sa
     * derniere seconde. Le port les faisait vivre tous les deux cinquante ticks : le faisceau avait
     * donc commence a s'effacer que ses eclairs restaient pleins, et le joueur voyait « les eclairs
     * s'arreter legerement apres le railgun ». Trente ticks, c'est exactement l'instant ou le rayon
     * commence a s'effacer — 1500 millisecondes sur 2500, voir {@code MdRayKind.RAILGUN}.
     */
    private static final int ARC_TICKS = 30;

    /** L'armement de la voie du fer, en ticks : les vingt de l'original. */
    private static final int IRON_ARMING_TICKS = 20;

    /** L'ecart lateral des arcs autour de l'axe, comme le sien (0,1 a 0,25). */
    private static final double BEAM_WOBBLE = 0.25;

    public RailgunSkill() {
        super("railgun", 4);
    }

    /**
     * Degats d'un tir, selon la MUNITION : de 75 a 125 pour une piece, de 40 a 80 pour du fer.
     *
     * <p>ECART DEMANDE PAR LE JOUEUR le 26/10 : la piece se lance, se rattrape en vol et se
     * consomme, le lingot ne demande qu'une main — elle doit donc valoir plus que le double de
     * lui. L'egalite d'avant (110 a 160 pour les deux) ne donnait aucune raison de lancer une
     * piece. Le reglage {@code general.damageScale} adoucit toujours l'ensemble sans rien
     * recompiler.
     *
     * <p>Le cout en CP suit l'original : 200 a 450, et il monte avec l'experience.
     */
    public float damage(AbilityData data, Ammo ammo) {
        float exp = data.getSkillExp(this);
        return ammo == Ammo.COIN ? lerp(COIN_DAMAGE_MIN, COIN_DAMAGE_MAX, exp)
                : lerp(IRON_DAMAGE_MIN, IRON_DAMAGE_MAX, exp);
    }

    /**
     * Le tir COMBINE, sous mag_manip : de 150 a 200, et un rail plus long que le normal.
     *
     * <p>Demande du joueur le 26/10 : tenir la manipulation magnetique et appuyer sur la touche du
     * railgun engage les deux gestes, donc cela doit payer. Ses degats suivent l'experience du
     * railgun, comme ceux des autres tirs, et la munition se consomme pareil.
     */
    public float comboDamage(AbilityData data) {
        return lerp(COMBO_DAMAGE_MIN, COMBO_DAMAGE_MAX, data.getSkillExp(this));
    }

    /**
     * Le tir est-il le COMBINE ? Il l'est quand mag_manip est tenu au moment de l'appui.
     *
     * <p>C'est la seule condition : c'est le geste en cours qui compte, pas un prereglage. La
     * manipulation magnetique est une veille — {@code data.isCharging} dit donc exactement ce
     * qu'on veut savoir, comme pour les crochets de la reflexion de vecteur. Tenir une veille
     * suppose la competence apprise : il n'y a pas d'autre verrou a poser.
     */
    public static boolean isCombo(AbilityData data) {
        return data.isCharging(ElectromasterCategory.MAG_MANIP);
    }

    /** Recharge reprise de l'original : de 300 a 160 ticks, soit 15 a 8 secondes. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(300f, 160f, data.getSkillExp(this));
    }

    /**
     * Experience de l'original : 0,005 pour un tir, 0,01 s'il touche. Le paquet
     * d'activation ne sait pas si le tir a porte, donc c'est le montant du tir qui est
     * verse ; la part du coup au but reviendra avec les evenements de degats.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0.005f;
    }

    /** Le cout en CP, repris de l'original : de 200 a 450 selon l'experience. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(CP_COST_MIN_EXP, CP_COST_MAX_EXP, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return CP_COST_MIN_EXP;
    }

    /**
     * Surcout repris de l'original : de 180 a 120 selon l'experience.
     *
     * C'est le tir qui remplit le plus la reserve : sur une reserve de 350 points,
     * deux tirs d'affilee suffisent a mettre le joueur en surcharge.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(180f, 120f, data.getSkillExp(this));
    }

    /**
     * Ce que le tir admet comme munition dans la main : le LINGOT de fer, et celui de cuivre.
     *
     * <p>L'original acceptait aussi le <b>bloc</b> de fer. Le joueur l'a retire le 26/10 : « de
     * base, avec un bloc en main on peut lancer le railgun, sauf que je ne veux plus que ce soit le
     * cas. Je veux pouvoir lancer un bloc uniquement via l'utilisation du mag manip. » Un bloc ne
     * se tire donc plus depuis une poche — sa place est dans l'aimantation, qui l'arrache du monde
     * et le paie (voir {@code Ammo.CARRIED}). Les blocs de cuivre travaille tombent avec lui, pour
     * la meme raison : ce sont des blocs.
     *
     * <p>Le cuivre reste admis en lingot, a la demande du joueur : il est de la meme famille que le
     * fer, et se tire pareil. Le cuivre <b>brut</b> et le minerai restent ecartes, comme avant.
     */
    public static boolean isAccepted(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.IRON_INGOT)
                || stack.is(net.minecraft.world.item.Items.COPPER_INGOT);
    }

    /**
     * Le tir a-t-il de quoi partir ?
     *
     * <p>La piece retombee d'abord, et le fer ensuite : c'est l'ordre de l'original, dont le
     * delegue regardait la piece en vol avant la main. Le refus est SILENCIEUX — voir
     * {@code ActivateSkillPacket}, qui ne facture rien quand cette question repond non.
     *
     * <p>C'est la seule competence instantanee qui refuse de partir : elle est le premier appelant
     * de ce crochet, qui servait aux maintiens (« y a-t-il un bloc a prendre ? »).
     */
    @Override
    public boolean canStart(Player player, AbilityData data) {
        return ammo(player, data) != Ammo.NONE;
    }

    /**
     * Ce qui part quand on tire, et c'est ce qui decide de la taille du faisceau et du prix.
     *
     * <p>L'original n'avait que deux cas — la piece et le fer —, et le port en avait ajoute un
     * troisieme avec le cuivre. Le quatrieme est celui du joueur du 26/10 : le bloc qu'on tient
     * par <b>mag_manip</b>, sans l'avoir dans la main (« il devrais aussi fonctionner si j'arrache
     * le bloc du sol directement »).
     */
    public enum Ammo {
        /** Une piece lancee et retombee : le tir du rituel. */
        COIN,
        /** Un LINGOT de la main : le tir de base. */
        INGOT,
        /**
         * Le bloc arrache par mag_manip : le tir combine, et le bloc y passe.
         *
         * <p>C'est la SEULE facon de tirer un bloc — un bloc dans une main n'est plus une munition
         * (voir {@link #isAccepted}), et c'est le joueur qui l'a demande.
         */
        CARRIED,
        /** Rien a tirer : le coup ne part pas, et rien n'est facture. */
        NONE;

        /** Un bloc donne-t-il un faisceau plus grand ? C'est la demande du joueur. */
        public boolean isBlock() {
            return this == CARRIED;
        }
    }

    /** Une piece assez retombee, ou du fer dans l'une des deux mains. */
    public static boolean hasAmmo(Player player) {
        return cn.academy.entity.EntityCoinThrowing.ready(player) != null
                || isAccepted(player.getMainHandItem())
                || isAccepted(player.getOffhandItem());
    }

    /**
     * Une piece est-elle encore en l'air pour ce joueur ?
     *
     * <p>Lu par le rappel des touches, qui allume alors le railgun en orange : c'est la fenetre du
     * QTE, et le joueur doit la voir. La piece se cherche dans le monde, donc cela marche des deux
     * cotes (voir {@code EntityCoinThrowing.of}).
     */
    public static boolean coinInFlight(Player player) {
        return player != null && cn.academy.entity.EntityCoinThrowing.of(player) != null;
    }

    /**
     * La piece est-elle assez retombee pour que le tir passe a travers ?
     *
     * <p>C'est le moment ou le rappel des touches passe de l'orange au <b>bleu clair</b> : le
     * joueur du 26/10 — « vers la fin de l'animation de la piece la couleur orange passe au bleu
     * clair pour indiquer que c'est le bon moment pour taper la piece ». Meme regle que le tir
     * lui-meme ({@code EntityCoinThrowing.ready}), donc les deux ne peuvent pas diverger : ce qui
     * brille est exactement ce qui part.
     */
    public static boolean coinReady(Player player) {
        return player != null && cn.academy.entity.EntityCoinThrowing.ready(player) != null;
    }

    /**
     * La munition du tir, dans l'ordre de l'original : la piece d'abord, la main ensuite, et le
     * bloc de mag_manip en dernier recours.
     *
     * <p>La main passe AVANT le bloc arrache : ce qu'on tient est un choix plus explicite que ce
     * qu'on porte a distance, et c'est aussi l'ordre qui consomme le moins de surprises — c'est
     * la poche qui paie avant le decor.
     */
    public static Ammo ammo(Player player, AbilityData data) {
        if (cn.academy.entity.EntityCoinThrowing.ready(player) != null) return Ammo.COIN;

        // Les deux mains, et rien d'autre qu'un LINGOT : un bloc n'est plus une munition de poche
        // (voir {@link #isAccepted}), il ne s'en tire que par l'aimantation.
        if (isAccepted(player.getMainHandItem()) || isAccepted(player.getOffhandItem())) {
            return Ammo.INGOT;
        }

        return carriedBlock(player, data) != null ? Ammo.CARRIED : Ammo.NONE;
    }

    /** La main qui porte la munition : la principale d'abord, comme {@link #ammo}. */
    private static net.minecraft.world.item.ItemStack payingStack(Player player) {
        net.minecraft.world.item.ItemStack main = player.getMainHandItem();
        return isAccepted(main) ? main : player.getOffhandItem();
    }

    /**
     * Le bloc que le joueur tient par mag_manip, s'il y en a un.
     *
     * <p>Le maintien de l'aimantation le designe par son identifiant (voir
     * {@code AbilityData.getHoldTargetId}), qui est deja la facon dont elle le suit d'un tick a
     * l'autre : le railgun n'a donc rien de plus a tenir de son cote.
     */
    public static cn.academy.entity.EntityMagManipBlock carriedBlock(Player player, AbilityData data) {
        if (player == null || data == null) return null;
        net.minecraft.world.entity.Entity entity = player.level().getEntity(
                data.getHoldTargetId(ElectromasterCategory.MAG_MANIP));
        return entity instanceof cn.academy.entity.EntityMagManipBlock block && block.isAlive()
                ? block : null;
    }

    /**
     * Le temps d'<b>armement</b> de la voie du fer : les vingt ticks du {@code Delegate} de
     * l'original.
     *
     * <p>C'est son {@code chargeTicks = 20}, ouvert a l'appui, decremente par son {@code onKeyTick},
     * et suivi du tir a zero. Son {@code onKeyUp} le ramenait a moins un : relacher avant l'echeance
     * annulait tout. Le port compte donc chez le client et n'envoie son appui qu'a l'echeance — voir
     * {@code AbilityClientEvents}, qui tient le compte —, ce qui revient au meme : le serveur ne voit
     * le tir qu'une fois les vingt ticks passes, et une seconde touche du railgun suffit a l'armer
     * pendant que la premiere piece, elle, part sur-le-champ.
     *
     * <p>UNE PIECE QUI VOLE N'ARME RIEN : l'appui est alors celui du QTE, et il part immediatement,
     * comme chez l'original dont le decompte ne s'ouvrait que « quand il n'avait pas de piece ».
     * Sinon c'est la main qui decide : du fer — voir {@link #isAccepted} —, et rien d'autre.
     */
    @Override
    public int getArmingTicks(Player player, AbilityData data) {
        if (cn.academy.entity.EntityCoinThrowing.of(player) != null) return 0;
        // Les deux mains, comme la munition elle-meme : un bloc en main secondaire s'arme pareil.
        return isAccepted(player.getMainHandItem()) || isAccepted(player.getOffhandItem())
                ? IRON_ARMING_TICKS : 0;
    }

    /**
     * Les deux verrous de la rafale de la main, lus sur une donnee et un prereglage quelconques.
     *
     * <p>C'est la condition {@code spawn} de {@code Railgun.onThrowCoin} : l'aptitude ouverte
     * ({@code CPData.canUseAbility} : allumee, pas de surcharge pleine, pas de brouillage) ET le
     * railgun sur une touche du prereglage en service ({@code hasControllable}).
     *
     * <p>Une fonction pure, et c'est ce qui la rend utilisable des DEUX cotes : le serveur lui passe
     * la donnee qu'il tient, le client celle de ses miroirs. Les deux lisent donc exactement la meme
     * regle, et un joueur ne peut pas voir chez lui un effet que les autres ne voient pas.
     */
    public boolean allowsHandEffect(AbilityData data, cn.academy.ability.preset.AbilityPreset preset) {
        if (!data.isActivated() || data.isOverloadRecovering() || data.isInterfered()) return false;
        return preset.contains(getName());
    }

    /**
     * Le serveur annonce la rafale a ceux qui voient ce joueur.
     *
     * <p>C'est le {@code NetworkMessage.sendToAllAround(player, 30, MSG_CHARGE_EFFECT)} de
     * l'original : chacun la dessine alors sur la main du lanceur, de trois quarts. Le port suit le
     * meme chemin que ses faisceaux — les joueurs qui suivent le lanceur — et s'annonce a lui-meme
     * aussi, parce qu'un joueur en vue de trois quarts doit la voir sur son propre modele.
     */
    public void announceHandEffect(Player player) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                new cn.academy.ability.network.RailgunHandPacket(player.getId()));
    }

    /**
     * Et l'annonce complete, verrous compris, telle que le lancer de piece l'appelle.
     *
     * <p>La piece part d'un objet, pas d'une competence : ce n'est donc pas le systeme d'activation
     * qui l'annonce, et c'est ici que le serveur relit ses deux verrous — voir
     * {@link #allowsHandEffect}.
     */
    public static void announceHandEffectIfAllowed(Player player) {
        AbilityData data = player.getCapability(cn.academy.ability.AbilityCapability.ABILITY_DATA)
                .resolve().orElse(null);
        if (data == null) return;
        cn.academy.ability.preset.PresetData presets = cn.academy.ability.preset.PresetTracker.of(player);
        if (presets == null) return;
        if (!ElectromasterCategory.RAILGUN.allowsHandEffect(data, presets.getCurrent())) return;
        ElectromasterCategory.RAILGUN.announceHandEffect(player);
    }

    /**
     * Le tir consomme sa munition : la piece en vol, ou un fer de la main.
     *
     * <p>C'est le {@code MSG_COIN_PERFORM} de l'original — la piece meurt avant que le tir parte —
     * et son {@code MSG_ITEM_PERFORM} pour le fer, qui retirait un exemplaire sauf en creatif. Rien
     * n'est rendu : c'est le prix du tir, en plus de ses CP et de son surcout.
     */
    private static void consumeAmmo(Player player, AbilityData data, Ammo ammo) {
        switch (ammo) {
            case COIN -> {
                cn.academy.entity.EntityCoinThrowing coin =
                        cn.academy.entity.EntityCoinThrowing.ready(player);
                if (coin != null) coin.consume();
            }
            case INGOT -> {
                // Le creatif ne paie rien : c'est la regle de l'original pour la main.
                net.minecraft.world.item.ItemStack paying = payingStack(player);
                if (!player.getAbilities().instabuild && isAccepted(paying)) paying.shrink(1);
            }
            case CARRIED -> {
                // Le bloc arrache y PASSE, TOUJOURS : il est le prix du tir, et le joueur l'a
                // redemande le 26/10 — « ça ne supprime pas le bloc qu'on tient ». Le creatif ne
                // le sauve donc plus : ce bloc-la ne vient pas d'une poche, il vient du monde.
                cn.academy.entity.EntityMagManipBlock block = carriedBlock(player, data);
                if (block != null) block.discard();

                // ET L'AIMANTATION S'ARRETE LA, comme si elle avait servi. Sans cela elle restait
                // ouverte, et son tick suivant retrouvait une main pleine de blocs : le tir en
                // arrachait un autre, et le bloc detruit semblait remplace sur-le-champ. « Je
                // voudrais que apres un railgun ça comptabilise comme si la competence avait ete
                // utilisee et donc qu'elle s'arrete par defaut. » C'est endHeld qui le fait, avec
                // sa recharge — le meme chemin qu'un second appui.
                if (player instanceof net.minecraft.server.level.ServerPlayer server) {
                    cn.academy.ability.AbilityEvents.endHeld(server, data,
                            ElectromasterCategory.MAG_MANIP);
                    // Et une recharge, puisqu'elle n'en declare pas : sans elle, elle se rallume
                    // au tick suivant et arrache un autre bloc (voir MAGNET_COOLDOWN_TICKS).
                    data.setCooldown(ElectromasterCategory.MAG_MANIP, MAGNET_COOLDOWN_TICKS);
                }
            }
            case NONE -> { }
        }
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // La munition et la FORME du tir d'abord, et la consommation ensuite : consommer le bloc
        // arrache ferme l'aimantation, et le tir ne doit pas y perdre son cote combine pour
        // autant — la forme se lit sur l'etat de l'appui, pas sur celui d'apres.
        Ammo ammo = ammo(player, data);
        boolean fromCoin = ammo == Ammo.COIN;
        boolean combo = isCombo(data);
        // Et un BLOC agrandit ce faisceau de trente pour cent, quel qu'il soit : la munition se
        // lit dans la taille du tir — voir {@link #BLOCK_BEAM_SCALE}.
        double scale = ammo.isBlock() ? BLOCK_BEAM_SCALE : 1.0;
        double length = (combo ? COMBO_BEAM_LENGTH : RailgunHit.LENGTH) * scale;
        double radius = (combo ? COMBO_RADIUS : RailgunHit.RADIUS) * scale;
        int arcs = (int) Math.round((combo ? COMBO_BEAM_ARCS : BEAM_ARCS) * scale);

        // La munition part maintenant : la piece meurt avant que le tir parte, chez l'original.
        consumeAmmo(player, data, ammo);

        // LA RAFALE D'ARCS, et c'est ici pour le fer seulement : sur une piece, le lancer s'en est
        // deja charge (voir ModItems.CoinItem.use), alors que la voie du fer ne se decide que la,
        // au moment ou le tir part. C'est ce que faisait l'original, dont le Delegate.onKeyDown
        // appelait spawnClientEffect des que la main tenait du fer. L'annonce s'en va aux autres
        // joueurs aussi : voir announceHandEffect.
        if (!fromCoin) announceHandEffect(player);

        // Le seul son de l'original qui se pose dans le monde plutot qu'au joueur : un tir de
        // railgun s'entend de loin. Il part a CHAQUE tir — le port ne le jouait qu'en touchant,
        // ce qui rendait muet le tir qui manque.
        cn.academy.sound.AcademySounds.playAt(player.level(), player.position(),
                cn.academy.ModSounds.EM_RAILGUN, 0.5f, 1.0f);

        shootBeam(player, length, arcs);

        // LES DEGATS : TOUT UN CYLINDRE, et non une cible unique. C'est le RangedRayDamage de
        // l'original — rayon 2, longueur 50, voir RailgunHit — et le port, lui, ne prenait qu'une
        // cible a trente blocs : le joueur a trouve le trou tout seul, « un monstre a 46 blocs ne se
        // fait pas tuer alors que dans le vrai mod si ». Toutes les cibles du cylindre sont donc
        // touchees, de la plus proche a la plus lointaine, chacune avec l'attenuation de sa distance.
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f).normalize();
        float base = scaled(combo ? comboDamage(data) : damage(data, ammo));

        for (Entity target : entitiesInBeam(player, eye, look, length, radius)) {
            if (!(target instanceof LivingEntity living)) continue;
            Vec3 to = target.position().subtract(eye);
            double distance = RailgunHit.hitDistance(to.x, to.y, to.z, look.x, look.y, look.z,
                    radius, length);
            if (distance < 0.0) continue;

            living.hurt(skillDamage(player),
                    (float) (base * RailgunHit.damageFactor(distance, length)));
            Vec3 push = living.position().subtract(player.position()).normalize().scale(KNOCKBACK);
            living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.2, push.z));
            living.hurtMarked = true;
        }

        // Et il CREUSE. Le port ne cassait rien du tout : le joueur du 26/10 — « par contre le
        // railgun ne casse pas les blocs la ». C'est la seconde moitie de {@code RangedRayDamage},
        // celle qui suit les memes colonnes que les degats.
        // La PROFONDEUR que cette munition perce, et le budget de chaque colonne qui va avec.
        double digBlocks = length * digLength(ammo);
        digBeam(player, eye, look, digBlocks, radius, (float) (digBlocks * DIG_ENERGY_PER_BLOCK));
    }

    /** Jusqu'ou cette munition creuse : la part du trait qui perce vraiment. */
    public static double digLength(Ammo ammo) {
        return switch (ammo) {
            case COIN -> COIN_DIG_LENGTH;
            case INGOT -> INGOT_DIG_LENGTH;
            case CARRIED -> BLOCK_DIG_LENGTH;
            case NONE -> 0.0;
        };
    }

    /**
     * La profondeur REELLE, faisceau compris : la part du trait multipliee par l'elargissement de
     * la munition.
     *
     * <p>C'est elle qui se compare d'une munition a l'autre, et c'est la seule mesure honnete : le
     * bloc perce la meme PART que la piece, mais son faisceau est trente pour cent plus long, donc
     * son trou l'est aussi. Trente-deux blocs pour le lingot, cinquante pour la piece, soixante-cinq
     * pour le bloc — et quatre-vingt-onze en tir combine.
     */
    public static double digReach(Ammo ammo) {
        return digLength(ammo) * (ammo.isBlock() ? BLOCK_BEAM_SCALE : 1.0);
    }

    /**
     * Le tunnel : chaque colonne du disque creuse tant que son energie tient.
     *
     * <p>C'est l'original au plus pres — {@code STEP} de 0,9, rayon tire a 0,9-1,1 par colonne (le
     * bord du tunnel est donc irregulier) —, mais le budget d'une colonne se lit sur sa
     * <b>profondeur</b> et non sur une somme partagee : chaque colonne va au bout de sa portee dans
     * la roche, et seule la durete d'un bloc dur l'arrete. C'etait la correction du joueur du 26/10 :
     * « ce n'est pas le dig_width que je voulais, je parle de la profondeur du tunnel, pas de son
     * rayon ».
     *
     * <p>Un bloc indestructible (durete negative, la bedrock) arrete sa colonne au lieu d'etre
     * ignore : sans cela le faisceau creuserait derriere lui.
     *
     * <p>Deux ecarts, dits : rien n'est lache au sol (comme les quatre autres competences du port
     * qui creusent), et le tir ne s'arrete pas a la premiere creature touchee — le port n'a pas
     * cette notion de cible qui bloque le rayon.
     *
     * <p>Le tout est gate par {@code general.destroyBlocks}, et chaque bloc passe par
     * {@code BlockEvent.BreakEvent} : ce qu'une protection de serveur refuse ne part pas.
     */
    private static void digBeam(Player player, Vec3 eye, Vec3 look, double length, double radius,
                                float perColumn) {
        if (!cn.academy.Config.destroyBlocks) return;
        if (player.level().isClientSide) return;

        // Les deux axes du disque : la largeur du regard, puis sa hauteur. Un tir vertical n'a pas
        // de largeur — il n'y a alors rien a creuser, et on le dit plutot que de diviser par zero.
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-6) return;
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        net.minecraft.util.RandomSource random = player.getRandom();

        // Les COLONNES : un point de depart par case du disque, sans doublon — deux points dans la
        // meme case creuseraient le meme tunnel deux fois, et l'energie s'y perdrait.
        java.util.Set<net.minecraft.core.BlockPos> columns = new java.util.LinkedHashSet<>();
        for (double s = -radius; s <= radius; s += DIG_STEP) {
            for (double t = -radius; t <= radius; t += DIG_STEP) {
                double edge = radius * (0.9 + random.nextDouble() * 0.2);
                if (s * s + t * t > edge * edge) continue;
                Vec3 seed = eye.add(right.scale(s)).add(up.scale(t));
                columns.add(net.minecraft.core.BlockPos.containing(seed));
            }
        }
        if (columns.isEmpty()) return;

        for (net.minecraft.core.BlockPos seed : columns) {
            // L'original tirait la part de chaque colonne a plus ou moins cinq pour cent : deux
            // tirs dans le meme mur ne font donc pas exactement le meme trou.
            float budget = perColumn * (0.95f + random.nextFloat() * 0.1f);
            digColumn(player, seed, look, length, budget);
        }
    }

    /** Une colonne : le tunnel d'un point du disque, tant que son energie tient. */
    private static void digColumn(Player player, net.minecraft.core.BlockPos seed, Vec3 look,
                                  double length, float energy) {
        cn.academy.util.Plotter plotter = new cn.academy.util.Plotter(
                seed.getX(), seed.getY(), seed.getZ(), look.x, look.y, look.z);
        float left = energy;

        for (int step = 0; step <= (int) Math.ceil(length) && left > 0f; ++step) {
            net.minecraft.core.BlockPos pos = plotter.next();
            net.minecraft.world.level.block.state.BlockState state =
                    player.level().getBlockState(pos);
            if (state.isAir()) continue;

            float hardness = state.getDestroySpeed(player.level(), pos);
            if (hardness < 0f) return;                        // indestructible : la colonne s'arrete
            if (hardness > left) return;                     // plus assez d'energie pour ce bloc

            if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new net.minecraftforge.event.level.BlockEvent.BreakEvent(
                            player.level(), pos, state, player))) {
                continue;
            }
            left -= hardness;
            player.level().removeBlock(pos, false);
        }
    }

    /**
     * Les entites qui peuvent etre dans le faisceau, de la plus proche du tireur a la plus loin.
     *
     * <p>Une boite d'abord, et c'est une commodite autant qu'une optimisation : elle englobe le
     * cylindre, et le test exact — {@link RailgunHit#hitDistance} — se fait ensuite sur chaque
     * candidate. C'est l'ordre de l'original, qui triait ses cibles par distance avant de les
     * frapper une a une.
     */
    private static List<Entity> entitiesInBeam(Player player, Vec3 eye, Vec3 look,
                                              double length, double radius) {
        Vec3 end = eye.add(look.scale(length));
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(eye, end)
                .inflate(radius * RailgunHit.WIDEN + 1.0);

        List<Entity> targets = player.level().getEntities(player, box);
        targets.sort(java.util.Comparator.comparingDouble(
                target -> target.distanceToSqr(eye)));
        return targets;
    }

    /**
     * Le rail : quinze arcs semes le long du tir, jusqu'a quarante-cinq blocs.
     *
     * <p>C'est ce que dessinait {@code EntityRailgunFX} : un rayon lumineux, et des arcs
     * gresillant le long de son axe, un tous les un a deux blocs, avec un petit ecart lateral
     * autour de la ligne. Le port n'a pas encore de rendu de rayon — il lui faudrait un trait
     * qu'il n'a pas — mais les arcs, eux, se font avec le moteur d'eclairs qu'il a deja, et
     * c'est eux qui donnent au tir son cote electrique.
     */
    private static void shootBeam(Player player, double length, int arcs) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        net.minecraft.util.RandomSource random = player.getRandom();

        // Le faisceau d'abord : le trait lumineux que tout le reste entoure. C'est un CYLINDRE,
        // pas un ruban — voir ArcRenderer, ou les siens sont dessines.
        //
        // Il part des YEUX, comme dans l'original, et va jusqu'au bout de la portee du tir —
        // celle du tir COMBINE quand il est tenu, et c'est le paquet qui la porte.
        sendBeam(player, eye, eye.add(look.scale(length)), BEAM_ARC_TICKS);

        for (int i = 0; i < arcs; i++) {
            double start = 1.0 + i * (length - 1.0) / arcs;
            // Leur LONGUEUR, et elle a ete raccourcie : le joueur les trouvait « trop grands ». Ce
            // n'est pas la place qu'ils prennent dans l'original — qui les seme un tous les un a deux
            // blocs — mais la longueur de chacun, et une par seconde et demie de tir.
            double end = Math.min(length, start + 0.8 + random.nextDouble() * 0.8);
            // Les arcs ne sont pas SUR l'axe : ils gresillent autour, comme les siens. Un rail
            // parfaitement droit ne ressemblerait a rien.
            //
            // ECART ASSUME, et c'est un bug de l'original que le joueur a remarque : chez lui
            // les arcs partaient toujours vers l'est, quelle que soit la visee — sa fabrique
            // les generait le long de l'axe X, et seule leur POSITION etait tournee. Ici les
            // deux bouts sont pris sur la visee, donc les arcs suivent le faisceau.
            Vec3 from = wobble(eye.add(look.scale(start)), random);
            Vec3 to = wobble(eye.add(look.scale(end)), random);
            sendArc(player, cn.academy.ability.client.arc.ArcPattern.RAILGUN.name(), from, to, ARC_TICKS);
        }
    }

    /** Un point du rail, ecarte d'un rien de l'axe : la demi-borne laterale de l'original. */
    private static Vec3 wobble(Vec3 point, net.minecraft.util.RandomSource random) {
        return point.add(wobble(random), wobble(random), wobble(random));
    }

    /** Une composante de cet ecart, entre moins et plus {@code BEAM_WOBBLE}. */
    private static double wobble(net.minecraft.util.RandomSource random) {
        return (random.nextDouble() - 0.5) * BEAM_WOBBLE * 2.0;
    }

    /**
     * Envoie le faisceau lui-meme : un cylindre, que le client dessine.
     *
     * <p>Le drapeau du paquet dit « ceci est un faisceau », donc le serveur n'a rien a savoir
     * de sa forme — voir {@code ArcEffectPacket.beam}.
     */
    private static void sendBeam(Player player, Vec3 from, Vec3 to, int ticks) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                cn.academy.ability.network.ArcEffectPacket.beam(from, to, ticks, player.getId()));
    }

    /**
     * Envoie un arc a tous ceux qui voient le tireur.
     *
     * <p>Le motif voyage par son nom, donc le serveur n'a rien a connaitre du rendu — c'est le
     * message d'effet de l'original, celui qui fait qu'on voit le tir venir.
     */
    private static void sendArc(Player player, String pattern, Vec3 from, Vec3 to, int ticks) {
        cn.academy.ability.network.AbilityNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF
                        .with(() -> player),
                new cn.academy.ability.network.ArcEffectPacket(pattern, from, to, ticks, false,
                        player.getId()));
    }
}
