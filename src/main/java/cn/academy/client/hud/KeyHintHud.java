package cn.academy.client.hud;

import cn.academy.ability.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.client.AbilityKeyBindings;
import cn.academy.ability.client.ClientAbilityData;
import cn.academy.ability.preset.client.ClientPresetData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Le rappel des touches d'aptitude : le <i>Control Hint</i> de l'original.
 *
 * <p>Une ligne par touche d'aptitude, posee a la place que la config lui donne. Elle montre le
 * <b>capuchon</b> de la touche, l'<b>icone</b> de la competence que le prereglage en service y a
 * rangee, et la <b>recharge</b> qui reste (le bas de l'icone s'assombrit). C'est le portage de
 * {@code KeyHintUI} : ses mesures, son echelle et ses textures sont les siennes, et tout est
 * dans {@link KeyHintVisuals}, qui se relit en JUnit.
 *
 * <p>Deux choses que l'original faisait et qui manquent : le fondu de 0,3 s a l'apparition, et
 * le halo clignotant autour d'une competence utilisable (son {@code drawGlow}, qui demandait le
 * moteur de rendu de lambdalib2). Le reste y est, y compris le gris du capuchon quand
 * l'aptitude ne peut pas servir — surcharge ou brouillage.
 */
public final class KeyHintHud {

    private static final ResourceLocation PLATE = texture("back");
    private static final ResourceLocation FRAME = texture("icon_back");

    /** La plaque en degrade fait 256x83 chez l'original, etiree sur la largeur de la ligne. */
    private static final int PLATE_TEX_W = 256;
    private static final int PLATE_TEX_H = 83;

    /** La taille de la police du jeu, en pixels : c'est elle qui donne l'echelle du texte. */
    private static final float FONT_SIZE = 9.0f;
    /** La taille de l'etiquette chez l'original ({@code new FontOption(32, ...)}). */
    private static final float LABEL_SIZE = 32.0f;

    private KeyHintHud() {}

    /**
     * Dessine le rappel, ou l'ecran et la config le posent.
     *
     * <p>Rien n'est dessine tant que l'aptitude est eteinte : l'original accrochait cet element
     * a {@code CPData.isActivated()}, comme le temoin de CP. Les deux apparaissent et
     * disparaissent donc ensemble.
     */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        AbilityData data = ClientAbilityData.get();
        if (!data.isActivated()) return;

        HudElement element = HudElement.KEY_HINT;
        HudLayout layout = HudConfig.read();
        int left = layout.placeX(element, screenWidth, element.getWidth());
        int top = layout.placeY(element, screenHeight, element.getHeight());

        // L'aptitude ne peut pas servir : l'original grisait le capuchon plutot que de cacher
        // le rappel, pour que le joueur voie ce qu'il a sous la main. C'est le VERROU qui
        // compte ici — il tient jusqu'a la fin de la descente — et non l'etat montre par la
        // barre, qui s'efface des le delai ecoule.
        boolean usable = !data.isOverloadRecovering() && !data.isInterfered();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0);
        pose.scale(KeyHintVisuals.SCALE, KeyHintVisuals.SCALE, 1.0f);
        // L'original reculait toute sa colonne de lignes avant de dessiner : sans ce decalage,
        // plaque, cadre et icone mordent sur le bord droit de l'ecran (voir
        // KeyHintVisuals.CONTENT_SHIFT_X, ou le chiffre est explique).
        pose.translate(KeyHintVisuals.CONTENT_SHIFT_X, 0, 0);

        // Seules les touches qui portent une competence s'affichent, et elles se suivent sans
        // trou : c'est ce que faisait l'original, qui ne parcourait que ses delegues existants
        // et avancait d'une ligne a chacun.
        java.util.List<String> names = new java.util.ArrayList<>(KeyHintVisuals.ROWS);
        for (int slot = 0; slot < KeyHintVisuals.ROWS; slot++) {
            names.add(ClientPresetData.skillAt(slot));
        }
        int row = 0;
        for (int slot : KeyHintVisuals.visibleSlots(names)) {
            drawRow(graphics, data, slot, row * KeyHintVisuals.ROW_STEP, usable);
            row++;
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        pose.popPose();
        RenderSystem.disableBlend();
    }

    /** Une ligne : la plaque, le capuchon, la competence, et la recharge qui reste. */
    private static void drawRow(GuiGraphics graphics, AbilityData data, int slot, int y,
                                boolean usable) {
        blit(graphics, PLATE, 0, y, KeyHintVisuals.PLATE_W, KeyHintVisuals.PLATE_H,
                0, 0, PLATE_TEX_W, PLATE_TEX_H, PLATE_TEX_W, PLATE_TEX_H, 0xFFFFFF, 1.0f);

        KeyMapping key = AbilityKeyBindings.abilityKey(slot);
        boolean mouse = key.getKey().getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE;
        int button = key.getKey().getValue();
        String label = key.getTranslatedKeyMessage().getString();
        KeyHintVisuals.Cap cap = KeyHintVisuals.cap(mouse, button);

        // Le capuchon, gris quand l'aptitude ne peut pas servir (comme l'original).
        float capTint = usable ? 1.0f : 0.7f;
        blit(graphics, capTexture(cap, label),
                KeyHintVisuals.CAP_X, y + KeyHintVisuals.CAP_Y,
                KeyHintVisuals.CAP_SIZE, KeyHintVisuals.CAP_SIZE,
                0, 0, 50, 48, 50, 48,
                gray(capTint), 1.0f);

        String capLabel = KeyHintVisuals.capLabel(cap, label, button);
        if (!capLabel.isEmpty()) {
            drawLabel(graphics, capLabel, y);
        }

        // Le cadre de l'icone, puis l'icone de la competence rangee sur cette touche.
        blit(graphics, FRAME,
                KeyHintVisuals.FRAME_X, y + KeyHintVisuals.FRAME_Y,
                KeyHintVisuals.FRAME_SIZE, KeyHintVisuals.FRAME_SIZE,
                0, 0, KeyHintVisuals.FRAME_SIZE, KeyHintVisuals.FRAME_SIZE,
                KeyHintVisuals.FRAME_SIZE, KeyHintVisuals.FRAME_SIZE, 0xFFFFFF, 1.0f);

        Skill skill = KeyHintVisuals.skillByName(ClientPresetData.skillAt(slot));
        if (skill == null) return;

        // La duree vient de la donnee du JOUEUR (celle qui a ete posee), et non de la courbe :
        // trois competences posent leur recharge depuis leur effet et n'annoncent aucune duree
        // (voir AbilityData.getCooldownTotal), donc leur icone ne s'estompait jamais.
        int total = data.getCooldownTotal(skill);
        if (total <= 0) total = skill.getCooldownTicks(data);
        float remaining = KeyHintVisuals.cooldownFraction(data.getCooldown(skill), total);

        ResourceLocation icon = skill.getHintIcon();
        blit(graphics, icon,
                KeyHintVisuals.ICON_X, y + KeyHintVisuals.ICON_Y,
                KeyHintVisuals.ICON_SIZE, KeyHintVisuals.ICON_SIZE,
                0, 0, KeyHintVisuals.ICON_SIZE, KeyHintVisuals.ICON_SIZE,
                KeyHintVisuals.ICON_SIZE, KeyHintVisuals.ICON_SIZE, 0xFFFFFF,
                KeyHintVisuals.cooldownIconAlpha(remaining));

        // La recharge qui reste : l'original assombrissait le bas de l'icone d'autant, en plus
        // de l'estomper entierement (voir COOLDOWN_ICON_ALPHA).
        if (remaining <= 0f) return;

        int filled = Math.round(KeyHintVisuals.ICON_SIZE * remaining);
        graphics.fill(
                KeyHintVisuals.ICON_X, y + KeyHintVisuals.ICON_Y + KeyHintVisuals.ICON_SIZE - filled,
                KeyHintVisuals.ICON_X + KeyHintVisuals.ICON_SIZE,
                y + KeyHintVisuals.ICON_Y + KeyHintVisuals.ICON_SIZE,
                KeyHintVisuals.COOLDOWN_OVERLAY);
    }

    /**
     * L'etiquette du capuchon, centree dessus.
     *
     * <p>L'original la dessinait avec une police de 32 unites, dans l'espace du rappel : le
     * texte se remet donc a l'echelle de cette police, sinon la pose du rappel ne laisserait
     * que quelques pixels.
     */
    private static void drawLabel(GuiGraphics graphics, String text, int rowY) {
        var font = Minecraft.getInstance().font;
        float scale = LABEL_SIZE / FONT_SIZE;
        int width = font.width(text);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(
                KeyHintVisuals.CAP_X + KeyHintVisuals.CAP_SIZE / 2.0f - width * scale / 2.0f,
                rowY + KeyHintVisuals.CAP_Y + KeyHintVisuals.CAP_SIZE / 2.0f - FONT_SIZE * scale / 2.0f,
                0);
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, KeyHintVisuals.LABEL_COLOR, false);
        pose.popPose();
    }

    private static ResourceLocation capTexture(KeyHintVisuals.Cap cap, String label) {
        return texture(KeyHintVisuals.capTexture(cap, label));
    }

    /** Un gris a trois canaux egaux, pour la teinte du capuchon. */
    private static int gray(float value) {
        int channel = Math.round(255.0f * value);
        return (channel << 16) | (channel << 8) | channel;
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture,
                             int x, int y, int width, int height,
                             int u, int v, int uWidth, int vHeight, int texW, int texH,
                             int rgb, float alpha) {
        if (width <= 0 || height <= 0) return;
        // Le melange se perd en route, et c'est le piege de cet element. `drawString` (comme
        // `fill`) dessine via un RenderType translucide dont le nettoyage appelle
        // `RenderSystem.disableBlend()` : tout ce qui suit l'etiquette d'un capuchon se
        // dessine donc sans melange. Consequences vecues : les pixels semi-transparents du
        // cadre et de l'icone s'ecrivent en plein (cadre plus sombre, icone opaque), et
        // l'estompage de recharge est ignore sur toutes les lignes SAUF la premiere (ses deux
        // touches sont des boutons de souris, donc sans etiquette). On rallume avant chaque
        // texture : c'est le seul endroit qui dessine ici.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f, alpha);
        graphics.blit(texture, x, y, width, height, u, v, uWidth, vHeight, texW, texH);
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("academy", "textures/guis/key_hint/" + path + ".png");
    }
}
