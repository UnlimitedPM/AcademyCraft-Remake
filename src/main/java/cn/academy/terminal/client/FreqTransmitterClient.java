package cn.academy.terminal.client;

import java.util.List;

import cn.academy.AcademyCraft;
import cn.academy.ability.network.AbilityNetwork;
import cn.academy.energy.network.FreqTransmitterReplyPacket;
import cn.academy.energy.network.FreqTransmitterRequestPacket;
import cn.academy.terminal.app.AppFreqTransmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Le mode « Emetteur de frequence » : un HUD et des clics droits, comme {@code FreqTransmitterUI}.
 *
 * <p>Ouvrir l'application ferme le terminal et arme ce mode. Le clic droit sur un bloc fait alors
 * avancer le raccordement :
 * <ul>
 *   <li>un <b>Matrix</b> : mot de passe, puis clic droit sur un noeud a y rattacher ;</li>
 *   <li>un <b>noeud</b> : mot de passe, puis clic droit sur une machine ou un generateur.</li>
 * </ul>
 * Le clic gauche, un clic dans le vide ou vingt secondes sans rien faire abandonnent. Chaque etape
 * dit au joueur ce qu'on attend de lui, avec les textes de l'original.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AcademyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class FreqTransmitterClient {

    private enum Stage {
        START, QUERYING, ASK_PASS, AUTHORIZING, LINK_MATRIX, LINK_NODE, LINKING,
        NOTIFY_QUIT, NOTIFY_RETURN
    }

    private static final long IDLE_TIMEOUT = 20000L;
    private static final long NET_TIMEOUT = 3000L;
    private static final long QUIT_DELAY = 1200L;
    private static final long RETURN_DELAY = 700L;
    private static final String LANG = "ac.app.freq_transmitter.";

    private static boolean active;
    private static Stage stage = Stage.START;
    private static Stage returnStage = Stage.START;
    private static long since;
    private static long timeout;
    private static String message = "";
    private static int sourceKind;
    private static BlockPos source = BlockPos.ZERO;
    private static String sourceName = "";
    private static String pass = "";

    private FreqTransmitterClient() {}

    /** Arme le mode : appele par l'ouverture de l'application. */
    public static void start() {
        active = true;
        message = "";
        pass = "";
        source = BlockPos.ZERO;
        sourceKind = 0;
        enter(Stage.START, IDLE_TIMEOUT);
    }

    private static void enter(Stage next, long limit) {
        stage = next;
        since = System.currentTimeMillis();
        timeout = limit;
    }

    private static void notifyQuit(String key) {
        message = key;
        enter(Stage.NOTIFY_QUIT, QUIT_DELAY);
    }

    private static void notifyReturn(String key, Stage back) {
        message = key;
        returnStage = back;
        enter(Stage.NOTIFY_RETURN, RETURN_DELAY);
    }

    private static Stage linkStage() {
        return sourceKind == 1 ? Stage.LINK_MATRIX : Stage.LINK_NODE;
    }

    private static void send(FreqTransmitterRequestPacket packet) {
        AbilityNetwork.CHANNEL.sendToServer(packet);
    }

    // ------------------------------------------------------------------
    // Les clics
    // ------------------------------------------------------------------

    private static void onClick(BlockPos hit) {
        switch (stage) {
            case START -> {
                if (hit == null) {
                    active = false;
                    return;
                }
                send(FreqTransmitterRequestPacket.query(hit));
                enter(Stage.QUERYING, NET_TIMEOUT);
            }
            case LINK_MATRIX, LINK_NODE -> {
                if (hit == null) {
                    notifyQuit("e4");
                    return;
                }
                send(FreqTransmitterRequestPacket.link(source, hit, pass));
                message = "e5";
                enter(Stage.LINKING, NET_TIMEOUT);
            }
            default -> { }
        }
    }

    private static void onAbort() {
        if (stage == Stage.START) {
            active = false;
        } else if (stage == Stage.LINK_MATRIX || stage == Stage.LINK_NODE) {
            notifyQuit("e4");
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active || !(event.isUseItem() || event.isAttack())) return;
        event.setCanceled(true);
        event.setSwingHand(false);

        if (event.isAttack()) {
            onAbort();
            return;
        }
        HitResult hit = Minecraft.getInstance().hitResult;
        onClick(hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                ? block.getBlockPos() : null);
    }

    // ------------------------------------------------------------------
    // Les reponses du serveur
    // ------------------------------------------------------------------

    public static void onReply(FreqTransmitterReplyPacket reply) {
        if (!active) return;
        boolean ok = "ok".equals(reply.result());
        switch (reply.action()) {
            case QUERY -> {
                if (stage != Stage.QUERYING) return;
                if (!ok) {
                    notifyQuit(reply.result());
                    return;
                }
                source = reply.pos();
                sourceKind = reply.kind();
                sourceName = reply.name();
                pass = "";
                if (reply.encrypted()) {
                    enter(Stage.ASK_PASS, IDLE_TIMEOUT);
                    Minecraft.getInstance().setScreen(new PasswordScreen(sourceKind, sourceName));
                } else {
                    enter(linkStage(), IDLE_TIMEOUT);
                }
            }
            case AUTH -> {
                if (stage != Stage.AUTHORIZING) return;
                if (ok) {
                    enter(linkStage(), IDLE_TIMEOUT);
                } else {
                    notifyQuit("e1");
                }
            }
            case LINK -> {
                if (stage != Stage.LINKING) return;
                if (ok) {
                    notifyReturn("e6", linkStage());
                } else {
                    notifyQuit(reply.kind() == 1 ? "e2" : "e3");
                }
            }
        }
    }

    private static void submitPassword(String typed) {
        pass = typed;
        enter(Stage.AUTHORIZING, NET_TIMEOUT);
        send(FreqTransmitterRequestPacket.auth(source, typed));
        Minecraft.getInstance().setScreen(null);
    }

    private static void abortFromScreen() {
        if (stage == Stage.ASK_PASS) notifyQuit("e4");
    }

    // ------------------------------------------------------------------
    // Le temps
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;

        long elapsed = System.currentTimeMillis() - since;
        if (stage == Stage.NOTIFY_QUIT) {
            if (elapsed > timeout) active = false;
        } else if (stage == Stage.NOTIFY_RETURN) {
            if (elapsed > timeout) enter(returnStage, IDLE_TIMEOUT);
        } else if (elapsed > timeout) {
            Minecraft mc = Minecraft.getInstance();
            notifyQuit("st");
            if (mc.screen instanceof PasswordScreen) mc.setScreen(null);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
    }

    // ------------------------------------------------------------------
    // Le HUD
    // ------------------------------------------------------------------

    private static String messageKey() {
        return switch (stage) {
            case START, QUERYING -> "s0_0";
            case LINK_MATRIX -> "s2_0";
            case LINK_NODE -> "s3_0";
            case AUTHORIZING -> "s1_1";
            case LINKING -> "e5";
            case NOTIFY_QUIT, NOTIFY_RETURN -> message;
            case ASK_PASS -> "";
        };
    }

    private static void box(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0x77272727);
        graphics.renderOutline(x, y, width, height, 0xAAFFFFFF);
    }

    /** Le cadre du titre et celui du message, a la place qu'ils avaient dans l'original. */
    public static class Overlay implements IGuiOverlay {
        @Override
        public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                           int screenWidth, int screenHeight) {
            if (!active) return;
            Font font = gui.getFont();

            Component title = AppFreqTransmitter.INSTANCE.getDisplayName();
            box(graphics, 15, 15, 30 + font.width(title), 18);
            graphics.blit(AppFreqTransmitter.INSTANCE.getIcon(), 17, 15, 18, 18, 0f, 0f, 128, 128,
                    128, 128);
            graphics.drawString(font, title, 39, 20, 0xFFFFFFFF, false);

            String key = messageKey();
            if (key.isEmpty()) return;
            List<FormattedCharSequence> lines = font.split(Component.translatable(LANG + key), 120);
            int widest = 0;
            for (FormattedCharSequence line : lines) widest = Math.max(widest, font.width(line));
            int x = screenWidth / 2 + 10;
            int y = screenHeight / 2 + 10;
            box(graphics, x, y, widest + 20, lines.size() * font.lineHeight + 10);
            int row = y + 5;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(font, line, x + 5, row, 0xFFFFFFFF, false);
                row += font.lineHeight;
            }
        }
    }

    // ------------------------------------------------------------------
    // Le mot de passe
    // ------------------------------------------------------------------

    /** La saisie du mot de passe, masquee : « PASS: **** », comme chez l'original. */
    private static final class PasswordScreen extends Screen {
        private final int kind;
        private final String name;
        private EditBox box;

        PasswordScreen(int kind, String name) {
            super(Component.empty());
            this.kind = kind;
            this.name = name;
        }

        @Override
        protected void init() {
            box = new EditBox(font, width / 2 - 70, height / 2 - 10, 140, 20, Component.empty());
            box.setMaxLength(16);
            box.setFormatter((text, index) ->
                    FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
            addRenderableWidget(box);
            setInitialFocus(box);
        }

        @Override
        public boolean keyPressed(int key, int scan, int modifiers) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                submitPassword(box.getValue());
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }

        @Override
        public void onClose() {
            abortFromScreen();
            super.onClose();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            graphics.drawString(font, (kind == 1 ? "SSID: " : "NODE: ") + name, width / 2 - 70,
                    height / 2 - 24, 0xFFBFBFBF);
            graphics.drawString(font, Component.translatable(LANG + "s1_0"), width / 2 - 70,
                    height / 2 + 16, 0xFF30FFFF);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
