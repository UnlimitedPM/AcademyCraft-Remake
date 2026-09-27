package cn.academy.client.hud;

import cn.academy.AcademyCraft;
import cn.academy.client.gui.CustomizeUiLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Les messages du mod : le quatrieme et dernier element du HUD de l'original.
 *
 * <p>Portage de {@code NotifyUI} : le panneau, l'icone qui glisse depuis la droite et les deux
 * lignes. L'original n'en montrait qu'<b>une seule a la fois</b> — une nouvelle remplacait celle
 * qui passait — et elle vivait six secondes. Le deroulement est dans {@link NotificationVisuals},
 * qui se relit en JUnit.
 *
 * <p>Les mesures viennent de {@link CustomizeUiLayout}, ou l'ecran de reglage lit les siennes :
 * l'apercu et le HUD ne peuvent donc pas diverger.
 *
 * <p>Chez l'original, une seule chose produisait une notification : un tutoriel qui s'ouvre (voir
 * {@code ClientTutorialData}). Rien d'autre, et le port n'en invente pas — l'element sert a ce que
 * le mod a a dire, pas a meubler.
 */
public final class NotificationHud {

    private static final ResourceLocation PANEL = texture("notification/back");

    /** La notification en cours, et son age en secondes. Une seule a la fois, comme l'original. */
    private static ResourceLocation icon;
    private static Component title;
    private static Component content;
    private static float age;

    private NotificationHud() {}

    /** Montre une notification, en remplacant celle qui passe. */
    public static void show(ResourceLocation icon, Component title, Component content) {
        NotificationHud.icon = icon;
        NotificationHud.title = title;
        NotificationHud.content = content;
        NotificationHud.age = 0f;
    }

    /** Un tick client : la notification vieillit et disparait au bout de ses six secondes. */
    public static void tick() {
        if (icon == null) return;
        age += 1f / 20f;
        if (!NotificationVisuals.visible(age)) clear();
    }

    /** Oublie celle qui passe. */
    public static void clear() {
        icon = null;
        title = null;
        content = null;
        age = 0f;
    }

    /** Y a-t-il quelque chose a l'ecran ? (Pour le debogage et les tests.) */
    public static boolean isShowing() {
        return icon != null;
    }

    /** L'age de la notification en cours, en secondes. */
    public static float getAge() {
        return age;
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (icon == null) return;

        HudElement element = HudElement.NOTIFICATION;
        HudLayout layout = HudConfig.read();
        int left = layout.placeX(element, screenWidth, element.getWidth());
        int top = layout.placeY(element, screenHeight, element.getHeight());

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0);
        pose.scale(CustomizeUiLayout.NOTIFY_SCALE, CustomizeUiLayout.NOTIFY_SCALE, 1.0f);

        // Le panneau : son opacite entraine celle du reste, comme dans l'original ou les trois
        // etaient teintes ensemble pendant la sortie.
        drawImage(graphics, PANEL, 0, 0, CustomizeUiLayout.NOTIFY_W, CustomizeUiLayout.NOTIFY_H,
                CustomizeUiLayout.NOTIFY_W, CustomizeUiLayout.NOTIFY_H,
                NotificationVisuals.fade(age));

        // L'icone, qui glisse de la droite vers sa place pendant la premiere seconde.
        float iconAlpha = NotificationVisuals.iconAlpha(age);
        if (iconAlpha > 0f) {
            int side = CustomizeUiLayout.NOTIFY_ICON;
            drawImage(graphics, icon, (int) NotificationVisuals.iconX(age),
                    CustomizeUiLayout.NOTIFY_ICON_Y, side, side, side, side, iconAlpha);
        }

        // Les deux lignes, qui n'arrivent qu'avec le glissement.
        float textAlpha = NotificationVisuals.textAlpha(age);
        drawLine(graphics, title, CustomizeUiLayout.NOTIFY_TITLE_X,
                CustomizeUiLayout.NOTIFY_TITLE_Y, CustomizeUiLayout.NOTIFY_TITLE_FONT, textAlpha);
        drawLine(graphics, content, CustomizeUiLayout.NOTIFY_TEXT_X,
                CustomizeUiLayout.NOTIFY_TEXT_Y, CustomizeUiLayout.NOTIFY_TEXT_FONT, textAlpha);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.popPose();
    }

    /**
     * Une image, avec son opacite.
     *
     * <p>Pour l'icone, la zone lue et la taille declaree valent toutes les deux sa taille
     * d'affichage : les UV restent donc dans 0..1 quelle que soit la dimension reelle du fichier,
     * et l'image est <b>etiree</b> a 83x83. C'est ce que faisait l'original, qui dessinait un
     * rectangle la ou sa texture etait liee sans jamais lire ses dimensions. Declarer la vraie
     * taille du fichier ferait deborder les UV, et l'image se repeterait.
     *
     * <p>Le melange est rallume avant chaque image : tout ce qui se dessine (une ligne de texte,
     * un aplat) l'eteint en se nettoyant.
     */
    private static void drawImage(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                                  int width, int height, int sourceWidth, int sourceHeight,
                                  float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        graphics.blit(texture, x, y, width, height, 0f, 0f, sourceWidth, sourceHeight,
                sourceWidth, sourceHeight);
    }

    /** Une des deux lignes, a l'echelle de l'original et dans la pose du panneau. */
    private static void drawLine(GuiGraphics graphics, Component text, int x, int y,
                                 float fontSize, float alpha) {
        if (text == null || alpha <= 0f) return;

        // L'original ne laissait jamais une ligne plus transparente qu'un dixieme : c'est ce qui
        // fait qu'on la voit encore pendant la sortie.
        float visible = Math.max(0.1f, alpha);
        float scale = CustomizeUiLayout.plainFontScale(fontSize);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, visible);
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0,
                CustomizeUiLayout.NOTIFY_TEXT_COLOR, false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.popPose();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AcademyCraft.MOD_ID,
                "textures/guis/" + path + ".png");
    }
}
