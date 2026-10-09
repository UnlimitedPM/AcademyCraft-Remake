package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.TargetingUtil;
import cn.academy.ability.client.arc.ArcPattern;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.ability.network.ArcEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.Random;

/** Active skill, port of original ArcGen: short-range electric arc, chance to set target on fire. */
public class ArcGenSkill extends Skill {

    private static final int IGNITE_TICKS = 80;

    /** La duree de l'eclair, en ticks : le {@code Life(10)} de l'original. */
    private static final int ARC_TICKS = 10;

    private final Random random = new Random();

    public ArcGenSkill() {
        super("arc_gen", 1);
    }

    //
    // Courbes reprises de l'original : les degats vont de 5 a 9, la portee de 6 a 15
    // blocs, et la chance d'embraser de 0 a 60 %, le tout selon l'experience de la
    // competence. Le cout en CP suit : 30 a 70, comme chez lui.
    //

    public float damage(AbilityData data) {
        return lerp(5f, 9f, data.getSkillExp(this));
    }

    public double range(AbilityData data) {
        return lerp(6f, 15f, data.getSkillExp(this));
    }

    public float igniteChance(AbilityData data) {
        return lerp(0f, 0.6f, data.getSkillExp(this));
    }

    /** Recharge reprise de l'original : de 15 a 5 ticks, soit de 0,75 a 0,25 seconde. */
    @Override
    public int getCooldownTicks(AbilityData data) {
        return (int) lerp(15f, 5f, data.getSkillExp(this));
    }

    /**
     * L'arc ne verse plus son experience a l'activation : c'est l'effet qui sait ce
     * qu'il vient de toucher.
     *
     * <p>L'original donnait deux montants selon ce que son rayon rencontrait, et rien du
     * tout quand il ne rencontrait rien (voir {@link #onActivate}). Les verser depuis le
     * paquet, qui les versait avant l'effet, revenait a donner le prix du coup au but a
     * un arc qui n'avait touche qu'un mur.
     */
    @Override
    public float getExpGain(AbilityData data) {
        return 0f;
    }

    /** Oui : le montant depend de ce que le rayon a trouve. */
    @Override
    public boolean earnsExpOnEffect() {
        return true;
    }

    /**
     * Ce que rapporte un arc qui touche un etre vivant : 0,48 % a 0,72 % de la barre,
     * selon l'experience deja acquise.
     */
    public float hitExp(AbilityData data) {
        return lerp(0.0048f, 0.0072f, data.getSkillExp(this));
    }

    /**
     * Ce que rapporte un arc qui ne touche qu'un bloc : 0,18 % a 0,27 %.
     *
     * <p>C'est le cas courant — on tire sur un mur ou sur le sol — et l'original le
     * payait presque trois fois moins que le coup au but.
     */
    public float blockExp(AbilityData data) {
        return lerp(0.0018f, 0.0027f, data.getSkillExp(this));
    }

    /** Le cout en CP, repris de l'original : de 30 a 70 selon l'experience. */
    @Override
    public float getCpCost(AbilityData data) {
        return lerp(30f, 70f, data.getSkillExp(this));
    }

    /** Le cout au depart, pour qui n'a pas d'experience a donner. */
    @Override
    public float getCpCost() {
        return 30f;
    }

    /** Surcout repris de l'original : de 18 a 11 selon l'experience. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(18f, 11f, data.getSkillExp(this));
    }

    @Override
    public void onActivate(Player player, AbilityData data) {
        // L'arc claque meme s'il ne trouve rien : l'original le jouait a l'appui, avant de
        // savoir sur quoi son rayon tomberait.
        cn.academy.sound.AcademySounds.playFor(player, cn.academy.ModSounds.EM_ARC_WEAK, 0.5f);

        double range = range(data);
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        // L'eclair part des YEUX, comme dans l'original : il posait son entite a
        // `posY + eyeHeight` et l'orientait selon le regard. Ce n'est donc pas une main, et il
        // ne faut pas en chercher une : en vue interne le depart est sur la camera, donc
        // invisible, et en vue externe il tombe sur l'axe qui va de la camera au personnage —
        // d'ou l'impression qu'il sort du corps quand on vise devant soi, et de nulle part
        // quand on vise le ciel. ArcView recolle l'arc du tireur sur sa camera : c'est l'autre
        // moitie du meme mecanisme.
        // Le rayon des degats, lui, s'arrete au premier bloc : ce qui se trouve derriere un
        // mur ne s'attrape pas.
        BlockHitResult block = TargetingUtil.findBlockInSight(player, range);
        Vec3 end = block == null ? eye.add(look.scale(range)) : block.getLocation();
        // L'eclair, lui, va jusqu'au bout de la portee, meme si le mur est plus proche.
        // L'original faisait exactement cela : il envoyait sa portee au client, et son arc se
        // dessinait jusque-la quel que soit le bloc rencontre. Le couper au premier mur le
        // rendait beaucoup plus court que dans l'original.

        // L'eclair se dessine chez tous ceux qui voient le tireur, et pas seulement chez
        // lui : c'est le message d'effet de l'original, et c'est ce qui fait qu'on voit
        // l'attaque venir. Les motifs d'arcs sont purs — aucun type de Minecraft — donc le
        // serveur peut les nommer sans rien connaitre du rendu.
        // Le tireur voyage avec l'arc : c'est sa vue qui se recolle a sa camera.
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ArcEffectPacket(ArcPattern.WEAK.name(), eye, eye.add(look.scale(range)),
                        ARC_TICKS, false, player.getId()));

        Entity target = TargetingUtil.findEntityAlong(player, eye, end,
                e -> e instanceof LivingEntity);

        if (target instanceof LivingEntity living) {
            living.hurt(skillDamage(player), scaled(damage(data)));
            // L'arc peut charger un creeper : c'est l'autre moitie de l'EMDamageHelper de
            // l'original, invisible autrement (voir CreeperCharge).
            CreeperCharge.tryCharge(living, random.nextFloat());
            if (random.nextFloat() < igniteChance(data)) {
                living.setSecondsOnFire(IGNITE_TICKS / 20);
            }
            data.addSkillExp(this, hitExp(data));
            return;
        }

        // Rien de vivant devant : soit un bloc a pris l'arc, soit l'arc n'a rien touche.
        if (block == null) {
            // L'arc part dans le vide. L'original n'accordait alors AUCUNE experience : sa
            // ligne d'experience etait enfermee dans son « si le rayon a touche quelque
            // chose », et lancer dans le ciel ne rapportait donc rien. Le port s'en ecarte a
            // la demande du joueur — une competence qu'on peut lancer doit instruire, meme
            // quand elle ne touche rien. C'est le gain du bloc, le plus faible des deux :
            // toucher quelque chose vaut mieux que lancer dans le vide.
            data.addSkillExp(this, blockExp(data));
            return;
        }

        // C'est le bloc qui a pris l'arc. L'original enflammait alors le bloc juste au-dessus,
        // quand il y avait de la place — c'est le feu que l'arc laisse sur les murs, et c'est
        // aussi ce qui paye l'experience.
        BlockPos above = block.getBlockPos().above();
        if (random.nextFloat() < igniteChance(data) && player.level().isEmptyBlock(above)) {
            player.level().setBlockAndUpdate(above, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
        }
        data.addSkillExp(this, blockExp(data));
    }
}
