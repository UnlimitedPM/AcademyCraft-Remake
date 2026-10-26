package cn.academy.client.gui;

import cn.academy.ability.client.MineDetectFilter;
import cn.academy.ability.electromaster.MetalTargets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L'ecran de filtre de la detection de minerais : un minerai en vert, les autres en bleu.
 *
 * <p>Ouvert depuis <b>Settings &gt; Misc &gt; Filtrer les minerais</b>, comme le reglage du HUD.
 * Il ne regle rien qui appartienne au serveur : l'eclat est dessine chez le joueur, sur les
 * minerais qu'il a lui-meme balayes, et le choix vit donc dans la config du client — voir
 * {@link MineDetectFilter}.
 *
 * <p>La grille montre les memes blocs que le balayage reconnait ({@code MetalTargets.isOreBlock}),
 * et pas une liste ecrite a la main : un minerai ajoute par un autre mod s'y trouvera donc tout
 * seul, comme il se balaie tout seul. La premiere case est <b>aucun</b>, qui rend l'eclat d'avant.
 *
 * <p>Sa langue visuelle est celle du reste du mod : fond sombre, cerne clair, texte blanc, et le
 * vert de l'eclat pour le choix. Rien n'est emprunte a l'original, qui n'avait pas cet ecran — le
 * joueur l'a demande le 26/10, et c'est donc une piece du port.
 */
@OnlyIn(Dist.CLIENT)
public class OreFilterScreen extends Screen {

    /** La grille : cinq colonnes de quatre, vingt minerais par page. */
    private static final int COLUMNS = 5;
    private static final int ROWS = 4;
    private static final int CELL = 26;
    private static final int GAP = 4;
    private static final int PAD = 12;
    private static final int HEADER = 30;
    private static final int FOOTER = 22;

    private static final int BACK = 0xE612171D;
    private static final int EDGE = 0xFF97A3B0;
    private static final int CELL_BACK = 0x30FFFFFF;
    private static final int HOVER = 0xFFFFFFFF;
    private static final int CHOSEN = 0xFF5AE678;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA8B2BC;

    /** L'ecran d'ou l'on vient, pour y revenir sur Echap. */
    private final Screen parent;

    private final List<Entry> entries = new ArrayList<>();

    private int page;
    private int hovered = -1;

    /**
     * Un minerai de la grille.
     *
     * @param block le bloc, ou {@code null} pour la case « aucun »
     * @param key   sa cle d'enregistrement, celle qui s'ecrit dans la config
     * @param name  son nom, tel que le jeu l'ecrit
     */
    private record Entry(Block block, String key, Component name) {}

    public OreFilterScreen(Screen parent) {
        super(Component.translatable("ac.gui.orefilter.title"));
        this.parent = parent;

        // « Aucun » d'abord : c'est le defaut du mod, et il ne doit pas se chercher dans la grille.
        entries.add(new Entry(null, "", Component.translatable("ac.gui.orefilter.none")));

        // Les memes blocs que le balayage : la regle est celle de MetalTargets, pas une liste.
        List<Block> ores = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR) continue;
            if (!MetalTargets.isOreBlock(block.defaultBlockState())) continue;
            ores.add(block);
        }
        ores.sort(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        for (Block block : ores) {
            entries.add(new Entry(block, BuiltInRegistries.BLOCK.getKey(block).toString(),
                    block.getName()));
        }
    }

    // ------------------------------------------------------------------
    // La geometrie
    // ------------------------------------------------------------------

    private int panelWidth() {
        return COLUMNS * CELL + (COLUMNS - 1) * GAP + 2 * PAD;
    }

    private int panelHeight() {
        return HEADER + ROWS * CELL + (ROWS - 1) * GAP + FOOTER;
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int perPage() {
        return COLUMNS * ROWS;
    }

    private int pages() {
        return Math.max(1, (entries.size() + perPage() - 1) / perPage());
    }

    private int cellLeft(int slot) {
        return panelX() + PAD + (slot % COLUMNS) * (CELL + GAP);
    }

    private int cellTop(int slot) {
        return panelY() + HEADER + (slot / COLUMNS) * (CELL + GAP);
    }

    /** La case sous la souris, ou {@code -1}. */
    private int slotAt(double mouseX, double mouseY) {
        for (int slot = 0; slot < perPage(); ++slot) {
            int index = page * perPage() + slot;
            if (index >= entries.size()) break;
            int left = cellLeft(slot);
            int top = cellTop(slot);
            if (mouseX >= left && mouseX < left + CELL && mouseY >= top && mouseY < top + CELL) {
                return index;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Le dessin
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int left = panelX();
        int top = panelY();
        int right = left + panelWidth();
        int bottom = top + panelHeight();

        // Le panneau : le meme genre de fenetre que les autres ecrans du mod, sans texture pour
        // ne pas la deformer — un cerne clair et un fond sombre translucide.
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, EDGE);
        graphics.fill(left, top, right, bottom, BACK);
        graphics.fill(left, top, right, top + HEADER - 8, 0x30FFFFFF);

        String heading = title.getString();
        int titleX = (width - AcademyFonts.width(font, heading)) / 2;
        AcademyFonts.draw(graphics, font, heading, titleX, top + 8, TEXT, false);

        hovered = slotAt(mouseX, mouseY);

        for (int slot = 0; slot < perPage(); ++slot) {
            int index = page * perPage() + slot;
            if (index >= entries.size()) break;

            Entry entry = entries.get(index);
            int x = cellLeft(slot);
            int y = cellTop(slot);
            boolean chosen = entry.key().equals(MineDetectFilter.selected());

            graphics.fill(x, y, x + CELL, y + CELL,
                    chosen ? 0x505AE678 : CELL_BACK);
            if (entry.block() == null) {
                // « Aucun » : une case barree, dessinee a la main faute d'icone qui veuille dire ca.
                graphics.fill(x + 5, y + CELL / 2, x + CELL - 5, y + CELL / 2 + 1,
                        chosen ? CHOSEN : TEXT_DIM);
            } else {
                graphics.renderItem(new ItemStack(entry.block()), x + (CELL - 16) / 2,
                        y + (CELL - 16) / 2);
            }

            if (chosen) {
                graphics.renderOutline(x, y, CELL, CELL, CHOSEN);
            } else if (index == hovered) {
                graphics.renderOutline(x, y, CELL, CELL, HOVER);
            }
        }

        // Le bas : ce que la souris survole, sinon ce qui est choisi, et la page.
        Entry shown = hovered >= 0 ? entries.get(hovered) : null;
        if (shown == null) {
            String key = MineDetectFilter.selected();
            for (Entry entry : entries) {
                if (entry.key().equals(key)) {
                    shown = entry;
                    break;
                }
            }
        }
        if (shown != null) {
            String name = shown.name().getString();
            int nameX = (width - AcademyFonts.width(font, name)) / 2;
            AcademyFonts.draw(graphics, font, name, nameX, bottom - FOOTER + 5, TEXT, false);
        }

        Component pageText = Component.literal((page + 1) + " / " + pages());
        AcademyFonts.draw(graphics, font, pageText, right - PAD - AcademyFonts.width(font, pageText),
                bottom - FOOTER + 5, TEXT_DIM, false);
    }

    // ------------------------------------------------------------------
    // Les gestes
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = slotAt(mouseX, mouseY);
            if (index >= 0) {
                MineDetectFilter.select(entries.get(index).key());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY != 0) {
            page = Math.max(0, Math.min(pages() - 1, page + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Les fleches tournent les pages : c'est plus sur qu'une molette quand on n'en a pas.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) {
            page = Math.min(pages() - 1, page + 1);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) {
            page = Math.max(0, page - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
