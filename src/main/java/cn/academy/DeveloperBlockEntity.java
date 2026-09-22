package cn.academy;

import java.util.UUID;

import javax.annotation.Nullable;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DevelopAction;
import cn.academy.ability.develop.DevelopActionLevel;
import cn.academy.ability.develop.DevelopActionSkill;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.energy.EnergyReceiver;
import cn.academy.energy.NodeFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Developeur d'aptitudes : il transforme de l'energie en aptitudes.
 *
 * Portage de {@code TileDeveloper} et de {@code DevelopData} de la 1.12.2.
 *
 * <h2>Ou vit l'apprentissage</h2>
 *
 * Dans l'original, la progression etait portee par le <b>joueur</b>
 * ({@code DevelopData} etait un DataPart) et gardait une reference vers le
 * developeur utilise. Ici elle vit sur la <b>machine</b>. La difference pratique
 * est faible — dans les deux cas on peut lancer un apprentissage et s'en aller —
 * et cela evite d'ajouter toute une donnee par joueur pour une seule machine. Ce
 * qui change : un developeur casse perd l'apprentissage en cours, la ou
 * l'original aurait continué puis echoue faute de pouvoir tirer l'energie.
 *
 * <h2>Le deroulement</h2>
 *
 * Un apprentissage est une suite de <b>stimulations</b>. Chacune dure
 * {@code tps} ticks et coute {@code cps} unites d'energie. Le developeur tire
 * {@code cps / tps} par tick depuis son tampon ; si le tampon ne suit pas, tout
 * est perdu et l'apprentissage est marque en echec — c'est le comportement de
 * l'original, et c'est ce qui oblige a brancher la machine sur le reseau.
 *
 * L'apprentissage n'est valide qu'a la fin. Un joueur qui perd son niveau ou sa
 * categorie entre-temps echoue apres avoir paye : la encore, c'est l'original.
 */
public class DeveloperBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements MenuProvider, EnergyReceiver {

    /** Etat d'un apprentissage, repris de {@code DevelopData.DevState}. */
    public enum DevState {
        IDLE,
        DEVELOPING,
        FAILED,
        DONE
    }

    /** Cadence de recherche d'un noeud, en ticks. */
    private static final int NODE_SEARCH_INTERVAL = 100;

    /** Cadence de synchronisation vers le client, en ticks. */
    private static final int SYNC_INTERVAL = 10;

    private final DeveloperType type;

    private double energy;

    private DevState state = DevState.IDLE;

    /** Categorie en cours d'apprentissage, ou -1. */
    private int categoryId = -1;

    /**
     * Competence visee, ou -1 pour un apprentissage de niveau.
     *
     * Le developpeur mene un apprentissage a la fois, et il en existe deux sortes :
     * faire monter une categorie d'un cran, ou apprendre une de ses competences.
     * Ce champ dit laquelle des deux.
     */
    private int skillId = -1;

    /** Apprenti, ou {@code null} si personne. C'est la seule reference sauvegardee. */
    @Nullable
    private UUID student;

    /**
     * Reference vive vers l'apprenti, non sauvegardee.
     *
     * L'original gardait directement l'objet joueur. On garde l'UUID comme source
     * de verite — c'est ce qui survit a un rechargement — et cette reference en
     * plus, parce qu'un joueur peut exister sans figurer dans la liste des joueurs
     * connectes, ce qui est justement le cas des joueurs simules des tests.
     */
    @Nullable
    private ServerPlayer studentRef;

    @Nullable
    private String studentName;

    /** Action en cours, reconstruite au chargement a partir de la categorie. */
    @Nullable
    private DevelopAction action;

    private int stim;
    private int maxStim;
    private int tickThisStim;

    private boolean linked;

    private int searchCounter;
    private int syncCounter;

    public DeveloperBlockEntity(BlockPos pos, BlockState state, DeveloperType type) {
        super(type == DeveloperType.ADVANCED
                ? ModBlockEntities.DEVELOPER_ADVANCED.get()
                : ModBlockEntities.DEVELOPER_NORMAL.get(), pos, state);
        this.type = type;
    }

    public DeveloperType getDeveloperType() {
        return type;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, DeveloperBlockEntity dev) {
        if (!(level instanceof ServerLevel server)) return;

        if (++dev.searchCounter >= NODE_SEARCH_INTERVAL) {
            dev.searchCounter = 0;
            boolean nowLinked = NodeFinder.ensureLinked(server, pos);
            if (nowLinked != dev.linked) {
                dev.linked = nowLinked;
                dev.setChanged();
            }
        }

        if (dev.state == DevState.DEVELOPING) dev.advance(server);

        if (++dev.syncCounter >= SYNC_INTERVAL) {
            dev.syncCounter = 0;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /** Un tick d'apprentissage : une fraction de stimulation, ou l'echec. */
    private void advance(ServerLevel level) {
        ServerPlayer player = resolveStudent(level);
        if (player == null || action == null) {
            fail();
            return;
        }

        double cost = type.getEnergyPerTick();
        if (energy < cost) {
            // Le tampon n'a pas suivi. L'original repartait de zero et marquait
            // l'echec : l'energie deja depensee est perdue.
            fail();
            return;
        }

        energy -= cost;

        // L'original comptait tps + 1 ticks par stimulation (comparaison
        // stricte apres increment). On garde ce detail plutot que de le corriger
        // en silence : ce serait un changement de vitesse non demande.
        if (++tickThisStim > type.getTps()) {
            tickThisStim = 0;
            stim++;

            if (stim >= maxStim) {
                complete(player);
                return;
            }
        }
        setChanged();
    }

    /** Derniere etape : on verifie une derniere fois, puis on applique ou on echoue. */
    private void complete(ServerPlayer player) {
        boolean success = action != null && action.validate(player, type);
        if (success) {
            action.onLearned(player);
            reset(DevState.DONE);
        } else {
            fail();
        }
    }

    private void fail() {
        reset(DevState.FAILED);
    }

    private void reset(DevState newState) {
        state = newState;
        action = null;
        categoryId = -1;
        skillId = -1;
        student = null;
        studentRef = null;
        studentName = null;
        stim = 0;
        maxStim = 0;
        tickThisStim = 0;
        setChanged();
    }

    @Nullable
    private ServerPlayer resolveStudent(ServerLevel level) {
        if (student == null) return null;
        if (studentRef != null && studentRef.isAlive() && student.equals(studentRef.getUUID())) {
            return studentRef;
        }
        return level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(student);
    }

    // ------------------------------------------------------------------
    // Lancement d'un apprentissage
    // ------------------------------------------------------------------

    /**
     * Demarre l'apprentissage d'une categorie pour ce joueur.
     *
     * @return vrai si l'apprentissage a pu demarrer
     */
    public boolean startDeveloping(ServerPlayer player, int requestedCategoryId) {
        return startDeveloping(player, requestedCategoryId, -1);
    }

    /**
     * Demarre l'apprentissage d'une competence d'une categorie.
     *
     * @param requestedSkillId l'identifiant de la competence <b>dans sa categorie</b>,
     *                         ou -1 pour faire monter la categorie d'un cran
     * @return vrai si l'apprentissage a pu demarrer
     */
    public boolean startDeveloping(ServerPlayer player, int requestedCategoryId, int requestedSkillId) {
        if (state == DevState.DEVELOPING) return false;

        Category category = CategoryManager.INSTANCE.getCategory(requestedCategoryId);
        if (category == null) return false;

        DevelopAction candidate = buildAction(category, requestedSkillId);
        if (candidate == null) return false;

        // Au niveau maximal il n'y a plus rien a faire, et une competence deja
        // apprise n'a plus rien a apprendre. L'ecran grise deja ces lignes, donc ce
        // refus n'est qu'une ceinture de securite ; l'original ne prevenait pas le
        // joueur autrement qu'en changeant l'etat affiche.
        if (!candidate.validate(player, type)) return false;

        action = candidate;
        categoryId = requestedCategoryId;
        skillId = requestedSkillId;
        student = player.getUUID();
        studentRef = player;
        studentName = player.getGameProfile().getName();
        stim = 0;
        tickThisStim = 0;
        maxStim = candidate.getStimulations(player);
        state = DevState.DEVELOPING;
        setChanged();
        return true;
    }

    /**
     * L'action correspondant a une cible, ou {@code null} si la cible n'existe pas.
     *
     * Sert aussi bien au lancement qu'a la reconstruction apres un chargement : une
     * action n'est jamais sauvegardee, elle est refabriquee a partir de la categorie
     * et de la competence.
     */
    @Nullable
    private static DevelopAction buildAction(Category category, int targetSkillId) {
        if (targetSkillId < 0) return new DevelopActionLevel(category);
        Skill skill = category.getSkill(targetSkillId);
        return skill == null ? null : new DevelopActionSkill(skill);
    }

    /** Interrompt l'apprentissage en cours, sans rien rendre. */
    public void abort() {
        if (state == DevState.DEVELOPING) fail();
    }

    // ------------------------------------------------------------------
    // Etat, pour l'ecran
    // ------------------------------------------------------------------

    public DevState getState() {
        return state;
    }

    public int getStim() {
        return stim;
    }

    public int getMaxStim() {
        return maxStim;
    }

    public int getCategoryId() {
        return categoryId;
    }

    /** Competence visee, ou -1 si l'apprentissage porte sur le niveau de la categorie. */
    public int getSkillId() {
        return skillId;
    }

    @Nullable
    public String getStudentName() {
        return studentName;
    }

    /**
     * Avancement entre 0 et 1, comme {@code DevelopData.getDevelopProgress} : les
     * stimulations faites, plus la fraction de la stimulation en cours.
     */
    public double getProgress() {
        if (state != DevState.DEVELOPING || maxStim <= 0) return 0.0d;
        double done = (double) stim / maxStim;
        double current = (double) tickThisStim / maxStim / type.getTps();
        return Math.min(1.0d, done + current);
    }

    public double getEnergy() {
        return energy;
    }

    public int getEnergyStored() {
        return (int) energy;
    }

    public int getMaxEnergyStored() {
        return (int) type.getEnergy();
    }

    /** Renseigne le tampon directement (utilise par les tests). */
    public void setEnergy(double value) {
        energy = Math.min(type.getEnergy(), Math.max(0.0d, value));
        setChanged();
    }

    public boolean isLinked() {
        return linked;
    }

    // ------------------------------------------------------------------
    // EnergyReceiver
    // ------------------------------------------------------------------

    @Override
    public double getRequiredEnergy() {
        return type.getEnergy() - energy;
    }

    @Override
    public double injectEnergy(double amount) {
        double accepted = Math.min(amount, type.getEnergy() - energy);
        if (accepted > 0.0d) {
            energy += accepted;
            setChanged();
        }
        return amount - accepted;
    }

    @Override
    public double getBandwidth() {
        return type.getBandwidth();
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.putInt("state", state.ordinal());
        tag.putInt("category", categoryId);
        tag.putInt("skill", skillId);
        tag.putInt("stim", stim);
        tag.putInt("max_stim", maxStim);
        tag.putInt("tick_this_stim", tickThisStim);
        tag.putBoolean("linked", linked);
        if (student != null) tag.putUUID("student", student);
        if (studentName != null) tag.putString("student_name", studentName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = Math.min(type.getEnergy(), Math.max(0.0d, tag.getDouble("energy")));
        state = stateOf(tag.getInt("state"));
        categoryId = tag.getInt("category");
        skillId = tag.contains("skill") ? tag.getInt("skill") : -1;
        stim = tag.getInt("stim");
        maxStim = tag.getInt("max_stim");
        tickThisStim = tag.getInt("tick_this_stim");
        linked = tag.getBoolean("linked");
        student = tag.hasUUID("student") ? tag.getUUID("student") : null;
        studentRef = null;
        studentName = tag.contains("student_name") ? tag.getString("student_name") : null;

        // L'action n'est pas sauvegardee : elle est reconstruite a partir de la
        // categorie et de la competence, ce qui evite de stocker un objet qui n'a pas
        // de sens hors du jeu et qui pourrait ne plus exister apres une mise a jour.
        Category category = categoryId >= 0 ? CategoryManager.INSTANCE.getCategory(categoryId) : null;
        action = category != null ? buildAction(category, skillId) : null;
        if (state == DevState.DEVELOPING && action == null) state = DevState.FAILED;
    }

    private static DevState stateOf(int ordinal) {
        DevState[] values = DevState.values();
        if (ordinal < 0 || ordinal >= values.length) return DevState.IDLE;
        return values[ordinal];
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DeveloperMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy."
                + (type == DeveloperType.ADVANCED ? "developer_advanced" : "dev_normal"));
    }
}
