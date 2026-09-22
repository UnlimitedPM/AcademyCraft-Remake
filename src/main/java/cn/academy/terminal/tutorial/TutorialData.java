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

    private final Set<String> unlocked = new LinkedHashSet<>();

    /** Le terminal a-t-il deja ete donne a ce joueur ? Voir {@code TutorialTracker}. */
    private boolean terminalGiven;

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

    /** Oublie tout. Sert aux tests et au debogage. */
    public void reset() {
        unlocked.clear();
        terminalGiven = false;
    }

    public void copyFrom(TutorialData other) {
        unlocked.clear();
        unlocked.addAll(other.unlocked);
        terminalGiven = other.terminalGiven;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_TERMINAL, terminalGiven);
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
        ListTag list = tag.getList(TAG_UNLOCKED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            unlocked.add(list.getString(i));
        }
    }
}
