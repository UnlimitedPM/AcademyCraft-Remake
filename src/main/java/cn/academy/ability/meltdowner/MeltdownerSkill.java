package cn.academy.ability.meltdowner;

import cn.academy.ability.AbilityData;
import cn.academy.ability.AbilityEvents;
import cn.academy.ability.Skill;
import cn.academy.ability.client.md.MdRayKind;
import cn.academy.ability.electromaster.RailgunHit;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.MdRayPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import net.minecraftforge.network.PacketDistributor;

/** Active skill, port of original Meltdowner: sustained plasma beam, single-target hitscan damage. */
public class MeltdownerSkill extends Skill {

    /** Le tir ne part pas en dessous d'une seconde de charge : {@code TICKS_MIN}. */
    public static final int TICKS_MIN = 20;

    /** Au-dela de deux secondes, charger davantage ne change plus rien : {@code TICKS_MAX}. */
    public static final int TICKS_MAX = 40;

    /**
     * Au bout de combien de ticks de touche TENUE la charge se clot : {@code TICKS_TOLE} de
     * l'original, ou la charge s'abandonnait d'elle-meme.
     *
     * <p>Ce moment n'arrivait jamais dans le port : {@code AbilityData.tickCharges} plafonne le
     * compteur a {@code TICKS_MAX} (c'est la charge UTILE, celle qui dose les degats), donc rien ne
     * terminait plus la charge, qui durait tant que la reserve suivait. Le joueur l'a vu : « si je
     * reste appuyer sur la touche, je peux la charger a l'infini alors que dans le vrai mod, si on
     * reste juste appuyer, au bout d'un moment ca annule la competence, a la place, je veux que ca la
     * lance a la fin ».
     *
     * <p>Donc, ici : le tir PART. Rien d'autre ne change — c'est la charge la plus longue possible,
     * donc un tir a pleine puissance, avec la recharge qui va avec.
     */
    public static final int TICKS_TOLE = 100;

    /**
     * La portee du tir : les <b>cinquante blocs</b> du {@code maxIncrement} de l'original.
     *
     * <p>Le <b>dessin</b>, lui, s'arrete a trente ({@code MeltdownerVisuals.BEAM_LENGTH}) : c'est
     * l'original qui veut ca, et c'est deja le cas du railgun, dont les cinquante blocs viennent du
     * meme endroit.
     */
    public static final double LENGTH = 50.0;

    /**
     * Le rayon du faisceau : de <b>deux a trois blocs</b> selon l'experience, le {@code lerpf(2, 3,
     * exp)} de l'original.
     *
     * <p>C'est lui qui fait du tir un cylindre et non une ligne : le joueur — « pour tuer, il faut
     * obligatoirement que les monstres touche le meltdowner, alors que dans le vrai mod, non ».
     */
    public float radius(AbilityData data) {
        return lerp(2f, 3f, data.getSkillExp(this));
    }

    /**
     * L'energie de creusement : de 300 a 700 selon l'experience, multipliee par le facteur des degats.
     *
     * <p>Elle paie les blocs : chacun coute sa <b>durete</b>. Un mur de pierre part, l'obsidienne
     * demande plus qu'un tir n'en donne, et elle est partagee entre les colonnes du disque — donc un
     * rayon plus large creuse moins profond, comme chez l'original.
     */
    public float energy(AbilityData data) {
        return timeRate(data) * lerp(300f, 700f, data.getSkillExp(this));
    }

    public MeltdownerSkill() {
        super("meltdowner", 3);
    }

    /**
     * Le meltdowner est la competence qui se chargeait dans l'original : la touche
     * reste enfoncee, et le tir part au relachement avec ce qui a ete accumule.
     */
    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public int getMinChargeTicks(AbilityData data) {
        return TICKS_MIN;
    }

    @Override
    public int getMaxChargeTicks(AbilityData data) {
        return TICKS_MAX;
    }

    /**
     * Entretien de la charge, par tick.
     *
     * L'original demandait 10 a 15 CP par tick, et le port reprend ses chiffres :
     * une vingtaine de points pour un tir tenu au maximum, en plus de son cout
     * d'ouverture.
     */
    public float chargeCpCost(AbilityData data) {
        return lerp(10f, 15f, data.getSkillExp(this));
    }

    /** Le client rejoue ce chiffre pour ses nombres : voir {@link Skill#getTickUpkeep}. */
    @Override
    public float getTickUpkeep(AbilityData data, int ticks) {
        return ticks <= TICKS_TOLE ? chargeCpCost(data) : 0f;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        // L'original epingle le surcout de l'ouverture pendant toute la charge (le
        // `overloadKeep` de son contexte) : sans cela, la reserve redescendrait pendant
        // qu'on charge et le tir finirait par ne plus rien couter.
        data.setHeldOverload(this, data.getOverload());
        // Et le compte de la touche tenue repart de zero : c'est lui, et non le compteur de charge,
        // qui sait depuis combien de temps on appuie — voir onChargeTick.
        data.setHoldMark(this, 0);
    }

    /**
     * Un tick de charge : l'entretien, et le compte de la touche tenue.
     *
     * <p>Le compteur de la charge est plafonne a {@code TICKS_MAX} par
     * {@code AbilityData.tickCharges} — c'est la <b>charge utile</b>, celle qui dose les degats — donc
     * il ne peut pas dire depuis combien de temps la touche est enfoncee. C'est la marque de maintien
     * qui compte, remise a zero a l'ouverture ({@link #onStart}), et c'est elle qui decide du tir de
     * cloture : voir {@link #TICKS_TOLE}.
     *
     * <p>Ce tir passe par {@code AbilityEvents.endHeld}, comme un vrai relachement : la recharge est
     * lue avant que le compteur ne soit oublie, l'effet de fin passe, et le client est prevenu — sans
     * quoi son animation continuerait toute seule.
     */
    @Override
    public boolean onChargeTick(Player player, AbilityData data, int chargeTicks) {
        int held = data.getHoldMark(this) + 1;
        if (held >= TICKS_TOLE) {
            onActivate(player, data);
            if (player instanceof ServerPlayer server) {
                AbilityEvents.endHeld(server, data, this);
            }
            return false;
        }
        data.setHoldMark(this, held);
        return data.consumeControlPoint(getTickUpkeep(data, chargeTicks));
    }

    /**
     * Facteur de charge des <b>degats</b> : <b>2,4</b> juste au minimum, <b>2,8</b> a pleine charge.
     *
     * <p>L'original valait 0,8 a 1,2, et le port le suivait. Le joueur l'a fait monter d'un coup :
     * « je veux changer le facteur de charge de 0.8 - 1.2 a 2.4 - 2.8, parce que la maintenant par
     * rapport au railgun elle manque cruellement de degats » — puis, voyant que la recharge et
     * l'experience montaient avec lui : « fait en sorte de remettre sont temps de chargement comme
     * avant et sont gain d'exp aussi ». Seul ce facteur-ci a donc bouge ; la recharge et l'experience
     * se lisent sur {@link #cooldownRate}, qui garde les chiffres du vrai mod.
     */
    public float timeRate(AbilityData data) {
        int ticks = Math.min(data.getChargeTicks(this), TICKS_MAX);
        return lerp(2.4f, 2.8f, (ticks - TICKS_MIN) / (float) (TICKS_MAX - TICKS_MIN));
    }

    /**
     * Le facteur de <b>recharge</b> et d'<b>experience</b> : 0,8 a 1,2, celui de l'original.
     *
     * <p>C'est le {@link #timeRate} d'avant la demande du joueur, garde tel quel pour ces deux
     * courbes : elles etaient trop lourdes une fois multipliees par le facteur des degats — trente-six
     * secondes d'attente au depart de l'experience, et une experience triplee.
     */
    public float cooldownRate(AbilityData data) {
        int ticks = Math.min(data.getChargeTicks(this), TICKS_MAX);
        return lerp(0.8f, 1.2f, (ticks - TICKS_MIN) / (float) (TICKS_MAX - TICKS_MIN));
    }

    /**
     * Degats repris de l'original : de 18 a 50 selon l'experience, le tout multiplie par
     * le facteur de charge des degats. Un tir tenu au maximum fait donc moitie plus mal qu'un
     * tir relache tout juste passe.
     *
     * <p>Les deux bouts, chiffres : au depart de l'experience un tir minimal fait <b>43,2</b>
     * (2,4 x 18) et un tir tenu <b>50,4</b> (2,8 x 18) ; a l'experience maximale, <b>120</b> et
     * <b>140</b>. La marque de radiation, elle, multiplie encore ce total de 1,4 a 1,8 (voir
     * {@link RadiationIntensifySkill}) : 140 deviennent 252.
     *
     * <p>Et depuis la demande du joueur — « fait en sorte qu'il frappe tous les monstres qui passe
     * dans le rayon et pas uniquement 1 seul monstre » —, ce total n'est plus pour UNE cible : il
     * s'applique a <b>chacun</b> des vivants que le rayon traverse. Voir {@link #onActivate}.
     */
    public float damage(AbilityData data) {
        return timeRate(data) * lerp(18f, 50f, data.getSkillExp(this));
    }

    /**
     * Experience : 0,002 de base, multiplie par le facteur de la recharge — celui de l'original, que
     * le joueur a demande de retrouver : « et sont gain d'exp aussi ».
     */
    @Override
    public float getExpGain(AbilityData data) {
        return cooldownRate(data) * 0.002f;
    }

    /**
     * Recharge reprise de l'original : 15 a 7 secondes selon l'experience, multipliees par le
     * facteur de la recharge — celui du vrai mod, 0,8 a 1,2 : <b>12 a 18 secondes</b> au depart de
     * l'experience, <b>5,6 a 8,4</b> a la fin.
     *
     * <p>Elle a suivi le facteur des degats le temps d'une version — jusqu'a trente-six secondes —,
     * et le joueur l'a fait redescendre : « fait en sorte de remettre sont temps de chargement comme
     * avant ». Les deux facteurs sont donc separes.
     */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) (cooldownRate(data) * 20 * lerp(15f, 7f, data.getSkillExp(this)));
    }

    /**
     * Rien a l'appui : le tir se paie par tick de charge.
     *
     * <p>L'original posait 200 a 170 de surcout a l'ouverture et 10 a 15 CP par tick de
     * charge, et rien d'autre. Le port y ajoutait 20 CP fixes, une facture qu'il ne
     * devait pas — c'est {@link #chargeCpCost} qui dit le prix du tir.
     */
    @Override
    public float getCpCost() {
        return 0f;
    }

    /**
     * Surcout repris de l'original : de 200 a 170 selon l'experience, a l'ouverture
     * du tir.
     *
     * L'original drainait encore 10 a 15 points par tick pendant la charge ; ce
     * drainage n'est pas porte, comme les couts en CP de cet original qui supposent
     * une toute autre echelle de reserve.
     */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(200f, 170f, data.getSkillExp(this));
    }

    /**
     * Le tir : le faisceau, et tout ce qu'il traverse.
     *
     * <p>L'original ne frappait qu'<b>une</b> cible, la premiere du regard. Le joueur a demande autre
     * chose : « fait en sorte qu'il frappe tous les monstres qui passe dans le rayon et pas uniquement
     * 1 seul monstre ». Le rayon est donc parcouru d'un bout a l'autre — vingt blocs, ou jusqu'au
     * premier mur — et chaque vivant qu'il coupe encaisse les degats, du plus proche au plus loin.
     */
    @Override
    public void onActivate(Player player, AbilityData data) {
        // La seule competence du port qui se declare dans la categorie des joueurs :
        // c'est ce que l'original demandait, pour que le curseur des competences la
        // baisse avec le reste.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.MD_MELTDOWNER,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);

        beam(player);

        for (LivingEntity living : inTheBeam(player, data)) {
            living.hurt(skillDamage(player), scaled(damage(data) * (float) fade(player, living)));
        }
        digBeam(player, data);
    }

    /**
     * Tout ce que le faisceau traverse, du plus proche au plus loin.
     *
     * <p>Ce n'est pas une ligne, c'est un <b>cylindre</b> : l'original demandait a son
     * {@code RangedRayDamage} toutes les betes dont la distance a l'axe du tir restait sous le rayon
     * du faisceau — deux a trois blocs selon l'experience — et le joueur l'a rappele : « pour tuer,
     * il faut obligatoirement que les monstres touche le meltdowner, alors que dans le vrai mod,
     * non ».
     *
     * <p>La longueur est celle de l'original, {@link #LENGTH} : cinquante blocs, quand le dessin du
     * rayon s'arrete a trente. C'est deja le cas du railgun, et c'est le meme {@code maxIncrement}.
     */
    private List<LivingEntity> inTheBeam(Player player, AbilityData data) {
        Vec3 from = player.getEyePosition(1f);
        Vec3 look = player.getViewVector(1f);
        double radius = radius(data);

        List<LivingEntity> touched = new java.util.ArrayList<>();
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                from, from.add(look.scale(LENGTH))).inflate(radius * RailgunHit.WIDEN + 1.0);
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isPickable() && !e.isSpectator())) {
            Vec3 to = centre(living).subtract(from);
            if (RailgunHit.hitDistance(to.x, to.y, to.z, look.x, look.y, look.z,
                    radius, LENGTH) >= 0) {
                touched.add(living);
            }
        }
        touched.sort(java.util.Comparator.comparingDouble(l -> l.position().distanceToSqr(from)));
        return touched;
    }

    /**
     * Le centre d'une bete, et non ses pieds.
     *
     * <p>C'est un ecart avec l'original, qui prenait {@code posX, posY, posZ} — donc les pieds — pour
     * mesurer la distance a l'axe. Un golem de fer de deux blocs et demi voyait ainsi sa tete sortir du
     * cylindre, alors que c'est son corps qui le traverse. Le centre est ce que le joueur voit.
     */
    private static Vec3 centre(LivingEntity living) {
        return living.position().add(0, living.getBbHeight() / 2, 0);
    }

    /**
     * Ce qu'il reste des degats a cette distance : le rayon <b>s'attenue</b>, comme celui de
     * l'original.
     *
     * <p>Le {@code startDamage} de l'original valait son tarif plein au depart et n'en gardait plus
     * que <b>vingt pour cent</b> au bout de cinquante blocs. La distance qui compte est celle a
     * l'AXE, comme pour le railgun : un mur de flanc n'amortit rien de plus qu'un mur de face.
     */
    private static double fade(Player player, LivingEntity living) {
        Vec3 from = player.getEyePosition(1f);
        Vec3 look = player.getViewVector(1f);
        Vec3 to = centre(living).subtract(from);
        double along = to.x * look.x + to.y * look.y + to.z * look.z;
        double offX = to.x - look.x * along;
        double offY = to.y - look.y * along;
        double offZ = to.z - look.z * along;
        return RailgunHit.damageFactor(Math.sqrt(offX * offX + offY * offY + offZ * offZ), LENGTH);
    }

    /**
     * Le trou : le faisceau creuse les blocs qu'il traverse, tant qu'il a de l'energie.
     *
     * <p>Portage de la seconde moitie de {@code RangedRayDamage.perform}, et le joueur l'a demande
     * tel quel : « si je tape un mur avec, c'est cense faire un gros trou dedans ». Le principe est
     * celui de l'original :
     *
     * <ul>
     *   <li>un <b>disque</b> de colonnes, de la largeur du faisceau, donne la forme du trou — voir
     *       {@link BeamBlocks} ;</li>
     *   <li>chaque colonne est ensuite parcourue le long du tir par un {@code Plotter}, et le bloc
     *       qu'elle rencontre part si l'energie restante couvre sa <b>durete</b>
     *       ({@code getDestroySpeed}), qui est alors consommee ;</li>
     *   <li>l'energie totale ({@link #energy}) est partagee entre les colonnes : un rayon plus large
     *       creuse donc moins profond, comme chez lui.</li>
     * </ul>
     *
     * <p>Deux details qui ne sont pas de l'original, et qui sont dits : un bloc <b>indestructible</b>
     * (durete negative, la bedrock) arrete sa colonne au lieu d'etre ignore — sinon le faisceau
     * creuserait derriere ; et rien n'est lache au sol, la ou l'original laissait tomber un bloc sur
     * vingt (les quatre autres competences du port qui creusent ne lachent rien non plus).
     *
     * <p>Le tout est gate par {@code Config.destroyBlocks}, le {@code generic.destroyBlocks} de la
     * 1.12.2, et chaque bloc passe par {@code BlockEvent.BreakEvent} : ce qu'une protection de serveur
     * refuse ne part pas.
     */
    private void digBeam(Player player, AbilityData data) {
        if (!cn.academy.Config.destroyBlocks) return;
        if (player.level().isClientSide) return;

        Vec3 from = player.getEyePosition(1f);
        Vec3 look = player.getViewVector(1f);
        // Les deux axes du disque : la largeur du regard, puis sa hauteur. Un tir vertical n'a pas de
        // largeur — il n'y a alors rien a creuser, et on le dit plutot que de diviser par zero.
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-6) return;
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        List<double[]> columns = BeamBlocks.offsets(radius(data));
        float each = energy(data) / Math.max(1, columns.size());

        for (double[] offset : columns) {
            Vec3 start = from.add(right.scale(offset[0])).add(up.scale(offset[1]));
            cn.academy.util.Plotter plotter = new cn.academy.util.Plotter(
                    net.minecraft.util.Mth.floor(start.x), net.minecraft.util.Mth.floor(start.y),
                    net.minecraft.util.Mth.floor(start.z), look.x, look.y, look.z);

            float left = each;
            for (int step = 0; step <= LENGTH && left > 0; step++) {
                net.minecraft.core.BlockPos pos = plotter.next();
                net.minecraft.world.level.block.state.BlockState state =
                        player.level().getBlockState(pos);
                if (state.isAir()) continue;

                float hardness = state.getDestroySpeed(player.level(), pos);
                if (hardness < 0) break;
                if (hardness > left) continue;

                if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new net.minecraftforge.event.level.BlockEvent.BreakEvent(
                                player.level(), pos, state, player))) {
                    continue;
                }
                left -= hardness;
                player.level().removeBlock(pos, false);
            }
        }
    }

    /**
     * Le faisceau : ce que le tir devient une fois lache.
     *
     * <p>L'original le posait chez son lanceur seul, par son {@code MSG_PERFORM}. Le port
     * l'annonce a tous ceux qui voient le tireur, comme celui de la salve : les degats du
     * meltdowner ne tombent pas sur lui seulement, il serait etrange que les autres ne voient
     * rien venir.
     *
     * <p>Le rayon voyage par le paquet des rayons du plasma, avec ses deux bouts et son tireur :
     * c'est le client qui le recolle a la main de celui qui a tire — voir {@code MdRayView}. Ses
     * deux points sont ceux de l'original, et ils ne prennent pas la meme origine : voir
     * {@link MeltdownerVisuals}.
     */
    private void beam(Player player) {
        if (!(player instanceof ServerPlayer server)) return;

        Vec3 look = player.getViewVector(1f);
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> server),
                new MdRayPacket(MdRayKind.MELTDOWNER.name(),
                        MeltdownerVisuals.beamFrom(player.getEyePosition(1f), look),
                        MeltdownerVisuals.beamTo(player.position(), look,
                                MeltdownerVisuals.BEAM_LENGTH),
                        player.getId()));
    }
}
