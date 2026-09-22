package cn.academy.ability.electromaster;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.energy.EnergyReceiver;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Competence tenue, portage de CurrentCharging : le joueur branche sa propre reserve
 * sur une machine, et lui verse de l'energie tant qu'il tient la touche.
 *
 * <p>L'original appelait cela « charging », et c'etait le premier degre
 * d'electromaster : le moyen de remplir une machine sans passer par le reseau. Il
 * fallait viser un bloc a quinze blocs au plus, et chaque tick coulait un peu de CP
 * dans la machine, tant qu'il en restait.
 *
 * <h2>Ce qui a change</h2>
 *
 * L'original savait aussi charger un <b>objet</b> tenu : c'est le mode {@code isItem}
 * de son contexte, qui branchait la reserve sur les objets a energie. Le port n'a pas
 * d'objets a energie, donc ce mode n'existe pas ici — viser une machine est le seul
 * usage. De meme, il rechargeait les generateurs ; le port n'a que des recepteurs.
 *
 * <p>L'energie debloquee, elle, est reprise telle quelle : le reseau du port est celui
 * de l'original, aux memes capacites. Seul le cout en CP est ramene a l'echelle de la
 * reserve du port (3 a 7 sur 2800, soit 0,11 a 0,25 sur 100) : la duree de charge qui
 * en resulte est celle de l'original — une vingtaine de secondes pour vider sa reserve,
 * pour la meme quantite d'energie versee.
 */
public class ChargingSkill extends Skill {

    /** Portee de la visee, comme l'original. */
    private static final double RANGE = 15.0;

    public ChargingSkill() {
        super("charging", 1);
    }

    /**
     * Energie versee par tick : 15 a 35, comme l'original.
     *
     * {@code Math.floor} vient de l'original, qui arrondissait son debit : verser une
     * quantite entiere evite qu'une machine se remplisse de poussieres de points.
     */
    public double chargeSpeed(AbilityData data) {
        return Math.floor(lerp(15f, 35f, data.getSkillExp(this)));
    }

    /** Cout par tick : 3 a 7 sur la reserve de l'original, donc 0,11 a 0,25 sur 100. */
    public float cpPerTick(AbilityData data) {
        return lerp(0.11f, 0.25f, data.getSkillExp(this));
    }

    /** Surcout d'ouverture : de 65 a 48, comme l'original. */
    @Override
    public float getOverloadCost(AbilityData data) {
        return lerp(65f, 48f, data.getSkillExp(this));
    }

    /** Le branchement lui-meme ne coute pas de CP : c'est l'entretien qui paie. */
    @Override
    public float getCpCost() {
        return 0f;
    }

    @Override
    public boolean isHeld() {
        return true;
    }

    @Override
    public void onStart(Player player, AbilityData data) {
        data.setHeldOverload(this, data.getOverload());
    }

    @Override
    public boolean onHoldTick(Player player, AbilityData data, int heldTicks) {
        // L'entretien se paie par tick ; quand la reserve est vide, le branchement
        // s'arrete, comme le terminate() de l'original.
        if (!data.consumeControlPoint(cpPerTick(data))) return false;

        boolean fed = feed(player, data);
        // 0,0001 par tick utile, 0,00003 sinon : l'original distinguait le branchement
        // qui alimente vraiment quelque chose de celui qui ne fait que briller.
        data.addSkillExp(this, fed ? 0.0001f : 0.00003f);
        return true;
    }

    /** Verse l'energie du tick a la machine visee, s'il y en a une. */
    private boolean feed(Player player, AbilityData data) {
        BlockHitResult hit = rayTrace(player);
        if (hit.getType() != HitResult.Type.BLOCK) return false;

        BlockEntity entity = player.level().getBlockEntity(hit.getBlockPos());
        if (!(entity instanceof EnergyReceiver receiver)) return false;

        // Une machine ne prend que ce qui lui manque, et qu'a hauteur de sa bande
        // passante : lui offrir davantage serait gaspille, et le lui compter serait
        // voler le joueur.
        double offered = Math.min(chargeSpeed(data),
                Math.min(receiver.getRequiredEnergy(), receiver.getBandwidth()));
        if (offered <= 0.0d) return false;

        double refused = receiver.injectEnergy(offered);
        return refused < offered;
    }

    private static BlockHitResult rayTrace(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(RANGE));
        return player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }
}
