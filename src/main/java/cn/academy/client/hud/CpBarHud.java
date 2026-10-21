package cn.academy.client.hud;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.preset.client.ClientPresetData;
import cn.academy.command.AimCommandSpec;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL14;

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

    /**
     * Le fond se pose a 80 % d'opacite chez l'original, mais sa texture n'en porte que 50 :
     * la plaque sortait donc a 40 % et le joueur l'a trouvee trop pale (30/09). Elle est
     * maintenant posee pleine, ce qui la fait lire a 50 % — l'ecart est assume, et il se
     * retire d'un chiffre si le voile d'ecran de l'original arrive un jour.
     */
    private static final float BACK_ALPHA = 1.0f;
    /** Le bandeau strie de la surcharge. */
    private static final float STRIPE_ALPHA = 0.5f;

    /**
     * Le blanc qui ne teinte rien, opacite comprise.
     *
     * <p>Il porte ses huit chiffres : {@code 0xFFFFFF} n'en a que six, donc pas d'opacite du
     * tout — et comme le dessin la multiplie a celle de la couleur, un blanc sans opacite ne
     * dessinait rien. C'est exactement ce qui a rendu invisible le fond de la barre et tout
     * l'etat de surcharge.
     */
    private static final int WHITE = 0xFFFFFFFF;

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

        // Les deux valeurs affichees suivent les vraies au lieu de les sauter : c'est l'animation
        // de l'original. Ce pas se fait a CHAQUE image, meme quand la barre est en surcharge,
        // sinon la valeur affichee resterait figee en arriere et la bande ne pourrait pas refluer
        // depuis le plein quand la surcharge redescend.
        smooth(data.getMaxOverload() > 0 ? data.getOverload() / data.getMaxOverload() : 0.0f,
                data.getControlPoint() / maxCp);

        if (data.isOverloaded()) {
            drawOverloaded(graphics);
        } else {
            blit(graphics, BACK_NORMAL, 0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H,
                    0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H, WHITE, BACK_ALPHA);
            drawOverloadBand(graphics, bufferedOverload);
            drawFill(graphics, bufferedCp);
            drawPowerEmblem(graphics, bufferedCp);
        }

        // Le rappel des prereglages se pose apres le reste, dans les deux etats : chez l'original
        // aussi il vivait en dehors du choix entre barre normale et surcharge.
        drawPresetHint(graphics);

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
                0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H, WHITE, BACK_ALPHA);
        blit(graphics, FRONT_OVERLOAD, CpBarVisuals.STRIPE_X, 0,
                CpBarVisuals.STRIPE_W, CpBarVisuals.TEX_H,
                CpBarVisuals.STRIPE_X, 0, CpBarVisuals.STRIPE_W, CpBarVisuals.TEX_H,
                WHITE, STRIPE_ALPHA);

        // Le clignotement de l'original : entre 0,3 et 1,0 d'opacite, une seconde et quart par
        // battement.
        float pulse = 0.3f + 0.35f * (float) (Math.sin(Util.getMillis() / 200.0) + 1.0);
        blit(graphics, HIGHLIGHT, 0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H,
                0, 0, CpBarVisuals.TEX_W, CpBarVisuals.TEX_H, WHITE, Math.min(pulse, 1.0f));
    }

    /**
     * L'embleme du pouvoir, decoupe DANS le remplissage.
     *
     * <p>L'original n'ajoutait pas une image par-dessus sa barre : son {@code cpbar_cp.frag}
     * multipliait l'opacite du remplissage par {@code 1 - icone.a}, si bien que la silhouette du
     * pouvoir apparaissait <b>en creux</b>, avec le fond de la barre — donc le monde — a travers.
     * Le portage s'en tenait a « l'ecart assume » de ne pas l'avoir ; le joueur l'a demande :
     * « on est cense avoir une petite icone en plus pour nous indiquer quel pouvoir on possede ».
     *
     * <p>Le trou se fait ici sans shader, et c'est le FOND qu'on peint dans la forme de l'icone :
     * chez l'original, ce qu'on voit dans le creux n'est pas le monde seul mais le monde AVEC la
     * plaque de la barre par-dessus. Le melange {@code CONSTANT_COLOR / ONE_MINUS_SRC_ALPHA} ecrit
     * cette couleur-la dans la forme de l'icone — la teinte de l'icone ne compte pas, seule son
     * opacite, comme dans son fragment shader. Un effacement pur ({@code ZERO /
     * ONE_MINUS_SRC_ALPHA}) donnerait du NOIR : le fond du jeu a deja ete compose, et le multiplier
     * par zero ne le rend pas.
     *
     * <p>Et la plaque n'est ecrite qu'a SON opacite (128 sur 255) : le remplissage de la barre se
     * lit donc encore dessous, ce qui est exactement ce que faisait l'original — son creux laissait
     * voir le monde a travers la plaque. Pleine, la forme faisait une tache noire.
     *
     * <p>Il faut ecrire la geometrie nous-memes : le type de rendu d'un {@code blit} de GUI
     * repose son propre melange a chaque envoi, et effacerait le notre. D'ou le {@code flush()}
     * avant — tout ce qui a ete empile doit deja etre a l'ecran.
     */
    private static void drawPowerEmblem(GuiGraphics graphics, float progress) {
        Category power = AimCommandSpec.activePower(ClientAbilityData.get(),
                CategoryManager.INSTANCE.getCategories());
        if (power == null) return;

        ResourceLocation icon = power.getOverlayIcon();
        cn.academy.client.gui.GuiTextures.nearest(icon);
        graphics.flush();

        int x0 = (int) Math.ceil(CpBarVisuals.emblemLeft(progress));
        int y0 = CpBarVisuals.ICON_Y;
        int x1 = CpBarVisuals.ICON_X + CpBarVisuals.ICON_SIZE;
        int y1 = y0 + CpBarVisuals.ICON_SIZE;
        float u0 = (x0 - CpBarVisuals.ICON_X) / (float) CpBarVisuals.ICON_SIZE;

        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.CONSTANT_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GL14.glBlendColor(CpBarVisuals.PLATE_RED, CpBarVisuals.PLATE_GREEN,
                CpBarVisuals.PLATE_BLUE, 1.0f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        // L'icone doit etre BLANCHE : seule son opacite compte, comme dans le fragment shader de
        // l'original. Or ses pixels sont bleus (196, 225, 255), et le melange {@code CONSTANT_COLOR}
        // multiplie la couleur de la source avant de l'ecrire. On pousse donc les trois canaux
        // au-dela de un pour les saturer : le blanc ecrit alors exactement la couleur du fond. Et
        // l'opacite est celle de la plaque, pour que la barre se lise encore dans la forme.
        RenderSystem.setShaderColor(2.0f, 1.5f, 1.2f, CpBarVisuals.EMBLEM_COVERAGE);
        RenderSystem.setShaderTexture(0, icon);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, x0, y0, 0.0f).uv(u0, 0.0f).endVertex();
        buffer.vertex(matrix, x0, y1, 0.0f).uv(u0, 1.0f).endVertex();
        buffer.vertex(matrix, x1, y1, 0.0f).uv(1.0f, 1.0f).endVertex();
        buffer.vertex(matrix, x1, y0, 0.0f).uv(1.0f, 0.0f).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Le rappel des prereglages : quatre carres numerotes sous la barre, celui en service entoure.
     *
     * <p>C'est le {@code drawPresetHint} de l'original, montre deux secondes apres un changement
     * de prereglage et pas une image de plus. Il repond a une question que le HUD ne disait pas :
     * « quand on change de prereglage, dans le CP indicator on est cense voir dans quel prereglage
     * on se trouve ».
     */
    private static void drawPresetHint(GuiGraphics graphics) {
        long switched = ClientPresetData.switchAt();
        if (switched < 0L) return;

        long now = Util.getMillis();
        long previous = ClientPresetData.previousSwitchAt();
        float alpha = CpBarVisuals.presetHintAlpha(now - switched,
                previous < 0L ? Long.MAX_VALUE : switched - previous);
        if (alpha <= 0.0f) return;

        int current = ClientPresetData.get().getCurrentId();
        int back = CpBarVisuals.withAlpha(CpBarVisuals.PRESET_BACK_RGB, alpha);
        int text = CpBarVisuals.withAlpha(CpBarVisuals.PRESET_TEXT_RGB,
                CpBarVisuals.presetLabelAlpha(alpha));

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int size = CpBarVisuals.PRESET_SIZE;
        Font font = Minecraft.getInstance().font;
        // Le numero : la cellule de 46 de l'original, bornee par la gravure, donc grossie
        // dans la pose pour retrouver sa taille.
        int cell = cn.academy.client.gui.AcademyFonts.cell(CpBarVisuals.PRESET_CELL);
        float scale = CpBarVisuals.PRESET_CELL / (float) cell;

        for (int i = 0; i < 4; i++) {
            int x = CpBarVisuals.PRESET_X + i * CpBarVisuals.PRESET_STEP;
            int y = CpBarVisuals.PRESET_Y;

            graphics.fill(x, y, x + size, y + size, back);

            String label = String.valueOf(i + 1);
            double width = cn.academy.client.gui.AcademyFonts.width(font, label, cell) * scale;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + size / 2.0 - width / 2.0, y + CpBarVisuals.PRESET_LABEL_LIFT, 0.0);
            pose.scale(scale, scale, 1.0f);
            cn.academy.client.gui.AcademyFonts.drawTop(graphics, font, label, 0, 0, text, cell);
            pose.popPose();

            if (i == current) {
                cn.academy.client.gui.GlowSprites.draw(graphics, x, y, size, size,
                        CpBarVisuals.PRESET_GLOW, CpBarVisuals.PRESET_TEXT_RGB,
                        CpBarVisuals.PRESET_GLOW_ALPHA * Math.min(1.0f, alpha / CpBarVisuals.PRESET_ALPHA_SCALE));
            }
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
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
