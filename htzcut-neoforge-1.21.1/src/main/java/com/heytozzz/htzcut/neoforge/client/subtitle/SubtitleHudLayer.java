package com.heytozzz.htzcut.neoforge.client.subtitle;

import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.neoforge.HTZCutMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Renders the dialogue subtitle box as a 9-slice panel (corners fixed,
 * edges/center tiled) built from nine 16x16 textures, with the wrapped
 * subtitle text centered inside. Registered as a GUI layer in
 * NetworkRegistration's sibling client init (see HTZCutMod).
 *
 * Tile textures are all 16x16 and individually tileable, so any box
 * size just needs to be rounded up to a multiple of TILE.
 *
 * Note: uses GuiGraphics#blit's classic (u, v, width, height,
 * textureWidth, textureHeight) overload. If a future Minecraft/NeoForge
 * bump replaces that with the atlas-sprite blit variant, only this
 * class's draw calls need updating - the box-size math above is
 * unaffected.
 */
public final class SubtitleHudLayer {

    private static final int TILE = 16;
    private static final int PADDING = 12;
    private static final int MAX_TEXT_WIDTH = 240;
    private static final int MIN_COLS = 4;
    private static final int MIN_ROWS = 3;
    private static final int BOTTOM_MARGIN = 40; // clears the hotbar/xp bar
    private static final int TOP_MARGIN = 10;

    private static final ResourceLocation TOP_LEFT = texture("top_left");
    private static final ResourceLocation TOP = texture("top");
    private static final ResourceLocation TOP_RIGHT = texture("top_right");
    private static final ResourceLocation LEFT = texture("left");
    private static final ResourceLocation CENTER = texture("center");
    private static final ResourceLocation RIGHT = texture("right");
    private static final ResourceLocation BOTTOM_LEFT = texture("bottom_left");
    private static final ResourceLocation BOTTOM = texture("bottom");
    private static final ResourceLocation BOTTOM_RIGHT = texture("bottom_right");

    private SubtitleHudLayer() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(HTZCutMod.MOD_ID, "textures/gui/subtitle_box/" + name + ".png");
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!ClientSubtitleHandler.isActive()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(Component.literal(ClientSubtitleHandler.text()), MAX_TEXT_WIDTH);
        if (lines.isEmpty()) {
            return;
        }

        int textWidth = 0;
        for (FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        int textHeight = lines.size() * font.lineHeight;

        int cols = Math.max(MIN_COLS, ceilDiv(textWidth + PADDING * 2, TILE));
        int rows = Math.max(MIN_ROWS, ceilDiv(textHeight + PADDING * 2, TILE));
        int boxWidth = cols * TILE;
        int boxHeight = rows * TILE;

        int x = (guiGraphics.guiWidth() - boxWidth) / 2;
        int y = ClientSubtitleHandler.position() == SubtitlePosition.TOP
                ? TOP_MARGIN
                : guiGraphics.guiHeight() - boxHeight - BOTTOM_MARGIN;

        drawBox(guiGraphics, x, y, cols, rows);

        int textY = y + (boxHeight - textHeight) / 2;
        for (FormattedCharSequence line : lines) {
            int lineWidth = font.width(line);
            int textX = x + (boxWidth - lineWidth) / 2;
            guiGraphics.drawString(font, line, textX, textY, 0xFFFFFF);
            textY += font.lineHeight;
        }
    }

    private static void drawBox(GuiGraphics g, int x, int y, int cols, int rows) {
        int right = x + (cols - 1) * TILE;
        int bottom = y + (rows - 1) * TILE;

        blit(g, TOP_LEFT, x, y);
        blit(g, TOP_RIGHT, right, y);
        blit(g, BOTTOM_LEFT, x, bottom);
        blit(g, BOTTOM_RIGHT, right, bottom);

        for (int col = 1; col < cols - 1; col++) {
            int tileX = x + col * TILE;
            blit(g, TOP, tileX, y);
            blit(g, BOTTOM, tileX, bottom);
        }

        for (int row = 1; row < rows - 1; row++) {
            int tileY = y + row * TILE;
            blit(g, LEFT, x, tileY);
            blit(g, RIGHT, right, tileY);
        }

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < cols - 1; col++) {
                blit(g, CENTER, x + col * TILE, y + row * TILE);
            }
        }
    }

    private static void blit(GuiGraphics g, ResourceLocation texture, int x, int y) {
        g.blit(texture, x, y, 0, 0, TILE, TILE, TILE, TILE);
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
