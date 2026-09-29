package cn.academy.client.hud;

import cn.academy.ability.AbilityData;
import cn.academy.ability.client.ClientAbilityData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Dessine le temoin de points de controle : la barre de CP, a sa place configuree.
 *
 * <p>Portage du dessin de {@code CPBar} de l'original. Sa barre est une image de {@code 964x147}
 * dessinee a {@code 0,2} — donc 193x29 a l'ecran, la taille que {@link HudElement#CP_BAR}
 * annonce — assemblee en trois morceaux, tous dessines depuis la <b>droite</b> :
 *
 * <ol>
 *   <li>le fond, sur toute l'image ;</li>
 *   <li>la <b>surcharge</b>, une bande posee vers le haut, dont la longueur suit la surcharge et
 *       dont la couleur monte de presque transparente a rouge ;</li>
 *   <li>le <b>remplissage de CP</b>, qui pousse aussi vers la gauche, avec son extremite coupee
 *       en biais — voir {@link CpBarVisuals} ;</li>
 * </ol>
 *
 * <p>Quand la surcharge est pleine, l'original ne montre plus le remplissage du tout : le fond
 * rougit, un bandeau strie se pose dessus et l'etiquette « OVERLOADED » clignote.
 *
 * <p>ECART ASSUME : l'original decoupait l'icone de la categorie <b>dans</b> le remplissage a
 * l'aide d'un shader (l'alpha de l'icone perçait la barre, d'ou l'eclair au bout). Ici la barre
 * est dessinee sans shader, donc sans ce trou : c'est la seule chose qui manque au rendu.
 */
@OnlyIn(Dist.CLIENT)
public final class CpBarHud {

    private static final ResourceLocation BACK_NORMAL = texture("cpbar/back_normal");
    private static final ResourceLocation BACK_OVERLOAD = texture("cpbar/back_overload");
    private static final ResourceLocation CP = texture("cpbar/cp");
    private static final ResourceLocation MASK = texture("cpbar/mask");
    private static final ResourceLocation FRONT_OVERLOAD = texture("cpbar/front_overload");
    private static final ResourceLocation HIGHLIGHT = texture("cpbar/highlight_overload");

    /** Le fond se pose a 80 % d'opacite chez l'original. */
    private static final float BACK_ALPHA = 0.8f;
    /** Le bandeau strie de la surcharge. */
    private static final float STRIPE_ALPHA = 0.5f;

    /** Ce que la barre montre en ce moment : l'animation a besoin de la valeur precedente. */
    private static float bufferedOverload;
    private static float bufferedCp;
    private static long lastFrame;

    private CpBarHud() {
    }

    /**
     * Dessine la barre pour ce joueur, ou l'ecran et la config la posent.
     *
     * <p>Rien n'est dessine tant que le joueur n'a pas de categorie : sans pouvoir, il n'y a pas
     * de points de controle a montrer, et la barre vide de l'original ne servait a rien.
     */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        AbilityData data = ClientAbilityData.get();
        // L'aptitude eteinte, chez l'original, il n'y avait plus de barre du tout : c'est la
        // touche d'allumage qui la fait apparaitre, avec le reste du HUD.
        if (!data.isActivated()) return;

        float maxCp = data.getMaxControlPoint();
        if (maxCp <= 0) return;

        HudElement element = HudElement.CP_BAR;
        HudLayout layout = HudConfig.read();
        int left = layout.placeX(element, screenWidth, element.getWidth());
        int top = layout.placeY(element, screenHeight, element.getHeight());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0);
        pose.scale(CpBarVisuals.SCALE, CpBarVisuals.SCALE, 1.0f);

        if (data.isOverloaded()) {
            drawOverloaded(graphics);
        } else {
            // Les deux barres suivent leur valeur au lieu de la sauter : c'est l'animation de
            // l'original, et c'est elle qui montre la surcharge refluer apres une surcharge
            // pleine, au lieu de la voir disparaitre d'un coup.
            smooth(data.getMaxOverload() > 0 ? data.getOverload() / data.getMaxOverload() : 0.0f,
                    data.getControlPoint() / maxCp);

            blit(graphics, BACK_NORMAL, 0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H,
                    0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H, 0xFFFFFF, BACK_ALPHA);
            drawOverloadBand(graphics, bufferedOverload);
            drawFill(graphics, bufferedCp);
        }

        // La couleur du shader teinte TOUT ce qui suit tant qu'on ne la remet pas : sans cette
        // ligne, le texte du HUD dessine apres sortirait colore.
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        pose.popPose();
    }

    /**
     * Avance les deux valeurs affichees vers les valeurs reelles.
     *
     * <p>L'original faisait ce pas une fois par image, avec le temps ecoule depuis la
     * precedente — et jamais plus de 100 ms, pour qu'une pause du jeu ne fasse pas bondir la
     * barre au retour. Le pas lui-meme vient de {@link CpBarVisuals#BALANCE_SPEED} : c'est la
     * meme animation pour les deux barres, comme chez lui.
     */
    private static void smooth(float overload, float cp) {
        long now = Util.getMillis();
        float seconds = lastFrame == 0L ? 0.0f : Math.min(now - lastFrame, 100L) / 1000.0f;
        lastFrame = now;

        float step = CpBarVisuals.balanceStep(seconds);
        bufferedOverload = CpBarVisuals.balance(bufferedOverload, overload, step);
        bufferedCp = CpBarVisuals.balance(bufferedCp, cp, step);
    }

    /**
     * Le remplissage de CP, avec son extremite coupee.
     *
     * <p>Un rectangle ne sait pas pencher : la coupe se dessine donc <b>ligne par ligne</b>, en
     * partant du bord superieur. La hauteur de la bande coupee vaut celle du remplissage et sa
     * largeur vaut {@link CpBarVisuals#CUT}, qui approche un pixel par ligne : la coupe sort
     * exactement, sans marche visible.
     */
    private static void drawFill(GuiGraphics graphics, float progress) {
        int rgb = CpBarVisuals.fillColor(progress);
        int right = CpBarVisuals.FILL_X + CpBarVisuals.FILL_W;
        int bodyLeft = (int) Math.ceil(CpBarVisuals.fillLeftAt(progress, CpBarVisuals.FILL_H - 1));

        if (bodyLeft < right) {
            blit(graphics, CP, bodyLeft, CpBarVisuals.FILL_Y, right - bodyLeft,
                    CpBarVisuals.FILL_H,
                    bodyLeft, CpBarVisuals.FILL_Y, right - bodyLeft, CpBarVisuals.FILL_H,
                    rgb, 1.0f);
        }

        for (int row = 0; row < CpBarVisuals.FILL_H; row++) {
            int rowLeft = (int) Math.ceil(CpBarVisuals.fillLeftAt(progress, row));
            if (rowLeft >= bodyLeft) break;
            blit(graphics, CP, rowLeft, CpBarVisuals.FILL_Y + row, right - rowLeft, 1,
                    rowLeft, CpBarVisuals.FILL_Y + row, right - rowLeft, 1, rgb, 1.0f);
        }
    }

    /** La bande de surcharge, posee vers le haut de l'image et colorée par son propre niveau. */
    private static void drawOverloadBand(GuiGraphics graphics, float overload) {
        if (overload <= 0.0f) return;
        int left = (int) Math.round(CpBarVisuals.overloadLeft(overload));
        int right = CpBarVisuals.OVER_X + CpBarVisuals.OVER_W;
        blit(graphics, MASK, left, CpBarVisuals.OVER_Y, right - left, CpBarVisuals.OVER_H,
                left, CpBarVisuals.OVER_Y, right - left, CpBarVisuals.OVER_H,
                CpBarVisuals.overloadColor(overload), 1.0f);
    }

    /** La surcharge pleine : fond rouge, bandeau strie, et l'etiquette qui clignote. */
    private static void drawOverloaded(GuiGraphics graphics) {
        blit(graphics, BACK_OVERLOAD, 0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H,
                0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H, 0xFFFFFF, BACK_ALPHA);
        blit(graphics, FRONT_OVERLOAD, CpBarVisuals.STRIPE_X, 0,
                CpBarVisuals.STRIPE_W, CpBarVisuals.TEX_H,
                CpBarVisuals.STRIPE_X, 0, CpBarVisuals.STRIPE_W, CpBarVisuals.TEX_H,
                0xFFFFFF, STRIPE_ALPHA);

        // Le clignotement de l'original : entre 0,3 et 1,0 d'opacite, une seconde et quart par
        // battement.
        float pulse = 0.3f + 0.35f * (float) (Math.sin(Util.getMillis() / 200.0) + 1.0);
        blit(graphics, HIGHLIGHT, 0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H,
                0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H, 0xFFFFFF, Math.min(pulse, 1.0f));
    }

    /**
     * Pose un morceau de l'image, teinte d'une couleur et d'une opacite.
     *
     * <p>{@code u}/{@code v} sont la position dans l'image et {@code uWidth}/{@code vHeight} la
     * taille du morceau : c'est ce couple qui permet de <b>recadrer</b> au lieu d'etirer, et
     * c'est indispensable ici — la barre ne montre que la partie droite de son remplissage, sans
     * jamais changer l'echelle de son dessin.
     *
     * <p>L'opacite demandee <b>multiplie</b> celle de la couleur au lieu de la remplacer : les
     * couleurs de la surcharge portent la leur, et c'est elle qui la rend presque invisible au
     * debut puis de plus en plus rouge. La remplacer rendait la surcharge opaque, ce qui n'etait
     * pas ce que ces nombres disaient.
     */
    private static void blit(GuiGraphics graphics, ResourceLocation texture,
                             int x, int y, int width, int height,
                             int u, int v, int uWidth, int vHeight, int rgb, float alpha) {
        if (width <= 0 || height <= 0) return;
        RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f, CpBarVisuals.alphaOf(rgb) * alpha);
        graphics.blit(texture, x, y, width, height, u, v, uWidth, vHeight,
                CpBarVisuals.TEX_W, CpBarVisuals.TEX_H);
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("academy", "textures/guis/" + path + ".png");
    }
}
