package cn.academy.terminal.tutorial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ce que le joueur a <b>deja</b> ouvert, parmi les tutoriels.
 *
 * <p>Portage de {@code TutorialData} de l'original, reduit a ce qui sert ici. L'original
 * y rangeait deux choses : les conditions deja remplies (un ensemble de bits, chacune
 * restant acquise une fois vraie) et les tutoriels deja ouverts. Le port n'a qu'une
 * condition possible — avoir eu l'objet en main a un moment — donc le second ensemble
 * suffit : un tutoriel entre ici une fois, et n'en sort plus.
 *
 * <p>C'est ce qui distingue un tutoriel <b>ouvert</b> d'un tutoriel qu'on pourrait
 * seulement relire : ranger ensuite son bloc de fer dans un coffre ne referme pas le
 * tutoriel. Sans cet ensemble, l'ecran regarderait ce que le joueur porte au moment ou
 * il l'ouvre — une simplification que le port avait prise, et que ceci remplace.
 */
public class TutorialData implements INBTSerializable<CompoundTag> {

    private static final String TAG_UNLOCKED = "unlocked";
    private static final String TAG_TERMINAL = "terminal_given";
    private static final String TAG_MISAKA = "misaka_id";

    private final Set<String> unlocked = new LinkedHashSet<>();

    /** Le terminal a-t-il deja ete donne a ce joueur ? Voir {@code TutorialTracker}. */
    private boolean terminalGiven;

    /**
     * Le numero de Misaka du joueur, ou {@code -1} tant qu'il n'a pas ete tire.
     *
     * <p>L'original en donnait un a chaque joueur ({@code RandUtils.rangei(1000, 19000)}) et
     * s'en servait dans le tutoriel d'accueil : « this is Misaka No.14163 ». Le port y mettait
     * le nom du compte Minecraft — le joueur l'a vu tout de suite.
     */
    private int misakaId = -1;

    /** Ouvre un tutoriel. Retourne vrai si cela a change quelque chose. */
    public boolean unlock(String id) {
        return id != null && unlocked.add(id);
    }

    public boolean isUnlocked(String id) {
        return id != null && unlocked.contains(id);
    }

    /** Les identifiants ouverts, dans l'ordre ou ils l'ont ete. */
    public Set<String> getUnlocked() {
        return Set.copyOf(unlocked);
    }

    public int count() {
        return unlocked.size();
    }

    public boolean isTerminalGiven() {
        return terminalGiven;
    }

    public void setTerminalGiven(boolean given) {
        terminalGiven = given;
    }

    /** Le numero de Misaka du joueur, ou {@code -1} s'il n'a pas encore ete tire. */
    public int getMisakaId() {
        return misakaId;
    }

    /**
     * Tire le numero de Misaka s'il n'existe pas encore.
     *
     * @return vrai si un numero vient d'etre tire — donc s'il y a quelque chose a envoyer
     */
    public boolean ensureMisakaId() {
        if (misakaId >= 0) return false;

        // Les bornes de l'original : son {@code RandUtils.rangei(1000, 19000)}.
        misakaId = 1000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(18000);
        return true;
    }

    /** Oublie tout. Sert aux tests et au debogage. */
    public void reset() {
        unlocked.clear();
        terminalGiven = false;
        misakaId = -1;
    }

    public void copyFrom(TutorialData other) {
        unlocked.clear();
        unlocked.addAll(other.unlocked);
        terminalGiven = other.terminalGiven;
        misakaId = other.misakaId;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_TERMINAL, terminalGiven);
        tag.putInt(TAG_MISAKA, misakaId);
        ListTag list = new ListTag();
        for (String id : unlocked) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(TAG_UNLOCKED, list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        unlocked.clear();
        terminalGiven = tag.getBoolean(TAG_TERMINAL);
        misakaId = tag.contains(TAG_MISAKA) ? tag.getInt(TAG_MISAKA) : -1;
        ListTag list = tag.getList(TAG_UNLOCKED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            unlocked.add(list.getString(i));
        }
    }
}
