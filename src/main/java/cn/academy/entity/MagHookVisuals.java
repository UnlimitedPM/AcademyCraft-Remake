package cn.academy.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Les nombres du crochet magnetique, portage d'{@code EntityMagHook} et de son
 * {@code RendererMagHook}.
 *
 * <p>Classe sans aucun type qui ait besoin du jeu, comme {@link SilbarnVisuals} et pour la meme
 * raison : ce sont les nombres de l'entite et de son rendu, et les figer par un test est la seule
 * facon de verifier qu'ils ne bougent pas sans lancer un jeu.
 *
 * <h2>Ce qu'il fait, en deux nombres</h2>
 *
 * <p>Deux blocs par tick et une gravite de 0,05 par tick : le crochet part vite et tombe
 * lentement, donc il file droit sur une vingtaine de blocs avant que la chute ne se voie. C'est
 * exactement ce qu'on demande a un objet qu'on vise — il arrive ou le regard pointait, et c'est
 * de la que vient tout son usage.
 *
 * <h2>Et son accroche</h2>
 *
 * <p>Le point d'accroche n'est pas sur la face touchee mais a 0,51 du <b>centre</b> de son bloc,
 * c'est-a-dire un centieme <b>devant</b> elle : le crochet se pose contre la paroi, et l'angle
 * qu'il prend est celui de la face. Le rendu change alors de modele — {@code maghook_open}, sa
 * pince ouverte — et c'est ce qui se voit de loin.
 */
public final class MagHookVisuals {

    /** La vitesse du lancer : deux blocs par tick, l'original. */
    public static final double SPEED = 2.0;

    /** Sa gravite : 0,05 par tick, celle du {@code Rigidbody} de l'original. */
    public static final double GRAVITY = 0.05;

    /** Le mal qu'il fait a ce qu'il touche en vol : quatre, l'original. */
    public static final float HIT_DAMAGE = 4f;

    /** Son cote en vol, en blocs : le {@code setSize(.5f, .5f)} de l'original. */
    public static final float FLY_SIZE = 0.5f;

    /**
     * Son cote une fois plante : le {@code setSize(1f, 1f)} de l'original.
     *
     * <p>Un bloc entier pour un objet de trente centimetres, et c'est voulu : c'est ce que le
     * joueur vise quand il veut s'y accrocher, et une pince qu'il faudrait toucher au centimetre
     * pres ne serait pas jouable. C'est cette boite que la traction du deplacement magnetique doit
     * rencontrer — voir {@code MagMovementSkill.findTarget}, qui ne vise que les entites
     * {@code isPickable}.
     */
    public static final float HIT_SIZE = 1f;

    /** Distance du centre du bloc a laquelle il se plante : 0,51, l'original. */
    public static final double SNAP = 0.51;

    /** Echelle du modele : le {@code glScaled(0.0054)} de l'original. */
    public static final float MODEL_SCALE = 0.0054f;

    /**
     * Les groupes du fichier {@code maghook.obj} — les sept du crochet, et les memes dans
     * {@code maghook_open.obj}.
     *
     * <p>Le lecteur OBJ du port rend ses faces par groupe, parce que c'est ainsi que les modeles
     * de blocs du mod s'en servent. Un nom qui change dans le fichier laisserait donc un modele
     * vide, sans erreur : c'est ce que le test de cette classe surveille.
     */
    public static final List<String> GROUPS = List.of(
            "Box010", "Object009", "Object008", "Object004", "Object005", "Object007", "Object006");

    private MagHookVisuals() {}

    /**
     * Le point ou le crochet se plante, une fois sa face connue.
     *
     * <p>C'est le {@code setPosition(hookX + 0.5 + dir * 0.51, ...)} de l'original, les trois axes
     * d'un coup : le demi-bloc vient du centre du bloc, et la face donne son sens. Un centieme de
     * bloc devant elle, donc — c'est ce qui le fait paraitre pose CONTRE la paroi et non dedans.
     */
    public static Vec3 snapTo(BlockPos block, Direction side) {
        return new Vec3(block.getX() + 0.5 + side.getStepX() * SNAP,
                        block.getY() + 0.5 + side.getStepY() * SNAP,
                        block.getZ() + 0.5 + side.getStepZ() * SNAP);
    }

    /**
     * Le lacet du crochet plante, selon la face qui le porte.
     *
     * <p>C'est le {@code switch(hitSide.getIndex())} de {@code preRender}, et il tombe juste : les
     * indices d'{@code EnumFacing} de la 1.12.2 et les rangs de {@code Direction} de la 1.20.1
     * suivent le meme ordre — bas, haut, nord, sud, ouest, est — donc la face se lit directement.
     *
     * <p>Les faces du haut et du bas n'ont pas de lacet : c'est le tangage qui les couche, voir
     * {@link #pitchFor}.
     */
    public static float yawFor(Direction side) {
        return switch (side) {
            case SOUTH -> 180f;
            case WEST -> -90f;
            case EAST -> 90f;
            default -> 0f;
        };
    }

    /** Le tangage du crochet plante : couche a plat contre un plafond ou un sol, sinon droit. */
    public static float pitchFor(Direction side) {
        return switch (side) {
            case DOWN -> -90f;
            case UP -> 90f;
            default -> 0f;
        };
    }
}
