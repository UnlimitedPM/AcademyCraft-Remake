package cn.academy.ability.electromaster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

import java.lang.reflect.Field;

/**
 * Le creeper « haute tension » de l'original : un eclair de l'electromaster a trois chances sur
 * dix de charger la bete qu'il vient de toucher.
 *
 * <p>Portage de {@code EMDamageHelper.attack} de l'original, qui frappait puis levait le drapeau
 * du creeper. Cette mecanique ne se voit pas dans les competences — elle vit dans un aide d'une
 * quinzaine de lignes — et pourtant elle change beaucoup : un creeper charge explose avec une
 * puissance DOUBLE ({@code Creeper} : 2,0 au lieu de 1,0, soit une explosion de puissance 6 au
 * lieu de 3), donc ses explosions tuent les monstres d'a cote, et un monstre tue par l'explosion
 * d'un creeper charge laisse sa TETE. Le joueur, lui, voit la bete se mettre a luire.
 *
 * <p>Pourquoi une reflexion et pas une ligne de code : Minecraft n'ouvre que
 * {@code Creeper.isPowered()}, et sa seule porte publique est {@code Creeper.thunderHit(...)} —
 * qui ne dit pas « charge cette bete » mais « la foudre vient de tomber ici » : il met le feu huit
 * secondes et ajoute cinq degats de foudre ({@code Entity.thunderHit}). L'emprunter changerait les
 * degats de la competence et enflammerait la cible, ce que l'original ne faisait pas. Le drapeau
 * {@code DATA_IS_POWERED} est donc atteint par son nom, exactement comme chez lui
 * ({@code ReflectionUtils.getObfField(EntityCreeper.class, "POWERED", "field_184714_b")}), avec ses
 * DEUX noms essayes a la suite : celui du developpement et celui du jeu livre.
 *
 * <p>Le nom du jeu livre n'est pas devine, il est releve : les mappings de Forge de la 1.20.1
 * ({@code srg_to_official_1.20.1.tsrg}) donnent la ligne {@code f_32274_ DATA_IS_POWERED}. C'est la
 * seule ligne a relire avant de changer de version de Minecraft — sans elle, la mecanique
 * s'eteindrait en silence, ce que l'original laissait deja ouvert (« TODO need Test »). Le GameTest
 * {@code laFoudreChargeParfoisLeCreeper} la surveille sur une vraie bete, dans le jeu.
 */
public final class CreeperCharge {

    /** Trois chances sur dix : le {@code RandUtils.nextFloat() < 0.3f} de l'original. */
    public static final float CHANCE = 0.3f;

    /** Le drapeau du creeper, sous ses deux noms : celui du developpement, puis celui du jeu. */
    private static final String[] FLAG_NAMES = {"DATA_IS_POWERED", "f_32274_"};

    private static EntityDataAccessor<Boolean> flag;
    private static boolean looked;

    private CreeperCharge() {
    }

    /**
     * Charge la cible si c'est un creeper, si le tirage passe, et si elle ne l'est pas deja.
     *
     * @param target la bete que la competence vient de toucher
     * @param roll   le tirage, entre 0 et 1 (chaque competence tient son propre hasard)
     * @return vrai quand le drapeau vient de basculer
     */
    public static boolean tryCharge(LivingEntity target, float roll) {
        if (!(target instanceof Creeper creeper) || roll >= CHANCE || creeper.isPowered()) {
            return false;
        }
        EntityDataAccessor<Boolean> powered = flag();
        if (powered == null) {
            return false;
        }
        creeper.getEntityData().set(powered, true);
        return true;
    }

    /**
     * Le drapeau du creeper, lu une fois pour toutes.
     *
     * <p>Rend {@code null} si aucun des deux noms ne repond : la charge ne se fait pas, et c'est
     * tout. Une mecanique en moins vaut mieux qu'un plantage au premier eclair.
     */
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> flag() {
        if (!looked) {
            looked = true;
            for (String name : FLAG_NAMES) {
                try {
                    Field field = Creeper.class.getDeclaredField(name);
                    field.setAccessible(true);
                    flag = (EntityDataAccessor<Boolean>) field.get(null);
                    break;
                } catch (ReflectiveOperationException | ClassCastException ignored) {
                    // Le nom suivant, ou rien du tout.
                }
            }
        }
        return flag;
    }
}
